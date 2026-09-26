package com.bankingsystem.web;

import com.bankingsystem.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final BankingService bankingService;

    public DashboardController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("user", bankingService.getCustomer(username));
        model.addAttribute("accounts", bankingService.getAccountsForUser(username));
        model.addAttribute("transactions", bankingService.getRecentTransactions(username));
        return "dashboard";
    }

    @GetMapping("/accounts")
    public String accounts(Authentication authentication, Model model) {
        model.addAttribute("accounts", bankingService.getAccountsForUser(authentication.getName()));
        return "accounts";
    }

    @GetMapping("/transactions/new")
    public String transactionForm(@RequestParam(required = false) String type,
                                  Authentication authentication, Model model) {
        model.addAttribute("accounts", bankingService.getAccountsForUser(authentication.getName()));
        model.addAttribute("type", type == null ? "deposit" : type);
        model.addAttribute("transactionForm", new TransactionForm());
        return "transaction-form";
    }

    @PostMapping("/transactions/deposit")
    public String deposit(@Valid @ModelAttribute TransactionForm form, BindingResult result,
                          Authentication authentication, Model model) {
        if (result.hasErrors()) return transactionForm("deposit", authentication, model);
        return completeTransaction(() -> bankingService.deposit(authentication.getName(), form.getAccountNumber(), form.getAmount()),
                "deposit", authentication, model);
    }

    @PostMapping("/transactions/withdraw")
    public String withdraw(@Valid @ModelAttribute TransactionForm form, BindingResult result,
                           Authentication authentication, Model model) {
        if (result.hasErrors()) return transactionForm("withdraw", authentication, model);
        return completeTransaction(() -> bankingService.withdraw(authentication.getName(), form.getAccountNumber(), form.getAmount()),
                "withdraw", authentication, model);
    }

    @PostMapping("/transactions/transfer")
    public String transfer(@Valid @ModelAttribute TransactionForm form, BindingResult result,
                           Authentication authentication, Model model) {
        if (result.hasErrors()) return transactionForm("transfer", authentication, model);
        return completeTransaction(() -> bankingService.transfer(authentication.getName(), form.getAccountNumber(),
                        form.getDestinationAccountNumber(), form.getAmount()), "transfer", authentication, model);
    }

    private String completeTransaction(Runnable operation, String type, Authentication authentication, Model model) {
        try {
            operation.run();
            return "redirect:/dashboard?success=" + type;
        } catch (RuntimeException exception) {
            model.addAttribute("error", exception.getMessage());
            return transactionForm(type, authentication, model);
        }
    }
}
