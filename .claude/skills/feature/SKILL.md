---
name: feature
description: Automate complete feature development from context loading through design to implementation and testing. Use when user says "새 기능 추가", "기능 개발", "/feature", or provides a feature specification.
---

# Feature Development Skill

You are a Backend Developer for the Dawa Festival Platform.

## Purpose

Automate the complete feature development workflow from context loading through design to implementation and testing.

## Trigger Scenarios

- User requests a new feature
- User says "새 기능 추가", "기능 개발", "기능 구현"
- User invokes `/feature [FeatureName]`
- User provides a feature specification file
- User describes business requirements

## Execution Flow

### Phase 1: Context Loading (Strict Lazy Loading)

**Goal:** Load only what's needed, avoid unnecessary context

**Step 1: Initial Load **

- Read `CLAUDE.md` - Project overview
- Read `core/essential-rules.yaml` - Non-negotiable rules
- Read `memory.md` - Current status and known issues

**Step 2: Identify Target Domain(s)**

Analyze user request to identify:

- **Primary Domain:** The main domain for this feature
- **Secondary Domains:** Related domains (if needed)

**Step 3: Load Required Context (On Demand)**

**MUST READ:**

- `core/system-design.yaml` - Architecture, API, Entity rules
- `references/data-model/domains/{PrimaryDomain}/*.yaml` - Use Glob to find all domain files

**CONDITIONAL READ (only if needed):**

- `references/data-model/domains/{SecondaryDomain}/*.yaml` - ONLY IF cross-domain validation or foreign key reference
  required
- `references/conventions/testing-guide.md` - When writing tests
- `references/conventions/coding-style.md` - When unsure about style

**FORBIDDEN:**

- Do NOT load unrelated domain YAMLs
- Do NOT read entire codebase

### Phase 2: Feature Design

**Check Input:**

**IF** spec file provided:

1. Read spec file
2. Validate completeness (API contract, business logic, validations)
3. **SKIP** design phase
4. **PROCEED** to implementation

**ELSE** (no spec provided):

1. Use template from `.claude/docs/templates/feature-spec.yaml`
2. Create spec based on user requirements
3. Ask user for approval before implementation

**Constraints (CRITICAL):**

1. **Immutable Spec:**
    - Do NOT modify provided spec file without user permission
    - If spec is unclear/incomplete, **ASK** user first
    - If spec conflicts with existing architecture, **ASK** user for guidance

2. **No Over-engineering:**
    - Implement ONLY what's in the spec
    - Do NOT add "nice-to-have" features
    - Do NOT create extra validations not specified
    - Do NOT add fields/methods not in the spec

3. **Follow System Design:**
    - Adhere to layer architecture (Controller → UseCase → Domain Service → Repository)
    - Follow API naming conventions from `core/system-design.yaml`
    - Use Static Factory Method for DTOs (see `application-layer.md`)

### Phase 3: Implementation

**Step 1: Implement Layers (Bottom-Up)**

**Order:** Entity → Repository → Domain Service → UseCase → Controller → DTO

**Entity (if new entity needed):**

- Create entity class in `src/main/java/com/project/bangjjack/domain/{domain}/domain/entity/`
- **MUST** apply `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)` — **NEVER** use `@Setter`
- State changes via static factory method `create()` or domain methods only

```java
@Entity
@Table(name = "{table_name}")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class {Name} extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fieldName;

    public static {Name} create(String fieldName) {
        return new {Name}(fieldName);
    }
}
```

**Repository (if new repository needed):**

- Create repository interface in `src/main/java/com/project/bangjjack/domain/{domain}/domain/repository/`
- Extend JpaRepository or custom interface

```java
public interface {Name}Repository extends JpaRepository<{Name}, Long> {
}
```

**Domain Service:**

- Create in `src/main/java/com/project/bangjjack/domain/{domain}/domain/service/`
- **MUST** apply `@Service`, `@RequiredArgsConstructor`
- Depends ONLY on Repository — **FORBIDDEN:** Domain Service → Domain Service

```java
@Service
@RequiredArgsConstructor
public class {Name}CreateService {

    private final {Name}Repository {name}Repository;

    public {Name} create{Name}({Name} {name}) {
        return {name}Repository.save({name});
    }
}
```

**UseCase:**

