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
    void addUser(User user) throws IOException, IllegalArgumentException;

    /**
     * Deletes a user identified by username.
     *
     * @param username The username of the user to delete.
     * @return true if a user was found and deleted, false otherwise.
     * @throws IOException If an I/O error occurs during loading or saving.
     */
    boolean deleteUser(String username) throws IOException; // NEW: Delete user method

    /**
     * Updates an existing user's information.
     *
     * @param updatedUser The User object with updated information (matched by username).
     * @return true if the user was found and updated, false otherwise.
     * @throws IOException If an I/O error occurs during loading or saving.
     * @throws IllegalArgumentException If updatedUser data is invalid or username is missing.
     */
    boolean updateUser(User updatedUser) throws IOException, IllegalArgumentException; // NEW: Update user method
}