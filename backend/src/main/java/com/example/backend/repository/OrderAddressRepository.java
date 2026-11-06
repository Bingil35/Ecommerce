package com.example.backend.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.OrderAddress;
public interface OrderAddressRepository extends JpaRepository<OrderAddress, UUID> {
}
