package com.side.shop.cart.domain;

import static jakarta.persistence.CascadeType.ALL;
import static lombok.AccessLevel.PROTECTED;

import com.side.shop.member.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.access.AccessDeniedException;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Cart {

    @Id
    @GeneratedValue
    private Long id;

    private Long memberId;

    @OneToMany(mappedBy = "cart", cascade = ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public static Cart create(Long memberId) {
        Cart cart = new Cart();
        cart.memberId = memberId;
        return cart;
    }

    public void addItem(CartItem item) {
        for (CartItem existing : items) {
            if (existing.isSameOption(item)) {
                existing.increase(item.getQuantity());
                return;
            }
        }
        items.add(item);
        item.assignCart(this);
    }

    public void changeItemQuantity(Long cartItemId, int quantity, Member loginMember) {
        CartItem item = findItemById(cartItemId);

        // 소유자 검증
//        if (!item.getCart().getMemberId().equals(loginMember.getId())) {
//            throw new AccessDeniedException("접근 권한이 없습니다.");
//
//        }

        if (quantity <= 0) {
            removeItem(item);
            return;
        }

        item.changeQuantity(quantity);
    }

    private CartItem findItemById(Long cartItemId) {
        return items.stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("장바구니에 없는 상품입니다."));
    }

    private void removeItem(CartItem item) {
        items.remove(item);
        item.assignCart(null);
    }
}
