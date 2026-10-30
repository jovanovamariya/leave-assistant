package dev.leaveassistant.policy;

import java.util.List;

/** Builds the two prompts sent to the model. Plain Java, so the wording is easy to test. */
final class AnswerPrompt {

    static final String REFUSAL =
            "I can't find this in the leave policy. Please contact the HR team at hr@harborpine.example.";

    private AnswerPrompt() {
    }

    static String system() {
        return """
                You are the leave assistant of Harbor & Pine Software.
                Answer the question using ONLY the policy excerpts given in the user message.
                If the excerpts do not clearly contain the answer, reply with exactly this sentence and nothing else: %s
                Never guess and never use outside knowledge. Do not invent numbers, dates or rules.
                You do not know how many leave days a particular employee has left: the leave system holds that number.
                Keep the answer short. Mention the section you used, for example (section 6).
                Answer in the language of the question.
                """.formatted(REFUSAL);
    }

    static String user(String question, List<PolicyPassage> passages) {
        StringBuilder text = new StringBuilder("Policy excerpts:\n\n");
        for (int i = 0; i < passages.size(); i++) {
            PolicyPassage passage = passages.get(i);
            text.append('[').append(i + 1).append("] (page ").append(passage.page()).append(")\n")
                    .append(passage.text()).append("\n\n");
        }
        return text.append("Question: ").append(question).toString();
    }
}
