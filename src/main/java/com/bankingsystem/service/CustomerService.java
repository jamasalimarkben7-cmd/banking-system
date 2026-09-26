package com.bankingsystem.service;

import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.Customer;
import com.bankingsystem.repository.CustomerRepository;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer registerCustomer(Customer customer) {
        if (customerRepository.existsByUsername(customer.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + customer.getUsername());
        }
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + customer.getEmail());
        }
        return customerRepository.save(customer);
    }

    public Customer getCustomer(@NonNull Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    public Customer getCustomerByUsername(String username) {
        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with username: " + username));
    }

    public List<Customer> getCustomers() {
        return customerRepository.findAll();
    }

    public List<Account> getAccountsForCustomer(@NonNull Long customerId) {
        Customer customer = getCustomer(customerId);
        return customer.getAccounts();
    }

    public Customer updateCustomer(@NonNull Long id, Customer updatedCustomer) {
        Customer existing = getCustomer(id);
        existing.setEmail(updatedCustomer.getEmail());
        existing.setFirstName(updatedCustomer.getFirstName());
        existing.setLastName(updatedCustomer.getLastName());
        existing.setCustomerType(updatedCustomer.getCustomerType());
        return customerRepository.save(existing);
    }

    public void deleteCustomer(@NonNull Long id) {
        customerRepository.deleteById(id);
    }
}