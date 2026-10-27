# Codex agents for this Java library

## Agents

| Agent | Model | Reasoning | Job |
| --- | --- | --- | --- |
| planner | gpt-6-astra | high | Architecture, migrations, and implementation plans |
| java_coder | gpt-6.1-sol | medium | Implementation, fixes, and focused regression tests |
| reviewer | gpt-6-astra | xhigh | Strict correctness and reliability inspection |
| documenter | gpt-6.1-sol | medium | Handwritten Javadoc, package overviews, and other docs |
| test_engineer | gpt-6.1-sol | high | Unit, integration, regression, and failure-path tests |
| debugger | gpt-6-astra | high | Root-cause analysis and local reproductions |
| security_reviewer | gpt-6-astra | xhigh | XML, signing, input handling, and data-boundary inspection |
| performance_analyst | gpt-6-astra | high | Allocation, parsing, memory, latency, and scaling analysis |
| refactorer | gpt-6.1-sol | high | Behavior-preserving refactoring |
| build_engineer | gpt-6.1-sol | high | Maven, fixed dependencies/plugins, generated sources, and CI |

Each role is a standalone TOML file in .codex/agents/. The file sets name,
description, developer_instructions, model, and model_reasoning_effort.
For especially difficult implementation work, adjust java_coder's effort to high
or select an available stronger model in that agent's file.

## How coordination works

The root file selects applicable repository rules. Agent files select a task role,
model, effort, and scoped output. Supplemental context records additional facts.
A role's generic wording cannot relax this checkout's explicit contracts.

The parent gives each task its relevant contracts, scope, and file ownership.
Writers run sequentially in one checkout. Maven clean/build/verification, Javadoc,
and fixtures that share files or JVM state must not race. Independent source
inspection can run in parallel after the reviewed snapshot has settled.

One verification owner runs the required final checks after all edits. Focused
worker checks do not replace final verification. Documentation changes also need
the separate handwritten-source JDK Javadoc validation from AGENTS.md.

The existing exact toolchain, Maven commands, algorithms, allocation limits,
coverage thresholds, and doclint settings remain defined in AGENTS.md. They are
not maintained again in this guide or PROJECT_CONTEXT.md.

## Example prompts

### Planning

```text
Use planner to plan the library change described below. Read the repository-specific
rules, identify affected handwritten and schema inputs, preserve applicable public
and XML contracts, and return implementation steps and verification requirements.
Do not implement the change.
[Describe the change and acceptance criteria.]
```

### Implementation

```text
Delegate this fix to java_coder. Apply the repository-specific rules, preserve the
supported API and checked-exception contracts, and add focused regression tests.
Use schema inputs for generated-model changes. Run focused checks and report the
final verification still owed to the coordinating agent.
[Describe the defect and expected behavior.]
```

### Strict review

```text
Use reviewer to inspect committed changes from the merge base of origin/main and
HEAD to HEAD. Exclude uncommitted changes. Apply both Repository-Specific Rules
and Code Review Rules in root AGENTS.md. Report all supported P0, P1, and P2 findings,
their locations, triggers, impact, and focused regression scenarios.
Do not edit tracked files; state the reviewed commits and verification performed.
```

Use the real base branch. For an audit or uncommitted work, specify that scope
instead of silently mixing it with a committed branch review.

### Documentation

```text
Use documenter to update handwritten API and package documentation for this change.
Document applicable mutation, sharing, stream ownership, errors, encoding, and
signature integrity/trust boundaries. Follow the separate JDK Javadoc validation
in AGENTS.md and finish with the prescribed whitespace check. Do not edit generated
model documentation.
```

### Test engineering

```text
Use test_engineer to strengthen regression coverage for these changes. Follow the
repository's allocation and signing fixture patterns, isolate risky allocation
cases, serialize shared-property tests, and preserve the existing coverage gates.
Do not change production code; report exposed defects to the parent.
```

### Security inspection

```text
Have security_reviewer inspect this settled diff against the root's XML and
signature contracts. Trace actual parser inputs, signature structures, provider
selection, and integrity-versus-trust boundaries. Return supported findings only;
do not change files or probe external systems.
```

### Complete change

```text
Implement the change below using the appropriate named agents. Use planner only
for consequential design work, java_coder for implementation, and test_engineer
where additional test work is useful. Run writers sequentially in this checkout.
After edits settle, use reviewer for inspection and documenter for affected docs.
Assign one owner to complete the final Maven checks and, when applicable, separate
JDK Javadoc checks required by AGENTS.md. Report actual results and remaining limits.
[Describe the change, constraints, and acceptance criteria.]
```

Use debugger, performance_analyst, refactorer, or build_engineer by name for their
specific tasks. They apply the same root contracts; they are not mandatory stages.

## Permissions and availability

Custom agents inherit the parent session's permissions and tools. A review role's
instruction to avoid edits is a behavioral instruction, not a separate read-only
sandbox. Select enforced read-only access on the parent session when required.

Agent model and effort settings are pinned in each TOML. Changing the primary
session's model does not change a role's pinned setting. Use models and efforts
available to your account; make any substitution explicit in the role's file.

Official format references:

- [Custom Codex agents](https://learn.chatgpt.com/docs/agent-configuration/subagents)
- [Project configuration](https://learn.chatgpt.com/docs/config-file/config-basic)
- [Configuration reference](https://learn.chatgpt.com/docs/config-file/config-reference)
- [Models and reasoning](https://learn.chatgpt.com/docs/models)
