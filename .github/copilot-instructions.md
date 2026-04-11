# GitHub Copilot Instructions

## Project

JavaFX 21 / Java 21 Linux system monitor. Maven build. Target platform: Ubuntu, AMD GPU.

## Style

- Use `var` only when the type is unambiguous from the right-hand side. Avoid it for method
  return values, streams, or complex generics.
- Prefer records for simple immutable data carriers.
- Use `Optional` instead of returning `null` from collector methods.
- All public classes and methods must have Javadoc.
- No wildcard imports.
- Max line length: 120 characters.

## Patterns

- Collectors return `Optional<T>` or a domain-specific result object — never raw `null`.
- `Optional<T>` is a return type only — never use it as a method parameter or field type.
- File reads: always use `try-with-resources` and `Files.readString()` / `Files.readAllLines()`.
- External process invocation (`smartctl`, `nvme`): use `ProcessBuilder`, capture stdout,
  check exit code before parsing output.
- JavaFX properties: use `SimpleStringProperty`, `SimpleDoubleProperty` etc. in `MetricRow`.
- Chart data: maintain a fixed-size rolling window (default 60 samples) using `ArrayDeque`.

## Testing

- Unit test each collector independently by injecting a fake filesystem path or mocked `ProcessBuilder`.
- JUnit 5 only. No JUnit 4 annotations.
- Test class naming: `<ClassName>Test`.