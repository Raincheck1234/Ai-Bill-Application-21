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

    /**
     * Adds a new user to the data source.
     *
     * @param user The new user to add.
     * @throws IOException If an I/O error occurs during saving.
     * @throws IllegalArgumentException If the user data is invalid (e.g., null fields).
     */
    void addUser(User user) throws IOException, IllegalArgumentException; // NEW: Add user method

    // Add other user-related methods if needed later (e.g., deleteUser, updateUser)
}