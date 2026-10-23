package dev.leaveassistant.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** Record of an ingested policy file. The hash lets a repeated ingest be recognised (rule R-7). */
@Entity
@Table(name = "policy_document")
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected PolicyDocument() {
        // required by JPA
    }

    public PolicyDocument(String name, String contentHash, int chunkCount) {
        this.name = name;
        this.contentHash = contentHash;
        this.chunkCount = chunkCount;
    }

    @PrePersist
    void onCreate() {
        this.ingestedAt = Instant.now();
    }

    public void update(String contentHash, int chunkCount) {
        this.contentHash = contentHash;
        this.chunkCount = chunkCount;
        this.ingestedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getContentHash() { return contentHash; }
    public int getChunkCount() { return chunkCount; }
    public Instant getIngestedAt() { return ingestedAt; }
}
