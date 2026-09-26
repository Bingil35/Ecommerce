package com.example.backend.dto.response;

import java.util.UUID;

import com.example.backend.entity.User;

public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String roleDefault;
    private String status;

    public UserResponse() {
    }

    public UserResponse(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.avatarUrl = user.getAvatarUrl();
        this.roleDefault = user.getRoleDefault();
        this.status = user.getStatus().name();
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRoleDefault() {
        return roleDefault;
    }

    public String getStatus() {
        return status;
    }
}