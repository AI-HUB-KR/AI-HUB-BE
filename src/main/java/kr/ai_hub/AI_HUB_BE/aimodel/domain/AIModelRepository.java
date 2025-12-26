package kr.ai_hub.AI_HUB_BE.aimodel.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIModelRepository extends JpaRepository<AIModel, Integer> {

    Optional<AIModel> findByModelName(String modelName);

    List<AIModel> findByIsActive(Boolean isActive);

    List<AIModel> findByIsActiveTrueOrderByCreatedAtDesc();
}
