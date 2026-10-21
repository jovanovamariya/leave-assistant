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
- [ ] Status rules (allowed status changes, with history)
- [ ] Leave request API (create, list, edit, cancel, approve, reject)
- [ ] Policy ingestion and semantic search with pgvector
- [ ] Assistant with cited answers and a refusal rule
- [ ] Assistant tools: balance, my requests, draft request
- [ ] Evaluation harness (20 labelled questions)
- [ ] React UI
- [ ] Metrics, Dockerfile, docker-compose, Kubernetes manifests
