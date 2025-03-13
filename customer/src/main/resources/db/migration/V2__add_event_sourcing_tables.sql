
ALTER TABLE IF EXISTS event_store
    ADD COLUMN IF NOT EXISTS version INT;

CREATE INDEX IF NOT EXISTS idx_event_store_version ON event_store (version);

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
                                            error_message TEXT,

                                            CONSTRAINT fk_outbox_aggregate FOREIGN KEY (aggregate_id)
                                                REFERENCES event_store (aggregate_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_outbox_processed ON event_outbox (processed);
CREATE INDEX IF NOT EXISTS idx_outbox_timestamp ON event_outbox (timestamp);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_id ON event_outbox (aggregate_id);

CREATE TABLE IF NOT EXISTS customer_snapshots (
                                                  customer_id UUID PRIMARY KEY,
                                                  version INT NOT NULL,
                                                  snapshot_data JSONB NOT NULL,
                                                  timestamp TIMESTAMP NOT NULL,

                                                  CONSTRAINT fk_snapshot_customer FOREIGN KEY (customer_id)
                                                      REFERENCES event_store (aggregate_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_customer_snapshots_version ON customer_snapshots (version);

CREATE TABLE IF NOT EXISTS outbox_processor_state (
                                                      id VARCHAR(50) PRIMARY KEY,
                                                      last_processed_id UUID,
                                                      last_execution_time TIMESTAMP,
                                                      processing_status VARCHAR(20),
                                                      error_message TEXT,
                                                      metadata JSONB
);

CREATE OR REPLACE FUNCTION get_customer_events(p_customer_id UUID)
    RETURNS TABLE (
                      event_id UUID,
                      event_type VARCHAR(255),
                      version INT,
                      timestamp TIMESTAMP,
                      event_data JSONB,
                      correlation_id UUID
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT e.event_id, e.event_type, e.version, e.timestamp, e.event_data, e.correlation_id
        FROM event_store e
        WHERE e.aggregate_id = p_customer_id
          AND e.aggregate_type = 'Customer'
          AND e.deleted = false
        ORDER BY e.version ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW customer_events_view AS
SELECT
    e.event_id,
    e.aggregate_id as customer_id,
    e.event_type,
    e.version,
    e.timestamp,
    e.event_data->>'email' as email,
    e.event_data->'firstName' as first_name,
    e.event_data->'lastName' as last_name,
    e.correlation_id
FROM event_store e
WHERE e.aggregate_type = 'Customer'
  AND e.deleted = false
ORDER BY e.aggregate_id, e.version;

CREATE INDEX IF NOT EXISTS idx_event_store_customer_email
    ON event_store ((event_data->>'email'))
    WHERE aggregate_type = 'Customer' AND event_type IN ('CustomerRegisteredEvent', 'CustomerEmailChangedEvent');