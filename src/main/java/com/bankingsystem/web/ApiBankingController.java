package com.bankingsystem.web;

import com.bankingsystem.dto.api.SuccessResponse;
import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.service.JdbcBankingService;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banking")
public class ApiBankingController {

    private final JdbcBankingService bankingService;

    public ApiBankingController(JdbcBankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<SuccessResponse> deposit(@Valid @RequestBody TransactionRequest request, Authentication authentication) {
        Transaction transaction = bankingService.deposit(request.getAccountNumber(), request.getAmount(), authentication);
        TransactionResponse response = toTransactionResponse(transaction);
        return ResponseEntity.ok(new SuccessResponse(200, "Deposit successful", response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<SuccessResponse> withdraw(@Valid @RequestBody TransactionRequest request, Authentication authentication) {
        Transaction transaction = bankingService.withdraw(request.getAccountNumber(), request.getAmount(), authentication);
        TransactionResponse response = toTransactionResponse(transaction);
        return ResponseEntity.ok(new SuccessResponse(200, "Withdrawal successful", response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<SuccessResponse> transfer(@Valid @RequestBody TransactionRequest request, Authentication authentication) {
        Transaction transaction = bankingService.transfer(request.getAccountNumber(), request.getDestinationAccountNumber(), request.getAmount(), authentication);
        TransactionResponse response = toTransactionResponse(transaction);
        return ResponseEntity.ok(new SuccessResponse(200, "Transfer successful", response));
    }

    @GetMapping("/accounts")
    public ResponseEntity<SuccessResponse> getAccounts(Authentication authentication) {
        List<Account> accounts = bankingService.getAccountsForUser(authentication.getName());
        List<AccountResponse> response = accounts.stream().map(this::toAccountResponse).collect(Collectors.toList());
        return ResponseEntity.ok(new SuccessResponse(200, "Accounts retrieved", response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<SuccessResponse> getTransactions(Authentication authentication) {
        List<Transaction> transactions = bankingService.getRecentTransactions(authentication.getName());
        List<TransactionResponse> response = transactions.stream().map(this::toTransactionResponse).collect(Collectors.toList());
        return ResponseEntity.ok(new SuccessResponse(200, "Transactions retrieved", response));
    }

    @GetMapping("/profile")
    public ResponseEntity<SuccessResponse> getProfile(Authentication authentication) {
        com.bankingsystem.model.User user = bankingService.getUser(authentication.getName());
        com.bankingsystem.dto.response.CustomerResponse response = toCustomerResponse(user);
        return ResponseEntity.ok(new SuccessResponse(200, "Profile retrieved", response));
    }

    private AccountResponse toAccountResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountType(account.getAccountType().name());
        response.setBalance(account.getBalance());
        response.setOwnerUsername(account.getCustomer().getUsername());
        return response;
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAccountNumber(transaction.getAccount().getAccountNumber());
        response.setType(transaction.getType().name());
        response.setAmount(transaction.getAmount());
        response.setCreatedAt(transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
        response.setBalanceAfter(transaction.getBalanceAfter());
        return response;
    }

    private com.bankingsystem.dto.response.CustomerResponse toCustomerResponse(com.bankingsystem.model.User user) {
        com.bankingsystem.dto.response.CustomerResponse response = new com.bankingsystem.dto.response.CustomerResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setCustomerType(com.bankingsystem.model.CustomerType.valueOf(user.getRole().name()));
        return response;
    }
}