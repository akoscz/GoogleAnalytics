package com.akoscz.googleanalytics.dagger;

import com.akoscz.googleanalytics.GoogleAnalyticsConfig;
import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.inject.Singleton;

@Module
public class HttpClientModule {

    @Provides
    public PoolingHttpClientConnectionManager providesConnectionManager() {
        return new PoolingHttpClientConnectionManager();
    }

    @Provides
    public HttpClientBuilder providesHttpClientBuilder(PoolingHttpClientConnectionManager connectionManager, GoogleAnalyticsConfig config) {
        connectionManager.setDefaultMaxPerRoute(config.getMaxThreads());
        connectionManager.setMaxTotal(config.getMaxThreads());

        return HttpClients.custom().setConnectionManager(connectionManager);
    }

    @Provides
    public CloseableHttpClient providesHttpClient(HttpClientBuilder builder, GoogleAnalyticsConfig config) {
        if (StringUtils.isNotEmpty(config.getUserAgent())) {
            builder.setUserAgent(config.getUserAgent());
        }

        if (StringUtils.isNotEmpty(config.getProxyHost())) {
            builder.setProxy(new HttpHost(config.getProxyHost(), config.getProxyPort()));

            if (StringUtils.isNotEmpty(config.getProxyUserName())) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(config.getProxyHost(), config.getProxyPort()),
                        new UsernamePasswordCredentials(config.getProxyUserName(), config.getProxyPassword()));
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }

        return builder.build();
    }
}