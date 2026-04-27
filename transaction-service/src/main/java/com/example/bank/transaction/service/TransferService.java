package com.example.bank.transaction.service;

import com.example.bank.transaction.client.AccountClient;
import com.example.bank.transaction.domain.Transfer;
import com.example.bank.transaction.domain.TransferStatus;
import com.example.bank.transaction.dto.AccountResponse;
import com.example.bank.transaction.dto.BalanceOperationRequest;
import com.example.bank.transaction.dto.CreateTransferRequest;
import com.example.bank.transaction.dto.TransferResponse;
import com.example.bank.transaction.exception.BusinessException;
import com.example.bank.transaction.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountClient accountClient;
    private final OutboxService outboxService;

    public TransferService(TransferRepository transferRepository, AccountClient accountClient, OutboxService outboxService) {
        this.transferRepository = transferRepository;
        this.accountClient = accountClient;
        this.outboxService = outboxService;
    }

    public TransferResponse createTransfer(CreateTransferRequest request) {
        return transferRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::toResponse)
                .orElseGet(() -> executeTransfer(request));
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> findAll() {
        return transferRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse findById(UUID id) {
        return transferRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Transfer not found: " + id));
    }

    private TransferResponse executeTransfer(CreateTransferRequest request) {
        validateRequest(request);

        AccountResponse source = accountClient.getAccount(request.sourceAccountId());
        AccountResponse target = accountClient.getAccount(request.targetAccountId());
        validateAccounts(request, source, target);

        Transfer transfer = new Transfer();
        transfer.setSourceAccountId(request.sourceAccountId());
        transfer.setTargetAccountId(request.targetAccountId());
        transfer.setAmount(request.amount());
        transfer.setCurrency(request.currency());
        transfer.setIdempotencyKey(request.idempotencyKey());
        transfer.setStatus(TransferStatus.PENDING);
        transfer = transferRepository.save(transfer);

        boolean debited = false;
        try {
            accountClient.debit(request.sourceAccountId(), new BalanceOperationRequest(request.amount(), "transfer-debit-" + transfer.getId()));
            debited = true;
            accountClient.credit(request.targetAccountId(), new BalanceOperationRequest(request.amount(), "transfer-credit-" + transfer.getId()));
            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setFailureReason(null);
            transfer = transferRepository.save(transfer);
            outboxService.createTransferCompletedEvent(transfer);
            return toResponse(transfer);
        } catch (Exception ex) {
            if (debited) {
                try {
                    accountClient.credit(request.sourceAccountId(), new BalanceOperationRequest(request.amount(), "transfer-compensation-" + transfer.getId()));
                } catch (Exception compensationEx) {
                    transfer.setFailureReason("Transfer failed and compensation also failed: " + compensationEx.getMessage());
                }
            }
            transfer.setStatus(TransferStatus.FAILED);
            if (transfer.getFailureReason() == null) {
                transfer.setFailureReason(ex.getMessage());
            }
            return toResponse(transferRepository.save(transfer));
        }
    }

    private void validateRequest(CreateTransferRequest request) {
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new BusinessException("Source and target accounts must be different");
        }
    }

    private void validateAccounts(CreateTransferRequest request, AccountResponse source, AccountResponse target) {
        if (!request.currency().equalsIgnoreCase(source.currency()) || !request.currency().equalsIgnoreCase(target.currency())) {
            throw new BusinessException("Transfer currency must match source and target account currencies");
        }
        if (!"ACTIVE".equalsIgnoreCase(source.status()) || !"ACTIVE".equalsIgnoreCase(target.status())) {
            throw new BusinessException("Both accounts must be ACTIVE to execute a transfer");
        }
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getTargetAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getIdempotencyKey(),
                transfer.getStatus(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }
}
