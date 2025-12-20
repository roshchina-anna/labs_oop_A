package ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class TableCreationDialog extends JDialog {
    private final DefaultTableModel model;
    private boolean confirmed;

    public TableCreationDialog(MainWindow owner, int rows) {
        super(owner, "Точки функции", true);
        this.model = new DefaultTableModel(new Object[]{"x", "y"}, rows) {
        };
        initUi(rows);
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUi(int rows) {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        JTable table = new JTable(model);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Отмена");
        JButton ok = new JButton("Создать");
        bottom.add(cancel);
        bottom.add(ok);
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public DefaultTableModel getModel() {
        return model;
    }
}