package models;

public class Customer {
    private String username;
    private String password;
    private String address;
    private String cellNo;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCellNo() { return cellNo; }
    public void setCellNo(String cellNo) { this.cellNo = cellNo; }
}