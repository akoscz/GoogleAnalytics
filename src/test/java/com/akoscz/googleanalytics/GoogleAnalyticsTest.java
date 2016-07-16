package com.akoscz.googleanalytics;

import com.akoscz.googleanalytics.util.UserAgent;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

public class GoogleAnalyticsTest {
    @Rule
    public ExpectedException thrown= ExpectedException.none();

    public final String trackingId = "UA-12345-123";
    public final UUID clientId = UUID.randomUUID();
    public final String applicationName = "Test Application";

    private GoogleAnalytics.Tracker tracker;

    @Before
    public void beforeTest() {
        GoogleAnalyticsConfig config = new GoogleAnalyticsConfig();
        config.setHttpMethod(GoogleAnalyticsConfig.HttpMethod.GET);
        config.setUserAgent(String.valueOf(new UserAgent(applicationName, "testVersion")));

        tracker = GoogleAnalytics.buildTracker(trackingId, clientId, applicationName, config)
            .type(GoogleAnalytics.HitType.pageview);
    }

    @Test
    public void testRequiredParams() throws Exception {
        // ensure that tracker is constructed with required params
        GoogleAnalytics googleAnalytics = tracker.build();
        assertEquals(trackingId, googleAnalytics.getTrackingId());
        assertEquals(clientId, googleAnalytics.getClientId());
        assertEquals(applicationName, googleAnalytics.getApplicationName());
        assertEquals(1, googleAnalytics.getProtocolVersion());
        assertEquals("https://www.google-analytics.com/collect", googleAnalytics.getConfig().getEndpoint());

        // ensure url is built with proper query params and values
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "tid", trackingId);
        assertQueryParam(url, "cid", String.valueOf(clientId));
        assertQueryParam(url, "an", applicationName);

