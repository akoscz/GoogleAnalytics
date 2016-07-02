package com.akoscz.googleanalytics;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
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

    @Getter
    private final GoogleAnalyticsConfig config;

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
    private String v() {
        return "v=" + protocolVersion;
    }


    /**
     * Required for all hit types.
     *
     * The tracking ID / web property ID. The format is UA-XXXX-Y.
     * All collected data is associated by this ID.
     */
    @Getter
    private String trackingId;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String tid() {
        if (trackingId == null || trackingId.isEmpty()) throw new IllegalArgumentException("'trackingId' cannot be null or empty!");
        if (!Pattern.matches("[U][A]-[0-9]+-[0-9]+", trackingId)) throw new IllegalArgumentException("Malformed trackingId: '" + trackingId + "'.  Expected: 'UA-[0-9]+-[0-9]+'");

        return "&tid=" + URLEncoder.encode(trackingId, ENCODING);
    }

    /**
     * Optional.
     *
     * When present, the IP address of the sender will be anonymized.
     * For example, the IP will be anonymized if any of the following parameters are present in the payload: &aip=, &aip=0, or &aip=1
     */
    @Getter
    private Boolean anonymizeIP;
    private String aip() {
        if (anonymizeIP == null) return null;
        return "&aip=" + (anonymizeIP ? "1" : "0");
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
    @SneakyThrows(UnsupportedEncodingException.class)
    private String ds() {
        if (dataSource == null || dataSource.isEmpty()) return null;
        return "&ds=" + URLEncoder.encode(dataSource, ENCODING);
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
    private String z() {
        if (cacheBuster == null || !cacheBuster) return null;
        return "&z=" + new Random().nextLong();
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
    @SneakyThrows(UnsupportedEncodingException.class)
    private String cid() {
        if (clientId == null) throw new IllegalArgumentException("'clientId' cannot be null!");
        return "&cid=" + URLEncoder.encode(clientId.toString(), ENCODING);
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
    @SneakyThrows(UnsupportedEncodingException.class)
    private String uid() {
        if (userId == null || userId.isEmpty()) return null;
        return "&uid=" + URLEncoder.encode(userId, ENCODING);
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
    @SneakyThrows(UnsupportedEncodingException.class)
    private String t() {
        if (type == null) throw new IllegalArgumentException("'type' cannot be null!");
        return "&t=" + URLEncoder.encode(type.name(), ENCODING);
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
    @SneakyThrows(UnsupportedEncodingException.class)
    private String cd() {
        if (type == HitType.screenview && (screenName == null || screenName.isEmpty())) {
            throw new IllegalArgumentException("'screenName' cannot be null or empty when HitType.screenview is specified!");
        }
        if (screenName == null || screenName.isEmpty()) return null;
        if (screenName.getBytes().length > 2048) throw new RuntimeException("'screenName' cannot exceed 2048 bytes!");
        return "&cd=" + URLEncoder.encode(screenName, ENCODING);
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
    @SneakyThrows(UnsupportedEncodingException.class)
    private String an() {
        if (applicationName == null || applicationName.isEmpty()) throw new IllegalArgumentException("'applicationName' cannot be null or empty!");
        if (applicationName.getBytes().length > 100) throw new RuntimeException("'applicationName' cannot exceed 100 bytes!");
        return "&an=" + URLEncoder.encode(applicationName, ENCODING);
    }

    /**
     * Optional.
     *
     * Specifies the application version.
     */
    @Getter
    private String applicationVersion;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String av() {
        if (applicationVersion == null || applicationVersion.isEmpty()) return null;
        if (applicationVersion.getBytes().length > 100) throw new RuntimeException("'applicationVersion' cannot exceed 100 bytes!");
        return "&av=" + URLEncoder.encode(applicationVersion, ENCODING);
    }

    /**
     * Optional.
     *
     * Application identifier.
     */
    @Getter
    private String applicationId;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String aid() {
        if (applicationId == null || applicationId.isEmpty()) return null;
        if (applicationId.getBytes().length > 150) throw new RuntimeException("'applicationId' cannot exceed 150 bytes!");
        return "&aid=" + URLEncoder.encode(applicationId, ENCODING);
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
    @SneakyThrows(UnsupportedEncodingException.class)
    private String ec() {
        if (type == HitType.event && (category == null || category.isEmpty())) {
            throw new IllegalArgumentException("event 'category' cannot be null or empty when HitType.event is specified!");
        }
        if (category == null || category.isEmpty()) return null;
        if (category.getBytes().length > 150) throw new RuntimeException("'category' cannot exceed 150 bytes!");
        return "&ec=" + URLEncoder.encode(category, ENCODING);
    }

    /**
     * Required for event hit type.
     *
     * Specifies the event action. Must not be empty.
     */
    @Getter
    private String action;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String ea() {
        if (type == HitType.event && (action == null || action.isEmpty())) {
            throw new IllegalArgumentException("event 'action' cannot be null or empty when HitType.event is specified!");
        }
        if (action == null || action.isEmpty()) return null;
        if (action.getBytes().length > 500) throw new RuntimeException("event 'action' cannot exceed 500 bytes!");
        return "&ea=" + URLEncoder.encode(action, ENCODING);
    }

    /**
     * Optional.
     *
     * Specifies the event label.
     */
    @Getter
    private String label;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String el() {
        if (label == null || label.isEmpty()) return null;
        if (label.getBytes().length > 500) throw new RuntimeException("event 'label' cannot exceed 500 bytes!");
        return "&el=" + URLEncoder.encode(label, ENCODING);
    }

    /**
     * Optional.
     *
     * Specifies the event value. Values must be non-negative.
     */
    @Getter
    private Integer value;
    private String ev() {
        if (value == null) return null;
        if (value < 0) throw new IllegalArgumentException("event 'value' cannot be negative!");
        return "&ev=" + value;
    }

    // *****************************
    // *****************************
    // *****************************

    /**
     * Build a Tracker instance by which you can compose your GoogleAnalytics tracking request.
     * @param trackingId Required Valid Google Analytics Tracking Id.
     * @param clientId Required Valid Client Id UUID.
     * @param applicationName Required non-null non-empty Application Name.
     * @return A Google Analytics Tracker instance.
     */
    public static Tracker buildTracker(String trackingId, UUID clientId, String applicationName) {
        final GoogleAnalyticsConfig config = new GoogleAnalyticsConfig();
        return requiredParamsBuilder()
                .config(config)
                .executor(GoogleAnalyticsThreadFactory.createExecutor(config))
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

        final String url = buildUrlString();

        if (asynchronous) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    doNetworkOperation(url);
                }
            });
        } else {
            doNetworkOperation(url);
        }

        // clear all non-required fields
        resetTracker();
    }

    protected void doNetworkOperation(String url) {
        log.info("executing on thread: " + Thread.currentThread().getName());

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
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
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String line;
                // read from the urlconnection via the bufferedreader
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line + "\n");
                }
                bufferedReader.close();

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
                .append("?")
                .append(v())    // protocol version
                .append(aip())  // anonymize IP
                .append(ds())   // data source
                .append(tid())  // tracking id
                .append(cid())  // client id
                .append(uid())  // user id
                .append(ec())   // event category
                .append(ea())   // event action
                .append(el())   // event label
                .append(ev())   // event value
                .append(t())    // event type
                .append(an())   // application name
                .append(av())   // application version
                .append(aid())  // application id
                .append(cd())   // screen name
                .append(z())    // cache buster
                .toString();

        if (urlString.getBytes().length > 8000) {
            throw new RuntimeException("URL string length must not exceed 8000 bytes!");
        }

        return urlString;
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
    String getUserAgent() {
        return new UserAgent(getApplicationName(), getApplicationVersion()).toString();
    }
}