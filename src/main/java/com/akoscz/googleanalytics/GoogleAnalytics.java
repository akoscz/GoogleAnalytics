package com.akoscz.googleanalytics;

import lombok.Builder;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * This class implements the GoogleAnalytics Measurement Protocol.
 * It provides support for a subset of the Measurement Protocol parameters.
 * See: https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
 */
@Log
@Builder(builderClassName = "Tracker", builderMethodName = "requiredParamsBuilder")
public class GoogleAnalytics {
    public enum HitType {
        pageview,
        screenview,
        event,
//    transaction,
//    item,
//    social,
//    exception,
//    timing;
    }

    private static final int PROTOCOL_VERSION = 1;
    private static final String ENCODING = "UTF-8";
    private static final Level DEFAULT_LOG_LEVEL = Level.SEVERE;

    private final ThreadPoolExecutor executor;
    private final CloseableHttpClient httpClient;

    @Getter
    private final GoogleAnalyticsConfig config;

    private ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();

    // *****************************
    // ********** GENERAL **********
    // *****************************

    /**
     * Required for all hit types.
     *
     * The Protocol version. The current value is '1'.
     * This will only change when there are changes made that are not backwards compatible.
     */
    @Getter
    private int protocolVersion;
    private static final String PROTOCOL_VERSION_KEY = "v";
    private GoogleAnalyticsParameter getProtocolVersionParam() {
        return GoogleAnalyticsParameter.of(PROTOCOL_VERSION_KEY, String.valueOf(protocolVersion));
    }

    /**
     * Required for all hit types.
     *
     * The tracking ID / web property ID. The format is UA-XXXX-Y.
     * All collected data is associated by this ID.
     */
    @Getter
    private String trackingId;
    private static final String TRACKING_ID_KEY = "tid";
    private GoogleAnalyticsParameter getTrackingIdParam() {
        if (trackingId == null || trackingId.isEmpty()) throw new IllegalArgumentException("'trackingId' cannot be null or empty!");
        if (!Pattern.matches("[U][A]-[0-9]+-[0-9]+", trackingId)) throw new IllegalArgumentException("Malformed trackingId: '" + trackingId + "'.  Expected: 'UA-[0-9]+-[0-9]+'");

        return GoogleAnalyticsParameter.of(TRACKING_ID_KEY, String.valueOf(trackingId));
    }

    /**
     * Optional.
     *
     * When present, the IP address of the sender will be anonymized.
     * For example, the IP will be anonymized if any of the following parameters are present in the payload: &aip=, &aip=0, or &aip=1
     */
    @Getter
    private Boolean anonymizeIP;
    private static final String ANONYIZE_IP_KEY = "aip";
    private GoogleAnalyticsParameter getAnonymizeIpParam() {
        if (anonymizeIP == null) return GoogleAnalyticsParameter.EMPTY;
        return GoogleAnalyticsParameter.of(ANONYIZE_IP_KEY, anonymizeIP ? "1" : "0");
    }

