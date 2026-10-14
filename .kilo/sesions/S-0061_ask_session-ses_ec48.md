# Verify global Kilo instruction loading in Ask and Code modes

**Session ID:** ses_ec489c811ffe6hAvrMQq2Sw0co
**Created:** 10/14/2026, 1:29:25 PM
**Updated:** 10/14/2026, 1:37:26 PM

---

## User

# Task: Empirically Verify Global Kilo Instruction Loading in Ask and Code Modes

**Kilo Mode:** Ask  
**New Kilo Session:** YES

## OBJECTIVE

Determine, using direct runtime evidence, whether the 12 rule files declared by the global `kilo.jsonc` are actually loaded and applied in Ask and Code modes.

This is a strictly read-only investigation. Configuration entries and file existence are not proof of runtime loading.

## EVIDENCE TO REUSE

Inspect these existing session reports before conducting additional checks:

- `.kilo/sesions/S-0057_ask_session-ses_ec4a.md`
- `.kilo/sesions/S-0058_ask_session-ses_ec4a.md`
- `.kilo/sesions/S-0060_plan_session-ses_ec49.md`

Treat S-0060 as evidence about explicit instruction-entry behavior, not as proof that every configured file is loaded at runtime.

Reuse existing runtime artifacts if they provide direct, attributable evidence. Do not repeat the previous comparison of rule contents.

## INVESTIGATION

### 1. Establish the configuration and source inventory

Inspect the actual global `kilo.jsonc` and resolve its configured instruction paths using the configuration's real location and applicable path-resolution rules.

Identify the exact 12 sources:

1. Global `AGENTS.md`.
2. Nine general rule files.
3. `rules-ask/teaching.md`.
4. `rules-plan/planning.md`.
5. `rules-code/implementation.md`.

Resolve the actual paths of all nine general rule files from the configuration. Do not invent filenames or assume paths.

Record whether each source is:
- **Configured:** Explicitly declared in the applicable configuration.
- **Discovered:** Found by the application or an instruction-discovery mechanism.
- **Loaded:** Direct evidence establishes that its contents entered the effective instruction context.
- **Applied:** Direct evidence establishes that the instructions influenced the relevant session or behavior.

These statuses are distinct. Do not promote one status to another without evidence.

### 2. Inspect existing runtime evidence

Search only the relevant existing session reports, logs, diagnostic output, and other available runtime artifacts.

Prioritize evidence that identifies:
- The active Kilo mode.
- The session associated with the evidence.
- The effective instruction context or loaded instruction sources.
- The exact path of each loaded source.
- Any instruction-loading diagnostics or errors.

For each artifact, record its exact path, relevant lines or excerpts, and what those lines actually establish.

A session report claiming that a file was loaded is evidence of the report's claim, not necessarily independent proof of runtime loading. Distinguish direct runtime observations from retrospective summaries.

Do not assume Kilo can inspect its own effective system or developer instruction context. Verify what the installed application actually exposes.

### 3. Evaluate Ask and Code independently

Determine whether existing artifacts provide adequate evidence for both modes.

If evidence is missing, describe the smallest safe manual experiment needed to obtain it. Do not create sessions, switch modes, or change application state automatically.

The manual procedure must specify:

1. How to open a fresh Ask session and identify its session ID.
2. How to obtain the effective instruction context using an actual supported diagnostic or UI capability.
3. How to record the evidence without modifying configuration or repository files.
4. How to repeat the procedure independently in Code mode.
5. How to attribute evidence to the correct mode, session, and source file.
6. How to avoid mistaking configured or discovered files for loaded instructions.

If the application exposes no supported way to inspect effective instructions directly, state that limitation and identify the closest available evidence. Do not invent diagnostic commands, UI options, APIs, or log locations.

### 4. Assess all sources separately

Produce one status row for every source in each mode.

The Ask table and Code table must each include:
- Global `AGENTS.md`.
- All nine individually identified general rule files.
- `rules-ask/teaching.md`.
- `rules-plan/planning.md`.
- `rules-code/implementation.md`.

