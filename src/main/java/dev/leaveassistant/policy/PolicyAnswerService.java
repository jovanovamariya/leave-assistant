package dev.leaveassistant.policy;

import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Answers a question from the policy and streams the answer. Rule R-1: no passage found means the fixed
 * refusal, without calling the model.
 */
@Service
public class PolicyAnswerService {

    static final int TOP_K = 4;

    private final PolicySearchService search;
    private final AnswerModel model;

    PolicyAnswerService(PolicySearchService search, AnswerModel model) {
        this.search = search;
        this.model = model;
    }

    public Flux<AnswerEvent> answer(String question) {
        List<PolicyPassage> passages = search.search(question, TOP_K);   // throws on a blank question
        if (passages.isEmpty()) {
            return Flux.just(AnswerEvent.token(AnswerPrompt.REFUSAL), AnswerEvent.done(List.of()));
        }
        String user = AnswerPrompt.user(question.strip(), passages);
        return Flux.defer(() -> {
            StringBuilder full = new StringBuilder();
            return model.stream(AnswerPrompt.system(), user)
                    .doOnNext(full::append)
                    .map(AnswerEvent::token)
                    .concatWith(Flux.defer(() -> Flux.just(AnswerEvent.done(
                            full.toString().strip().startsWith(AnswerPrompt.REFUSAL) ? List.of() : citations(passages)))));
        });
    }

    private static List<AnswerEvent.Citation> citations(List<PolicyPassage> passages) {
        return passages.stream()
                .map(p -> new AnswerEvent.Citation(p.document(), p.page(), p.section()))
                .distinct()
                .toList();
    }
}
