# Deferred Tuning Model Ideas

These ideas are intentionally deferred until Suslog has reliable label capture and enough clean state history.

## Explicit Tuning Series

Do not add a `TuningSeries` table yet.

Possible future shape:

```kotlin
data class TuningSeries(
    val id: String,
    val configId: String,
    val baseStateId: Long,
    val axle: Axle,
    val adjusterLabel: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
)
```

Purpose:

- group a sweep such as Front Rebound or Rear Compression
- make fitting inputs cleaner
- show named experiments in Tuning

Reason deferred:

- runtime diff classification should prove the workflow first
- fewer database migrations while the model is still changing

## Quadratic Curve Fitting

Do not implement curve fitting until a config has several clean points for the same axis/adjuster.

Future rule of thumb:

- fewer than 3 distinct x values: show best observed only
- 3 to 4 distinct x values: optional low-confidence quadratic
- 5 or more distinct x values: curve plus confidence

Recommendation should remain "next test", not "final optimum".

## Confidence Model

Confidence should wait until repeated data exists.

Possible inputs:

- number of clean points
- spread of tested click range
- repeatability of score at same or nearby setup
- whether lap time exists
- whether mixed states interrupt the sweep

## Repeated Same-Setup Test Points

Current behavior updates the latest config state when setup is unchanged. Repeated same-setup testing may be useful later for noise estimation.

Possible future UI split:

- Update this test result
- Record new test point

Reason deferred:

- adds workflow complexity before labels and scoring are stable

## Persisted Change Metadata

Do not add these database columns yet:

- `parentStateId`
- `changedAxle`
- `changedAdjusterLabel`
- `deltaClicks`
- `changeType`

Runtime derivation from setup snapshots is enough for MVP. Persist only if recommendations and charts need stable historical classification.

## Global Or Multivariable Optimizer

Avoid a global suspension model for now.

Suslog should stay a config-level local coordinate optimizer:

- one fixed condition
- one axis
- one adjuster
- one next test

Global recommendations across cars, drivers, tire states, weather, or tracks are out of scope until the data model matures.
