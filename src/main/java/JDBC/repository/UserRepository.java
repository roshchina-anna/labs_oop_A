package JDBC.repository;

import DTO.User;
import JDBC.repository.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    // добавление пользователя
    public Integer insert(User user) {
        logger.info("Operation start: adding a user {}", user.getUsername());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/users/insert_user.sql"),
                     Statement.RETURN_GENERATED_KEYS
             )) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    logger.info("The operation is completed: the user has been added with the ID {}", id);
                    return id;
                } else {
                    logger.error("Critical error: failed to retrieve user ID");
                    throw new SQLException("Couldn't get user ID");
                }
            }
        } catch (SQLException e) {
            logger.error("Error when adding user {}: {}", user.getUsername(), e.getMessage());
            throw new RuntimeException("Failed to insert user", e);
        }
    }
    // поиск пользователя по имени
    public User findByUsername(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/users/select_user_username.sql")
             )) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Mapper.mapToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка поиска пользователя по username: {}", username, e);
        }
        return null;
    }

    // получение всех пользователей
    public List<User> findAll() {
        logger.info("Operation start: getting all users");
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SqlHelper.loadSqlFromFile("scripts/users/select_all_users.sql"))) {
            logger.info("ResultSet successfully obtained");
            while (rs.next()) {
                users.add(Mapper.mapToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Error when getting users: {}", e.getMessage());
            return new ArrayList<>();
        }
        logger.info("Operation completed: {} users found", users.size());
        return users;
    }

    // поиск пользователя по ID
    public User findById(Integer id) {
        logger.info("Operation start: user ID search {}", id);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/users/select_user_id.sql")
             )) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("Operation completed: the user has been found");
                    return Mapper.mapToUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error when searching user by ID {}: {}", id, e.getMessage());
        }
        logger.info("The operation was completed: the user with ID {} was not found", id);
        return null;
    }

    // поиск с сортировкой
    public List<User> findAllSortedByUsername() {
        logger.info("Search for users sorted by username");
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     SqlHelper.loadSqlFromFile("scripts/users/select_users_sorted_username.sql")
             )) {
            while (rs.next()) {
                users.add(Mapper.mapToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Error when getting sorted users: {}", e.getMessage());
            return new ArrayList<>(); // возвращаем пустой список при ошибке
        }
        logger.info("Found {} sorted users", users.size());
        return users;
    }

    // обновление пользователя
    public boolean update(User user) {
        logger.info("Start of the operation: updating the user with the ID {}", user.getId());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/users/update_user.sql")
             )) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole());
            stmt.setInt(4, user.getId());
            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;
            logger.info("Operation completed: user {} updated", success ? "" : "not ");
            return success;
        } catch (SQLException e) {
            logger.error("Error when updating user ID {}: {}", user.getId(), e.getMessage());
            return false;
        }
    }


    // удаление пользователя
    public boolean delete(Integer id) {
        logger.info("Start of the operation: deleting a user with an ID {}", id);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/users/delete_user.sql")
             )) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;
            logger.info("Operation completed: user {} deleted", success ? "" : "not ");
            return success;
        } catch (SQLException e) {
            logger.error("Error when deleting user ID {}: {}", id, e.getMessage());
            return false;
        }
    }
}