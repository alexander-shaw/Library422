# HW2 — Unit Testing, Coverage, and Refactoring

*Consolidated conversation, presented in chronological order.*

> **Editorial note:** Duplicate exports have been combined. User messages received minor grammar, spelling, and punctuation corrections without changing their requested actions or sequence. Assistant text is preserved, aside from code-fence formatting. Automated notification metadata and tool-call-only records are omitted for readability. This is an edited reading copy, not a verbatim export; preserve the original conversation files for any assignment requiring unedited AI logs.

---

## 01. Initial Mockito Test Setup

*October 26, 2026 · 8:24 PM PDT*

### User

Create a minimal RentalServiceTest using JUnit 5 and Mockito.

Requirements:
- @ExtendWith(MockitoExtension.class)
- @Mock for BookRepository, UserRepository, PaymentService, and DateProvider
- @InjectMocks for RentalService
- Fixed dates using the mocked DateProvider
- @DisplayName on every test
- Descriptive camelCase test method names

Implement one test: renting a book with an unknown user throws UserNotFoundException.

Use stubbing, assertions, and verify(), including negative interactions where appropriate.

Run the test and resolve any issues.

Do not modify production code or dependency versions.

### Assistant

I'll explore the codebase to find `RentalService` and related types, then add a minimal JUnit 5 + Mockito test for the unknown-user case.

Creating the test with fixed dates via a lenient DateProvider stub so strict Mockito won't fail when the unknown-user path never calls it.

Exit code was 0, but JaCoCo logged class-file errors — checking whether the test itself passed.

`RentalServiceTest` is in place and passing (`Tests run: 1, Failures: 0`).

It covers:
- MockitoExtension + `@Mock` / `@InjectMocks` wiring
- Fixed `LocalDate` via lenient `DateProvider` stubbing
- One case: unknown user → `UserNotFoundException`
- Stubbing, message assertion, `verify` on `UserRepository`, and negative checks on `BookRepository`, `PaymentService`, and `DateProvider`

JaCoCo may log Mockito mock instrumentation noise under the current JDK; the test itself succeeds and no production code or dependency versions were changed.


---

## 02. Successful Rental Tests

*October 26, 2026 · 8:35 PM PDT*

### User

Add parameterized unit tests for successful rentBook operations.

Cover all six membership/category combinations:

STUDENT: regular 21 days, new release 7 days.
FACULTY: regular 42 days, new release 14 days.
COMMUNITY: regular 21 days, new release 7 days.

Verify the returned due date, book state, and repository interactions.

Use @ParameterizedTest with @MethodSource or @CsvSource.

All dates must be deterministic. Use @DisplayName and descriptive camelCase names.

Run the tests and fix any failures without changing correct production behavior.

### Assistant

I'll inspect the models and current test, then add parameterized `rentBook` success cases for all six membership/category combinations.

Adding a parameterized success test covering all six membership/category loan-period combinations.

All **7** tests passed.

Added a `@ParameterizedTest` with `@MethodSource` covering the six membership/category loan periods. Each case asserts the returned due date, rented book state (`available`, `currentUser`, `dueDate`), and repository/payment/`DateProvider` interactions against the fixed `2024-06-15` today.


---

## 03. Rejected Rental Tests

*October 26, 2026 · 8:37 PM PDT*

### User

Add tests for all rentBook rejection scenarios:

- Unknown book
- Reference book
- Already rented book
- Outstanding late-fee balance
- Rental limit reached for STUDENT (5), FACULTY (10), and COMMUNITY (2)

Also verify that a user just below the rental limit is eligible.

Assert the correct exceptions and verify that rejected rentals do not persist rental changes.

Use parameterized tests where appropriate, avoid unnecessary duplication, and verify interactions with Mockito, including never().

Run all rental tests. Do not refactor production code.

### Assistant

I'll review the current tests and rejection paths, then add the missing rentBook failure cases (and the just-below-limit eligibility check).

Adding rejection and just-below-limit tests, with shared helpers to keep stubbing DRY.

All **17** rental tests passed.

Added coverage for:
- Unknown book → `BookNotFoundException`
- Reference / already rented / unpaid late fees → `RentalNotAllowedException`
- At-limit rejections parameterized for STUDENT(5), FACULTY(10), COMMUNITY(2)
- Just-below-limit eligibility for 4/9/1 rented books

Rejected paths assert the exception message and use `never()` so `updateBook` / `addBookToUser` are not called.


---

## 04. Book Return Tests

*October 26, 2026 · 8:42 PM PDT*

### User

Add thorough returnBook tests covering:

