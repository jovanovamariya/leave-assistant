package dev.leaveassistant.policy;

/** A passage found by a search: its text, where it came from and how similar it is to the question (0 to 1). */
public record PolicyPassage(String text, String document, int page, String section, double score) {
}
