package com.example.bank.account.service;

import com.example.bank.account.domain.AccountStatus;
import com.example.bank.account.domain.BankAccount;
import com.example.bank.account.dto.AccountResponse;
import com.example.bank.account.dto.BalanceOperationRequest;
import com.example.bank.account.dto.CreateAccountRequest;
import com.example.bank.account.dto.UpdateAccountRequest;
import com.example.bank.account.exception.AccountNotFoundException;
import com.example.bank.account.exception.BusinessException;
import com.example.bank.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        accountRepository.findByAccountNumber(request.accountNumber())
                .ifPresent(existing -> {
                    throw new BusinessException("Account number already exists: " + request.accountNumber());
                });

        BankAccount account = new BankAccount();
        account.setAccountNumber(request.accountNumber());
        account.setOwnerName(request.ownerName());
        account.setBalance(request.initialBalance());
        account.setCurrency(request.currency());
        account.setStatus(request.status() == null ? AccountStatus.ACTIVE : request.status());
        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        return toResponse(load(id));
    }

    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request) {
        BankAccount account = load(id);
        account.setOwnerName(request.ownerName());
        if (request.status() != null) {
            account.setStatus(request.status());
        }
        return toResponse(accountRepository.save(account));
    }

    public void deleteAccount(UUID id) {
        BankAccount account = load(id);
        accountRepository.delete(account);
    }

    public AccountResponse debit(UUID id, BalanceOperationRequest request) {
        BankAccount account = load(id);
        validateActiveAccount(account);
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException("Insufficient balance on account " + account.getAccountNumber());
        }
        account.setBalance(account.getBalance().subtract(request.amount()));
        return toResponse(accountRepository.save(account));
    }

    public AccountResponse credit(UUID id, BalanceOperationRequest request) {
        BankAccount account = load(id);
        validateActiveAccount(account);
        account.setBalance(account.getBalance().add(request.amount()));
        return toResponse(accountRepository.save(account));
    }

    private BankAccount load(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));
    }

    private void validateActiveAccount(BankAccount account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account " + account.getAccountNumber() + " is not active");
        }
    }

    private AccountResponse toResponse(BankAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance().setScale(2, RoundingMode.HALF_UP),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