    /**
     * Optional.
     *
     * Indicates the data source of the hit.
     * For example:
     * hits sent from analytics.js will have data source set to 'web';
     * hits sent from one of the mobile SDKs will have data source set to 'app'.
     */
    @Getter
    private String dataSource;
    private static final String DATA_SOURCE_KEY = "ds";
    private GoogleAnalyticsParameter getDataSourceParam() {
        if (dataSource == null || dataSource.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        return GoogleAnalyticsParameter.of(DATA_SOURCE_KEY, dataSource);
    }

    /**
     * Optional.
     *
     * Used to send a random number in GET requests to ensure browsers and proxies don't cache hits.
     * It should be sent as the final parameter of the request since we've seen some 3rd party internet filtering software
     * add additional parameters to HTTP requests incorrectly.
     * This value is not used in reporting.
     */
    @Getter
    private Boolean cacheBuster;
    private static final String CACHE_BUSTER_KEY = "z";
    private GoogleAnalyticsParameter getCacheBusterParam() {
        if (cacheBuster == null || !cacheBuster) return GoogleAnalyticsParameter.EMPTY;
        return GoogleAnalyticsParameter.of(CACHE_BUSTER_KEY, String.valueOf(new Random().nextLong()));
    }

    // *****************************
    // *********** USER ************
    // *****************************

    /**
     * Required for all hit types.
     *
     * This anonymously identifies a particular user, device, or browser instance.
     * For the web, this is generally stored as a first-party cookie with a two-year expiration.
     * For mobile apps, this is randomly generated for each particular instance of an application install.
     * The value of this field should be a random UUID (version 4) as described in http://www.ietf.org/rfc/rfc4122.txt
     */
    @Getter
    private UUID clientId;
    private static final String CLIENT_ID_KEY = "cid";
    private GoogleAnalyticsParameter getClientIdParam() {
        if (clientId == null) throw new IllegalArgumentException("'clientId' cannot be null!");
        return GoogleAnalyticsParameter.of(CLIENT_ID_KEY, clientId.toString());
    }

    /**
     * Optional.
     *
     * This is intended to be a known identifier for a user provided by the site owner/tracking library user.
     * It must not itself be PII (personally identifiable information).
     * The value should never be persisted in GA cookies or other Analytics provided storage.
     */
    @Getter
    private String userId;
    private static final String USER_ID_KEY = "uid";
    private GoogleAnalyticsParameter getUserIdParam() {
        if (userId == null || userId.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        return GoogleAnalyticsParameter.of(USER_ID_KEY, userId);
    }

    // *****************************
    // ************ HIT ************
    // *****************************

    /**
     * Required for all hit types.
     *
     * The type of hit. Must be one of 'pageview', 'screenview', 'event'.
     * The following 'transaction', 'item', 'social', 'exception', 'timing' are not yet supported.
     */
    @Getter
    private HitType type;
    private static final String HIT_TYPE_KEY = "t";
    private GoogleAnalyticsParameter getHitTypeParam() {
        if (type == null) throw new IllegalArgumentException("'type' cannot be null!");
        return GoogleAnalyticsParameter.of(HIT_TYPE_KEY, type.name());
    }

    // *****************************
    // **** CONTENT INFORMATION ****
    // *****************************

    /**
     * Required for 'screenview' hit type.
     *
     * This parameter is optional on web properties, and required on mobile properties for screenview hits,
     * where it is used for the 'Screen Name' of the screenview hit.
     * On web properties this will default to the unique URL of the page by either using the &dl parameter as-is
     * or assembling it from &dh and &dp.
     */
    @Getter
    private String screenName;
    private static final String SCREEN_NAME_KEY = "cd";
    private GoogleAnalyticsParameter getScreenNameParam() {
        if (type == HitType.screenview && (screenName == null || screenName.isEmpty())) {
            throw new IllegalArgumentException("'screenName' cannot be null or empty when HitType.screenview is specified!");
        }
        if (screenName == null || screenName.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        if (screenName.getBytes().length > 2048) throw new RuntimeException("'screenName' cannot exceed 2048 bytes!");
        return GoogleAnalyticsParameter.of(SCREEN_NAME_KEY, screenName);
    }
    // *****************************
    // **** APPLICATION TRACKING ***
    // *****************************

    /**
     * Required for all hit types.
     *
     * Specifies the application name.
     */
    @Getter
    private String applicationName;
    private static final String APPLICATION_NAME_KEY = "an";
    private GoogleAnalyticsParameter getApplicationNameParam() {
        if (applicationName == null || applicationName.isEmpty()) throw new IllegalArgumentException("'applicationName' cannot be null or empty!");
        if (applicationName.getBytes().length > 100) throw new RuntimeException("'applicationName' cannot exceed 100 bytes!");
        return GoogleAnalyticsParameter.of(APPLICATION_NAME_KEY, applicationName);
    }

    /**
     * Optional.
     *
     * Specifies the application version.
     */
    @Getter
    private String applicationVersion;
    private static final String APPLICATION_VERSION_KEY = "av";
    private GoogleAnalyticsParameter getApplicationVersionParam() {
        if (applicationVersion == null || applicationVersion.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        if (applicationVersion.getBytes().length > 100) throw new RuntimeException("'applicationVersion' cannot exceed 100 bytes!");
        return GoogleAnalyticsParameter.of(APPLICATION_VERSION_KEY, applicationVersion);
    }

    /**
     * Optional.
     *
     * Application identifier.
     */
    @Getter
    private String applicationId;
    private static final String APPLICATION_ID_KEY = "aid";
    private GoogleAnalyticsParameter getApplicationIdParam() {
        if (applicationId == null || applicationId.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        if (applicationId.getBytes().length > 150) throw new RuntimeException("'applicationId' cannot exceed 150 bytes!");
        return GoogleAnalyticsParameter.of(APPLICATION_ID_KEY, applicationId);
    }

    // *****************************
    // ****** EVENT TRACKING  ******
    // *****************************

    /**
     * Required for 'event' hit type.
     *
     * Specifies the event category. Must not be empty.
     */
    @Getter
    private String category;
    private static final String CATEGORY_KEY = "ec";
    private GoogleAnalyticsParameter getCategoryParam() {
        if (type == HitType.event && (category == null || category.isEmpty())) {
            throw new IllegalArgumentException("event 'category' cannot be null or empty when HitType.event is specified!");
        }
        if (category == null || category.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        if (category.getBytes().length > 150) throw new RuntimeException("'category' cannot exceed 150 bytes!");
        return GoogleAnalyticsParameter.of(CATEGORY_KEY, category);
    }

    /**
     * Required for event hit type.
     *
     * Specifies the event action. Must not be empty.
     */
    @Getter
    private String action;
    private static final String ACTION_KEY = "ea";
    private GoogleAnalyticsParameter getActionParam() {
        if (type == HitType.event && (action == null || action.isEmpty())) {
            throw new IllegalArgumentException("event 'action' cannot be null or empty when HitType.event is specified!");
        }
        if (action == null || action.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        if (action.getBytes().length > 500) throw new RuntimeException("event 'action' cannot exceed 500 bytes!");
        return GoogleAnalyticsParameter.of(ACTION_KEY, action);
    }

    /**
     * Optional.
     *
     * Specifies the event label.
     */
    @Getter
    private String label;
    private static final String LABEL_KEY = "el";
    private GoogleAnalyticsParameter getLabelParam() {
        if (label == null || label.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        if (label.getBytes().length > 500) throw new RuntimeException("event 'label' cannot exceed 500 bytes!");
        return GoogleAnalyticsParameter.of(LABEL_KEY, label);
    }

    /**
     * Optional.
     *
     * Specifies the event value. Values must be non-negative.
     */
    @Getter
    private Integer value;
    private static final String VALUE_KEY = "ev";
    private GoogleAnalyticsParameter getValueParam() {
        if (value == null) return GoogleAnalyticsParameter.EMPTY;
        if (value < 0) throw new IllegalArgumentException("event 'value' cannot be negative!");
        return GoogleAnalyticsParameter.of(VALUE_KEY, value.toString());
    }

    // *****************************
    // *****************************
    // *****************************

    /**
     * Build a Tracker instance with default config values by which you can compose your GoogleAnalytics tracking request.
     * @param trackingId Required Valid Google Analytics Tracking Id.
     * @param clientId Required Valid Client Id UUID.
     * @param applicationName Required non-null non-empty Application Name.
     * @return A Google Analytics Tracker instance.
     */
    public static Tracker buildTracker(String trackingId, UUID clientId, String applicationName) {
        return GoogleAnalytics.buildTracker(trackingId, clientId, applicationName, null);
    }

    /**
     * Build a Tracker instance by which you can compose your GoogleAnalytics tracking request.
     * @param trackingId Required Valid Google Analytics Tracking Id.
     * @param clientId Required Valid Client Id UUID.
     * @param applicationName Required non-null non-empty Application Name.
     * @param config The configuration parameters for the tracker.  If null, default config values will be used.
     * @return A Google Analytics Tracker instance.
     */
    public static Tracker buildTracker(String trackingId, UUID clientId, String applicationName, GoogleAnalyticsConfig config) {
        if (config == null) config = new GoogleAnalyticsConfig();

        return requiredParamsBuilder()
                .config(config)
                .executor(GoogleAnalyticsThreadFactory.createExecutor(config))
                .httpClient(createHttpClient(config))
                .protocolVersion(PROTOCOL_VERSION)
                .trackingId(trackingId)
                .clientId(clientId)
                .applicationName(applicationName);
    }

    /**
     * Enable debug mode.
     *
     * By enabling debug mode, all network traffic will go to the debug endpoint.
     * Logging level will automatically be set to Level.ALL
     * @param enableDebug True to enable debug mode, False otherwise.
     */
    public void setDebug(boolean enableDebug) {
        config.setDebug(enableDebug);
        log.setLevel(Level.ALL);
    }

    /**
     * Set the Log Level for logging
     * @param logLevel A java.util.logging.LogLevel value.
     *                 Passing in null will reset the log level to the default, Level.SEVERE
     */
    public void setLogLevel(Level logLevel) {
        if (logLevel == null) {
            // reset to default
            logLevel = DEFAULT_LOG_LEVEL;
        }
        log.setLevel(logLevel);
    }

    /**
     * Send the parameters over the network to Google Analytics.
     * Note that this method will clear all the non-required parameters irregardless of success or failure
     * of the network request.
     * This method defaults to performing the network operation asynchronously.
     */
    public void send() {
        // default to send asynchronously
        send(true);
    }

    /**
     * Send the parameters over the network to Google Analytics.
     * Note that this method will clear all the non-required parameters irregardless of success or failure
     * of the network request.
     * @param asynchronous True to perform the network operation asynchronously, False otherwise.
     */
    public void send(boolean asynchronous) {

        if (config.isHttpMethodGet()) {
            final String url = buildUrlString();
            if (asynchronous) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        doGetNetworkOperation(url);
                    }
                });
            } else {
                doGetNetworkOperation(url);
            }
        } else { // POST method
            final ArrayList<NameValuePair> postParams = buildPostParams();

            if (asynchronous) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        doPostNetworkOperation(postParams);
                    }
                });
            } else {
                doPostNetworkOperation(postParams);
            }

        }

        // clear all non-required fields
        resetTracker();
    }

