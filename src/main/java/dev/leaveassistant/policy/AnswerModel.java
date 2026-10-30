package dev.leaveassistant.policy;

import reactor.core.publisher.Flux;

/** The language model, seen from our code: a prompt goes in, pieces of text come out as they are produced. */
public interface AnswerModel {

    Flux<String> stream(String systemPrompt, String userPrompt);
}
