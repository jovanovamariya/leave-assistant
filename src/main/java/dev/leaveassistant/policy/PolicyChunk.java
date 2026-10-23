package dev.leaveassistant.policy;

/** A passage of the policy that is embedded and stored: its text, where it came from and its section. */
public record PolicyChunk(String text, String document, int page, String section) {
}
