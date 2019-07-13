### **动态定时任务（基于接口）**

为了演示效果，这里选用 Mysql数据库 和 Mybatis 来查询和调整定时任务的执行周期，然后观察定时任务的执行情况。

#### 1.引入依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.1.4.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.task</groupId>
	<artifactId>springboot-task</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>springboot-task</name>
	<description>Demo project for Spring Boot</description>

	<properties>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.mybatis.spring.boot</groupId>
			<artifactId>mybatis-spring-boot-starter</artifactId>
			<version>2.0.1</version>
		</dependency>

		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>
```



#### **2.添加数据库记录**

在Navicat 连接本地数据库，随便打开查询窗口，然后执行脚本内容，如下：

>DROP DATABASE IF EXISTS `socks`;
>
>CREATE DATABASE `socks`;
>
>USE `SOCKS`;
>
>DROP TABLE IF EXISTS `cron`;
>
>CREATE TABLE `cron` (
>
>`cron_id` varchar(30),
>
>`cron` varchar(30)
>
>);
>
>INSERT INTO `cron` VALUES ('1', '0/5 * * * * ?');

然后在项目中的application.yml 添加数据源：

```properties
#application.yml 配置如下：spring:
datasource:
url: jdbc:mysql://localhost:3306/socks?useSSL=false
username: root
password: root
```

#### **3.创建定时器**

数据库准备好数据之后，我们编写定时任务，注意这里添加的是TriggerTask，目的是循环读取我们在数据库设置好的执行周期，以及执行相关定时任务的内容。具体代码如下：



```java
package com.task.springboottask.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
public class CompleteScheduleConfig implements SchedulingConfigurer {

    @Autowired
    @SuppressWarnings("all")
    CronMapper cronMapper;

    /**
     * 执行定时任务.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.addTriggerTask(
            //1.添加任务内容(Runnable)
            () -> {
                doSomething();
            },

            //2.设置执行周期(Trigger)
            triggerContext -> {
                //2.1 从数据库获取执行周期
                String cron = cronMapper.getCron();
                //2.2 合法性校验.
                if (StringUtils.isEmpty(cron)) {
                    // Omitted Code ..
                 }
                //2.3 返回执行周期(Date)
                return new CronTrigger(cron).nextExecutionTime(triggerContext);
            }
        );
    }

    private void doSomething() {
        System.out.println("执行定时任务2: " + LocalDateTime.now().toLocalTime());
    }
}
```

查询数据库代码：

```java
package com.task.springboottask.controller;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CronMapper {
    @Select("select cron from cron limit 1")
    String getCron();
}
```

#### **4. 动态修改执行周期**

启动应用后，查看控制台，打印时间是我们预期执行定时任务2: 12:59:22.013
执行定时任务2: 12:59:23.014
执行定时任务2: 12:59:24.001
执行定时任务2: 12:59:25.017
执行定时任务2: 12:59:26.017的每5秒一次：

>执行定时任务2: 12:54:10.021
>执行定时任务2: 12:54:15.008
>执行定时任务2: 12:54:20.015
>执行定时任务2: 12:54:25.014
>执行定时任务2: 12:54:30.006
>执行定时任务2: 12:54:35.010

然后打开Navicat ，将执行周期修改为每1秒执行一次，如下：

![1562993820674](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1562993820674.png)

查看控制台，发现执行周期已经改变，并且不需要我们重启应用，十分方便。如下：

>执行定时任务2: 12:59:22.013
>执行定时任务2: 12:59:23.014
>执行定时任务2: 12:59:24.001
>执行定时任务2: 12:59:25.017
>执行定时任务2: 12:59:26.017