package com.task.springboottask.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.StringUtils;

public abstract class SchedulingConfigurerRealization implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfigurerRealization.class);

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                //1.添加任务内容(Runnable)
                () -> {
                    businessHandle();
                },

                //2.设置执行周期(Trigger)
                triggerContext -> {
                    //2.1 从数据库获取执行周期
                    String cron =  getCronInfo();
                    //2.2 合法性校验.
                    if (StringUtils.isEmpty(cron)) {
                        logger.warn("cron内容不能为空！");
                    }
                    //2.3 返回执行周期(Date)
                    return new CronTrigger(cron).nextExecutionTime(triggerContext);
                }
        );
    }

    public abstract String getCronInfo();

    public abstract void businessHandle();
}
