package com.example.backend.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.backend.entity.Notification;
public interface NotificationRepository extends JpaRepository<Notification,UUID> {
    List<Notification> findTop100ByUser_IdOrderByCreatedAtDesc(UUID userId);
    long countByUser_IdAndReadFalse(UUID userId);
    java.util.Optional<Notification> findByIdAndUser_Id(UUID id,UUID userId);
    @Modifying @Query("update Notification n set n.read=true where n.user.id=:userId and n.read=false")
    int markAllReadByUser_Id(@Param("userId") UUID userId);
}