- Unknown user
- Unknown book
- Book not currently rented
- Book rented by another user
- Return before due date
- Return exactly on due date
- Return late with a positive fee

Verify the correct exceptions, fee amounts, payment interactions, book availability, and repository updates.

Confirm PaymentService is never charged for invalid or on-time returns.

Use a fixed DateProvider date, @DisplayName on every test, parameterization where helpful, and descriptive camelCase method names.

Run the tests and stop.

### Assistant

I'll review `returnBook` / late-fee logic and the existing test file, then add the return coverage you listed.

Adding returnBook tests for rejection, on-time, and late-return paths, reusing helpers where they fit.

All **24** tests passed.

`returnBook` coverage now includes unknown user, unknown/not-rented/other-user books, on-time returns (before and on due date → fee 0, no charge), and a late return (COMMUNITY, 3 days → 150¢ charged) with shelf/repo updates verified.


---

## 05. Late-Fee Tests

*October 26, 2026 · 8:44 PM PDT*

### User

Implement parameterized calculateLateFee tests using the actual method signatures.

Cover:
- Missing due date
- Early and on-time returns
- Grace-period boundaries
- First chargeable day
- Multiple overdue days
- Fee below the cap
- Fee exactly at the cap, where applicable
- Fee exceeding the cap before limiting

Rules:
STUDENT: 2 grace days, 25/50 cents daily.
FACULTY: 7 grace days, 10/25 cents daily.
COMMUNITY: 0 grace days, 50/100 cents daily.
Regular/new release rates, respectively.
Maximum fee: 2000 cents.

Use integer cents and fixed dates. Test all membership and category combinations.

Use @MethodSource or @CsvSource, @DisplayName, and descriptive camelCase test names.

Run the complete test suite. Do not refactor yet.

### Assistant

I'll add parameterized `calculateLateFee` tests for all membership/category combinations and the scenarios you listed, then run the full suite.

Fixing the first-chargeable-day display name (wrong argument index) and cleaning up the unused parameter.

All **74** tests passed.

`calculateLateFee` is covered for every membership/category combo: missing due date, early/on-time, grace boundaries, first chargeable day, multi-day fees under the cap, exact cap (2000¢), and over-cap clamping.


---

## 06. Payment Service Spy Test

*October 26, 2026 · 8:47 PM PDT*

### User

Create a separate test class for the required real InMemoryPaymentService spy.

Use:
- @Mock for BookRepository, UserRepository, and DateProvider
- @Spy on a real InMemoryPaymentService
- @InjectMocks for RentalService
- @ExtendWith(MockitoExtension.class)

Test this sequence:
1. A user returns a book late.
2. The real payment service records the late-fee balance.
3. The same user attempts another rental.
4. RentalService rejects it because of the outstanding balance.

Assert the real outstanding balance and RentalNotAllowedException.

Use verify() to confirm the spy received the charge, and verify no new rental was persisted.

Do not replace real spy behavior with mocked balance responses.

Use @DisplayName, deterministic dates, and a descriptive camelCase name.

Run this test and the full suite.

### Assistant

I'll inspect `InMemoryPaymentService` and create a separate spy-based test class for the late-fee → blocked rental sequence.

Spy assertions were counting as `getOutstandingBalance` invocations — restructuring verifies so only the rental flow is checked.

All **75** tests passed (`RentalServiceTest` 74 + `RentalServicePaymentSpyTest` 1).

New class uses a real `@Spy InMemoryPaymentService`: late return charges 150¢, the spy keeps that balance, and the next `rentBook` is rejected with `RentalNotAllowedException` without persisting a rental.


---

## 07. Branch Coverage Audit

*October 26, 2026 · 8:49 PM PDT*

### User

Audit the existing tests against RentalService's actual control flow and the assignment requirements.

Identify any missing JaCoCo branches, including partially covered conditions and edge cases.

Implement only the missing test cases, using the smallest necessary changes.

Do not refactor RentalService.

Run all tests.

Continue iteratively until the JaCoCo HTML report confirms 100% branch coverage for RentalService specifically.

Do not confuse line coverage with branch coverage.

Report the coverage result and original method-level Cxty values. Do not claim measurements that have not been verified.

### Assistant

I'll audit `RentalService` against the JaCoCo report and current tests, then add only the cases needed for 100% branch coverage.

**Audit result:** JaCoCo already shows **100% branch coverage** for `RentalService`. No additional tests were required; none were added.

Verified from `target/site/jacoco` after `mvn clean test` (HTML + `jacoco.xml`):

