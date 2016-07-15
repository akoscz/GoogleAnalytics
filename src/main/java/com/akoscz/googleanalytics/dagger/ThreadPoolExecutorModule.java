package com.akoscz.googleanalytics.dagger;

import com.akoscz.googleanalytics.GoogleAnalyticsConfig;
import com.akoscz.googleanalytics.util.GoogleAnalyticsThreadFactory;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Module
public class ThreadPoolExecutorModule {

    @Provides
    ThreadPoolExecutor providesExecutor(GoogleAnalyticsThreadFactory threadFactory, LinkedBlockingDeque<Runnable> queue,
                                        RejectedExecutionHandler rejectedExecutionHandler, GoogleAnalyticsConfig config) {
        return new ThreadPoolExecutor(
                config.getMinThreads(),
                config.getMaxThreads(),
                config.getThreadTimeout(),
                TimeUnit.SECONDS,
                queue,
                threadFactory,
                rejectedExecutionHandler);
    }

    @Provides
    GoogleAnalyticsThreadFactory providesThreadFactory(GoogleAnalyticsConfig config) {
        return new GoogleAnalyticsThreadFactory(config.getThreadNameFormat());
    }

    @Provides
    LinkedBlockingDeque<Runnable> providesQueue(GoogleAnalyticsConfig config) {
        return new LinkedBlockingDeque<Runnable>(config.getQueueSize());
    }

    @Provides
    @Singleton
    RejectedExecutionHandler providesRejectedExecutionHandler() {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }
}
