package hello.springai.domain.openai.service;

import hello.springai.domain.openai.dto.response.ComposerResponseDto;
import hello.springai.domain.openai.entity.Chat;
import hello.springai.domain.openai.repository.ChatRepository;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class OpenAIService {

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final OpenAiImageModel openAiImageModel;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ChatRepository chatRepository;
    private final VectorStore elasticsearchVectorStore;

    /**
     * OpenAIService 생성자
     * Spring의 의존성 주입을 통해 필요한 모든 OpenAI 모델 초기화
     *
     * @param openAiChatModel                  GPT 채팅 모델
     * @param openAiEmbeddingModel             임베딩 생성 모델
     * @param openAiImageModel                 DALL-E 이미지 생성 모델
     * @param openAiAudioSpeechModel           TTS(텍스트 -> 음성) 모델
     * @param openAiAudioTranscriptionModel    STT(음성 -> 텍스트) 모델
     * @param chatMemoryRepository             대화 메모리 저장소
     * @param chatRepository                   대화 이력 저장소
     * @param elasticsearchVectorStore         RAG용 Elasticsearch 벡터 저장소
     */
    public OpenAIService(OpenAiChatModel openAiChatModel, OpenAiEmbeddingModel openAiEmbeddingModel, OpenAiImageModel openAiImageModel, OpenAiAudioSpeechModel openAiAudioSpeechModel, OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel, ChatMemoryRepository chatMemoryRepository, ChatRepository chatRepository, VectorStore elasticsearchVectorStore) {
        this.openAiChatModel = openAiChatModel;
        this.openAiEmbeddingModel = openAiEmbeddingModel;
        this.openAiImageModel = openAiImageModel;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatRepository = chatRepository;
        this.elasticsearchVectorStore = elasticsearchVectorStore;
    }

    /**
     * GPT 모델을 사용하여 텍스트 응답 생성 (동기 방식)
     *
     * @param text 사용자의 입력 텍스트
     * @return GPT 모델이 생성한 응답 텍스트
     * @see #generateStream(String) 스트리밍 방식의 대안 메서드
     */
    public String generate(String text) {
        // message
        SystemMessage systemMessage = new SystemMessage("");
        UserMessage userMessage = new UserMessage(text);
        AssistantMessage assistantMessage = new AssistantMessage("");

        // option
        OpenAiChatOptions options = OpenAiChatOptions.builder()
//                .model("gpt-4.1-mini")
                .model("gpt-5")
                .temperature(0.7)
                .build();

        // prompt
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);

        // request & response
        ChatResponse response = openAiChatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    /**
     * GPT 모델을 사용하여 구조화된 응답을 생성 (작곡가 정보)
     * ChatClient를 사용하여 응답을 ComposerResponseDto 리스트로 파싱
     *
     * @param text 작곡가에 대한 질문 텍스트
     * @return 작곡가 정보 DTO 리스트 (한국어)
     */
    public List<ComposerResponseDto> generateChat(String text) {

        ChatClient chatClient = ChatClient.create(openAiChatModel);

        // message
        SystemMessage systemMessage = new SystemMessage("");
        UserMessage userMessage = new UserMessage(text);
        AssistantMessage assistantMessage = new AssistantMessage("");

        // option
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.7)
                .build();

        // prompt
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);

        // request & response
        return chatClient
                .prompt(prompt)
                .user(u -> u.text(text + "\n 모든 필드의 값(value)은 한국어로 작성해줘."))
                .call()
                .entity(new ParameterizedTypeReference<List<ComposerResponseDto>>() {});
    }

    /**
     * GPT 모델을 사용하여 RAG 기반 스트리밍 응답 생성 (비동기 방식)
     *
     * 주요 기능:
     * - ChatMemory를 통한 대화 컨텍스트 관리 (최근 10개 메시지)
     * - Elasticsearch VectorStore 기반 RAG 검색 (유사도 0.5 이상, 상위 15개)
     * - 검색된 문서를 System Message로 제공하여 컨텍스트 강화
     * - 전체 대화 이력을 DB에 영구 저장
     * - ChatTools 활용 가능
     *
     * @param text 사용자의 입력 텍스트
     * @return GPT 모델이 생성한 RAG 기반 응답의 Flux 스트림
     * @see #generate(String) 동기 방식의 대안 메서드
     */
    public Flux<String> generateStream(String text) {

        ChatClient chatClient = ChatClient.create(openAiChatModel);

        // ChatMemory로 관리하기 위한 key 명시 (user&page)
        String userId = "beethoven" + "_" + "1770";

        // history - 전체 대화 저장 (User)
        Chat chatUser = new Chat();
        chatUser.setUserId(userId);
        chatUser.setMessageType(MessageType.USER);
        chatUser.setContent(text);

        // message (multi-turn)
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();
        chatMemory.add(userId, new UserMessage(text)); // 신규 메시지도 추가

        // VectorStore 검색 (RAG)
        String ragContext = "";
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(text)
                    .topK(15)
                    .similarityThreshold(0.5d)
                    .build();

            List<Document> relevantDocs = elasticsearchVectorStore.similaritySearch(searchRequest);

            if(!relevantDocs.isEmpty()) {
                StringBuilder context = new StringBuilder("\n\n=== 참고 문서 ===\n");
                for(int i = 0; i < Math.min(5, relevantDocs.size()); i++) {
                    Document doc = relevantDocs.get(i);
                    context.append("\n[문서 ").append(i + 1).append("]\n");
                    context.append(doc.getText()).append("\n");
                }
                ragContext = context.toString();
            }
        } catch (Exception e) {
            // VectorStore 검색 실패 시 무시하고 계속 진행
            System.err.println("RAG 검색 실패: " + e.getMessage());
        }

        // 2. System Message에 RAG 컨텍스트 + 가이드 포함
        String systemPrompt = """
                당신은 친절하고 유능한 AI 어시스턴트입니다.
                
                %s
                
                답변 가이드:
                - 위 참고 문서에 관련 정보가 있다면 우선적으로 활용하세요
                - 참고 문서가 없거나 관련이 없다면, 당신의 지식을 활용하여 답변하세요
                - 자연스럽고 친절하게 답변해주세요
                """.formatted(ragContext.isEmpty() ? "제공된 참고 문서가 없습니다." : ragContext);

        SystemMessage systemMessage = new SystemMessage(systemPrompt);

        // 3. ChatMemory에 추가
        chatMemory.add(userId, systemMessage);
        chatMemory.add(userId, new UserMessage(text));

        // option
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.7)
                .build();

        // prompt
        Prompt prompt = new Prompt(chatMemory.get(userId), options);

        // 응답 메시지를 저장할 임시 버퍼
        StringBuilder responseBuffer = new StringBuilder();

        // request & response
        return chatClient.prompt(prompt)
                .tools(new ChatToolsService())
