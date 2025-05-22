package model;

// 用户模型类
public class User {
    private String username;
    private String password;
    private String role; // e.g., "user", "admin"
    private String transactionFilePath; // Path to the user's transaction CSV file

    // Constructors
    public User() {
    }

    public User(String username, String password, String role, String transactionFilePath) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.transactionFilePath = transactionFilePath;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTransactionFilePath() {
        return transactionFilePath;
    }

    public void setTransactionFilePath(String transactionFilePath) {
        this.transactionFilePath = transactionFilePath;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", transactionFilePath='" + transactionFilePath + '\'' +
                '}';
    }
}