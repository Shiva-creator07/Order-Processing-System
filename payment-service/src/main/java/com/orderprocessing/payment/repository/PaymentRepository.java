package com.orderprocessing.payment.repository;

import com.orderprocessing.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByOrderId(UUID orderId);
}
