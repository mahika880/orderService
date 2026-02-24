package com.example.orderservice.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Executable;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "orderExecutor")
    public Executor orderExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(5);
            executor.setQueueCapacity(50);
            executor.setThreadNamePrefix("Order-Assync-");
            executor.setTaskDecorator(runnable -> {
                var contextMap = MDC.getCopyOfContextMap();
                return () -> {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);

                    }

                    try {
                        runnable.run();
                    } finally {
                        MDC.clear();
                    }
                };
            } );
            executor.initialize();
            return executor;
    }
}
