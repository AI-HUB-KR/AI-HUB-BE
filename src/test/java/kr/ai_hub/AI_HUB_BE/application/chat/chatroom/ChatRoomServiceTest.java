package kr.ai_hub.AI_HUB_BE.application.chat.chatroom;

import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.ChatRoomListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.ChatRoomResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.CreateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.UpdateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.Message;
import kr.ai_hub.AI_HUB_BE.domain.chat.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ModelNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.RoomNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIModelRepository aiModelRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User user;
    private AIModel aiModel;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .isActivated(true)
                .build();

        aiModel = AIModel.builder()
                .modelId(1)
                .modelName("GPT-4")
                .isActive(true)
                .build();

        chatRoom = ChatRoom.builder()
                .roomId(UUID.randomUUID())
                .user(user)
                .title("Test Room")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("채팅방 생성 성공")
    void createChatRoom_Success() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest("New Room", 1);
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(aiModelRepository.findById(1)).willReturn(Optional.of(aiModel));
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);

        // when
        ChatRoomResponse response = chatRoomService.createChatRoom(request);

        // then
        assertThat(response).isNotNull();
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 사용자 없음")
    void createChatRoom_UserNotFound() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest("New Room", 1);
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.createChatRoom(request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("채팅방 생성 실패 - AI 모델 없음")
    void createChatRoom_ModelNotFound() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest("New Room", 1);
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(aiModelRepository.findById(1)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.createChatRoom(request))
                .isInstanceOf(ModelNotFoundException.class);
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 비활성 AI 모델")
    void createChatRoom_ModelInactive() {
        // given
        AIModel inactiveModel = AIModel.builder().modelId(1).isActive(false).build();
        CreateChatRoomRequest request = new CreateChatRoomRequest("New Room", 1);
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(aiModelRepository.findById(1)).willReturn(Optional.of(inactiveModel));

        // when & then
        assertThatThrownBy(() -> chatRoomService.createChatRoom(request))
                .isInstanceOf(ModelNotFoundException.class)
                .hasMessage("해당 AI 모델은 현재 사용할 수 없습니다");
    }

    @Test
    @DisplayName("채팅방 목록 조회 성공")
    void getChatRooms_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ChatRoom> chatRoomPage = new PageImpl<>(List.of(chatRoom));

        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(chatRoomRepository.findByUser(user, pageable)).willReturn(chatRoomPage);
        given(messageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom)).willReturn(Optional.empty());

        // when
        Page<ChatRoomListItemResponse> result = chatRoomService.getChatRooms(pageable);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Test Room");
    }

    @Test
    @DisplayName("채팅방 상세 조회 성공")
    void getChatRoom_Success() {
        // given
        UUID roomId = chatRoom.getRoomId();
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));

        // when
        ChatRoomResponse response = chatRoomService.getChatRoom(roomId);

        // then
        assertThat(response.title()).isEqualTo("Test Room");
    }

    @Test
    @DisplayName("채팅방 상세 조회 실패 - 권한 없음")
    void getChatRoom_Forbidden() {
        // given
        UUID roomId = chatRoom.getRoomId();
        given(securityContextHelper.getCurrentUserId()).willReturn(2); // Different user
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));

        // when & then
        assertThatThrownBy(() -> chatRoomService.getChatRoom(roomId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("채팅방 수정 성공")
    void updateChatRoom_Success() {
        // given
        UUID roomId = chatRoom.getRoomId();
        UpdateChatRoomRequest request = new UpdateChatRoomRequest("Updated Room");
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));

        // when
        ChatRoomResponse response = chatRoomService.updateChatRoom(roomId, request);

        // then
        assertThat(response.title()).isEqualTo("Updated Room");
    }

    @Test
    @DisplayName("채팅방 삭제 성공")
    void deleteChatRoom_Success() {
        // given
        UUID roomId = chatRoom.getRoomId();
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));

        // when
        chatRoomService.deleteChatRoom(roomId);

        // then
        verify(chatRoomRepository).delete(chatRoom);
    }
}
