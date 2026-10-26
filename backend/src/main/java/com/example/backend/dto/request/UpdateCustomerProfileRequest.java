package com.example.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateCustomerProfileRequest {
    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 255, message = "Email tối đa 255 ký tự.")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    @Pattern(regexp = "^(|[0-9+().\\s-]{7,20})$", message = "Số điện thoại không đúng định dạng.")
    private String phone;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
