# Tuning Quantification MVP Plan

Suslog should model tuning as local optimization inside one fixed setup config.

Working meaning:

- Config = one fixed condition, such as a track, surface condition, tire set, and car state.
- State = one tested setup snapshot inside that condition.
- Labels = driver-labeled observations for that state.
- Score = derived value computed from labels for ranking and charting.
- Recommendation = next test suggestion, not a final global optimum.

## Phase 1: Stable Labels And Score

Goal: collect enough clean driver feedback to make state history useful before adding fitting.

Add labels to each config state:

- `entryBalance`: -2..+2
- `midBalance`: -2..+2
- `exitBalance`: -2..+2
- `overallGrip`: 1..5
- `bodyControlBalance`: -2..+2
- `lapTimeMillis`: optional
- `note`: optional

Keep raw labels in the database. Do not persist score because the formula may change.

Initial computed score:

- grip: 45%
- entry/mid/exit balance: 40%
- body control: 15%

Balance weighting:

- entry: 40%
- mid: 20%
- exit: 40%

UI changes:

- Config feedback panel records all labels.
- Tuning history displays score per selected state.
- Tuning charts can show score trend before any curve fitting exists.

## Phase 2: Runtime Diff Validator

Goal: identify which states are clean enough for later modeling without blocking users from recording real notes.

For each config state, compare it with the previous state and classify the setup diff:

- `BASELINE`: no parent or no setup change.
- `AXLE_SINGLE_ADJUSTER`: front or rear axle changed together, same adjuster, same final left/right setting.
- `SINGLE_CORNER`: only one corner changed.
- `MIXED`: multiple adjusters, mixed corners, or inconsistent deltas/settings.
- `UNKNOWN`: cannot classify reliably.

MVP implementation should compute this at runtime from setup snapshots. Avoid adding database columns until the classification proves useful.

UI changes:

- Show the classified change in tuning history.
- Mark whether the state is included in model input.
- Mixed states remain visible in the timeline, but are excluded from tuning recommendations.

## Phase 3: Basic Recommendation Without Curve Fitting

Goal: give useful next-test guidance without pretending to know an optimum.

Use valid `AXLE_SINGLE_ADJUSTER` points to find:

- best observed state
- current selected state
- nearest clean direction tested
- suggested next test when data is sparse

Recommendation copy should stay conservative:

- "Best observed"
- "Try one more step"
- "Keep all other settings unchanged"
- "Low data confidence"

Do not claim global optimal setup.

Possible UI:

- Recommendation card in tuning history.
- Apply button sends the recommended state or next-test target to Setup as a recommendation.
- Setup still requires the user to manually adjust and save.

## Current UX Constraint

The Setup screen defaults to left/right axle lock enabled. When enabled, adjusting a corner adjuster sets the matching left/right adjuster on the same axle to the same target click.

This supports the one-axis/one-adjuster tuning protocol while still allowing the lock to be disabled for corner-specific work.