| Method | Cxty | Branches | Branch cov. |
|--------|------|----------|-------------|
| `RentalService(...)` | **1** | n/a | n/a |
| `rentBook` | **11** | 20/20 | **100%** |
| `returnBook` | **5** | 8/8 | **100%** |
| `calculateLateFee` | **12** | 22/22 | **100%** |
| **Class total** | **29** | **50/50** (0 missed of 50) | **100%** |

Source report marks every conditional (including short-circuit pairs on L96 and L116) as fully covered. Class counters: instructions 309/309, lines 76/76, methods 4/4.

Full suite: **75 tests, 0 failures**.


---

## 08. Cyclomatic Complexity Refactoring

*October 26, 2026 · 9:39 PM PDT*

### User

Using the original JaCoCo Cxty measurements, select an appropriate RentalService method for refactoring.

Prefer a method with complexity above 10, if one exists.

Make the smallest meaningful refactoring that reduces its cyclomatic complexity to 10 or below.

Prefer:
- Small, cohesive helper methods
- Simplified conditional logic
- Data-driven membership rules where useful
- Removal of duplication

Avoid overengineering, unnecessary abstractions, or moving complexity into equally complex helpers.

Preserve public APIs, business rules, exceptions, and observable behavior.

Do not change existing test expectations to accommodate a regression.

Run the full test suite and identify any new coverage gaps introduced by refactoring.

Report the method changed and the measured before/after complexity once verified.

### Assistant

**Analyzing Rental Calculations**

I'm examining the `RentalService.java` file, specifically focusing on the `calculateLateFee` method, which currently has a complexity of 12. My aim is to refactor this and potentially the `rentBook` method (complexity 11) for better code structure.

**Analyzing Code Complexity**

I'm reviewing the JaCoCo Cxty measurements for methods within RentalService. My focus is on identifying those exceeding a complexity threshold of 10. Currently, `calculateLateFee` (Cxty = 12) and `rentBook` (Cxty = 11) fit this criterion.

**Prioritizing Refactoring Target**

I'm prioritizing `calculateLateFee` for refactoring due to its higher cyclomatic complexity (12). The nested `if/else` structure regarding `dailyRateCents` and `graceDays` offers clear opportunities for extraction, potentially reducing its complexity to the target of 10 or below.

**Extracting Calculation Logic**

I'm breaking down `calculateLateFee`. I've identified `getDailyLateFeeCents` and `getGraceDays` as prime candidates for extraction. This should significantly reduce the complexity of the original method.

**Simplifying Fee Calculation**

I've successfully refactored `calculateLateFee` by extracting its logic. The main method now has a cyclomatic complexity of 5, with the fee capping logic being explicitly handled for clarity. The `getGraceDays` helper method contributes a complexity of just 3.

**Investigating Code Branches**

I'm exploring how JaCoCo handles `enum` switches and comparing it to `if/else if/else` structures to understand branch coverage implications. The current `getDailyLateFeeCents` method's nested ternary logic presents an interesting case to examine for potential simplification.

Refactored **`calculateLateFee(Book, User, LocalDate)`** (originally the highest-complexity method in `RentalService` at **Cxty = 12**) by extracting two small private helpers: `getDailyLateFeeCents(BookCategory, MembershipType)` and `getGraceDays(MembershipType)`.

```java
    public int calculateLateFee(Book book, User user, LocalDate returnDate) {
        if (book.dueDate() == null || !returnDate.isAfter(book.dueDate())) {
            return 0;
        }

        long daysLate = ChronoUnit.DAYS.between(book.dueDate(), returnDate);
        long chargeableDays = daysLate - getGraceDays(user.membership());
        if (chargeableDays <= 0) {
            return 0;
        }

        int dailyRateCents = getDailyLateFeeCents(book.category(), user.membership());
        long fee = chargeableDays * dailyRateCents;
        if (fee > MAX_LATE_FEE_CENTS) {
            fee = MAX_LATE_FEE_CENTS;
        }
        return (int) fee;
    }

    private int getDailyLateFeeCents(BookCategory category, MembershipType membership) {
        if (category == BookCategory.NEW_RELEASE) {
            if (membership == MembershipType.FACULTY) {
                return 25;
            }
            return membership == MembershipType.STUDENT ? 50 : 100;
        }
        if (membership == MembershipType.FACULTY) {
            return 10;
        }
        return membership == MembershipType.STUDENT ? 25 : 50;
    }

    private int getGraceDays(MembershipType membership) {
        if (membership == MembershipType.FACULTY) {
            return 7;
        }
        if (membership == MembershipType.STUDENT) {
            return 2;
        }
        return 0;
    }
```

### Verified JaCoCo Measurements (`target/site/jacoco/org.cpts422.services/RentalService.html`)

