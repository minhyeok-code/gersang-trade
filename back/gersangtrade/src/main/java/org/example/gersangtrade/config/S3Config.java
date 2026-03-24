package org.example.gersangtrade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 클라이언트 설정.
 * 크롤러 Batch Job에서 아이템/용병 이미지를 S3에 업로드할 때 사용한다.
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

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    /**
     * AWS SDK v2 S3 클라이언트 빈.
     * StaticCredentialsProvider를 사용하여 환경변수 기반 인증을 적용한다.
     */
    @Bean
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
