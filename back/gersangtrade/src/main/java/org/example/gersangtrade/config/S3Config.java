package org.example.gersangtrade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 클라이언트 설정.
 *
 * <p>환경변수로 설정해야 하는 값:
 * <pre>
 * AWS_ACCESS_KEY    — AWS IAM 액세스 키
 * AWS_SECRET_KEY    — AWS IAM 시크릿 키
 * AWS_REGION        — S3 버킷 리전 (예: ap-northeast-2)
 * AWS_S3_BUCKET     — S3 버킷명
 * </pre>
 */
@Configuration
public class S3Config {

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    /** aws.access-key가 설정된 경우에만 S3Client 빈을 생성한다. 로컬 환경에서는 생성되지 않는다. */
    @Bean
    @ConditionalOnProperty(name = "aws.access-key")
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }
}
