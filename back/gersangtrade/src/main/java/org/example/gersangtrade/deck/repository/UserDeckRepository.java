package org.example.gersangtrade.deck.repository;

import org.example.gersangtrade.domain.deck.UserDeck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeckRepository extends JpaRepository<UserDeck, Long> {

    /** 유저의 전체 덱 조회 */
    List<UserDeck> findByUserId(Long userId);

    /** 유저의 활성 덱 조회 */
    Optional<UserDeck> findByUserIdAndActiveTrue(Long userId);

    /** 본인 덱 존재 여부 */
    boolean existsByIdAndUser_Id(Long id, Long userId);
}
