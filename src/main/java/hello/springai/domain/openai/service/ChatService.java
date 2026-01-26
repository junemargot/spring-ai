package hello.springai.domain.openai.service;

import hello.springai.domain.openai.entity.Chat;
import hello.springai.domain.openai.repository.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Transactional(readOnly = true)
    public List<Chat> readAllChats(String userId) {
        return chatRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }
}
