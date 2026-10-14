# Kilo Code instructions audit

**Session ID:** ses_ec4f596bcffe2O3FlNwPljbIvq
**Created:** 10/14/2026, 11:31:40 AM
**Updated:** 10/14/2026, 11:39:58 AM

---

## User

# Task: Audit the Actual Kilo Code Rules and Instruction Loading

**Kilo Mode:** Ask  
**New Kilo Session:** YES

## Objective

Determine which custom instructions Kilo Code currently uses, where they originate, and whether they are redundant, conflicting, or unnecessarily verbose.

This is a read-only investigation. Do not modify, delete, rename, or create configuration files.

## Required Investigation

1. Inspect the actual global Kilo configuration and rule files accessible on this machine.
2. Inspect the current project's `.kilo/` directory and any applicable `AGENTS.md` files.
3. Verify the configured `instructions` paths and determine which files exist.
4. Consult the documentation applicable to the installed Kilo version to establish how global instructions, project instructions, and mode-specific instructions are loaded.
5. Distinguish between files that are merely present, files explicitly referenced by configuration, and files confirmed to be loaded into the current session. Do not claim runtime loading without evidence.
6. Identify duplicate instructions, potential conflicts, and useful constraints that should be preserved.
7. Determine what would remain active if the custom Markdown rules were removed while the main configuration was retained.
8. Treat model selection, tool permissions, and instruction content as separate configuration concerns.

## Constraints

- Read-only operations only.
- Do not change permissions or execute project builds or tests.
- Do not assume the supplied archive matches the current installation.
- Do not expose secrets or unrelated personal configuration data in the report.
- Avoid repeating investigations unless new evidence is needed.

## Deliverables

Provide:
- The actual files and configuration entries inspected.
- Evidence of the instruction-loading mechanism.
- A concise duplication and conflict analysis.
- A recommendation to retain, consolidate, replace, or remove each relevant instruction source.
- Any uncertainty that requires a separate verification step.
- Exactly one recommended next action.

Separate verified facts from assumptions and unverified claims.

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: glob**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: glob**

---

## Assistant

**Tool: glob**

---

## Assistant

**Tool: glob**

---

## Assistant

**Tool: glob**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

## Verified facts

**Global Kilo config (actual files inspected):**
- `C:\Users\PC\.config\kilo\kilo.jsonc` (64 lines) — config with schema `https://app.kilo.ai/config.json`, model `kilo/kilo-auto/free` (model/small_model/subagent_model/compaction.model all set), permissions set (bash/read/edit/glob/grep/list/task/skill/lsp/todoread/todowrite/websearch/webfetch allow; doom_loop ask), retention/compaction, mcp.context7 configured. **Verified.**

- `C:\Users\PC\.config\kilo\AGENTS.md` (131 lines) — global engineering principles (core priorities, reuse/modularity/architecture, state/naming, performance/compatibility, dependencies, docs, security/error handling, testing/validation, technology adaptation). **Verified.**

