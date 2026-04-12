package com.festora.menuservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class SupabaseConfig {

    @Value("${supabase.endpoint}")
    private String endpoint;

    @Value("${supabase.bucket}")
    private String bucket;

    @Value("${supabase.access-key}")
    private String accessKey;

    @Value("${supabase.secret-key}")
    private String secretKey;

    @PostConstruct
    public void debug() {
        System.out.println("===== S3 DEBUG =====");
        System.out.println("ENDPOINT = " + endpoint);
        System.out.println("BUCKET   = " + bucket);

        System.out.println("====================");
    }


    @Bean
    public S3Client s3Client(
            @Value("${supabase.endpoint}") String endpoint,
            @Value("${supabase.access-key}") String accessKey,
            @Value("${supabase.secret-key}") String secretKey
    ) {
        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.AP_SOUTH_1)
                .forcePathStyle(true)
                .build();
    }


    @Bean
    public S3Presigner s3Presigner(
            @Value("${supabase.endpoint}") String endpoint,
            @Value("${supabase.access-key}") String accessKey,
            @Value("${supabase.secret-key}") String secretKey
    ) {
        AwsBasicCredentials creds =
                AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.AP_SOUTH_1)
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }

}