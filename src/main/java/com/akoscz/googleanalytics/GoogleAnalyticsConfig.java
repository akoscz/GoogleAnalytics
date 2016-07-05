package com.akoscz.googleanalytics;

import lombok.Getter;
import lombok.Setter;

/**
 * Data class that holds the configuration parameters such as:
 *  - which endpoint we are connecting to
 *  - thread pool params
 *  - debug on/off.  Setting debug to true will change the endpoint param to the debug endpoint.
 */
public class GoogleAnalyticsConfig {

    public enum HttpMethod {
        POST,
        GET;
    }

    private static final String GA_ENDPOINT = "https://www.google-analytics.com/collect";
    private static final String GA_DEBUG_ENDPOINT = "https://www.google-analytics.com/debug/collect";
    private static final String GA_THREAD_NAME_FORMAT = "googleanalytics-thread-{0}";
    private static final int DEFAULT_MIN_THREADS = 1;
    private static final int DEFAULT_MAX_THREADS = 10;
    private static final int DEFAULT_QUEUE_SIZE = DEFAULT_MAX_THREADS * 100;
    private static final int DEFAULT_THREAD_TIMEOUT = 5;

    @Setter @Getter
    private String endpoint = GA_ENDPOINT;
    @Getter @Setter
    private int minThreads = DEFAULT_MIN_THREADS;
    @Getter @Setter
    private int maxThreads = DEFAULT_MAX_THREADS;
    @Getter @Setter
    private int queueSize = DEFAULT_QUEUE_SIZE;
    @Getter @Setter
    private int threadTimeout = DEFAULT_THREAD_TIMEOUT;
    @Setter @Getter
    private String threadNameFormat = GA_THREAD_NAME_FORMAT;
    @Setter @Getter
    private String proxyHost;
    @Setter @Getter
    private int proxyPort;
    @Setter @Getter
    private String proxyUserName;
    @Setter @Getter
    private String proxyPassword;
    @Setter @Getter
    private String userAgent;
    @Setter @Getter
    private HttpMethod httpMethod = HttpMethod.POST;

    @Getter
    private boolean debug;
    public void setDebug(boolean enableDebug) {
        debug = enableDebug;
        setEndpoint(debug ? GA_DEBUG_ENDPOINT: GA_ENDPOINT);
    }

    public boolean isHttpMethodGet() {
        return httpMethod == HttpMethod.GET;
    }
}
