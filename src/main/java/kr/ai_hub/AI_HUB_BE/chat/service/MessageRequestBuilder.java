package kr.ai_hub.AI_HUB_BE.chat.service;

import kr.ai_hub.AI_HUB_BE.chat.dto.ChatHistoryMessage;
import kr.ai_hub.AI_HUB_BE.chat.dto.SendMessageRequest;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModel;
import kr.ai_hub.AI_HUB_BE.chat.domain.ChatRoom;
import kr.ai_hub.AI_HUB_BE.chat.domain.Message;
import kr.ai_hub.AI_HUB_BE.chat.domain.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRequestBuilder {

    private final MessageRepository messageRepository;

    /**
     * AI 서버로 전송할 요청 바디를 구성합니다.
     */
    public Map<String, Object> build(SendMessageRequest request, AIModel aiModel, ChatRoom chatRoom) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", request.message());
        requestBody.put("model", aiModel.getModelName());

        if (request.files() != null && !request.files().isEmpty()) {
            List<Map<String, String>> filesPayload = request.files().stream()
                    .map(file -> Map.of(
                            "id", file.id(),
                            "type", file.type().getValue()
                    ))
                    .toList();
            requestBody.put("files", filesPayload);
        }

        List<ChatHistoryMessage> history = buildChatHistory(chatRoom, requestBody);
        if (!history.isEmpty()) {
            requestBody.put("history", history);
        }

        return requestBody;
    }

    /**
     * 채팅방의 이전 대화 기록을 시간순으로 조회하여 history 배열을 구성하고, conversationId를 설정합니다.
     * 파일 첨부 정보는 제외합니다.
     */
    private List<ChatHistoryMessage> buildChatHistory(ChatRoom chatRoom, Map<String, Object> requestBody) {
        List<Message> previousMessages = messageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);

        if (previousMessages.isEmpty()) {
            log.info("채팅방에 이전 메시지 없음: roomId={}", chatRoom.getRoomId());
            return List.of();
        }

        requestBody.put("conversationId", previousMessages.getLast().getResponseId());

        return previousMessages.stream()
                .map(ChatHistoryMessage::from)
                .toList();
    }
}
