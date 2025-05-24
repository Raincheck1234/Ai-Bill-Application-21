package Controller;

import Service.TransactionService;
import model.MonthlySummary;

import java.awt.*;
import java.util.Map;
import java.util.List; // Import List
import java.util.ArrayList; // Import ArrayList
import java.util.HashMap; // Import HashMap
import java.util.Collections; // Import Collections for sorting
import java.util.Comparator; // Import Comparator

import javax.swing.*;

// Import XChart classes
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.SwingWrapper; // Might be needed for Swing components
import org.knowm.xchart.XChartPanel; // Use XChartPanel for Swing display
import org.knowm.xchart.style.Styler.LegendPosition; // For chart styling


/**
 * Panel to display transaction data visualizations using XChart.
 */
public class VisualizationPanel extends JPanel {

    private final TransactionService transactionService;

    private JComboBox<String> monthSelector;
    private JComboBox<String> chartTypeSelector;
    private JButton generateChartButton;
    private JPanel chartDisplayPanel;

    // Define string constants for chart types to avoid magic strings in comparisons
    private static final String SELECT_CHART_TYPE_PROMPT = "Select Chart Type";
    private static final String MONTHLY_EXPENSE_PIE_CHART = "Monthly Expense Category Pie Chart";
    private static final String MONTHLY_TREND_BAR_CHART = "Monthly Income/Expense Trend Bar Chart";
    private static final String SELECT_MONTH_PROMPT = "Select Month";


    /**
     * Constructor to inject the TransactionService.
     * @param transactionService The service to retrieve user transaction data.
     */
    public VisualizationPanel(TransactionService transactionService) {
        this.transactionService = transactionService;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Control Panel (Top) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        chartTypeSelector = new JComboBox<>(new String[]{SELECT_CHART_TYPE_PROMPT, MONTHLY_EXPENSE_PIE_CHART, MONTHLY_TREND_BAR_CHART});
        controlPanel.add(new JLabel("Chart Type:"));
        controlPanel.add(chartTypeSelector);

        monthSelector = new JComboBox<>();
        monthSelector.setEnabled(false);
        controlPanel.add(new JLabel("Select Month:"));
        controlPanel.add(monthSelector);


        generateChartButton = new JButton("Generate Chart");
        controlPanel.add(generateChartButton);

        add(controlPanel, BorderLayout.NORTH);


        // --- Chart Display Panel (Center) ---
        chartDisplayPanel = new JPanel(new BorderLayout());
        chartDisplayPanel.setBackground(Color.WHITE);
        add(chartDisplayPanel, BorderLayout.CENTER);


        // --- Action Listeners ---
        chartTypeSelector.addActionListener(e -> {
            String selectedType = (String) chartTypeSelector.getSelectedItem();
            boolean needsMonth = MONTHLY_EXPENSE_PIE_CHART.equals(selectedType);
            monthSelector.setEnabled(needsMonth);
            // Populate months only when Pie Chart is selected
            if (needsMonth) {
                populateMonthSelector();
            } else {
                monthSelector.removeAllItems(); // Clear months if not needed
                monthSelector.addItem(SELECT_MONTH_PROMPT); // Add default item back
            }
        });

        generateChartButton.addActionListener(e -> {
            generateAndDisplayChart();
        });

        // Initial state display
        displayPlaceholderChart("Please select a chart type and necessary parameters to generate the chart.");

    }

