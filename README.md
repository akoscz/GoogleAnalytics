# GoogleAnalytics

A quick and simple *limited feature* implemenetation of the Google Analytics [Measurement Protocol](https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide) API in Java.

This [GoogleAnalytics.java](src/main/java/com/akoscz/googleanalytics/GoogleAnalytics.java) implementation relies on the [Lombok](https://projectlombok.org/index.html) code generation annotation library to generate a [Builder](https://projectlombok.org/features/Builder.html) class to allow for composing the fields of a GoogleAnalytics request.

Notes: 
* Only the `/collect` endpoint is supported by means of a `GET` request.
* Each invocation to send() kicks off a new Thread for performing the network operation.
* Only the following hit types are available for now:
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

#License

* [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
