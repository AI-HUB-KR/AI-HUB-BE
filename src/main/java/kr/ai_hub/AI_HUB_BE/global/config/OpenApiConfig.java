package kr.ai_hub.AI_HUB_BE.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger 문서 설정
 * <p>
 * Swagger UI는 http://localhost:8080/swagger-ui.html 에서 접근 가능합니다.
 * OpenAPI 스펙은 http://localhost:8080/v3/api-docs 에서 조회 가능합니다.
 * </p>
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "AI-HUB Backend API",
        version = "1.0.0",
        description = "AI-HUB 서비스의 백엔드 API 명세서입니다.",
        contact = @Contact(
            name = "AI-HUB Support",
            url = "https://github.com/AI-HUB"
        ),
        license = @License(
            name = "MIT License"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Local Development Server"
        ),
        @Server(
            url = "${deployment.address}",
            description = "Production Server"
        )
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Access Token을 사용한 인증. " +
                  "Authorization 헤더에 'Bearer {token}' 형식으로 전달해주세요."
)
public class OpenApiConfig {
    // OpenAPI 설정이 애노테이션으로 정의되어 있습니다.
    // 추가 커스터마이징이 필요한 경우 이 클래스에 빈을 추가하세요.
}