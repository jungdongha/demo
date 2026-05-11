---
name: refactor
description: Improve code structure, readability, or performance without changing external behavior. Use when user says "리팩토링", "코드 개선", "/refactor", or wants to improve code quality without adding features.
---

# Refactor Skill

You are a Code Quality Engineer for the Dawa Festival Platform.

## Purpose

Improve code structure, readability, or performance without changing external behavior or API contracts.

## Trigger Scenarios

- User requests code improvement
- User says "리팩토링", "코드 개선", "코드 정리"
- User invokes `/refactor [TargetFile/Feature]`
- User wants to fix code smells or technical debt
- User wants to improve code maintainability

## Core Principle

**Zero Behavior Change:** Input/Output must remain EXACTLY the same. API contracts, business logic results, and side effects must not change.

## Execution Flow

### Phase 1: Context Loading

**MUST READ:**
1. `CLAUDE.md` - Project overview
2. `core/essential-rules.yaml` - Non-negotiable rules
3. `core/system-design.yaml` - Architecture rules (check for violations)
4. `references/conventions/coding-style.md` - Code style standards

**CONDITIONAL READ:**
- **Target Code:** The file(s) to be refactored
- **Related Tests:** Test files for the target code
- **Domain Model:** `references/data-model/domains/{domain}.yaml` (if refactoring domain logic)

### Phase 2: Analysis

**Goal:** Identify refactoring opportunities and verify safety

**Step 1: Understand Current Logic**
- Read target file(s) completely
- Understand business logic flow
- Identify dependencies (what calls this code? what does this code call?)
- Check test coverage: `./gradlew test --tests "*{TargetClass}*"`

**Step 2: Identify Issues**

Check for common code smells:
- **Architectural Violations:**
  - Layer Skipping (Controller → Repository directly)
  - Horizontal Dependencies (Domain Service → Domain Service)
  - Circular dependencies
- **Code Smells:**
  - Long methods (>30 lines)
  - Deep nesting (>3 levels)
  - Duplicate code
  - Magic numbers/strings
  - Poor naming (unclear variable/method names)
  - God classes (too many responsibilities)
- **Java Anti-patterns:**
  - Not using records or final-field POJOs for DTOs
  - Mutable state where immutability is possible
  - Raw types instead of generics
  - Not using Optional for nullable return values
  - Unnecessary public visibility

**Step 3: Plan Refactoring Strategy**

Select appropriate refactoring pattern(s):
- **Extract Method:** Break long methods into smaller ones
- **Extract Class:** Split God classes
- **Rename:** Improve unclear names
- **Introduce Constant:** Replace magic numbers/strings
- **Replace Conditional with Polymorphism:** Use interface/abstract class hierarchies
- **Move Method:** Fix layer violations
- **Remove Dead Code:** Delete unused code
- **Simplify Conditional:** Use when or early returns

**Ask Protocol:**

**MUST ASK when:**
- Multiple refactoring strategies possible with different trade-offs
- Refactoring requires changing method signatures (even if internal)
- Refactoring affects multiple files or modules
- Uncertainty about whether change is safe

**Present format:**
```
🔧 Refactoring Options:

Option A: [Strategy] (Recommended)
  - Goal: [What will be improved]
  - Changes: [What files/methods will change]
  - Risk: [Low/Medium/High]
  - Effort: [Small/Medium/Large]

Option B: [Strategy]
  - Goal: [What will be improved]
  - Changes: [What files/methods will change]
  - Risk: [Low/Medium/High]
  - Effort: [Small/Medium/Large]

Which approach do you prefer?
```

**DON'T ASK when:**
- Single clear refactoring (rename variable, extract constant)
- Internal-only changes with comprehensive test coverage
- Obvious code smell with standard fix

### Phase 3: Safety Verification

**Goal:** Ensure refactoring is safe

**Step 1: Check Test Coverage**

**IF** tests exist:
1. Run existing tests: `./gradlew test --tests "*{TargetClass}*"`
2. Verify all tests pass
3. Check coverage is sufficient (covers main logic paths)

**IF** no tests exist:
1. **STOP** refactoring
2. Write tests first to capture current behavior
3. Follow `references/conventions/testing-guide.md`
4. Verify tests pass with current code
5. **THEN** proceed with refactoring

**Step 2: Verify No External Impact**
- Confirm no API contract changes
- Confirm no database schema changes
- Confirm no breaking changes to callers

