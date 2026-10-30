# HR Leave Assistant

A small web app where an employee asks questions about the company leave policy and gets
answers with citations, and manages leave requests and balances. It is a learning and
portfolio project covering the full stack: React, Spring Boot, PostgreSQL with pgvector,
Spring AI (embeddings, RAG, tool calling), evaluation, observability, Docker and CI.

The RAG pipeline is inspired by a public Spring AI tutorial; all code in this repository
is written from scratch, and the policy document is fictional.

## Stack

| Area | Choice |
| --- | --- |
| Language and build | Java 21, Maven |
| Backend | Spring Boot, Spring Web, Validation, Spring Data JPA |
| Database | PostgreSQL with pgvector, Flyway migrations |
| AI | Spring AI with Ollama (free, local chat and embeddings), PDF Document Reader |
| Observability | Spring Boot Actuator, Micrometer |
| Tests | JUnit, Mockito, Testcontainers, evaluation harness |
| CI | GitHub Actions |
| Frontend | React, Vite, TypeScript |

## Run locally

Docker Desktop must be running. Docker Compose support starts the database for you.

```bash
./mvnw verify                 # build and run the tests (Testcontainers needs Docker)
./mvnw spring-boot:run        # start the app; the pgvector database starts automatically
```

You can also start the database by hand with `docker compose up -d`.

For the assistant, install Ollama (ollama.com) and pull the two models named in
`src/main/resources/application.yml`.

Health check: http://localhost:8080/actuator/health

## Roadmap

- [x] Project scaffold, database, CI
- [x] Leave tables and demo data (Flyway migrations)
- [x] Working-day, balance and overlap rules, with unit tests
- [x] Status rules (allowed status changes)
- [x] Leave request API (create, list, get, history, cancel, approve, reject, balances)
- [ ] Policy ingestion and semantic search with pgvector
- [ ] Assistant with cited answers and a refusal rule
- [ ] Assistant tools: balance, my requests, draft request
- [ ] Evaluation harness (20 labelled questions)
- [ ] React UI
- [ ] Metrics, Dockerfile, docker-compose, Kubernetes manifests

## API

| Method and path | Purpose |
| --- | --- |
| POST /api/leave-requests | Create a request (201) |
| GET /api/leave-requests | List requests |
| GET /api/leave-requests/{id} | Read one request |
| GET /api/leave-requests/{id}/history | Status history |
| POST /api/leave-requests/{id}/cancel, /approve, /reject | Change status |
| GET /api/balances | Allowance, used and remaining days |

Errors use problem responses: 400 for invalid input, 404 for an unknown request, 409 when a business
rule says no (balance, overlap, status). A Postman collection is in `docs/postman/`.

## Design decisions

### Why the assistant refuses in two layers

The assistant must only answer from the leave policy (rule R-1). My first idea was to refuse when the
best search result has a low similarity score. I tested that on five questions, and a score alone does
not work:

| Question | Answer in the policy? | Best score |
|---|---|---|
| can I cancel approved leave | yes | 0.81 |
| how many vacation days do I get | yes | 0.65 |
| what happens to days I did not use | yes | 0.63 |
| what is the parental leave | no | 0.59 |
| can I work from home | no | 0.51 |

The score measures how close the *topic* is, not whether an answer exists. The word "leave" makes every
question look close to the policy, and one right answer scored below one unanswerable question in
earlier tests. So there are two layers:

1. **Threshold (0.6).** A cheap first filter. If nothing passes, the app returns the fixed refusal
   sentence and does not call the model at all.
2. **Prompt.** The model may use only the retrieved excerpts, and must reply with the exact refusal
   sentence if they do not contain the answer.

The sample is small and the gap between 0.59 and 0.63 is thin, so the threshold is not trusted alone.
An evaluation set of questions with and without answers will measure how often each layer is right
(planned).

### Why the answer is streamed

On a CPU the model needs several seconds before the full answer exists. The answer endpoint
(`GET /api/policy/answer`) streams it as server-sent events (a `Flux` from Spring AI), so the first
words appear at once. The last event lists the policy sections the answer cites.
