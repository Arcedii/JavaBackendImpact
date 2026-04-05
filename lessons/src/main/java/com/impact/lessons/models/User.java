package com.impact.lessons.models;

public class User {

    private Long id;
    private String username;
    private String email;
    private String password;
    private int role;
    private boolean enabled;

    public User(String username, String email, String password, int role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = true;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public int getRole() {
        return role;
    }
}
