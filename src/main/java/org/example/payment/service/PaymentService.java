package org.example.payment.service;

import org.example.payment.dto.CreatePaymentRequest;
import org.example.payment.dto.FailPaymentRequest;
import org.example.payment.exception.BusinessException;
import org.example.payment.exception.ErrorCode;
import org.example.payment.model.Payment;
import org.example.payment.model.PaymentStatus;
import org.example.payment.model.PaymentStatusHistory;
import org.example.payment.repository.PaymentRepository;
import org.example.payment.repository.PaymentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP");

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final PaymentStateMachine stateMachine;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository historyRepository,
            PaymentStateMachine stateMachine
    ) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public CreatePaymentResult createPayment(CreatePaymentRequest request) {
        validateCreateRequest(request);

        return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(existing -> new CreatePaymentResult(existing, false))
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setId(UUID.randomUUID().toString());
                    payment.setIdempotencyKey(request.idempotencyKey());
                    payment.setSourceAccount(request.sourceAccount());
                    payment.setDestinationAccount(request.destinationAccount());
                    payment.setReference(request.reference());
                    payment.setAmount(request.amount());
                    payment.setCurrency(request.currency());
                    payment.setStatus(PaymentStatus.CREATED);
                    payment.setCreatedAt(LocalDateTime.now());
                    payment.setUpdatedAt(payment.getCreatedAt());

                    paymentRepository.insert(payment);
                    addHistory(payment.getId(), null, PaymentStatus.CREATED, null, null, "API");

                    return new CreatePaymentResult(payment, true);
                });
    }

    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + paymentId));
    }

    public List<Payment> listPayments(PaymentStatus status) {
        if (status == null) {
            return paymentRepository.findAll();
        }
        return paymentRepository.findByStatus(status);
    }

    public List<PaymentStatusHistory> getHistory(String paymentId) {
        getPayment(paymentId);
        return historyRepository.findByPaymentId(paymentId);
    }

    @Transactional
    public Payment validate(String paymentId) {
        return transition(paymentId, PaymentStatus.VALIDATED, null, null, "API_VALIDATE");
    }

    @Transactional
    public Payment send(String paymentId) {
        return transition(paymentId, PaymentStatus.SENT, null, null, "API_SEND");
    }

    @Transactional
    public Payment complete(String paymentId) {
        return transition(paymentId, PaymentStatus.COMPLETED, null, null, "API_COMPLETE");
    }

    @Transactional
    public Payment fail(String paymentId, FailPaymentRequest request) {
        return transition(paymentId, PaymentStatus.FAILED, request.errorCode(), request.errorMessage(), "API_FAIL");
    }

    private void validateCreateRequest(CreatePaymentRequest request) {
        if (request.sourceAccount().equals(request.destinationAccount())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT, "sourceAccount and destinationAccount must be different");
        }
        if (!SUPPORTED_CURRENCIES.contains(request.currency())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENCY, "Unsupported currency: " + request.currency());
        }
    }

    private Payment transition(
            String paymentId,
            PaymentStatus targetStatus,
            String errorCode,
            String errorMessage,
            String triggeredBy
    ) {
        Payment payment = getPayment(paymentId);
        stateMachine.assertCanTransition(payment.getStatus(), targetStatus);

        PaymentStatus fromStatus = payment.getStatus();
        payment.setStatus(targetStatus);
        payment.setErrorCode(errorCode);
        payment.setErrorMessage(errorMessage);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.update(payment);

        addHistory(payment.getId(), fromStatus, targetStatus, errorCode, errorMessage, triggeredBy);
        return payment;
    }

    private void addHistory(
            String paymentId,
            PaymentStatus from,
            PaymentStatus to,
            String errorCode,
            String errorMessage,
            String triggeredBy
    ) {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPaymentId(paymentId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setErrorCode(errorCode);
        history.setErrorMessage(errorMessage);
        history.setTriggeredBy(triggeredBy);
        history.setChangedAt(LocalDateTime.now());
        historyRepository.insert(history);
    }

    public record CreatePaymentResult(Payment payment, boolean created) {
    }
}

