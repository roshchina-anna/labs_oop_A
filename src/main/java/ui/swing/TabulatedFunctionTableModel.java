package ui.swing;

import functions.TabulatedFunction;

import javax.swing.table.AbstractTableModel;

public class TabulatedFunctionTableModel extends AbstractTableModel {
    private final boolean editableY;
    private TabulatedFunction function;

    public TabulatedFunctionTableModel(boolean editableY) {
        this.editableY = editableY;
    }

    public void setFunction(TabulatedFunction function) {
        this.function = function;
        fireTableDataChanged();
    }

    public TabulatedFunction getFunction() {
        return function;
    }

    @Override
    public int getRowCount() {
        return function == null ? 0 : function.getCount();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "x" : "y";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (function == null) {
            return null;
        }
        if (columnIndex == 0) {
            return function.getX(rowIndex);
        }
        return function.getY(rowIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return editableY && columnIndex == 1 && function != null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!isCellEditable(rowIndex, columnIndex)) {
            return;
        }
        try {
            double newValue = Double.parseDouble(aValue.toString());
            function.setY(rowIndex, newValue);
        } catch (NumberFormatException ignored) {
            // ignore invalid input, JTable will keep old value
        }
    }
}