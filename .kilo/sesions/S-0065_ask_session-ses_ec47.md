# Diagnosing Kilo Code instruction loading

**Session ID:** ses_ec471a892ffe1gNe30sEzx9YMv
**Created:** 10/14/2026, 1:55:46 PM
**Updated:** 10/14/2026, 2:03:37 PM

---

## User

# Task: Identify a Supported Diagnostic for Effective Instruction Context in Kilo Code 7.8.8

**Kilo Mode:** Ask
**New Kilo Session:** YES

## OBJECTIVE

Determine whether the installed Kilo Code 7.8.8 extension provides a supported, evidence-backed way to inspect the effective instruction sources loaded into a particular session and active mode.

Find the least expensive reliable diagnostic method before requiring manual Ask/Code experiments.

## PRIOR EVIDENCE

Reuse these existing reports:

cheack: "C:\modding_mc\MobVariantsBRS\\.kilo\sesions"

- `.kilo/sesions/S-0057_ask_session-ses_ec4a.md`
- `.kilo/sesions/S-0060_plan_session-ses_ec49.md`
- `.kilo/sesions/S-0061_ask_session-ses_ec48.md`

Treat S-0061 as the latest investigation report.

Established findings:

- The 12 configured rule files have no direct per-mode runtime-loading evidence in the previously inspected artifacts.
- Configuration declarations and the presence of `AGENTS.md` in context do not establish that every configured rule's contents entered the effective instruction context.
- Previous configuration inventories and rule-content comparisons are out of scope.

Read the reports first. Reuse their findings and inspect original artifacts only when necessary to resolve a specific uncertainty.

## INSPECTION

1. Verify the installed extension version and identify the actual installation directory using available read-only evidence.
2. Inspect only the extension source, bundled documentation, command registrations, views, diagnostics, logging implementations, or runtime artifacts relevant to instruction-context inspection.
3. Search specifically for an implemented or documented mechanism that exposes:
   - Instruction source paths.
   - Evidence that source contents were loaded into the effective context, rather than merely discovered or configured.
   - Association with a particular session.
   - Association with the active Ask or Code mode.
4. Verify any proposed mechanism against its implementation or documentation applicable to the installed version. Record exact file paths, relevant symbols or command identifiers, and concise supporting excerpts where available.
5. If a candidate diagnostic exists, establish what it actually proves and how to access its output without modifying the installation or disturbing existing sessions.

Do not conduct another general configuration inventory. Expand inspection only when specific evidence identifies a plausible diagnostic mechanism.

## EVIDENCE STANDARD

Distinguish these states explicitly:

1. Configured: a source is declared in configuration.
2. Discovered: the application can locate or enumerate a source.
3. Loaded: the source contents are incorporated into a session's effective instruction context.
4. Applied: the contents influence the instructions used by the model.

Do not treat these states as interchangeable.

A diagnostic that lists configured files is insufficient to establish runtime loading. A prompt transcript containing instruction-like text is insufficient to identify its original source unless attribution is independently supported.

Do not infer capabilities from similarly named features in other versions.

## CONSTRAINTS

- Strictly read-only.
- Do not create, switch, or terminate Kilo sessions.
- Do not edit configuration, rules, extension files, permissions, or project files.
- Do not install, update, or disable extensions.
- Do not launch Minecraft, run builds, or execute unrelated tests.
- Do not repeat completed investigations without identifying the specific unresolved question the new inspection will answer.
- Do not invent commands, UI labels, APIs, log locations, implementation details, or results.
- If a required artifact is inaccessible, report the limitation instead of assuming its contents.

## MANUAL EVIDENCE COLLECTION

If no supported diagnostic is verified, provide the smallest practical, attributable Ask/Code test procedure.

The procedure must:

- Preserve existing sessions and configuration.
- Identify the extension version, session identity, active mode, test time, and relevant workspace.
- Capture the minimum available evidence needed to distinguish configured sources from sources demonstrably included in effective context.
- Explain how the user can save or retrieve the evidence using existing supported facilities.
- State explicitly when source attribution or runtime loading cannot be established.

Do not ask the user to perform these tests during this investigation.

## REPORT

Produce a concise Markdown report containing:

