package com.bankingsystem.deposit;

import com.bankingsystem.exception.InsufficientBalanceException;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.CustomerType;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.TransactionRepository;
import com.bankingsystem.service.BankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepositWithdrawTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private BankingService bankingService;

    private Customer owner;
    private Account account;

    @BeforeEach
    void setUp() {
        owner = new Customer("john", "john@example.com", "John", "Doe", CustomerType.INDIVIDUAL);
        account = new Account("CHK000001", AccountType.CHECKING, owner);
        account.credit(new BigDecimal("5000.00"));
    }

    @Test
    void shouldDepositSuccessfullyAndUpdateBalance() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        bankingService.deposit("john", "CHK000001", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("5300.00"), account.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void shouldHandleMultipleDeposits() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        bankingService.deposit("john", "CHK000001", new BigDecimal("100.00"));
        bankingService.deposit("john", "CHK000001", new BigDecimal("200.00"));
        bankingService.deposit("john", "CHK000001", new BigDecimal("50.50"));

        assertEquals(new BigDecimal("5350.50"), account.getBalance());
        verify(transactionRepository, times(3)).save(any(Transaction.class));
    }

    @Test
    void shouldRejectDepositWithZeroAmount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.deposit("john", "CHK000001", BigDecimal.ZERO));
        assertEquals(new BigDecimal("5000.00"), account.getBalance());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectDepositWithNegativeAmount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.deposit("john", "CHK000001", new BigDecimal("-100.00")));
        assertEquals(new BigDecimal("5000.00"), account.getBalance());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectDepositWithNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> bankingService.deposit("john", "CHK000001", null));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldWithdrawSuccessfullyAndUpdateBalance() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        bankingService.withdraw("john", "CHK000001", new BigDecimal("200.00"));

        assertEquals(new BigDecimal("4800.00"), account.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void shouldRejectWithdrawalExceedingBalance() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class,
                () -> bankingService.withdraw("john", "CHK000001", new BigDecimal("6000.00")));
        assertEquals(new BigDecimal("5000.00"), account.getBalance());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectWithdrawalWithExactBalance() {
        Account zeroBalanceAccount = new Account("ACC001", AccountType.SAVINGS, owner);
        zeroBalanceAccount.credit(new BigDecimal("100.00"));
        when(accountRepository.findByAccountNumberForUpdate("ACC001")).thenReturn(Optional.of(zeroBalanceAccount));

        assertThrows(InsufficientBalanceException.class,
                () -> bankingService.withdraw("john", "ACC001", new BigDecimal("100.01")));
        assertEquals(new BigDecimal("100.00"), zeroBalanceAccount.getBalance());
    }

    @Test
    void shouldRejectWithdrawalWithZeroAmount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.withdraw("john", "CHK000001", BigDecimal.ZERO));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectWithdrawalWithNegativeAmount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.withdraw("john", "CHK000001", new BigDecimal("-50.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectWithdrawalFromNonExistentAccount() {
        when(accountRepository.findByAccountNumberForUpdate("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.withdraw("john", "UNKNOWN", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectDepositToNonExistentAccount() {
        when(accountRepository.findByAccountNumberForUpdate("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.deposit("john", "UNKNOWN", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectWithdrawalWithAmountHavingMoreThanTwoDecimalPlaces() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.withdraw("john", "CHK000001", new BigDecimal("100.123")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}