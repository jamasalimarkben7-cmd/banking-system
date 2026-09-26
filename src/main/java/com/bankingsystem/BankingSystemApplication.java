package com.bankingsystem;

import com.bankingsystem.model.Customer;
import com.bankingsystem.model.CustomerType;
import com.bankingsystem.model.Role;
import com.bankingsystem.model.User;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class BankingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSystemApplication.class, args);
    }

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() throws UnknownHostException {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.setPort(8080);
        factory.setAddress(InetAddress.getByName("0.0.0.0"));
        return factory;
    }

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.findByUsername("admin").isPresent()) {
                User admin = new User("admin", passwordEncoder.encode("admin123"), Role.ADMIN);
                admin.setEmail("admin@example.com");
                admin.setFirstName("Admin");
                admin.setLastName("User");
                userRepository.save(admin);
                System.out.println("[INFO] Default admin user created: admin / admin123");
            }
            if (!customerRepository.findByUsername("admin").isPresent()) {
                Customer adminCustomer = new Customer("admin", "admin@example.com", "Admin", "User", CustomerType.INDIVIDUAL);
                customerRepository.save(adminCustomer);
                System.out.println("[INFO] Default admin customer profile created");
            }
        };
    }
}
