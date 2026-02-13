package com.example.monitoring.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserForm {
    private Long id;

    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 3, max = 50, message = "아이디는 3자 이상 50자 이하여야 합니다")
    private String username;

    @Size(min = 4, max = 100, message = "비밀번호는 4자 이상 100자 이하여야 합니다")
    private String password;

    @NotBlank(message = "성명은 필수입니다")
    @Size(max = 100, message = "성명은 100자 이하여야 합니다")
    private String name;

    @Size(max = 100, message = "부서는 100자 이하여야 합니다")
    private String department;

    @Size(max = 50, message = "직급은 50자 이하여야 합니다")
    private String position;

    @NotBlank(message = "역할은 필수입니다")
    private String role = "USER";

    private Boolean enabled = true;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
