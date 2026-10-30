package dev.leaveassistant.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class PolicyAnswerServiceTest {

    private static final PolicyPassage CANCEL = new PolicyPassage(
            "6. Cancelling leave\n• You can cancel an approved request only before its start date.",
            "Leave-Policy.pdf", 1, "6. Cancelling leave", 0.81);

    private final List<String> prompts = new ArrayList<>();

    private PolicyAnswerService service(List<PolicyPassage> found, Flux<String> modelOutput) {
        PolicySearchService search = new PolicySearchService((q, k) -> found, 0.6);
        AnswerModel model = (system, user) -> {
            prompts.add(user);
            return modelOutput;
        };
        return new PolicyAnswerService(search, model);
    }

    @Test
    void nothingFoundMeansTheFixedRefusalAndTheModelIsNotCalled() {
        List<AnswerEvent> events = service(List.of(), Flux.just("never used"))
                .answer("what is the parental leave").collectList().block();

        assertEquals(AnswerPrompt.REFUSAL, events.get(0).text());
        assertEquals("done", events.get(1).type());
        assertTrue(events.get(1).citations().isEmpty());
        assertTrue(prompts.isEmpty());
    }

    @Test
    void theAnswerIsStreamedPieceByPieceAndEndsWithTheSources() {
        List<AnswerEvent> events = service(List.of(CANCEL), Flux.just("Yes, ", "before the start date ", "(section 6)."))
                .answer("can I cancel approved leave").collectList().block();

        assertEquals(4, events.size());
        assertEquals("Yes, ", events.get(0).text());
        assertEquals("token", events.get(2).type());
        AnswerEvent done = events.get(3);
        assertEquals("done", done.type());
        assertEquals(List.of(new AnswerEvent.Citation("Leave-Policy.pdf", 1, "6. Cancelling leave")), done.citations());
    }

    @Test
    void whenTheModelRefusesNoSourcesAreShown() {
        List<AnswerEvent> events = service(List.of(CANCEL), Flux.just(AnswerPrompt.REFUSAL))
                .answer("can I work from home").collectList().block();

        assertTrue(events.get(events.size() - 1).citations().isEmpty());
    }

    @Test
    void thePromptContainsTheQuestionAndThePassages() {
        service(List.of(CANCEL), Flux.just("ok")).answer("  can I cancel approved leave ").collectList().block();

        String user = prompts.get(0);
        assertTrue(user.contains("Question: can I cancel approved leave"));
        assertTrue(user.contains("only before its start date"));
        assertTrue(AnswerPrompt.system().contains(AnswerPrompt.REFUSAL));
    }

    @Test
    void aBlankQuestionIsRejected() {
        PolicyAnswerService service = service(List.of(CANCEL), Flux.just("ok"));
        try {
            service.answer("  ");
            assertFalse(true, "expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("The question must not be empty", expected.getMessage());
        }
    }

    private static final PolicyPassage REQUEST = new PolicyPassage("4. How to request leave\nSubmit the request.",
            "Leave-Policy.pdf", 1, "4. How to request leave", 0.68);

    @Test
    void onlyTheSectionsNamedInTheAnswerAreShownAsSources() {
        List<AnswerEvent> events = service(List.of(CANCEL, REQUEST), Flux.just("Only before the start date (section 6)."))
                .answer("can I cancel approved leave").collectList().block();

        assertEquals(List.of(new AnswerEvent.Citation("Leave-Policy.pdf", 1, "6. Cancelling leave")),
                events.get(events.size() - 1).citations());
    }

    @Test
    void whenTheAnswerNamesNoSectionAllFoundSectionsAreShown() {
        List<AnswerEvent> events = service(List.of(CANCEL, REQUEST), Flux.just("Only before the start date."))
                .answer("can I cancel approved leave").collectList().block();

        assertEquals(2, events.get(events.size() - 1).citations().size());
    }
}
