package hello.springai.domain.openai.service;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
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

    public OpenAIService(OpenAiChatModel openAiChatModel, OpenAiEmbeddingModel openAiEmbeddingModel, OpenAiImageModel openAiImageModel, OpenAiAudioSpeechModel openAiAudioSpeechModel, OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel) {
        this.openAiChatModel = openAiChatModel;
        this.openAiEmbeddingModel = openAiEmbeddingModel;
        this.openAiImageModel = openAiImageModel;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
    }

    // 1-1. Chat Model : response
    public String generate(String text) {
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
        ChatResponse response = openAiChatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    // 1-2. Chat Model : response stream
    public Flux<String> generateStream(String text) {
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
        return openAiChatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText());
    }

    // 2. Embedding Model
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

    // 3. Image Model (DALL-E)
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

    // 4-1. Audio Model - TTS
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

    // 4-2. Audio Model - STT
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
