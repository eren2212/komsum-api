package com.ereniridere.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling // SSE heartbeat (@Scheduled) için gerekli
public class AsyncConfig {

	// İlçe geneline bildirim yayını ana isteği bloklamamalı —
	// bu thread pool listener metotlarının arka planda çalışmasını sağlar.
	@Bean(name = "notificationExecutor")
	public Executor notificationExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("notif-async-");
		executor.initialize();
		return executor;
	}
}
