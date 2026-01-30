package hello.springai.api;

import hello.springai.domain.openai.dto.response.ComposerResponseDto;
import hello.springai.domain.openai.entity.Chat;
import hello.springai.domain.openai.service.ChatService;
import hello.springai.domain.openai.service.OpenAIService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final OpenAIService openAIService;
    private final ChatService chatService;

    public ChatController(OpenAIService openAIService, ChatService chatService) {
        this.openAIService = openAIService;
        this.chatService = chatService;
    }

//    @PostMapping("/chat")
//    public String chat(@RequestBody Map<String, String> body) {
//        return openAIService.generate(body.get("text"));
//    }

    @PostMapping("/chat")
    public List<ComposerResponseDto> chat(@RequestBody Map<String, String> body) {
        return openAIService.generateChat(body.get("text"));
    }

    @PostMapping("/chat/stream")
    public Flux<String> streamChat(@RequestBody Map<String, String> body) {
        return openAIService.generateStream(body.get("text"));
    }

    @PostMapping("/chat/history/{userid}")
    public List<Chat> getChatHistory(@PathVariable("userid") String userId) {
        return chatService.readAllChats(userId);
    }
}
