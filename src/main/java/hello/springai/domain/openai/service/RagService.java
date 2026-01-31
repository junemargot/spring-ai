package hello.springai.domain.openai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

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

        // option
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.7)
                .build();

        // RAG Advisor 설정
        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor
                .builder(elasticsearchVectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.0d)
                        .topK(6)
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
