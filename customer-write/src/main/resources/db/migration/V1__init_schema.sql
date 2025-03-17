CREATE TABLE IF NOT EXISTS event_store (
                                           event_id UUID PRIMARY KEY,
                                           aggregate_id UUID NOT NULL,
                                           aggregate_type VARCHAR(255) NOT NULL,
                                           event_type VARCHAR(255) NOT NULL,
                                           version INT NOT NULL,
                                           event_timestamp  TIMESTAMP NOT NULL,
                                           event_data JSONB NOT NULL,
                                           trace_id VARCHAR(100),
                                           span_id VARCHAR(100),
                                           user_id VARCHAR(100),
                                           deleted BOOLEAN DEFAULT FALSE,
                                           CONSTRAINT unique_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_event_store_aggregate_id ON event_store (aggregate_id);
CREATE INDEX IF NOT EXISTS idx_event_store_aggregate_type ON event_store (aggregate_type);
CREATE INDEX IF NOT EXISTS idx_event_store_event_type ON event_store (event_type);
CREATE INDEX IF NOT EXISTS idx_event_store_event_timestamp  ON event_store (event_timestamp );
CREATE INDEX IF NOT EXISTS idx_event_store_trace_id ON event_store (trace_id);
CREATE INDEX IF NOT EXISTS idx_event_store_deleted ON event_store (deleted);

CREATE OR REPLACE VIEW customer_email_view AS
WITH latest_events AS (
    SELECT
        e.aggregate_id as customer_id,
        e.event_type,
        e.event_data->>'email' AS email,
        e.version,
        e.event_timestamp,
        e.trace_id,
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
    event_timestamp ,
    trace_id
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
        e.event_timestamp ,
        e.trace_id,
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
             e.event_data->>'email' AS email,
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
    s.event_timestamp  as last_updated,
    s.trace_id as last_trace_id
FROM latest_status s
         LEFT JOIN latest_email e ON s.customer_id = e.customer_id AND e.rn = 1
         LEFT JOIN latest_names n ON s.customer_id = n.customer_id AND n.rn = 1
WHERE s.rn = 1;

CREATE OR REPLACE FUNCTION get_customer_events(p_customer_id UUID)
    RETURNS TABLE (
                      event_id UUID,
                      event_type VARCHAR(255),
                      version INT,
                      event_timestamp  TIMESTAMP,
                      event_data JSONB,
                      trace_id VARCHAR(100),
                      span_id VARCHAR(100)
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT
            e.event_id,
            e.event_type,
            e.version,
            e.event_timestamp ,
            e.event_data,
            e.trace_id,
            e.span_id
        FROM event_store e
        WHERE e.aggregate_id = p_customer_id
          AND e.aggregate_type = 'Customer'
          AND e.deleted = false
        ORDER BY e.version ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW customer_events_tracing AS
SELECT
    e.event_id,
    e.aggregate_id as customer_id,
    e.event_type,
    e.version,
    e.event_timestamp ,
    e.trace_id,
    e.span_id,
    e.user_id
FROM event_store e
WHERE e.aggregate_type = 'Customer'
  AND e.deleted = false
ORDER BY e.event_timestamp  DESC;

CREATE INDEX IF NOT EXISTS idx_event_store_customer_email
    ON event_store ((event_data->>'email'))
    WHERE aggregate_type = 'Customer'
        AND event_type IN ('CustomerRegisteredEvent', 'CustomerEmailChangedEvent');