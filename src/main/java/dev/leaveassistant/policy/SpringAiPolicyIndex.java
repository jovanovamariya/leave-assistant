package dev.leaveassistant.policy;

import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** Stores policy chunks in the Spring AI vector store (pgvector). Embeddings come from Ollama. */
@Component
class SpringAiPolicyIndex implements PolicyIndex {

    private final VectorStore vectorStore;

    SpringAiPolicyIndex(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void add(List<PolicyChunk> chunks) {
        List<Document> documents = chunks.stream()
                .map(chunk -> new Document(chunk.text(), Map.<String, Object>of(
                        "document", chunk.document(),
                        "page", chunk.page(),
                        "section", chunk.section())))
                .toList();
        vectorStore.add(documents);
    }

    @Override
    public void deleteDocument(String document) {
        vectorStore.delete("document == '" + document + "'");
    }
}
