package com.delivery.order.service;

import java.util.Locale;
import java.util.stream.Collectors;
import com.delivery.order.model.Dish;
import com.delivery.order.repository.DishRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private static final String SYSTEM_INSTRUCTIONS = """
            Você é o atendente virtual de um restaurante de delivery.
            Responda brevemente e em português do Brasil.
            Responda perguntas sobre pratos, ingredientes, disponibilidade, preços e recomendações.
            Use apenas o cardápio atual fornecido pelo sistema; não invente pratos nem preços.
            Recuse educadamente qualquer pergunta fora do contexto do restaurante e
            convide o cliente a perguntar sobre o cardápio.
            """;

    private final DishRepository dishes;
    private final ChatClient chatClient;
    private final boolean enabled;

    public ChatService(DishRepository dishes, ChatClient.Builder builder,
                       @Value("${app.ai.enabled:false}") boolean enabled) {
        this.dishes = dishes;
        this.chatClient = builder.build();
        this.enabled = enabled;
    }

    @Transactional(readOnly = true)
    public String answer(String question) {
        // Read a fresh catalog on EVERY question, not a snapshot from app startup.
        String catalog = dishes.findAll().stream()
                .map(this::catalogLine)
                .collect(Collectors.joining("\n"));

        if (!enabled) {
            return "O assistente de IA está desativado. Configure OPENAI_API_KEY "
                    + "(ou OPENAI_BASE_URL para um modelo local) e APP_AI_ENABLED=true.";
        }

        try {
            String reply = chatClient.prompt()
                    .system(spec -> spec.text(SYSTEM_INSTRUCTIONS
                                    + "\nCardápio atual (preço em reais; unidades no estoque):\n{catalog}")
                            .param("catalog", catalog))
                    .user(question)
                    .call()
                    .content();
            return reply == null || reply.isBlank() ? "Não consegui gerar uma resposta agora." : reply;
        } catch (RuntimeException ex) {
            return "O assistente está temporariamente indisponível. Tente novamente em instantes.";
        }
    }

    private String catalogLine(Dish dish) {
        return String.format(Locale.ROOT, "- %s | R$ %s | estoque: %d | %s",
                dish.getName(), dish.getPrice().toPlainString(), dish.getStock(), dish.getDescription());
    }
}
