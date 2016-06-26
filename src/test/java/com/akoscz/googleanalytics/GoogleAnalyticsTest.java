package com.akoscz.googleanalytics;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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
        tracker = GoogleAnalytics.buildTracker(trackingId, clientId, applicationName)
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
        assertEquals("https://www.google-analytics.com/collect", googleAnalytics.getEndpoint());

        // ensure url is built with proper query params and values
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "tid", trackingId);
        assertQueryParam(url, "cid", String.valueOf(clientId));
        assertQueryParam(url, "an", applicationName);
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
    }

    @Test
    public void testTrackingId_Malformed() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Malformed trackingId: 'XXX-111-222'.  Expected: 'UA-[0-9]+-[0-9]+'");
        // ensure proper format
        tracker.trackingId("XXX-111-222").build().buildUrlString();
    }

    @Test
    public void testTrackingId_NullOrEmpty() throws Exception {
        thrown.expectMessage("'trackingId' cannot be null or empty!");
        // ensure that empty string is not allowed
        tracker.trackingId("").build().buildUrlString();
        // ensure that null is not allowed
        tracker.trackingId(null).build().buildUrlString();
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
    }

    @Test
    public void testClientId_NullOrEmpty() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'clientId' cannot be null!");
        // ensure that null is not allowed
        tracker.clientId(null).build().buildUrlString();
    }

    @Test
    public void testApplicationName() throws Exception {
        GoogleAnalytics googleAnalytics = tracker.applicationName("blah").build();
        // ensure setter works
        assertEquals("blah", googleAnalytics.getApplicationName());

        // ensure url is built with proper query param and value
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "an", "blah");
    }

    @Test
    public void testApplicationName_NullOrEmpty() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'applicationName' cannot be null or empty!");
        // ensure empty string is not allowed
        tracker.applicationName("").build().buildUrlString();
        // ensure null is not allowed
        tracker.applicationName(null).build().buildUrlString();
    }

    @Test
    public void testHitType() throws Exception {
        GoogleAnalytics googleAnalytics = tracker.type(GoogleAnalytics.HitType.pageview).build();
        // ensure setter works
        assertEquals(GoogleAnalytics.HitType.pageview, googleAnalytics.getType());

        // ensure url is built with proper query param and value
        String url = tracker.build().buildUrlString();
        assertQueryParam(url, "t", String.valueOf(GoogleAnalytics.HitType.pageview));
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

        // ensure the setter works
        tracker.anonymizeIP(false);
        googleAnalytics = tracker.build();
        assertFalse(googleAnalytics.getAnonymizeIP());

        // ensure url is built with proper query param and value
        url = googleAnalytics.buildUrlString();
        assertQueryParam(url, "aip", "0");
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
    }

    @Test
    public void testDataSource_NullOrEmpty() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'clientId' cannot be null!");
        // ensure that null is not allowed
        tracker.clientId(null).build().buildUrlString();
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
                    .build();

        GoogleAnalytics spyGoogleAnalytics = spy(googleAnalytics);
        // mock out doNetworkOperation method so that we do not hit the network
        doNothing().when(spyGoogleAnalytics).doNetworkOperation(anyString());

        // perform synchronous send so that we do not spin up a worker thread in a test method
        spyGoogleAnalytics.send(false);

        // verify that after we called send(), all non-required params are reset
        assertNull("'userId' should have been reset.", spyGoogleAnalytics.getUserId());
        assertNull("'category' should have been reset.", spyGoogleAnalytics.getCategory());
        assertNull("'action' should have been reset.", spyGoogleAnalytics.getAction());
        assertNull("'label' should have been reset.", spyGoogleAnalytics.getLabel());
        assertNull("'value' should have been reset.", spyGoogleAnalytics.getValue());
        assertNull("'type' should have been reset.", spyGoogleAnalytics.getType());
        assertNull("'applicationVersion' should have been reset.", spyGoogleAnalytics.getApplicationVersion());
        assertNull("'applicationId' should have been reset.", spyGoogleAnalytics.getApplicationId());
        assertNull("'screenName' should have been reset.", spyGoogleAnalytics.getScreenName());
        assertNull("'dataSource' should have been reset.", spyGoogleAnalytics.getDataSource());
        assertNull("'anonymizeIP' should have been reset.", spyGoogleAnalytics.getAnonymizeIP());
        assertNull("'cacheBuster' should have been reset.", spyGoogleAnalytics.getCacheBuster());

        // verify that required params are retained
        assertEquals(trackingId, spyGoogleAnalytics.getTrackingId());
        assertEquals(clientId, spyGoogleAnalytics.getClientId());
        assertEquals(applicationName, spyGoogleAnalytics.getApplicationName());
        assertEquals(1, spyGoogleAnalytics.getProtocolVersion());
        assertEquals("https://www.google-analytics.com/collect", googleAnalytics.getEndpoint());
    }

    /***********************/
    /** TEST UTIL METHODS **/
    /***********************/

    private void assertQueryParam(String url, String key, String expectedValue) throws UnsupportedEncodingException, MalformedURLException {
        Map<String, String> params = splitQuery(url);
        String actualValue = params.get(key);
        assertEquals(expectedValue, actualValue);
    }

    private void assertQueryParamContainsKeyWithNonEmptyValue(String url, String key) throws UnsupportedEncodingException, MalformedURLException {
        Map<String, String> params = splitQuery(url);
        assertTrue("expected '" + key + "' query param but it does not exist. url=" + url, params.containsKey(key));

        String actualValue = params.get(key);
        assertNotNull("'" + key + "' query param cannot be null. url=" + url, actualValue);

        assertTrue("'" + key + "' query param cannot be empty. url=" + url, !actualValue.isEmpty());
    }

    public static Map<String, String> splitQuery(String urlString) throws UnsupportedEncodingException, MalformedURLException {
        URL url = new URL(urlString);
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
}