package Service.User; // Changed package

import DAO.UserDao;
import model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {
    private final UserDao userDao;
    private final Map<String, User> userCache = new HashMap<>(); // Cache users in memory

    public UserService(UserDao userDao) {
        this.userDao = userDao;
        loadUsers(); // Load users when the service is initialized
    }

    private void loadUsers() {
        try {
            List<User> users = userDao.getAllUsers();
            userCache.clear(); // Clear previous cache
            for (User user : users) {
                userCache.put(user.getUsername(), user);
            }
            System.out.println("Loaded " + userCache.size() + " users into cache.");
        } catch (IOException e) {
            System.err.println("Failed to load users from data source.");
            e.printStackTrace();
            // Depending on requirements, you might want to exit or handle this more gracefully
            // For now, we'll allow the app to run with an empty user list, though login will fail.
        }
    }

    /**
     * Authenticates a user.
     *
     * @param username The username.
     * @param password The password.
     * @return The authenticated User object if successful, null otherwise.
     */
    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }
        User user = userCache.get(username.trim
       ());
        if (user != null && user.getPassword().equals(password.trim())) { // Simple password check
            System.out.println("Authentication successful for user: " + username);
            return user; // Authentication successful, return the User object
        }
        System.out.println("Authentication failed for username: " + username);
        return null; // Authentication failed
    }

    /**
     * Retrieves a user by username from the cache.
     * @param username The username.
     * @return The User object or null if not found.
     */
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userCache.get(username.trim());
    }


    // Add other user management methods if needed (e.g., registerUser, deleteUser)
}