package dev.leaveassistant.policy;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    private final PolicyIngestionService ingestion;

    public PolicyController(PolicyIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    /** Loads or reloads the policy. Safe to call repeatedly. */
    @PostMapping("/ingest")
    public IngestResult ingest() {
        return ingestion.ingest();
    }
}
