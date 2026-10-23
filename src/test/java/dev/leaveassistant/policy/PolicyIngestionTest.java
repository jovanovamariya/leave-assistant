package dev.leaveassistant.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leaveassistant.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * Ingestion with the real PDF, the real reader and a real database, but an in-memory index instead of
 * Ollama and pgvector. This is why the test runs in CI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, PolicyIngestionTest.FakeIndexConfig.class})
class PolicyIngestionTest {

    static class InMemoryPolicyIndex implements PolicyIndex {
        final List<PolicyChunk> chunks = new ArrayList<>();
        int addCalls;

        @Override
        public void add(List<PolicyChunk> newChunks) {
            addCalls++;
            chunks.addAll(newChunks);
        }

        @Override
        public void deleteDocument(String document) {
            chunks.removeIf(chunk -> chunk.document().equals(document));
        }
    }

    @TestConfiguration
    static class FakeIndexConfig {
        @Bean
        @Primary
        InMemoryPolicyIndex inMemoryPolicyIndex() {
            return new InMemoryPolicyIndex();
        }
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InMemoryPolicyIndex index;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
        jdbc.update("DELETE FROM policy_document");
        index.chunks.clear();
        index.addCalls = 0;
    }

    private ResponseEntity<String> ingest() {
        return client.post().uri("/api/policy/ingest")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> { })
                .toEntity(String.class);
    }

    private int storedDocuments() {
        return jdbc.queryForObject("SELECT count(*) FROM policy_document", Integer.class);
    }

    @Test
    void firstIngestStoresTheChunksAndRecordsTheFile() {
        ResponseEntity<String> response = ingest();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"INGESTED\""), response.getBody());
        assertFalse(index.chunks.isEmpty());
        assertTrue(response.getBody().contains("\"chunks\":" + index.chunks.size()), response.getBody());
        assertEquals(1, storedDocuments());
        assertTrue(index.chunks.stream().allMatch(chunk -> chunk.document().equals("Leave-Policy.pdf")));
        assertTrue(index.chunks.stream().anyMatch(chunk -> chunk.text().contains("26 working days")));
    }

    @Test
    void ingestingTheSameFileTwiceChangesNothing() {
        ingest();
        int chunksAfterFirst = index.chunks.size();

        ResponseEntity<String> second = ingest();

        assertEquals(HttpStatus.OK, second.getStatusCode());
        assertTrue(second.getBody().contains("\"status\":\"UNCHANGED\""), second.getBody());
        assertEquals(chunksAfterFirst, index.chunks.size());
        assertEquals(1, index.addCalls);
        assertEquals(1, storedDocuments());
    }

    @Test
    void aChangedFileReplacesTheOldChunksInsteadOfAddingToThem() {
        ingest();
        int chunksAfterFirst = index.chunks.size();
        // Pretend the file was ingested before with different content.
        jdbc.update("UPDATE policy_document SET content_hash = 'old-hash'");

        ResponseEntity<String> second = ingest();

        assertTrue(second.getBody().contains("\"status\":\"INGESTED\""), second.getBody());
        assertEquals(chunksAfterFirst, index.chunks.size());
        assertEquals(2, index.addCalls);
        assertEquals(1, storedDocuments());
    }
}
