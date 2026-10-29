package dev.leaveassistant.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PolicySearchServiceTest {

    private static PolicyPassage passage(String section, double score) {
        return new PolicyPassage("text of " + section, "Doc.pdf", 1, section, score);
    }

    private final List<PolicyPassage> found = List.of(
            passage("a", 0.68), passage("b", 0.55), passage("c", 0.49), passage("d", 0.30));

    @Test
    void dropsPassagesBelowTheMinimumScoreAndKeepsTheOrder() {
        PolicySearchService service = new PolicySearchService((query, topK) -> found, 0.5);

        List<PolicyPassage> result = service.search("How many days?", 4);

        assertEquals(List.of("a", "b"), result.stream().map(PolicyPassage::section).toList());
    }

    @Test
    void passesTheTrimmedQuestionAndTopKToTheSearch() {
        String[] seenQuery = new String[1];
        int[] seenTopK = new int[1];
        PolicySearchService service = new PolicySearchService((query, topK) -> {
            seenQuery[0] = query;
            seenTopK[0] = topK;
            return found;
        }, 0.5);

        service.search("  How many days?  ", 3);

        assertEquals("How many days?", seenQuery[0]);
        assertEquals(3, seenTopK[0]);
    }

    @Test
    void returnsNothingWhenNoPassageIsCloseEnough() {
        PolicySearchService service = new PolicySearchService((query, topK) -> List.of(passage("x", 0.2)), 0.5);

        assertTrue(service.search("question", 3).isEmpty());
    }

    @Test
    void aScoreExactlyAtTheMinimumIsKept() {
        PolicySearchService service = new PolicySearchService((query, topK) -> List.of(passage("x", 0.5)), 0.5);

        assertEquals(1, service.search("question", 3).size());
    }

    @Test
    void aBlankQuestionIsRejected() {
        PolicySearchService service = new PolicySearchService((query, topK) -> found, 0.5);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, 4));
        assertThrows(IllegalArgumentException.class, () -> service.search("", 4));
        assertThrows(IllegalArgumentException.class, () -> service.search("   ", 4));
    }

    @Test
    void topKOutsideTheAllowedRangeIsRejected() {
        PolicySearchService service = new PolicySearchService((query, topK) -> found, 0.5);

        assertThrows(IllegalArgumentException.class, () -> service.search("question", 0));
        assertThrows(IllegalArgumentException.class, () -> service.search("question", 11));
        assertEquals(2, service.search("question", 1).size());
        assertEquals(2, service.search("question", 10).size());
    }
}
