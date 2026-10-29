package dev.leaveassistant.policy;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Searches the policy and drops passages that are clearly unrelated. The minimum score is only a first
 * filter: spike S1 showed that a similarity score alone cannot decide whether a question is answerable.
 */
@Service
public class PolicySearchService {

    static final int DEFAULT_TOP_K = 4;
    static final int MAX_TOP_K = 10;

    private final PolicySearch search;
    private final double minScore;

    PolicySearchService(PolicySearch search, @Value("${app.policy.min-score:0.5}") double minScore) {
        this.search = search;
        this.minScore = minScore;
    }

    public List<PolicyPassage> search(String question, int topK) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("The question must not be empty");
        }
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK must be between 1 and " + MAX_TOP_K);
        }
        return search.search(question.strip(), topK).stream()
                .filter(passage -> passage.score() >= minScore)
                .toList();
    }
}