        // ensure that required field appear in the post params
        List<GoogleAnalyticsParameter> postParams = tracker.build().buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("tid", trackingId)));
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("cid", String.valueOf(clientId))));
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("an", applicationName)));
    }

    @Test
    public void testTrackingId() throws Exception {
        String testTrackingId = "UA-111-222";
        GoogleAnalytics googleAnalytics = tracker.trackingId(testTrackingId).build();
        // ensure setter works
        assertEquals(testTrackingId, googleAnalytics.getTrackingId());

        // ensure url is built with proper query param and value
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "tid", testTrackingId);

        List<GoogleAnalyticsParameter> postParams = tracker.build().buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("tid", testTrackingId)));
    }

    @Test
    public void testTrackingId_Malformed() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Malformed trackingId: 'XXX-111-222'.  Expected: 'UA-[0-9]+-[0-9]+'");
        // ensure proper format
        tracker.trackingId("XXX-111-222").build().buildUrlString();
    }

    @Test
    public void testTrackingId_Null() throws Exception {
        thrown.expectMessage("'trackingId' cannot be null or empty!");
        // ensure that null is not allowed
        tracker.trackingId(null).build().buildUrlString();
    }

    @Test
    public void testTrackingId_Empty() throws Exception {
        thrown.expectMessage("'trackingId' cannot be null or empty!");
        // ensure that empty string is not allowed
        tracker.trackingId("").build().buildUrlString();
    }

    @Test
    public void testClientId() throws Exception {
        UUID testClientId = UUID.fromString("1e9d9268-a57a-45f5-8e5e-dfc02dc8548c");
        GoogleAnalytics googleAnalytics = tracker.clientId(testClientId).build();
        // ensure setter works
        assertEquals(testClientId, googleAnalytics.getClientId());

        // ensure url is built with proper query param and value
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "cid", String.valueOf(testClientId));

        List<GoogleAnalyticsParameter> postParams = tracker.build().buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("cid", String.valueOf(testClientId))));
    }

    @Test
    public void testClientId_Null() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'clientId' cannot be null!");
        // ensure that null is not allowed
        tracker.clientId(null).build().buildUrlString();
    }

    @Test
    public void testUserId() throws Exception {
        String testUserId = "testUserId";
        GoogleAnalytics googleAnalytics = tracker.userId(testUserId).build();
        // ensure setter works
        assertEquals(testUserId, googleAnalytics.getUserId());

        // ensure url is built with proper query param and value
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "uid", testUserId);

        List<GoogleAnalyticsParameter> postParams = tracker.build().buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("uid", testUserId)));
    }

    @Test
    public void testUserId_NullAndEmpty() throws Exception {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getUserId());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "uid");

        // set a value and verify it
        googleAnalytics = tracker.userId("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "uid", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.userId("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "uid");

        // set a value and verify it
        googleAnalytics = tracker.userId("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "uid", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.userId(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "uid");
    }


    @Test
    public void testApplicationName() throws Exception {
        GoogleAnalytics googleAnalytics = tracker.applicationName("blah").build();
        // ensure setter works
        assertEquals("blah", googleAnalytics.getApplicationName());

        // ensure url is built with proper query param and value
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "an", "blah");

        List<GoogleAnalyticsParameter> postParams = tracker.build().buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("an", "blah")));
    }

    @Test
    public void testApplicationName_Null() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'applicationName' cannot be null or empty!");
        // ensure null is not allowed
        tracker.applicationName(null).build().buildUrlString();
    }

    @Test
    public void testApplicationName_Empty() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'applicationName' cannot be null or empty!");
        // ensure empty string is not allowed
        tracker.applicationName("").build().buildUrlString();
    }

    @Test
    public void testApplicationName_MaxLength() throws Exception {
        int maxLength = 100;
        String testAppName = StringUtils.repeat("a", maxLength + 1);
        GoogleAnalytics googleAnalytics = tracker.applicationName(testAppName).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("'applicationName' cannot exceed 100 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testHitType() throws Exception {
        GoogleAnalytics googleAnalytics = tracker.type(GoogleAnalytics.HitType.pageview).build();
        // ensure setter works
        assertEquals(GoogleAnalytics.HitType.pageview, googleAnalytics.getType());

        // ensure url is built with proper query param and value
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "t", String.valueOf(GoogleAnalytics.HitType.pageview));

        List<GoogleAnalyticsParameter> postParams = tracker.build().buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("t", String.valueOf(GoogleAnalytics.HitType.pageview))));
    }

    @Test
    public void testHitType_Missing() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing HitType. 'type' cannot be null!");
        // ensure null is not allowed
        tracker.type(null).build().buildUrlString();
    }

    @Test
    public void testMaxUrlLength() throws Exception {
        String longString = String.format("%1$"+8000+ "s", "");

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("URL string length must not exceed 8000 bytes!");
        tracker.userId(longString).build().buildUrlString();
    }

    @Test
    public void testAnonymiseIP() throws UnsupportedEncodingException, MalformedURLException {
        // ensure the setter works
        tracker.anonymizeIP(true);
        GoogleAnalytics googleAnalytics = tracker.build();
        assertTrue(googleAnalytics.getAnonymizeIP());

        // ensure url is built with proper query param and value
        String url = googleAnalytics.buildUrlString();
        assertQueryParam(url, "aip", "1");

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("aip", "1")));

        // ensure the setter works
        tracker.anonymizeIP(false);
        googleAnalytics = tracker.build();
        assertFalse(googleAnalytics.getAnonymizeIP());

        // ensure url is built with proper query param and value
        url = googleAnalytics.buildUrlString();
        assertQueryParam(url, "aip", "0");

        postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("aip", "0")));
    }

    @Test
    public void testAnonymiseIP_Null() {
        // ensure the setter works
        tracker.anonymizeIP(null);
        GoogleAnalytics googleAnalytics = tracker.build();

        assertNull(googleAnalytics.getAnonymizeIP());

        // ensure url is built with proper query param and value
        String url = googleAnalytics.buildUrlString();
        assertTrue("url should not contain 'aip' query parameter", !url.contains("&aip"));
    }

    @Test
    public void testDataSource() throws Exception {
        String expectedDataSource = "test datasource";
        GoogleAnalytics googleAnalytics = tracker.dataSource(expectedDataSource).build();
        // ensure setter works
        assertEquals(expectedDataSource, googleAnalytics.getDataSource());

        // ensure url is built with proper query param and value
        String url = googleAnalytics.buildUrlString();
        assertQueryParam(url, "ds", expectedDataSource);

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("ds", expectedDataSource)));
    }

    @Test
    public void testDataSource_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getDataSource());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ds");

        // set a value and verify it
        googleAnalytics = tracker.dataSource("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "ds", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.dataSource("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ds");


        // set a value and verify it
        googleAnalytics = tracker.dataSource("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "ds", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.dataSource(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ds");
    }

    @Test
    public void testCacheBuster() throws UnsupportedEncodingException, MalformedURLException {
        // ensure the setter works
        tracker.cacheBuster(true);
        GoogleAnalytics googleAnalytics = tracker.build();
        assertTrue(googleAnalytics.getCacheBuster());

        // ensure url is built with proper query param and value
        String url = googleAnalytics.buildUrlString();
        assertQueryParamContainsKeyWithNonEmptyValue(url, "z");

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        // ensure that cache buster param never appears in the post params
        for (NameValuePair param : postParams) {
            if("z".equals(param.getName())) {
                fail("cache buster param should not appear in post params!");
            }
        }

        // ensure the setter works
        tracker.cacheBuster(false);
        googleAnalytics = tracker.build();
        assertFalse(googleAnalytics.getCacheBuster());

        // ensure url is built with proper query param and value
        url = googleAnalytics.buildUrlString();
        assertQueryParam(url, "z", null);

    }

    @Test
    public void testCacheBuster_Null() {
        // ensure the setter works
        tracker.cacheBuster(null);
        GoogleAnalytics googleAnalytics = tracker.build();

        assertNull(googleAnalytics.getCacheBuster());

        // ensure url is built with proper query param and value
        String url = googleAnalytics.buildUrlString();
        assertTrue("url should not contain 'z' query parameter", !url.contains("&z"));
    }

    @Test
    public void testScreeName() throws UnsupportedEncodingException, MalformedURLException {
        String testScreenName = "testScreenName";
        GoogleAnalytics googleAnalytics = tracker.screenName(testScreenName).build();

        assertEquals(testScreenName, googleAnalytics.getScreenName());
        assertQueryParamContainsKeyWithNonEmptyValue(googleAnalytics.buildUrlString(), "cd");
        assertQueryParam(googleAnalytics.buildUrlString(), "cd", testScreenName);

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("cd", testScreenName)));
    }

    @Test
    public void testScreeName_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getScreenName());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "cd");

        // set a value and verify it
        googleAnalytics = tracker.screenName("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "cd", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.screenName("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "cd");

        // set a value and verify it
        googleAnalytics = tracker.screenName("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "cd", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.screenName(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "cd");
    }

    @Test
    public void testScreeName_Null() throws UnsupportedEncodingException, MalformedURLException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'screenName' cannot be null or empty when HitType.screenview is specified!");
        tracker.screenName(null)
                .type(GoogleAnalytics.HitType.screenview)
                .build()
                .buildUrlString();
    }

    @Test
    public void testScreeName_Empty() throws UnsupportedEncodingException, MalformedURLException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'screenName' cannot be null or empty when HitType.screenview is specified!");
        tracker.screenName("")
                .type(GoogleAnalytics.HitType.screenview)
                .build()
                .buildUrlString();
    }

    @Test
    public void testScreeName_MaxLength() throws UnsupportedEncodingException, MalformedURLException {
        int maxScreenNameLength = 2048;
        String testScreenName = StringUtils.repeat("a", maxScreenNameLength + 1);

        GoogleAnalytics googleAnalytics = tracker.screenName(testScreenName).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("'screenName' cannot exceed 2048 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testCategory() throws UnsupportedEncodingException, MalformedURLException {
        String testCategory= "testCategory";
        GoogleAnalytics googleAnalytics = tracker.category(testCategory).build();

        assertEquals(testCategory, googleAnalytics.getCategory());
        assertQueryParamContainsKeyWithNonEmptyValue(googleAnalytics.buildUrlString(), "ec");
        assertQueryParam(googleAnalytics.buildUrlString(), "ec", testCategory);

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("ec", testCategory)));
    }

    @Test
    public void testCategory_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getCategory());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ec");

        // set a value and verify it
        googleAnalytics = tracker.category("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "ec", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.category("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ec");

        // set a value and verify it
        googleAnalytics = tracker.category("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "ec", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.category(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ec");
    }

    @Test
    public void testCategory_Null() throws UnsupportedEncodingException, MalformedURLException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("event 'category' cannot be null or empty when HitType.event is specified!");
        tracker.category(null)
                .type(GoogleAnalytics.HitType.event)
                .build()
                .buildUrlString();
    }

    @Test
    public void testCategory_Empty() throws UnsupportedEncodingException, MalformedURLException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("event 'category' cannot be null or empty when HitType.event is specified!");
        tracker.category("")
                .type(GoogleAnalytics.HitType.event)
                .build()
                .buildUrlString();
    }

    @Test
    public void testCategory_MaxLength() throws UnsupportedEncodingException, MalformedURLException {
        int maxCategoryLength = 150;
        String testCategory = StringUtils.repeat("a", maxCategoryLength + 1);

        GoogleAnalytics googleAnalytics = tracker.category(testCategory).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("'category' cannot exceed 150 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testAction() throws UnsupportedEncodingException, MalformedURLException {
        String testAction = "testAction";
        GoogleAnalytics googleAnalytics = tracker.action(testAction).build();

        assertEquals(testAction, googleAnalytics.getAction());
        assertQueryParamContainsKeyWithNonEmptyValue(googleAnalytics.buildUrlString(), "ea");
        assertQueryParam(googleAnalytics.buildUrlString(), "ea", testAction);

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("ea", testAction)));
    }

    @Test
    public void testAction_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getAction());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "el");

        // set a value and verify it
        googleAnalytics = tracker.action("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "ea", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.action("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ea");

        // set a value and verify it
        googleAnalytics = tracker.action("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "ea", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.action(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "ea");
    }

    @Test
    public void testAction_Null() throws UnsupportedEncodingException, MalformedURLException {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("event 'action' cannot be null or empty when HitType.event is specified!");
        tracker.action(null)
                .type(GoogleAnalytics.HitType.event)
                .category("blah")
                .build()
                .buildUrlString();
    }

    @Test
    public void testAction_Empty() throws UnsupportedEncodingException, MalformedURLException {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("event 'action' cannot be null or empty when HitType.event is specified!");
        tracker.action("")
                .type(GoogleAnalytics.HitType.event)
                .category("blah")
                .build()
                .buildUrlString();
    }

    @Test
    public void testAction_MaxLength() throws UnsupportedEncodingException, MalformedURLException {
        int maxActionLength = 500;
        String testAction = StringUtils.repeat("a", maxActionLength + 1);

        GoogleAnalytics googleAnalytics = tracker.action(testAction).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("event 'action' cannot exceed 500 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testLabel() throws UnsupportedEncodingException, MalformedURLException {
        String testLabel = "testLabel";
        GoogleAnalytics googleAnalytics = tracker.label(testLabel).build();

        assertEquals(testLabel, googleAnalytics.getLabel());
        assertQueryParamContainsKeyWithNonEmptyValue(googleAnalytics.buildUrlString(), "el");
        assertQueryParam(googleAnalytics.buildUrlString(), "el", testLabel);

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("el", testLabel)));
    }

    @Test
    public void testLabel_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getLabel());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "el");

        // set a value and verify it
        googleAnalytics = tracker.label("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "el", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.label("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "el");

        // set a value and verify it
        googleAnalytics = tracker.label("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "el", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.label(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "el");
    }

    @Test
    public void testLabel_MaxLength() throws UnsupportedEncodingException, MalformedURLException {
        int maxLabelLength = 500;
        String testLabel = StringUtils.repeat("a", maxLabelLength + 1);

        GoogleAnalytics googleAnalytics = tracker.label(testLabel).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("event 'label' cannot exceed 500 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testValue() throws UnsupportedEncodingException, MalformedURLException {
        int expectedValue = 10;
        GoogleAnalytics googleAnalytics = tracker.value(expectedValue).build();

        assertEquals(expectedValue, googleAnalytics.getValue().intValue());
        assertQueryParamContainsKeyWithNonEmptyValue(googleAnalytics.buildUrlString(), "ev");
        assertQueryParam(googleAnalytics.buildUrlString(), "ev", String.valueOf(expectedValue));

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("ev", String.valueOf(expectedValue))));
    }

    @Test
    public void testValue_Negative() throws UnsupportedEncodingException, MalformedURLException {
        int expectedNegativeValue = -10;
        GoogleAnalytics googleAnalytics = tracker.value(expectedNegativeValue).build();
        assertEquals(expectedNegativeValue, googleAnalytics.getValue().intValue());
        // it's a bummer that we do not have value validators in lombock's builder.
        // the negative illegal value exception is only caught when we build the url
        thrown.expectMessage("event 'value' cannot be negative!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testApplicationVersion_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getApplicationVersion());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "av");

        // set a value and verify it
        googleAnalytics = tracker.applicationVersion("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "av", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.applicationVersion("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "av");

        // set a value and verify it
        googleAnalytics = tracker.applicationVersion("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "av", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.applicationVersion(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "av");
    }

    @Test
    public void testApplicationVersion_MaxLength() throws UnsupportedEncodingException, MalformedURLException {
        int maxLength = 100;
        String testAppversion = StringUtils.repeat("a", maxLength + 1);

        GoogleAnalytics googleAnalytics = tracker.applicationVersion(testAppversion).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("'applicationVersion' cannot exceed 100 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testApplicationId_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getApplicationId());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "aid");

        // set a value and verify it
        googleAnalytics = tracker.applicationId("blah").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "aid", "blah");

        // clear out the value with an empty string
        googleAnalytics = tracker.applicationId("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "aid");

        // set a value and verify it
        googleAnalytics = tracker.applicationId("blah2").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "aid", "blah2");

        // clear out the value with an empty string
        googleAnalytics = tracker.applicationId(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "aid");
    }

    @Test
    public void testApplicationId_MaxLength() throws UnsupportedEncodingException, MalformedURLException {
        int maxLength = 150;
        String testAppversionId = StringUtils.repeat("a", maxLength + 1);

        GoogleAnalytics googleAnalytics = tracker.applicationId(testAppversionId).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("'applicationId' cannot exceed 150 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testExceptionDescription_NullAndEmpty() throws UnsupportedEncodingException, MalformedURLException {
        GoogleAnalytics googleAnalytics = tracker.type(GoogleAnalytics.HitType.exception).build();
        // ensure that its initialized to null
        assertNull(googleAnalytics.getExceptionDescription());
        // ensure we start off with absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "exd");

        // set a value and verify it
        googleAnalytics = tracker.exceptionDescription("BlahException").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "exd", "BlahException");

        // clear out the value with an empty string
        googleAnalytics = tracker.exceptionDescription("").build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "exd");

        // set a value and verify it
        googleAnalytics = tracker.exceptionDescription("Blah2Exception").build();
        assertQueryParam(googleAnalytics.buildUrlString(), "exd", "Blah2Exception");

        // clear out the value with an empty string
        googleAnalytics = tracker.exceptionDescription(null).build();
        // verify absent query param
        assertQueryParamAbsent(googleAnalytics.buildUrlString(), "exd");
    }

    @Test
    public void testExceptionDescription_MaxLength() throws UnsupportedEncodingException, MalformedURLException {
        int maxLength = 150;
        String testExDescription = StringUtils.repeat("e", maxLength + 1);

        GoogleAnalytics googleAnalytics = tracker.type(GoogleAnalytics.HitType.exception).exceptionDescription(testExDescription).build();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("'exceptionDescription' cannot exceed 150 bytes!");
        googleAnalytics.buildUrlString();
    }

    @Test
    public void testIsExceptionFatal() throws UnsupportedEncodingException, MalformedURLException {
        // ensure the default is true
        GoogleAnalytics googleAnalytics = tracker.type(GoogleAnalytics.HitType.exception).build();
        assertTrue("'isFatalException' default should have been 'true'", googleAnalytics.getIsExceptionFatal());

        // ensure url is built with proper query param and value
        String url = googleAnalytics.buildUrlString();
        assertQueryParam(url, "exf", "1");

        List<GoogleAnalyticsParameter> postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("exf", "1")));

        // ensure the setter works
        tracker.isExceptionFatal(false);
        googleAnalytics = tracker.build();
        assertFalse(googleAnalytics.getIsExceptionFatal());

        // ensure url is built with proper query param and value
        url = googleAnalytics.buildUrlString();
        assertQueryParam(url, "exf", "0");

        postParams = googleAnalytics.buildPostParams();
        assertTrue(postParams.contains(GoogleAnalyticsParameter.of("exf", "0")));

        googleAnalytics.resetTracker();
        googleAnalytics = tracker.build();
        // ensure the default is returned to true
        assertTrue("'isFatalException' default should have been 'true'", googleAnalytics.getIsExceptionFatal());
    }

    @Test
    public void testIsExceptionFatal_Null() {
        // ensure the setter works
        tracker.anonymizeIP(null);
        GoogleAnalytics googleAnalytics = tracker.build();

        assertNull(googleAnalytics.getAnonymizeIP());

        // ensure url is built with proper query param and value
        String url = googleAnalytics.buildUrlString();
        assertTrue("url should not contain 'aip' query parameter", !url.contains("&aip"));
    }

    @Test
    public void testSend_resetTracker() {
        GoogleAnalytics googleAnalytics = tracker
                    .userId("testUserId")
                    .category("testCategory")
                    .action("testAction")
                    .label("testLabel")
                    .value(1000)
                    .type(GoogleAnalytics.HitType.event)
                    .applicationVersion("testVersion")
                    .applicationId("testId")
                    .screenName("testScreen")
                    .dataSource("testDataSource")
                    .anonymizeIP(true)
                    .cacheBuster(true)
                    .exceptionDescription("Test Exception Description")
                    .isExceptionFatal(false)
                    .build();

        GoogleAnalytics spyGoogleAnalytics = spy(googleAnalytics);
        // mock out doGetNetworkOperation method so that we do not hit the network
        doNothing().when(spyGoogleAnalytics).doGetNetworkOperation(anyString());

        // perform synchronous send so that we do not spin up a worker thread in a test method
        spyGoogleAnalytics.send(false);

        googleAnalytics = spyGoogleAnalytics.getGlobalTracker().build();
        // verify that after we called send(), all non-required params are reset
        assertNull("'userId' should have been reset.", googleAnalytics.getUserId());
        assertNull("'category' should have been reset.", googleAnalytics.getCategory());
        assertNull("'action' should have been reset.", googleAnalytics.getAction());
        assertNull("'label' should have been reset.", googleAnalytics.getLabel());
        assertNull("'value' should have been reset.", googleAnalytics.getValue());
        assertNull("'type' should have been reset.", googleAnalytics.getType());
        assertNull("'applicationVersion' should have been reset.", googleAnalytics.getApplicationVersion());
        assertNull("'applicationId' should have been reset.", googleAnalytics.getApplicationId());
        assertNull("'screenName' should have been reset.", googleAnalytics.getScreenName());
        assertNull("'dataSource' should have been reset.", googleAnalytics.getDataSource());
        assertNull("'anonymizeIP' should have been reset.", googleAnalytics.getAnonymizeIP());
        assertNull("'cacheBuster' should have been reset.", googleAnalytics.getCacheBuster());
        assertNull("'exceptionDescription' should have been reset.", googleAnalytics.getExceptionDescription());
        assertTrue("'isFatalException' should have been reset", googleAnalytics.getIsExceptionFatal());

        // verify that required params are retained
        assertEquals(trackingId, googleAnalytics.getTrackingId());
        assertEquals(clientId, googleAnalytics.getClientId());
        assertEquals(applicationName, googleAnalytics.getApplicationName());
        assertEquals(1, googleAnalytics.getProtocolVersion());
        assertEquals("https://www.google-analytics.com/collect", googleAnalytics.getConfig().getEndpoint());
    }

    @Test
    public void testBuildUrl() {
        GoogleAnalytics googleAnalytics = tracker
                .userId("testUserId")
                .category("testCategory")
                .action("testAction")
                .label("testLabel")
                .value(1000)
                .type(GoogleAnalytics.HitType.event)
                .applicationVersion("testVersion")
                .applicationId("testId")
                .screenName("testScreen")
                .dataSource("testDataSource")
                .anonymizeIP(true)
                .build();

        StringBuilder expectedUrlStringBuilder = new StringBuilder()
                .append("https://www.google-analytics.com/collect")
                .append("?v=1") // protocol version
                .append("&aip=1") // anonymize IP
                .append("&ds=testDataSource") // data source
                .append("&tid=UA-12345-123") // tracking id
                .append("&cid=" + clientId) // client id
                .append("&uid=testUserId") // user id
                .append("&ec=testCategory") // event category
                .append("&ea=testAction") // event action
                .append("&el=testLabel") // event label
                .append("&ev=1000") // event value
                .append("&t=event") // event type
                .append("&an=Test+Application") // application name
                .append("&av=testVersion") // application version
                .append("&aid=testId") // application id
                .append("&cd=testScreen"); // screen name

        String actualUrlString = googleAnalytics.buildUrlString();
        assertEquals(actualUrlString, expectedUrlStringBuilder.toString());
    }

    @Test
    public void testSetDebug() {
        GoogleAnalytics googleAnalytics = tracker.build();
        googleAnalytics.setDebug(true);

        assertTrue(googleAnalytics.getConfig().isDebug());
        assertEquals("https://www.google-analytics.com/debug/collect", googleAnalytics.getConfig().getEndpoint());
    }

    @Test
    public void testSetLogger() {
        GoogleAnalytics googleAnalytics = tracker.build();
        Level expectedLevel = Level.FINE;
        googleAnalytics.setLogLevel(expectedLevel);

        // ensure that the log level is what we expect
        assertEquals(expectedLevel, Logger.getLogger(BaseAnalytics.class.getName()).getLevel());

        googleAnalytics.setDebug(true);
        // ensure the enabling debug changes the log level to ALL
        assertEquals(Level.ALL, Logger.getLogger(BaseAnalytics.class.getName()).getLevel());

        googleAnalytics.setLogLevel(null);
        assertEquals(Level.SEVERE, Logger.getLogger(BaseAnalytics.class.getName()).getLevel());

        expectedLevel = Level.WARNING;
        tracker.build().setLogLevel(expectedLevel);

        // ensure that the log level is what we expect
        assertEquals(expectedLevel, Logger.getLogger(BaseAnalytics.class.getName()).getLevel());

        tracker.build().setDebug(true);
        // ensure the enabling debug changes the log level to ALL
        assertEquals(Level.ALL, Logger.getLogger(BaseAnalytics.class.getName()).getLevel());

        tracker.build().setLogLevel(null);
        assertEquals(Level.SEVERE, Logger.getLogger(BaseAnalytics.class.getName()).getLevel());
    }

    @Test
    public void testPostParams_MaxLength() {
        List<GoogleAnalyticsParameter> postParams = tracker.build().buildPostParams();

        int bytesCount = 0;
        for (NameValuePair postParam : postParams) {
            bytesCount += ((GoogleAnalyticsParameter)postParam).countBytes();
        }
        // account for the separator bytes
        bytesCount += postParams.size() - 1;

        final int maxPostBodySize = 8192;
        // calculate the number of bytes we need to reach the max limit.
        // we subtract 4 for "&ds=" since that will added for us by the GoogleAnalyticsParameter.toString()
        int fluffCount = maxPostBodySize - bytesCount - 4 /* &ds= */;
        // create a string long enough to reach the max post body size
        String fluff = StringUtils.repeat("f", fluffCount);

        // add the fluff into the dataSource field "ds={fluff}", and get the updated post parameters
        postParams = tracker.dataSource(fluff).build().buildPostParams();

        GoogleAnalyticsParameter fluffParam = GoogleAnalyticsParameter.EMPTY;
        bytesCount = 0;
        // count the bytes again
        for (NameValuePair postParam : postParams) {
            bytesCount += ((GoogleAnalyticsParameter)postParam).countBytes();
            if (postParam.getName().equals("ds"))
                fluffParam = (GoogleAnalyticsParameter) postParam;
        }
        // account for the separator bytes
        bytesCount += postParams.size() - 1;

        assertEquals(fluffCount, fluffParam.countBytes() - 3 /* ds= */);
        assertEquals(maxPostBodySize, bytesCount);

        // now bump up the fluff by 1 and expect a failure
        fluff = StringUtils.repeat("f", fluffCount + 1);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Post data parameters must not exceed 8192 bytes!");
        tracker.dataSource(fluff).build().buildPostParams();
    }

    @Test
    public void testNetworkToLocanhost() {
        // TODO: inject a mock httpClient
        // GoogleAnalyticsConfig config = tracker.build().getConfig();
        // config.setEndpoint("http://localhost:8080");
        // config.setHttpMethod(GoogleAnalyticsConfig.HttpMethod.POST);
        // GoogleAnalytics googleAnalytics = tracker.config(config).build();
        // googleAnalytics.setDebug(true);
        // googleAnalytics.setLogLevel(Level.ALL);
        // tracker.build().send(false);
    }

    /***********************/
    /** TEST UTIL METHODS **/
    /***********************/

    private void assertQueryParam(String url, String key, String expectedValue) {
        Map<String, String> params = splitQuery(url);
        String actualValue = params.get(key);
        assertEquals(expectedValue, actualValue);
    }

    private void assertQueryParamContainsKeyWithNonEmptyValue(String url, String key) {
        Map<String, String> params = splitQuery(url);
        assertTrue("expected '" + key + "' query param but it does not exist. url=" + url, params.containsKey(key));

        String actualValue = params.get(key);
        assertNotNull("'" + key + "' query param cannot be null. url=" + url, actualValue);

        assertTrue("'" + key + "' query param cannot be empty. url=" + url, !actualValue.isEmpty());
    }

    private void assertQueryParamAbsent(String url, String key) {
        Map<String, String> params = splitQuery(url);
        assertFalse("expected '" + key + "' query param to be absent from url=" + url, params.containsKey(key));
    }

    public static Map<String, String> splitQuery(String urlString) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        try {

            URL url = new URL(urlString);
            String query = url.getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
        } catch (Exception ex) {
            fail("failed in splitQuery(String url): " + ex.toString() + "\nurl='"  + urlString + "'");
        }
        return query_pairs;
    }
}