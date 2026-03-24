package org.example.gersangtrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 매월 1일 새벽 3시 크롤링 배치 스케줄러를 위해 @EnableScheduling 활성화 */
@SpringBootApplication
@EnableScheduling
public class GersangtradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GersangtradeApplication.class, args);
    }

}
