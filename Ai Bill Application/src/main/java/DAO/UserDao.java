package DAO;

import model.User;

import java.io.IOException;
import java.util.List;

/**
 * Interface for Data Access Object (DAO) operations related to Users.
 */
public interface UserDao {

    /**
     * Loads all users from the configured data source.
     *
     * @return A list of all users.
     * @throws IOException If an I/O error occurs during loading.
     */
    List<User> getAllUsers() throws IOException;

    // Add other user-related methods if needed later (e.g., addUser, deleteUser)
}