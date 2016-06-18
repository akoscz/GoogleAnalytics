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

    private void assertQueryParam(String url, String key, String expectedValue) throws UnsupportedEncodingException, MalformedURLException {
        Map<String, String> params = splitQuery(url);
        String actualValue = params.get(key);
        assertEquals(actualValue, expectedValue);
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