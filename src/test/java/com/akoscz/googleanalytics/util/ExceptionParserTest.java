package com.akoscz.googleanalytics.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

public class ExceptionParserTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDescription() {

        ExceptionParser exceptionParser = new ExceptionParser("com.akoscz");
        String threadName = Thread.currentThread().getName();

        RuntimeException exception = new RuntimeException("Test exception");

        String expectedDescription = "RuntimeException(\"Test exception\"); @ExceptionParserTest:testGetDescription:18; {" + threadName + "}";
        String description = exceptionParser.getDescription(threadName, exception);

        assertEquals(expectedDescription, description);
    }


    @Test
    public void testGetDescription_NullThrowable() {
        ExceptionParser exceptionParser = new ExceptionParser("com.akoscz");

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("throwable");

        exceptionParser.getDescription(null, null);
    }
}