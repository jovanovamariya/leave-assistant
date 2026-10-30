package dev.leaveassistant.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits the text of a policy into chunks, one or more per numbered section ("4. How to request leave").
 * A section that continues on the next page is kept together: its text is joined across the page break and
 * the chunk records the page where the section starts. Each chunk text starts with its section heading,
 * which gives the embedding model context. Page footers are ignored.
 * Plain Java on purpose: no Spring and no AI, so it is easy to test.
 */
public class PolicyChunker {

    private static final Pattern HEADING = Pattern.compile("^\\d{1,2}\\.\\s+\\S.*$");
    private static final Pattern NOISE = Pattern.compile("^(Page \\d+|[^.]*\\(fictional company\\) - Leave Policy(\\s+Page \\d+)?)$");
    private static final String FIRST_SECTION = "Introduction";

    private final int maxChars;

    public PolicyChunker() {
        this(900);
    }

    public PolicyChunker(int maxChars) {
        if (maxChars < 100) {
            throw new IllegalArgumentException("maxChars must be at least 100");
        }
        this.maxChars = maxChars;
    }

    public List<PolicyChunk> chunk(String document, List<PolicyPage> pages) {
        List<PolicyChunk> chunks = new ArrayList<>();
        String section = FIRST_SECTION;
        int sectionPage = pages.isEmpty() ? 1 : pages.get(0).number();
        // The body is NOT flushed at the end of a page: a section may continue on the next one.
        StringBuilder body = new StringBuilder();
        for (PolicyPage page : pages) {
            for (String rawLine : page.text().split("\\R")) {
                String line = rawLine.strip().replaceAll("\\s+", " ");
                if (line.isEmpty() || NOISE.matcher(line).matches()) {
                    continue;
                }
                if (HEADING.matcher(line).matches()) {
                    addChunks(chunks, document, sectionPage, section, body);
                    section = line;
                    sectionPage = page.number();
                } else {
                    body.append(line.startsWith("•") || body.isEmpty() ? "\n" : " ").append(line);
                }
            }
        }
        addChunks(chunks, document, sectionPage, section, body);
        return chunks;
    }

    private void addChunks(List<PolicyChunk> chunks, String document, int page, String section, StringBuilder body) {
        String text = body.toString().strip();
        body.setLength(0);
        if (text.isEmpty()) {
            return;
        }
        for (String piece : pack(text)) {
            chunks.add(new PolicyChunk(section + "\n" + piece, document, page, section));
        }
    }

    /** Packs whole sentences into pieces of at most maxChars. A single longer sentence stays whole. */
    private List<String> pack(String text) {
        List<String> sentences = Arrays.stream(text.split("(?<=[.!?])\\s+|\\n"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (!current.isEmpty() && current.length() + 1 + sentence.length() > maxChars) {
                pieces.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(sentence);
        }
        if (!current.isEmpty()) {
            pieces.add(current.toString());
        }
        return pieces;
    }
}
