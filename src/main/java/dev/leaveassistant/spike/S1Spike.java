package dev.leaveassistant.spike;

import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spike S1 (throwaway): embed text with Ollama, store it in pgvector, search it.
 * Runs only with the "spike" profile. Delete this file when the spike is done.
 */
@Component
@Profile("spike")
class S1Spike implements CommandLineRunner {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    S1Spike(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== S1: embedding ===");
        long start = System.nanoTime();
        float[] vector = embeddingModel.embed("Every full-time employee has 26 working days of paid annual leave.");
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Vector size: " + vector.length + " (expected 768)");
        System.out.println("First embedding took " + ms + " ms (includes loading the model)");

        List<Document> documents = List.of(
                new Document("Every full-time employee has 26 working days of paid annual leave per year.",
                        Map.of("topic", "annual-leave")),
                new Document("Up to 5 unused days can be carried over and must be used by 31 March.",
                        Map.of("topic", "carry-over")),
                new Document("Every employee has 10 paid sick days. A medical certificate is needed from the fourth day.",
                        Map.of("topic", "sick-leave")),
                new Document("The office kitchen is cleaned every Friday afternoon.",
                        Map.of("topic", "unrelated")));

        start = System.nanoTime();
        vectorStore.add(documents);
        System.out.println("Stored " + documents.size() + " documents in "
                + (System.nanoTime() - start) / 1_000_000 + " ms");
        List<String> ids = documents.stream().map(Document::getId).toList();

        try {
            search("How many vacation days do I get each year?");
            search("What happens to days I did not use?");
            search("Can I work from home on Fridays?");
        } finally {
            vectorStore.delete(ids);
            System.out.println("Cleaned up the test documents.");
        }
        System.out.println("=== S1 done ===");
    }

    private void search(String question) {
        long start = System.nanoTime();
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(3).build());
        System.out.println();
        System.out.println("Question: " + question + "  (" + (System.nanoTime() - start) / 1_000_000 + " ms)");
        for (Document document : results) {
            System.out.printf("  score %s  [%s]  %s%n", document.getScore(),
                    document.getMetadata().get("topic"), document.getText());
        }
    }
}
