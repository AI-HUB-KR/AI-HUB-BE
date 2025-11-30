package kr.ai_hub.AI_HUB_BE.application.chat.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 파일 유효성 검증 서비스
 * Apache Tika를 사용하여 파일의 실제 형식을 검증합니다.
 * - Magic Number 검증: 파일 헤더의 바이너리 패턴으로 실제 형식 확인
 * - MIME Type 검증: Content-Type과 실제 파일 형식 일치 확인
 * - Path Traversal 방지: 위험한 경로 조작 차단
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 지원되는 MIME Type 정의
     * 이미지: JPEG, PNG, WebP
     * 문서는 추후 지원 예정
     */
    private static final Map<String, String> ALLOWED_MIME_TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp")
    // Map.entry("gif", "image/gif"),
    // Map.entry("pdf", "application/pdf")
    );

    /**
     * 파일 유효성 종합 검증
     * 1. 파일 존재 여부 확인
     * 2. 파일 크기 검증
     * 3. 파일명 유효성 검증 (Path Traversal 방지)
     * 4. 확장자 검증
     * 5. MIME Type 검증
     * 6. Tika를 사용한 실제 파일 형식 검증 (Magic Number)
     *
     * @param file 검증할 파일
     * @throws ValidationException 검증 실패 시 예외 발생
     */
    public void validateFile(MultipartFile file) {
        // 1. 파일 존재 여부 확인
        if (file == null || file.isEmpty()) {
            log.warn("파일이 제공되지 않았습니다");
            throw new ValidationException("파일이 제공되지 않았습니다");
        }

        log.debug("파일 검증 시작: fileName={}, size={}",
                file.getOriginalFilename(), file.getSize());

        // 2. 파일 크기 검증
        validateFileSize(file);

        // 3. 파일명 유효성 검증
        String originalFilename = validateFilename(file.getOriginalFilename());

        // 4. 확장자 검증
        String extension = validateExtension(originalFilename);

        // 5. MIME Type 검증
        validateContentType(file.getContentType(), extension);

        // 6. 실제 파일 형식 검증 (Tika Magic Number)
        try {
            validateMagicNumber(file, extension);
            log.info("파일 검증 성공: fileName={}, extension={}", originalFilename, extension);
        } catch (IOException e) {
            log.error("파일 읽기 실패: {}", e.getMessage(), e);
            throw new ValidationException("파일 읽기 중 오류가 발생했습니다: " + e.getMessage());
        } catch (TikaException e) {
            log.error("Tika 설정 오류: {}", e.getMessage(), e);
            throw new ValidationException("파일 형식 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 크기 검증
     * 최대 파일 크기: 10MB
     *
     * @param file 검증할 파일
     * @throws ValidationException 파일 크기 초과 시 예외 발생
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("파일 크기 초과: size={}, maxSize={}", file.getSize(), MAX_FILE_SIZE);
            throw new ValidationException(
                    String.format("파일 크기가 너무 큽니다. 최대 크기: %dMB", MAX_FILE_SIZE / 1024 / 1024));
        }
    }

    /**
     * 파일명 유효성 검증 (Path Traversal 방지)
     * ../를 포함한 위험한 경로 패턴을 차단합니다.
     *
     * @param originalFilename 원본 파일명
     * @return 안전한 파일명
     * @throws ValidationException 유효하지 않은 파일명인 경우 예외 발생
     */
    private String validateFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            log.warn("파일명이 유효하지 않습니다");
            throw new ValidationException("파일명이 유효하지 않습니다");
        }

        // Path Traversal 공격 방지: File.getName()은 경로 구분자만 추출
        String safeName = new File(originalFilename).getName();
        if (!safeName.equals(originalFilename)) {
            log.warn("위험한 파일명 감지: originalName={}, safeName={}",
                    originalFilename, safeName);
            throw new ValidationException("유효하지 않은 파일명입니다");
        }

        return originalFilename;
    }

    /**
     * 파일 확장자 검증 및 추출
     *
     * @param filename 파일명
     * @return 소문자로 변환된 확장자
     * @throws ValidationException 지원하지 않는 확장자인 경우 예외 발생
     */
    private String validateExtension(String filename) {
        String extension = getFileExtension(filename);

        if (extension.isEmpty() || !ALLOWED_MIME_TYPES.containsKey(extension)) {
            log.warn("지원하지 않는 파일 확장자: extension={}, allowed={}",
                    extension, ALLOWED_MIME_TYPES.keySet());
            throw new ValidationException(
                    String.format("지원하지 않는 파일 형식입니다: %s. 지원되는 형식: %s",
                            extension, String.join(", ", ALLOWED_MIME_TYPES.keySet())));
        }

        return extension;
    }

    /**
     * Content-Type 검증
     * 파일의 Content-Type이 확장자와 일치하는지 확인합니다.
     *
     * @param contentType 파일의 Content-Type
     * @param extension   파일 확장자
     * @throws ValidationException Content-Type이 일치하지 않는 경우 예외 발생
     */
    private void validateContentType(String contentType, String extension) {
        if (contentType == null || contentType.isBlank()) {
            log.warn("Content-Type이 제공되지 않았습니다: extension={}", extension);
            throw new ValidationException("파일의 Content-Type 정보가 없습니다");
        }

        String expectedMimeType = ALLOWED_MIME_TYPES.get(extension);
        if (!contentType.equals(expectedMimeType)) {
            log.warn("Content-Type 불일치: contentType={}, expected={}, extension={}",
                    contentType, expectedMimeType, extension);
            throw new ValidationException(
                    String.format("파일 형식이 일치하지 않습니다. 확장자: %s, Content-Type: %s",
                            extension, contentType));
        }
    }

    /**
     * Tika를 사용한 파일 형식 검증 (Magic Number 기반)
     * 실제 파일 헤더의 바이너리 패턴을 분석하여 파일 형식을 검증합니다.
     * 이를 통해 확장자를 위조한 파일을 감지할 수 있습니다.
     *
     * @param file      검증할 파일
     * @param extension 파일 확장자
     * @throws IOException         파일 읽기 실패 시 예외 발생
     * @throws TikaException       Tika 설정 오류 시 예외 발생
     * @throws ValidationException 파일 형식이 일치하지 않는 경우 예외 발생
     */
    private void validateMagicNumber(MultipartFile file, String extension) throws IOException, TikaException {
        TikaConfig tikaConfig = new TikaConfig();
        Metadata metadata = new Metadata();

        // 메타데이터에 파일명 설정 (더 정확한 감지를 위해)
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());

        // 파일을 byte[] 형태로 변환하여 Tika에 전달
        byte[] fileBytes = file.getBytes();
        try (TikaInputStream tikaInputStream = TikaInputStream.get(fileBytes, metadata)) {

            Detector detector = tikaConfig.getDetector();
            MediaType detectedType = detector.detect(tikaInputStream, metadata);

            log.debug("Tika 감지 결과: detectedType={}, extension={}, contentType={}",
                    detectedType, extension, file.getContentType());

            // 감지된 MIME Type이 허용된 목록에 있는지 확인
            String expectedMimeType = ALLOWED_MIME_TYPES.get(extension);
            String detectedMimeType = detectedType.toString();

            if (!expectedMimeType.equals(detectedMimeType)) {
                log.warn("파일 형식 불일치 (Magic Number): expected={}, detected={}, extension={}",
                        expectedMimeType, detectedMimeType, extension);
                throw new ValidationException(
                        String.format("파일 형식이 일치하지 않습니다. 감지된 타입: %s", detectedMimeType));
            }
        }
    }

    /**
     * 파일명에서 확장자 추출
     * 마지막 '.' 이후의 문자열을 소문자로 변환하여 반환합니다.
     *
     * @param filename 파일명
     * @return 소문자로 변환된 확장자 (확장자가 없으면 빈 문자열)
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 지원하는 파일 확장자 목록 반환
     * 클라이언트에 제공할 수 있는 정보
     *
     * @return 지원하는 확장자 Set
     */
    public Set<String> getSupportedExtensions() {
        return ALLOWED_MIME_TYPES.keySet();
    }

    /**
     * 최대 파일 크기 반환 (MB)
     *
     * @return 최대 파일 크기 (MB)
     */
    public long getMaxFileSizeMB() {
        return MAX_FILE_SIZE / 1024 / 1024;
    }
}
