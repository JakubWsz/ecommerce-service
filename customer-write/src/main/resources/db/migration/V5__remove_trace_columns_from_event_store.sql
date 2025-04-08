DROP INDEX IF EXISTS idx_event_store_trace_id;

ALTER TABLE event_store
    DROP COLUMN IF EXISTS trace_id;

ALTER TABLE event_store
    DROP COLUMN IF EXISTS span_id;