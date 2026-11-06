package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;

@Entity
@Table(name="notifications")
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @Column(length=255) private String title;
    @Column(columnDefinition="text") private String content;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name="reference_type",columnDefinition="notification_type_enum") private NotificationType referenceType=NotificationType.SYSTEM;
    @Column(name="reference_id") private UUID referenceId;
    @Column(name="is_read") private Boolean read=false;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @PrePersist void create(){if(createdAt==null)createdAt=OffsetDateTime.now();if(read==null)read=false;if(referenceType==null)referenceType=NotificationType.SYSTEM;}
    public UUID getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getContent(){return content;} public void setContent(String v){content=v;}
    public NotificationType getReferenceType(){return referenceType;} public void setReferenceType(NotificationType v){referenceType=v;}
    public UUID getReferenceId(){return referenceId;} public void setReferenceId(UUID v){referenceId=v;}
    public Boolean getRead(){return read;} public void setRead(Boolean v){read=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
