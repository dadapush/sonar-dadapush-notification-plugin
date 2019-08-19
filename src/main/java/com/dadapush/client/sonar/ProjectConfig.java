package com.dadapush.client.sonar;

import java.util.Objects;
import org.sonar.api.config.Settings;

public class ProjectConfig {
    private final String projectKey;
    private final String channelToken;
    private final boolean qgFailOnly;

    public ProjectConfig(String projectKey, String channelToken, boolean qgFailOnly) {
        this.projectKey = projectKey;
        this.channelToken = channelToken;
        this.qgFailOnly = qgFailOnly;
    }

    /**
     * Cloning constructor
     *
     * @param c
     */
    public ProjectConfig(ProjectConfig c) {
        this.projectKey = c.getProjectKey();
        this.channelToken = c.getChannelToken();
        this.qgFailOnly = c.isQgFailOnly();
    }

    static ProjectConfig create(Settings settings, String configurationId) {
        String configurationPrefix = DaDaPushNotificationProp.CONFIG.property() + "." + configurationId + ".";
        String projectKey = settings.getString(configurationPrefix + DaDaPushNotificationProp.PROJECT.property());
        String channelToken = settings.getString(configurationPrefix + DaDaPushNotificationProp.CHANNEL_TOKEN
            .property());
        boolean qgFailOnly = settings.getBoolean(configurationPrefix + DaDaPushNotificationProp.QG_FAIL_ONLY.property());
        return new ProjectConfig(projectKey, channelToken, qgFailOnly);
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getChannelToken() {
        return channelToken;
    }

    public boolean isQgFailOnly() {
        return qgFailOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectConfig that = (ProjectConfig) o;
        return qgFailOnly == that.qgFailOnly &&
                Objects.equals(projectKey, that.projectKey) &&
                Objects.equals(channelToken, that.channelToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectKey, channelToken, qgFailOnly);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ProjectConfig{");
        sb.append("projectKey='").append(projectKey).append('\'');
        sb.append(", channelToken='").append(channelToken).append('\'');
        sb.append(", qgFailOnly=").append(qgFailOnly);
        sb.append('}');
        return sb.toString();
    }
}
