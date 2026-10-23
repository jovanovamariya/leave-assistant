package dev.leaveassistant.policy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the policy PDF into the search index (rule R-7: ingesting the same file twice changes nothing).
 * Read the PDF, split it into chunks, store the chunks, remember the file's hash.
 */
@Service
public class PolicyIngestionService {

    private final Resource policyFile;
    private final PolicyPdfReader reader;
    private final PolicyIndex index;
    private final PolicyDocumentRepository documents;
    private final PolicyChunker chunker = new PolicyChunker();

    PolicyIngestionService(@Value("${app.policy.resource:classpath:policy/Leave-Policy.pdf}") Resource policyFile,
                           PolicyPdfReader reader,
                           PolicyIndex index,
                           PolicyDocumentRepository documents) {
        this.policyFile = policyFile;
        this.reader = reader;
        this.index = index;
        this.documents = documents;
    }

    @Transactional
    public IngestResult ingest() {
        String name = policyFile.getFilename();
        String hash = sha256(policyFile);

        Optional<PolicyDocument> existing = documents.findByName(name);
        if (existing.isPresent() && existing.get().getContentHash().equals(hash)) {
            return new IngestResult("UNCHANGED", name, existing.get().getChunkCount());
        }

        List<PolicyChunk> chunks = chunker.chunk(name, reader.read(policyFile));
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No text found in " + name);
        }

        // An updated file replaces the old chunks instead of adding to them.
        index.deleteDocument(name);
        index.add(chunks);

        if (existing.isPresent()) {
            existing.get().update(hash, chunks.size());
        } else {
            documents.save(new PolicyDocument(name, hash, chunks.size()));
        }
        return new IngestResult("INGESTED", name, chunks.size());
    }

    private static String sha256(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
