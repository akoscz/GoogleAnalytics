package com.akoscz.googleanalytics;

import com.akoscz.googleanalytics.dagger.BaseComponent;
import com.akoscz.googleanalytics.dagger.ConfigModule;
import com.akoscz.googleanalytics.dagger.DaggerBaseComponent;
import com.akoscz.googleanalytics.util.ExceptionReporter;
import dagger.Lazy;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

/**
 * The BaseAnaytics abstract class holds our Dagger graph, the global tracker instance along with other class members
 * and methods to handle sending data to the GoogleAnalytics endpoint.
 *
 * The following abstract methods are declared which must be implemented by extending classes:
 *     abstract String buildUrlString();
 *     abstract List<GoogleAnalyticsParameter> buildPostParams();
 *     abstract GoogleAnalyticsConfig getConfig();
 *     abstract void validateRequiredParams();
 *     abstract void resetTracker();
 */
@Log
public abstract class BaseAnalytics {

    public static final int PROTOCOL_VERSION = 1;
    private static final String ENCODING = "UTF-8";
    private static final Level DEFAULT_LOG_LEVEL = Level.SEVERE;

    @Getter
    protected static BaseComponent graph;
    @Getter
    protected static GoogleAnalytics.Tracker globalTracker;

    @Inject
    Lazy<ThreadPoolExecutor> executor;
    @Inject
    Lazy<CloseableHttpClient> httpClient;

    protected ArrayList<GoogleAnalyticsParameter> postParameters = new ArrayList<GoogleAnalyticsParameter>();

    /**
     * Default constructor which initializes the Dagger graph.
     */
    public BaseAnalytics() {
        graph.inject(this);
    }

    /**
     * Build a Tracker instance with default config values by which you can compose your GoogleAnalytics tracking request.
     * @param trackingId Required Valid Google Analytics Tracking Id.
     * @param clientId Required Valid Client Id UUID.
     * @param applicationName Required non-null non-empty Application Name.
     * @return A Google Analytics Tracker instance.
     */
    public static GoogleAnalytics.Tracker buildTracker(String trackingId, UUID clientId, String applicationName) {
        return buildTracker(trackingId, clientId, applicationName, null);
    }

    /**
     * Build a Tracker instance by which you can compose your GoogleAnalytics tracking request.
     * @param trackingId Required Valid Google Analytics Tracking Id.
     * @param clientId Required Valid Client Id UUID.
     * @param applicationName Required non-null non-empty Application Name.
     * @param config The configuration parameters for the tracker.  If null, default config values will be used.
     * @return A Google Analytics Tracker instance.
     */
    public static GoogleAnalytics.Tracker buildTracker(@NonNull String trackingId, @NonNull UUID clientId,
                                                       @NonNull String applicationName, GoogleAnalyticsConfig config) {
        if (config == null) config = new GoogleAnalyticsConfig();

        GoogleAnalytics.Tracker tracker = GoogleAnalytics.trackerBuilder()
                .config(config)
                .protocolVersion(PROTOCOL_VERSION)
                .trackingId(trackingId)
                .clientId(clientId)
                .applicationName(applicationName)
                .isExceptionFatal(true); // initialize default value

        graph = DaggerBaseComponent.builder()
                .configModule(new ConfigModule(config))
                .build();

        // set the global tracker instance
        globalTracker = tracker;

        return globalTracker;
    }