    protected void doGetNetworkOperation(String url) {
        log.info("executing on thread: " + Thread.currentThread().getName());

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", getUserAgent());

            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warning("Error requesting url: '" + url + "'. Response code: " + responseCode);
            } else {
                log.info("Successfully hit tracker: '" + url + "'");
            }

            if (config.isDebug()) {
                StringBuilder content = new StringBuilder();
                @Cleanup BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String line;
                // read from the urlconnection via the bufferedreader
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line + "\n");
                }
                log.info(content.toString());
            }

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void doPostNetworkOperation(ArrayList<NameValuePair> postParameters) {
        log.info("executing on thread: " + Thread.currentThread().getName());

        try {
            HttpPost httpPost = new HttpPost(config.getEndpoint());
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

            @Cleanup CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warning("Error posting to endpoint: '" + config.getEndpoint() + "'. Response code: '" + responseCode + "'\n" + httpPost.toString());
            } else {
                log.info("Successfully posted params to tracker: '" + postParameters + "'");
            }

            if (config.isDebug()) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                log.info(responseBody);
            }

            EntityUtils.consumeQuietly(httpResponse.getEntity());
        } catch (Exception e) {
            log.warning("Problem sending post request: " + e.toString());
        }
    }

    /**
     * Clear all non-required fields
     */
    private void resetTracker() {
        userId = null;
        category = null;
        action = null;
        label = null;
        value = null;
        type = null;
        applicationVersion = null;
        applicationId = null;
        screenName = null;
        dataSource = null;
        anonymizeIP = null;
        cacheBuster = null;
    }

    /**
     * Build the URL that will be used for the network request.
     * @return The URL string containing the query params of all available parameters.
     */
    public String buildUrlString() {
        if (type == null) throw new IllegalArgumentException("Missing HitType. 'type' cannot be null!");

        String urlString = new CustomStringBuilder()
                .append(config.getEndpoint())
                .append(getProtocolVersionParam().toStringWithPrefix("?"))    // protocol version
                .append(getAnonymizeIpParam().toStringWithPrefix("&"))  // anonymize IP
                .append(getDataSourceParam().toStringWithPrefix("&"))   // data source
                .append(getTrackingIdParam().toStringWithPrefix("&"))  // tracking id
                .append(getClientIdParam().toStringWithPrefix("&"))  // client id
                .append(getUserIdParam().toStringWithPrefix("&"))  // user id
                .append(getCategoryParam().toStringWithPrefix("&"))   // event category
                .append(getActionParam().toStringWithPrefix("&"))   // event action
                .append(getLabelParam().toStringWithPrefix("&"))   // event label
                .append(getValueParam().toStringWithPrefix("&"))   // event value
                .append(getHitTypeParam().toStringWithPrefix("&"))    // event type
                .append(getApplicationNameParam().toStringWithPrefix("&"))   // application name
                .append(getApplicationVersionParam().toStringWithPrefix("&"))   // application version
                .append(getApplicationIdParam().toStringWithPrefix("&"))  // application id
                .append(getScreenNameParam().toStringWithPrefix("&"))   // screen name
                .append(getCacheBusterParam().toStringWithPrefix("&"))    // cache buster
                .toString();

        if (urlString.getBytes().length > 8000) {
            throw new RuntimeException("URL string length must not exceed 8000 bytes!");
        }

        return urlString;
    }

    public ArrayList<NameValuePair> buildPostParams() {
        postParameters.clear();

        postParameters.add(getProtocolVersionParam());
        postParameters.add(getAnonymizeIpParam());
        postParameters.add(getDataSourceParam());
        postParameters.add(getTrackingIdParam());
        postParameters.add(getClientIdParam());
        postParameters.add(getUserIdParam());
        postParameters.add(getCacheBusterParam());
        postParameters.add(getActionParam());
        postParameters.add(getLabelParam());
        postParameters.add(getValueParam());
        postParameters.add(getHitTypeParam());
        postParameters.add(getApplicationNameParam());
        postParameters.add(getApplicationVersionParam());
        postParameters.add(getScreenNameParam());
        postParameters.add(getUserIdParam());

        return postParameters;
    }

    /**
     * A custom string builder that does not append null or empty character sequences
     */
    class CustomStringBuilder {
        StringBuilder builder;

        CustomStringBuilder() {
            builder = new StringBuilder();
        }

        public CustomStringBuilder append(CharSequence charSequence) {
            // do not append null or empty character sequences
            if (charSequence != null && charSequence.length() >= 0) {
                builder.append(charSequence);
            }
            return this;
        }

        public String toString() {
            return builder.toString();
        }

    }

    /* package private */
    private String getUserAgent() {
        return new UserAgent(getApplicationName(), getApplicationVersion()).toString();
    }

    private static CloseableHttpClient createHttpClient(GoogleAnalyticsConfig config) {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(config.getMaxThreads());
        connManager.setMaxTotal(config.getMaxThreads());
        HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);

        if (StringUtils.isNotEmpty(config.getUserAgent())) {
            builder.setUserAgent(config.getUserAgent());
        }

        if (StringUtils.isNotEmpty(config.getProxyHost())) {
            builder.setProxy(new HttpHost(config.getProxyHost(), config.getProxyPort()));

            if (StringUtils.isNotEmpty(config.getProxyUserName())) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(config.getProxyHost(), config.getProxyPort()),
                        new UsernamePasswordCredentials(config.getProxyUserName(), config.getProxyPassword()));
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }

        return builder.build();
    }
}