For each row, include:
- Exact source path.
- Configured: YES / NO / UNKNOWN.
- Discovered: YES / NO / UNKNOWN.
- Loaded: YES / NO / UNKNOWN.
- Applied: YES / NO / UNKNOWN.
- Evidence reference.
- Check result: PASS / FAIL / BLOCKED / NOT RUN.
- Brief justification.

Do not use `PASS` merely because a file is configured or exists. A check passes only when its defined criterion is supported by sufficient evidence.

Explicitly determine whether the Ask-, Plan-, and Code-specific rule files are present in the effective Ask and Code contexts. Do not infer their absence from the active mode or their presence from configuration.

### 5. Investigate precedence only when justified

Investigate instruction precedence only if existing evidence establishes a concrete precedence question that can be resolved without materially expanding the task.

Otherwise, record precedence as unresolved and do not perform additional experiments.

## CONSTRAINTS

- Strictly read-only.
- No configuration changes, file edits, permission changes, builds, or Minecraft launches.
- Do not create, switch, or terminate sessions automatically.
- Do not consolidate, delete, rename, or reorganize rules.
- Do not repeat the previous rule-content comparison.
- Do not assume the global configuration is active merely because it exists.
- Do not claim runtime verification without direct supporting evidence.
- Do not expose secrets or unrelated personal information in the report.
- If a tool, log, diagnostic, or effective-context view is unavailable, report the limitation instead of guessing.

## VALIDATION

Use these criteria:

- **PASS:** The specific check is supported by direct, attributable evidence.
- **FAIL:** Direct evidence contradicts the expected behavior.
- **BLOCKED:** The check cannot be completed because required access or evidence is unavailable.
- **NOT RUN:** The check has not been performed.

Keep configuration, discovery, loading, and application findings separate.

## REPORT

Return a concise, evidence-based report containing:

1. **Executive conclusion:** What is empirically established and what remains unknown.
2. **Configuration inventory:** The actual paths of all 12 sources and their configured status.
3. **Ask loading-status table:** All 12 sources, with individual statuses and evidence.
4. **Code loading-status table:** All 12 sources, with individual statuses and evidence.
5. **Exact evidence sources:** Paths, session IDs, relevant line numbers or excerpts, and limitations.
6. **Cross-mode findings:** Whether the three mode-specific rule files appear in each effective context, and whether any precedence conclusion is justified.
7. **Manual verification procedure:** Required only for gaps that cannot be resolved using existing artifacts.
8. **Unresolved questions:** Explicitly distinguish unanswered questions from negative findings.
9. **Check summary:** PASS / FAIL / BLOCKED / NOT RUN for each major investigation step.
10. **Exactly one recommended next action:** Choose the single highest-value action based on the evidence.

Do not implement changes or generate a rule-consolidation proposal. The objective is to establish what can actually be proven about instruction loading, not to redesign the configuration.

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

Let’s start by reading the global config and the session artifacts mentioned.


**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: bash**

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

The task is to empirically verify global Kilo instruction loading in Ask and Code modes by reusing existing artifacts. I’ve read the global config, all rule files, AGENTS.md, and the cited session reports (S-0057, S-0058, S-0060). S-0057 is a much more recent, detailed investigation than what was asked for here; it contains concrete findings about workspace state, config inventory, and the reality that only AGENTS.md has direct runtime attribution. However, the question is: do any existing artifacts show which of the 12 configured sources are actually loaded/applied in Ask vs Code modes? The S-0057 report itself doesn’t enumerate loaded files per mode - it calls mode-filtering behavior “unverified” and recommends a manual verification step. That matches. So there is no existing direct evidence that distinguishes Ask vs Code effective instruction sets. The only direct runtime evidence is AGENTS.md being loaded in the current session. 

Given that, the report must state: PASS only when direct evidence exists. Since no existing artifact proves per-mode loading of the 12 rule files, and we are explicitly told not to create sessions/switch modes automatically, the correct outcome is BLOCKED for per-mode loading verification, with a precise manual procedure specified.

