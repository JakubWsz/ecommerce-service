CREATE INDEX idx_event_store_data_gin ON event_store USING GIN (event_data);

CREATE INDEX idx_event_store_type_timestamp
    ON event_store(aggregate_type, event_timestamp);

CREATE OR REPLACE FUNCTION refresh_customer_views()
    RETURNS TRIGGER AS $$
BEGIN

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_refresh_customer_views
    AFTER INSERT ON event_store
    FOR EACH ROW
    WHEN (NEW.aggregate_type = 'Customer')
EXECUTE FUNCTION refresh_customer_views();

CREATE OR REPLACE FUNCTION create_customer_snapshot()
    RETURNS TRIGGER AS $$
DECLARE
    events_count INTEGER;
    snapshot_threshold INTEGER := 10;
    customer_state JSONB;
BEGIN
    SELECT COUNT(*) INTO events_count
    FROM event_store
    WHERE aggregate_id = NEW.aggregate_id
      AND deleted = false;

    IF events_count % snapshot_threshold = 0 THEN
        customer_state := get_customer_state(NEW.aggregate_id);

        INSERT INTO customer_snapshots (
            customer_id,
            version,
            snapshot_data
        ) VALUES (
                     NEW.aggregate_id,
                     NEW.version,
                     customer_state
                 );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_create_customer_snapshot
    AFTER INSERT ON event_store
    FOR EACH ROW
    WHEN (NEW.aggregate_type = 'Customer')
EXECUTE FUNCTION create_customer_snapshot();

CREATE INDEX idx_customer_snapshots_version
    ON customer_snapshots(version);