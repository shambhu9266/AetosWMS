package com.example.backend.config;

import com.example.backend.model.Budget;
import com.example.backend.repo.BudgetRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedBudgets(BudgetRepository budgets) {
        return args -> {
            // IT Department
            budgets.findByDepartment("IT").orElseGet(() -> {
                Budget b = new Budget();
                b.setDepartment("IT");
                b.setTotalBudget(new BigDecimal("150000"));
                b.setRemainingBudget(new BigDecimal("120000"));
                return budgets.save(b);
            });
            
            // Sales Department
            budgets.findByDepartment("Sales").orElseGet(() -> {
                Budget b = new Budget();
                b.setDepartment("Sales");
                b.setTotalBudget(new BigDecimal("200000"));
                b.setRemainingBudget(new BigDecimal("85000"));
                return budgets.save(b);
            });
            
            // Management Department
            budgets.findByDepartment("Management").orElseGet(() -> {
                Budget b = new Budget();
                b.setDepartment("Management");
                b.setTotalBudget(new BigDecimal("100000"));
                b.setRemainingBudget(new BigDecimal("45000"));
                return budgets.save(b);
            });
            
            // Finance Department
            budgets.findByDepartment("Finance").orElseGet(() -> {
                Budget b = new Budget();
                b.setDepartment("Finance");
                b.setTotalBudget(new BigDecimal("300000"));
                b.setRemainingBudget(new BigDecimal("250000"));
                return budgets.save(b);
            });
        };
    }
}