    /**
     * Populates the month selector combo box with months from available data.
     */
    private void populateMonthSelector() {
        monthSelector.removeAllItems();
        monthSelector.addItem(SELECT_MONTH_PROMPT);

        try {
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            if (summaries != null && !summaries.isEmpty()) {
                // Sort month identifiers chronologically
                summaries.keySet().stream().sorted().forEach(monthSelector::addItem);
            } else {
                JOptionPane.showMessageDialog(this, "No monthly transaction data found to generate charts.", "Insufficient Data", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("Error loading monthly summaries for month selector: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load month data!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Generates and displays the selected chart based on user selection using XChart.
     */
    private void generateAndDisplayChart() {
        String selectedChartType = (String) chartTypeSelector.getSelectedItem();
        String selectedMonth = (String) monthSelector.getSelectedItem();

        // Clear previous chart
        chartDisplayPanel.removeAll();
        chartDisplayPanel.revalidate();
        chartDisplayPanel.repaint();

        try {
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            if (summaries == null || summaries.isEmpty()) {
                displayPlaceholderChart("No monthly transaction data found to generate charts.");
                return;
            }

            if (MONTHLY_EXPENSE_PIE_CHART.equals(selectedChartType)) {
                if (selectedMonth == null || selectedMonth.equals(SELECT_MONTH_PROMPT) || selectedMonth.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please select a month to view.", "Information", JOptionPane.INFORMATION_MESSAGE);
                    displayPlaceholderChart("Please select a valid month to generate the pie chart.");
                    return;
                }
                // --- Generate Pie Chart ---
                MonthlySummary selectedMonthSummary = summaries.get(selectedMonth);
                if (selectedMonthSummary == null || selectedMonthSummary.getExpenseByCategory().isEmpty()) {
                    displayPlaceholderChart(selectedMonth + " has no expense category data.");
                    return;
                }

                System.out.println("Generating Pie Chart for " + selectedMonth + "...");
                PieChart chart = new PieChartBuilder()
                        .width(chartDisplayPanel.getWidth() > 0 ? chartDisplayPanel.getWidth() : 600) // Ensure width > 0
                        .height(chartDisplayPanel.getHeight() > 0 ? chartDisplayPanel.getHeight() : 400) // Ensure height > 0
                        .title(selectedMonth + " Expense Categories")
                        .build();

                selectedMonthSummary.getExpenseByCategory().entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                        .forEach(entry -> chart.addSeries(entry.getKey(), entry.getValue()));

                // Customize chart style
                chart.getStyler().setLegendPosition(LegendPosition.OutsideE);


                // Add the chart to the display panel
                XChartPanel<PieChart> chartPanel = new XChartPanel<>(chart);
                chartDisplayPanel.add(chartPanel, BorderLayout.CENTER);
                System.out.println("Pie Chart generated and displayed.");


            } else if (MONTHLY_TREND_BAR_CHART.equals(selectedChartType)) {
                // --- Generate Bar Chart (Category Chart) ---
                System.out.println("Generating Monthly Income/Expense Trend Bar Chart...");

                List<String> months = new ArrayList<>();
                List<Double> totalIncomes = new ArrayList<>();
                List<Double> totalExpenses = new ArrayList<>();

                List<String> sortedMonths = new ArrayList<>(summaries.keySet());
                Collections.sort(sortedMonths);

                for (String month : sortedMonths) {
                    MonthlySummary ms = summaries.get(month);
                    months.add(month);
                    totalIncomes.add(ms.getTotalIncome());
                    totalExpenses.add(ms.getTotalExpense());
                }

                CategoryChart chart = new CategoryChartBuilder()
                        .width(chartDisplayPanel.getWidth() > 0 ? chartDisplayPanel.getWidth() : 800) // Ensure width > 0
                        .height(chartDisplayPanel.getHeight() > 0 ? chartDisplayPanel.getHeight() : 500) // Ensure height > 0
                        .title("Monthly Income/Expense Trend")
                        .xAxisTitle("Month")
                        .yAxisTitle("Amount (CNY)")
                        .build();

                chart.addSeries("Total Income", months, totalIncomes);
                chart.addSeries("Total Expense", months, totalExpenses);

                // Customize chart style
                chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
                chart.getStyler().setStacked(false);

                // Add the chart to the display panel
                XChartPanel<CategoryChart> chartPanel = new XChartPanel<>(chart);
                chartDisplayPanel.add(chartPanel, BorderLayout.CENTER);
                System.out.println("Bar Chart generated and displayed.");


            } else {
                displayPlaceholderChart("Please select a chart type and necessary parameters to generate the chart.");
            }

        } catch (Exception e) {
            System.err.println("Error generating chart: " + selectedChartType);
            e.printStackTrace();
            displayPlaceholderChart("Failed to generate chart!\n" + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to generate chart!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            chartDisplayPanel.revalidate();
            chartDisplayPanel.repaint();
        }
    }

    /**
     * Helper method to display a placeholder message.
     */
    private void displayPlaceholderChart(String message) {
        // Clear previous content first
        chartDisplayPanel.removeAll();

        JLabel placeholderLabel = new JLabel(message, SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16)); // Using a common font name
        chartDisplayPanel.add(placeholderLabel, BorderLayout.CENTER);

        chartDisplayPanel.revalidate();
        chartDisplayPanel.repaint();
    }

    // Optional: Method to trigger initial setup when panel is displayed
    // Call this from MenuUI's ActionListener for the Visualization button
    public void refreshPanelData() {
        System.out.println("VisualizationPanel refreshPanelData called.");
        // Reset chart type selector to default on refresh
        chartTypeSelector.setSelectedItem(SELECT_CHART_TYPE_PROMPT);
        // Display initial instruction message
        displayPlaceholderChart("Please select a chart type and necessary parameters to generate the chart.");
    }

}