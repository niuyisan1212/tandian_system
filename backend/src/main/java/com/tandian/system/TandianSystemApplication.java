package com.tandian.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        SpringApplication.run(TandianSystemApplication.class, args);
        System.out.println("========================================");
        System.out.println("探店路线规划系统启动成功！");
        System.out.println("访问地址：http://localhost:8080/api");
        System.out.println("========================================");
    }
}
