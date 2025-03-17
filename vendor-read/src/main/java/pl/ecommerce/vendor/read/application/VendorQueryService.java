package pl.ecommerce.vendor.read.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.vendor.read.api.dto.VendorResponse;
import pl.ecommerce.vendor.read.api.dto.VendorSummary;
import pl.ecommerce.vendor.read.infrastructure.repository.VendorReadRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorQueryService {

	private final VendorReadRepository vendorRepository;
	private final VendorMapper vendorMapper;

	public Mono<VendorResponse> findById(UUID id, TracingContext tracingContext) {
		log.info("Finding vendor by id: {}, traceId: {}", id, tracingContext.getTraceId());
		return vendorRepository.findById(id)
				.map(vendor -> {
					log.info("Vendor found: {}, traceId: {}", vendor.getId(), tracingContext.getTraceId());
					return vendorMapper.toVendorResponse(vendor, tracingContext);
				})
				.doOnNext(vendor -> log.debug("Mapped vendor: {}, traceId: {}", vendor.getId(), tracingContext.getTraceId()))
				.switchIfEmpty(Mono.defer(() -> {
					log.warn("Vendor not found with id: {}, traceId: {}", id, tracingContext.getTraceId());
					return Mono.empty();
				}));
	}

	public Mono<VendorResponse> findByEmail(String email, TracingContext tracingContext) {
		log.info("Finding vendor by email: {}, traceId: {}", email, tracingContext.getTraceId());
		return vendorRepository.findByEmail(email)
				.map(vendor -> {
					log.info("Vendor found by email: {}, traceId: {}", vendor.getId(), tracingContext.getTraceId());
					return vendorMapper.toVendorResponse(vendor, tracingContext);
				})
				.switchIfEmpty(Mono.defer(() -> {
					log.warn("Vendor not found with email: {}, traceId: {}", email, tracingContext.getTraceId());
					return Mono.empty();
				}));
	}

	public Mono<Page<VendorSummary>> findByStatus(VendorStatus status, TracingContext tracingContext, Pageable pageable) {
		log.info("Finding vendors by status: {}, page: {}, size: {}, traceId: {}",
				status, pageable.getPageNumber(), pageable.getPageSize(), tracingContext.getTraceId());

		return vendorRepository.countByStatus(status)
				.flatMap(total -> {
					if (total == 0) {
						return Mono.just(Page.empty(pageable));
					}

					return vendorRepository.findByStatus(status, pageable)
							.map(vendor -> vendorMapper.toVendorSummary(vendor, tracingContext))
							.collectList()
							.map(vendors -> new PageImpl<>(vendors, pageable, total));
				});
	}

	public Mono<Page<VendorSummary>> findAllActive(Pageable pageable, TracingContext tracingContext) {
		log.info("Finding all active vendors, page: {}, size: {}, traceId: {}",
				pageable.getPageNumber(), pageable.getPageSize(), tracingContext.getTraceId());

		return vendorRepository.countByStatus(VendorStatus.ACTIVE)
				.flatMap(total -> {
					if (total == 0) {
						return Mono.just(Page.empty(pageable));
					}

					return vendorRepository.findByStatus(VendorStatus.ACTIVE, pageable)
							.map(vendor -> vendorMapper.toVendorSummary(vendor, tracingContext))
							.collectList()
							.map(vendors -> new PageImpl<>(vendors, pageable, total));
				});
	}

	public Mono<Page<VendorSummary>> searchByName(String query, Pageable pageable, TracingContext tracingContext) {
		log.info("Searching vendors by name: {}, page: {}, size: {}, traceId: {}",
				query, pageable.getPageNumber(), pageable.getPageSize(), tracingContext.getTraceId());

		return vendorRepository.countByNameContainingIgnoreCaseOrBusinessNameContainingIgnoreCase(query, query)
				.flatMap(total -> {
					if (total == 0) {
						return Mono.just(Page.empty(pageable));
					}

					return vendorRepository.findByNameContainingIgnoreCaseOrBusinessNameContainingIgnoreCase(
									query, query, pageable)
							.map(vendor -> vendorMapper.toVendorSummary(vendor, tracingContext))
							.collectList()
							.map(vendors -> new PageImpl<>(vendors, pageable, total));
				});
	}

	public Mono<Page<VendorSummary>> findByCategory(UUID categoryId, Pageable pageable, TracingContext tracingContext) {
		log.info("Finding vendors by category: {}, page: {}, size: {}, traceId: {}",
				categoryId, pageable.getPageNumber(), pageable.getPageSize(), tracingContext.getTraceId());

		return vendorRepository.countByCategoriesCategoryId(categoryId)
				.flatMap(total -> {
					if (total == 0) {
						return Mono.just(Page.empty(pageable));
					}

					return vendorRepository.findByCategoriesCategoryId(categoryId, pageable)
							.map(vendor -> vendorMapper.toVendorSummary(vendor, tracingContext))
							.collectList()
							.map(vendors -> new PageImpl<>(vendors, pageable, total));
				});
	}
}