-- Record of ingested policy files. The hash lets a repeated ingest be recognised (rule R-7).
CREATE TABLE policy_document (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(200) NOT NULL UNIQUE,
    content_hash VARCHAR(64)  NOT NULL,
    chunk_count  INTEGER      NOT NULL,
    ingested_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_policy_document_chunks CHECK (chunk_count > 0)
);
