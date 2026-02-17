TESTING.md

1. 테스트 전략 개요

본 프로젝트의 테스트 전략은 다음 원칙을 따른다.

Mock 라이브러리 사용 금지

테스트 목적에 따라 계층별로 테스트를 분리

통합 테스트와 성능 테스트는 같은 API를 호출

테스트 코드는 중복 작성하지 않는다

테스트는 아래 4가지로 구분한다.

테스트 종류 목적 기술
Domain Test 비즈니스 규칙 검증 순수 Java
Repository Test JPA 매핑/쿼리 검증 DataJpaTest
Controller 통합 테스트 유스케이스 검증 SpringBootTest
성능 테스트 부하/동시성 검증 k6 2. Domain Test (순수 자바 단위 테스트)
2.1 목적

비즈니스 규칙이 올바른지 검증

JPA, Spring, DB와 완전히 분리

가장 빠르고 가장 많이 작성되는 테스트

2.2 규칙

@SpringBootTest, @DataJpaTest 사용 ❌

Repository, EntityManager 사용 ❌

new 생성자로 직접 객체 생성

테스트 대상은 Domain(Entity)의 public 메서드

2.3 예시
class OrderTest {

    @Test
    void 주문_금액은_상품가격과_수량의_곱이다() {
        Order order = Order.create(1L, 1000, 3);

        assertThat(order.getTotalAmount()).isEqualTo(3000);
    }

    @Test
    void 수량이_0이하면_주문할_수_없다() {
        assertThatThrownBy(() -> Order.create(1L, 1000, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

}

Domain Test는 JPA Entity를 사용하더라도 “순수 자바 객체”로 취급한다.

3. Repository Test (JPA 검증용 테스트)
   3.1 목적

JPA 매핑 검증

JPQL / QueryDSL 쿼리 결과 검증

fetch join, N+1 문제 확인

Lock, Version 동작 검증

비즈니스 규칙 검증이 목적이 아니다

3.2 사용 어노테이션
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers

3.3 예시
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void 주문과_주문상품을_fetch_join으로_조회한다() {
        Order order = orderRepository.findWithItemsById(1L).get();

        assertThat(order.getItems()).hasSize(2);
    }

}

4. Controller 통합 테스트 (유스케이스 테스트)
   4.1 핵심 원칙

UseCase(Application) 단독 통합 테스트는 만들지 않는다

이유

실제 사용자는 항상 HTTP API를 호출

k6 성능 테스트도 Controller를 호출

UseCase 테스트는 Controller 테스트와 중복

👉 유스케이스 테스트의 책임은 Controller 통합 테스트가 가진다

4.2 목적

요청 → 응답 전체 흐름 검증

트랜잭션 경계 검증

Domain + Repository + Service 조합 검증

실사용 시나리오 검증

4.3 사용 어노테이션
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers

4.4 예시
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
                      "quantity": 2
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").exists());
    }

}

5. 성능 테스트 (k6)
   5.1 원칙

Controller 통합 테스트와 동일한 API

테스트 대상 로직은 동일

실행기만 다르다

구분 실행 환경
Controller 통합 테스트 JUnit
성능 테스트 k6
5.2 코드 중복이 아닌 이유

Java 테스트 코드 ❌

HTTP 요청 시나리오만 정의

비즈니스 로직 재작성 없음

5.3 예시 (k6)
import http from 'k6/http';

export default function () {
http.post('http://localhost:8080/api/orders', JSON.stringify({
memberId: 1,
productId: 10,
quantity: 2
}), {
headers: { 'Content-Type': 'application/json' }
});
}

6. 테스트 작성 순서 (권장)

Domain Test 작성

Repository Test (필요한 경우)

Controller 통합 테스트

k6 성능 테스트

7. 테스트 전략 한 줄 요약

비즈니스는 Domain에서 검증하고,
유스케이스는 Controller에서 검증하며,
성능은 같은 API를 k6로 검증한다.
