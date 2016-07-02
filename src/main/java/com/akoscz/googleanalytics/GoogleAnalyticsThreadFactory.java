package com.akoscz.googleanalytics;

import java.text.MessageFormat;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class GoogleAnalyticsThreadFactory implements ThreadFactory {
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private String threadNameFormat = null;

	public GoogleAnalyticsThreadFactory(String threadNameFormat) {
		this.threadNameFormat = threadNameFormat;
	}

	public Thread newThread(Runnable r) {
		Thread thread = new Thread(Thread.currentThread().getThreadGroup(), r, MessageFormat.format(threadNameFormat, threadNumber.getAndIncrement()), 0);
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	}

	public static synchronized ThreadPoolExecutor createExecutor(GoogleAnalyticsConfig config) {
		if (config == null) {
			throw new RuntimeException("'config' cannot be null!");
		}

		return new ThreadPoolExecutor(
				config.getMinThreads(),
				config.getMaxThreads(),
				config.getThreadTimeout(),
				TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(
						config.getQueueSize()),
						new GoogleAnalyticsThreadFactory(config.getThreadNameFormat()),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
}