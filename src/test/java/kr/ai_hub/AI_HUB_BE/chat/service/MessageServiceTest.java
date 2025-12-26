package kr.ai_hub.AI_HUB_BE.chat.service;

import kr.ai_hub.AI_HUB_BE.chat.dto.MessageListItemResponse;
import kr.ai_hub.AI_HUB_BE.chat.dto.MessageResponse;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModel;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.chat.domain.ChatRoom;
import kr.ai_hub.AI_HUB_BE.chat.domain.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.chat.domain.Message;
import kr.ai_hub.AI_HUB_BE.chat.domain.MessageRepository;
import kr.ai_hub.AI_HUB_BE.chat.domain.MessageRole;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWallet;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.MessageNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.RoomNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIModelRepository aiModelRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private MessageTransactionService messageTransactionService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private MessageRequestBuilder messageRequestBuilder;

    @Mock
    private AiSseHandler aiSseHandler;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private MessageService messageService;

    private User user;
    private ChatRoom chatRoom;
    private Message message;
    private AIModel aiModel;
    private UserWallet userWallet;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .isActivated(true)
                .build();

        chatRoom = ChatRoom.builder()
                .roomId(UUID.randomUUID())
                .user(user)
                .title("Test Room")
                .build();

        aiModel = AIModel.builder()
                .modelId(1)
                .modelName("GPT-4")
                .isActive(true)
                .build();

        message = Message.builder()
                .messageId(UUID.randomUUID())
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content("Hello")
                .aiModel(aiModel)
                .createdAt(LocalDateTime.now())
                .build();

        userWallet = UserWallet.builder()
                .walletId(1)
                .user(user)
                .balance(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    @DisplayName("메시지 목록 조회 성공")
    void getMessages_Success() {
        // given
        UUID roomId = chatRoom.getRoomId();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> messagePage = new PageImpl<>(List.of(message));

        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(messageRepository.findByChatRoom(chatRoom, pageable)).willReturn(messagePage);

        // when
        Page<MessageListItemResponse> result = messageService.getMessages(roomId, pageable);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("메시지 목록 조회 실패 - 권한 없음")
    void getMessages_Forbidden() {
        // given
        UUID roomId = chatRoom.getRoomId();
        Pageable pageable = PageRequest.of(0, 10);

        given(securityContextHelper.getCurrentUserId()).willReturn(2); // Different user
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));

        // when & then
        assertThatThrownBy(() -> messageService.getMessages(roomId, pageable))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("메시지 상세 조회 성공")
    void getMessage_Success() {
        // given
        UUID messageId = message.getMessageId();
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when
        MessageResponse response = messageService.getMessage(messageId);

        // then
        assertThat(response.content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("메시지 상세 조회 실패 - 메시지 없음")
    void getMessage_NotFound() {
        // given
        UUID messageId = UUID.randomUUID();
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(messageRepository.findById(messageId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageService.getMessage(messageId))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    @DisplayName("메시지 상세 조회 실패 - 권한 없음")
    void getMessage_Forbidden() {
        // given
        UUID messageId = message.getMessageId();
        given(securityContextHelper.getCurrentUserId()).willReturn(2); // Different user
        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageService.getMessage(messageId))
                .isInstanceOf(ForbiddenException.class);
    }
}