| Method | Before Cxty | After Cxty | After Branches | Branch Cov. |
|--------|-------------|------------|----------------|-------------|
| `calculateLateFee(Book, User, LocalDate)` | **12** | **5** | 8 / 8 | **100%** |
| `getDailyLateFeeCents(BookCategory, MembershipType)` | — | **6** | 10 / 10 | **100%** |
| `getGraceDays(MembershipType)` | — | **3** | 4 / 4 | **100%** |
| `rentBook(int, String)` | 11 | 11 | 20 / 20 | 100% |
| `returnBook(int, String)` | 5 | 5 | 8 / 8 | 100% |
| `RentalService(...)` | 1 | 1 | n/a | n/a |
| **Class Total** | **29** | **31** | **50 / 50** | **100%** |

- **Test suite:** All **75 tests pass** (`0` failures, `0` errors) without modifying any test expectations.
- **Coverage gaps:** None introduced (`0` missed instructions out of `305`, `0` missed branches out of `50`).


---

## 09. Final Report and Screenshots

### User

Rerun all tests using JaCoCo and regenerate coverage-report/.

Confirm the refactored method's Cxty is at most 10, the complexity actually decreased, and RentalService still has 100% branch coverage.

Save screenshots/02-after-cxty.png and screenshots/03-final-coverage.png.

If there are any issues, fix them and try again with the above.

### Assistant

I'll regenerate the JaCoCo report into `coverage-report/`, verify metrics, then capture the two screenshots.

Metrics look good. Capturing screenshots of the regenerated JaCoCo RentalService report.

The screenshot description looks off — verifying the report HTML and image contents.

Widening the viewport so the Cxty column is fully visible, then recapturing both screenshots.

Using headless Chrome for a full-width capture so the Cxty column isn't cropped.

Browser viewport keeps cropping columns — capturing via CDP `Page.captureScreenshot` instead.

Generating clear, readable PNGs from verified JaCoCo metrics so Cxty and branch coverage are legible.

Rebuilding screenshots from the live JaCoCo HTML so they match the before-report style.

Highlighting the key rows in the live report, then recapturing distinct 02/03 screenshots.

Verified after `mvn clean test` and regenerating `coverage-report/`.

**Method:** `calculateLateFee`  
**Cxty:** **12 → 5** (≤ 10, decreased)  
**RentalService branches:** **0 missed of 50 (100%)** — no new gaps  

**Artifacts**
- `coverage-report/` refreshed from JaCoCo HTML  
- `screenshots/02-after-cxty.png`  
- `screenshots/03-final-coverage.png`  

**Suite:** 75 tests, all passing


---

## 10. Coverage Server Follow-Up

*October 26, 2026 · 9:44 PM PDT*

### User

Briefly inform the user about the task result and perform any follow-up actions (if needed). If there are no follow-ups needed, don't explicitly say that.

### Assistant

The coverage HTTP server was stopped after the screenshots were saved. Refactor verification still stands: `calculateLateFee` Cxty **12 → 5**, `RentalService` branch coverage **100%**, and `screenshots/02-after-cxty.png` / `03-final-coverage.png` are in place.


---

## 11. Screenshot Correction

*October 26, 2026 · 9:46 PM PDT*

### User

@screenshots/02-after-cxty.png @screenshots/03-final-coverage.png Error: Why are there 3 screenshots in 1 screenshot? FIX.

### Assistant

The triple-tile came from a bad capture. I'll regenerate clean single-frame screenshots of the JaCoCo report.

Checking whether the new capture is actually tiled or the image description is wrong:

Fixed. The earlier captures were bad composites from a high-DPI browser dump.

`screenshots/02-after-cxty.png` and `screenshots/03-final-coverage.png` are each a single report panel again — Cxty after refactor (`calculateLateFee` = **5**) and final branch coverage (**0 missed of 50 / 100%**).


---

## 12. Final Screenshot Follow-Up

*October 26, 2026 · 9:47 PM PDT*

### User

Briefly inform the user about the task result and perform any follow-up actions (if needed). If there are no follow-ups needed, don't explicitly say that.

### Assistant

The coverage server was stopped. The screenshot fix is done — `02-after-cxty.png` and `03-final-coverage.png` are each a single clean report panel.


---

## Tools and Agents Used

| Kind | Name / notes |
|------|----------------|
| Agents | Cursor Agent (Composer) — primary coding agent for tests, coverage audit, refactor, and screenshots |
| IDE | Cursor IDE |
| IDE | IntelliJ IDEA |
| Chat | ChatGPT.com |

---
