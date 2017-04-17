# GoogleAnalytics #
[![Build Status](https://travis-ci.org/akoscz/GoogleAnalytics.svg?branch=master)](https://travis-ci.org/akoscz/GoogleAnalytics) [![Code Coverage](https://img.shields.io/codecov/c/github/akoscz/GoogleAnalytics/master.svg)](https://codecov.io/github/akoscz/GoogleAnalytics?branch=master)


A quick and simple *limited feature* implemenetation of the Google Analytics [Measurement Protocol](https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide) API in Java.

This [GoogleAnalytics.java](src/main/java/com/akoscz/googleanalytics/GoogleAnalytics.java) implementation relies on the [Lombok](https://projectlombok.org/index.html) code generation annotation library to generate a [Builder](https://projectlombok.org/features/Builder.html) class to allow for composing the fields of a GoogleAnalytics request.

**Notes:** 
* The `/collect` and `/debug/collect` endpoints are supported.
* The `/batch` endpoit is not supported.
* Both `POST` and `GET` http request types are availabe.  `POST` is the default.
* To enable debug mode use `GoogleAnalytics.setDebug(true)`. It will update the endpoint to `/debug/collect` and set logging level to `Level.ALL` for verbose logging.
* To control the logging level, use `GoogleAnalytics.setLogLevel(Level)`.  The default logging level is `Level.SEVERE`.
* Invoking the `GoogleAnalytics.send()` method will perform the network I/O asynchronously by spinning up a new Thread for doing the work.
* For sychronous operation, use `GoogleAnalytics.send(false)` which will perform the network I/O on the thread it was invoked from.
* All non-required parameters are cleared from the Tracker irregardless of success or failure of the network I/O when `GoogleAnalytics.send()` is invoked.
* The following hit types are currently supported:
    * pageview
    * screenview
    * event

*Usage example:*

    String trackingId = "UA-12345677-12";
    UUID clientId = UUID.randomUUID();
    String appName = "My Application"
    
    GoogleAnalytics.Tracker tracker  = GoogleAnalytics.buildTracker(trackingId, clientId, appName);
    
    tracker.type(GoogleAnalytics.HitType.event)
            .category("application")
            .action("startup")
            .build()
            .send();

# License #

* [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
