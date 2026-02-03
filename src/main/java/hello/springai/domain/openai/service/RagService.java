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

    /**
     * @param elasticsearchVectorStore 문서 검색을 위한 Elasticsearch 벡터 저장소
     * @param openAiChatMocel GPT 채팅 모델
     */
    private final VectorStore elasticsearchVectorStore;
    private final OpenAiChatModel openAiChatModel;

    public RagService(VectorStore elasticsearchVectorStore, OpenAiChatModel openAiChatModel) {
        this.elasticsearchVectorStore = elasticsearchVectorStore;
        this.openAiChatModel = openAiChatModel;
    }

    /**
     * RAG 기반 스트리밍 응답 생성
     * <p>
     * 동작 과정:
     * 1. 질문과 유사한 문서를 Elasticsearch에서 검색 (유사도 0.4 이상, 상위 15개)
     * 2. 검색된 문서를 컨텍스트로 제공
     * 3. GPT 모델이 컨텍스트 기반으로 답변 생성
     * 4. 응답을 스트리밍 방식으로 반환
     *
     * RAG 설정:
     * - 유사도 임계값: 0.4
     * - 검색 문서 수: 15개
     * - 모델: gpt-4.1-mini
     * - Temperature: 0.7
     *
     * @param question 사용자의 질문
     * @return RAG 기반 응답의 Flux 스트림
     */
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
