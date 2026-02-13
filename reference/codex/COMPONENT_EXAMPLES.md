# COMPONENT_EXAMPLES

Purpose: 새 기능 추가 시 바로 복사/응용 가능한 최소 컴포넌트 예시를 제공한다.

## 1. Controller 예시
```java
@RestController
@RequestMapping("/api/example")
@RequiredArgsConstructor
public class ExampleController {
    private final ExampleService exampleService;

    @PostMapping
    public ResponseEntity<ExampleResponse> create(@RequestBody ExampleRequest request) {
        Long id = exampleService.create(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ExampleResponse(id));
    }
}
```

## 2. Service 예시
```java
@Service
@RequiredArgsConstructor
public class ExampleService {
    private final ExampleRepository exampleRepository;

    @Transactional
    public Long create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 비어있을 수 없습니다.");
        }
        Example saved = exampleRepository.save(new Example(name));
        return saved.getId();
    }
}
```

## 3. Repository 예시
```java
public interface ExampleRepository extends JpaRepository<Example, Long> {
    boolean existsByName(String name);
}
```

## 4. Integration Test 예시
```java
@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExampleIntegrationTest {
}
```
