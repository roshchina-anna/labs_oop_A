package repository;

import DTO.Function;
import DTO.Point;
import DTO.User;
import JDBC.repository.FunctionRepository;
import JDBC.repository.PointRepository;
import JDBC.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PointRepositoryTest {
    private PointRepository pointRepository;
    private FunctionRepository functionRepository;
    private UserRepository userRepository;

    private Integer functionId1;
    private Integer functionId2;
    private Integer userId;

    @BeforeEach
    void setUp() {
        pointRepository = new PointRepository();
        functionRepository = new FunctionRepository();
        userRepository = new UserRepository();
        // очищаем таблицы перед тестами
        clearPointsTable();
        clearFunctionsTable();
        clearUsersTable();
        // добавляем пользователя
        User user = new User("bob", "123");
        userId = userRepository.insert(user);
        // добавляем функции
        functionId1 = functionRepository.insert(new Function("f1", "x*2", userId));
        functionId2 = functionRepository.insert(new Function("f2", "x", userId));
        // добавляем точки
        pointRepository.insert(new Point(functionId1, 1.0, 2.0));
        pointRepository.insert(new Point(functionId1, 2.0, 4.0));
        pointRepository.insert(new Point(functionId1, 3.0, 6.0));
        pointRepository.insert(new Point(functionId2, 1.0, 1.0));
        pointRepository.insert(new Point(functionId2, 2.0, 2.0));
    }

    @AfterEach
    void tearDown() {
        // очищаем таблицы после тестов
        clearPointsTable();
        clearFunctionsTable();
        clearUsersTable();
    }

    @Test
    void testInsert() {
        Point newPoint = new Point(functionId1, 4.0, 8.0);
        pointRepository.insert(newPoint);

        List<Point> points = pointRepository.findByFunctionId(functionId1);
        assertEquals(4, points.size());
    }

    @Test
    void testFindByFunctionId() {
        List<Point> points1 = pointRepository.findByFunctionId(functionId1);
        assertEquals(3, points1.size());

        List<Point> points2 = pointRepository.findByFunctionId(functionId2);
        assertEquals(2, points2.size());

        // Проверяем данные первой функции
        boolean found1 = false, found2 = false, found3 = false;
        for (Point p : points1) {
            if (Math.abs(p.getXValue() - 1.0) < 0.001 && Math.abs(p.getYValue() - 2.0) < 0.001) found1 = true;
            if (Math.abs(p.getXValue() - 2.0) < 0.001 && Math.abs(p.getYValue() - 4.0) < 0.001) found2 = true;
            if (Math.abs(p.getXValue() - 3.0) < 0.001 && Math.abs(p.getYValue() - 6.0) < 0.001) found3 = true;
        }
        assertTrue(found1);
        assertTrue(found2);
        assertTrue(found3);
    }

    @Test
    void testFindByFunctionIdEmpty() {
        List<Point> points = pointRepository.findByFunctionId(999);
        assertNotNull(points);
        assertTrue(points.isEmpty());
    }

    @Test
    void testFindByFunctionIdAndX() {
        Point point = pointRepository.findByFunctionIdAndX(functionId1, 2.0);
        assertNotNull(point);
        assertEquals(functionId1, point.getFunctionId());
        assertEquals(2.0, point.getXValue(), 0.001);
        assertEquals(4.0, point.getYValue(), 0.001);
    }

    @Test
    void testFindByFunctionIdAndXNonExistent() {
        Point point = pointRepository.findByFunctionIdAndX(functionId1, 999.0);
        assertNull(point);

        Point point2 = pointRepository.findByFunctionIdAndX(999, 1.0);
        assertNull(point2);
    }

    @Test
    void testUpdate() {
        Point pointToUpdate = pointRepository.findByFunctionIdAndX(functionId1, 2.0);
        assertNotNull(pointToUpdate);

        pointToUpdate.setYValue(10.0);
        boolean updateResult = pointRepository.update(pointToUpdate);
        assertTrue(updateResult);

        Point updatedPoint = pointRepository.findByFunctionIdAndX(functionId1, 2.0);
        assertNotNull(updatedPoint);
        assertEquals(10.0, updatedPoint.getYValue(), 0.001);
    }

    @Test
    void testUpdateNonExistent() {
        Point nonExistentPoint = new Point(999, 999.0, 999.0);
        boolean updateResult = pointRepository.update(nonExistentPoint);
        assertFalse(updateResult);
    }

    @Test
    void testDelete() {
        boolean deleteResult = pointRepository.delete(functionId1, 2.0);
        assertTrue(deleteResult);

        Point deletedPoint = pointRepository.findByFunctionIdAndX(functionId1, 2.0);
        assertNull(deletedPoint);

        List<Point> points = pointRepository.findByFunctionId(functionId1);
        assertEquals(2, points.size());
    }

    @Test
    void testDeleteNonExistent() {
        boolean deleteResult = pointRepository.delete(999, 999.0);
        assertFalse(deleteResult);
    }

    @Test
    void testFindAllSortedByX() {
        clearPointsTable();

        // добавляем точки в разном порядке по X
        pointRepository.insert(new Point(functionId1, 3.0, 30.0));
        pointRepository.insert(new Point(functionId1, 1.0, 10.0));
        pointRepository.insert(new Point(functionId1, 2.0, 20.0));

        List<Point> sortedPoints = pointRepository.findAllSortedByX();

        assertEquals(3, sortedPoints.size());
        assertEquals(1.0, sortedPoints.get(0).getXValue(), 0.001);
        assertEquals(2.0, sortedPoints.get(1).getXValue(), 0.001);
        assertEquals(3.0, sortedPoints.get(2).getXValue(), 0.001);
    }

    @Test
    void testFindAllSortedByY() {
        clearPointsTable();

        // добавляем точки в разном порядке по Y
        pointRepository.insert(new Point(functionId1, 10.0, 3.0));
        pointRepository.insert(new Point(functionId1, 20.0, 1.0));
        pointRepository.insert(new Point(functionId1, 30.0, 2.0));

        List<Point> sortedPoints = pointRepository.findAllSortedByY();

        assertEquals(3, sortedPoints.size());
        assertEquals(1.0, sortedPoints.get(0).getYValue(), 0.001);
        assertEquals(2.0, sortedPoints.get(1).getYValue(), 0.001);
        assertEquals(3.0, sortedPoints.get(2).getYValue(), 0.001);
    }
    // метод для очистки таблицы точек
    private void clearPointsTable() {
        if (functionId1 != null) {
            List<Point> pts1 = pointRepository.findByFunctionId(functionId1);
            for (Point p : pts1) {
                pointRepository.delete(functionId1, p.getXValue());
            }
        }
        if (functionId2 != null) {
            List<Point> pts2 = pointRepository.findByFunctionId(functionId2);
            for (Point p : pts2) {
                pointRepository.delete(functionId2, p.getXValue());
            }
        }
    }
    // метод для очистки таблицы функций
    private void clearFunctionsTable() {
        List<Function> functions = functionRepository.findAll();
        for (Function function : functions) {
            if (function.getId() != null) {
                functionRepository.delete(function.getId());
            }
        }
    }
    // метод для очистки таблицы пользователей
    private void clearUsersTable() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getId() != null) {
                userRepository.delete(user.getId());
            }
        }
    }
}