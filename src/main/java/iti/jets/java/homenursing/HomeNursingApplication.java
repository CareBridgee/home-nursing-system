package iti.jets.java.homenursing;

import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {OpenAiEmbeddingAutoConfiguration.class})
@EnableScheduling
public class HomeNursingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeNursingApplication.class, args);
    }
}
