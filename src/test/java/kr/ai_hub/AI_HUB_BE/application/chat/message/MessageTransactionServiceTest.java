package kr.ai_hub.AI_HUB_BE.application.chat.message;

import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiUsage;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SendMessageRequest;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.Message;
import kr.ai_hub.AI_HUB_BE.domain.chat.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.MessageRole;
import kr.ai_hub.AI_HUB_BE.domain.payment.CoinTransaction;
import kr.ai_hub.AI_HUB_BE.domain.payment.CoinTransactionRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageTransactionServiceTest {

    @InjectMocks
    private MessageTransactionService messageTransactionService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Test
    @DisplayName("User 메시지 저장")
    void saveUserMessage() {
        // given
        ChatRoom chatRoom = ChatRoom.builder().roomId(UUID.randomUUID()).build();
        AIModel aiModel = AIModel.builder().modelId(1).build();
        SendMessageRequest request = SendMessageRequest.builder()
                .message("Hello")
                .modelId(1)
                .build();

        Message savedMessage = Message.builder()
                .messageId(UUID.randomUUID())
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content("Hello")
                .build();

        given(messageRepository.save(any(Message.class))).willReturn(savedMessage);

        // when
        Message result = messageTransactionService.saveUserMessage(chatRoom, aiModel, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Hello");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("응답 처리 및 코인 차감")
    void processCompletedResponse() {
        // given
        User user = User.builder().userId(1).build();
        ChatRoom chatRoom = ChatRoom.builder().roomId(UUID.randomUUID()).user(user).build();
        AIModel aiModel = AIModel.builder()
                .modelId(1)
                .modelName("GPT-4")
                .inputPricePer1m(BigDecimal.valueOf(100))
                .outputPricePer1m(BigDecimal.valueOf(200))
                .build();
        Message userMessage = Message.builder()
                .messageId(UUID.randomUUID())
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .build();

        UserWallet wallet = UserWallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(1000))
                .build();

        AiUsage usage = new AiUsage(1000, 500, 1500); // input 1000, output 500
        // Cost:
        // Input: 1000 / 1M * 100 = 0.1
        // Output: 500 / 1M * 200 = 0.1
        // Total: 0.2

        given(userWalletRepository.findByUserUserId(user.getUserId())).willReturn(Optional.of(wallet));
        given(chatRoomRepository.findById(chatRoom.getRoomId())).willReturn(Optional.of(chatRoom));

        // when
        messageTransactionService.processCompletedResponse(
                chatRoom, aiModel, user, userMessage, "resp-1", "Answer", usage);

        // then
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(999.8));
        verify(messageRepository).save(argThat(msg -> msg.getRole() == MessageRole.ASSISTANT)); // Assistant message
        verify(messageRepository).save(userMessage); // Update user message
        verify(coinTransactionRepository).save(any(CoinTransaction.class));
    }

    @Test
    @DisplayName("User 메시지 삭제")
    void deleteUserMessage() {
        // given
        Message message = Message.builder().messageId(UUID.randomUUID()).build();
        given(messageRepository.findById(message.getMessageId())).willReturn(Optional.of(message));

        // when
        messageTransactionService.deleteUserMessage(message);

        // then
        verify(messageRepository).delete(message);
    }
}
