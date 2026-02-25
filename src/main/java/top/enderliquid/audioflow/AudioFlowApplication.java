package top.enderliquid.audioflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("top.enderliquid.audioflow.mapper")
public class AudioFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudioFlowApplication.class, args);
    }

}

