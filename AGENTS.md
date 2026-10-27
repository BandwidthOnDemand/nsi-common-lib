# AGENTS.md

## Repository-Specific Rules

These rules apply to implementation and review in this checkout. The code review
rules below add requirements when the user requests a review.

### Build and dependencies

- This library targets Java 17 bytecode and supports builds and runtime use only on
  Java 17, 21, 25, and 27. Maven 3.9 or newer is required. Other Java majors are
  outside the supported configuration. CI must build on all four supported JDKs
  and run the same Java 17-built packaged artifact on each supported runtime.
- Run focused tests with
  `mvn --batch-mode --no-transfer-progress -Dtest=ClassName test`, then run
  `mvn --batch-mode --no-transfer-progress clean verify` for final verification.
  Add `-o` when dependencies and plugins are already cached.
- `make build` skips tests. Use `make test` or Maven verification to exercise them.
  The Makefile selects its Java 17 Maven container through `MAVEN_IMAGE`.
- Keep dependency and plugin versions fixed to stable releases, using the Spring
  Boot parent and existing `pom.xml` properties for version management. Verify
  compatibility when upgrading; successful compilation alone is insufficient.
- Preserve public signatures and checked-exception contracts when making bug or
  compatibility fixes. Document intentional API migrations.

### JAXB, XML, and encoding

- Edit `src/main/schema/**` for generated model and binding changes. Maven XJC
  generates the NML, NSA, and signature packages under `target/`; do not hand-edit
  generated Java files or their documentation.
- JAXB and Mail use `jakarta.xml.bind.*` and `jakarta.mail.*`. Java SE XML APIs,
  including `javax.xml.namespace`, `javax.xml.datatype`, and `javax.xml.crypto`,
  retain their `javax` packages. Preserve schema namespaces and root QNames.
- Reuse JAXB contexts, but create marshallers and unmarshallers per operation.
  Preserve the legacy `{uri}local` wrapper used by generic serialization helpers.
- Keep production XML parsing namespace-aware and reject DTDs and external entities.
  Reuse the secure `DomParser` entry points for XML text and byte inputs.
- Treat Java strings as decoded characters. Byte inputs follow their XML encoding
  declarations; text codecs use UTF-8. Preserve Unicode through serialization,
  including DOMs originally parsed from a different byte encoding.
- Parsing and materializing helpers leave caller-owned input streams open. Methods
  opening files close their own streams. Callers own wrappers returned by stream
  codecs; closing those wrappers can close the underlying streams.
- Apply gzip before transfer encoding, and decompress once after transfer decoding.
  Finish or close encoding wrappers before extracting their completed output.

### XML signatures

- Use public JCA and XML signature APIs. Support `jsr105Provider` overrides through
  registered provider names/classes or publicly constructible external providers;
  do not introduce JDK-internal reflection or module-access flags.
- Preserve RSA-SHA256 and DSA-SHA256 signing with SHA-512 reference digests. Keep
  secure validation enabled and reject SHA-1 and unsupported signing key types.
- `generateExternalSignature` returns a separate signature document without changing
  the source DOM. Its sole reference is `urn:net:es:nsi:common:document`, resolved
  locally from the supplied DOM. Require the inclusive canonicalization transform
  with comments and reject other targets, extra references, and other transforms.
- Preserve signed text whitespace and comments covered by detached signatures;
  pretty-print before signing. Enveloped signing intentionally modifies its input.
- Embedded certificate selection verifies signature integrity, not certificate trust
  or signer authorization. Keep that boundary explicit in APIs and documentation.
- Missing aliases, unavailable certificates, malformed signatures, and ambiguous
  signature structures must fail explicitly rather than produce misleading success
  or incidental null-pointer failures.

### Domain and URL contracts

- Validate every token and endpoint in a label-family group against its registered
  `LabelType` bounds before allocating individual range members. Retain the
  1,048,576-value limit per range and overflow-safe arithmetic. The standalone
  `stringToIntegerSet` helper supports nonnegative integers through
  `Integer.MAX_VALUE`; do not impose VLAN or MPLS family bounds on that API.
- Preserve label normalization, equality/hashCode consistency, mixed-family round
  trips, encounter order, and deduplication. Rejected label mutations leave the old
  state intact. STPs own their label-set membership; their label objects remain
  shared and mutable. Identifier hashing must match case-insensitive equality,
  including Unicode case variants.
