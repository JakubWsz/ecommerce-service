CREATE TABLE IF NOT EXISTS event_store (
                                           event_id UUID PRIMARY KEY,
                                           aggregate_id UUID NOT NULL,
                                           aggregate_type VARCHAR(255) NOT NULL,
                                           event_type VARCHAR(255) NOT NULL,
                                           version INT NOT NULL,
                                           timestamp TIMESTAMP NOT NULL,
                                           correlation_id UUID,
                                           event_data JSONB NOT NULL,
                                           metadata JSONB,
                                           deleted BOOLEAN DEFAULT FALSE,

                                           CONSTRAINT unique_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_event_store_aggregate_id ON event_store (aggregate_id);
CREATE INDEX IF NOT EXISTS idx_event_store_aggregate_type ON event_store (aggregate_type);
CREATE INDEX IF NOT EXISTS idx_event_store_event_type ON event_store (event_type);
CREATE INDEX IF NOT EXISTS idx_event_store_timestamp ON event_store (timestamp);
CREATE INDEX IF NOT EXISTS idx_event_store_correlation_id ON event_store (correlation_id);

CREATE TABLE IF NOT EXISTS event_outbox (
                                            id UUID PRIMARY KEY,
                                            aggregate_id UUID NOT NULL,
                                            aggregate_type VARCHAR(255) NOT NULL,
                                            event_type VARCHAR(255) NOT NULL,
                                            event_data JSONB NOT NULL,
                                            timestamp TIMESTAMP NOT NULL,
                                            processed BOOLEAN DEFAULT FALSE,
                                            processing_attempts INT DEFAULT 0,
                                            last_attempt_timestamp TIMESTAMP,
                                            error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_event_outbox_processed ON event_outbox (processed, last_attempt_timestamp);

CREATE TABLE IF NOT EXISTS customer_snapshots (
                                                  customer_id UUID PRIMARY KEY,
                                                  version INT NOT NULL,
                                                  snapshot_data JSONB NOT NULL,
                                                  timestamp TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_snapshots_version ON customer_snapshots (version);

CREATE OR REPLACE VIEW customer_email_view AS
WITH latest_events AS (
    SELECT
        e.aggregate_id as customer_id,
        e.event_type,
        e.event_data->'email' AS email,
        e.version,
        e.timestamp,
        ROW_NUMBER() OVER (
            PARTITION BY e.aggregate_id
            ORDER BY e.version DESC
            ) AS rn
    FROM event_store e
    WHERE e.aggregate_type = 'Customer'
      AND e.deleted = FALSE
      AND (
        e.event_type = 'CustomerRegisteredEvent'
            OR e.event_type = 'CustomerEmailChangedEvent'
        )
)
SELECT
    customer_id,
    email::TEXT AS email,
    version,
    timestamp
FROM latest_events
WHERE rn = 1;

CREATE OR REPLACE VIEW customer_current_state AS
WITH latest_status AS (
    SELECT
        e.aggregate_id as customer_id,
        CASE
            WHEN e.event_type = 'CustomerDeletedEvent' THEN 'DELETED'
            WHEN e.event_type = 'CustomerDeactivatedEvent' THEN 'INACTIVE'
            WHEN e.event_type = 'CustomerReactivatedEvent' THEN 'ACTIVE'
            WHEN e.event_type = 'CustomerRegisteredEvent' THEN 'ACTIVE'
            ELSE NULL
            END as status,
        e.version,
        e.timestamp,
        ROW_NUMBER() OVER (
            PARTITION BY e.aggregate_id
            ORDER BY e.version DESC
            ) AS rn
    FROM event_store e
    WHERE e.aggregate_type = 'Customer'
      AND e.deleted = FALSE
      AND (
        e.event_type = 'CustomerRegisteredEvent'
            OR e.event_type = 'CustomerDeactivatedEvent'
            OR e.event_type = 'CustomerReactivatedEvent'
            OR e.event_type = 'CustomerDeletedEvent'
        )
),
     latest_email AS (
         SELECT
             e.aggregate_id as customer_id,
             e.event_data->'email' AS email,
             e.version,
             ROW_NUMBER() OVER (
                 PARTITION BY e.aggregate_id
                 ORDER BY e.version DESC
                 ) AS rn
         FROM event_store e
         WHERE e.aggregate_type = 'Customer'
           AND e.deleted = FALSE
           AND (
             e.event_type = 'CustomerRegisteredEvent'
                 OR e.event_type = 'CustomerEmailChangedEvent'
             )
     ),
     latest_names AS (
         SELECT
             e.aggregate_id as customer_id,
             COALESCE(
                     e.event_data->'changes'->>'firstName',
                     e.event_data->>'firstName'
             ) AS first_name,
             COALESCE(
                     e.event_data->'changes'->>'lastName',
                     e.event_data->>'lastName'
             ) AS last_name,
             e.version,
             ROW_NUMBER() OVER (
                 PARTITION BY e.aggregate_id
                 ORDER BY e.version DESC
                 ) AS rn
         FROM event_store e
         WHERE e.aggregate_type = 'Customer'
           AND e.deleted = FALSE
           AND (
             e.event_type = 'CustomerRegisteredEvent'
                 OR e.event_type = 'CustomerUpdatedEvent'
             )
     )
SELECT
    s.customer_id,
    e.email::TEXT AS email,
    n.first_name::TEXT AS first_name,
    n.last_name::TEXT AS last_name,
    s.status,
    s.timestamp as last_updated
FROM latest_status s
         LEFT JOIN latest_email e ON s.customer_id = e.customer_id AND e.rn = 1
         LEFT JOIN latest_names n ON s.customer_id = n.customer_id AND n.rn = 1
WHERE s.rn = 1;