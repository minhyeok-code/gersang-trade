package org.example.gersangtrade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 감사(Auditing) 기능을 활성화하는 설정 클래스.
 * BaseEntity의 createdAt, updatedAt 자동 관리에 필요하다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
