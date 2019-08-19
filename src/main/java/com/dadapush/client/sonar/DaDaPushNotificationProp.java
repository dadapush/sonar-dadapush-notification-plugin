package com.dadapush.client.sonar;

public enum DaDaPushNotificationProp {

    BASE_PATH("dadapush.basePath"),
    /**
     * Is this plugin enabled in general?
     * Per project notification sending depends on this and a project specific configuration existing.
     */
    ENABLED("dadapush.enabled"),

    CONFIG("dadapush.projectconfig"),
    /**
     * @see DaDaPushNotificationProp#CONFIG
     */
    PROJECT("project"),
    /**
     * @see DaDaPushNotificationProp#CONFIG
     */
    CHANNEL_TOKEN("channelToken"),
    /**
     * @see DaDaPushNotificationProp#CONFIG
     */
    QG_FAIL_ONLY("qg");

    private String property;


    DaDaPushNotificationProp(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }
}
