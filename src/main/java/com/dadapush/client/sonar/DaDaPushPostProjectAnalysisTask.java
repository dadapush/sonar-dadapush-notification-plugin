package com.dadapush.client.sonar;

import com.dadapush.client.ApiClient;
import com.dadapush.client.ApiException;
import com.dadapush.client.Configuration;
import com.dadapush.client.api.DaDaPushMessageApi;
import com.dadapush.client.model.MessagePushRequest;
import com.dadapush.client.model.ResultOfMessagePushResponse;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@SuppressWarnings("deprecation")
public class DaDaPushPostProjectAnalysisTask extends AbstractComponent implements
    PostProjectAnalysisTask {

  private static final Logger LOG = Loggers.get(DaDaPushPostProjectAnalysisTask.class);

  private final I18n i18n;

  private final ApiClient apiClient;

  private DecimalFormat percentageFormat;

  public DaDaPushPostProjectAnalysisTask(Settings settings, I18n i18n) {
    super(settings);
    apiClient = Configuration.getDefaultApiClient();
    this.i18n = i18n;
    this.percentageFormat = new DecimalFormat();
    this.percentageFormat.setMaximumFractionDigits(2);
  }

  @Override
  public void finished(ProjectAnalysis analysis) {
    refreshSettings();
    if (!isPluginEnabled()) {
      LOG.info("DaDaPush Notification plugin disabled, skipping. Settings are [{}]",
          logRelevantSettings());
      return;
    }
    LOG.info("Analysis ScannerContext: [{}]", analysis.getScannerContext().getProperties());
    String projectKey = analysis.getProject().getKey();

    Optional<ProjectConfig> projectConfigOptional = getProjectConfig(projectKey);
    if (!projectConfigOptional.isPresent()) {
      return;
    }

    ProjectConfig projectConfig = projectConfigOptional.get();
    if (shouldSkipSendingNotification(projectConfig, analysis.getQualityGate())) {
      return;
    }
    if (StringUtils.isEmpty(projectConfig.getChannelToken())) {
      LOG.info("DaDaPush Notification will not sent, " + analysis.toString(),
          projectConfig.toString());
      return;
    }
    LOG.info("DaDaPush Notification will be sent: " + analysis.toString());

    String basePath = getBasePath();
    if (StringUtils.isNotEmpty(basePath)) {
      apiClient.setBasePath(basePath);
    }

    QualityGate qualityGate = analysis.getQualityGate();

    StringBuilder titleBuilder = new StringBuilder();

    StringBuilder contentBuilder = new StringBuilder();
    if (qualityGate != null) {
      titleBuilder.append(qualityGate.getStatus());
    } else {
      titleBuilder.append("Unknown Status");
    }

    contentBuilder.append("Project: ").append(analysis.getProject().getName()).append("\n");

    if (qualityGate != null) {
      contentBuilder.append("Status: ").append(qualityGate.getStatus()).append("\n");
      List<String> collect = qualityGate.getConditions()
          .stream()
          .filter(condition -> !projectConfig.isQgFailOnly() || notOkNorNoValue(condition))
          .map(this::translate)
          .collect(Collectors.toList());

      final StringBuilder result = new StringBuilder();
      Iterator<String> iterator = collect.iterator();
      while (iterator.hasNext()) {
        final String value = Objects.toString(iterator.next(), "");
        result.append(value);

        if (iterator.hasNext()) {
          result.append("\n");
        }
      }
      contentBuilder.append(result.toString()).append("\n");
    } else {
      contentBuilder.append("Status: Unknown").append("\n");
    }

    DaDaPushMessageApi apiInstance = new DaDaPushMessageApi(apiClient);
    MessagePushRequest body = new MessagePushRequest();
    body.setTitle(StringUtils.substring(titleBuilder.toString(), 0, 50));
    body.setContent(StringUtils.substring(contentBuilder.toString(), 0, 500));
    body.setNeedPush(true);
    try {
      ResultOfMessagePushResponse result = apiInstance
          .createMessage(body, Objects.requireNonNull(projectConfig.getChannelToken()));
      if (result.getCode() == 0) {
        LOG.info("send notification success, messageId=" + result.getData().getMessageId());
      } else {
        LOG
            .warn("send DaDaPush Notification fail, detail: " + result.getCode() + " " + result
                .getErrmsg());
      }
    } catch (ApiException e) {
      LOG.error("send DaDaPush Notification fail", e);
    }
  }

  private boolean notOkNorNoValue(QualityGate.Condition condition) {
    return !(QualityGate.EvaluationStatus.OK.equals(condition.getStatus())
        || QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus()));
  }

  private String translate(QualityGate.Condition condition) {
    String i18nKey = "metric." + condition.getMetricKey() + ".name";
    String conditionName = i18n.message(Locale.ENGLISH, i18nKey, condition.getMetricKey());

    if (QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus())) {
      // No value for given metric
      return conditionName + "\n" + condition.getStatus().name();
    } else {
      StringBuilder sb = new StringBuilder();
      appendValue(condition, sb);
      appendValuePostfix(condition, sb);
      if (condition.getWarningThreshold() != null) {
        sb.append(", warning if ");
        appendValueOperatorPrefix(condition, sb);
        sb.append(condition.getWarningThreshold());
        appendValuePostfix(condition, sb);
      }
      if (condition.getErrorThreshold() != null) {
        sb.append(", error if ");
        appendValueOperatorPrefix(condition, sb);
        sb.append(condition.getErrorThreshold());
        appendValuePostfix(condition, sb);
      }
      return conditionName + ": " + condition.getStatus().name() + "\n" + sb.toString();
    }
  }

  private void appendValue(QualityGate.Condition condition, StringBuilder sb) {
    if ("".equals(condition.getValue())) {
      sb.append("value: ");
      sb.append("NaN");
    } else {
      if (valueIsPercentage(condition)) {
        sb.append("value: ");
        appendPercentageValue(condition.getValue(), sb);
      } else {
        sb.append("value: ");
        sb.append(condition.getValue());
      }
    }
  }

  private void appendPercentageValue(String s, StringBuilder sb) {
    try {
      Double d = Double.parseDouble(s);
      sb.append(percentageFormat.format(d));
    } catch (NumberFormatException e) {
      LOG.error("Failed to parse [{}] into a Double due to [{}]", s, e.getMessage());
      sb.append(s);
    }
  }

  private void appendValueOperatorPrefix(QualityGate.Condition condition, StringBuilder sb) {
    switch (condition.getOperator()) {
      case EQUALS:
        sb.append("==");
        break;
      case NOT_EQUALS:
        sb.append("!=");
        break;
      case GREATER_THAN:
        sb.append(">");
        break;
      case LESS_THAN:
        sb.append("<");
        break;
    }
  }

  private void appendValuePostfix(QualityGate.Condition condition, StringBuilder sb) {
    if (valueIsPercentage(condition)) {
      sb.append("%");
    }
  }

  private boolean valueIsPercentage(QualityGate.Condition condition) {
    switch (condition.getMetricKey()) {
      case CoreMetrics.NEW_COVERAGE_KEY:
      case CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY:
        return true;
    }
    return false;
  }

}