1. **Version verification:** exact installed version and supporting evidence.
2. **Evidence inspected:** exact paths and relevant implementation or documentation references.
3. **Diagnostic findings:** verified capabilities, how to access them, and what each capability proves.
4. **Conclusion:** either identify a supported diagnostic or explicitly state that none was verified.
5. **Fallback procedure:** minimal manual Ask/Code evidence collection, if feasible.
6. **Limitations:** unresolved uncertainties and unavailable evidence.
7. **Recommended next action:** exactly one action, selected according to the strongest available evidence.

Use `VERIFIED`, `NOT VERIFIED`, or `BLOCKED` for individual findings where appropriate. Distinguish a confirmed absence in inspected implementation from a failure to locate a capability.

Do not implement changes or initiate further investigations. Stop after delivering the report.

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

Now I have a thorough understanding of the prior investigations. Let me inspect the installed Kilo Code 7.8.8 extension for any diagnostic mechanisms.


**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: bash**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: grep**

---

## Assistant

**Tool: grep**

---

## Assistant

**Tool: grep**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: bash**

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

**Tool: websearch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: grep**

---

## Assistant

**Tool: skill**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: read**

---

## Assistant

Now I have a comprehensive understanding. Let me compile the final report.


**Tool: bash**

---

## Assistant

**Tool: read**

---

## Assistant

Now I have all the evidence needed. Let me produce the final report.

---

# Diagnostic Report: Supported Instruction Context Inspection in Kilo Code 7.8.8

## 1. Version Verification

**Installed Version:** 7.8.8  
**Evidence:** Extension manifest at `C:\Users\PC\.vscode\extensions\kilocode.kilo-code-7.8.8-win32-x64\package.json`, line 5: `"version": "7.8.8"`  
**Installation Directory:** `C:\Users\PC\.vscode\extensions\kilocode.kilo-code-7.8.8-win32-x64\`  
**Repository:** `https://github.com/Kilo-Org/kilocode.git` (directory `packages/kilo-vscode`)  
**VS Code Engine Requirement:** `^1.105.1`  
**Status:** **VERIFIED**

---

## 2. Evidence Inspected

| Artifact | Path / Source | Type |
|----------|---------------|------|
| Extension manifest | `C:\Users\PC\.vscode\extensions\kilocode.kilo-code-7.8.8-win32-x64\package.json` | Local metadata |
| Extension commands | `package.json` → `contributes.commands` (493 commands) | Command registry |
| Bundled documentation | `docs/agent-behaviour/rules-workflows-subtab-parity.md` | Implementation evidence |
| Bundled documentation | `docs/agent-behaviour/modes-subtab-parity.md` | Feature parity tracking |
| Infrastructure docs | `docs/infrastructure/dedicated-output-channel.md` | Logging architecture |
| Backend source (GitHub) | `packages/opencode/src/session/instruction.ts` | Instruction loading implementation |
| Official docs (current) | `kilo.ai/docs/customize/custom-instructions`, `custom-rules` | Public documentation |
| Prior session reports | `.kilo/sesions/S-0057_ask_session-ses_ec4a.md`, `S-0060_plan_session-ses_ec49.md`, `S-0061_ask_session-ses_ec48.md` | Previous investigations |

**Note on documentation version match:** The official docs describe the v7 platform generally. No version-pinned snapshot for 7.8.8 was located. The changelog's 7.8.8 entry is consistent with v7 platform docs, so matching is *reasonable but not version-certified*.

---

## 3. Diagnostic Findings

### 3.1 Registered VS Code Commands (package.json)

The extension registers **493 commands** under `kilo-code.new.*`. None relate to instruction context inspection:

- Session management: `new.plusButtonClicked`, `new.closeTask`, `new.historyButtonClicked`, `new.agentManagerOpen`
- Settings: `new.settingsButtonClicked`, `new.settingsSearch`, `new.reload` ("Reload Config and Skills")
- Agent Manager: 50+ commands for worktrees, sessions, terminals
- Code actions: `explainCode`, `fixCode`, `improveCode`, `addToContext`
- **No command** named `inspect`, `diagnose`, `showInstructions`, `listLoadedRules`, `instructionContext`, or similar.

