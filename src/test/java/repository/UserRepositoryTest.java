package repository;

import DTO.User;
import JDBC.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {
    private UserRepository userRepository;
    private Integer userId1;
    private Integer userId2;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        // очищаем таблицу перед тестами
        clearUsersTable();
        // добавляем пользователей
        userId1 = userRepository.insert(new User("user1", "hash1"));
        userId2 = userRepository.insert(new User("user2", "hash2"));
    }

    @AfterEach
    void tearDown() {
        // очищаем таблицу после тестов
        clearUsersTable();
    }

    @Test
    void testInsert() {
        Integer newId = userRepository.insert(new User("new user", "new pass_hash"));
        User foundUser = userRepository.findById(newId);
        assertNotNull(foundUser);
        assertEquals("new user", foundUser.getUsername());
        assertEquals("new pass_hash", foundUser.getPasswordHash());
    }

    @Test
    void testFindById() {
        User user = userRepository.findById(userId1);
        assertNotNull(user);
        assertEquals(userId1, user.getId());
        assertEquals("user1", user.getUsername());
        assertEquals("hash1", user.getPasswordHash());
    }

    @Test
    void testFindByIdNonExistent() {
        User user = userRepository.findById(99999);
        assertNull(user);
    }

    @Test
    void testFindAll() {
        List<User> users = userRepository.findAll();
        assertEquals(2, users.size());
    }
    @Test
    void testFindByUsername() {
        User foundUser = userRepository.findByUsername("user1");
        assertNotNull(foundUser);
        assertEquals(userId1, foundUser.getId());
        assertEquals("user1", foundUser.getUsername());
        assertEquals("hash1", foundUser.getPasswordHash());
    }

    @Test
    void testFindByUsernameNonExistent() {
        User foundUser = userRepository.findByUsername("nonexistent_user");
        assertNull(foundUser);
    }

    @Test
    void testUpdate() {
        User userToUpdate = userRepository.findById(userId1);
        assertNotNull(userToUpdate);

        userToUpdate.setUsername("updated_user1");
        userToUpdate.setPasswordHash("new_hash_123");

        boolean updateResult = userRepository.update(userToUpdate);
        assertTrue(updateResult);

        User updatedUser = userRepository.findById(userId1);
        assertNotNull(updatedUser);
        assertEquals("updated_user1", updatedUser.getUsername());
        assertEquals("new_hash_123", updatedUser.getPasswordHash());
    }

    @Test
    void testUpdateNonExistent() {
        User nonExistentUser = new User("nonexistent", "hash");
        nonExistentUser.setId(99999);

        boolean updateResult = userRepository.update(nonExistentUser);
        assertFalse(updateResult);
    }

    @Test
    void testDelete() {
        boolean deleteResult = userRepository.delete(userId2);
        assertTrue(deleteResult);

        User deletedUser = userRepository.findById(userId2);
        assertNull(deletedUser);

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
    }

    @Test
    void testDeleteNonExistent() {
        boolean deleteResult = userRepository.delete(99999);
        assertFalse(deleteResult);
    }

    @Test
    void testFindAllSortedByUsername() {
        // Добавляем пользователей в разном порядке
        clearUsersTable();

        userRepository.insert(new User("charlie", "123"));
        userRepository.insert(new User("alice", "122"));
        userRepository.insert(new User("bob", "121"));

        List<User> sortedUsers = userRepository.findAllSortedByUsername();

        assertEquals(3, sortedUsers.size());
        assertEquals("alice", sortedUsers.get(0).getUsername());
        assertEquals("bob", sortedUsers.get(1).getUsername());
        assertEquals("charlie", sortedUsers.get(2).getUsername());
    }

    @Test
    void testFindAllSortedByUsernameEmpty() {
        clearUsersTable();

        List<User> sortedUsers = userRepository.findAllSortedByUsername();
        assertNotNull(sortedUsers);
        assertTrue(sortedUsers.isEmpty());
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