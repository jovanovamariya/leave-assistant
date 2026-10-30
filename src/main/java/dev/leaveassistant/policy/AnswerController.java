package dev.leaveassistant.policy;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/policy")
public class AnswerController {

    private final PolicyAnswerService answers;

    public AnswerController(PolicyAnswerService answers) {
        this.answers = answers;
    }

    /** Streams the answer as server-sent events: many "token" events, then one "done" event with the sources. */
    @GetMapping(path = "/answer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AnswerEvent> answer(@RequestParam("q") String question) {
        return answers.answer(question);
    }
}
