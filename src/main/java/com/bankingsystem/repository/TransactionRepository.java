package com.bankingsystem.repository;

import com.bankingsystem.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTop20ByAccountCustomerUsernameOrderByCreatedAtDesc(String username);
}