- Create in `src/main/java/com/project/bangjjack/domain/{domain}/application/usecase/`
- **Naming: `{Domain}UseCase` — Single Facade per domain (e.g., `PostUseCase`)**
- **MUST** apply `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)` on class
- **MUST** apply `@Transactional` on each write method
- **FORBIDDEN:** Do NOT call other UseCases
- **FORBIDDEN:** Do NOT call Repositories directly (ALWAYS go through Domain Service)

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Name}UseCase {

    private final {Name}CreateService {name}CreateService;
    private final {Name}GetService {name}GetService;

    @Transactional
    public {Name}Response create{Name}(Create{Name}Request request) {
        {Name} {name} = {Name}.create(request.field());
        {Name} saved = {name}CreateService.create{Name}({name});
        return {Name}Response.from(saved);
    }

    public List<{Name}Response> getAll{Name}s() {
        return {name}GetService.getAll{Name}s().stream()
                .map({Name}Response::from)
                .toList();
    }
}
```

**Controller:**

- Create in `src/main/java/com/project/bangjjack/domain/{domain}/presentation/`
- **MUST** apply `@RestController`, `@RequiredArgsConstructor`
- Delegate to UseCase (1:1 mapping) — no business logic

```java
@RestController
@RequestMapping("/api/v1/{resources}")
@RequiredArgsConstructor
public class {Name}Controller {

    private final Create{Name}UseCase create{Name}UseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<{Name}Response> create{Name}(@RequestBody @Valid Create{Name}Request request) {
        {Name}Response response = create{Name}UseCase.execute(request);
        return CommonResponse.success({Name}ResponseCode.{NAME}_CREATED, response);
    }
}
```

**DTO (Request/Response):**

- Request DTO: `src/main/java/com/project/bangjjack/domain/{domain}/application/dto/request/`
- Response DTO: `src/main/java/com/project/bangjjack/domain/{domain}/application/dto/response/`
- ErrorCode: `src/main/java/com/project/bangjjack/domain/{domain}/application/exception/`
- ResponseCode: `src/main/java/com/project/bangjjack/domain/{domain}/presentation/response/`

```java
// Request — record with @Valid annotations
public record Create{Name}Request(
        @NotBlank @Size(max = 100) String field
) {}

// Response — record with static factory method
public record {Name}Response(Long id, String field, LocalDateTime createdAt) {
    public static {Name}Response from({Name} {name}) {
        return new {Name}Response({name}.getId(), {name}.getField(), {name}.getCreatedAt());
    }
}

// ResponseCode — @Getter + @AllArgsConstructor MUST apply
@Getter
@AllArgsConstructor
public enum {Name}ResponseCode implements ResponseCodeInterface {
    {NAME}_CREATED(201, HttpStatus.CREATED, "..."),
    {NAME}_FOUND(200, HttpStatus.OK, "...");

    private final int code;
    private final HttpStatus status;
    private final String message;
}

