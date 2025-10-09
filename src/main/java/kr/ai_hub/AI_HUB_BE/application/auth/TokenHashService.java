package kr.ai_hub.AI_HUB_BE.application.auth;

import kr.ai_hub.AI_HUB_BE.global.error.exception.IllegalSystemStateException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 토큰 원문을 SHA-256으로 해시하기 위한 유틸리티 컴포넌트.
// 저장소에는 항상 해시값만 저장하여 원문 노출을 방지한다.
@Component
public class TokenHashService {

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalSystemStateException("SHA-256 MessageDigest not available", e);
        }
    }
}
