package astro;

import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class CollisionApp extends JFrame {

    JTable table;
    DefaultTableModel model;
    JTextField searchField;
    JComboBox<String> filterBox;
    JLabel statusLabel;

    int currentPage = 1;
    int pageSize = 50;
    int totalRecords = 0;

    public CollisionApp() {
        setTitle("Astrophysical Collision Database System");
        setSize(1320, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));

        // Header
        JPanel header = new JPanel();
        header.setBackground(new Color(60, 100, 220));
        header.setPreferredSize(new Dimension(100, 60));
        header.setLayout(new BorderLayout());
        JLabel title = new JLabel("  ✦ Astrophysical Collision Database System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        sidebar.setBackground(new Color(240, 243, 250));

        JButton btnHome = new JButton("Home");
        JButton btnAdd = new JButton("Add Collision");
        JButton btnEdit = new JButton("Edit Collision");
        JButton btnDelete = new JButton("Delete Collision");
        JButton btnStats = new JButton("Statistics");
        JButton btnExport = new JButton("Export CSV");

        Dimension btnSize = new Dimension(160,38);
        for (JButton b : Arrays.asList(btnHome, btnAdd, btnEdit, btnDelete, btnStats, btnExport)) {
            b.setMaximumSize(btnSize);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setFocusPainted(false);
            b.setBackground(new Color(220, 230, 250));
            b.setForeground(Color.BLACK);
            sidebar.add(Box.createRigidArea(new Dimension(0,8)));
            sidebar.add(b);
        }
        add(sidebar, BorderLayout.WEST);

        // Main area
        JPanel main = new JPanel(new BorderLayout(8,8));
        main.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        main.setBackground(Color.WHITE);
        add(main, BorderLayout.CENTER);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.setBackground(Color.WHITE);
        searchField = new JTextField(24);
        filterBox = new JComboBox<>(new String[]{"All", "Type", "Galaxy", "Location_ID"});
        JButton btnSearch = new JButton("Search");
        JButton btnRefresh = new JButton("Refresh");
        JButton btnPrev = new JButton("Prev");
        JButton btnNext = new JButton("Next");

        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        controls.add(filterBox);
        controls.add(btnSearch);
        controls.add(btnRefresh);
        controls.add(btnPrev);
        controls.add(btnNext);

        main.add(controls, BorderLayout.NORTH);

        // Table
        String[] cols = {"Collision_ID","Type","Energy","Velocity","Date",
                         "Location_ID","Galaxy_Name","Coordinates","Distance_ly","Description"};
        model = new DefaultTableModel(cols, 0);
        table = new JTable(model) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == getColumnCount()-1) return new MultiLineCellRenderer();
                return super.getCellRenderer(row, column);
            }
        };
        table.setRowHeight(32);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {80,130,110,90,110,100,140,160,120,420};
        for (int i=0;i<widths.length;i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JTableHeader th = table.getTableHeader();
        th.setReorderingAllowed(false);
        th.setFont(new Font("Segoe UI", Font.BOLD, 13));
        th.setBackground(new Color(220, 230, 250));

        JScrollPane scroll = new JScrollPane(table);
        main.add(scroll, BorderLayout.CENTER);

        // Status
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(Color.WHITE);
        statusLabel = new JLabel("Ready");
        bottom.add(statusLabel);
        main.add(bottom, BorderLayout.SOUTH);

        // Button actions
        btnRefresh.addActionListener(e -> { currentPage = 1; loadCollisions(null,null); });
        btnSearch.addActionListener(e -> handleSearch());
        btnPrev.addActionListener(e -> { if (currentPage>1) { currentPage--; loadCollisions((String)filterBox.getSelectedItem(), searchField.getText().trim()); }});
        btnNext.addActionListener(e -> { int max = (int)Math.ceil((double)totalRecords/pageSize); if (currentPage<max) { currentPage++; loadCollisions((String)filterBox.getSelectedItem(), searchField.getText().trim()); }});

        btnAdd.addActionListener(e -> new AddCollisionForm(this).setVisible(true));
        btnHome.addActionListener(e -> { currentPage = 1; loadCollisions(null,null); });
        btnExport.addActionListener(e -> exportToCSV());
        btnStats.addActionListener(e -> showStatsDialog());
        btnEdit.addActionListener(e -> editSelectedCollision());
        btnDelete.addActionListener(e -> deleteSelectedCollision());

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) {
                    int r = table.getSelectedRow();
                    if (r>=0) showDetailDialog(r);
                }
            }
        });

        loadCollisions(null, null);
    }

    private void handleSearch() {
        String keyword = searchField.getText().trim();
        String filter = (String) filterBox.getSelectedItem();
        if (keyword.isEmpty() && !"All".equals(filter)) {
            JOptionPane.showMessageDialog(this, "Please enter something to search.");
            return;
        }
        currentPage = 1;
        loadCollisions(filter, keyword);
    }

    // --------------------------- MAIN LOGIC --------------------------

    public void loadCollisions(String filter, String keyword) {
        model.setRowCount(0);
        statusLabel.setText("Loading...");
        String base = "SELECT c.Collision_ID,c.Type,c.Energy,c.Velocity,c.Date," +
                " l.Location_ID,l.Galaxy_Name,l.Coordinates,l.Distance_from_Earth,c.Description " +
                "FROM Collision c JOIN Location l ON c.Location_ID=l.Location_ID ";

        String where = "";
        if (filter!=null && keyword!=null && !"All".equals(filter) && !keyword.isEmpty()) {
            if ("Type".equals(filter)) where = " WHERE c.Type LIKE ? ";
            else if ("Galaxy".equals(filter)) where = " WHERE l.Galaxy_Name LIKE ? ";
            else if ("Location_ID".equals(filter)) where = " WHERE l.Location_ID LIKE ? ";
        }
        String orderLimit = " ORDER BY c.Date DESC LIMIT ? OFFSET ?";

        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement cps = conn.prepareStatement("SELECT COUNT(*) FROM Collision")) {
                ResultSet cr = cps.executeQuery();
                if (cr.next()) totalRecords = cr.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(base + where + orderLimit)) {
                int idx = 1;
                if (!where.isEmpty()) ps.setString(idx++, "%" + keyword + "%");
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, (currentPage-1)*pageSize);

                ResultSet rs = ps.executeQuery();
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Object[] row = new Object[md.getColumnCount()];
                    for (int i=0;i<md.getColumnCount();i++) row[i] = rs.getObject(i+1);
                    model.addRow(row);
                }
            }

            int maxPage = Math.max(1, (int)Math.ceil((double)totalRecords/pageSize));
            statusLabel.setText(String.format("Page %d of %d — Total: %d", currentPage, maxPage, totalRecords));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage());
            statusLabel.setText("Error");
        }
    }

    private void showDetailDialog(int row) {
        StringBuilder sb = new StringBuilder();
        for (int c=0;c<table.getColumnCount();c++) {
            String col = table.getColumnName(c);
            Object val = table.getValueAt(row, c);
            sb.append(String.format("%s: %s%n", col, val==null?"":val.toString()));
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(700,400));
        JOptionPane.showMessageDialog(this, sp, "Collision Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void editSelectedCollision() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to edit.");
            return;
        }

        String id = table.getValueAt(row, 0).toString();

        // Build edit dialog
        JDialog dialog = new JDialog(this, "Edit Collision " + id, true);
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10,10));

        JPanel form = new JPanel(new GridLayout(0,2,5,5));
        form.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JTextField tType = new JTextField(table.getValueAt(row, 1).toString());
        JTextField tEnergy = new JTextField(table.getValueAt(row, 2).toString());
        JTextField tVelocity = new JTextField(table.getValueAt(row, 3).toString());
        JTextField tDate = new JTextField(table.getValueAt(row, 4).toString());
        JTextArea tDesc = new JTextArea(table.getValueAt(row, 9)==null?"":table.getValueAt(row, 9).toString(),4,20);
        JScrollPane scroll = new JScrollPane(tDesc);

        form.add(new JLabel("Type:")); form.add(tType);
        form.add(new JLabel("Energy:")); form.add(tEnergy);
        form.add(new JLabel("Velocity:")); form.add(tVelocity);
        form.add(new JLabel("Date:")); form.add(tDate);
        form.add(new JLabel("Description:"));
        form.add(scroll);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE Collision SET Type=?, Energy=?, Velocity=?, Date=?, Description=? WHERE Collision_ID=?")) {

                ps.setString(1, tType.getText().trim());
                ps.setDouble(2, Double.parseDouble(tEnergy.getText().trim()));
                ps.setDouble(3, Double.parseDouble(tVelocity.getText().trim()));
                ps.setString(4, tDate.getText().trim());
                ps.setString(5, tDesc.getText().trim());
                ps.setString(6, id);

                int n = ps.executeUpdate();
                if (n > 0) {
                    JOptionPane.showMessageDialog(dialog, "Collision updated successfully!");
                    dialog.dispose();
                    loadCollisions(null, null);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void deleteSelectedCollision() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.");
            return;
        }

        String id = table.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete collision " + id + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM Collision WHERE Collision_ID=?")) {
                ps.setString(1, id);
                int n = ps.executeUpdate();
                if (n > 0) {
                    JOptionPane.showMessageDialog(this, "Collision deleted successfully!");
                    loadCollisions(null, null);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting: " + ex.getMessage());
            }
        }
    }

    private void showStatsDialog() {
        Map<String,Integer> counts = new LinkedHashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Type, COUNT(*) AS cnt FROM Collision GROUP BY Type ORDER BY cnt DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) counts.put(rs.getString("Type"), rs.getInt("cnt"));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading stats: " + ex.getMessage());
            return;
        }

        JDialog dlg = new JDialog(this, "Collision Types Statistics", true);
        dlg.setSize(800,500);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8,8));
        dlg.add(new StatsChartPanel(counts), BorderLayout.CENTER);

        DefaultTableModel tm = new DefaultTableModel(new String[]{"Type","Count"},0);
        counts.forEach((k,v) -> tm.addRow(new Object[]{k,v}));
        JTable t = new JTable(tm);
        dlg.add(new JScrollPane(t), BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void exportToCSV() {
        try (FileWriter fw = new FileWriter("collisions_export.csv")) {
            for (int r=0;r<model.getRowCount();r++) {
                for (int c=0;c<model.getColumnCount();c++) {
                    Object o = model.getValueAt(r,c);
                    String s = o==null? "" : o.toString().replace("\n"," ").replace(","," ");
                    fw.write(s);
                    if (c < model.getColumnCount()-1) fw.write(",");
                }
                fw.write("\n");
            }
            JOptionPane.showMessageDialog(this, "Exported to collisions_export.csv");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage());
        }
    }

    static class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {
        MultiLineCellRenderer() { setLineWrap(true); setWrapStyleWord(true); setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(value==null? "": value.toString());
            setFont(table.getFont());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            int prefH=getPreferredSize().height+8;
            if (table.getRowHeight(row)<prefH) table.setRowHeight(row,prefH);
            return this;
        }
    }

    static class StatsChartPanel extends JPanel {
        private final Map<String,Integer> counts;
        StatsChartPanel(Map<String,Integer> counts){this.counts=counts;setBackground(Color.WHITE);}
        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(counts.isEmpty()){g.drawString("No data",20,20);return;}
            Graphics2D g2=(Graphics2D)g;
            int w=getWidth(),h=getHeight(),padding=60;
            int chartW=w-padding*2,chartH=h-padding*2,x0=padding,y0=padding;
            int max=counts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            int barW=Math.max(30,chartW/Math.max(1,counts.size())-10),i=0,gap=10;
            for(Map.Entry<String,Integer> e:counts.entrySet()){
                int val=e.getValue(),barH=(int)((double)val/max*(chartH-40));
                int bx=x0+i*(barW+gap),by=y0+(chartH-barH);
                g2.setColor(new Color(80,160,220));
                g2.fillRect(bx,by,barW,barH);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(bx,by,barW,barH);
                drawCenteredString(g2,String.valueOf(val),bx,by-14,barW,12);
                drawCenteredString(g2,e.getKey(),bx,by+barH+6,barW,40);
                i++;
            }
        }
        private void drawCenteredString(Graphics2D g2,String text,int x,int y,int w,int h){
            FontMetrics fm=g2.getFontMetrics();
            int tx=x+(w-fm.stringWidth(text))/2;
            int ty=y+((h-fm.getHeight())/2)+fm.getAscent();
            g2.drawString(text,tx,ty);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CollisionApp().setVisible(true));
    }
}
