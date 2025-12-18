package JDBC.repository;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlHelper {
    private static final Logger logger = LoggerFactory.getLogger(SqlHelper.class);
    public static String loadSqlFromFile(String filePath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) throw new IOException();
            String sql = new String(inputStream.readAllBytes());
            logger.info("The parsing was successful");
            return sql;
        } catch (IOException e) {
            logger.error("an error has occurred");
            throw new RuntimeException(e);
        }
    }
}