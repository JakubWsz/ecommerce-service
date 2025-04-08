package pl.ecommerce.customer.read.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException; // Do obsługi błędów
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.commons.tracing.TracedOperation;
import pl.ecommerce.customer.read.aplication.dto.CustomerResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerSummary;
import pl.ecommerce.customer.read.aplication.mapper.CustomerMapper;
import pl.ecommerce.customer.read.aplication.service.CustomerQueryService;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customers", description = "Read operations for customer data")
public class CustomerQueryController implements CustomerApi {

	private final CustomerQueryService customerQueryService;

	@Override
	@TracedOperation("getCustomerById")
	public Mono<ResponseEntity<CustomerResponse>> getCustomerById(UUID id) {
		log.info("Received request to get customer with id: {}", id);
		Mono<CustomerResponse> responseMono = customerQueryService.findById(id)
				.map(CustomerMapper::toCustomerResponse);
		return asResponseEntity(responseMono);
	}

	@Override
	@TracedOperation("getCustomerByEmail")
	public Mono<ResponseEntity<CustomerResponse>> getCustomerByEmail(String email) {
		log.info("Received request to get customer with email: {}", email);
		Mono<CustomerResponse> responseMono = customerQueryService.findByEmail(email)
				.map(CustomerMapper::toCustomerResponse);
		return asResponseEntity(responseMono);
	}

	@TracedOperation("getCustomerByStatus")
	@Override
	public Mono<ResponseEntity<Page<CustomerSummary>>> getCustomerByStatus(String status, int page, int size,
																		   String sortBy, String sortDir) {
		log.info("Received request to get customers by status. page={}, size={}", page, size);

		return Mono.fromCallable(() -> CustomerStatus.valueOf(status.toUpperCase()))
				.onErrorMap(IllegalArgumentException.class, e ->
						new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value: " + status)
				)
				.flatMap(validStatus -> {
					Pageable pageable = createPageable(page, size, sortBy, sortDir);
					Mono<Tuple2<List<CustomerReadModel>, Long>> dataMono =
							customerQueryService.findByStatus(validStatus, pageable);
					return buildPageResponse(dataMono, pageable);
				})
				.transform(this::asResponseEntity);
	}

	@Override
	@TracedOperation("getAllCustomers")
	public Mono<ResponseEntity<Page<CustomerSummary>>> getAllCustomers(int page, int size, String sortBy,
																	   String sortDir) {
		log.info("Received request to get all customers. page={}, size={}", page, size);
		Pageable pageable = createPageable(page, size, sortBy, sortDir);
		Mono<Tuple2<List<CustomerReadModel>, Long>> dataMono = customerQueryService.findAllActive(pageable);

		return buildPageResponse(dataMono, pageable)
				.transform(this::asResponseEntity);
	}

	@Override
	@TracedOperation("searchCustomers")
	public Mono<ResponseEntity<Page<CustomerSummary>>> searchCustomers(String query, int page, int size) {
		log.info("Received request to search customers with query: {}", query);
		Pageable pageable = PageRequest.of(page, size);
		Mono<Tuple2<List<CustomerReadModel>, Long>> dataMono = customerQueryService.searchByName(query, pageable);

		return buildPageResponse(dataMono, pageable)
				.transform(this::asResponseEntity);
	}

	private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		return PageRequest.of(page, size, Sort.by(direction, sortBy));
	}

	private Mono<Page<CustomerSummary>> buildPageResponse(
			Mono<Tuple2<List<CustomerReadModel>, Long>> dataAndCountMono,
			Pageable pageable) {

		return dataAndCountMono.map(tuple -> mapToPage(tuple.getT1(), pageable, tuple.getT2()));
	}

	private Page<CustomerSummary> mapToPage(List<CustomerReadModel> models, Pageable pageable, Long count) {
		List<CustomerSummary> summaries = models.stream()
				.map(CustomerMapper::toCustomerSummary)
				.collect(Collectors.toList());
		return new PageImpl<>(summaries, pageable, count);
	}

	private <T> Mono<ResponseEntity<T>> asResponseEntity(Mono<T> resultMono) {
		return resultMono
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
}