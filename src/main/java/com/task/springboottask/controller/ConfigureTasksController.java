package com.task.springboottask.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

@Configuration
@EnableScheduling
public class ConfigureTasksController {

    //@Scheduled(cron = "0/5 * * * * ?")
    public void configureTasks() {
        System.out.println("定时任务执行：" + LocalDate.now());
    }
}
