package com.expensetracker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Handles all business logic for managing expenses: adding, updating,
 * deleting, summarizing, and saving/loading data from a CSV file.
 *
 * Keeping this logic separate from the JavaFX UI code (Main.java) follows
 * the separation-of-concerns principle and makes the logic independently
 * testable.
 */
public class ExpenseManager {

    private static final String CSV_HEADER = "id,date,category,description,amount";
    private final Path dataFile;
    private final List<Expense> expenses = new ArrayList<>();

    public ExpenseManager(String filePath) {
        this.dataFile = Paths.get(filePath);
        loadFromFile();
    }

    public List<Expense> getAllExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public Expense addExpense(LocalDate date, String category, String description, double amount) {
        Expense expense = new Expense(date, category, description, amount);
        expenses.add(expense);
        saveToFile();
        return expense;
    }

    public boolean updateExpense(int id, LocalDate date, String category, String description, double amount) {
        for (Expense e : expenses) {
            if (e.getId() == id) {
                e.setDate(date);
                e.setCategory(category);
                e.setDescription(description);
                e.setAmount(amount);
                saveToFile();
                return true;
            }
        }
        return false;
    }

    public boolean deleteExpense(int id) {
        boolean removed = expenses.removeIf(e -> e.getId() == id);
        if (removed) {
            saveToFile();
        }
        return removed;
    }

    public double getTotalSpending() {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    /** Returns total spending grouped by category, sorted by highest spend first. */
    public Map<String, Double> getTotalsByCategory() {
        Map<String, Double> totals = new TreeMap<>();
        for (Expense e : expenses) {
            totals.merge(e.getCategory(), e.getAmount(), Double::sum);
        }
        return totals;
    }

    public List<Expense> filterByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            return getAllExpenses();
        }
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.getCategory().equalsIgnoreCase(category)) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    public List<Expense> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllExpenses();
        }
        String lower = keyword.toLowerCase();
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.getDescription().toLowerCase().contains(lower)
                    || e.getCategory().toLowerCase().contains(lower)) {
                result.add(e);
            }
        }
        return result;
    }

    private void loadFromFile() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    expenses.add(Expense.fromCsvLine(line));
                } catch (Exception parseError) {
                    System.err.println("Skipping malformed row: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load expenses file: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            if (dataFile.getParent() != null) {
                Files.createDirectories(dataFile.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
                writer.write(CSV_HEADER);
                writer.newLine();
                for (Expense e : expenses) {
                    writer.write(e.toCsvLine());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not save expenses file: " + e.getMessage());
        }
    }
}
