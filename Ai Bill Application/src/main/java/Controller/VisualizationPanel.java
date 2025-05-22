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

        chartTypeSelector = new JComboBox<>(new String[]{"请选择图表类型", "月度支出分类饼图", "月度收支趋势柱状图"});
        controlPanel.add(new JLabel("图表类型:"));
        controlPanel.add(chartTypeSelector);

        monthSelector = new JComboBox<>();
        monthSelector.setEnabled(false);
        controlPanel.add(new JLabel("选择月份:"));
        controlPanel.add(monthSelector);


        generateChartButton = new JButton("生成图表");
        controlPanel.add(generateChartButton);

        add(controlPanel, BorderLayout.NORTH);


        // --- Chart Display Panel (Center) ---
        chartDisplayPanel = new JPanel(new BorderLayout());
        chartDisplayPanel.setBackground(Color.WHITE);
        add(chartDisplayPanel, BorderLayout.CENTER);


        // --- Action Listeners ---
        chartTypeSelector.addActionListener(e -> {
            String selectedType = (String) chartTypeSelector.getSelectedItem();
            boolean needsMonth = "月度支出分类饼图".equals(selectedType);
            monthSelector.setEnabled(needsMonth);
            // Populate months only when Pie Chart is selected
            if (needsMonth) {
                populateMonthSelector();
            } else {
                monthSelector.removeAllItems(); // Clear months if not needed
                monthSelector.addItem("请选择月份"); // Add default item back
            }
        });

        generateChartButton.addActionListener(e -> {
            generateAndDisplayChart();
        });

        // Initial state display
        displayPlaceholderChart("请选择一种图表类型和必要参数来生成图表。");

        // Initial data loading if needed on panel creation - Let's load months when Pie Chart is selected
        // populateMonthSelector(); // Removed, triggered by chart type selection
    }

    /**
     * Populates the month selector combo box with months from available data.
     */
    private void populateMonthSelector() {
        monthSelector.removeAllItems();
        monthSelector.addItem("请选择月份");

        try {
            Map<String, MonthlySummary> summaries = transactionService.getMonthlyTransactionSummary();
            if (summaries != null && !summaries.isEmpty()) {
                // Sort month identifiers chronologically
                summaries.keySet().stream().sorted().forEach(monthSelector::addItem);
                // monthSelector.setEnabled(true); // Enabled by chartTypeSelector listener
            } else {
                // monthSelector.setEnabled(false); // Disabled by chartTypeSelector listener
                JOptionPane.showMessageDialog(this, "没有找到月度交易数据来生成图表。", "数据不足", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("Error loading monthly summaries for month selector: " + e.getMessage());
            // monthSelector.setEnabled(false); // Disabled by chartTypeSelector listener
            JOptionPane.showMessageDialog(this, "加载月份数据失败！\n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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
                displayPlaceholderChart("没有找到月度交易数据来生成图表。");
                return;
            }

            if ("月度支出分类饼图".equals(selectedChartType)) {
                if (selectedMonth == null || selectedMonth.equals("请选择月份") || selectedMonth.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "请选择要查看的月份。", "提示", JOptionPane.INFORMATION_MESSAGE);
                    displayPlaceholderChart("请选择一个有效的月份来生成饼图。");
                    return;
                }
                // --- Generate Pie Chart ---
                MonthlySummary selectedMonthSummary = summaries.get(selectedMonth);
                if (selectedMonthSummary == null || selectedMonthSummary.getExpenseByCategory().isEmpty()) {
                    displayPlaceholderChart(selectedMonth + " 月没有支出分类数据。");
                    return;
                }

                System.out.println("Generating Pie Chart for " + selectedMonth + "...");
                PieChart chart = new PieChartBuilder()
                        .width(chartDisplayPanel.getWidth())
                        .height(chartDisplayPanel.getHeight())
                        .title(selectedMonth + " 月支出分类")
                        .build();

                selectedMonthSummary.getExpenseByCategory().entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                        .forEach(entry -> chart.addSeries(entry.getKey(), entry.getValue()));

                // Customize chart style (optional - COMMENT OUT lines causing errors)
                // chart.getStyler().setLegendVisible(true); // Keep if it works
                // chart.getStyler().setAnnotationType(org.knowm.xchart.style.Styler.AnnotationType.LabelAndPercentage); // COMMENT OUT or FIX
                // chart.getStyler().setDonutTogether(true); // COMMENT OUT or FIX

                // Add the chart to the display panel
                XChartPanel<PieChart> chartPanel = new XChartPanel<>(chart);
                chartDisplayPanel.add(chartPanel, BorderLayout.CENTER);
                System.out.println("Pie Chart generated and displayed.");


            } else if ("月度收支趋势柱状图".equals(selectedChartType)) {
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
                        .width(chartDisplayPanel.getWidth())
                        .height(chartDisplayPanel.getHeight())
                        .title("月度收支趋势")
                        .xAxisTitle("月份")
                        .yAxisTitle("金额 (元)")
                        .build();

                chart.addSeries("总收入", months, totalIncomes);
                chart.addSeries("总支出", months, totalExpenses);

                // Customize chart style (optional - COMMENT OUT lines causing errors)
                // chart.getStyler().setLegendPosition(LegendPosition.OutsideS); // Keep if it works
                // chart.getStyler().setHasAnnotations(true); // COMMENT OUT or FIX
                // chart.getStyler().setStacked(false); // Keep if it works

                // Add the chart to the display panel
                XChartPanel<CategoryChart> chartPanel = new XChartPanel<>(chart);
                chartDisplayPanel.add(chartPanel, BorderLayout.CENTER);
                System.out.println("Bar Chart generated and displayed.");


            } else {
                displayPlaceholderChart("请选择一种图表类型和必要参数来生成图表。");
            }

        } catch (Exception e) {
            System.err.println("Error generating chart: " + selectedChartType);
            e.printStackTrace();
            displayPlaceholderChart("生成图表失败！\n" + e.getMessage());
            JOptionPane.showMessageDialog(this, "生成图表失败！\n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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
        placeholderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        chartDisplayPanel.add(placeholderLabel, BorderLayout.CENTER);

        chartDisplayPanel.revalidate();
        chartDisplayPanel.repaint();
    }

    // Optional: Method to trigger initial setup when panel is displayed
    // Call this from MenuUI's ActionListener for the Visualization button
    public void refreshPanelData() {
        System.out.println("VisualizationPanel refreshPanelData called.");
        // Populate month selector when the panel is visible
        // It's populated by the chartTypeSelector listener when Pie chart is selected.
        // So, no need to call populateMonthSelector here directly.

        // Reset chart type selector to default on refresh
        chartTypeSelector.setSelectedItem("请选择图表类型");
        // This action will trigger monthSelector logic

        // Display initial instruction message
        displayPlaceholderChart("请选择一种图表类型和必要参数来生成图表。");
    }

}