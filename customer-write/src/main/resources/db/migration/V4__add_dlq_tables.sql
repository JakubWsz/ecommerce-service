CREATE TABLE IF NOT EXISTS dead_letter_queue (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 message_id VARCHAR(255) NOT NULL,
                                                 original_topic VARCHAR(255) NOT NULL,
                                                 message_key VARCHAR(255),
                                                 payload TEXT NOT NULL,
                                                 error_message TEXT,
                                                 status VARCHAR(50) NOT NULL,
                                                 retry_count INT DEFAULT 0,
                                                 notes TEXT,
                                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                                 CONSTRAINT uk_dlq_message_id UNIQUE (message_id)
);

CREATE INDEX idx_dlq_status ON dead_letter_queue(status);
CREATE INDEX idx_dlq_created_at ON dead_letter_queue(created_at);
CREATE INDEX idx_dlq_original_topic ON dead_letter_queue(original_topic);

CREATE TABLE IF NOT EXISTS dlq_retry_history (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 message_id VARCHAR(255) NOT NULL,
                                                 attempt_number INT NOT NULL,
                                                 retry_time TIMESTAMP WITH TIME ZONE NOT NULL,
                                                 result VARCHAR(50) NOT NULL,
                                                 error_message TEXT,
                                                 CONSTRAINT fk_retry_message_id FOREIGN KEY (message_id)
                                                     REFERENCES dead_letter_queue(message_id) ON DELETE CASCADE
);

CREATE INDEX idx_retry_message_id ON dlq_retry_history(message_id);

CREATE OR REPLACE VIEW dlq_summary_view AS
SELECT
    original_topic,
    status,
    COUNT(*) as message_count,
    MIN(created_at) as oldest_message,
    MAX(created_at) as newest_message,
    AVG(retry_count) as avg_retry_count
FROM dead_letter_queue
GROUP BY original_topic, status;