package kr.ai_hub.AI_HUB_BE.application.chat.message;

import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidationServiceTest {

    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService();
    }

    @Test
    @DisplayName("유효한 이미지 파일 검증 성공")
    void validateFile_Success() {
        // given
        // JPEG magic number: FF D8 FF E0
        byte[] jpegContent = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                jpegContent);

        // when & then
        assertThatCode(() -> fileValidationService.validateFile(file))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("파일이 없는 경우 실패")
    void validateFile_NullOrEmpty() {
        // null
        assertThatThrownBy(() -> fileValidationService.validateFile(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("파일이 제공되지 않았습니다");

        // empty
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]);
        assertThatThrownBy(() -> fileValidationService.validateFile(emptyFile))
                .isInstanceOf(ValidationException.class)
                .hasMessage("파일이 제공되지 않았습니다");
    }

    @Test
    @DisplayName("지원하지 않는 확장자 실패")
    void validateFile_InvalidExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content".getBytes());

        assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("지원하지 않는 파일 형식입니다");
    }

    @Test
    @DisplayName("Content-Type 불일치 실패")
    void validateFile_ContentTypeMismatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_PNG_VALUE, // Mismatch
                "content".getBytes());

        assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("파일 형식이 일치하지 않습니다");
    }

    @Test
    @DisplayName("Magic Number 불일치 실패 (확장자 위조)")
    void validateFile_MagicNumberMismatch() {
        // PNG content but JPG extension
        // PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngContent = new byte[] {
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                pngContent);

        assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("파일 형식이 일치하지 않습니다");
    }
}
