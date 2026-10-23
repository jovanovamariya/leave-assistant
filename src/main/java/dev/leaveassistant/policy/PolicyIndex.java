package dev.leaveassistant.policy;

import java.util.List;

/**
 * Where policy chunks are stored for search. The real implementation uses Spring AI and pgvector;
 * tests use an in-memory version, so they need neither Ollama nor a vector database.
 */
public interface PolicyIndex {

    void add(List<PolicyChunk> chunks);

    /** Removes every chunk of the given document, for example before an updated file is ingested. */
    void deleteDocument(String document);
}
