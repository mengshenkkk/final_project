package org.example.payment.controller;

import jakarta.validation.Valid;
import org.example.payment.dto.CreatePaymentRequest;
import org.example.payment.dto.FailPaymentRequest;
import org.example.payment.dto.PaymentHistoryResponse;
import org.example.payment.dto.PaymentResponse;
import org.example.payment.model.Payment;
import org.example.payment.model.PaymentStatus;
import org.example.payment.model.PaymentStatusHistory;
import org.example.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentService.CreatePaymentResult result = paymentService.createPayment(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(toResponse(result.payment()));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getById(@PathVariable String paymentId) {
        return toResponse(paymentService.getPayment(paymentId));
    }

    @GetMapping
    public List<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status) {
        return paymentService.listPayments(status).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{paymentId}/history")
    public List<PaymentHistoryResponse> history(@PathVariable String paymentId) {
        return paymentService.getHistory(paymentId).stream().map(this::toHistoryResponse).toList();
    }

    @PostMapping("/{paymentId}/validate")
    public PaymentResponse validate(@PathVariable String paymentId) {
        return toResponse(paymentService.validate(paymentId));
    }

    @PostMapping("/{paymentId}/send")
    public PaymentResponse send(@PathVariable String paymentId) {
        return toResponse(paymentService.send(paymentId));
    }

    @PostMapping("/{paymentId}/complete")
    public PaymentResponse complete(@PathVariable String paymentId) {
        return toResponse(paymentService.complete(paymentId));
    }

    @PostMapping("/{paymentId}/fail")
    public PaymentResponse fail(@PathVariable String paymentId, @Valid @RequestBody FailPaymentRequest request) {
        return toResponse(paymentService.fail(paymentId, request));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getSourceAccount(),
                payment.getDestinationAccount(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus(),
                payment.getErrorCode(),
                payment.getErrorMessage(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private PaymentHistoryResponse toHistoryResponse(PaymentStatusHistory history) {
        return new PaymentHistoryResponse(
                history.getId(),
                history.getPaymentId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getErrorCode(),
                history.getErrorMessage(),
                history.getTriggeredBy(),
                history.getChangedAt()
        );
    }
}



