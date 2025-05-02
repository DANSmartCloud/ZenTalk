package com.ZenTalk.model;

import java.time.LocalDateTime;
import java.util.List;

public class User {
    private String id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private String gender;
    private String birthday;
    private List<String> tags;
    private boolean invisible;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User() {}

    public static class Builder {
        private final User user;

        public Builder() {
            user = new User();
            user.createdAt = LocalDateTime.now();
            user.updatedAt = LocalDateTime.now();
            user.status = "active";
        }

        public Builder withUsername(String username) {
            user.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            user.password = password;
            return this;
        }

        public Builder withEmail(String email) {
            user.email = email;
            return this;
        }

        public Builder withPhone(String phone) {
            user.phone = phone;
            return this;
        }

        public User build() {
            return user;
        }
    }

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; } // Added getter for password
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public String getGender() { return gender; }
    public String getBirthday() { return birthday; }
    public List<String> getTags() { return tags; }
    public boolean isInvisible() { return invisible; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters for mutable fields
    public void setPassword(String password) { // Added setter for password
        this.password = password;
        this.updatedAt = LocalDateTime.now();
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
