package com.bankingsystem.service;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.exception.InsufficientBalanceException;
import com.bankingsystem.exception.SecurityException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public BankingService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsForUser(String username) {
        return accountRepository.findByCustomerUsername(username);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getRecentTransactions(String username) {
        return transactionRepository.findTop20ByAccountCustomerUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public void deposit(String username, String accountNumber, BigDecimal amount) {
        Account account = getOwnedAccountForUpdate(username, accountNumber);
        validateAmount(amount);
        account.credit(amount);
        transactionRepository.save(new Transaction(TransactionType.DEPOSIT, amount, account, null, accountNumber));
    }

    @Transactional
    public void withdraw(String username, String accountNumber, BigDecimal amount) {
        Account account = getOwnedAccountForUpdate(username, accountNumber);
        validateAmount(amount);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds");
        }
        account.debit(amount);
        transactionRepository.save(new Transaction(TransactionType.WITHDRAWAL, amount, account, accountNumber, null));
    }

    @Transactional
    public void transfer(String username, String sourceAccountNumber, String destinationAccountNumber,
                         BigDecimal amount) {
        if (Objects.equals(sourceAccountNumber, destinationAccountNumber)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        validateAmount(amount);
        Account source = getOwnedAccountForUpdate(username, sourceAccountNumber);
        Account destination = accountRepository.findByAccountNumberForUpdate(destinationAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Destination account was not found"));
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds");
        }
        source.debit(amount);
        destination.credit(amount);
        transactionRepository.save(new Transaction(TransactionType.TRANSFER, amount, source,
                sourceAccountNumber, destinationAccountNumber));
        transactionRepository.save(new Transaction(TransactionType.TRANSFER, amount, destination,
                sourceAccountNumber, destinationAccountNumber));
    }

    private Account getOwnedAccountForUpdate(String username, String accountNumber) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        if (!account.getCustomer().getUsername().equals(username)) {
            throw new SecurityException("You do not have access to this account");
        }
        return account;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException("Amount must be positive and have at most two decimal places");
        }
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(String username) {
        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found"));
    }
}
