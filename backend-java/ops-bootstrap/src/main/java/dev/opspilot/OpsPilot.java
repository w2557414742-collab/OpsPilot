package dev.opspilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 启动模块化单体与数据库任务队列调度器。 */
@EnableScheduling
@SpringBootApplication
public class OpsPilot {
    /** 从命令行参数和环境变量启动应用。 */
    public static void main(String[] args) {
        SpringApplication.run(OpsPilot.class, args);
    }
}
