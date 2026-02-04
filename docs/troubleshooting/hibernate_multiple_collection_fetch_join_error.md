# @EntityGraph로 두 개의 컬렉션을 한 번에 조회시 에러

## 문제 상황

상품 상세 조회 API를 구현하던 중,  
`Product` 엔티티에서 두 개의 컬렉션(`options`, `images`)을 한 번에 조회하기 위해  
Spring Data JPA의 `@EntityGraph`를 사용하였다.

```java
@EntityGraph(attributePaths = {"options", "images"})
Optional<Product> findDetailById(Long id);
```
엔티티 매핑은 다음과 같았다.

```java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("sortOrder ASC")
private List<ProductImage> images = new ArrayList<>();

@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("productSize ASC")
private List<ProductOption> options = new ArrayList<>();
```
상품 상세 조회 API 호출 시 다음과 같은 Hibernate 예외가 발생하며 정상 동작하지 않았다.
```log
org.springframework.dao.InvalidDataAccessApiUsageException: org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags
```
컬렉션 중 하나를 `Set`으로 변경하면 에러가 사라지는 현상이 관찰되었다.

## 원인

Hibernate는 하나의 쿼리에서 둘 이상의 `List`(Bag) 컬렉션을 `fetch join`하는 것을 허용하지 않는다.

그 이유는 다음과 같다.

- `List`는 **순서(index)가 중요한 컬렉션**이다.
- `fetch join` 결과는 SQL 상에서 **카테시안 곱 형태의 ResultSet**으로 반환된다.
- Hibernate는 한 row를 기준으로
    - `options`가 몇 번째 요소인지
    - `images`가 몇 번째 요소인지  
      를 **동시에 판단할 수 없다**.

즉, 두 개의 `List` 컬렉션 모두에 대해 **index를 안정적으로 재구성할 수 없기 때문에**  
Hibernate는 예외를 발생시킨다.

반면,

- `Set`은 **순서 개념이 없고**
- **중복 제거만 수행**하면 되기 때문에

`Set + List` 조합은 허용된다.

## 해결 방법
### **1. 컬렉션 fetch join을 한 쿼리에서 처리하지 않도록 설계 변경**

상품 상세 조회 시 다음과 같이 쿼리를 분리하였다.

Product + options : `EntityGraph` 사용

images : 별도의 Repository에서 조회
```java
Product product = productRepository
        .findDetailById(productId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrder(productId);
```

### **2. 컬렉션 타입을 `Set`으로 변경**

Hibernate의 `MultipleBagFetchException`을 피하기 위한 또 다른 방법은  
하나 이상의 `List`(Bag) 컬렉션을 `Set`으로 변경하는 것이다.

`Set`은 순서(index) 개념이 없기 때문에
`Set` + `List` 조합으로 `fetch join`을 수행하면 Hibernate의 `MultipleBagFetchException`은 발생하지 않는다.

```java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("sortOrder ASC")
private Set<ProductImage> images = new HashSet<>();

@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("productSize ASC")
private List<ProductOption> options = new ArrayList<>();
```

그러나 이 방법에는 다음과 같은 문제가 있다.

### 1. List 컬렉션의 중복 문제는 해결되지 않는다

fetch join 결과는 SQL 상에서 **카테시안 곱 형태의 ResultSet**으로 반환된다.  
`Set`은 중복 제거가 가능하지만, `List`는 중복 row를 그대로 수용한다.

따라서 `Set + List` 구조에서는  
`List` 컬렉션(`options`)에 **중복 데이터가 포함될 수 있다.**

---

### 2. Set + Set으로 변경하면 도메인 의미가 훼손된다

두 컬렉션을 모두 `Set`으로 변경하면  
기술적으로는 fetch join 에러와 중복 문제를 모두 피할 수 있다.

하지만 상품 이미지와 옵션은 다음과 같은 **명확한 순서 의미**를 가진다.

- **이미지**
    - 썸네일
    - 노출 순서

- **옵션**
    - 사이즈 순서

`Set`은 순서를 보장하지 않기 때문에  
컬렉션 타입 변경은 **도메인 모델의 의미를 왜곡**하게 된다.

# 정리

Hibernate는 두 개 이상의 `List` 컬렉션 fetch join을 지원하지 않는다.

따라서 상세 조회에서 여러 컬렉션이 필요한 경우  
컬렉션 타입 변경(`Set` 사용) 대신  
조회 쿼리를 분리하고, 서비스 계층에서 DTO를 조립하는 방식을 선택했다.






