package com.akoscz.googleanalytics;

import com.akoscz.googleanalytics.util.NonNullOrEmptyStringBuilder;
import lombok.Builder;
import lombok.Getter;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * This class implements the GoogleAnalytics Measurement Protocol.
 * It provides support for a subset of the Measurement Protocol parameters.
 * See: https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
 */
@Builder(builderClassName = "Tracker", builderMethodName = "trackerBuilder")
public class GoogleAnalytics extends BaseAnalytics {

    public enum HitType {
        pageview,
        screenview,
        event,
        // transaction,
        // item,
        // social,
        exception,
        // timing;
    }

    @Getter
    private GoogleAnalyticsConfig config;


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
    // ******** EXCEPTIONS *********
    // *****************************

    /**
     * Optional.
     *
     * Specifies the description of an exception.
     */
    @Getter
    private String exceptionDescription;
    private static final String EX_DESCRIPTION_KEY = "exd";
    private GoogleAnalyticsParameter getExceptionDescriptionParam() {
        if (type != HitType.exception || exceptionDescription == null || exceptionDescription.isEmpty()) return GoogleAnalyticsParameter.EMPTY;
        if (exceptionDescription.getBytes().length > 150) throw new RuntimeException("event 'exceptionDescription' cannot exceed 150 bytes!");
        return GoogleAnalyticsParameter.of(EX_DESCRIPTION_KEY, exceptionDescription);
    }

    /**
     * Optional.
     *
     * Specifies whether the exception was fatal.  Default is TRUE.
     */
    @Getter
    private Boolean isExceptionFatal = true;
    private static final String EX_FATAL_KEY = "exf";
    private GoogleAnalyticsParameter getIsExceptionFatalParam() {
        if (type != HitType.exception || isExceptionFatal == null) return GoogleAnalyticsParameter.EMPTY;
        return GoogleAnalyticsParameter.of(EX_FATAL_KEY, isExceptionFatal ? "1" : "0");
    }

    // *****************************
    // *****************************
    // *****************************

    /**
     * Ensure that the Measurement Protocol required parameters are valid and rules are enforced.
     * https://developers.google.com/analytics/devguides/collection/protocol/v1/reference#required
     */
    /* package */ void validateRequiredParams() {
        if (clientId == null)
            throw new IllegalArgumentException("'clientId' cannot be null!");

        if (applicationName == null || applicationName.isEmpty())
            throw new IllegalArgumentException("'applicationName' cannot be null or empty!");

        if (trackingId == null || trackingId.isEmpty())
            throw new IllegalArgumentException("'trackingId' cannot be null or empty!");

        if (!Pattern.matches("[U][A]-[0-9]+-[0-9]+", trackingId))
            throw new IllegalArgumentException("Malformed trackingId: '" + trackingId + "'.  Expected: 'UA-[0-9]+-[0-9]+'");

        if (type == null)
            throw new IllegalArgumentException("Missing HitType. 'type' cannot be null!");

        if (type == HitType.event && (category == null || category.isEmpty()))
            throw new IllegalArgumentException("event 'category' cannot be null or empty when HitType.event is specified!");

        if (type == HitType.event && (action == null || action.isEmpty()))
            throw new IllegalArgumentException("event 'action' cannot be null or empty when HitType.event is specified!");

        if (type == HitType.screenview && (screenName == null || screenName.isEmpty()))
            throw new IllegalArgumentException("'screenName' cannot be null or empty when HitType.screenview is specified!");
    }

    /**
     * Build the URL that will be used for the GET network request.
     * @return The URL string containing the query params of all available parameters.
     */
    /* package */ String buildUrlString() {
        validateRequiredParams();

        String urlString = new NonNullOrEmptyStringBuilder()
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
                .append(getExceptionDescriptionParam().toStringWithPrefix("&")) // exception description
                .append(getIsExceptionFatalParam().toStringWithPrefix("&")) // exception fatal flag
                .toString();

        if (urlString.getBytes().length > 8000) {
            throw new RuntimeException("URL string length must not exceed 8000 bytes!");
        }

        return urlString;
    }

    /**
     * Build the list of parameters that will be used for the POST request.
     * @return The list of non empty GoogleAnalyticsParameter's
     */
    /* package */ List<GoogleAnalyticsParameter> buildPostParams() {
        postParameters.clear();

        validateRequiredParams();

        postParameters.add(getProtocolVersionParam());
        postParameters.add(getAnonymizeIpParam());
        postParameters.add(getDataSourceParam());
        postParameters.add(getTrackingIdParam());
        postParameters.add(getClientIdParam());
        postParameters.add(getCategoryParam());
        postParameters.add(getUserIdParam());
        postParameters.add(getActionParam());
        postParameters.add(getLabelParam());
        postParameters.add(getValueParam());
        postParameters.add(getHitTypeParam());
        postParameters.add(getApplicationNameParam());
        postParameters.add(getApplicationVersionParam());
        postParameters.add(getScreenNameParam());
        postParameters.add(getUserIdParam());
        postParameters.add(getExceptionDescriptionParam());
        postParameters.add(getIsExceptionFatalParam());

        // remove empty parameters
        postParameters.removeAll(Collections.singleton(GoogleAnalyticsParameter.EMPTY));

        // count the bytes
        int bytesCount = 0;
        for (GoogleAnalyticsParameter postParam : postParameters) {
            bytesCount += postParam.countBytes();
        }
        // account for the separator bytes at the end of each key=value pair, except the very last one
        bytesCount += postParameters.size() - 1;

        if (bytesCount > 8192) {
            throw new RuntimeException("Post data parameters must not exceed 8192 bytes!");
        }

        return postParameters;
    }

    /**
     * Clear all non-required fields and reset default values where applicable
     */
    /* package */ void resetTracker() {
        globalTracker
                .userId(null)
                .category(null)
                .action(null)
                .label(null)
                .value(null)
                .type(null)
                .applicationVersion(null)
                .applicationId(null)
                .screenName(null)
                .dataSource(null)
                .anonymizeIP(null)
                .cacheBuster(null)
                .exceptionDescription(null)
                .isExceptionFatal(true);
    }

}