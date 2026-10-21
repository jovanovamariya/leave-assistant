-- Leave tables for the HR Leave Assistant (see data model in the project doc).
-- One demo employee, so there is no employee table yet.

CREATE TABLE leave_request (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    leave_type   VARCHAR(20)  NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    working_days INTEGER      NOT NULL,
    reason       VARCHAR(500),
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_leave_request_type   CHECK (leave_type IN ('ANNUAL', 'SICK')),
    CONSTRAINT ck_leave_request_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_leave_request_dates  CHECK (end_date >= start_date),
    CONSTRAINT ck_leave_request_days   CHECK (working_days > 0)
);

-- Overlap checks (rule R-4) look up active requests by date range.
CREATE INDEX ix_leave_request_status_dates ON leave_request (status, start_date, end_date);

-- History of status changes (rule R-5). from_status is empty for the first event.
CREATE TABLE leave_request_event (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id  BIGINT      NOT NULL REFERENCES leave_request (id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_leave_event_from CHECK (from_status IS NULL OR from_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_leave_event_to   CHECK (to_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX ix_leave_request_event_request ON leave_request_event (request_id);

-- Allowance per leave type (rule R-6: kept in the database, never decided by the assistant).
-- Remaining days = allowance_days - used_days.
CREATE TABLE leave_balance (
    leave_type     VARCHAR(20) PRIMARY KEY,
    allowance_days INTEGER     NOT NULL,
    used_days      INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT ck_leave_balance_type      CHECK (leave_type IN ('ANNUAL', 'SICK')),
    CONSTRAINT ck_leave_balance_allowance CHECK (allowance_days >= 0),
    CONSTRAINT ck_leave_balance_used      CHECK (used_days >= 0 AND used_days <= allowance_days)
);

-- Public holidays excluded from working-day counts (rule R-2).
CREATE TABLE public_holiday (
    holiday_date DATE         PRIMARY KEY,
    name         VARCHAR(100) NOT NULL
);
