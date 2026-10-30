package dev.leaveassistant.policy;

import java.util.List;

/**
 * One event of a streamed answer. "token" events carry a piece of the answer text; the last event, "done",
 * carries the sources the answer is based on.
 */
public record AnswerEvent(String type, String text, List<Citation> citations) {

    /** Where a statement in the answer comes from. */
    public record Citation(String document, int page, String section) {
    }

    static AnswerEvent token(String text) {
        return new AnswerEvent("token", text, List.of());
    }

    static AnswerEvent done(List<Citation> citations) {
        return new AnswerEvent("done", "", citations);
    }
}
