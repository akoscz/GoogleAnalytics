package com.akoscz.googleanalytics;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class GoogleAnalyticsParameterTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void toStringWithPrefix_EMPTY() {
        assertEquals(GoogleAnalyticsParameter.EMPTY, GoogleAnalyticsParameter.of(null, null));
        assertNull(GoogleAnalyticsParameter.EMPTY.toStringWithPrefix("blah"));
    }

    @Test
    public void toStringWithPrefix_NullAndEmptyValue() {
        assertNull(GoogleAnalyticsParameter.of("key", null).toStringWithPrefix("blah"));
        assertNull(GoogleAnalyticsParameter.of("key", "").toStringWithPrefix("blah"));
    }

    @Test
    public void toStringWithPrefix() {
        assertEquals("blahkey=value", GoogleAnalyticsParameter.of("key", "value").toStringWithPrefix("blah"));
    }

}