**Status:** **NOT VERIFIED** — No command exists for instruction context inspection.

### 3.2 UI Views and Panels

- **Sidebar Webview** (`kilo-code.SidebarProvider`): Primary chat interface
- **Tab/Editor Panel** (`kilo-code.TabPanelProvider`): Full-tab chat
- **Agent Manager Panel**: Multi-agent orchestration
- **Settings Webview**: Includes **Rules sub-tab** (shows configured `config.instructions` paths only)

From `rules-workflows-subtab-parity.md`:
> "The new extension treats instruction files as explicit config entries — you add paths to `config.instructions` and they're **always active**. There's **no discovery or toggling**."

The Rules sub-tab shows a **flat list of explicitly configured instruction file paths** from `config.instructions`. It does **not** show:
- Which files were actually loaded into the current session
- Which files are active for the current mode (Ask/Code/Plan)
- Runtime loading errors or skipped files
- Per-mode filtering status

**Status:** **NOT VERIFIED** — UI shows configured paths only, not effective loaded context.

### 3.3 Logging and Output Channels

From `dedicated-output-channel.md`:
> "Agent Manager has its own output channel. No general 'Kilo Code' output channel exists."
> "Remaining Work: Create `vscode.window.createOutputChannel('Kilo Code')`... Route all `[Kilo New]` log messages to this channel"

**Status:** **NOT VERIFIED** — No general output channel exists; `[Kilo New]` logs go to console only. No instruction-loading logs are documented or exposed.

### 3.4 Backend Instruction Loading Implementation (instruction.ts)

The CLI backend (`packages/opencode/src/session/instruction.ts`) implements:

| Function | Purpose | Exposed to VS Code? |
|----------|---------|---------------------|
| `systemSources()` | Returns `Map<filepath, Source>` for all discovered instruction files with provenance | **No** |
| `systemPaths()` | Returns `Set<filepath>` of all instruction file paths | **No** |
| `system()` | Returns `string[]` of formatted `"Instructions from: {filepath}\n{content}"` blocks | **No** (used internally for system prompt) |
| `loaded(messages)` | Extracts instruction file paths from tool call metadata in session history | **No** |
| `resolve()` | Attaches nearby `AGENTS.md` files when reading files | **No** |

The `system()` function produces exactly the diagnostic output needed: **file paths with attribution and full content**. However, this is **internal to the CLI** and not exposed via any:
- VS Code command
- RPC endpoint
- CLI slash command (TUI has `/status` but not instruction dump)
- Output channel

**Status:** **CAPABILITY EXISTS IN BACKEND BUT NOT EXPOSED** — The diagnostic data is generated internally but no supported facility surfaces it.

### 3.5 Documentation Claims vs. Reality

| Claim | Source | Verification |
|-------|--------|--------------|
| "Inspect the agent's prompts and context" | Anaconda marketing page | **Marketing language** — no such feature in extension |
| "Settings → Agent Behaviour → Rules sub-tab" manages rules | `custom-rules.md`, `custom-instructions.md` | **VERIFIED** — Shows configured paths only |
| "System prompt preview" button | `modes-subtab-parity.md` | **NOT AVAILABLE** — Listed as P2 remaining work: "requires a CLI endpoint to return the assembled prompt for an agent" |
| "Preview System Prompt" in legacy | `modes-subtab-parity.md` comparison table | **NOT AVAILABLE** in new extension |

**Status:** **NO SUPPORTED DIAGNOSTIC DOCUMENTED OR IMPLEMENTED** in the VS Code extension.

---

## 4. Conclusion

**No supported, evidence-backed diagnostic exists in Kilo Code 7.8.8 (VS Code extension) to inspect the effective instruction sources loaded into a particular session and active mode.**

| Diagnostic Need | Supported? | Evidence |
|-----------------|------------|----------|
| List instruction source paths loaded in current session | **NO** | No command, UI, log, or RPC exposes this |
| Show which configured sources were actually loaded (vs. merely configured) | **NO** | Rules sub-tab shows configured paths only |
| Associate loaded sources with active mode (Ask/Code/Plan) | **NO** | No mode-aware instruction inspection |
| Attribute instruction content to original source file | **NO** | Backend generates this (`system()`) but not exposed |
| Access loader errors or skipped files | **NO** | No logging channel; silent skip for URL failures |

