package kr.ai_hub.AI_HUB_BE.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * RestClient 설정
 * <p>
 * AI 서버와의 HTTP 통신을 위한 RestClient Bean을 생성합니다.
 * SSE(Server-Sent Events) 스트리밍 지원을 포함합니다.
 * Virtual Threads가 I/O 블로킹을 자동으로 처리하므로 동기식 설정을 사용합니다.
 * </p>
 */
@Slf4j
@Configuration
public class RestClientConfig {

    @Value("${ai-server.url}")
    private String aiServerUrl;

    /**
     * AI 서버 통신용 RestClient Bean (SSE 스트리밍용, 5분 타임아웃)
     */
    @Bean
    @Qualifier("aiServerRestClient")
    public RestClient aiServerRestClient() {
        return buildRestClient(Duration.ofMinutes(5));
    }

    /**
     * AI 서버 파일 업로드용 RestClient Bean (1분 타임아웃)
     */
    @Bean
    @Qualifier("aiServerUploadClient")
    public RestClient aiServerUploadClient() {
        return buildRestClient(Duration.ofMinutes(1));
    }

    private RestClient buildRestClient(Duration readTimeout) {
        log.info("RestClient 초기화: AI Server URL = {}, readTimeout = {}", aiServerUrl, readTimeout);

        HttpClient httpClient = HttpClient.newBuilder().build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
