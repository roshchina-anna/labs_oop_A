package JDBC.repository;

import DTO.Function;
import JDBC.repository.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FunctionRepository {
    private static final Logger logger = LoggerFactory.getLogger(FunctionRepository.class);

    // добавление функции
    public Integer insert(Function function) {
        logger.info("Operation start: adding a function {}", function.getName());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/functions/insert_function.sql"),
                     Statement.RETURN_GENERATED_KEYS
             )) {
            stmt.setString(1, function.getName());
            stmt.setString(2, function.getExpression());
            stmt.setInt(3, function.getUserId());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    logger.info("Operation completed: function added with ID {}", id);
                    return id;
                } else {
                    logger.error("Critical error: failed to get function ID");
                    throw new SQLException("Couldn't get the function ID");
                }
            }
        } catch (SQLException e) {
            logger.error("Error when adding function {}: {}", function.getName(), e.getMessage());
            throw new RuntimeException("Failed to insert function", e);
        }
    }

    // получение всех функций
    public List<Function> findAll() {
        logger.info("Operation start: getting all the functions");
        List<Function> functions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     SqlHelper.loadSqlFromFile("scripts/functions/select_all_functions.sql")
             )) {
            while (rs.next()) {
                functions.add(Mapper.mapToFunction(rs));
            }
        } catch (SQLException e) {
            logger.error("Error when getting all functions: {}", e.getMessage());
            return new ArrayList<>();
        }
        logger.info("Operation completed: {} functions found", functions.size());
        return functions;
    }

    // поиск функции по ID
    public Function findById(Integer id) {
        logger.info("Operation start: function search by ID {}", id);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/functions/select_function_id.sql")
             )) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("Operation completed: function found");
                    return Mapper.mapToFunction(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error when searching function by ID {}: {}", id, e.getMessage());
        }
        logger.info("Operation completed: function with ID {} not found", id);
        return null;
    }

    // поиск функции по id пользователя
    public List<Function> findByUserId(Integer userId) {
        logger.info("Search for functions by user_id: {}", userId);
        List<Function> functions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/functions/select_functions_user_id.sql")
             )) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    functions.add(Mapper.mapToFunction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error when getting functions for user ID {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
        logger.info("Found {} functions for user ID {}", functions.size(), userId);
        return functions;
    }

    // поиск с сортировкой
    public List<Function> findAllSortedByName() {
        logger.info("Search for functions sorted by name");
        List<Function> functions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     SqlHelper.loadSqlFromFile("scripts/functions/select_functions_sorted_name.sql")
             )) {
            while (rs.next()) {
                functions.add(Mapper.mapToFunction(rs));
            }
        } catch (SQLException e) {
            logger.error("Error when getting sorted functions: {}", e.getMessage());
            return new ArrayList<>();
        }
        logger.info("Found {} sorted functions", functions.size());
        return functions;
    }

    // обновление функции
    public boolean update(Function function) {
        logger.info("Operation start: updating the function with ID {}", function.getId());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/functions/update_function.sql")
             )) {
            stmt.setString(1, function.getName());
            stmt.setString(2, function.getExpression());
            stmt.setInt(3, function.getUserId());
            stmt.setInt(4, function.getId());
            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;
            logger.info("Operation completed: the {} function has been updated", success ? "" : "not ");
            return success;
        } catch (SQLException e) {
            logger.error("Error when updating function ID {}: {}", function.getId(), e.getMessage());
            return false;
        }
    }

    // удаление функции
    public boolean delete(Integer id) {
        logger.info("Start of the operation: deleting the function from the ID {}", id);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/functions/delete_function.sql")
             )) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;
            logger.info("Operation completed: the {} function has been deleted", success ? "" : "not ");
            return success;
        } catch (SQLException e) {
            logger.error("Error when deleting function ID {}: {}", id, e.getMessage());
            return false;
        }
    }
}