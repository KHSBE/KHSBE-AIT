package com.compass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 생성일, 수정일 자동화 활성화 (이건 DB 관리를 위해 꼭 필요하니 남겨둡니다!)
@EnableJpaAuditing
@SpringBootApplication // 이제 OpenAI 찌꺼기 없이 순정 상태로 실행합니다.
public class CompassApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompassApplication.class, args);
    }
}