package com.akoscz.googleanalytics.dagger;

import com.akoscz.googleanalytics.GoogleAnalyticsConfig;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class ConfigModule {

    private GoogleAnalyticsConfig config;

    public ConfigModule(GoogleAnalyticsConfig config) {
        this.config = config;
    }

    @Provides
    GoogleAnalyticsConfig providesConfig() {
        return config;
    }
}
