# GoogleAnalytics

A quick 'limited feature' implemenetation of the Google Analytics [Measurement Protocol](https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide) API in Java.

Note: Only the `/collect` endpoint is supported by means of a `GET` request.

This [GoogleAnalytics.java](src/main/java/com/akoscz/googleanalytics/GoogleAnalytics.java) implementation relies on the [Lombok](https://projectlombok.org/index.html) code generation annotation library to generate a [Builder](https://projectlombok.org/features/Builder.html) class to allow for composing the fields of a GoogleAnalytics request.


Usage example:

    String trackingId = "UA-12345677-12";
    UUID clientId = UUID.randomUUID();
    String appName = "My Application"
    
    GoogleAnalytics.Tracker tracker  = GoogleAnalytics.buildTracker(trackingId, clientId, appName);
    
    tracker.type(GoogleAnalytics.HitType.event)
            .category("application")
            .action("startup")
            .build()
            .send();
