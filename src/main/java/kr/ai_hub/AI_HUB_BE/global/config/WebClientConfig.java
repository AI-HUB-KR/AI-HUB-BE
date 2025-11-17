package kr.ai_hub.AI_HUB_BE.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 설정
 * <p>
 * AI 서버와의 HTTP 통신을 위한 WebClient Bean을 생성합니다.
 * SSE(Server-Sent Events) 스트리밍 지원을 포함합니다.
 * Virtual Threads가 I/O 블로킹을 자동으로 처리하므로 간소화된 설정을 사용합니다.
 * </p>
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${ai-server.url}")
    private String aiServerUrl;

    /**
     * AI 서버 통신용 WebClient Bean
     * <p>
     * - Response Timeout: 5분 (SSE 스트리밍용)
     * - Max In-Memory Size: 10MB
     * </p>
     */
    @Bean
    public WebClient aiServerWebClient() {
        log.info("WebClient 초기화: AI Server URL = {}", aiServerUrl);

        // 간소화된 HTTP 클라이언트 설정 (Virtual Threads 사용)
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5));  // SSE 스트리밍을 위한 긴 타임아웃

        // Exchange Strategies 설정 (버퍼 사이즈 증가)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))  // 10MB
                .build();

        return WebClient.builder()
                .baseUrl(aiServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
