package com.example.bank.transaction.client;

import com.example.bank.transaction.dto.AccountResponse;
import com.example.bank.transaction.dto.BalanceOperationRequest;
import com.example.bank.transaction.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://ACCOUNT-SERVICE").build();
    }

    public AccountResponse getAccount(UUID accountId) {
        try {
            return restClient.get()
                    .uri("/api/accounts/{id}", accountId)
                    .retrieve()
                    .body(AccountResponse.class);
        } catch (RestClientException ex) {
            throw new BusinessException("Unable to fetch account " + accountId + ": " + ex.getMessage());
        }
    }

    public void debit(UUID accountId, BalanceOperationRequest request) {
        callBalanceOperation(accountId, request, "debit");
    }

    public void credit(UUID accountId, BalanceOperationRequest request) {
        callBalanceOperation(accountId, request, "credit");
    }

    private void callBalanceOperation(UUID accountId, BalanceOperationRequest request, String operation) {
        try {
            restClient.post()
                    .uri("/api/accounts/{id}/" + operation, accountId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BusinessException("Account " + operation + " failed for " + accountId + ": " + ex.getMessage());
        }
    }
}
