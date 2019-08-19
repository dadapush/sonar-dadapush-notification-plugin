package com.dadapush.client.sonar;

import static com.dadapush.client.sonar.DaDaPushNotificationProp.CHANNEL_TOKEN;
import static com.dadapush.client.sonar.DaDaPushNotificationProp.CONFIG;
import static com.dadapush.client.sonar.DaDaPushNotificationProp.ENABLED;
import static com.dadapush.client.sonar.DaDaPushNotificationProp.BASE_PATH;
import static com.dadapush.client.sonar.DaDaPushNotificationProp.PROJECT;
import static com.dadapush.client.sonar.DaDaPushNotificationProp.QG_FAIL_ONLY;

import java.util.ArrayList;
import java.util.List;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;

public class DaDaPushPlugin implements Plugin {

  private static final String CATEGORY = "DaDaPush";
  private static final String SUBCATEGORY = "DaDaPush Notification";


  @Override
  public void define(Context context) {
    List<Object> extensions = new ArrayList<>();

    // The configurable properties
    addPluginPropertyDefinitions(extensions);

    // The actual plugin component(s)
    extensions.add(DaDaPushPostProjectAnalysisTask.class);

    context.addExtensions(extensions);
  }

  private void addPluginPropertyDefinitions(List<Object> extensions) {
    extensions.add(PropertyDefinition.builder(BASE_PATH.property())
        .name("DaDaPush Base Path")
        .description("default value: https://www.dadapush.com")
        .type(PropertyType.STRING)
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .defaultValue("https://www.dadapush.com")
        .index(0)
        .build());
    extensions.add(PropertyDefinition.builder(ENABLED.property())
        .name("Plugin enabled")
        .description("Are DaDaPush notifications enabled in general?")
        .defaultValue("false")
        .type(PropertyType.BOOLEAN)
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(1)
        .build());
    extensions.add(
        PropertyDefinition.builder(CONFIG.property())
            .name("Project specific configuration")
            .description(
                "Project specific configuration: Specify channel token and notification only on failing Qualilty Gate. "
                    +
                    "If a channel token is not configured for a project, no notification will be sent for project.")
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(2)
            .fields(
                PropertyFieldDefinition.build(PROJECT.property())
                    .name("Project Key")
                    .description(
                        "Ex: com.dadapush.client:sonar-dadapush-plugin, can use '*' wildcard at the end")
                    .type(PropertyType.STRING)
                    .build(),
                PropertyFieldDefinition.build(CHANNEL_TOKEN.property())
                    .name("Channel Token")
                    .description("create DaDaPush channel: https://www.dadapush.com/channel/list")
                    .type(PropertyType.STRING)
                    .build(),
                PropertyFieldDefinition.build(QG_FAIL_ONLY.property())
                    .name("Send on failed Quality Gate")
                    .description("Should notification be sent only if Quality Gate did not pass OK")
                    .type(PropertyType.BOOLEAN)
                    .build()
            )
            .build());
  }
}
