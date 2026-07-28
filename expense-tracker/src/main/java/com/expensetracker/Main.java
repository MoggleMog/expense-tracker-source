package com.expensetracker;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Personal Expense Tracker
 * A JavaFX desktop application for recording, viewing, and analyzing
 * personal expenses, with data persisted to a local CSV file.
 */
public class Main extends Application {

    private static final String[] CATEGORIES = {
            "Food", "Transportation", "Housing", "Utilities",
            "Entertainment", "Healthcare", "Shopping", "Education", "Other"
    };
    private static final String DATA_FILE = "data/expenses.csv";

    private final ExpenseManager manager = new ExpenseManager(DATA_FILE);
    private final ObservableList<Expense> tableData = FXCollections.observableArrayList();

    private TableView<Expense> table;
    private Label totalLabel;
    private Label statusLabel;
    private PieChart pieChart;
    private ComboBox<String> filterBox;
    private TextField searchField;

    // Form fields
    private DatePicker dateField;
    private ComboBox<String> categoryField;
    private TextField descriptionField;
    private TextField amountField;
    private Button saveButton;
    private Button clearButton;
    private Expense editingExpense = null;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setId("root");

        root.setTop(buildHeader());
        root.setLeft(buildForm());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        refreshTable();
        refreshSummary();

        Scene scene = new Scene(root, 1150, 700);
        scene.getStylesheets().add(getClass().getResource("/com/expensetracker/styles.css").toExternalForm());

