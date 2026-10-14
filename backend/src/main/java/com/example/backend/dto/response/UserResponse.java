package com.example.backend.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.backend.entity.User;

public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    // Kept for compatibility with the existing frontend contract.
    private String roleDefault;
    private List<String> roles = new ArrayList<>();
    private String status;

    public UserResponse() {
    }

    public UserResponse(User user, List<String> roles) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.avatarUrl = user.getAvatarUrl();
        this.roles = roles == null ? new ArrayList<>() : new ArrayList<>(roles);
        this.roleDefault = this.roles.isEmpty() ? null : this.roles.get(0);
        this.status = user.getStatus() == null ? null : user.getStatus().name();
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getRoleDefault() { return roleDefault; }
    public List<String> getRoles() { return roles; }
    public String getStatus() { return status; }
}
