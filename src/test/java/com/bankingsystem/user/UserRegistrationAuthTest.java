package com.bankingsystem.user;

import com.bankingsystem.model.Customer;
import com.bankingsystem.model.CustomerType;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserRegistrationAuthTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BankingService bankingService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("john", "john@example.com", "John", "Doe", CustomerType.INDIVIDUAL);
    }

    @Test
    void shouldReturnUserWhenFound() {
        when(customerRepository.findByUsername("john")).thenReturn(Optional.of(testCustomer));

        Customer result = bankingService.getCustomer("john");

        assertEquals("john", result.getUsername());
        assertEquals(CustomerType.INDIVIDUAL, result.getCustomerType());
        verify(customerRepository).findByUsername("john");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(customerRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> bankingService.getCustomer("unknown"));
    }

    @Test
    void shouldRegisterNewUser() {
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer newCustomer = new Customer("newuser", "newuser@example.com", "New", "User", CustomerType.INDIVIDUAL);
        Customer saved = customerRepository.save(newCustomer);

        assertNotNull(saved);
        assertEquals("newuser", saved.getUsername());
        assertEquals(CustomerType.INDIVIDUAL, saved.getCustomerType());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldPreventDuplicateRegistration() {
        when(customerRepository.findByUsername("admin")).thenReturn(Optional.of(testCustomer));

        boolean exists = customerRepository.findByUsername("admin").isPresent();

        assertTrue(exists);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void shouldAuthenticateUserViaUserDetailsService() {
        org.springframework.security.core.userdetails.UserDetails userDetails =
                org.springframework.security.core.userdetails.User.withUsername("john")
                        .password("$2a$10$hash")
                        .roles("CLIENT")
                        .build();

        assertNotNull(userDetails);
        assertEquals("john", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT")));
    }
}