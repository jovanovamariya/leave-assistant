package dev.leaveassistant.policy;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    private final PolicyIngestionService ingestion;
    private final PolicySearchService search;

    public PolicyController(PolicyIngestionService ingestion, PolicySearchService search) {
        this.ingestion = ingestion;
        this.search = search;
    }

    /** Loads or reloads the policy. Safe to call repeatedly. */
    @PostMapping("/ingest")
    public IngestResult ingest() {
        return ingestion.ingest();
    }

    /** Semantic search: the passages closest in meaning to the question, best first. */
    @GetMapping("/search")
    public List<PolicyPassage> search(@RequestParam("q") String question,
                                      @RequestParam(name = "topK", defaultValue = "4") int topK) {
        return search.search(question, topK);
    }
}
