package repository;

import DTO.Function;
import DTO.User;
import JDBC.repository.FunctionRepository;
import JDBC.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FunctionRepositoryTest {
    private FunctionRepository functionRepository;
    private UserRepository userRepository;
    private Integer functionId1;
    private Integer functionId2;
    private Integer functionId3;
    Integer userId1;
    Integer userId2;

    @BeforeEach
    void setUp() {
        functionRepository = new FunctionRepository();
        userRepository = new UserRepository();
        // очищаем таблицу перед тестами
        clearFunctionsTable();
        clearUsersTable();
        User user = new User("bob", "123");
        userId1 = userRepository.insert(user);
        User user2 = new User("pop", "456");
        userId2 = userRepository.insert(user2);
        // добавляем функции
        functionId1 = functionRepository.insert(new Function("sin(x)", "Math.sin(x)", userId1));
        functionId2 = functionRepository.insert(new Function("cos(x)", "Math.cos(x)", userId1));
        functionId3 = functionRepository.insert(new Function("x^2", "x * x", userId2));
    }

    @AfterEach
    void tearDown() {
        // очищаем таблицу после тестов
        clearFunctionsTable();
    }

    @Test
    void testInsert() {
        Function newFunction = new Function("tan(x)", "Math.tan(x)", userId1);
        Integer newId = functionRepository.insert(newFunction);
        assertNotNull(newId);
        assertTrue(newId > 0);

        Function foundFunction = functionRepository.findById(newId);
        assertNotNull(foundFunction);
        assertEquals("tan(x)", foundFunction.getName());
        assertEquals("Math.tan(x)", foundFunction.getExpression());
        assertEquals(userId1, foundFunction.getUserId());
    }

    @Test
    void testFindById() {
        Function function = functionRepository.findById(functionId1);
        assertNotNull(function);
        assertEquals(functionId1, function.getId());
        assertEquals("sin(x)", function.getName());
        assertEquals("Math.sin(x)", function.getExpression());
        assertEquals(userId1, function.getUserId());
    }

    @Test
    void testFindByIdNonExistent() {
        Function function = functionRepository.findById(99999);
        assertNull(function);
    }

    @Test
    void testFindAll() {
        List<Function> functions = functionRepository.findAll();
        assertEquals(3, functions.size());

        boolean foundSin = false, foundCos = false, foundX2 = false;
        for (Function f : functions) {
            if ("sin(x)".equals(f.getName())) foundSin = true;
            if ("cos(x)".equals(f.getName())) foundCos = true;
            if ("x^2".equals(f.getName())) foundX2 = true;
        }
        assertTrue(foundSin);
        assertTrue(foundCos);
        assertTrue(foundX2);
    }

    @Test
    void testFindByUserId() {
        List<Function> user1Functions = functionRepository.findByUserId(userId1);
        assertEquals(2, user1Functions.size());

        List<Function> nonExistentUserFunctions = functionRepository.findByUserId(999);
        assertNotNull(nonExistentUserFunctions);
        assertTrue(nonExistentUserFunctions.isEmpty());
    }

    @Test
    void testUpdate() {
        Function functionToUpdate = functionRepository.findById(functionId1);
        assertNotNull(functionToUpdate);

        functionToUpdate.setName("updated_sin(x)");
        functionToUpdate.setExpression("Math.sin(x) * 2");
        functionToUpdate.setUserId(userId2);

        boolean updateResult = functionRepository.update(functionToUpdate);
        assertTrue(updateResult);

        Function updatedFunction = functionRepository.findById(functionId1);
        assertNotNull(updatedFunction);
        assertEquals("updated_sin(x)", updatedFunction.getName());
        assertEquals("Math.sin(x) * 2", updatedFunction.getExpression());
        assertEquals(userId2, updatedFunction.getUserId());
    }

    @Test
    void testUpdateNonExistent() {
        Function nonExistentFunction = new Function("nonexistent", "none", 999);
        nonExistentFunction.setId(99999);

        boolean updateResult = functionRepository.update(nonExistentFunction);
        assertFalse(updateResult);
    }

    @Test
    void testDeleteFunctionOfUser2() {
        assertTrue(functionRepository.delete(functionId2));
        assertNull(functionRepository.findById(functionId2));
        assertEquals(2, functionRepository.findAll().size());
    }

    @Test
    void testDeleteNonExistent() {
        boolean deleteResult = functionRepository.delete(99999);
        assertFalse(deleteResult);
    }

    @Test
    void testFindAllSortedByName() {
        clearFunctionsTable();
        assertNotNull(userId1, "userId1 must be initialized in setUp()");

        // добавляем функции в произвольном порядке
        functionRepository.insert(new Function("cosine", "Math.cos(x)", userId1));
        functionRepository.insert(new Function("alpha", "x + 1", userId1));
        functionRepository.insert(new Function("beta", "x - 1", userId1));

        // отсортированный список
        List<Function> sortedFunctions = functionRepository.findAllSortedByName();

        // проверка
        assertEquals(3, sortedFunctions.size());
        assertEquals("alpha", sortedFunctions.get(0).getName());
        assertEquals("beta", sortedFunctions.get(1).getName());
        assertEquals("cosine", sortedFunctions.get(2).getName());
    }

    @Test
    void testFindAllSortedByNameEmpty() {
        clearFunctionsTable();

        List<Function> sortedFunctions = functionRepository.findAllSortedByName();
        assertNotNull(sortedFunctions);
        assertTrue(sortedFunctions.isEmpty());
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