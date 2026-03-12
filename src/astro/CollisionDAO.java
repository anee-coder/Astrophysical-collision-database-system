package astro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CollisionDAO {

    public void getAllCollisions() {
        String query = "SELECT Collision_ID, Type, Energy, Velocity, Date, Location_ID, Description FROM Collision LIMIT 10";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n=== Sample Collisions from Database ===\n");
            while (rs.next()) {
                System.out.printf("ID: %d | Type: %s | Energy: %.2f | Velocity: %.2f | Date: %s | Location: %s | Desc: %s%n",
                        rs.getInt("Collision_ID"),
                        rs.getString("Type"),
                        rs.getDouble("Energy"),
                        rs.getDouble("Velocity"),
                        rs.getDate("Date"),
                        rs.getString("Location_ID"),
                        rs.getString("Description"));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error fetching collisions: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        CollisionDAO dao = new CollisionDAO();
        dao.getAllCollisions();
    }
}
