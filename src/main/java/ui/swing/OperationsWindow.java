package ui.swing;

import functions.TabulatedFunction;
import io.FunctionsIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;

public class OperationsWindow extends JDialog {
    private final AppContext context;
    private final TabulatedFunctionTableModel firstModel = new TabulatedFunctionTableModel(true);
    private final TabulatedFunctionTableModel secondModel = new TabulatedFunctionTableModel(true);
    private final TabulatedFunctionTableModel resultModel = new TabulatedFunctionTableModel(false);

    private TabulatedFunction first;
    private TabulatedFunction second;

    public OperationsWindow(MainWindow owner, AppContext context) {
        super(owner, "Операции", true);
        this.context = context;
        initUi();
        setSize(900, 500);
        setLocationRelativeTo(owner);
    }

    private void initUi() {
        JPanel content = new JPanel(new GridLayout(1, 3, 8, 0));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        content.add(buildOperandPanel("Первый операнд", firstModel, true));
        content.add(buildOperandPanel("Второй операнд", secondModel, false));
        content.add(buildResultPanel());

        setContentPane(content);
    }

    private JPanel buildOperandPanel(String title, TabulatedFunctionTableModel model, boolean firstOperand) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton fromTable = new JButton("Создать из таблицы");
        JButton fromFunction = new JButton("Создать из функции");
        JButton load = new JButton("Загрузить");
        JButton save = new JButton("Сохранить");

        fromTable.addActionListener(e -> createFromTable(model, firstOperand));
        fromFunction.addActionListener(e -> createFromFunction(model, firstOperand));
        load.addActionListener(e -> loadFunction(model, firstOperand));
        save.addActionListener(e -> saveFunction(model));

        actions.add(fromTable);
        actions.add(fromFunction);
        actions.add(load);
        actions.add(save);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Результат"));

        JTable table = new JTable(resultModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton add = new JButton("+");
        JButton subtract = new JButton("-");
        JButton multiply = new JButton("*");
        JButton divide = new JButton("/");
        JButton save = new JButton("Сохранить");

        add.addActionListener(e -> performOperation(OperationType.ADD));
        subtract.addActionListener(e -> performOperation(OperationType.SUB));
        multiply.addActionListener(e -> performOperation(OperationType.MUL));
        divide.addActionListener(e -> performOperation(OperationType.DIV));
        save.addActionListener(e -> saveFunction(resultModel));

        actions.add(add);
        actions.add(subtract);
        actions.add(multiply);
        actions.add(divide);
        actions.add(save);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void createFromTable(TabulatedFunctionTableModel model, boolean isFirst) {
        String input = JOptionPane.showInputDialog(this, "Количество точек", "3");
        if (input == null) {
            return;
        }
        int count;
        try {
            count = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Введите целое число точек", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        TableCreationDialog dialog = new TableCreationDialog((MainWindow) getOwner(), count);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }
        double[] x = new double[count];
        double[] y = new double[count];
        for (int i = 0; i < count; i++) {
            try {
                x[i] = Double.parseDouble(String.valueOf(dialog.getModel().getValueAt(i, 0)));
                y[i] = Double.parseDouble(String.valueOf(dialog.getModel().getValueAt(i, 1)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Заполните все значения корректно", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        TabulatedFunction function = context.getFactory().create(x, y);
        assignFunction(model, function, isFirst);
    }

    private void createFromFunction(TabulatedFunctionTableModel model, boolean isFirst) {
        FunctionGeneratorDialog dialog = new FunctionGeneratorDialog((MainWindow) getOwner());
        dialog.setVisible(true);
        TabulatedFunctionFactoryParameters params = dialog.buildParameters();
        if (params == null) {
            return;
        }
        TabulatedFunction function = context.getFactory().create(params.source(), params.from(), params.to(), params.count());
        assignFunction(model, function, isFirst);
    }

    private void loadFunction(TabulatedFunctionTableModel model, boolean isFirst) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                TabulatedFunction function = FunctionsIO.deserialize(bis);
                assignFunction(model, function, isFirst);
            } catch (IOException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(this, "Не удалось загрузить функцию: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFunction(TabulatedFunctionTableModel model) {
        if (model.getFunction() == null) {
            JOptionPane.showMessageDialog(this, "Нет данных для сохранения", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                FunctionsIO.serialize(bos, model.getFunction());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Не удалось сохранить функцию: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performOperation(OperationType type) {
        if (first == null || second == null) {
            JOptionPane.showMessageDialog(this, "Создайте оба операнда", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            TabulatedFunction result = switch (type) {
                case ADD -> context.getOperationService().add(first, second);
                case SUB -> context.getOperationService().subtract(first, second);
                case MUL -> context.getOperationService().multiply(first, second);
                case DIV -> context.getOperationService().divide(first, second);
            };
            resultModel.setFunction(result);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка операции: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void assignFunction(TabulatedFunctionTableModel model, TabulatedFunction function, boolean firstOperand) {
        model.setFunction(function);
        if (firstOperand) {
            first = function;
        } else {
            second = function;
        }
    }

    private enum OperationType {ADD, SUB, MUL, DIV}
}