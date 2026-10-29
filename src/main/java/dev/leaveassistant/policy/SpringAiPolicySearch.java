package dev.leaveassistant.policy;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** Semantic search in the Spring AI vector store (pgvector). The question is embedded by Ollama. */
@Component
class SpringAiPolicySearch implements PolicySearch {

    private final VectorStore vectorStore;

    SpringAiPolicySearch(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<PolicyPassage> search(String query, int topK) {
        List<Document> found = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build());
        return found.stream().map(SpringAiPolicySearch::toPassage).toList();
    }

    private static PolicyPassage toPassage(Document document) {
        Double score = document.getScore();
        return new PolicyPassage(
                document.getText(),
                text(document.getMetadata().get("document")),
                number(document.getMetadata().get("page")),
                text(document.getMetadata().get("section")),
                score == null ? 0.0 : score);
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int number(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }
}
