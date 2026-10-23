package dev.leaveassistant.policy;

/** The text of one page of a policy document. Page numbers start at 1. */
public record PolicyPage(int number, String text) {
}
