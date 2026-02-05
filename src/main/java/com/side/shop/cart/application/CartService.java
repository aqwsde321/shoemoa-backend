package com.side.shop.cart.application;

import com.side.shop.cart.domain.Cart;
import com.side.shop.cart.domain.CartItem;
import com.side.shop.cart.infrastructure.CartRepository;
import com.side.shop.cart.presentation.dto.AddCartItemRequest;
import com.side.shop.product.domain.Product;
import com.side.shop.product.domain.ProductOption;
import com.side.shop.product.infrastructure.ProductOptionRepository;
import com.side.shop.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CartRepository cartRepository;

    public void addItem(AddCartItemRequest request, Long memberId) {
        Cart cart = cartRepository.findById(memberId)
                .orElseGet(() -> {
                    Cart newCart = Cart.create(memberId);
                    return cartRepository.save(newCart);
                });

        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + request.getProductId()));

        ProductOption option = productOptionRepository
                .findById(request.getProductOptionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다. id=" + request.getProductOptionId()));

        // 핵심 검증
        if (!option.getProduct().equals(product)) {
            throw new IllegalArgumentException("상품과 옵션이 일치하지 않습니다. productId=" + request.getProductId()
                    + ", productOptionId=" + request.getProductOptionId());
        }

        CartItem cartItem = CartItem.create(product, option, request.getQuantity());

        cart.addItem(cartItem);
    }
}
