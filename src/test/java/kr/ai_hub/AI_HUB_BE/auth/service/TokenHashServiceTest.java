package kr.ai_hub.AI_HUB_BE.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TokenHashServiceTest {

    @InjectMocks
    private TokenHashService tokenHashService;

    @Test
    @DisplayName("토큰 해시 생성 - SHA-256")
    void hashToken() {
        // given
        String token = "test-token";

        // when
        String hash = tokenHashService.hashToken(token);

        // then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64); // SHA-256 hex string length
        assertThat(hash).matches("^[a-f0-9]+$"); // Hexadecimal format
    }

    @Test
    @DisplayName("동일한 토큰은 동일한 해시 반환")
    void hashToken_Consistency() {
        // given
        String token = "consistent-token";

        // when
        String hash1 = tokenHashService.hashToken(token);
        String hash2 = tokenHashService.hashToken(token);

        // then
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 토큰은 다른 해시 반환")
    void hashToken_Uniqueness() {
        // given
        String token1 = "token-1";
        String token2 = "token-2";

        // when
        String hash1 = tokenHashService.hashToken(token1);
        String hash2 = tokenHashService.hashToken(token2);

        // then
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
