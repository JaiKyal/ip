package com.example.shoppingcart.repository;

import com.example.shoppingcart.entity.CartItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select new com.example.shoppingcart.repository.CartLine(i.product.id, i.quantity) "
            + "from CartItem i where i.cart.id = :cartId order by i.product.id")
    List<CartLine> findLines(@Param("cartId") Long cartId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem i where i.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);
}
