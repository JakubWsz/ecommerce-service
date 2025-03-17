CREATE TABLE IF NOT EXISTS event_store (
                                           event_id UUID PRIMARY KEY,
                                           aggregate_id UUID NOT NULL,
                                           aggregate_type VARCHAR(255) NOT NULL,
                                           event_type VARCHAR(255) NOT NULL,
                                           version INT NOT NULL,
                                           event_timestamp TIMESTAMP NOT NULL,
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
CREATE INDEX IF NOT EXISTS idx_event_store_event_timestamp ON event_store (event_timestamp);
CREATE INDEX IF NOT EXISTS idx_event_store_trace_id ON event_store (trace_id);
CREATE INDEX IF NOT EXISTS idx_event_store_deleted ON event_store (deleted);

CREATE OR REPLACE VIEW vendor_email_view AS
WITH latest_events AS (
    SELECT
        e.aggregate_id as vendor_id,
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
    WHERE e.aggregate_type = 'Vendor'
      AND e.deleted = FALSE
      AND e.event_type = 'VendorRegisteredEvent'
)
SELECT
    vendor_id,
    email::TEXT AS email,
    version,
    event_timestamp,
    trace_id
FROM latest_events
WHERE rn = 1;

CREATE OR REPLACE VIEW vendor_current_state AS
WITH latest_status AS (
    SELECT
        e.aggregate_id as vendor_id,
        CASE
            WHEN e.event_type = 'VendorDeletedEvent' THEN 'DELETED'
            WHEN e.event_type = 'VendorStatusChangedEvent' THEN e.event_data->>'newStatus'
            WHEN e.event_type = 'VendorRegisteredEvent' THEN e.event_data->>'status'
            ELSE NULL
            END as status,
        e.version,
        e.event_timestamp,
        e.trace_id,
        ROW_NUMBER() OVER (
            PARTITION BY e.aggregate_id
            ORDER BY e.version DESC
            ) AS rn
    FROM event_store e
    WHERE e.aggregate_type = 'Vendor'
      AND e.deleted = FALSE
      AND (
        e.event_type = 'VendorRegisteredEvent'
            OR e.event_type = 'VendorStatusChangedEvent'
            OR e.event_type = 'VendorDeletedEvent'
        )
),
     latest_info AS (
         SELECT
             e.aggregate_id as vendor_id,
             COALESCE(
                     e.event_data->'changes'->>'name',
                     e.event_data->>'name'
             ) AS name,
             COALESCE(
                     e.event_data->'changes'->>'businessName',
                     e.event_data->>'businessName'
             ) AS business_name,
             e.event_data->>'email' AS email,
             e.version,
             ROW_NUMBER() OVER (
                 PARTITION BY e.aggregate_id
                 ORDER BY e.version DESC
                 ) AS rn
         FROM event_store e
         WHERE e.aggregate_type = 'Vendor'
           AND e.deleted = FALSE
           AND (
             e.event_type = 'VendorRegisteredEvent'
                 OR e.event_type = 'VendorUpdatedEvent'
             )
     ),
     latest_verification AS (
         SELECT
             e.aggregate_id as vendor_id,
             CASE
                 WHEN e.event_type = 'VendorVerificationCompletedEvent' THEN true
                 ELSE false
                 END as verified,
             e.version,
             ROW_NUMBER() OVER (
                 PARTITION BY e.aggregate_id
                 ORDER BY e.version DESC
                 ) AS rn
         FROM event_store e
         WHERE e.aggregate_type = 'Vendor'
           AND e.deleted = FALSE
           AND e.event_type = 'VendorVerificationCompletedEvent'
     )
SELECT
    s.vendor_id,
    i.email::TEXT AS email,
    i.name::TEXT AS name,
    i.business_name::TEXT AS business_name,
    COALESCE(v.verified, false) AS verified,
    s.status,
    s.event_timestamp as last_updated,
    s.trace_id as last_trace_id
FROM latest_status s
         LEFT JOIN latest_info i ON s.vendor_id = i.vendor_id AND i.rn = 1
         LEFT JOIN latest_verification v ON s.vendor_id = v.vendor_id AND v.rn = 1
WHERE s.rn = 1;

CREATE OR REPLACE FUNCTION get_vendor_events(p_vendor_id UUID)
    RETURNS TABLE (
                      event_id UUID,
                      event_type VARCHAR(255),
                      version INT,
                      event_timestamp TIMESTAMP,
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
            e.event_timestamp,
            e.event_data,
            e.trace_id,
            e.span_id
        FROM event_store e
        WHERE e.aggregate_id = p_vendor_id
          AND e.aggregate_type = 'Vendor'
          AND e.deleted = false
        ORDER BY e.version ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW vendor_events_tracing AS
SELECT
    e.event_id,
    e.aggregate_id as vendor_id,
    e.event_type,
    e.version,
    e.event_timestamp,
    e.trace_id,
    e.span_id,
    e.user_id
FROM event_store e
WHERE e.aggregate_type = 'Vendor'
  AND e.deleted = false
ORDER BY e.event_timestamp DESC;

CREATE INDEX IF NOT EXISTS idx_event_store_vendor_email
    ON event_store ((event_data->>'email'))
    WHERE aggregate_type = 'Vendor'
        AND event_type = 'VendorRegisteredEvent';

CREATE INDEX IF NOT EXISTS idx_event_store_vendor_business_name
    ON event_store ((event_data->>'businessName'))
    WHERE aggregate_type = 'Vendor'
        AND (event_type = 'VendorRegisteredEvent' OR event_type = 'VendorUpdatedEvent');