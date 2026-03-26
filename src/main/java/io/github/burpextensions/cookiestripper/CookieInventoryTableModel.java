package io.github.burpextensions.cookiestripper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

final class CookieInventoryTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Strip",
            "Cookie Name",
            "Seen",
            "Last Tool"
    };

    private final Map<String, CookieEntry> entriesByKey = new LinkedHashMap<String, CookieEntry>();
    private final List<CookieEntry> rows = new ArrayList<CookieEntry>();

    @Override
    public synchronized int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class;
        }
        if (columnIndex == 2) {
            return Integer.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        CookieEntry entry = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return entry.isSelected();
            case 1:
                return entry.getDisplayName();
            case 2:
                return entry.getSeenCount();
            case 3:
                return entry.getLastTool();
            default:
                return null;
        }
    }

    @Override
    public synchronized void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex != 0) {
            return;
        }
        rows.get(rowIndex).setSelected(Boolean.TRUE.equals(value));
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    synchronized void recordCookie(String cookieName, String toolName) {
        if (cookieName == null || cookieName.trim().isEmpty()) {
            return;
        }

        String normalized = cookieName.trim();
        String key = normalized.toLowerCase(Locale.ROOT);
        CookieEntry entry = entriesByKey.get(key);
        if (entry == null) {
            entry = new CookieEntry(normalized);
            entriesByKey.put(key, entry);
            rows.add(entry);
            Collections.sort(rows, (left, right) -> left.getDisplayName().compareToIgnoreCase(right.getDisplayName()));
        } else {
            entry.updateDisplayName(normalized);
        }
        entry.seen(toolName);
        SwingUtilities.invokeLater(this::fireTableDataChanged);
    }

    synchronized boolean shouldStrip(String cookieName) {
        if (cookieName == null) {
            return false;
        }
        CookieEntry entry = entriesByKey.get(cookieName.trim().toLowerCase(Locale.ROOT));
        return entry != null && entry.isSelected();
    }

    synchronized int getSelectedCount() {
        int count = 0;
        for (CookieEntry entry : rows) {
            if (entry.isSelected()) {
                count++;
            }
        }
        return count;
    }

    synchronized int getUniqueCount() {
        return rows.size();
    }

    synchronized void clearInventory() {
        entriesByKey.clear();
        rows.clear();
        SwingUtilities.invokeLater(this::fireTableDataChanged);
    }

    synchronized void setAllSelected(boolean selected) {
        for (CookieEntry entry : rows) {
            entry.setSelected(selected);
        }
        SwingUtilities.invokeLater(this::fireTableDataChanged);
    }

    synchronized void selectCookies(Set<String> cookieNames) {
        for (CookieEntry entry : rows) {
            if (cookieNames.contains(entry.getKey())) {
                entry.setSelected(true);
            }
        }
        SwingUtilities.invokeLater(this::fireTableDataChanged);
    }
}
