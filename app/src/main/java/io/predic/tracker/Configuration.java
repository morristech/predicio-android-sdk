package io.predic.tracker;

class Configuration {
    private Configuration() {}

    public static final int SECOND = 1000;
    public static final int MINUTE = SECOND * 60;
    public static final int HOUR = MINUTE * 60;
    public static final int DAY = HOUR * 24;

    public static final int INTERVAL_TRACKING_LOCATION = MINUTE;
    public static final int INTERVAL_TRACKING_IDENTITY = HOUR * 6;
    public static final int INTERVAL_TRACKING_APPS = DAY;
}
