package JDBC.repository;

import DTO.Function;
import DTO.Point;
import DTO.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mapper {
    private static final Logger logger = LoggerFactory.getLogger(Mapper.class.getName());
    public static User mapToUser(ResultSet rs) throws SQLException {
        logger.info("Mapping ResultSet to User DTO");
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        return user;
    }

    public static Function mapToFunction(ResultSet rs) throws SQLException {
        logger.info("Mapping ResultSet to Function DTO");
        Function function = new Function();
        function.setId(rs.getInt("id"));
        function.setName(rs.getString("name"));
        function.setExpression(rs.getString("expression"));
        function.setUserId(rs.getInt("user_id"));
        return function;
    }

    public static Point mapToPoint(ResultSet rs) throws SQLException {
        logger.info("Mapping ResultSet to Point DTO");
        Point point = new Point();
        point.setFunctionId(rs.getInt("function_id"));
        point.setXValue(rs.getDouble("x_value"));
        point.setYValue(rs.getDouble("y_value"));
        return point;
    }
}