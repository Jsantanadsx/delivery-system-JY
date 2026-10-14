package com.delivery.order.api;

import java.util.Map;
import com.delivery.order.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssistantController {
    private final ChatService chat;
    public AssistantController(ChatService chat) { this.chat = chat; }

    @PostMapping("/assistant")
    public Map<String, String> answer(@Valid @RequestBody AssistantRequest request) {
        return Map.of("answer", chat.answer(request.question()));
    }
}
