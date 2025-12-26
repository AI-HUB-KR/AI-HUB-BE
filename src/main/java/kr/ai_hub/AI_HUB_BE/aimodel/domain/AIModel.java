package kr.ai_hub.AI_HUB_BE.aimodel.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model", indexes = {
    @Index(name = "idx_ai_model_name", columnList = "model_name"),
    @Index(name = "idx_ai_model_is_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AIModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Integer modelId;

    @Column(name = "model_name", length = 50, nullable = false, unique = true)
    private String modelName;

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    @Column(name = "display_explain", length = 100)
    private String displayExplain;

    @Column(name = "input_price_per_1m", precision = 20, scale = 10, nullable = false)
    private BigDecimal inputPricePer1m;

    @Column(name = "output_price_per_1m", precision = 20, scale = 10, nullable = false)
    private BigDecimal outputPricePer1m;
    /**
     * 모델 마크업 비율 (예: 0.2는 20% 마크업)
     */
    @Builder.Default
    @Column(name = "model_markup_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal modelMarkupRate = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String displayName, String displayExplain,
                       BigDecimal inputPricePer1m, BigDecimal outputPricePer1m,
                       BigDecimal modelMarkupRate,Boolean isActive) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (displayExplain != null) {
            this.displayExplain = displayExplain;
        }
        if (inputPricePer1m != null) {
            this.inputPricePer1m = inputPricePer1m;
        }
        if (outputPricePer1m != null) {
            this.outputPricePer1m = outputPricePer1m;
        }
        if (modelMarkupRate != null) {
            this.modelMarkupRate = modelMarkupRate;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    /**
     * 입력 토큰 1백만 단위당 가격에 markup 적용 가격을 반환합니다.
     * @return BigDecimal 입력 토큰 1백만 단위당 가격에 markup 적용 가격
     */
    public BigDecimal getInputPricePer1m() {
        BigDecimal markupRate = this.modelMarkupRate != null ? this.modelMarkupRate : BigDecimal.ZERO;
        BigDecimal markupMultiplier = BigDecimal.ONE.add(markupRate);
        return this.inputPricePer1m.multiply(markupMultiplier);
    }

    /**
     * 출력 토큰 1백만 단위당 가격에 markup 적용 가격을 반환합니다.
     * @return BigDecimal 출력 토큰 1백만 단위당 가격에 markup 적용 가격
     */
    public BigDecimal getOutputPricePer1m() {
        BigDecimal markupRate = this.modelMarkupRate != null ? this.modelMarkupRate : BigDecimal.ZERO;
        BigDecimal markupMultiplier = BigDecimal.ONE.add(markupRate);
        return this.outputPricePer1m.multiply(markupMultiplier);
    }

    public void deactivate() {
        this.isActive = false;
    }
}
