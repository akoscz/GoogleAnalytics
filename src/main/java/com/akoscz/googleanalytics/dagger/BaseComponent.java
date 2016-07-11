package com.akoscz.googleanalytics.dagger;

import com.akoscz.googleanalytics.BaseAnalytics;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {HttpClientModule.class, ThreadPoolExecutorModule.class, ConfigModule.class})
public interface BaseComponent {

    void inject(BaseAnalytics baseAnalyics);
}
