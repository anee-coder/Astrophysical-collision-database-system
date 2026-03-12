package astro;

import java.awt.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.swing.*;

public class AddCollisionForm extends JDialog {

    private JTextField txtType, txtEnergy, txtVelocity, txtDate;
    private JComboBox<String> cmbLocation;
    private JTextArea txtDescription;
    private final CollisionApp parent;

    public AddCollisionForm(CollisionApp parent) {
        super(parent, "Add New Collision", true);
        this.parent = parent;

        setSize(420, 520);
        setLayout(new BorderLayout(10,10));
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Color.WHITE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        // Fields
        txtType = new JTextField();
        txtEnergy = new JTextField();
        txtVelocity = new JTextField();
        txtDate = new JTextField("YYYY-MM-DD");
        txtDescription = new JTextArea(4, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(txtDescription, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        cmbLocation = new JComboBox<>();
        loadLocations();

        addRow(panel, gbc, 0, "Collision Type:", txtType);
        addRow(panel, gbc, 1, "Energy:", txtEnergy);
        addRow(panel, gbc, 2, "Velocity:", txtVelocity);
        addRow(panel, gbc, 3, "Date (YYYY-MM-DD):", txtDate);
        addRow(panel, gbc, 4, "Location:", cmbLocation);
        addRow(panel, gbc, 5, "Description:", descScroll);

        JPanel btnPanel = new JPanel();
        JButton btnSave = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> saveCollision());
        btnCancel.addActionListener(e -> dispose());
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int y, String label, Component field) {
        gbc.gridx = 0; gbc.gridy = y;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1; gbc.gridy = y;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    private void loadLocations() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Location_ID, Galaxy_Name FROM Location ORDER BY Location_ID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cmbLocation.addItem(rs.getString("Location_ID") + " - " + rs.getString("Galaxy_Name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading locations: " + ex.getMessage());
        }
    }

    private void saveCollision() {
        String type = txtType.getText().trim();
        String energyStr = txtEnergy.getText().trim();
        String velocityStr = txtVelocity.getText().trim();
        String date = txtDate.getText().trim();
        String desc = txtDescription.getText().trim();
        String location = (String) cmbLocation.getSelectedItem();

        // Input validation
        if (type.isEmpty() || energyStr.isEmpty() || velocityStr.isEmpty() || date.isEmpty() || location == null) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!");
            return;
        }

        double energy, velocity;
        try {
            energy = Double.parseDouble(energyStr);
            velocity = Double.parseDouble(velocityStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Energy and Velocity must be numeric values!");
            return;
        }

        if (!isValidDate(date)) {
            JOptionPane.showMessageDialog(this, "Invalid date format! Use YYYY-MM-DD.");
            return;
        }

        String locationID = location.split(" - ")[0];

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO Collision (Collision_ID, Type, Energy, Velocity, Date, Location_ID, Description) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

            // Generate new ID like C101, C102, etc.
            String newId = generateNextID(conn);
            ps.setString(1, newId);
            ps.setString(2, type);
            ps.setDouble(3, energy);
            ps.setDouble(4, velocity);
            ps.setString(5, date);
            ps.setString(6, locationID);
            ps.setString(7, desc);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Collision added successfully!");
                parent.loadCollisions(null, null);
                dispose();
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private boolean isValidDate(String dateStr) {
        try {
            new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private String generateNextID(Connection conn) throws SQLException {
        String lastId = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT Collision_ID FROM Collision ORDER BY Collision_ID DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) lastId = rs.getString(1);
        }
        if (lastId == null) return "C001";
        int num = Integer.parseInt(lastId.substring(1)) + 1;
        return String.format("C%03d", num);
    }
}
