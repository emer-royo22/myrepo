package models;

public class User {
    private int id;
    private String username;
    private String password;
    private String role_id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role_id; }
    public void setRole(String role_id) { this.role_id = role_id; }
    
}
