package dev.leaveassistant.policy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Answers a question from the policy and streams the answer. Rule R-1: no passage found means the fixed
 * refusal, without calling the model.
 */
@Service
public class PolicyAnswerService {

    static final int TOP_K = 4;
    private static final Pattern SECTION_MENTION = Pattern.compile("(?i)section\\s+(\\d{1,2})");

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
                    .concatWith(Flux.defer(() -> Flux.just(AnswerEvent.done(sources(full.toString(), passages)))));
        });
    }

    /** No sources for a refusal; otherwise the sections the answer names, or all found ones if it names none. */
    static List<AnswerEvent.Citation> sources(String answer, List<PolicyPassage> passages) {
        if (answer.strip().startsWith(AnswerPrompt.REFUSAL)) {
            return List.of();
        }
        List<AnswerEvent.Citation> all = passages.stream()
                .map(p -> new AnswerEvent.Citation(p.document(), p.page(), p.section()))
                .distinct()
                .toList();
        Set<String> named = new HashSet<>();
        Matcher matcher = SECTION_MENTION.matcher(answer);
        while (matcher.find()) {
            named.add(matcher.group(1));
        }
        List<AnswerEvent.Citation> cited = all.stream()
                .filter(c -> named.contains(c.section().split("\\.")[0].strip()))
                .toList();
        return cited.isEmpty() ? all : cited;
    }
}
