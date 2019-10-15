package com.task.springboottask.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;

/**
 * 注释
 */
@Configuration
@EnableScheduling
public class CompleteScheduleConfig extends SchedulingConfigurerRealization {

    @Autowired
    @SuppressWarnings("all")
    CronMapper cronMapper;

    @Override
    public String getCronInfo() {
        return cronMapper.getCron();
    }

    @Override
    public void businessHandle() {
        System.out.println("执行定时任务2: " + LocalDateTime.now().toLocalTime());
    }
}