package com.bankingsystem.service;

import com.bankingsystem.exception.InsufficientBalanceException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.exception.SecurityException;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.model.User;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.TransactionRepository;
import com.bankingsystem.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JdbcBankingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public JdbcBankingService(AccountRepository accountRepository, TransactionRepository transactionRepository, UserRepository userRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public Transaction deposit(String accountNumber, BigDecimal amount, Authentication authentication) {
        validateAmount(amount);
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
        validateOwnership(account, authentication);
        account.setBalance(account.getBalance().add(amount.stripTrailingZeros()));
        accountRepository.save(account);
        Transaction transaction = createTransaction(account, TransactionType.DEPOSIT, amount.stripTrailingZeros(), account.getBalance());
        return transactionRepository.save(transaction);
    }

    public Transaction withdraw(String accountNumber, BigDecimal amount, Authentication authentication) {
        validateAmount(amount);
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
        validateOwnership(account, authentication);
        if (account.getBalance().compareTo(amount.stripTrailingZeros()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount.stripTrailingZeros()));
        accountRepository.save(account);
        Transaction transaction = createTransaction(account, TransactionType.WITHDRAWAL, amount.stripTrailingZeros(), account.getBalance());
        return transactionRepository.save(transaction);
    }

    public Transaction transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, Authentication authentication) {
        validateAmount(amount);
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }
        Account fromAccount = accountRepository.findByAccountNumberForUpdate(fromAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + fromAccountNumber));
        Account toAccount = accountRepository.findByAccountNumberForUpdate(toAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + toAccountNumber));
        validateOwnership(fromAccount, authentication);
        if (fromAccount.getBalance().compareTo(amount.stripTrailingZeros()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount.stripTrailingZeros()));
        toAccount.setBalance(toAccount.getBalance().add(amount.stripTrailingZeros()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        Transaction outTransaction = createTransaction(fromAccount, TransactionType.TRANSFER_OUT, amount.stripTrailingZeros(), fromAccount.getBalance());
        Transaction inTransaction = createTransaction(toAccount, TransactionType.TRANSFER_IN, amount.stripTrailingZeros(), toAccount.getBalance());
        transactionRepository.save(outTransaction);
        return transactionRepository.save(inTransaction);
    }

    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public Customer getCustomer(String username) {
        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + username));
    }

    public List<Account> getAccountsForUser(String username) {
        return accountRepository.findByCustomerUsername(username);
    }

    public List<Transaction> getRecentTransactions(String username) {
        return transactionRepository.findTop20ByAccountCustomerUsernameOrderByCreatedAtDesc(username);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
    }

    private void validateOwnership(Account account, Authentication authentication) {
        if (!account.getCustomer().getUsername().equals(authentication.getName())) {
            throw new SecurityException("Unauthorized access to account");
        }
    }

    private Transaction createTransaction(Account account, TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {
        Transaction transaction = new Transaction(type, amount, account, account.getAccountNumber(), account.getAccountNumber());
        transaction.setBalanceAfter(balanceAfter);
        return transaction;
    }
}