package com.example.backend.controller;

import com.example.backend.dto.request.SaveCustomerAddressRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CustomerAddressResponse;
import com.example.backend.service.CustomerAddressService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/addresses")
public class CustomerAddressController {
    private final CustomerAddressService service;
    public CustomerAddressController(CustomerAddressService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<CustomerAddressResponse>> list(Authentication auth) {
        return ApiResponse.success("Lấy danh sách địa chỉ thành công.", service.list(auth.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerAddressResponse> get(Authentication auth, @PathVariable UUID id) {
        return ApiResponse.success("Lấy địa chỉ thành công.", service.get(auth.getName(), id));
    }

    @PostMapping
    public ApiResponse<CustomerAddressResponse> create(Authentication auth,
            @Valid @RequestBody SaveCustomerAddressRequest request) {
        return ApiResponse.success("Thêm địa chỉ thành công.", service.create(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerAddressResponse> update(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody SaveCustomerAddressRequest request) {
        return ApiResponse.success("Cập nhật địa chỉ thành công.", service.update(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable UUID id) {
        service.delete(auth.getName(), id);
        return ApiResponse.success("Xóa địa chỉ thành công.", null);
    }

    @PutMapping("/{id}/default")
    public ApiResponse<CustomerAddressResponse> setDefault(Authentication auth, @PathVariable UUID id) {
        return ApiResponse.success("Đặt địa chỉ mặc định thành công.", service.setDefault(auth.getName(), id));
    }
}
