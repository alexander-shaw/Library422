# LibraryApplication — HW2

JUnit 5 + Mockito tests and JaCoCo coverage for `RentalService`, including a complexity refactor of `calculateLateFee`.

## How to run tests

```bash
mvn clean test
```

JaCoCo HTML is written to `target/site/jacoco/`. This repository also keeps a copy at `coverage-report/` (refreshed from that output).

## Coverage report path

- Primary HTML: `coverage-report/index.html`
- `RentalService` class report: `coverage-report/org.cpts422.services/RentalService.html`
- Machine-readable: `coverage-report/jacoco.xml`

## Final branch coverage (`RentalService`)

**100%** — **0 missed** of **50** branches (JaCoCo “Missed Branches”; verified from `coverage-report/jacoco.xml` after `mvn clean test`).

## Complexity refactor

| Item | Value |
|------|--------|
| Method | `calculateLateFee(Book, User, LocalDate)` |
| Before Cxty | **12** (from `screenshots/01-before-cxty.png` / pre-refactor JaCoCo) |
| After Cxty | **5** (from `screenshots/02-after-cxty.png` / post-refactor JaCoCo) |

### Refactoring (brief)

Fee rate and membership grace-day logic were extracted into private helpers `getDailyLateFeeCents(...)` and `getGraceDays(...)`, leaving `calculateLateFee` to handle due-date checks, chargeable days, multiplication, and the 2000¢ cap.

## Screenshots

| File | Shows |
|------|--------|
| `screenshots/01-before-cxty.png` | Pre-refactor Cxty (`calculateLateFee` = 12) |
| `screenshots/02-after-cxty.png` | Post-refactor Cxty (`calculateLateFee` = 5) |
| `screenshots/03-final-coverage.png` | Final 100% branch coverage (**0 missed** of **50** branches) |

## AI log

| Path | Description |
|------|-------------|
| `ai-log/` | AI usage log folder |
| `ai-log/history.md` | Chronological AI conversation history |

**Tools used:** IntelliJ IDEA, Cursor IDE, ChatGPT.com.

## Project layout

```
LibraryApplication/
├── src/
│   ├── main/java/
│   └── test/java/
├── coverage-report/
│   ├── index.html
│   └── ...
├── screenshots/
│   ├── 01-before-cxty.png
│   ├── 02-after-cxty.png
│   └── 03-final-coverage.png
├── ai-log/
│   └── history.md
├── pom.xml
└── README.md
```
