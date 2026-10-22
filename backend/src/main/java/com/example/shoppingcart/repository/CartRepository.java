package com.example.shoppingcart.repository;

import com.example.shoppingcart.entity.Cart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    /**
     * Locks the user's cart row until the transaction ends. Cart changes and checkout for one user therefore
     * run one at a time, which makes double clicks and parallel tabs safe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.userId = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);

    /** Creates the cart if it does not exist yet. Safe when two requests race (the unique index decides). */
    @Modifying
    @Query(nativeQuery = true, value = "insert into carts (user_id, created_at, updated_at) "
            + "values (:userId, now(), now()) on conflict (user_id) do nothing")
    int insertIfAbsent(@Param("userId") Long userId);
}
