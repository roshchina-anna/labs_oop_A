package ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainWindow extends JFrame {
    private final AppContext context = new AppContext();
    private SettingsDialog settingsDialog;
    private OperationsWindow operationsWindow;
    private DifferentiationWindow differentiationWindow;

    public MainWindow() {
        super("Табулированные функции");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUi();
        setSize(420, 180);
        setLocationRelativeTo(null);
    }

    private void initUi() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton settings = new JButton("Настройки");
        JButton operations = new JButton("Операции");
        JButton differentiation = new JButton("Дифференцирование");

        settings.addActionListener(e -> openSettings());
        operations.addActionListener(e -> openOperations());
        differentiation.addActionListener(e -> openDifferentiation());

        buttons.add(settings);
        buttons.add(operations);
        buttons.add(differentiation);

        content.add(buttons, BorderLayout.CENTER);
        setContentPane(content);
    }

    private void openSettings() {
        if (settingsDialog == null) {
            settingsDialog = new SettingsDialog(this, context);
        }
        settingsDialog.setVisible(true);
    }

    private void openOperations() {
        if (operationsWindow == null) {
            operationsWindow = new OperationsWindow(this, context);
        }
        operationsWindow.setVisible(true);
    }

    private void openDifferentiation() {
        if (differentiationWindow == null) {
            differentiationWindow = new DifferentiationWindow(this, context);
        }
        differentiationWindow.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}