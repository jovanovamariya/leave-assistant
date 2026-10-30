package dev.leaveassistant.policy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/** Streams the answer from the chat model (Ollama) through Spring AI. */
@Component
class SpringAiAnswerModel implements AnswerModel {

    private final ChatClient chatClient;

    SpringAiAnswerModel(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userPrompt) {
        return chatClient.prompt().system(systemPrompt).user(userPrompt).stream().content();
    }
}
