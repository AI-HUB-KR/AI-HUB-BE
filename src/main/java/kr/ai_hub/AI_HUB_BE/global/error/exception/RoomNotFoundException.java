package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class RoomNotFoundException extends BaseException {

    public RoomNotFoundException() {
        super(ErrorCode.ROOM_NOT_FOUND);
    }

    public RoomNotFoundException(String message) {
        super(ErrorCode.ROOM_NOT_FOUND, message);
    }
}