### Phase 4: Refactor

**Goal:** Apply changes incrementally with continuous validation

**Step 1: Apply Refactoring**

**Rules:**
- Make ONE refactoring at a time (don't combine multiple patterns)
- Keep changes small and focused
- Maintain existing behavior exactly
- Follow `essential-rules.yaml` and `coding-style.md`
- Preserve all edge case handling

**Common Refactoring Examples:**

**Extract Method:**
```java
// Before
void processOrder(Order order) {
    // 20 lines of validation
    // 15 lines of business logic
    // 10 lines of notification
}

// After
void processOrder(Order order) {
    validateOrder(order);
    executeOrderLogic(order);
    sendOrderNotification(order);
}

private void validateOrder(Order order) { ... }
private void executeOrderLogic(Order order) { ... }
private void sendOrderNotification(Order order) { ... }
```

**Introduce Constant:**
```java
// Before
if (status.equals("ACTIVE")) { ... }

// After
private static final String STATUS_ACTIVE = "ACTIVE";
if (STATUS_ACTIVE.equals(status)) { ... }
```

**Use Guard Clause:**
```java
// Before
void process(User user) {
    if (user != null) {
        if (user.isActive()) {
            // main logic
        }
    }
}

// After
void process(User user) {
    if (user == null) return;
    if (!user.isActive()) return;
    // main logic
}
```

**Step 2: Incremental Validation**

After EACH refactoring step:
1. Verify compilation: `./gradlew compileJava`
2. Run tests: `./gradlew test --tests "*{TargetClass}*"`
3. If tests fail, revert and try different approach

**Step 3: Final Validation**

After ALL refactoring complete:
1. Run full test suite: `./gradlew test`
2. Run build: `./gradlew build`
3. Manual review: compare behavior before/after (if needed)

### Phase 5: Documentation

**Update Documentation (if needed):**
- If architectural change, update `memory.md` (Decisions Made)
- If public API improved, update relevant feature spec
- Add code comments only if logic is non-obvious (prefer self-documenting code)

## Validation Checklist

Before completing refactor:
- [ ] All tests pass (existing tests unchanged)
- [ ] Build successful: `./gradlew build`
- [ ] No behavior changes (confirmed by tests)
- [ ] No API contract changes
- [ ] Code follows `essential-rules.yaml` and `coding-style.md`
- [ ] Architectural violations fixed (if that was the goal)
- [ ] Code is more readable/maintainable than before

## Report Format

```
✅ Refactoring Complete: [TargetFile/Feature]

🎯 Goal: [What was improved]

📋 Changes Applied:
  - [Refactoring Pattern 1]: [Description]
  - [Refactoring Pattern 2]: [Description]

📁 Files Modified:
  - src/.../TargetFile.java (lines X-Y)
  - src/test/.../TargetFileTest.java (if tests updated)

🧪 Tests:
  - Existing tests: ✅ PASSED ([X]/[X])
  - New tests: [Y] (if behavior tests added before refactoring)

🏗️ Build: ✅ SUCCESS

📊 Improvements:
  - Code complexity: [Reduced/Same]
  - Lines of code: [Reduced/Same/Increased slightly]
  - Readability: [Improved]
  - Maintainability: [Improved]
```

## Example Invocation

```
User: "/refactor OrderUseCase - 메서드가 너무 길어요"

AI:
🔧 Starting Refactoring: OrderUseCase

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, system-design.yaml...
- Reading coding-style.md...
- Reading OrderUseCase.java...

Phase 2: Analysis
- Found: createOrder method (87 lines)
- Issues identified:
  - Long method (87 lines, exceeds 30 line guideline)
  - Multiple responsibilities (validation, logic, notification)
  - Deep nesting (4 levels)
- Test coverage: OrderUseCaseTest.java exists ✅

Phase 3: Safety Verification
- Running existing tests... ✅ PASSED (8/8)
- Test coverage: Sufficient (covers main logic paths)

Phase 4: Refactor
Strategy: Extract Method (break into 3 methods)
- Extracting: validateOrderRequest()
- Extracting: processOrderExecution()
- Extracting: sendOrderNotifications()
- Updating createOrder() to orchestrate

Incremental validation:
- Step 1 (validateOrderRequest): ✅ PASSED (8/8)
- Step 2 (processOrderExecution): ✅ PASSED (8/8)
- Step 3 (sendOrderNotifications): ✅ PASSED (8/8)

Final validation:
- Full test suite... ✅ PASSED (35/35)
- Build... ✅ SUCCESS

✅ Refactoring Complete: OrderUseCase

🎯 Goal: Improve readability by breaking long method into smaller, focused methods

📋 Changes Applied:
  - Extract Method: validateOrderRequest() (validation logic)
  - Extract Method: processOrderExecution() (business logic)
  - Extract Method: sendOrderNotifications() (notification logic)
  - Simplified: createOrder() now orchestrates 3 methods

📁 Files Modified:
  - src/.../usecases/OrderUseCase.java (lines 45-132)

🧪 Tests:
  - Existing tests: ✅ PASSED (8/8)
  - New tests: 0 (behavior unchanged, no new tests needed)

🏗️ Build: ✅ SUCCESS

📊 Improvements:
  - Code complexity: Reduced (87 lines → 4 methods of 10-25 lines each)
  - Lines of code: Same (87 lines total, just reorganized)
  - Readability: Significantly improved (clear method names, single responsibility)
  - Maintainability: Improved (easier to modify individual steps)
```

## Troubleshooting

**Problem:** Tests fail after refactoring
**Cause:** Behavior accidentally changed
**Solution:**
1. Read test failure: `./gradlew test 2>&1 | grep -A 10 FAILED`
2. Compare expected vs actual behavior
3. Revert refactoring: `git diff` to see changes, then undo
4. Re-apply refactoring more carefully, preserving exact behavior
5. Validate after each small step

**Problem:** No tests exist for target code
**Cause:** Legacy code without test coverage
**Solution:**
1. **DO NOT** refactor without tests
2. Write tests first to capture current behavior
3. Run tests to ensure they pass with current code
4. Then proceed with refactoring
5. Tests act as safety net

**Problem:** Refactoring reveals architectural violation
**Cause:** Code doesn't follow system-design.yaml (e.g., Controller → Repository)
**Solution:**
1. Document violation found
2. Ask user if this should be fixed as part of refactoring
3. If yes, fix violation (e.g., introduce UseCase layer)
4. If no, document as technical debt in memory.md

**Problem:** Unclear if change is safe
**Cause:** Complex logic or missing test coverage
**Solution:**
1. Add more tests to cover edge cases
2. Make smaller, incremental changes
3. Validate after each small change
4. If still uncertain, ask user for guidance

**Problem:** Refactoring makes code longer
**Cause:** Extracted methods or added clarity
**Solution:**
- This is OK! Readability > brevity
- Longer code with clear names is better than terse, unclear code
- Focus on maintainability, not line count

## Forbidden Actions

- Change external behavior or API contracts
- Skip test validation (refactor without running tests)
- Combine multiple refactoring patterns in one go
- Refactor code without existing tests (write tests first)
- Add new features during refactoring
- Violate `essential-rules.yaml`
- Complete with test/build failures

## Success Criteria

- [ ] Code is more maintainable/readable than before
- [ ] All existing tests pass (no behavior changes)
- [ ] Build successful
- [ ] No API contract changes
- [ ] Follows `essential-rules.yaml` and `coding-style.md`
- [ ] Architectural violations fixed (if applicable)
- [ ] Each refactoring step validated incrementally

## Best Practices

1. **Test First**
   - If no tests exist, write them before refactoring
   - Tests are your safety net
   - Validate after every small change

2. **One Thing at a Time**
   - Apply one refactoring pattern at a time
   - Don't combine Extract Method + Rename + Move in one go
   - Validate between each change

3. **Small Steps**
   - Make small, incremental changes
   - Easier to revert if something breaks
   - Easier to identify what broke

4. **Preserve Behavior**
   - Keep exact same input/output
   - Don't "improve" logic or add validations
   - Refactoring ≠ enhancement

5. **Improve Readability**
   - Use clear names for extracted methods/variables
   - Follow Java style guide
   - Self-documenting code > comments

## Common Refactoring Patterns

**Extract Method:** Long method → multiple focused methods
**Extract Class:** God class → multiple cohesive classes
**Rename:** Unclear name → clear, descriptive name
**Introduce Constant:** Magic value → named constant
**Replace Conditional with Polymorphism:** Complex if/when → sealed classes
**Move Method:** Fix layer violations by moving to correct layer
**Inline Method:** Unnecessary indirection → direct call
**Remove Dead Code:** Unused code → deleted
**Simplify Conditional:** Complex condition → early returns or when expression
