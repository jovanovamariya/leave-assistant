package dev.leaveassistant.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyChunkerTest {

    private static List<PolicyChunk> chunk(int maxChars, PolicyPage... pages) {
        return new PolicyChunker(maxChars).chunk("Doc.pdf", List.of(pages));
    }

    @Test
    void splitsAtNumberedHeadingsAndRecordsSectionPageAndDocument() {
        List<PolicyChunk> chunks = chunk(900, new PolicyPage(1, "1. Scope\nThis is scope text.\n2. Leave\nLeave text here."));

        assertEquals(2, chunks.size());
        assertEquals("1. Scope", chunks.get(0).section());
        assertEquals("2. Leave", chunks.get(1).section());
        assertEquals(1, chunks.get(0).page());
        assertEquals("Doc.pdf", chunks.get(0).document());
    }

    @Test
    void chunkTextStartsWithItsHeading() {
        List<PolicyChunk> chunks = chunk(900, new PolicyPage(1, "1. Scope\nThis is scope text."));

        assertEquals("1. Scope\nThis is scope text.", chunks.get(0).text());
    }

    @Test
    void aSectionContinuingOnTheNextPageKeepsItsName() {
        List<PolicyChunk> chunks = chunk(900,
                new PolicyPage(1, "4. Request\nFirst part."),
                new PolicyPage(2, "Second part.\n5. Status\nStatus text."));

        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(1).page());
        assertEquals("4. Request", chunks.get(1).section());
        assertEquals("4. Request\nSecond part.", chunks.get(1).text());
        assertEquals("5. Status", chunks.get(2).section());
    }

    @Test
    void pageFootersAreIgnored() {
        List<PolicyChunk> chunks = chunk(900, new PolicyPage(1,
                "1. Scope\nText one.\nPage 1\nHarbor & Pine Software (fictional company) - Leave Policy"));

        assertEquals(1, chunks.size());
        assertEquals("1. Scope\nText one.", chunks.get(0).text());
    }

    @Test
    void aLongSectionIsSplitAtSentenceBoundaries() {
        StringBuilder text = new StringBuilder("1. Long\n");
        for (int i = 1; i <= 6; i++) {
            text.append("This is sentence number ").append(i).append(" of the long section.\n");
        }

        List<PolicyChunk> chunks = chunk(150, new PolicyPage(1, text.toString()));

        assertEquals(2, chunks.size());
        for (PolicyChunk chunk : chunks) {
            assertEquals("1. Long", chunk.section());
            assertTrue(chunk.text().length() <= 150 + "1. Long".length() + 1);
        }
    }

    @Test
    void textBeforeTheFirstHeadingBelongsToTheIntroduction() {
        List<PolicyChunk> chunks = chunk(900, new PolicyPage(1, "Leave Policy\nWelcome.\n1. Scope\nText."));

        assertEquals(2, chunks.size());
        assertEquals("Introduction", chunks.get(0).section());
        assertEquals("Introduction\nLeave Policy Welcome.", chunks.get(0).text());
    }

    @Test
    void bulletPointsStayInTheirSection() {
        List<PolicyChunk> chunks = chunk(900, new PolicyPage(1, "2. Steps\n\u2022 First step.\n\u2022 Second step."));

        assertEquals(1, chunks.size());
        assertEquals("2. Steps", chunks.get(0).section());
        assertTrue(chunks.get(0).text().contains("First step."));
        assertTrue(chunks.get(0).text().contains("Second step."));
    }

    @Test
    void blankPagesAndNoPagesProduceNoChunks() {
        assertTrue(chunk(900, new PolicyPage(1, "  \n \n")).isEmpty());
        assertTrue(new PolicyChunker().chunk("Doc.pdf", List.of()).isEmpty());
    }

    @Test
    void aTinyLimitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PolicyChunker(50));
    }

    @Test
    void aFooterWithTheTextAndThePageNumberOnOneLineIsIgnored() {
        List<PolicyChunk> chunks = chunk(900, new PolicyPage(2,
                "10. Public holidays\nA public holiday is never deducted.\n"
                        + "Harbor & Pine Software (fictional company) - Leave Policy Page 2"));

        assertEquals(1, chunks.size());
        assertEquals("10. Public holidays\nA public holiday is never deducted.", chunks.get(0).text());
    }
}
