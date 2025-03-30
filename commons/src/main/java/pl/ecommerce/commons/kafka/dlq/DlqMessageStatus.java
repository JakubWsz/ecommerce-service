package pl.ecommerce.commons.kafka.dlq;

public enum DlqMessageStatus {

	PENDING_RETRY,

	RETRY_IN_PROGRESS,

	RETRY_SUCCEEDED,

	FAILED_PERMANENTLY
}