- Select inbound and outbound NML ports by their respective relation types. Extension
  lookup must respect namespaces as well as local names.
- URL appending preserves raw escapes, authority, query, and fragment for hierarchical
  URLs. For opaque URLs such as `jar:file:/tmp/example.jar!/dir`, preserve the raw
  scheme-specific component; `getRawPath()` is null. Join with one slash, avoid
  double encoding, and do not replace concatenation with relative-URI resolution.
  URL construction must not fetch the target.

### Regression tests and documentation

- Add focused tests for corrected behavior and confirm they expose the original
  defect when practical. Keep tests independent of external services and use
  JUnit-managed temporary directories for output.
- Exercise invalid-range allocation regressions in an isolated JVM with `-Xmx64m`,
  a process timeout, and captured diagnostics. Reuse the pattern in
  `LabelContractsTest`; do not constrain the full suite to that heap because valid
  full-domain MPLS expansion requires more memory.
- Use the running JDK's `keytool` and `SigningTestSupport` for disposable signing
  fixtures. Serialize tests that change JVM system properties or the default time
  zone, and restore prior values, including previously absent properties.
- Keep the configured 85% line and 75% branch coverage gates passing. Reports are
  generated under `target/site/jacoco/`. Generated JAXB model packages are excluded;
  handwritten parser wrappers remain included.
- Maintain Javadoc for handwritten APIs and package overviews. Document mutation,
  collection sharing, stream ownership, null/error behavior, encoding, and security
  boundaries. Use implementation comments for non-obvious reasons and edge cases.
- Maven verification skips Javadoc by default. For documentation changes, run JDK
  Javadoc separately over handwritten sources with the compiled classes and Maven
  dependencies on its classpath. Use `-Xdoclint:all,-missing -Werror` to validate
  syntax and links without implicit-constructor warnings; still document explicitly
  declared APIs. Run this validation on JDK 17 and JDK 27. Finish with
  `git diff --check`.

## Agent Coordination

### Shared rules and task ownership

- The Repository-Specific Rules above apply to every agent. Custom agent files
  select a role, model, and reasoning setting; they do not relax these contracts
  or replace the required verification. Apply the Code Review Rules below when
  the assigned task is a review.
- Keep repository requirements and the review rubric in this AGENTS.md as their
  single maintained source. Use .codex/PROJECT_CONTEXT.md only for supplemental
  facts that are not already specified here.
- If implementation, supplemental context, or generic role guidance conflicts
  with an explicit repository rule, identify the discrepancy; do not silently
  redefine the rule to match the implementation.
- Respect the user's task scope and preserve unrelated working-tree changes.
  Inspect the working tree before editing. Do not commit, push, publish, deploy,
  or change shared systems unless the user's task authorizes that action.

### Agent routing

Custom agents live in .codex/agents/. For a named specialist request, delegate to
that custom agent so its configured model and reasoning effort apply. Otherwise,
use only the specialists the task needs; do not invoke the whole roster by default.

| Agent | Assignment |
| --- | --- |
| planner | Architecture, migrations, and implementation plans |
| java_coder | Java implementation, bug fixes, and focused regression tests |
| reviewer | Strict correctness, concurrency, reliability, and compatibility review |
| documenter | Handwritten Javadoc, package overviews, README, and API documentation |
| test_engineer | Unit, integration, regression, and failure-path tests |
| debugger | Root-cause analysis and reproducible failure investigation |
| security_reviewer | XML and signing security, input handling, and data boundaries |
| performance_analyst | Allocation, parsing, memory, latency, and scaling analysis |
| refactorer | Scoped behavior-preserving Java refactoring |
| build_engineer | Maven, dependency management, generated sources, and CI |

### Coordination and verification

- Give delegated tasks a concrete outcome, scope, relevant contracts, and file
  ownership. Pass review baselines and acceptance criteria explicitly.
- Run writers sequentially in a shared checkout. Parallel writers require isolated
  worktrees and an integration owner. Parallel readers must not race build output,
  generated sources, signing fixtures, or shared JVM state.
- Serialize Maven clean/build/verification and Javadoc commands in one checkout.
  Assign one verification owner to run the required final checks after all edits
  have settled. Focused worker checks do not replace final Maven verification.
- For documentation changes, include the separate JDK Javadoc validation required
  above. A successful Maven verification alone does not satisfy that requirement.
