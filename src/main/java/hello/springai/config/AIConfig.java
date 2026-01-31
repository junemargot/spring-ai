package hello.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AIConfig {

    /**
     * InMemory 방식 (JDBC 의존성 없을 때)
     * Spring AI가 자동으로 생성해주므로 불필요
     */
//    @Bean
//    public ChatMemoryRepository chatMemoryRepository() {
//        return new InMemoryChatMemoryRepository();
//    }

    /**
     * JDBC 방식 (JDBC 의존성 있을 때)
     * Spring AI가 자동으로 생성해주므로 불필요
     * 단, 커스터마이징이 필요하다면 @Primary와 함께 활성화
     */
//    @Bean
//    @Primary
//    public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
//        return JdbcChatMemoryRepository.builder()
//                .jdbcTemplate(jdbcTemplate)
//                .transactionManager(transactionManager)
//                .build();
//    }

//    @Bean
//    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
//        return ChatClient.create(openAiChatModel);
//    }
}