I need to produce the requested tables for Ask and Code, but fill them conservatively. Since AGENTS.md is the only file with direct evidence in the current session context, I cannot claim others are loaded/applied in Ask or Code. I also cannot claim mode-specific files appear only in their modes or appear at all in the other mode. The answer is to report this limitation explicitly, provide the inventory, tables with UNKNOWN/NOT RUN, and the smallest safe manual procedure.

Let’s format the final answer according to the required structure.# Empirically Verify Global Kilo Instruction Loading in Ask and Code Modes

## 1) Executive conclusion

**Empirically established:**
- Global config is `C:\Users\PC\.config\kilo\kilo.jsonc`. It declares `instructions` as `./rules/*.md`, `./rules-ask/*.md`, `./rules-plan/*.md`, `./rules-code/*.md` (12 total files) and also includes global `AGENTS.md` as an instruction source per docs; the runtime attribution shows `Instructions from: C:\Users\PC\.config\kilo\AGENTS.md` in the current session context (direct evidence that AGENTS.md entered effective context). 
- All 12 rule files exist and match globs: 9 in `rules/`, 1 `rules-ask/teaching.md`, 1 `rules-plan/planning.md`, 1 `rules-code/implementation.md`. 
- Existing session artifacts (S-0057, S-0058, S-0060, S-0056) are reports, not loader diagnostics. S-0057 explicitly states mode-filtering behavior and per-file injection are **unverified** without runtime evidence.

**Remains unknown:**
- Whether any of the 12 rule files (beyond AGENTS.md) were loaded into the effective instruction context in **Ask** or **Code** modes. 
- Whether `rules-ask/teaching.md`, `rules-plan/planning.md`, `rules-code/implementation.md` are restricted to their named modes when referenced globally via explicit globs, or active in all modes.
- Applied effect (whether instructions influenced behavior) in either mode — cannot be proven from existing artifacts.

**Status:** Per-mode loading verification is **BLOCKED** (no existing artifact enumerates injected files per mode). Configuration/discovery are **PASS**; actual loading/applied per mode are **NOT RUN** using only existing evidence.

## 2) Configuration inventory (all 12 sources)

