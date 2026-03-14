package top.enderliquid.audioflow;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

import static top.enderliquid.audioflow.common.constant.TimeZoneConstants.GLOBAL_TIME_ZONE_ID;

@EnableScheduling
@SpringBootApplication
@MapperScan("top.enderliquid.audioflow.mapper")
public class AudioFlowApplication {

    @PostConstruct
    public void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(GLOBAL_TIME_ZONE_ID));
    }

    public static void main(String[] args) {
        SpringApplication.run(AudioFlowApplication.class, args);
    }

}

