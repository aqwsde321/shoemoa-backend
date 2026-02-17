# sample code

1️⃣ Domain (Order Aggregate)
1.1 Order 엔티티
package com.shop.domain.order.model;

import jakarta.persistence.\*;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private Long productId;
    private int quantity;
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected Order() {}

    private Order(Long memberId, Long productId, int quantity, int price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = quantity * price;
        this.status = OrderStatus.CREATED;
    }

    public static Order create(Long memberId, Long productId, int quantity, int price) {
        return new Order(memberId, productId, quantity, price);
    }

    public void cancel() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("취소할 수 없는 상태입니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // getter만 노출
    public Long getId() { return id; }
    public int getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }

}

1.2 OrderStatus
package com.shop.domain.order.model;

public enum OrderStatus {
CREATED,
CANCELLED
}

1.3 OrderRepository
package com.shop.domain.order.repository;

import com.shop.domain.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}

2️⃣ Application Service (UseCase)

Service는 조합만 한다.
비즈니스 판단 ❌

package com.shop.application.order;

import com.shop.domain.order.model.Order;
import com.shop.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Long createOrder(Long memberId, Long productId, int quantity, int price) {
        Order order = Order.create(memberId, productId, quantity, price);
        orderRepository.save(order);
        return order.getId();
    }

}

✔ Service는

다른 Aggregate Repository 호출 ❌

Domain 로직 직접 처리 ❌

3️⃣ Controller (Presentation)
3.1 Request DTO
package com.shop.presentation.order.request;

public record OrderCreateRequest(
Long memberId,
Long productId,
int quantity,
int price
) {}

3.2 Response DTO
package com.shop.presentation.order.response;

public record OrderCreateResponse(
Long orderId
) {}

3.3 OrderController
package com.shop.presentation.order;

import com.shop.application.order.OrderService;
import com.shop.presentation.order.request.OrderCreateRequest;
import com.shop.presentation.order.response.OrderCreateResponse;
import org.springframework.web.bind.annotation.\*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderCreateResponse create(@RequestBody OrderCreateRequest request) {
        Long orderId = orderService.createOrder(
            request.memberId(),
            request.productId(),
            request.quantity(),
            request.price()
        );
        return new OrderCreateResponse(orderId);
    }

}

4️⃣ Domain Test (순수 자바)
package com.shop.domain.order;

import com.shop.domain.order.model.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.\*;

class OrderTest {

    @Test
    void 주문_금액은_수량과_가격의_곱이다() {
        Order order = Order.create(1L, 10L, 3, 1000);

        assertThat(order.getTotalAmount()).isEqualTo(3000);
    }

    @Test
    void 수량이_0이하면_주문할_수_없다() {
        assertThatThrownBy(() ->
            Order.create(1L, 10L, 0, 1000)
        ).isInstanceOf(IllegalArgumentException.class);
    }

}

✔ Spring ❌
✔ JPA ❌
✔ Mock ❌

5️⃣ Controller 통합 테스트 (유스케이스 테스트)
package com.shop.presentation.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.\*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 주문을_생성한다() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 1,
                      "productId": 10,
                      "quantity": 2,
                      "price": 1000
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").exists());
    }

}

✔ 이 테스트가 UseCase 테스트 역할을 겸함
✔ k6는 같은 API를 호출

6️⃣ 이 예제가 “정답 구조”인 이유
항목 충족 여부
DDD 사고 ⭕
JPA 실무 친화 ⭕
Domain 응집도 ⭕
Service 비대화 방지 ⭕
Mock 없는 테스트 ⭕
k6 연계 가능 ⭕
팀 교육 난이도 ⭕
한 줄 요약

“Order 하나만 봐도 이 프로젝트 구조가 설명되면 성공이다.”
