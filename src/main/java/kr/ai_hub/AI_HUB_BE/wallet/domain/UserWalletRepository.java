package kr.ai_hub.AI_HUB_BE.wallet.domain;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {

    Optional<UserWallet> findByUser(User user);

    Optional<UserWallet> findByUserUserId(Integer userId);
}
