package dao;

import util.DBConnection;
import model.User;

import java.sql.Timestamp;

public class UserDAO {

    public boolean registerUser(User user)
    {
        String sql = "INSERT INTO users (email, password, registeredAt) VALUES (?, ?, ?)";
        try (var conn = DBConnection.getConnection();
             var preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getEmail());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(user.getRegisteredAt()));
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
