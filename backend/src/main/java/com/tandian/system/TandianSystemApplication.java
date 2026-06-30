package com.tandian.system;

import com.tandian.system.service.ScheduledTaskService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 探店路线规划系统主启动类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.tandian.system.mapper")
public class TandianSystemApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TandianSystemApplication.class, args);
        
        // 初始化定时任务
        try {
            ScheduledTaskService scheduledTaskService = context.getBean(ScheduledTaskService.class);
            scheduledTaskService.init();
        } catch (Exception e) {
            System.err.println("定时任务初始化失败：" + e.getMessage());
        }
        
        System.out.println("========================================");
        System.out.println("探店路线规划系统启动成功！");
        System.out.println("访问地址：http://localhost:8080/api");
        System.out.println("========================================");
    }
}
