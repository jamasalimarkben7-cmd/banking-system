package com.bankingsystem.authorization;

import com.bankingsystem.exception.SecurityException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTest {

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
    private Account johnsAccount;
    private Account janesAccount;

    @BeforeEach
    void setUp() {
        john = new Customer("john", "john@example.com", "John", "Doe", CustomerType.INDIVIDUAL);
        jane = new Customer("jane", "jane@example.com", "Jane", "Smith", CustomerType.INDIVIDUAL);

        johnsAccount = new Account("CHK000001", AccountType.CHECKING, john);
        johnsAccount.credit(new BigDecimal("5000.00"));

        janesAccount = new Account("CHK000002", AccountType.CHECKING, jane);
        janesAccount.credit(new BigDecimal("3000.00"));
    }

    @Test
    void shouldAllowDepositOnOwnAccount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnsAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        assertDoesNotThrow(() -> bankingService.deposit("john", "CHK000001", new BigDecimal("100.00")));
        verify(accountRepository).findByAccountNumberForUpdate("CHK000001");
    }

    @Test
    void shouldAllowWithdrawalOnOwnAccount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnsAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        assertDoesNotThrow(() -> bankingService.withdraw("john", "CHK000001", new BigDecimal("100.00")));
        verify(accountRepository).findByAccountNumberForUpdate("CHK000001");
    }

    @Test
    void shouldAllowTransferFromOwnAccount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnsAccount));
        when(accountRepository.findByAccountNumberForUpdate("CHK000002")).thenReturn(Optional.of(janesAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        assertDoesNotThrow(() -> bankingService.transfer("john", "CHK000001", "CHK000002", new BigDecimal("100.00")));
    }

    @Test
    void shouldRejectDepositToAnotherUsersAccount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000002")).thenReturn(Optional.of(janesAccount));

        assertThrows(SecurityException.class,
                () -> bankingService.deposit("john", "CHK000002", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectWithdrawalFromAnotherUsersAccount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000002")).thenReturn(Optional.of(janesAccount));

        assertThrows(SecurityException.class,
                () -> bankingService.withdraw("john", "CHK000002", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferFromAnotherUsersAccount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnsAccount));

        assertThrows(SecurityException.class,
                () -> bankingService.transfer("jane", "CHK000001", "CHK000002", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferToNonExistentAccount() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(johnsAccount));
        when(accountRepository.findByAccountNumberForUpdate("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.transfer("john", "CHK000001", "INVALID", new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectAccessToUnknownAccount() {
        when(accountRepository.findByAccountNumberForUpdate("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bankingService.deposit("john", "UNKNOWN", new BigDecimal("100.00")));
    }

    @Test
    void shouldRejectAccessToAccountOwnedByDifferentUser() {
        when(accountRepository.findByAccountNumberForUpdate("CHK000001")).thenReturn(Optional.of(janesAccount));

        assertThrows(SecurityException.class,
                () -> bankingService.deposit("john", "CHK000001", new BigDecimal("100.00")));
    }

    @Test
    void shouldRejectGetAccountsForUnauthorizedUser() {
        when(accountRepository.findByCustomerUsername("john")).thenReturn(List.of(johnsAccount));

        var accounts = bankingService.getAccountsForUser("john");
        assertEquals(1, accounts.size());
        assertEquals("CHK000001", accounts.get(0).getAccountNumber());
    }

    @Test
    void shouldRejectGetRecentTransactionsForNonExistentUser() {
        when(transactionRepository.findTop20ByAccountCustomerUsernameOrderByCreatedAtDesc("nonexistent"))
                .thenReturn(List.<Transaction>of());

        var transactions = bankingService.getRecentTransactions("nonexistent");
        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }
}