package kr.ai_hub.AI_HUB_BE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AiHubBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiHubBeApplication.class, args);
	}

}
