# Domain Ownership Plan

Date: 2026-08-26
Owner: PM (you)
Goal: Clarify specs/requirements and verify-fix code per domain with 6 people (including 1 General Spec owner)

## 1) Team split (6 people)

### Person 1 - General Spec Owner (Spec Governor)
- Scope specs:
  - specs/General Spec.md
  - Cross-check all feature specs under specs/*/spec.md
- Scope code:
  - No single-code ownership; reviews cross-domain consistency
- Main responsibilities:
  - Normalize requirement wording (EARS format consistency)
  - Enforce global decisions from General Spec and AGENTS.md
  - Track cross-domain dependency/conflicts
  - Final approve on requirement interpretation before code fixes are merged

### Person 2 - Identity & Profile Domain
- Scope specs:
  - specs/007-core-auth/spec.md
  - specs/001-profile-history/spec.md
- Scope code:
  - backend/src/main/java/com/workoutsmart/auth
  - backend/src/main/java/com/workoutsmart/profile
  - web/src/pages/auth
  - web/src/pages/profile
  - web/src/context
  - web/src/services/authApi.ts
  - web/src/services/profileApi.ts

### Person 3 - Nutrition & Stats Domain
- Scope specs:
  - specs/002-nutrition-tracking/spec.md
  - specs/005-stats-reports/spec.md
- Scope code:
  - backend/src/main/java/com/workoutsmart/nutrition
  - backend/src/main/java/com/workoutsmart/stats
  - web/src/pages/nutrition
  - web/src/pages/stats
  - web/src/services/nutritionApi.ts
  - web/src/services/statsApi.ts

### Person 4 - Social & Admin Domain
- Scope specs:
  - specs/003-social-community/spec.md
  - specs/006-admin-management/spec.md
- Scope code:
  - backend/src/main/java/com/workoutsmart/social
  - backend/src/main/java/com/workoutsmart/feed
  - backend/src/main/java/com/workoutsmart/admin
  - web/src/pages/social
  - web/src/pages/admin
  - web/src/services/socialApi.ts
  - web/src/services/feedApi.ts
  - web/src/services/adminApi.ts

### Person 5 - Planning & Exercise Library Domain
- Scope specs:
  - specs/008-workout-plan/spec.md
  - specs/011-custom-exercise/spec.md
  - specs/012-exercise-library-filters/spec.md
  - specs/013-manual-workout-builder/spec.md
- Scope code:
  - backend/src/main/java/com/workoutsmart/plan
  - backend/src/main/java/com/workoutsmart/exercise
  - web/src/pages/plan
  - web/src/components/ExerciseBrowser.tsx
  - web/src/services/planApi.ts

### Person 6 - Workout Execution Runtime Domain
- Scope specs:
  - specs/009-workout-tracking/spec.md
  - specs/010-uiux-polish/spec.md
  - specs/014-advanced-sets/spec.md
  - specs/015-time-based-exercises/spec.md
  - specs/016-workout-execution-mode/spec.md
  - specs/017-workout-music/spec.md
- Scope code:
  - backend/src/main/java/com/workoutsmart/tracking
  - backend/src/main/java/com/workoutsmart/common/SetType.java
  - web/src/pages/training
  - web/src/pages/training/execution
  - web/src/pages/training/music
  - web/src/services/trackingApi.ts

## 2) Current status snapshot (from tasks.md)

- 001-profile-history: done=13, open=3
- 002-nutrition-tracking: done=11, open=2
- 003-social-community: done=11, open=2
- 005-stats-reports: done=1, open=16 (highest risk)
- 006-admin-management: done=7, open=0
- 007-core-auth: done=41, open=5 (mostly deferred mobile)
- 008-workout-plan: done=45, open=0
- 009-workout-tracking: done=5, open=0
- 010-uiux-polish: done=17, open=0
- 011-custom-exercise: no tasks.md (spec exists, execution checklist gap)
- 012-exercise-library-filters: done=11, open=0
- 013-manual-workout-builder: done=11, open=0
- 014-advanced-sets: done=20, open=0
- 015-time-based-exercises: done=10, open=0
- 016-workout-execution-mode: done=14, open=0
- 017-workout-music: done=14, open=0

## 3) PM priority order (now)

1. Clarify requirements gaps and status labels for Draft specs.
2. Close domain with biggest open work: 005-stats-reports.
3. Fill execution artifact gap for 011-custom-exercise (tasks/plan alignment).
4. Run domain-by-domain code verification and fix issues.

## 4) Definition of Done per domain

A domain is DONE only when all are true:
- Spec text is unambiguous for all FRs and acceptance scenarios.
- tasks.md exists and maps to current code reality.
- Backend build passes and relevant tests pass.
- Frontend build passes and relevant tests pass.
- API contract and UI behavior match spec acceptance criteria.
- Any cross-domain dependency is confirmed by General Spec Owner.

## 5) Work protocol (PM + 6 people)

- Daily cycle (short):
  - Each domain owner posts: requirement clarifications, code defects found, fixes merged, blockers.
- General Spec Owner validates:
  - Requirement consistency across domains.
  - No contradiction with General Spec and fixed architecture decisions.
- PM actions:
  - Resolve priority/blocker decisions.
  - Rebalance workload when one domain exceeds capacity.

## 6) Immediate execution checklist

### PM + Person 1 (today)
- Review and normalize Draft spec statuses.
- Create/align missing execution artifacts for 011-custom-exercise.
- Publish a shared requirement clarification log (open questions + decisions).

### Person 3 (today)
- Start with 005-stats-reports open tasks.
- Confirm actual code-vs-task mismatch and update tasks/spec accordingly.

### Person 2, 4, 5, 6 (today)
- Run focused verification by domain.
- Log compile/test/lint issues and map each issue to FR/acceptance criterion.
- Submit small, isolated fixes per issue cluster.

## 7) Known environment blockers for verification

- Backend compile command currently blocked by missing JAVA_HOME in terminal environment.
- Frontend build command currently blocked by missing TypeScript runtime ("tsc not recognized").

PM should unblock environment first (JDK and web toolchain), then enforce domain verification pipeline.