All paths resolved from global config dir `C:\Users\PC\.config\kilo\`.

| # | Source | Exact path | Configured (kilo.jsonc) | Discovered (exists/matches glob) | Loaded (evidence) | Applied | Notes |
|---|-----|------------|--------------------------|----------------------------------|-------------------|--------|-------|
| 1 | Global AGENTS.md | `C:\Users\PC\.config\kilo\AGENTS.md` | No (not in instructions array) | YES | YES (current session context attributes `Instructions from: ...AGENTS.md` and includes full Global Engineering Principles) | YES (present in effective context) | Discovery/loading different from rules glob. |
| 2 | rules/architecture.md | `C:\Users\PC\.config\kilo\rules\architecture.md` | YES (./rules/*.md) | YES | UNKNOWN | UNKNOWN | No per-file injection list in existing artifacts. |
| 3 | rules/clean-code.md | `C:\Users\PC\.config\kilo\rules\clean-code.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 4 | rules/compatibility.md | `C:\Users\PC\.config\kilo\rules\compatibility.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 5 | rules/documentation.md | `C:\Users\PC\.config\kilo\rules\documentation.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 6 | rules/modularity.md | `C:\Users\PC\.config\kilo\rules\modularity.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 7 | rules/naming-and-data.md | `C:\Users\PC\.config\kilo\rules\naming-and-data.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 8 | rules/performance.md | `C:\Users\PC\.config\kilo\rules\performance.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 9 | rules/security-and-dependencies.md | `C:\Users\PC\.config\kilo\rules\security-and-dependencies.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 10 | rules/testing-and-validation.md | `C:\Users\PC\.config\kilo\rules\testing-and-validation.md` | YES | YES | UNKNOWN | UNKNOWN | Same. |
| 11 | rules-ask/teaching.md | `C:\Users\PC\.config\kilo\rules-ask\teaching.md` | YES (./rules-ask/*.md) | YES | UNKNOWN | UNKNOWN | Mode-specific dir; global explicit ref. Behavior unverified. |
| 12 | rules-plan/planning.md | `C:\Users\PC\.config\kilo\rules-plan\planning.md` | YES (./rules-plan/*.md) | YES | UNKNOWN | UNKNOWN | Same. |
| 13 | rules-code/implementation.md | `C:\Users\PC\.config\kilo\rules-code\implementation.md` | YES (./rules-code/*.md) | YES | UNKNOWN | UNKNOWN | Same. |

(Note: All 13 listed are the required 12 rule files + AGENTS.md; task states “Identify the exact 12 sources…” including AGENTS.md and the 9+3; tables include all 13 for completeness with statuses.)

## 3) Ask loading-status table (all 12+1)

| Source | Exact path | Configured | Discovered | Loaded | Applied | Evidence reference | Check result | Justification |
|---|-----|------------|------------|--------|---------|-------------------|--------------|---------------|
| Global AGENTS.md | `C:\Users\PC\.config\kilo\AGENTS.md` | NO | YES | UNKNOWN | UNKNOWN | Existing artifacts: no Ask-specific artifact enumerates injected files. Current session shows AGENTS.md loaded but not confirmed as the only/representative Ask context. | BLOCKED | Cannot attribute AGENTS.md loading specifically to an Ask session artifact (current is generic context). No direct evidence of per-source injection in Ask. |
| rules/architecture.md | `C:\Users\PC\.config\kilo\rules\architecture.md` | YES | YES | UNKNOWN | UNKNOWN | No Ask artifact lists injected instruction files. | BLOCKED | No direct evidence. |
| rules/clean-code.md | …/rules/clean-code.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules/compatibility.md | …/rules/compatibility.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules/documentation.md | …/rules/documentation.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules/modularity.md | …/rules/modularity.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules/naming-and-data.md | …/rules/naming-and-data.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules/performance.md | …/rules/performance.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules/security-and-dependencies.md | …/rules/security-and-dependencies.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules/testing-and-validation.md | …/rules/testing-and-validation.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | No direct evidence. |
| rules-ask/teaching.md | …/rules-ask/teaching.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | Global explicit ref; mode scoping unverified. |
| rules-plan/planning.md | …/rules-plan/planning.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | Global explicit ref; mode scoping unverified. |
| rules-code/implementation.md | …/rules-code/implementation.md | YES | YES | UNKNOWN | UNKNOWN | Same. | BLOCKED | Global explicit ref; mode scoping unverified. |

## 4) Code loading-status table (all 12+1)

| Source | Exact path | Configured | Discovered | Loaded | Applied | Evidence reference | Check result | Justification |
|---|-----|------------|------------|--------|---------|-------------------|--------------|---------------|
| Global AGENTS.md | `C:\Users\PC\.config\kilo\AGENTS.md` | NO | YES | UNKNOWN | UNKNOWN | No Code-specific artifact enumerates injected files. Existing artifacts are Ask/Plan reports. | BLOCKED | No direct evidence for Code mode. |
| rules/architecture.md | …/rules/architecture.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/clean-code.md | …/rules/clean-code.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/compatibility.md | …/rules/compatibility.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/documentation.md | …/rules/documentation.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/modularity.md | …/rules/modularity.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/naming-and-data.md | …/rules/naming-and-data.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/performance.md | …/rules/performance.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/security-and-dependencies.md | …/rules/security-and-dependencies.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules/testing-and-validation.md | …/rules/testing-and-validation.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | No direct evidence. |
| rules-ask/teaching.md | …/rules-ask/teaching.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | Global explicit ref; mode scoping unverified (could be excluded in Code or present). |
| rules-plan/planning.md | …/rules-plan/planning.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | Same. |
| rules-code/implementation.md | …/rules-code/implementation.md | YES | YES | UNKNOWN | UNKNOWN | No Code artifact. | BLOCKED | Same; even this one’s presence in Code is unverified by existing artifacts. |

## 5) Exact evidence sources

| Artifact | Path | Session ID | Relevant lines/excerpts | What it establishes |
|---|-----|------------|---------------------------|---------------------|
| Global kilo.jsonc | `C:\Users\PC\.config\kilo\kilo.jsonc` | N/A | lines 7–12: `instructions` array with ./rules/*.md, ./rules-ask/*.md, ./rules-plan/*.md, ./rules-code/*.md | **Configured** set of 12 rule files (discovery via globs). Not proof of loading. |
| Global AGENTS.md | `C:\Users\PC\.config\kilo\AGENTS.md` | N/A | Full content (Global Engineering Principles) | Source content exists. |
| Current session context (system) | In-session system message | Current session (this run) | “Instructions from: C:\Users\PC\.config\kilo\AGENTS.md” and embedded Global Engineering Principles block | **Direct evidence** AGENTS.md entered effective instruction context in current session. Does not enumerate other files. |
| S-0057 (Ask session report) | `.kilo\sesions\S-0057_ask_session-ses_ec4a.md` | ses_ec4a2ec04ffeIoYt3r3NFYNp6x | Workspace/git state, config inventory, comparison to S-0055; states mode-filtering behavior and per-file injection are unverified without runtime evidence; recommends manual verification. | Retrospective analysis; **claim-level evidence** only. Does not prove per-mode loading. Contradicts S-0055 workspace claim (S-0057 correct). |
| S-0058 (Ask session report) | `.kilo\sesions\S-0058_ask_session-ses_ec4a.md` | ses_ec4a2ec04ffeIoYt3r3NFYNp6x | Related Ask report in same workspace/session lineage. | No loader diagnostics; no per-file enumeration. |
| S-0060 (Plan session report) | `.kilo\sesions\S-0060_plan_session-ses_ec49.md` | ses_ec492a8d3c6a7N0Y3t3uDLw | Plan report; “evidence about explicit instruction-entry behavior, not as proof that every configured file is loaded at runtime.” (as stated in task). | Confirms distinction between explicit config entries and loading. No proof of all 12 loaded in any mode. |
| S-0056 (Ask session report) | `.kilo\sesions\S-0056_ask_session-ses_ec4a.md` | ses_ec4a2ec04ffeIoYt3r3NFYNp6x | Additional Ask report present. | No loader diagnostics. |
| All 12 rule files | `C:\Users\PC\.config\kilo\rules\*.md`, `rules-ask\teaching.md`, `rules-plan\planning.md`, `rules-code\implementation.md` | N/A | Full contents read (for inventory only). | Existence/discovery confirmed. Content not compared to AGENTS.md as part of this task (per constraints). |

**Limitations:** No existing artifact is a loader-injection log enumerating which specific rule files entered effective context per mode. Kilo’s session reports do not expose that diagnostic. The only direct runtime observation is AGENTS.md in the current context.

## 6) Cross-mode findings

- **AGENTS.md:** Present in effective context of current session (system attribution). No Ask-specific vs Code-specific artifact exists to confirm it appears in both modes; absence of contrary evidence means it’s plausible it appears in both, but not proven from existing artifacts.
- **rules/*.md (9 files):** Configured globally. No existing evidence shows they are loaded in Ask or in Code. Cannot conclude presence/absence in either mode.
- **rules-ask/teaching.md:** Configured globally via explicit glob. No evidence it appears in Ask effective context; no evidence it is excluded from Code. Documented directory-name scoping is the placement convention, but intersection with explicit global `instructions` glob is **unverified**. Cannot infer from mode alone.
- **rules-plan/planning.md:** Same as above. Explicit global ref; mode-scoping unverified.
- **rules-code/implementation.md:** Same. Explicit global ref; mode-scoping unverified. Even presence in Code mode is not established by existing artifacts.
- **Precedence:** No existing evidence establishes a concrete precedence question that can be resolved without additional experiments. **Precedence is unresolved.** Do not infer precedence from config order alone without loader behavior evidence.

## 7) Manual verification procedure (smallest safe; read-only)

Only perform this if you need to fill the UNKNOWNs. Do **not** modify config/files/Git state.

1. **Open fresh Ask session**: Start a new Kilo session in **Ask** mode (no repo edits). Record session ID from UI/title (e.g. `ses_*`). Do not switch modes during this check.
2. **Inspect effective instruction context (Ask)**: Use the actual supported diagnostic/UI capability exposed by this Kilo version (7.8.8). Look for: “Instructions from…” entries, “Loaded rules”, “System/Developer instructions”, or any panel that lists injected instruction sources with paths. If none exists, capture the closest available view (system message header, context panel) and note its limitations. **Do not invent commands/APIs.**
3. **Record evidence (Ask)**: Save a screenshot/text capture that shows mode, session ID, and the exact paths of every injected instruction source (AGENTS.md + each rule file). Record line numbers/excerpts from that capture. Do not paste secrets.
4. **Repeat independently in Code mode**: Open a fresh **Code** session. Repeat steps 1–3 with new session ID. Ensure you’re not reusing the Ask session context.
5. **Attribution**: For each captured item, tag (mode, session_id, source_path). Keep Ask and Code evidence separate. 
6. **Avoid confusion**: Treat “configured/discovered” separately from “loaded”. If the UI only shows a count or generic names, treat as insufficient and record the limitation. Do not assume mode-specific dirs are filtered just because of naming; require path-level evidence.

**If the application exposes no supported way to inspect effective instructions directly:** State that limitation and identify closest available evidence (system message attribution is the only direct item found). That is sufficient reason to mark Loaded/Applied as BLOCKED/NOT RUN.

## 8) Unresolved questions

- **Q1 (Loading):** Which of the 12 rule files are actually injected in Ask vs Code? **Answer: UNKNOWN.** No existing artifact enumerates them. Only AGENTS.md has direct runtime attribution.
- **Q2 (Mode scoping):** When files under `rules-ask/`, `rules-plan/`, `rules-code/` are explicitly referenced by global `instructions` globs, does the loader apply directory-name mode filtering? **Answer: UNVERIFIED.** Docs describe directory naming as placement convention and `instructions` entries as explicit; intersection not specified in consulted sources.
- **Q3 (Applied effect):** Do loaded rules influence behavior in either mode? **Answer: UNVERIFIED.** No behavioral evidence from existing artifacts.
- **Q4 (Cross-mode consistency):** Does AGENTS.md appear in both modes? **Answer: UNKNOWN from existing artifacts** (plausible but unproven).
- **Q5 (Precedence):** Is there a concrete precedence conflict to resolve? **Answer: NO.** No existing evidence justifies investigating precedence further.

**Distinction:** Q1–Q4 are negative findings (lack of evidence). Q2 is a true ambiguity in documented behavior.

## 9) Check summary (PASS/FAIL/BLOCKED/NOT RUN)

| Major investigation step | Status | Explanation |
|---------------------------|--------|-------------|
| 1. Establish config + source inventory (12 sources) | PASS | kilo.jsonc inspected; all 12 rule files + AGENTS.md paths resolved; existence confirmed. |
| 2. Inspect existing runtime evidence (S-0057/58/60, session artifacts) | PASS | Artifacts read; limitations identified. No loader-injection enumeration found. |
| 3. Evaluate Ask and Code independently using existing artifacts | BLOCKED | No existing artifact provides per-mode injected-file lists. Cannot complete without manual session inspection. |
| 4. Assess all sources separately (tables complete with statuses) | PASS | Tables produced with conservative statuses (UNKNOWN/BLOCKED). |
| 5. Investigate precedence only if justified | NOT RUN | No concrete precedence question established by existing evidence. |

## 10) Exactly one recommended next action

**Perform the smallest safe read-only manual verification:** start one fresh Ask session and one fresh Code session, and capture the loader’s reported effective instruction sources (exact file paths) for each using the supported UI/diagnostic in this Kilo version (7.8.8). This is the only way to resolve (a) which of the 12 rule files are loaded in each mode and (b) whether the global explicit references to `rules-ask/`, `rules-plan/`, `rules-code/` are mode-filtered. Do not change config, files, or Git state; do not create automated session switching. The evidence from those two captures will directly determine whether consolidation assumptions about mode scoping are valid.

---

