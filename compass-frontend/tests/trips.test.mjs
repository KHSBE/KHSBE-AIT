import { test } from "node:test";
import assert from "node:assert/strict";
import {
  localDateKey,
  selectTrips,
  tripDateLabel,
  tripLength,
  tripPhase,
} from "../src/utils/trips.mjs";

const today = "2026-11-06";
const plan = (
  id,
  startDate,
  endDate = startDate,
  title = "여행",
  region = "서울",
) => ({ id, startDate, endDate, title, region });

test("departure and return dates both count as ongoing, not past", () => {
  assert.equal(tripPhase(plan(1, today, "2026-11-08"), today), "ongoing");
  assert.equal(tripPhase(plan(1, "2026-11-01", today), today), "ongoing");
  assert.equal(tripPhase(plan(1, today), today), "ongoing");
});
test("classifies future, past and invalid dates without guessing", () => {
  assert.equal(tripPhase(plan(1, "2026-11-07"), today), "upcoming");
  assert.equal(tripPhase(plan(1, "2026-11-05"), today), "past");
  for (const invalid of [null, "", "2026-02-30", "invalid"])
    assert.equal(tripPhase(plan(1, invalid), today), "undated");
  assert.equal(
    tripPhase(plan(1, "2026-11-08", "2026-11-07"), today),
    "undated",
  );
});
test("computes inclusive travel days across month, year and leap-day boundaries", () => {
  assert.equal(tripLength(plan(1, "2026-11-07", "2026-11-09")), "2박 3일");
  assert.equal(tripLength(plan(1, today)), "당일 여행");
  assert.equal(tripLength(plan(1, "2026-12-31", "2027-01-02")), "2박 3일");
  assert.equal(tripLength(plan(1, "2028-02-28", "2028-03-01")), "2박 3일");
  assert.equal(tripLength(plan(1, null)), "");
  assert.equal(tripLength(plan(1, "2026-11-09", "2026-11-07")), "");
});
test("formats dates safely and does not duplicate same-day dates", () => {
  assert.equal(tripDateLabel(plan(1, today)), "2026.11.06");
  assert.equal(tripDateLabel(plan(1, null)), "날짜 미정");
  assert.equal(
    tripDateLabel(plan(1, today, "2026-11-08")),
    "2026.11.06 ~ 2026.11.08",
  );
});
test("uses the local calendar date instead of UTC truncation", () => {
  assert.equal(localDateKey(new Date(2026, 0, 2, 0, 5)), "2026-01-02");
});
test("sorts ongoing, soonest upcoming, newest past, then undated without mutating input", () => {
  const plans = [
    plan(1, "2026-10-01"),
    plan(2, "2026-11-10"),
    plan(3, "2026-11-07"),
    plan(4, "2026-11-03", "2026-11-08"),
    plan(5, "2026-11-05"),
    plan(6, null),
  ];
  assert.deepEqual(
    selectTrips(plans, { today }).map((item) => item.id),
    [4, 3, 2, 5, 1, 6],
  );
  assert.deepEqual(
    plans.map((item) => item.id),
    [1, 2, 3, 4, 5, 6],
  );
});
test("combines case-insensitive title/region search with the selected period", () => {
  const plans = [
    plan(1, "2026-11-07", "2026-11-09", "OSAKA 혼자 여행", "오사카"),
    plan(2, "2026-10-01", "2026-10-03", "OSAKA 가족 여행", "오사카"),
    plan(3, today),
  ];
  assert.deepEqual(
    selectTrips(plans, { today, filter: "upcoming", query: " osaka " }).map(
      (item) => item.id,
    ),
    [1],
  );
  assert.deepEqual(
    selectTrips(plans, { today, filter: "past", query: "오사카" }).map(
      (item) => item.id,
    ),
    [2],
  );
  assert.equal(selectTrips(plans, { today, query: "없는 여행" }).length, 0);
});