    /**
     * Register a default UncaughtExceptionHandler which reports all uncaught exceptions to Google Analytics.
     * If there exists a default UncaughtExceptionHandler it will be invoked after we have sent the
     * exception event to Google Analytics.
     *
     * @param packages The package names that we want to include in the event description from the exception stack trace.
     */
    public static void registerDefaultUncaughtExceptionHandler(String... packages) {
        Thread.UncaughtExceptionHandler existingUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionReporter(globalTracker, existingUncaughtExceptionHandler, packages));
    }

    /**
     * Register an UncaughtExceptionHandler for the given Thread which reports uncaught exceptions to Google Analytics.
     * If there exists an UncaughtExceptionHandler on the Thread it will be invoked after we have handled sending the
     * exception event to Google Analytics.
     *
     * @param thread The thread on which to register the UncaughtExceptionHandler.
     * @param packages The package names that we want to include in the event description from the exception stack trace.
     */
    public static void registerThreadUncaughtExceptionHandler(@NonNull Thread thread, String... packages) {
        Thread.UncaughtExceptionHandler existingUncaughtExceptionHandler = thread.getUncaughtExceptionHandler();
        thread.setUncaughtExceptionHandler(new ExceptionReporter(globalTracker, existingUncaughtExceptionHandler, packages));
    }

    /**
     * Enable debug mode.
     *
     * By enabling debug mode, all network traffic will go to the debug endpoint.
     * Logging level will automatically be set to Level.ALL
     * @param enableDebug True to enable debug mode, False otherwise.
     */
    public void setDebug(boolean enableDebug) {
        GoogleAnalyticsConfig config = getConfig();
        config.setDebug(enableDebug);
        globalTracker.config(config);
        log.setLevel(Level.ALL);
    }

    /**
     * Set the Log Level for logging
     * @param logLevel A java.util.logging.LogLevel value.
     *                 Passing in null will reset the log level to the default, Level.SEVERE
     */
    public void setLogLevel(Level logLevel) {
        if (logLevel == null) {
            // reset to default
            logLevel = DEFAULT_LOG_LEVEL;
        }
        log.setLevel(logLevel);
    }

    /**
     * Send the parameters over the network to Google Analytics.
     * Note that this method will clear all the non-required parameters irregardless of success or failure
     * of the network request.
     * This method defaults to performing the network operation asynchronously.
     */
    public void send() {
        // default to send asynchronously
        send(true);
    }

    /**
     * Send the parameters over the network to Google Analytics.
     * Note that this method will clear all the non-required parameters irregardless of success or failure
     * of the network request.
     * @param asynchronous True to perform the network operation asynchronously, False otherwise.
     */
    public void send(boolean asynchronous) {

        GoogleAnalyticsConfig config = getConfig();
        if (config.isHttpMethodGet()) {
            final String url = buildUrlString();
            if (asynchronous) {
                executor.get().submit(new Runnable() {
                    @Override
                    public void run() {
                        doGetNetworkOperation(url);
                    }
                });
            } else {
                doGetNetworkOperation(url);
            }
        } else { // POST method
            final List<GoogleAnalyticsParameter> postParams = buildPostParams();

            if (asynchronous) {
                executor.get().submit(new Runnable() {
                    @Override
                    public void run() {
                        doPostNetworkOperation(postParams);
                    }
                });
            } else {
                doPostNetworkOperation(postParams);
            }

        }

        // clear all non-required fields
        resetTracker();
    }

    protected void doGetNetworkOperation(String url) {
        log.info("executing on thread: " + Thread.currentThread().getName());

        HttpURLConnection connection = null;
        try {
            GoogleAnalyticsConfig config = getConfig();
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            if (StringUtils.isNotEmpty(config.getUserAgent())) {
                connection.setRequestProperty("User-Agent", config.getUserAgent());
            }

            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warning("Error requesting url: '" + url + "'. Response code: " + responseCode);
            } else {
                log.info("Successfully hit tracker: '" + url + "'");
            }

            if (config.isDebug()) {
                StringBuilder content = new StringBuilder();
                @Cleanup BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String line;
                // read from the urlconnection via the bufferedreader
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line + "\n");
                }
                log.info(content.toString());
            }

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void doPostNetworkOperation(List<GoogleAnalyticsParameter> postParameters) {
        log.info("executing on thread: " + Thread.currentThread().getName());

        try {
            GoogleAnalyticsConfig config = getConfig();
            HttpPost httpPost = new HttpPost(config.getEndpoint());
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, ENCODING));

            @Cleanup CloseableHttpResponse httpResponse = httpClient.get().execute(httpPost);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warning("Error posting to endpoint: '" + config.getEndpoint()
                        + "'. Response code: '" + responseCode + "'\n"
                        + httpPost.toString() + "\n" + postParameters);
            } else {
                log.info("Successfully posted params to tracker: " + postParameters);
            }

            if (config.isDebug()) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity(), ENCODING);
                log.info("response: " + responseBody);
            }

            EntityUtils.consumeQuietly(httpResponse.getEntity());
        } catch (Exception e) {
            log.warning("Problem sending post request: " + e.toString());
            e.printStackTrace();
        }
    }

    abstract String buildUrlString();
    abstract List<GoogleAnalyticsParameter> buildPostParams();
    abstract GoogleAnalyticsConfig getConfig();
    abstract void validateRequiredParams();
    abstract void resetTracker();
}