- Make the invalid-range JVM isolation and shared-property/time-zone isolation
  requirements explicit when assigning relevant tests.
- Settle implementation edits before reviewing a snapshot. Reassess affected
  findings and checks if code changes after review.
- Child agents return scoped results without further delegation unless the parent
  explicitly assigns it. The primary agent integrates results and deduplicates
  findings while remaining responsible for the final result.
- A review or diagnosis task does not authorize fixes. Implementation already
  authorized by the user does not need another approval gate after planning.
- Report the changed behavior, affected files, checks actually run and their
  results, and remaining limitations. Distinguish observed results from static
  reasoning, and do not claim unavailable checks passed.

## Code Review Rules

Apply these rules when asked to review Java code. These are review instructions;
they do not turn implementation tasks into review-only tasks.

### Review standard

- Be rigorous, skeptical, and direct. Prioritize correctness, security, data
  integrity, reliability, and compatibility. Critique code and explain consequences.
- Report every distinct, actionable defect supported by evidence within the
  requested scope. Do not stop after the first finding or impose a findings quota.
- For each finding, establish a reachable trigger, the faulty behavior, and the
  practical impact. A defensible code-path analysis is sufficient; execution is
  not required when the defect follows clearly from the code.
- Investigate plausible concerns before reporting them. Check callers, guards,
  contracts, framework behavior, and configuration that might invalidate a claim.
- Separate unresolved questions from defects. Do not present hypothetical callers,
  unknown requirements, or unsupported workload assumptions as established facts.
- Exclude personal preferences, generic best-practice advice, and mechanical style
  issues unless they violate an explicit requirement or cause a concrete problem.
- Do not invent issues, inflate severity, or equate unfamiliar code with bad code.

### Scope and method

- Establish the review target and baseline. For a branch review, identify the base,
  merge base, and reviewed head; state whether uncommitted changes are included.
  If the baseline is ambiguous, resolve it from context or ask before comparing.
- For change reviews, report defects introduced, exposed, or materially worsened
  by the change. Keep unrelated pre-existing issues out of the findings. For an
  explicitly requested full audit, evaluate all code in the requested scope.
- Read applicable repository instructions and relevant build files, CI settings,
  documentation, tests, callers, and implementations. Trace changed behavior
  across class and module boundaries where needed to evaluate its consequences.
- Identify the actual Java version and relevant dependency/framework versions.
  Verify version-sensitive behavior rather than assuming the newest semantics.
- Examine normal paths, boundaries, failures, concurrency, and lifecycle behavior
  where applicable. After analysis, make a final pass to remove false positives
  and merge findings that share the same root cause.
- Review only: do not edit tracked source, tests, configuration, dependencies, or
  repository instructions unless the user separately requests fixes.

### Correctness and Java contracts

- Trace nullability from real inputs and callers, including nullable return values
  and unboxing. Do not demand redundant null checks where contracts exclude null.
- Check boundary values, empty collections, overflow, truncation, numeric precision,
  parsing, character encoding, locale, time zones, and daylight-saving transitions.
- Check reference versus value equality, equals/hashCode consistency, comparator
  contracts, mutable map keys, collection mutation, aliasing, and defensive copies.
- Verify validation occurs before side effects and that partial failure cannot
  silently leave invalid state. Check ordering, duplicate requests, and idempotency.
- Check API, exception, serialization, schema, and configuration compatibility,
  including existing clients and mixed-version deployments when supported.

### Concurrency and asynchronous execution

- Establish which objects and state are actually shared and which threads access
  them. Check data races, visibility, safe publication, and compound operations.
- Do not assume volatile fields or concurrent collections make multi-step actions
  atomic. Inspect invariants that span multiple fields, keys, or operations.
- Check lock ordering, deadlock paths, lock scope, and blocking while holding locks.
- Inspect executor and queue bounds, starvation, backpressure, task rejection,
  asynchronous exception propagation, and dropped or unobserved failures.
- Trace timeout, interruption, cancellation, and shutdown behavior. Check whether
  interrupted execution exits or restores interrupt status as the contract requires.
- Check ThreadLocal and request-context cleanup across pooled or asynchronous work.
  Do not assume a singleton, framework bean, or parallel stream is thread-safe.

### Resources, failures, and operations

