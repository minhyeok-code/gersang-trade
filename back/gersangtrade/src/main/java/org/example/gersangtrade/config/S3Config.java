package org.example.gersangtrade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * AWS S3 클라이언트 설정.
 *
 * <p>인증 방식 (우선순위):
 * <ol>
 *   <li>aws.access-key 설정 시 → StaticCredentialsProvider (로컬 개발용)</li>
 *   <li>미설정 시 → SDK 기본 체인 → EC2 IAM 역할 자동 사용</li>
 * </ol>
 * aws.s3.bucket(AWS_S3_BUCKET)이 없으면 빈 자체를 생성하지 않는다.
 */
@Configuration
public class S3Config {

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Bean
    @ConditionalOnProperty(name = "aws.s3.bucket")
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        if (StringUtils.hasText(accessKey)) {
            // 로컬 개발: 명시적 액세스 키 사용
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    )
            );
        }
        // EC2: credentialsProvider 미설정 시 SDK가 IAM 역할(인스턴스 메타데이터)을 자동으로 사용

        return builder.build();
    }
}
