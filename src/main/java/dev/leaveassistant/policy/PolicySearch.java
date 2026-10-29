package dev.leaveassistant.policy;

import java.util.List;

/**
 * Finds the passages of the policy that are closest in meaning to a question, best first.
 * The real implementation uses Spring AI and pgvector; tests use a stand-in.
 */
public interface PolicySearch {

    List<PolicyPassage> search(String query, int topK);
}
