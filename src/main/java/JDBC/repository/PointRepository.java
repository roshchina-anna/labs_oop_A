package JDBC.repository;

import DTO.Point;
import JDBC.repository.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PointRepository {
    private static final Logger logger = LoggerFactory.getLogger(PointRepository.class);

    // добавление точки
    public void insert(Point point) {
        logger.info("Starting the operation: adding a point for the ID function {}", point.getFunctionId());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/points/insert_point.sql")
             )) {
            stmt.setInt(1, point.getFunctionId());
            stmt.setDouble(2, point.getXValue());
            stmt.setDouble(3, point.getYValue());
            stmt.executeUpdate();
            logger.info("The operation is completed: the point has been added");
        } catch (SQLException e) {
            logger.error("Error when adding point for function ID {}: {}", point.getFunctionId(), e.getMessage());
            throw new RuntimeException("Failed to insert point", e);
        }
    }

    // получение всех точек функции
    public List<Point> findByFunctionId(Integer functionId) {
        logger.info("Start of operation: obtaining points for the ID function {}", functionId);
        List<Point> points = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/points/select_all_points.sql")
             )) {
            stmt.setInt(1, functionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    points.add(Mapper.mapToPoint(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error when getting points for function ID {}: {}", functionId, e.getMessage());
            return new ArrayList<>();
        }
        logger.info("The operation is complete: {} points have been found for the function {}", points.size(), functionId);
        return points;
    }

    // получение конкретной точки
    public Point findByFunctionIdAndX(Integer functionId, Double xValue) {
        logger.info("Starting the operation: finding the point of the function {} with X={}", functionId, xValue);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/points/select_point.sql")
             )) {
            stmt.setInt(1, functionId);
            stmt.setDouble(2, xValue);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("Operation completed: the point has been found");
                    return Mapper.mapToPoint(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error when finding point for function ID {} with X={}: {}", functionId, xValue, e.getMessage());
        }
        logger.info("Operation completed: point not found");
        return null;
    }

    // обновление значения точки
    public boolean update(Point point) {
        logger.info("Starting the operation: updating the function point {} with X={}", point.getFunctionId(), point.getXValue());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/points/update_point.sql")
             )) {
            stmt.setDouble(1, point.getYValue());
            stmt.setInt(2, point.getFunctionId());
            stmt.setDouble(3, point.getXValue());
            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;
            logger.info("Operation completed: point {} updated", success ? "" : "not ");
            return success;
        } catch (SQLException e) {
            logger.error("Error when updating point for function ID {} with X={}: {}",
                    point.getFunctionId(), point.getXValue(), e.getMessage());
            return false;
        }
    }

    // удаление конкретной точки
    public boolean delete(Integer functionId, Double xValue) {
        logger.info("Start of operation: deleting the point of function {} with X={}", functionId, xValue);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SqlHelper.loadSqlFromFile("scripts/points/delete_point.sql")
             )) {
            stmt.setInt(1, functionId);
            stmt.setDouble(2, xValue);
            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;
            logger.info("Operation completed: point {} has been deleted", success ? "" : "not ");
            return success;
        } catch (SQLException e) {
            logger.error("Error when deleting point for function ID {} with X={}: {}",
                    functionId, xValue, e.getMessage());
            return false;
        }
    }

    // Поиск с сортировкой по X
    public List<Point> findAllSortedByX() {
        logger.info("Search for points sorted by x_value");
        List<Point> points = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     SqlHelper.loadSqlFromFile("scripts/points/select_points_sorted_x.sql")
             )) {
            while (rs.next()) {
                points.add(Mapper.mapToPoint(rs));
            }
        } catch (SQLException e) {
            logger.error("Error when getting sorted points: {}", e.getMessage());
            return new ArrayList<>();
        }
        logger.info("Found {} sorted points", points.size());
        return points;
    }

    // Поиск с сортировкой по Y
    public List<Point> findAllSortedByY() {
        logger.info("Search for points sorted by y_value");
        List<Point> points = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     SqlHelper.loadSqlFromFile("scripts/points/select_points_sorted_y.sql")
             )) {
            while (rs.next()) {
                points.add(Mapper.mapToPoint(rs));
            }

        } catch (SQLException e) {
            logger.error("Error when getting points sorted by Y: {}", e.getMessage());
            return new ArrayList<>();
        }
        logger.info("Found {} points sorted by Y", points.size());
        return points;
    }

}