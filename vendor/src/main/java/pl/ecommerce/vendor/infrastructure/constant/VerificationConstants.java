package pl.ecommerce.vendor.infrastructure.constant;

public interface VerificationConstants extends CommonErrorConstants, CommonLogConstants {
	String ERROR_DOCUMENT_NOT_FOUND = "Document not found: ";
	String ERROR_DOCUMENT_ALREADY_REVIEWED = "Document already reviewed";
	String ERROR_DOCUMENT_TYPE_REQUIRED = "Document type is required";
	String ERROR_DOCUMENT_URL_REQUIRED = "Document URL is required";

	String LOG_DOCUMENT_REVIEWED = "Document {} reviewed with status {}";
}