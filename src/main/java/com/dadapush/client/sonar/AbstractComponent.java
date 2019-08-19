package com.dadapush.client.sonar;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public abstract class AbstractComponent {

    private static final Logger LOG = Loggers.get(AbstractComponent.class);

    private final Settings settings;
    private Map<String, ProjectConfig> projectConfigMap = Collections.emptyMap();

    public AbstractComponent(Settings settings) {
        this.settings = settings;
    }

    /**
     * This method has to be called in the beginning of every actual plugin execution.
     * SonarQube seems to work in such a way that
     * <pre>
     * 1) the Settings object is constructor injected to this class.
     * 2) the values reflected by the Settings object reflect latest settings configured
     * 3) but the constructor of this class is called only once, and after that the class is never instantiated again (the same instance is reused)
     * 4) thus when the instance is used to perform something, we must refresh the projectConfigMap when the execution starts
     * </pre>
     */
    protected void refreshSettings() {
        LOG.info("Refreshing settings");
        refreshProjectConfigs();
    }

    private void refreshProjectConfigs() {
        LOG.info("Refreshing project configs");
        Set<ProjectConfig> oldValues = new HashSet<>();
        this.projectConfigMap.values().forEach(c -> oldValues.add(new ProjectConfig(c)));
        this.projectConfigMap = buildProjectConfigByProjectKeyMap(settings);
        Set<ProjectConfig> newValues = new HashSet<>(this.projectConfigMap.values());
        if (!oldValues.equals(newValues)) {
            LOG.info("Old configs [{}] --> new configs [{}]", oldValues, newValues);
        }
    }

    protected String getBasePath() {
        return settings.getString(DaDaPushNotificationProp.BASE_PATH.property());
    }

    protected boolean isPluginEnabled() {
        return settings.getBoolean(DaDaPushNotificationProp.ENABLED.property());
    }

    /**
     * Returns the sonar server url, with a trailing /
     *
     * @return
     */
    protected String getSonarServerUrl() {
        String u = settings.getString("sonar.core.serverBaseURL");
        if (u == null) {
            return null;
        }
        if (u.endsWith("/")) {
            return u;
        }
        return u + "/";
    }

    protected Optional<ProjectConfig> getProjectConfig(String projectKey) {
        List<ProjectConfig> projectConfigs = projectConfigMap.keySet()
                .stream()
                .filter(key -> key.endsWith("*") ? projectKey.startsWith(key.substring(0, key.length() - 1))
                        : key.equals(projectKey))
                .map(projectConfigMap::get)
                .collect(Collectors.toList());
        // Not configured at all
        if (projectConfigs.isEmpty()) {
            LOG.info("Could not find config for project [{}] in [{}]", projectKey, projectConfigMap);
            return Optional.empty();
        }

        if(projectConfigs.size() > 1) {
            LOG.warn("More than 1 project key was matched. Using first one: {}", projectConfigs.get(0).getProjectKey());
        }
        return Optional.of(projectConfigs.get(0));
    }

    private static Map<String, ProjectConfig> buildProjectConfigByProjectKeyMap(Settings settings) {
        Map<String, ProjectConfig> map = new HashMap<>();
        String[] projectConfigIndexes = settings.getStringArray(DaDaPushNotificationProp.CONFIG.property());
        LOG.info("DaDaPushNotificationProp.CONFIG=[{}]", projectConfigIndexes);
        for (String projectConfigIndex : projectConfigIndexes) {
            String projectKeyProperty = DaDaPushNotificationProp.CONFIG.property() + "." + projectConfigIndex + "." + DaDaPushNotificationProp.PROJECT.property();
            String projectKey = settings.getString(projectKeyProperty);
            if (projectKey == null) {
                throw MessageException.of("DaDaPush Notification configuration is corrupted. At least one project specific parameter has no project key. " +
                        "Contact your administrator to update this configuration in the global administration section of SonarQube.");
            }
            ProjectConfig value = ProjectConfig.create(settings, projectConfigIndex);
            LOG.info("Found project configuration [{}]", value);
            map.put(projectKey, value);
        }
        return map;
    }

    protected String logRelevantSettings() {
        Map<String, String> pluginSettings = new HashMap<>();
        mapSetting(pluginSettings, DaDaPushNotificationProp.BASE_PATH);
        mapSetting(pluginSettings, DaDaPushNotificationProp.ENABLED);
        mapSetting(pluginSettings, DaDaPushNotificationProp.CONFIG);
        return pluginSettings.toString() + "; project specific channel config: " + projectConfigMap;
    }

    private void mapSetting(Map<String, String> pluginSettings, DaDaPushNotificationProp key) {
        pluginSettings.put(key.name(), settings.getString(key.property()));
    }

    protected boolean shouldSkipSendingNotification(ProjectConfig projectConfig, QualityGate qualityGate) {
        // Disabled due to missing channel value
        if (projectConfig.getChannelToken() == null ||
                "".equals(projectConfig.getChannelToken().trim())) {
            LOG.info("DaDaPush Notification for project [{}] is blank, notifications disabled", projectConfig.getProjectKey());
            return true;
        }
        if (projectConfig.isQgFailOnly() && qualityGate != null && QualityGate.Status.OK.equals(qualityGate.getStatus())) {
            LOG.info("Project [{}] set up to send notification on failed Quality Gate, but was: {}", projectConfig.getProjectKey(), qualityGate.getStatus().name());
            return true;
        }
        return false;
    }
}
