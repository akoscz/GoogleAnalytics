package com.akoscz.googleanalytics.util;

import com.akoscz.googleanalytics.GoogleAnalytics;
import lombok.NonNull;

/**
 * The ExceptionReport is an UncaughtExceptionHandler which when encountering an uncaught exception will then send a
 * HitType.exception to Google Analytics.  The exception description is generated by the ExceptionParse class.
 * All uncaught exceptions are considered FATAL.
 * The network operation for reporting this event is performed on the same thread that the UncaughtExceptionHandler
 * is running on.  This is to ensure that event is sent before we hand off further handling of the uncaught exception
 * to the original handler if one exists.
 */
public class ExceptionReporter implements Thread.UncaughtExceptionHandler {

    private final GoogleAnalytics.Tracker tracker;
    private final Thread.UncaughtExceptionHandler originalHandler;
    private final ExceptionParser exceptionParser;

    public ExceptionReporter(@NonNull GoogleAnalytics.Tracker tracker, Thread.UncaughtExceptionHandler originalHandler, String... packages) {
        if (tracker == null) throw new IllegalArgumentException("'tracker' cannot be null");

        this.tracker = tracker;
        this.originalHandler = originalHandler;
        this.exceptionParser = new ExceptionParser(packages);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String description = exceptionParser.getDescription(t.getName(), e);
        tracker.type(GoogleAnalytics.HitType.exception)
                .exceptionDescription(description)
                .isExceptionFatal(true)
                .build()
                // fire off the network event synchronously
                .send(false);

        if (originalHandler != null) {
            originalHandler.uncaughtException(t, e);
        }
    }
}
