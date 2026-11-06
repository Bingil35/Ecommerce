package com.example.backend.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;

@Service
public class NotificationService {
    private final NotificationRepository notifications; private final UserRepository users; private final UserRoleRepository userRoles; private final ShopRepository shops; private final ShopFollowerRepository followers;
    public NotificationService(NotificationRepository notifications,UserRepository users,UserRoleRepository userRoles,ShopRepository shops,ShopFollowerRepository followers){this.notifications=notifications;this.users=users;this.userRoles=userRoles;this.shops=shops;this.followers=followers;}
    @Transactional(readOnly=true) public List<Notification> list(String email){User u=user(email);return notifications.findTop100ByUser_IdOrderByCreatedAtDesc(u.getId());}
    @Transactional(readOnly=true) public long unreadCount(String email){return notifications.countByUser_IdAndReadFalse(user(email).getId());}
    @Transactional public void markRead(String email,UUID id){Notification n=notifications.findByIdAndUser_Id(id,user(email).getId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy thông báo."));n.setRead(true);}
    @Transactional public void markAllRead(String email){notifications.markAllReadByUser_Id(user(email).getId());}
    @Transactional public void notifyUser(UUID userId,String title,String content,NotificationType type,UUID referenceId){users.findById(userId).ifPresent(u->save(u,title,content,type,referenceId));}
    @Transactional public void notifyRole(String role,String title,String content,NotificationType type,UUID referenceId){
        List<User> recipients=userRoles.findUserIdsByRoleName(role).stream().distinct().map(users::findById).flatMap(Optional::stream).filter(u->u.getStatus()==UserStatus.ACTIVE).toList();
        recipients.forEach(u->save(u,title,content,type,referenceId));
    }
    @Transactional public void notifyShopOwner(UUID shopId,String title,String content,NotificationType type,UUID referenceId){shops.findById(shopId).ifPresent(s->{if(s.getOwnerUserId()!=null)notifyUser(s.getOwnerUserId(),title,content,type,referenceId);});}
    @Transactional public void notifyShopFollowers(UUID shopId,String title,String content,NotificationType type,UUID referenceId){followers.findFollowerUserIds(shopId).stream().distinct().forEach(id->notifyUser(id,title,content,type,referenceId));}
    private User user(String email){return users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));}
    private void save(User user,String title,String content,NotificationType type,UUID referenceId){Notification n=new Notification();n.setUser(user);n.setTitle(title);n.setContent(content);n.setReferenceType(type);n.setReferenceId(referenceId);n.setRead(false);notifications.save(n);}
}