//                .advisors(ragAdvisor)
                .stream()
                .content()
                .map(token -> {
                    responseBuffer.append(token);
                    return token;
                })
                .doOnComplete(() -> {
                    // chatMemory 저장
                    chatMemory.add(userId, new AssistantMessage(responseBuffer.toString()));
                    chatMemoryRepository.saveAll(userId, chatMemory.get(userId));

                    // history - 전체 대화 저장
                    Chat chatAssistant = new Chat();
                    chatAssistant.setUserId(userId);
                    chatAssistant.setMessageType(MessageType.ASSISTANT);
                    chatAssistant.setContent(responseBuffer.toString());

                    chatRepository.saveAll(List.of(chatUser, chatAssistant));
                });

        // request & response
//        return openAiChatModel.stream(prompt)
//                .mapNotNull(response -> {
//                    String token = response.getResult().getOutput().getText();
//                    if(token != null) {
//                        responseBuffer.append(token);
//                        return token;
//                    }
//                    return null;
//
//                })
//                .doOnComplete(() -> {
//                    chatMemory.add(userId, new AssistantMessage(responseBuffer.toString()));
//                    chatMemoryRepository.saveAll(userId, chatMemory.get(userId));
//
//                    // history - 전체 대화 저장 (AI)
//                    Chat chatAssistant = new Chat();
//                    chatAssistant.setUserId(userId);
//                    chatAssistant.setMessageType(MessageType.ASSISTANT);
//                    chatAssistant.setContent(responseBuffer.toString());
//
//                    chatRepository.saveAll(List.of(chatUser, chatAssistant));
//                });
    }

    /**
     * 텍스트들을 벡터 임베딩으로 변환
     *
     * @param texts 임베딩으로 변환할 텍스트 리스트
     * @param model 사용할 OpenAI 임베딩 모델명
     * @return 각 텍스트에 대응하는 임베딩 벡터(float 배열)의 리스트
     */
    public List<float[]> generateEmbedding(List<String> texts, String model) {
        // option
        EmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();

        // prompt
        EmbeddingRequest prompt = new EmbeddingRequest(texts, embeddingOptions);

        // request & response
        EmbeddingResponse response = openAiEmbeddingModel.call(prompt);
        return response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    /**
     * DALL-E 모델을 사용하여 텍스트 설명으로부터 이미지를 생성
     *
     * @param text 이미지 생성을 위한 텍스트 프롬프트
     * @param count 생성할 이미지 개수
     * @param height 생성할 이미지의 높이
     * @param width 생성할 이미지의 너비
     * @return 생성된 이미지들의 URL 리스트
     */
    public List<String> generateImages(String text, int count, int height, int width) {
        // option
        OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
                .quality("hd")
                .N(count)
                .height(height)
                .width(width)
                .build();

        // prompt
        ImagePrompt prompt = new ImagePrompt(text, imageOptions);

        // request & response
        ImageResponse response = openAiImageModel.call(prompt);
        return response.getResults().stream()
                .map(image -> image.getOutput().getUrl())
                .toList();
    }

    /**
     * TTS(Text-To-Speech): 텍스트를 음성으로 변환
     *
     * @param text 음성으로 변환할 텍스트
     * @return MP3 형식의 음성 데이터
     */
    public byte[] tts(String text) {
        // option
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(1.0)
                .model(OpenAiAudioApi.TtsModel.TTS_1.value)
                .build();

        // prompt
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, speechOptions);

        // request & response
        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
        return response.getResult().getOutput();
    }

    /**
     * STT(Speech-To-Text): 음성 파일을 텍스트로 변환
     *
     * @param audioFile 변환할 음성 파일 리소스
     * @return VTT 형식의 텍스트 변환 결과
     */
    public String stt(Resource audioFile) {
        // option
        OpenAiAudioApi.TranscriptResponseFormat responseformat = OpenAiAudioApi.TranscriptResponseFormat.VTT;
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language("ko") // 인식 언어
                .prompt("당신의 미래는 내일이 아니라 오늘 당신이 하는 것에 의해 만들어진다.") // 음성 인식 전 참고할 텍스트 프롬프트
                .temperature(0f)
//                .model(OpenAiAudioApi.TtsModel.TTS_1.value) // 결과 타입 지정: VTT 자막 형식
                .model("whisper-1")  // Whisper는 OpenAI의 STT 모델
                .responseFormat(responseformat)
                .build();

        // prompt
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);

        // request & response
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
        return response.getResult().getOutput();
    }
}
