# Personal Expense Tracker

A desktop personal expense tracker built with **Java** and **JavaFX**. It lets a user
record daily expenses, categorize them, search and filter their spending history,
and view a visual breakdown of spending by category — all backed by simple,
human-readable CSV file storage so data is never lost between runs.

**Student Name(s):** _[Your Name Here]_

## Features

- **Add expenses** with date, category, description, and amount
- **Edit expenses** in place (double-click any row, or select + "Edit Selected")
- **Delete expenses** with a confirmation prompt
- **Search** expenses live by description or category
- **Filter** the expense list by category
- **Spending summary** — total spending and a pie chart breakdown by category
- **Data persistence** — all expenses are automatically saved to `data/expenses.csv`
  and reloaded the next time the app starts
- **Input validation** — rejects empty fields, invalid dates, and non-positive or
  non-numeric amounts, with clear warning dialogs
- **Clean, professional UI** — consistent color palette, spacing, and a
  responsive layout built with JavaFX and CSS

## Technologies Used

- Java 17
- JavaFX 21 (Controls, Charts)
- Maven (build & dependency management)
- CSV file I/O for persistence (`java.nio.file`)

## Project Structure

```
expense-tracker/
├── pom.xml
├── README.md
├── data/
│   └── expenses.csv          # created automatically on first run
└── src/
    └── main/
        ├── java/com/expensetracker/
        │   ├── Main.java            # JavaFX application & UI
        │   ├── Expense.java         # expense data model
        │   └── ExpenseManager.java  # business logic + CSV persistence
        └── resources/com/expensetracker/
            └── styles.css           # application styling
```

## How to Run

### Prerequisites
- JDK 17 or later
- Maven 3.8+
(JavaFX itself does **not** need to be installed separately — Maven downloads it
automatically via the dependencies in `pom.xml`.)

### Steps

1. Clone the repository:
   ```
   git clone <your-repo-url>
   cd expense-tracker
   ```

2. Run the application with Maven:
   ```
   mvn clean javafx:run
   ```

That's it — the app window will open, and `data/expenses.csv` will be created
automatically the first time you add an expense.

### Building a runnable jar (optional)
```
mvn clean package
```

## Usage

1. Fill in the **Date**, **Category**, **Description**, and **Amount** fields on the
   left and click **Add Expense**.
2. Your expense appears instantly in the table on the right, and the running
   total updates at the bottom of the window.
3. Use the **Filter** dropdown or the **Search** box above the table to narrow
   down the list.
4. **Double-click** any row (or select it and click **Edit Selected**) to load it
   back into the form for editing.
5. Select a row and click **Delete Selected** to remove it (a confirmation
   dialog will appear first).
6. Click **View Summary** to open a pie chart showing how your spending is
   distributed across categories.

## License

This project was created for educational purposes as part of a course assignment.
