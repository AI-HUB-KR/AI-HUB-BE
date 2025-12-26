package kr.ai_hub.AI_HUB_BE.chat.domain;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .isActivated(true)
                .build();
        userRepository.save(user);

        chatRoom = ChatRoom.builder()
                .user(user)
                .title("Test Room")
                .build();
        chatRoomRepository.save(chatRoom);
    }

    @Test
    @DisplayName("채팅방으로 메시지 목록 조회")
    void findByChatRoom() {
        // given
        Message message1 = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content("Hello")
                .build();
        Message message2 = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.ASSISTANT)
                .content("Hi")
                .build();
        messageRepository.saveAll(List.of(message1, message2));

        // when
        List<Message> result = messageRepository.findByChatRoom(chatRoom);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("content")
                .containsExactlyInAnyOrder("Hello", "Hi");
    }

    @Test
    @DisplayName("채팅방으로 메시지 목록 조회 - 생성일 오름차순")
    void findByChatRoomOrderByCreatedAtAsc() throws InterruptedException {
        // given
        Message message1 = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content("First")
                .build();
        messageRepository.save(message1);

        Thread.sleep(10);

        Message message2 = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.ASSISTANT)
                .content("Second")
                .build();
        messageRepository.save(message2);

        // when
        List<Message> result = messageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("First");
        assertThat(result.get(1).getContent()).isEqualTo("Second");
    }

    @Test
    @DisplayName("채팅방 ID로 메시지 목록 조회")
    void findByChatRoomRoomId() {
        // given
        Message message = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content("Test Content")
                .build();
        messageRepository.save(message);

        // when
        List<Message> result = messageRepository.findByChatRoomRoomId(chatRoom.getRoomId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test Content");
    }

    @Test
    @DisplayName("채팅방의 가장 최근 메시지 조회")
    void findTopByChatRoomOrderByCreatedAtDesc() throws InterruptedException {
        // given
        Message message1 = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content("Old")
                .build();
        messageRepository.save(message1);

        Thread.sleep(10);

        Message message2 = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.ASSISTANT)
                .content("New")
                .build();
        messageRepository.save(message2);

        // when
        Optional<Message> result = messageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("New");
    }

    @Test
    @DisplayName("채팅방 메시지 페이징 조회")
    void findByChatRoomWithPagination() {
        // given
        for (int i = 0; i < 15; i++) {
            messageRepository.save(Message.builder()
                    .chatRoom(chatRoom)
                    .role(MessageRole.USER)
                    .content("Message " + i)
                    .build());
        }

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        // when
        Page<Message> page = messageRepository.findByChatRoom(chatRoom, pageRequest);

        // then
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("사용자의 모든 메시지 조회 (채팅방 조인)")
    void findByChatRoomUser() {
        // given
        messageRepository.save(Message.builder().chatRoom(chatRoom).role(MessageRole.USER).content("Msg 1").build());
        messageRepository
                .save(Message.builder().chatRoom(chatRoom).role(MessageRole.ASSISTANT).content("Msg 2").build());

        // when
        List<Message> result = messageRepository.findByChatRoomUser(user);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("사용자의 총 메시지 수 조회")
    void countByUser() {
        // given
        messageRepository.save(Message.builder().chatRoom(chatRoom).role(MessageRole.USER).content("Msg 1").build());
        messageRepository
                .save(Message.builder().chatRoom(chatRoom).role(MessageRole.ASSISTANT).content("Msg 2").build());
        messageRepository.save(Message.builder().chatRoom(chatRoom).role(MessageRole.USER).content("Msg 3").build());

        // when
        long count = messageRepository.countByUser(user);

        // then
        assertThat(count).isEqualTo(3);
    }
}