// ErrorCode — @Getter + @AllArgsConstructor MUST apply
@Getter
@AllArgsConstructor
public enum {Name}ErrorCode implements ErrorCodeInterface {
    {NAME}_NOT_FOUND(40{XX}1, HttpStatus.NOT_FOUND, "...");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
```

**Step 3: Write Tests**

**Test Structure:**

- Use JUnit 5 with @ExtendWith(MockitoExtension.class) and @Nested
- Use Korean @DisplayName (e.g., "부스 메뉴 주문 시 재고가 차감된다")
- Follow AAA pattern (given / when / then)

**Test Coverage:**

- Happy path (successful case)
- Edge cases (boundary values)
- Error cases (validation failures, business rule violations)
- See `references/conventions/testing-guide.md` for details

**Run Tests:**

```bash
# Run specific tests
./gradlew test --tests "*{FeatureName}*"

# Full test suite
./gradlew test
```

**Step 2: Verification**

1. Compile: `./gradlew compileJava`
2. Run tests: `./gradlew test --tests "*{FeatureName}*"`
3. Build: `./gradlew build`
4. Check code style (manual review against `coding-style.md`)

## Validation Checklist

Before completing feature:

- [ ] All layers implemented (Controller, UseCase, Domain Service if needed, Repository if needed)
- [ ] DTOs use Static Factory Method pattern
- [ ] No layer skipping (Controller must call UseCase, not Repository)
- [ ] No horizontal dependencies (Domain Service → Domain Service)
- [ ] Tests written (happy path + edge cases + error cases)
- [ ] All tests pass: `./gradlew test`
- [ ] Build successful: `./gradlew build`
- [ ] Code follows `essential-rules.yaml`
- [ ] Korean test names used

## Report Format

```
✅ Feature Implemented: [FeatureName]

📋 Specification:
  - Primary Domain: [Domain]
  - API Endpoint: [Method] [Path]
  - Business Logic: [Brief description]

📁 Files Created/Modified:
  - src/.../domain/{domain}/presentation/{Name}Controller.java
  - src/.../domain/{domain}/application/{Name}UseCase.java
  - src/.../domain/{domain}/domain/{Name}Service.java (if needed)
  - src/.../domain/{domain}/application/dto/{Name}Request.java, {Name}Response.java
  - src/test/.../domain/{domain}/application/{Name}UseCaseTest.java

🧪 Test Results:
  - Test cases: [Number] (happy path + edge cases + error cases)
  - Status: ✅ PASSED ([X]/[X])

🏗️ Build Status: ✅ SUCCESS

📝 Notes:
  - [Any important decisions or trade-offs made]
```

## Example Invocation

```
User: "/feature 부스 메뉴 주문"

AI:
🚀 Starting Feature Development: 부스 메뉴 주문

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, memory.md...
- Identified Primary Domain: Order
- Identified Secondary Domains: Booth, Menu
- Loading core/system-design.yaml...
- Loading references/data-model/domains/order/*.yaml...
- Loading references/data-model/domains/booth/*.yaml...
- Loading references/data-model/domains/menu/*.yaml...
- Loading references/conventions/layers/application-layer.md...

Phase 2: Feature Design
- No spec file provided
- Creating spec from template...

[Shows draft spec]

Should I proceed with this design?

User: "Yes"

Phase 3: Implementation
- Implementing OrderController.java...
- Implementing OrderUseCase.java...
- Implementing OrderService.java...
- Creating OrderRequest.java, OrderResponse.java (with Static Factory Method)...
- Writing tests: OrderUseCaseTest.java...
  - "유효한 주문 요청 시 주문이 생성된다"
  - "메뉴 재고가 부족한 경우 예외가 발생한다"
  - "부스가 운영 중이 아닌 경우 예외가 발생한다"
- Running tests... ✅ PASSED (3/3)
- Running build... ✅ SUCCESS

✅ Feature Implemented: 부스 메뉴 주문

📋 Specification:
  - Primary Domain: Order
  - API Endpoint: POST /api/v1/orders
  - Business Logic: 메뉴 재고 확인 → 주문 생성 → 재고 차감

📁 Files Created:
  - src/.../domain/order/presentation/OrderController.java
  - src/.../domain/order/application/CreateOrderUseCase.java
  - src/.../domain/order/application/dto/OrderRequest.java, OrderResponse.java
  - src/test/.../domain/order/application/CreateOrderUseCaseTest.java

🧪 Test Results:
  - Test cases: 3 (1 happy + 2 error cases)
  - Status: ✅ PASSED (3/3)

🏗️ Build Status: ✅ SUCCESS

📝 Notes:
  - Static Factory Method used for OrderResponse.from(order)
  - Transaction boundary set in UseCase (@Transactional)
```

## Troubleshooting

**Problem:** Spec file is incomplete or unclear
**Cause:** User provided minimal requirements
**Solution:**

1. List missing information (API contract, validation rules, error cases)
2. Ask user to clarify or provide defaults based on `system-design.yaml`
3. Document assumptions in spec

**Problem:** Cross-domain dependency needed
**Cause:** Feature requires data from multiple domains
**Solution:**

1. Check if relationship already exists in entity YAML
2. If yes, use it
3. If no, ask user if new relationship should be created
4. Prefer UseCase orchestration over Domain Service coupling

**Problem:** Tests fail after implementation
**Cause:** Logic error or missing setup
**Solution:**

1. Read test failure message: `./gradlew test 2>&1 | grep -A 10 FAILED`
2. Check test data setup (are entities properly initialized?)
3. Verify mock behavior (if using mocks)
4. Run single test for debugging: `./gradlew test --tests "*{TestClass}*{TestMethod}*"`

**Problem:** Build fails with compilation error
**Cause:** Syntax error, missing imports, or type mismatch
**Solution:**

1. Read error message: `./gradlew compileKotlin`
2. Fix syntax errors
3. Add missing imports
4. Check type compatibility (especially with DTOs and entities)

## Forbidden Actions

- Modify immutable spec without user permission
- Skip test writing
- Violate layer architecture (Controller → Repository directly)
- Create horizontal dependencies (Domain Service → Domain Service)
- Add features not in spec ("nice-to-have" additions)
- Use English test names (must use Korean)
- Complete with build/test failures

## Success Criteria

- [ ] All layers implemented correctly (following architecture)
- [ ] No layer violations (Controller → UseCase → Domain Service/Repository)
- [ ] Tests written and passing (happy path + edge cases + error cases)
- [ ] Build successful
- [ ] DTOs use Static Factory Method pattern
- [ ] Code follows `essential-rules.yaml` and `coding-style.md`
- [ ] Feature matches spec exactly (no over-engineering)

## Best Practices

1. **Lazy Load Context**
    - Load only domains involved in the feature
    - Don't read entire codebase upfront

2. **Follow Architecture**
    - Respect layer boundaries
    - No shortcuts (Controller → Repository)
    - Keep Domain Services pure (no horizontal dependencies)

3. **Test First (or Alongside)**
    - Write tests as you implement
    - Use quick feedback loop: `./gradlew test --tests "*{Feature}*"`
    - Cover happy path and error cases

4. **Respect the Spec**
    - Implement exactly what's specified
    - Don't add extra features
    - Ask if spec is unclear

5. **Follow Existing Patterns**
    - Use Grep to find similar implementations
    - Follow existing code patterns
    - Maintain consistency with codebase
