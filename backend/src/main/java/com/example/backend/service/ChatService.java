package com.example.backend.service;

import com.example.backend.dto.response.ChatConversationResponse;
import com.example.backend.dto.response.ChatMessageResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ChatMessageType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final ObjectMapper objectMapper;
    private final MediaFileInspector mediaInspector;
    private final LocalStorageService storage;

    public ChatService(ChatConversationRepository conversations, ChatMessageRepository messages,
            UserRepository users, ShopRepository shops, ProductRepository products, ObjectMapper objectMapper,
            MediaFileInspector mediaInspector, LocalStorageService storage) {
        this.conversations = conversations; this.messages = messages; this.users = users; this.shops = shops;
        this.products = products; this.objectMapper = objectMapper;
        this.mediaInspector = mediaInspector; this.storage = storage;
    }

    @Transactional(readOnly = true)
    public StoredChatMedia uploadMedia(UUID conversationId, UUID senderId, MultipartFile file) {
        ChatConversation conversation = getConversation(conversationId);
        assertParticipant(conversation, senderId);
        MediaFileInspector.Detected media = mediaInspector.inspect(file);
        return new StoredChatMedia(storage.storeChatMedia(conversationId, file, media.extension()), media.type().name());
    }

    public record StoredChatMedia(String url, String messageType) {}

    @Transactional
    public ChatConversationResponse start(String email, UUID shopId) {
        User customer = users.findByEmailForUpdate(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Shop shop = shops.findById(shopId).filter(s -> s.getStatus() == ShopStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng đang hoạt động."));
        if (shop.getOwnerUserId().equals(customer.getId())) throw new IllegalArgumentException("Chủ cửa hàng không thể tự mở cuộc trò chuyện với shop của mình.");
        ChatConversation conversation = conversations.findByCustomer_IdAndShop_Id(customer.getId(), shopId).orElseGet(() -> {
            ChatConversation created = new ChatConversation(); created.setCustomer(customer); created.setShop(shop);
            return conversations.save(created);
        });
        return toResponse(conversation, customer.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> customerConversations(String email) {
        User customer = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        return conversations.findByCustomer_IdOrderByLastMessageAtDesc(customer.getId()).stream().map(c -> toResponse(c, customer.getId())).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> ownerConversations(String email) {
        User owner = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Shop shop = shops.findByOwnerUserId(owner.getId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng."));
        return conversations.findByShop_IdOrderByLastMessageAtDesc(shop.getId()).stream().map(c -> toResponse(c, owner.getId())).toList();
    }

    @Transactional
    public List<ChatMessageResponse> history(UUID conversationId, UUID userId, int size) {
        ChatConversation conversation = getConversation(conversationId);
        assertParticipant(conversation, userId);
        List<ChatMessage> result = messages.findByConversation_IdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, Math.min(Math.max(size, 1), 200)));
        java.util.Collections.reverse(result);
        result.stream().filter(m -> !m.isRead() && !m.getSenderId().equals(userId)).forEach(m -> m.setRead(true));
        return result.stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public ChatMessageResponse send(UUID conversationId, UUID senderId, String content, String requestedType) {
        String cleaned = content == null ? "" : content.trim();
        ChatMessageType type;
        try { type = requestedType == null ? ChatMessageType.TEXT : ChatMessageType.valueOf(requestedType.toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Loại tin nhắn không được hỗ trợ."); }
        if (type == ChatMessageType.PRODUCT) {
            if (cleaned.length() > 2000) throw new IllegalArgumentException("Liên kết sản phẩm không hợp lệ.");
        } else if (cleaned.isBlank() || cleaned.length() > 4000) throw new IllegalArgumentException("Tin nhắn phải có từ 1 đến 4000 ký tự.");
        if (type == ChatMessageType.IMAGE || type == ChatMessageType.VIDEO || type == ChatMessageType.FILE) {
            try {
                boolean localChatUpload = cleaned.startsWith("/uploads/chat/" + conversationId + "/") && !cleaned.contains("..");
                java.net.URI uri = java.net.URI.create(cleaned);
                boolean remoteUrl = uri.isAbsolute() && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
                if (!localChatUpload && !remoteUrl) throw new Exception();
            }
            catch (Exception ex) { throw new IllegalArgumentException("Tệp đính kèm cần có liên kết HTTP hoặc HTTPS hợp lệ."); }
        }
        ChatConversation conversation = getConversation(conversationId);
        assertParticipant(conversation, senderId);
        if (type == ChatMessageType.PRODUCT) cleaned = productCardContent(cleaned, conversation);
        ChatMessage message = new ChatMessage(); message.setConversation(conversation); message.setSenderId(senderId);
        message.setMessageType(type); message.setContent(cleaned); message.setRead(false);
        ChatMessage saved = messages.save(message);
        conversation.setLastMessageAt(saved.getCreatedAt() == null ? OffsetDateTime.now() : saved.getCreatedAt());
        return ChatMessageResponse.from(saved);
    }

    private String productCardContent(String link, ChatConversation conversation) {
        try {
            java.net.URI uri = java.net.URI.create(link);
            String path = uri.getPath();
            if (path == null || !path.matches(".*/product/[^/]+/?")) throw new IllegalArgumentException();
            String slug = path.replaceAll(".*/product/", "").replaceAll("/$", "");
            Product product = products.findBySlugAndShopIdAndStatus(slug, conversation.getShop().getId(), ProductStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Chỉ có thể chia sẻ sản phẩm đang bán của cửa hàng này."));
            return objectMapper.writeValueAsString(java.util.Map.of("url", link, "productId", product.getId().toString(),
                    "name", product.getName(), "imageUrl", product.getThumbnailUrl() == null ? "" : product.getThumbnailUrl()));
        } catch (tools.jackson.core.JacksonException ex) { throw new IllegalArgumentException("Không thể tạo thẻ sản phẩm."); }
        catch (IllegalArgumentException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalArgumentException("Liên kết sản phẩm không hợp lệ."); }
    }

    @Transactional(readOnly = true)
    public boolean canAccess(UUID conversationId, UUID userId) {
        ChatConversation conversation = conversations.findById(conversationId).orElse(null);
        return conversation != null && isParticipant(conversation, userId);
    }

    private ChatConversationResponse toResponse(ChatConversation c, UUID viewerId) {
        boolean customerView = c.getCustomer().getId().equals(viewerId);
        ChatMessage latest = messages.findFirstByConversation_IdOrderByCreatedAtDesc(c.getId()).orElse(null);
        String otherName = customerView ? c.getShop().getShopName() : c.getCustomer().getFullName();
        String avatar = customerView ? c.getShop().getLogoUrl() : c.getCustomer().getAvatarUrl();
        long unread = messages.countByConversation_IdAndSenderIdNotAndReadFalse(c.getId(), viewerId);
        return new ChatConversationResponse(c.getId(), c.getCustomer().getId(), c.getShop().getId(), otherName,
                avatar, preview(latest), c.getLastMessageAt(), unread,
                c.getShop().getShopName(), users.findById(c.getShop().getOwnerUserId()).map(User::getFullName).orElse("Chủ cửa hàng"), c.getShop().getSlug());
    }

    private String preview(ChatMessage message) {
        if (message == null) return null;
        if (message.getMessageType() == ChatMessageType.PRODUCT) {
            try { return "Sản phẩm: " + objectMapper.readTree(message.getContent()).path("name").asText("Đã chia sẻ sản phẩm"); }
            catch (Exception ignored) { return "Đã chia sẻ sản phẩm"; }
        }
        return switch (message.getMessageType()) {
            case IMAGE -> "Đã gửi hình ảnh";
            case VIDEO -> "Đã gửi video";
            case FILE -> "Đã gửi tệp";
            default -> message.getContent();
        };
    }

    private ChatConversation getConversation(UUID id) {
        return conversations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện."));
    }
    private void assertParticipant(ChatConversation c, UUID userId) {
        if (!isParticipant(c, userId)) throw new AccessDeniedException("Bạn không có quyền truy cập cuộc trò chuyện này.");
    }
    private boolean isParticipant(ChatConversation c, UUID userId) {
        return c.getCustomer().getId().equals(userId) || c.getShop().getOwnerUserId().equals(userId);
    }
}
