package com.example.backend.service;

import com.example.backend.dto.request.SaveCustomerAddressRequest;
import com.example.backend.dto.response.CustomerAddressResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.UserAddress;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.UserAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerAddressService {
    private final UserService userService;
    private final UserAddressRepository addressRepository;

    public CustomerAddressService(UserService userService, UserAddressRepository addressRepository) {
        this.userService = userService;
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> list(String email) {
        User user = userService.getUserByEmail(email);
        return addressRepository.findAllByUser_IdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream().map(CustomerAddressResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CustomerAddressResponse get(String email, UUID id) {
        return CustomerAddressResponse.from(findOwned(email, id));
    }

    @Transactional
    public CustomerAddressResponse create(String email, SaveCustomerAddressRequest request) {
        User user = userService.getUserByEmail(email);
        UserAddress address = new UserAddress();
        address.setUser(user);
        apply(address, request);
        if (Boolean.TRUE.equals(request.getIsDefault()) ||
                !addressRepository.existsByUser_Id(user.getId())) {
            clearDefault(user.getId());
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }
        return CustomerAddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public CustomerAddressResponse update(String email, UUID id, SaveCustomerAddressRequest request) {
        UserAddress address = findOwned(email, id);
        apply(address, request);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(address.getUser().getId());
            address.setIsDefault(true);
        }
        return CustomerAddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(String email, UUID id) {
        UserAddress address = findOwned(email, id);
        UUID userId = address.getUser().getId();
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        if (wasDefault) {
            List<UserAddress> remaining = addressRepository.findAllByUser_IdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsDefault(true);
                addressRepository.save(remaining.get(0));
            }
        }
    }

    @Transactional
    public CustomerAddressResponse setDefault(String email, UUID id) {
        UserAddress address = findOwned(email, id);
        clearDefault(address.getUser().getId());
        address.setIsDefault(true);
        return CustomerAddressResponse.from(addressRepository.save(address));
    }

    private UserAddress findOwned(String email, UUID id) {
        User user = userService.getUserByEmail(email);
        return addressRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ nhận hàng."));
    }

    private void clearDefault(UUID userId) {
        addressRepository.findAllByUser_IdOrderByIsDefaultDescCreatedAtDesc(userId).forEach(a -> {
            if (Boolean.TRUE.equals(a.getIsDefault())) a.setIsDefault(false);
        });
    }

    private void apply(UserAddress a, SaveCustomerAddressRequest r) {
        a.setRecipientName(r.getRecipientName().trim());
        a.setRecipientPhone(r.getRecipientPhone().trim());
        a.setProvinceCity(r.getProvinceCity().trim());
        a.setWard(r.getWard().trim());
        a.setAddressDetail(r.getAddressDetail().trim());
    }
}