        stage.setTitle("Personal Expense Tracker");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(600);
        stage.show();
    }

    // ---------------------------------------------------------------
    // Header
    // ---------------------------------------------------------------
    private Node buildHeader() {
        Label title = new Label("Personal Expense Tracker");
        title.setId("app-title");

        Label subtitle = new Label("Track where your money goes, one expense at a time.");
        subtitle.setId("app-subtitle");

        VBox titleBox = new VBox(2, title, subtitle);

        HBox header = new HBox(titleBox);
        header.setId("header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 25, 20, 25));
        return header;
    }

    // ---------------------------------------------------------------
    // Left: Add / Edit form
    // ---------------------------------------------------------------
    private Node buildForm() {
        Label formTitle = new Label("Add New Expense");
        formTitle.setId("section-title");

        dateField = new DatePicker(LocalDate.now());
        dateField.setMaxWidth(Double.MAX_VALUE);

        categoryField = new ComboBox<>(FXCollections.observableArrayList(CATEGORIES));
        categoryField.setEditable(true);
        categoryField.setPromptText("Select or type a category");
        categoryField.setMaxWidth(Double.MAX_VALUE);

        descriptionField = new TextField();
        descriptionField.setPromptText("e.g. Weekly groceries");

        amountField = new TextField();
        amountField.setPromptText("e.g. 45.99");

        saveButton = new Button("Add Expense");
        saveButton.setId("primary-button");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(e -> handleSave());

        clearButton = new Button("Clear");
        clearButton.setId("secondary-button");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(e -> clearForm());

        HBox buttonRow = new HBox(10, saveButton, clearButton);
        HBox.setHgrow(saveButton, Priority.ALWAYS);
        HBox.setHgrow(clearButton, Priority.ALWAYS);

        VBox form = new VBox(10,
                formTitle,
                labeled("Date", dateField),
                labeled("Category", categoryField),
                labeled("Description", descriptionField),
                labeled("Amount ($)", amountField),
                buttonRow
        );
        form.setId("form-panel");
        form.setPadding(new Insets(20));
        form.setPrefWidth(280);
        return form;
    }

    private VBox labeled(String labelText, Control control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        return new VBox(4, label, control);
    }

    private void handleSave() {
        String category = categoryField.getEditor().getText();
        if (category == null || category.isBlank()) {
            category = categoryField.getValue();
        }
        String description = descriptionField.getText();
        String amountText = amountField.getText();
        LocalDate date = dateField.getValue();

        StringBuilder errors = new StringBuilder();
        if (date == null) errors.append("- Please select a valid date.\n");
        if (category == null || category.isBlank()) errors.append("- Please select or enter a category.\n");
        if (description == null || description.isBlank()) errors.append("- Please enter a description.\n");

        double amount = 0;
        try {
            amount = Double.parseDouble(amountText.trim());
            if (amount <= 0) {
                errors.append("- Amount must be greater than zero.\n");
            }
        } catch (NumberFormatException | NullPointerException ex) {
            errors.append("- Amount must be a valid positive number.\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Please fix the following:", errors.toString());
            return;
        }

        if (editingExpense == null) {
            manager.addExpense(date, category.trim(), description.trim(), amount);
            setStatus("Expense added successfully.");
        } else {
            manager.updateExpense(editingExpense.getId(), date, category.trim(), description.trim(), amount);
            setStatus("Expense updated successfully.");
        }

        clearForm();
        refreshTable();
        refreshSummary();
    }

    private void clearForm() {
        editingExpense = null;
        dateField.setValue(LocalDate.now());
        categoryField.getEditor().clear();
        categoryField.setValue(null);
        descriptionField.clear();
        amountField.clear();
        saveButton.setText("Add Expense");
    }

    private void loadExpenseIntoForm(Expense expense) {
        editingExpense = expense;
        dateField.setValue(expense.getDate());
        categoryField.setValue(expense.getCategory());
        descriptionField.setText(expense.getDescription());
        amountField.setText(String.valueOf(expense.getAmount()));
        saveButton.setText("Update Expense");
    }

    // ---------------------------------------------------------------
    // Center: filter bar + table
    // ---------------------------------------------------------------
    private Node buildCenter() {
        Label sectionTitle = new Label("All Expenses");
        sectionTitle.setId("section-title");

        filterBox = new ComboBox<>();
        filterBox.getItems().add("All");
        filterBox.getItems().addAll(CATEGORIES);
        filterBox.setValue("All");
        filterBox.setOnAction(e -> refreshTable());

        searchField = new TextField();
        searchField.setPromptText("Search description or category...");
        searchField.textProperty().addListener((obs, old, val) -> refreshTable());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setId("danger-button");
        deleteButton.setOnAction(e -> handleDelete());

        Button editButton = new Button("Edit Selected");
        editButton.setId("secondary-button");
        editButton.setOnAction(e -> handleEdit());

        Button summaryButton = new Button("View Summary");
        summaryButton.setId("secondary-button");
        summaryButton.setOnAction(e -> showSummaryDialog());

        HBox toolbar = new HBox(10,
                new Label("Filter:"), filterBox, searchField, editButton, deleteButton, summaryButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox center = new VBox(10, sectionTitle, toolbar, table);
        center.setPadding(new Insets(20));
        return center;
    }

    private TableView<Expense> buildTable() {
        TableView<Expense> tv = new TableView<>();
        tv.setItems(tableData);
        tv.setPlaceholder(new Label("No expenses yet. Add your first expense on the left."));

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedDate()));
        dateCol.setPrefWidth(110);

        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(130);

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(260);

        TableColumn<Expense, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getAmount())));
        amountCol.setPrefWidth(100);
        amountCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        tv.getColumns().addAll(dateCol, categoryCol, descCol, amountCol);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Double-click a row to edit it.
        tv.setRowFactory(t -> {
            TableRow<Expense> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadExpenseIntoForm(row.getItem());
                }
            });
            return row;
        });

        return tv;
    }

    private void handleEdit() {
        Expense selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "No Selection", "Please select an expense to edit.");
            return;
        }
        loadExpenseIntoForm(selected);
    }

    private void handleDelete() {
        Expense selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "No Selection", "Please select an expense to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this expense: " + selected.getDescription() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Deletion");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            manager.deleteExpense(selected.getId());
            refreshTable();
            refreshSummary();
            setStatus("Expense deleted.");
        }
    }

    // ---------------------------------------------------------------
    // Bottom: status bar with total
    // ---------------------------------------------------------------
    private Node buildStatusBar() {
        statusLabel = new Label("Ready.");
        statusLabel.setId("status-label");

        totalLabel = new Label();
        totalLabel.setId("total-label");

        HBox bar = new HBox(statusLabel, spacer(), totalLabel);
        bar.setId("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 10, 20));
        return bar;
    }

    private Node spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    // ---------------------------------------------------------------
    // Summary dialog: pie chart of spending by category
    // ---------------------------------------------------------------
    private void showSummaryDialog() {
        Map<String, Double> totals = manager.getTotalsByCategory();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Spending Summary");

        Label heading = new Label("Spending by Category");
        heading.setId("section-title");

        if (totals.isEmpty()) {
            Label empty = new Label("No expenses recorded yet.");
            VBox box = new VBox(15, heading, empty);
            box.setPadding(new Insets(20));
            box.setAlignment(Pos.CENTER);
            dialog.setScene(new Scene(box, 350, 200));
            dialog.getScene().getStylesheets().add(getClass().getResource("/com/expensetracker/styles.css").toExternalForm());
            dialog.show();
            return;
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey() + " ($" + String.format("%.2f", entry.getValue()) + ")", entry.getValue()));
        }
        pieChart = new PieChart(pieData);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);

        Label totalLine = new Label(String.format("Total Spending: $%.2f", manager.getTotalSpending()));
        totalLine.setId("total-label");

        VBox box = new VBox(15, heading, pieChart, totalLine);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);

        Scene scene = new Scene(box, 480, 480);
        scene.getStylesheets().add(getClass().getResource("/com/expensetracker/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void refreshTable() {
        String category = filterBox.getValue();
        String keyword = searchField.getText();

        List<Expense> filtered = manager.filterByCategory(category);
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            filtered = filtered.stream()
                    .filter(e -> e.getDescription().toLowerCase().contains(lower)
                            || e.getCategory().toLowerCase().contains(lower))
                    .toList();
        }
        tableData.setAll(filtered);
    }

    private void refreshSummary() {
        double total = manager.getTotalSpending();
        totalLabel.setText(String.format("Total Spending: $%.2f", total));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
