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
			<groupId>com.alibaba</groupId>
			<artifactId>druid-spring-boot-starter</artifactId>
			<version>1.1.9</version>
		</dependency>

		<dependency>
			<groupId>com.alibaba</groupId>
			<artifactId>druid</artifactId>
			<version>1.1.0</version>
		</dependency>

		<dependency>
			<groupId>org.mybatis.spring.boot</groupId>
			<artifactId>mybatis-spring-boot-starter</artifactId>
			<version>2.0.1</version>
		</dependency>

		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
			<version>5.1.41</version>
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

然后在项目中的`application.properties`添加数据源：

```properties
#application.properties 配置如下：
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
spring.datasource.url=jdbc:mysql://{自己数据库连接}:{数据库端口}/{数据库名称}?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true
spring.datasource.username={数据库用户名}
spring.datasource.password={数据库密码}
spring.datasource.initialSize=5  #连接初始值，连接池启动时创建的连接数量的初始值
spring.datasource.minIdle=5      #最小空闲值，当空闲的连接数少于阀值时，连接池就会预申请去一些连接，以免洪峰来时来不及申请
spring.datasource.maxActive=20   #连接池的最大值，同一时间可以从池分配的最多连接数量，0时无限制
spring.datasource.maxWait=60000  #配置获取连接等待超时的时间
spring.datasource.timeBetweenEvictionRunsMillis=60000  #配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
spring.datasource.minEvictableIdleTimeMillis=300000    #配置一个连接在池中最小生存的时间，单位是毫秒
spring.datasource.validationQuery=SELECT 1 FROM DUAL   #用来验证数据库连接的有效性
spring.datasource.testWhileIdle=true
spring.datasource.testOnBorrow=false
spring.datasource.testOnReturn=false
spring.datasource.poolPreparedStatements=true   #是否对已备语句进行池管理（布尔值），是否对PreparedStatement进行缓存，并且指定每个连接上PSCache的大小
spring.datasource.maxPoolPreparedStatementPerConnectionSize=20
spring.datasource.filters=stat,wall       # 配置监控统计拦截的filters，去掉后监控界面sql无法统计，'wall'用于防火墙
spring.datasource.connectionProperties=druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000 # 通过connectProperties属性来打开mergeSql功能；慢SQL记录
```

#### **3.创建定时器**

数据库准备好数据之后，我们编写定时任务，注意这里添加的是TriggerTask，目的是循环读取我们在数据库设置好的执行周期，以及执行相关定时任务的内容。将公用代码抽出为抽象类`SchedulingConfigurerRealization`

```java
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
```



每次需要新建一个定时任务时，只需要继承`SchedulingConfigurerRealization`，然后具体实现`getCronInfo`和`businessHandle`两个方法即可。

> - `getCronInfo`方法是从数据库中获取定时信息
> - `businessHandle` 方法是具体业务逻辑

具体代码如下：

```java
package com.task.springboottask.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;

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

启动应用后，查看控制台，打印时间是我们预期执行定时的每5秒一次：

>执行定时任务2: 12:54:10.021 
>
>执行定时任务2: 12:54:15.008 
>
>执行定时任务2: 12:54:20.015 
>
>执行定时任务2: 12:54:25.014 
>
>执行定时任务2: 12:54:30.006 
>
>执行定时任务2: 12:54:35.010

然后打开Navicat ，将执行周期修改为每1秒执行一次，如下：

修改数据库cron内容为：`0/1 * * * * ?`

>执行定时任务2: 12:59:22.013 
>
>执行定时任务2: 12:59:23.014 
>
>执行定时任务2: 12:59:24.001 
>
>执行定时任务2: 12:59:25.017 
>
>执行定时任务2: 12:59:26.017