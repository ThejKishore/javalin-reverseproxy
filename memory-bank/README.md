# Memory Bank Index

Welcome to the Javalin Gateway Memory Bank — persistent context for AI-assisted development using Kiro-Lite.

## Quick Navigation

### 📋 Get Started Here
1. **[projectbrief.md](./projectbrief.md)** — Project identity, mission, scope
2. **[productContext.md](./productContext.md)** — Business goals, user stories, market positioning
3. **[systemPatterns.md](./systemPatterns.md)** — Architecture patterns, design decisions, data models
4. **[techContext.md](./techContext.md)** — Tech stack, file structure, build system
5. **[activeContext.md](./activeContext.md)** — What we know right now, common pitfalls, code locations
6. **[progress.md](./progress.md)** — Completed phases, roadmap, metrics
7. **[copilot-rules.md](./copilot-rules.md)** — Coding standards, testing patterns, review checklist

### 🎯 By Use Case

**I'm starting a new feature…**
→ Follow Kiro-Lite workflow in `copilot-rules.md` → Create feature folder with `/start feature <name>`

**I'm adding a new filter…**
→ Read `systemPatterns.md` (Filter Chain Pattern) → See `techContext.md` (File paths) → Check `copilot-rules.md` (Testing patterns)

**I'm confused about architecture…**
→ Start with `systemPatterns.md` → Refer to `techContext.md` for code locations

**I broke something or don't understand an error…**
→ Check `activeContext.md` (Common Pitfalls) → Review test examples in `copilot-rules.md`

**Where does my code go?**
→ See `techContext.md` → Directory Structure section

---

## Memory Bank Structure

```
memory-bank/
├── README.md                    # This file - navigation & overview
├── projectbrief.md              # Project identity & mission
├── productContext.md            # Business goals & user stories
├── systemPatterns.md            # Architecture patterns & design decisions
├── techContext.md               # Tech stack & implementation details
├── activeContext.md             # Current state, pitfalls, code locations
├── progress.md                  # Completed work & roadmap
├── copilot-rules.md             # Coding standards & best practices
└── examples/                    # Example feature workflows
    └── [feature-name]/
        ├── prd.md               # Product requirement document
        ├── design.md            # Architecture & design decisions
        ├── tasks.md             # Task breakdown with acceptance criteria
        └── context.md           # Feature-specific context
```

### Active Feature Memories
- [`jwt-csrf-csp-http-validation-filters/prd.md`](./jwt-csrf-csp-http-validation-filters/prd.md)
- [`jwt-csrf-csp-http-validation-filters/design.md`](./jwt-csrf-csp-http-validation-filters/design.md)
- [`jwt-csrf-csp-http-validation-filters/tasks.md`](./jwt-csrf-csp-http-validation-filters/tasks.md)
- [`jwt-csrf-csp-http-validation-filters/context.md`](./jwt-csrf-csp-http-validation-filters/context.md)

---

## Kiro-Lite Workflow Commands

When creating a new feature, follow this command sequence:

### Step 1: Initialize Feature
```
/start feature <feature-name>
```
Creates `/memory-bank/<feature-name>/` folder with `prd.md`, `design.md`, `tasks.md`, `context.md` templates.

### Step 2: Design Phase
After clarifying requirements with stakeholders:
```
/approve prd
```
Generates `design.md` with architecture, tech decisions, data models.

### Step 3: Task Breakdown
After design review:
```
/approve design
```
Generates `tasks.md` with atomic, testable task units (S/M/L effort estimates).

### Step 4: Implementation
After task review:
```
/approve tasks
```
Ready to implement. Then:
```
/implement <TASK_ID>
```
Implement one task. Show diffs. Finish with `/review complete`. Repeat for next task.

### Step 5: Memory Update
After all tasks done:
```
/update memory bank
```
Refresh `activeContext.md`, `progress.md`, `copilot-rules.md` with any new learnings.

---

## File Descriptions

| File | Size | Audience | When to Read |
|---|---|---|---|
| **projectbrief.md** | ~1KB | Everyone | First, to understand project mission |
| **productContext.md** | ~2KB | Product owners, architects | Business goals & user stories |
| **systemPatterns.md** | ~4KB | Architects, senior developers | Design patterns & architecture |
| **techContext.md** | ~5KB | All developers | File locations, stack, schema |
| **activeContext.md** | ~4KB | All developers | Before starting a task |
| **progress.md** | ~3KB | Everyone | Understand what's done & roadmap |
| **copilot-rules.md** | ~6KB | All developers | Code style, testing, review checklist |

---

## How to Update Memory Bank

After significant work (e.g., completing a feature), update relevant files:

1. **Did you add a new pattern?** → Update `systemPatterns.md`
2. **Did you discover a pitfall?** → Add to `activeContext.md` (Common Pitfalls section)
3. **Did you complete a phase?** → Update `progress.md` (Completed Phases)
4. **Did you learn new testing patterns?** → Update `copilot-rules.md`
5. **Did architecture change?** → Update `techContext.md` (Directory Structure)

**Best practice**: After `/update memory bank` command, review changes before committing.

---

## Integration with Copilot Instructions

This memory bank is referenced in `.github/copilot-instructions.md`. Before starting ANY task:

1. Read the **Core Instructions** in copilot-instructions.md
2. Follow **Memory Bank Priority** order (projectbrief → productContext → systemPatterns → techContext → activeContext)
3. Use **Kiro-Lite slash commands** to structure feature work
4. Reference **copilot-rules.md** for coding standards
5. Update memory bank when learnings emerge

---

## Commit Your Changes

Build memory bank into version control:
```bash
git add memory-bank/
git commit -m "docs: init Kiro-Lite memory bank for AI-assisted development

- projectbrief.md: Project identity & mission
- productContext.md: Business goals & user stories
- systemPatterns.md: Architecture patterns & design decisions
- techContext.md: Tech stack, file structure, build system
- activeContext.md: Current state, common pitfalls, code locations
- progress.md: Completed phases & roadmap
- copilot-rules.md: Coding standards & best practices
- README.md: Navigation & index

This memory bank enables structured feature development via Kiro-Lite
workflow and persistent AI context across sessions."
```

---

## Next Steps

✅ Memory bank is ready for use.

**To start a new feature:**
```
/start feature my-feature-name
```

**To work on an existing feature:**
- Check if `/memory-bank/<feature-name>/` exists
- Read `<feature-name>/prd.md` to understand scope
- Implement using Kiro-Lite workflow

**To contribute to the project:**
1. Read this README
2. Understand current state in `progress.md`
3. Check `activeContext.md` for known pitfalls
4. Follow coding standards in `copilot-rules.md`
5. Update memory bank when you learn something new

---

**Memory Bank Last Updated**: May 24, 2026  
**Project Status**: Production deployment ready  
**Kiro-Lite Enabled**: Yes ✅