The backend CLI *generates* the required diagnostic data (`system()` returns attributed instruction blocks), but **no supported facility in the VS Code extension surfaces this data**.

---

## 5. Fallback Procedure: Minimal Manual Ask/Code Evidence Collection

Since no supported diagnostic exists, the following read-only manual procedure can collect attributable evidence:

### Prerequisites
- Preserve all existing sessions, configuration, and repository state
- Do not modify any files or settings

### Procedure

#### For Ask Mode
1. **Open fresh Ask session**: Start a new Kilo session in **Ask** mode (via sidebar "New Task" → select Ask). Record the **session ID** from the UI (format: `ses_*`).
2. **Trigger instruction attribution**: Send a minimal prompt that forces the system prompt to be echoed or referenced, e.g.:
   > "What are the first 50 words of your system instructions?"
3. **Capture evidence**: Copy the assistant's response. Look for `"Instructions from: <filepath>"` attribution blocks (the backend `system()` function prefixes each instruction file's content with this format).
4. **Record metadata**: Note timestamp, session ID, active mode (Ask), workspace root (`C:\modding_mc\MobVariantsBRS`).

#### For Code Mode
5. **Repeat independently**: Open a fresh **Code** mode session (new session, select Code). Record new **session ID**.
6. **Trigger instruction attribution**: Send the same minimal prompt.
7. **Capture evidence**: Copy response with attribution blocks.
8. **Record metadata**: Timestamp, session ID, active mode (Code), workspace root.

### Evidence Analysis
- **Each "Instructions from: <filepath>" block** = **VERIFIED LOADED** for that session/mode
- **Files in `config.instructions` NOT appearing** = **NOT LOADED** (or silently skipped)
- **Compare Ask vs. Code**: Determines whether mode-specific globs (`rules-ask/`, `rules-code/`) are mode-filtered despite explicit global configuration

### Limitations of This Procedure
- **Cannot distinguish** "loaded but not echoed" from "not loaded" — only proves presence when attribution appears
- **Cannot detect** instructions injected via per-directory `AGENTS.md` discovery (dynamic, triggered by Read tool)
- **Relies on model cooperation** — model may summarize rather than quote verbatim
- **No access to loader-internal errors** (e.g., glob matched 0 files, URL fetch timeout)

---

## 6. Limitations and Unresolved Uncertainties

| Uncertainty | Status | Impact |
|-------------|--------|--------|
| Whether the 12 configured rule files are actually injected in Ask mode | **UNVERIFIED** | Blocks consolidation decision |
| Whether the 12 configured rule files are actually injected in Code mode | **UNVERIFIED** | Blocks consolidation decision |
| Whether explicit global `instructions` globs bypass mode-specific directory filtering | **UNVERIFIED** (parity doc says "always active" but no runtime proof) | Core architectural question |
| Whether `AGENTS.md` (global) is loaded in both modes | **PARTIAL** — Current session shows it loaded, but not proven for Code mode | Affects baseline |
| Loader errors (missing files, URL timeouts, glob matches) | **BLOCKED** — No logging channel | Silent failures possible |
| Instruction precedence order (global vs. project vs. agent prompt) | **DOCUMENTED** but not empirically verified for this config | Affects conflict resolution |

**Distinction:** "Not verified" = no evidence either way; "Blocked" = required access (logging channel, CLI endpoint) does not exist in installed version.

---

## 7. Recommended Next Action

**Perform the minimal manual Ask/Code evidence collection procedure (Section 5).**

**Justification:**
- This is the **only available method** to obtain attributable evidence of which instruction files are loaded in each mode
- Resolves the principal uncertainty gating the consolidation decision (mode-specific rule behavior)
- Requires no configuration changes, file edits, or session disruption
- Takes ~5 minutes per mode using existing UI
- Produces evidence that can be saved as text and compared directly

**Do not** wait for a supported diagnostic — none exists in 7.8.8, and the parity doc confirms "System prompt preview" is future work (P2) requiring a new CLI endpoint.

---

**Report Complete.**

---

