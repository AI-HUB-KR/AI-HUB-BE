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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .isActivated(true)
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("사용자로 채팅방 목록 조회")
    void findByUser() {
        // given
        ChatRoom chatRoom1 = ChatRoom.builder()
                .user(user)
                .title("Chat Room 1")
                .build();
        ChatRoom chatRoom2 = ChatRoom.builder()
                .user(user)
                .title("Chat Room 2")
                .build();
        chatRoomRepository.saveAll(List.of(chatRoom1, chatRoom2));

        // when
        List<ChatRoom> result = chatRoomRepository.findByUser(user);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("title")
                .containsExactlyInAnyOrder("Chat Room 1", "Chat Room 2");
    }

    @Test
    @DisplayName("사용자로 채팅방 목록 조회 - 생성일 내림차순")
    void findByUserOrderByCreatedAtDesc() throws InterruptedException {
        // given
        ChatRoom chatRoom1 = ChatRoom.builder()
                .user(user)
                .title("First Room")
                .build();
        chatRoomRepository.save(chatRoom1);

        // Ensure different timestamps
        Thread.sleep(10);

        ChatRoom chatRoom2 = ChatRoom.builder()
                .user(user)
                .title("Second Room")
                .build();
        chatRoomRepository.save(chatRoom2);

        // when
        List<ChatRoom> result = chatRoomRepository.findByUserOrderByCreatedAtDesc(user);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Second Room");
        assertThat(result.get(1).getTitle()).isEqualTo("First Room");
    }

    @Test
    @DisplayName("사용자로 채팅방 페이징 조회")
    void findByUserWithPagination() {
        // given
        for (int i = 0; i < 15; i++) {
            chatRoomRepository.save(ChatRoom.builder()
                    .user(user)
                    .title("Room " + i)
                    .build());
        }

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<ChatRoom> page = chatRoomRepository.findByUser(user, pageRequest);

        // then
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("사용자의 채팅방 개수 조회")
    void countByUser() {
        // given
        chatRoomRepository.save(ChatRoom.builder().user(user).title("Room 1").build());
        chatRoomRepository.save(ChatRoom.builder().user(user).title("Room 2").build());
        chatRoomRepository.save(ChatRoom.builder().user(user).title("Room 3").build());

        // when
        long count = chatRoomRepository.countByUser(user);

        // then
        assertThat(count).isEqualTo(3);
    }
}
