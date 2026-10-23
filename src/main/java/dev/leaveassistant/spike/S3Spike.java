package dev.leaveassistant.spike;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spike S3 (throwaway): how fast is the local chat model, and does it refuse when the answer is not in the passages?
 * Runs only with the "s3" profile. Delete this file when the spike is done.
 */
@Component
@Profile("s3")
class S3Spike implements CommandLineRunner {

    private static final String REFUSAL = "I could not find this information in the leave policy.";

    private static final String SYSTEM = """
            You answer questions about the company leave policy.
            Use ONLY the passages given in the user message.
            If the passages do not contain the answer, reply with exactly this sentence and nothing else:
            %s
            Otherwise answer in at most two sentences.
            """.formatted(REFUSAL);

    private static final String PASSAGES = """
            [Page 1] Every full-time employee has 26 working days of paid annual leave per year.
            [Page 1] Up to 5 unused days can be carried over and must be used by 31 March.
            [Page 2] Every employee has 10 paid sick days. A medical certificate is needed from the fourth day.
            [Page 2] The office kitchen is cleaned every Friday afternoon.
            """;

    private final ChatClient chat;

    S3Spike(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== S3: chat model ===");
        ask("How many vacation days do I get each year?");
        ask("When do carried-over days expire?");
        ask("Can I work from home on Fridays?");
        System.out.println("=== S3 done (the third answer should be the refusal sentence) ===");
    }

    private void ask(String question) {
        long start = System.nanoTime();
        String answer = chat.prompt()
                .system(SYSTEM)
                .user("Passages:\n" + PASSAGES + "\nQuestion: " + question)
                .call()
                .content();
        long seconds = (System.nanoTime() - start) / 1_000_000_000L;
        System.out.println();
        System.out.println("Question: " + question + "  (" + seconds + " s)");
        System.out.println("Answer:   " + answer);
    }
}