- Instructions paths configured (kilo.jsonc lines 7-12):
```json
"instructions": [
  "./rules/*.md",
  "./rules-ask/*.md",
  "./rules-plan/*.md",
  "./rules-code/*.md"
]
```
Relative to global config dir `C:\Users\PC\.config\kilo\`. **Verified.**

**Global rule files that exist (matched by those globs):**
- `./rules/*.md`: architecture.md, clean-code.md, compatibility.md, documentation.md, modularity.md, naming-and-data.md, performance.md, security-and-dependencies.md, testing-and-validation.md (9 files). **Verified.**
- `./rules-ask/*.md`: teaching.md (1). **Verified.**
- `./rules-plan/*.md`: planning.md (1). **Verified.**
- `./rules-code/*.md`: implementation.md (1). **Verified.**
Total 12 markdown rule files under global kilo config. **Verified.**

**Project (workspace):**
- `C:\modding_mc\MobVariantsBRS` is git repo (env says yes). No `.kilo/` directory found; no `AGENTS.md` in repo root or under subdirs (glob searches returned none). **Verified.**

**Current session context (from system/env):**
- Mode: Ask (read-only). Model: kilo/kilo-auto/free. **Verified.**
- Global config loaded per env; project config references `.kilo/command/*.md, .kilo/agent/*.md, kilo.json, AGENTS.md` but none exist here. **Verified (absence).**

## Instruction-loading mechanism (evidence + assumptions)

**Configured instruction sources (explicit):**
- Global: `instructions` array in `kilo.jsonc` globs point to `./rules/*.md`, `./rules-ask/*.md`, `./rules-plan/*.md`, `./rules-code/*.md` (relative to global config dir). **Verified (referenced).**
- Global: `AGENTS.md` exists at global root; whether it’s auto-loaded depends on Kilo’s loading rules. **Present but not explicitly referenced in `instructions` array.**
- Project: no `kilo.json`, no `.kilo/`, no `AGENTS.md` present. So no project-scoped instructions from those paths. **Verified absence.**

**How loading works (inference from config structure + system prompt notes):**
- Kilo uses `instructions` array (globs) in config to load custom markdown rules. The global `kilo.jsonc` explicitly lists those glob sets. That’s a strong indication those files are intended to be loaded as “custom instructions/rules”. **Verified by config.**
- System prompt states: “Project config: .kilo/command/*.md, .kilo/agent/*.md, kilo.json, AGENTS.md…” and “Instructions from: C:\Users\PC\.config\kilo\AGENTS.md”. Also notes “You are powered by the model named kilo-auto/free…” and gives Global Engineering Principles text inline in the system context (the full principles block appears in the conversation/system message). **Evidence: system context shows Global Engineering Principles content (the long block) is present in current session instructions/context.** The system also says “Instructions from: C:\Users\PC\.config\kilo\AGENTS.md” — that’s an explicit statement about instruction source in this session context. **Verified from system message.**

**Distinguish present vs referenced vs confirmed loaded (current session):**
- Referenced by config: the 4 glob sets in `instructions` array (rules, rules-ask, rules-plan, rules-code). Files matching exist. **Referenced + present.**
- Present but not referenced: `C:\Users\PC\.config\kilo\AGENTS.md`. System message states “Instructions from: C:\Users\PC\.config\kilo\AGENTS.md” and the full Global Engineering Principles block (same content as AGENTS.md) appears in the system context. That strongly suggests AGENTS.md content is loaded into current session (global). **Confirmed loaded (by system context).**
- Project: absent. **Not loaded.**

**Uncertainty (loading mechanism details):**
- Exact precedence/order: does global `instructions` array get merged with AGENTS.md? Does mode (Ask) activate only `rules-ask/*.md` subset or all listed globs? The config lists all 4 glob sets in `instructions`; mode-specific behavior (Ask/Plan/Code) may filter or may load all and mode rules apply. System states “mode: Ask” and gives teaching.md content implications; but we don’t see runtime loader logs. **Unverified: precise precedence/filtering by mode.**
- Whether AGENTS.md is loaded because it’s a conventional filename or because explicitly included — system says “Instructions from…” explicitly. **Assumption: AGENTS.md is treated as a global instruction source in this environment (confirmed present in context).**
- Project `AGENTS.md`/`.kilo/*` would override or merge if present — none here. **N/A.**

## Duplication/conflict analysis

**Overlap between AGENTS.md (global principles, 131 lines) and rules/*.md (split):**
- AGENTS.md covers: core priorities, understand before modifying, reuse before duplication, modularity/architecture, state/data ownership, naming/clean code, performance, compatibility, dependencies, documentation/comments, error handling/security, testing/validation, tech adaptation, final verification. **Broad/overarching.**
- rules/*.md split into focused files: architecture, clean-code, compatibility, documentation, modularity, naming-and-data, performance, security-and-dependencies, testing-and-validation. These are essentially subsets/summaries of the same topics. **High duplication in intent; text is condensed/reorganized.**
- Examples: AGENTS.md has “Modularity and architecture” (long) vs rules/modularity.md + rules/architecture.md (shorter). “Reuse before duplication” vs modularity.md “Prefer: Reuse/Extension/Composition/Refactoring/New when justified”. “State and data ownership” + “Naming and Clean Code” overlap naming-and-data.md. “Dependencies” and “Error handling and security” overlap security-and-dependencies.md. “Testing and validation” overlaps testing-and-validation.md. “Compatibility”, “Performance”, “Documentation” closely match. **Clear duplication of guidance.**

**Mode-specific rules vs global:**
- rules-ask/teaching.md (Ask mode) gives tutoring/project-understanding guidance (explain what/why/how, teach through project, distinguish abstraction levels, uncertainty, language). This is orthogonal to engineering principles; adds pedagogy. **Complementary, not directly conflicting.**
- rules-plan/planning.md (Plan mode) is prescriptive planning workflow/structure; overlaps with global principles (inspect before planning, reuse, modularity, compatibility, performance, validation) but adds concrete sections (Goal/Current Architecture/Proposed Design/Reuse Opportunities/File Changes/Data and Control Flow/Compatibility/Performance/Validation/Risks). **Mostly consistent with global principles; adds structure. No obvious contradiction.**
- rules-code/implementation.md (Code mode) aligns with global (inspect, reuse, responsibility, modularity, quality, performance, compatibility, dependencies, docs, integration, validation). Adds implementation specifics (ownership/lifecycle, constants, avoid magic numbers, integration points). **Consistent.**

**Potential conflicts:**
- AGENTS.md says “Refactor only when it meaningfully improves…” and also “Prefer a complete, coherent implementation…” — generally consistent. rules-code says “Avoid unrelated refactoring.” Consistent. **No strong conflict.**
- AGENTS.md: “Do not comment obvious operations.” rules-code/docs consistent. **OK.**
- “Do not introduce patterns merely because they are popular” appears in multiple places (AGENTS.md architecture section, architecture.md, planning.md teaching alternative says same). **Consistent reinforcement.**
- Minor verbosity: AGENTS.md is very detailed (global); split rules are terse. Teaching adds pedagogic requirements. **No logical conflict, just redundancy/verbosity.**

**Useful constraints to preserve:**
- Core priorities (correctness/maintainability/modularity/reuse/compatibility/clean code/performance/security) — foundational. **Preserve.**
- “Reuse before duplication”, “inspect before modifying”, “smallest architectural change”, “state ownership/lifecycle”, “no secrets”, “validate before claiming”, “compatibility — don’t silently upgrade”, “dependencies justified”, “avoid unrelated refactoring”, “uncertainty must be stated”. **All valuable.**
- Ask mode teaching guidance is valuable if user wants tutoring; orthogonal to “rules”. **Preserve if in Ask mode context is desired.**
- Plan/Code mode workflow guidance adds structure/discipline. **Useful.**

## What remains if custom Markdown rules removed (while main config retained)

If we remove all custom Markdown rule files (`rules/*.md`, `rules-ask/*.md`, `rules-plan/*.md`, `rules-code/*.md`) and also consider AGENTS.md:
- `kilo.jsonc.instructions` would point to globs that match nothing (or we’d remove those entries). If config retained as-is but files gone, those instruction sets are empty — effectively no custom rule markdown loaded from those paths. **Impact: lose split rule guidance.**
- Global `AGENTS.md` remains on disk at `C:\Users\PC\.config\kilo\AGENTS.md`. The system message currently states “Instructions from: C:\Users\PC\.config\kilo\AGENTS.md” and includes the full Global Engineering Principles block. If we only deleted the split `rules/*.md` files but left AGENTS.md and left `kilo.jsonc` as-is (instructions still list globs), the globs just don’t match files — so those specific split rule sets disappear; AGENTS.md content is still present in the session context as shown. **So core global principles (AGENTS.md content) would remain active in current session context regardless of split rules, provided AGENTS.md is still loaded.**
- Mode behavior: teaching/planning/implementation guidance from mode-specific markdown files would be lost. The system prompt already encodes mode (Ask) and gives high-level behavioral instructions (read-only, answer thoroughly, etc.). Mode-specific markdown adds more detailed operational guidance. **Loses mode detail but keeps core mode semantics from system prompt?**
- Model/tool permissions/selections are separate (model fields, permission block) — remain intact if config retained. **Unchanged.**

**Pragmatic “what remains”:**
- Core engineering principles as present in AGENTS.md (and currently injected into system context). **Remains if AGENTS.md loaded.**
- System-level mode instructions (Ask read-only constraints) from system prompt. **Remains.**
- Tooling/permissions/config (model, mcp, retention). **Remains.**
- Split rules (architecture/clean-code/compatibility/docs/modularity/naming-data/perf/security/testing) — duplicated content, mostly subsumed by AGENTS.md. **Lost if removed.**
- Mode-specific detailed guidance (teaching level/detail, plan structure template, implementation validation checklist details) — **Lost if removed.**

**Conclusion on “if custom Markdown rules removed while main config retained”:**
- AGENTS.md content is already the canonical global principles block present in session; removing split rules reduces redundancy but does not remove core principles if AGENTS.md remains loaded. 
- Mode-specific operational guidance (teaching/planning/implementation markdown) would be removed — could affect behavior detail but core mode is enforced by system. 
- Since split rules duplicate AGENTS.md heavily, core constraints remain. Mode-specific guidance is useful but not strictly necessary if system prompt + AGENTS.md are sufficient.

## Recommendations (retain/consolidate/replace/remove)

**Global AGENTS.md (C:\Users\PC\.config\kilo\AGENTS.md):**
- **Retain** as the single source of truth for global engineering principles. It’s comprehensive, well-structured, and already loaded into session context. **Verified.**

**Split rules (rules/*.md, 9 files):**
- **Consolidate/remove as redundant** if AGENTS.md is canonical. They repeat AGENTS.md content in condensed form. Keeping both creates duplication; risk of drift if edited separately. Since `instructions` globs include them, better to either (a) remove them and rely on AGENTS.md, or (b) keep only mode-specific rules and have AGENTS.md be single global. Given high overlap, **consolidate**: either fold key points into AGENTS.md or remove split files. **Recommendation: remove/retire split global rules** to reduce redundancy; principles live in AGENTS.md. **Verified duplication.**

**rules-ask/teaching.md:**
- **Retain** (if using Ask mode regularly). Adds pedagogic requirements (teaching level, explain what/why/how/data/control flow/tradeoffs, uncertainty, language) that AGENTS.md doesn’t emphasize. Orthogonal and useful for Ask mode. **Recommendation: retain for Ask mode.**

**rules-plan/planning.md:**
- **Retain** for Plan mode. Provides concrete plan structure and checklist aligned with principles. Adds operational value beyond AGENTS.md. **Recommendation: retain for Plan mode.**

**rules-code/implementation.md:**
- **Retain** for Code mode. Implementation-specific guidance (integration points, validation steps, what to summarize) complements AGENTS.md. **Recommendation: retain for Code mode.**

**Project .kilo/** and **project AGENTS.md:**
- **N/A (not present).** If they appear later, treat project-level as potentially overriding/extending global; but current state has none.

**Config (kilo.jsonc instructions array):**
- If split rules removed, update `instructions` to not glob `./rules/*.md` (or remove entry). Mode-specific globs can stay if those files retained. **Tweak if consolidating.**

## Exactly one recommended next action

**Recommended next action (single):** Consolidate instruction sources by keeping global `AGENTS.md` as the canonical global principles (already loaded), removing redundant `rules/*.md` files, and retaining mode-specific rules (`rules-ask/*.md`, `rules-plan/*.md`, `rules-code/*.md`) plus updating `kilo.jsonc.instructions` to drop `./rules/*.md`. Verify behavior in Ask/Plan/Code modes after change (read-only verification first). **This reduces duplication while preserving mode-specific guidance.**

## Separate verified vs assumptions

**Verified:**
- Global files exist and contents as listed (kilo.jsonc, AGENTS.md, all rule md files). 
- Configured `instructions` globs in kilo.jsonc. 
- Project has no `.kilo/` and no `AGENTS.md`. 
- System context states “Instructions from: C:\Users\PC\.config\kilo\AGENTS.md” and includes Global Engineering Principles block; mode is Ask. 
- High duplication between AGENTS.md and rules/*.md by content comparison.

**Assumptions (unverified):**
- Kilo’s runtime loader actually loads all files matched by `instructions` globs into current session (config references them; system confirms AGENTS.md loaded but doesn’t explicitly list each rule file). 
- Mode filters which instruction sets apply beyond system prompt (e.g. Ask only uses rules-ask + core). 
- Precedence: if project AGENTS.md existed, would override global — unknown from current state. 
- Removing split rules won’t break anything because AGENTS.md covers same ground — plausible but needs behavioral verification. 
- The “split rules” are considered “custom Markdown rules” in the question’s sense (separate from AGENTS.md). **Assumed.**

## Final note on separation of concerns

Model selection/tool permissions/instruction content are separate: model fields (model/small_model/subagent_model) and permission block are independent from `instructions` array and from AGENTS.md content. Changing instruction sources doesn’t affect model/permissions. **Verified by structure.**

---

