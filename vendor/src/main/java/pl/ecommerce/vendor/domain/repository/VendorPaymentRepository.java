package pl.ecommerce.vendor.domain.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.vendor.domain.model.VendorPayment;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface VendorPaymentRepository extends ReactiveMongoRepository<VendorPayment, UUID> {

	Flux<VendorPayment> findByVendorId(UUID vendorId);

	Flux<VendorPayment> findByVendorIdAndStatus(UUID vendorId, VendorPayment.VendorPaymentStatus status);

	Flux<VendorPayment> findByVendorIdAndPaymentDateBetween(UUID vendorId, LocalDateTime start, LocalDateTime end);
}