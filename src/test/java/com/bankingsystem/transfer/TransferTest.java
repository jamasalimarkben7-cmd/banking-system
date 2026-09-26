package com.bankingsystem.transfer;

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
public class TransferTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private BankingService bankingService;

    private Customer john;
    private Customer jane;
    private Account johnChecking;
    private Account janeChecking;

    @BeforeEach
    void setUp() {
        john = new Customer("john", "john@example.com", "John", "Doe", CustomerType.INDIVIDUAL);
        jane = new Customer("jane", "jane@example.com", "Jane", "Smith", CustomerType.INDIVIDUAL);

        johnChecking = new Account("CHK000001", AccountType.CHECKING, john);
        johnChecking.credit(new BigDecimal("5000.00"));

        janeChecking = new Account("CHK000002", AccountType.CHECKING, jane);
        janeChecking.credit(new BigDecimal("3000.00"));
    }

    @Test
    void shouldTransferSuccessfullyWithAtomicBehavior() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnChecking));
        when(accountRepository.findByAccountNumberForUpdate("CHK000002")).thenReturn(Optional.of(janeChecking));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        bankingService.transfer("john", "CHK000001", "CHK000002", new BigDecimal("500.00"));

        assertEquals(new BigDecimal("4500.00"), johnChecking.getBalance());
        assertEquals(new BigDecimal("3500.00"), janeChecking.getBalance());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void shouldTransferZeroAmountRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "CHK000001", "CHK000002", BigDecimal.ZERO));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWithInsufficientBalanceAndRollback() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnChecking));
        when(accountRepository.findByAccountNumberForUpdate("CHK000002")).thenReturn(Optional.of(janeChecking));

        assertThrows(InsufficientBalanceException.class,
                () -> bankingService.transfer("john", "CHK000001", "CHK000002", new BigDecimal("6000.00")));

        assertEquals(new BigDecimal("5000.00"), johnChecking.getBalance());
        assertEquals(new BigDecimal("3000.00"), janeChecking.getBalance());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRollbackTransferWhenDestinationAccountNotFound() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnChecking));
        when(accountRepository.findByAccountNumberForUpdate("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "CHK000001", "INVALID", new BigDecimal("100.00")));

        assertEquals(new BigDecimal("5000.00"), johnChecking.getBalance());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "CHK000001", "CHK000001", new BigDecimal("100.00")));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWithNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "CHK000001", "CHK000002", null));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "CHK000001", "CHK000002", new BigDecimal("-100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWithAmountHavingMoreThanTwoDecimalPlaces() {
        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "CHK000001", "CHK000002", new BigDecimal("100.123")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferFromNonExistentSourceAccount() {
        when(accountRepository.findByAccountNumberForUpdate("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "INVALID", "CHK000002", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWhenBothAccountsDoNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "INVALID1", "INVALID2", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldPreserveBalancesOnFailedTransfer() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnChecking));
        when(accountRepository.findByAccountNumberForUpdate("CHK000002")).thenReturn(Optional.of(janeChecking));

        try {
            bankingService.transfer("john", "CHK000001", "CHK000002", new BigDecimal("99999.00"));
        } catch (InsufficientBalanceException expected) {
        }

        assertEquals(new BigDecimal("5000.00"), johnChecking.getBalance());
        assertEquals(new BigDecimal("3000.00"), janeChecking.getBalance());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldCreateTwoTransactionsForSuccessfulTransfer() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnChecking));
        when(accountRepository.findByAccountNumberForUpdate("CHK000002")).thenReturn(Optional.of(janeChecking));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        bankingService.transfer("john", "CHK000001", "CHK000002", new BigDecimal("250.00"));

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountRepository, times(1)).findByAccountNumberForUpdate("CHK000001");
        verify(accountRepository, times(1)).findByAccountNumberForUpdate("CHK000002");
    }
}