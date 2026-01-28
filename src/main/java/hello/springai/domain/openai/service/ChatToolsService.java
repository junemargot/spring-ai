package hello.springai.domain.openai.service;

import hello.springai.domain.openai.dto.response.UserResponseDto;
import org.springframework.ai.tool.annotation.Tool;

public class ChatToolsService {

    @Tool(description = "User personal information: name, age, address, phone, email")
    public UserResponseDto getUserInfoTool() {
        return new UserResponseDto("홍길동", 15L, "서울특별시 종로구 청와대로 1", "010-0000-0000", "kildong@gmail.com");
    }

}
