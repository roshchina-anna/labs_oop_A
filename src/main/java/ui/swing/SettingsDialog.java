package ui.swing;

import functions.factory.TabulatedFunctionFactory;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class SettingsDialog extends JDialog {
    private final AppContext context;
    private TabulatedFunctionFactory selected;

    public SettingsDialog(MainWindow owner, AppContext context) {
        super(owner, "Настройки", true);
        this.context = context;
        this.selected = context.getFactory();
        initUi();
        pack();
        setLocationRelativeTo(owner);
    }
    private JPanel createOptionsPanel() {
        JRadioButton arrayBtn = new JRadioButton("Массивы",
                context.getFactory().getClass().equals(context.getArrayFactory().getClass()));
        JRadioButton listBtn = new JRadioButton("Связный список",
                context.getFactory().getClass().equals(context.getLinkedListFactory().getClass()));

        arrayBtn.addActionListener(e -> selected = context.getArrayFactory());
        listBtn.addActionListener(e -> selected = context.getLinkedListFactory());

        ButtonGroup group = new ButtonGroup();
        group.add(arrayBtn);
        group.add(listBtn);

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT));
        options.add(arrayBtn);
        options.add(listBtn);
        return options;
    }
    private void initUi() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel options = createOptionsPanel(); // ← вызов нового метода

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton apply = new JButton("Применить");
        JButton cancel = new JButton("Отмена");
        buttons.add(cancel);
        buttons.add(apply);

        apply.addActionListener(e -> {
            context.setFactory(selected);
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        content.add(options, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
    }
}