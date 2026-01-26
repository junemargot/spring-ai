package hello.springai.domain.openai.repository;


import hello.springai.domain.openai.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByUserIdOrderByCreatedAtAsc(String userId);
}
