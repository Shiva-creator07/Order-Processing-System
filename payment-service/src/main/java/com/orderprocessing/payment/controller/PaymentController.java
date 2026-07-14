package com.orderprocessing.payment.controller;

import com.orderprocessing.payment.model.PaymentTransaction;
import com.orderprocessing.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping
    public List<PaymentTransaction> getAll() {
        return paymentRepository.findAll();
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentTransaction> getByOrder(@PathVariable UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
