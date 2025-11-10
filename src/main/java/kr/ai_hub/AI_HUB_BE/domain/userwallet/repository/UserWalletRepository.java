package kr.ai_hub.AI_HUB_BE.domain.userwallet.repository;

import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {

    Optional<UserWallet> findByUser(User user);

    Optional<UserWallet> findByUserUserId(Integer userId);
}
