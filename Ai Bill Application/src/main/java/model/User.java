package model;

import lombok.*;

// User model class
@Getter
@Setter
@Builder
@AllArgsConstructor
public class User {
    // Getters and Setters
    private String username;
    private String password;
    private String role; // e.g., "user", "admin"
    private String transactionFilePath; // Path to the user's transaction CSV file
    // NEW Getters and Setters for summaryFilePath
    // It should return the value of the summaryFilePath field
    private String summaryFilePath; // Path to the user's summary CSV file
    // Constructors
    public User() {
    }

    public User(String username, String password, String role, String transactionFilePath) {
        this.username = username;
        this.password = password;
        this.role = role;
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