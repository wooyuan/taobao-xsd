package com.taobao.logistics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 * 定时任务配置类
 * 配置定时任务的线程池
 */
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(@NonNull ScheduledTaskRegistrar taskRegistrar) {
        // 设置定时任务使用的线程池大小为2
        // 这样可以避免定时任务之间相互阻塞
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(2));
    }
}