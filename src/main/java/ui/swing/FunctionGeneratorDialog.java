package ui.swing;

import functions.MathFunction;
import ui.SimpleFunctionRegistry;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;

public class FunctionGeneratorDialog extends JDialog {
    private final SimpleFunctionRegistry registry = new SimpleFunctionRegistry();
    private final JComboBox<String> functionSelect;
    private final JTextField fromField = new JTextField();
    private final JTextField toField = new JTextField();
    private final JTextField countField = new JTextField();
    private MathFunction result;

    public FunctionGeneratorDialog(MainWindow owner) {
        super(owner, "Создание из функции", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.functionSelect = new JComboBox<>(registry.getFunctions().keySet().toArray(new String[0]));
        initUi();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUi() {
        JPanel content = new JPanel(new GridLayout(0, 2, 8, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        content.add(new JLabel("Функция"));
        content.add(functionSelect);
        content.add(new JLabel("Начало"));
        content.add(fromField);
        content.add(new JLabel("Конец"));
        content.add(toField);
        content.add(new JLabel("Количество точек"));
        content.add(countField);

        JButton ok = new JButton("Создать");
        JButton cancel = new JButton("Отмена");
        ok.addActionListener(e -> {
            result = registry.getByName((String) functionSelect.getSelectedItem());
            dispose();
        });
        cancel.addActionListener(e -> {
            result = null;
            dispose();
        });

        content.add(cancel);
        content.add(ok);
        setContentPane(content);
    }

    public TabulatedFunctionFactoryParameters buildParameters() {
        if (result == null) {
            return null;
        }
        try {
            double from = Double.parseDouble(fromField.getText());
            double to = Double.parseDouble(toField.getText());
            int count = Integer.parseInt(countField.getText());
            return new TabulatedFunctionFactoryParameters(result, from, to, count);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}