package ui.swing;

import functions.TabulatedFunction;
import io.FunctionsIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;

public class DifferentiationWindow extends JDialog {
    private final AppContext context;
    private final TabulatedFunctionTableModel sourceModel = new TabulatedFunctionTableModel(true);
    private final TabulatedFunctionTableModel resultModel = new TabulatedFunctionTableModel(false);
    private TabulatedFunction source;

    public DifferentiationWindow(MainWindow owner, AppContext context) {
        super(owner, "Дифференцирование", true);
        this.context = context;
        initUi();
        setSize(700, 450);
        setLocationRelativeTo(owner);
    }

    private void initUi() {
        JPanel content = new JPanel(new GridLayout(1, 2, 8, 0));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        content.add(buildSourcePanel());
        content.add(buildResultPanel());

        setContentPane(content);
    }

    private JPanel buildSourcePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Исходная функция"));

        JTable table = new JTable(sourceModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton createTable = new JButton("Создать из таблицы");
        JButton createFunction = new JButton("Создать из функции");
        JButton load = new JButton("Загрузить");
        JButton save = new JButton("Сохранить");

        createTable.addActionListener(e -> createFromTable());
        createFunction.addActionListener(e -> createFromFunction());
        load.addActionListener(e -> loadFunction());
        save.addActionListener(e -> saveFunction(sourceModel));

        actions.add(createTable);
        actions.add(createFunction);
        actions.add(load);
        actions.add(save);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Производная"));

        JTable table = new JTable(resultModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton derive = new JButton("Вычислить");
        JButton save = new JButton("Сохранить");
        derive.addActionListener(e -> calculateDerivative());
        save.addActionListener(e -> saveFunction(resultModel));

        actions.add(derive);
        actions.add(save);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void createFromTable() {
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
        source = function;
        sourceModel.setFunction(function);
    }

    private void createFromFunction() {
        FunctionGeneratorDialog dialog = new FunctionGeneratorDialog((MainWindow) getOwner());
        dialog.setVisible(true);
        TabulatedFunctionFactoryParameters params = dialog.buildParameters();
        if (params == null) {
            return;
        }
        TabulatedFunction function = context.getFactory().create(params.source(), params.from(), params.to(), params.count());
        source = function;
        sourceModel.setFunction(function);
    }

    private void loadFunction() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                TabulatedFunction function = FunctionsIO.deserialize(bis);
                source = function;
                sourceModel.setFunction(function);
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

    private void calculateDerivative() {
        if (source == null) {
            JOptionPane.showMessageDialog(this, "Сначала создайте функцию", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            TabulatedFunction derivative = context.getDifferentialOperator().derive(source);
            resultModel.setFunction(derivative);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка вычисления: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}