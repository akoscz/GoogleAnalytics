package com.akoscz.googleanalytics;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.regex.Pattern;

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

    private static final String GA_ENDPOINT = "https://www.google-analytics.com/collect";
    private static final int PROTOCOL_VERSION = 1;
    private static final String ENCODING = "UTF-8";

    @Getter
    private String endpoint;

    @Getter
    private int protocolVersion;
    private String v() {
        return "v=" + protocolVersion;
    }

    // *****************************
    // ****** REQUIRED PARAMS ******
    // *****************************

    @Getter
    private String trackingId;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String tid() {
        if (trackingId == null || trackingId.isEmpty()) throw new IllegalArgumentException("'trackingId' cannot be null or empty!");
        if (!Pattern.matches("[U][A]-[0-9]+-[0-9]+", trackingId)) throw new IllegalArgumentException("Malformed trackingId: '" + trackingId + "'.  Expected: 'UA-[0-9]+-[0-9]+'");

        return "&tid=" + URLEncoder.encode(trackingId, ENCODING);
    }

    @Getter
    private UUID clientId;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String cid() {
        if (clientId == null) throw new IllegalArgumentException("'clientId' cannot be null!");
        return "&cid=" + URLEncoder.encode(clientId.toString(), ENCODING);
    }

    @Getter
    private String userId;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String uid() {
        if (userId == null || userId.isEmpty()) return null;
        return "&uid=" + URLEncoder.encode(userId, ENCODING);
    }

    // *****************************
    // ****** EVENT TRACKING  ******
    // *****************************

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

    @Getter
    private String label;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String el() {
        if (label == null || label.isEmpty()) return null;
        if (label.getBytes().length > 500) throw new RuntimeException("event 'label' cannot exceed 500 bytes!");
        return "&el=" + URLEncoder.encode(label, ENCODING);
    }

    @Getter
    private Integer value;
    private String ev() {
        if (value == null) return null;
        if (value < 0) throw new IllegalArgumentException("event 'value' cannot be negative!");
        return "&ev=" + value;
    }

    // *****************************
    // ************ HIT ************
    // *****************************

    @Getter
    private HitType type;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String t() {
        if (type == null) throw new IllegalArgumentException("'type' cannot be null!");
        return "&t=" + URLEncoder.encode(type.name(), ENCODING);
    }

    // *****************************
    // **** APPLICATION TRACKING ***
    // *****************************

    @Getter
    private String applicationName;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String an() {
        if (applicationName == null || applicationName.isEmpty()) throw new IllegalArgumentException("'applicationName' cannot be null or empty!");
        if (applicationName.getBytes().length > 100) throw new RuntimeException("'applicationName' cannot exceed 100 bytes!");
        return "&an=" + URLEncoder.encode(applicationName, ENCODING);
    }

    @Getter
    private String applicationVersion;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String av() {
        if (applicationVersion == null || applicationVersion.isEmpty()) return null;
        if (applicationVersion.getBytes().length > 100) throw new RuntimeException("'applicationVersion' cannot exceed 100 bytes!");
        return "&av=" + URLEncoder.encode(applicationVersion, ENCODING);
    }

    @Getter
    private String applicationId;
    @SneakyThrows(UnsupportedEncodingException.class)
    private String aid() {
        if (applicationId == null || applicationId.isEmpty()) return null;
        if (applicationId.getBytes().length > 150) throw new RuntimeException("'applicationId' cannot exceed 150 bytes!");
        return "&aid=" + URLEncoder.encode(applicationId, ENCODING);
    }

    // *****************************
    // **** CONTENT INFORMATION ****
    // *****************************

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
    // *****************************
    // *****************************

    public static Tracker buildTracker(String trackingId, UUID clientId, String applicationName) {
        return requiredParamsBuilder()
                .endpoint(GA_ENDPOINT)
                .protocolVersion(PROTOCOL_VERSION)
                .trackingId(trackingId)
                .clientId(clientId)
                .applicationName(applicationName);
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doNetworkOperation(url);
                }
            }).start();
        } else {
            doNetworkOperation(url);
        }

        // clear all non-required fields
        resetTracker();
    }

    private void doNetworkOperation(String url) {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", getUserAgent());

            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error requesting url '" + url + "', received response code " + responseCode);
            } else {
                System.out.println("Successfully hit tracker: " + url);
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
     * Clear all the non-required fields
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
    }

    /**
     * Build the URL that will be used for the network request.
     * @return The URL string containing the query params of all available parameters.
     */
    public String buildUrlString() {
        if (type == null) throw new IllegalArgumentException("Missing HitType. 'type' cannot be null!");

        String urlString = new CustomStringBuilder().append(endpoint)
                    .append("?")
                    .append(v())
                    .append(tid())
                    .append(cid())
                    .append(uid())
                    .append(ec())
                    .append(ea())
                    .append(el())
                    .append(ev())
                    .append(t())
                    .append(an())
                    .append(av())
                    .append(aid())
                    .append(cd())
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

    private String getUserAgent() {
        return new UserAgent(getApplicationName(), getApplicationVersion()).toString();
    }
}