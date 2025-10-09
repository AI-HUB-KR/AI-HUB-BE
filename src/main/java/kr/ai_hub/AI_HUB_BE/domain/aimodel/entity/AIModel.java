package kr.ai_hub.AI_HUB_BE.domain.aimodel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model")
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

    @Column(name = "input_price_per_1k", precision = 20, scale = 10, nullable = false)
    private BigDecimal inputPricePer1k;

    @Column(name = "output_price_per_1k", precision = 20, scale = 10, nullable = false)
    private BigDecimal outputPricePer1k;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
