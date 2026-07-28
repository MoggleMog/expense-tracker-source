package com.expensetracker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single expense record.
 * This is a plain data model (POJO) used throughout the application.
 */
public class Expense {

    private static int nextId = 1;

    private final int id;
    private LocalDate date;
    private String category;
    private String description;
    private double amount;

    public Expense(LocalDate date, String category, String description, double amount) {
        this.id = nextId++;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
    }

    // Constructor used when reloading records from the CSV file so IDs are preserved.
    public Expense(int id, LocalDate date, String category, String description, double amount) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        if (id >= nextId) {
            nextId = id + 1;
        }
    }

    public int getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    /** Converts this expense into a single CSV line for persistence. */
    public String toCsvLine() {
        // Description may contain commas, so it is wrapped in quotes and internal quotes are escaped.
        String safeDescription = description == null ? "" : description.replace("\"", "\"\"");
        return id + "," + date + "," + escapeCsv(category) + ",\"" + safeDescription + "\"," + amount;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Parses a CSV line (as written by toCsvLine) back into an Expense object. */
    public static Expense fromCsvLine(String line) {
        // Simple CSV parser that respects quoted fields containing commas.
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());

        int id = Integer.parseInt(fields.get(0).trim());
        LocalDate date = LocalDate.parse(fields.get(1).trim());
        String category = fields.get(2).trim();
        String description = fields.get(3);
        double amount = Double.parseDouble(fields.get(4).trim());
        return new Expense(id, date, category, description, amount);
    }

    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
}