- Trace ownership and cleanup for files, streams, sockets, JDBC objects, HTTP
  responses, executors, and other resources on success and failure. Do not require
  code to close resources owned by its caller or managed by the framework.
- Check swallowed exceptions, misleading success responses, lost causes, and error
  handling that hides failed or partially completed operations.
- Inspect connection and request timeouts, retry bounds, backoff, and idempotency.
  Check whether retries can duplicate effects or amplify an existing outage.
- Inspect shutdown and startup paths for unfinished work, leaked resources, and
  incorrect health signals. Flag missing diagnostics only when they prevent a
  specific failure from being detected or investigated.

### Security and data boundaries

- Trace untrusted inputs through authorization decisions and sensitive operations.
  Check object-level access controls and cross-user or cross-tenant data exposure.
- Check injection, unsafe deserialization, path traversal, SSRF, XML external
  entities, and unsafe process execution where the code exposes those surfaces.
- Check TLS certificate and hostname verification, credential handling, and secret
  or sensitive-data exposure in logs, errors, telemetry, and serialized responses.
- Check whether request size, decompression, parsing, regular expressions, or other
  attacker-controlled work can exhaust memory, CPU, threads, or connections.
- Demonstrate the relevant input path and missing protection. Do not report a
  vulnerability solely because a potentially dangerous API appears in the code.

### Persistence and frameworks, when applicable

- Verify transaction boundaries, rollback behavior, isolation assumptions, and
  consistency between database changes and external effects.
- Check uniqueness and locking assumptions, lost updates, concurrent writes,
  pagination correctness, and duplicate processing after retries or redelivery.
- For Spring/JPA/Hibernate or similar frameworks, inspect relevant proxy behavior,
  annotation applicability, entity identity, lazy loading, and query behavior.
  Base findings on the configured framework version and actual execution path.
- Inspect migrations and persisted formats for data loss, deployment ordering, and
  backward compatibility where mixed versions or rollback must be supported.

### Performance and retained memory

- Look for reachable N+1 queries, repeated remote calls, avoidable quadratic work,
  unbounded result materialization, caches, queues, listeners, and retained objects.
- Connect each finding to a credible input size, request rate, or lifecycle. State
  assumptions. Do not claim a measured regression without measurements.
- Avoid speculative micro-optimization and blanket bans on language features.

### Verification

- Use the repository's documented build wrapper and checks. Run relevant existing
  tests and configured static analysis where the environment supports them.
- Inspect commands before execution; do not run checks that deploy, modify shared
  systems, or require production access as part of an ordinary code review.
- Do not install tools, change dependencies, or alter configuration just to review.
  Report missing prerequisites and checks that could not be completed.
- Evaluate whether tests exercise the changed behavior and its significant failure
  paths. For each defect, suggest a focused regression test or reproduction.
- Report missing tests as a separate finding only when an explicit testing rule is
  violated or a concrete, consequential behavior lacks required coverage. Avoid
  duplicating a bug finding with a generic request to add tests.
- Distinguish observed test results from static reasoning. Never claim a check ran
  or passed when it did not. Passing tests do not disprove a supported defect.

### Findings and priority

- Order findings by impact and urgency. Use the following review rubric:
  - P0: Critical, broadly applicable failure requiring immediate action, such as
    unavoidable data destruction or complete service failure on a normal path.
  - P1: High-impact defect to fix urgently, such as a reachable security breach,
    serious data corruption, deadlock, or major supported-workflow failure.
  - P2: Other actionable correctness, reliability, compatibility, or demonstrated
    performance defects that should be fixed in the normal development cycle.
  - P3: Low-impact improvements; include only when the user requests suggestions.
- Priority reflects impact and urgency; confidence reflects evidence. Never raise
  priority to make an issue appear in an integration that filters lower priorities.
- For a general review task, include supported P0, P1, and P2 findings. If a review
  integration constrains severity or output format, follow that interface without
  relabeling findings or claiming filtered issues were fully reported.
- Each finding must include a short severity-prefixed title, the precise file and
  smallest useful line range, triggering conditions, impact, and supporting
  reasoning. Include a minimal correction direction and regression-test scenario
  where the interface permits. Avoid unsolicited rewrites.
- Lead with findings. Then briefly report checks performed, checks not run and why,
  remaining material questions, and coverage limitations where the interface permits.
- If no actionable defects are found, say so. Do not claim the code is defect-free,
  secure, or ready to merge solely because the review produced no findings.
