package hello.springai.domain.openai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class RagService {

    private final VectorStore elasticsearchVectorStore;
    private final OpenAiChatModel openAiChatModel;

    public RagService(VectorStore elasticsearchVectorStore, OpenAiChatModel openAiChatModel) {
        this.elasticsearchVectorStore = elasticsearchVectorStore;
        this.openAiChatModel = openAiChatModel;
    }

    public Flux<String> generateStreamWithRag(String question) {

        ChatClient chatClient = ChatClient.create(openAiChatModel);

        // OpenAI Option 설정
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.7)
                .build();

        // RAG Advisor 설정
        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor
                .builder(elasticsearchVectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.4d)
                        .topK(15)
                        .build())
                .build();

        // prompt 생성
        Prompt prompt = new Prompt(question, options);

        // 스트리밍 응답
        return chatClient.prompt(prompt)
                .advisors(ragAdvisor)
                .stream()
                .content();
    }
}
