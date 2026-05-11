# System Manifest
# Role: AI Agent Bootloader

system:
  name: Jurine-Agent
  version: 2.0.0 (Token Optimized)
  mode: strict-layered-ddd

boot_sequence:
  - order: 1
    file: .claude/core/essential-rules.yaml
    purpose: Load constraints & stack
  - order: 2
    file: .claude/memory.md
    purpose: Load context
  - order: 3
    file: .claude/CLAUDE.md
    purpose: Load index

resource_map:
  design:
    - .claude/core/system-design.yaml
  domain_model:
    path: .claude/references/data-model/domains/*.yaml
    enums: .claude/references/data-model/enums.yaml

agent_behavior:
  - rule: Always validate against 'essential-rules.yaml'.
  - rule: Load 'system-design.yaml' only when designing/implementing.
  - rule: "Ask Protocol: If ambiguous, STOP and ASK."
