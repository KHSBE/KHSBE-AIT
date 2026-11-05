const DAY_MS = 86400000;

function parseDate(value) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value))
    return null;
  const date = new Date(`${value}T00:00:00Z`);
  return Number.isFinite(date.getTime()) &&
    date.toISOString().slice(0, 10) === value
    ? date
    : null;
}

export function localDateKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function tripPhase(plan, today = localDateKey()) {
  if (
    !parseDate(plan.startDate) ||
    !parseDate(plan.endDate) ||
    plan.endDate < plan.startDate
  )
    return "undated";
  if (plan.endDate < today) return "past";
  return plan.startDate > today ? "upcoming" : "ongoing";
}

export function tripLength(plan) {
  const start = parseDate(plan.startDate);
  const end = parseDate(plan.endDate);
  if (!start || !end || end < start) return "";
  const nights = Math.round((end - start) / DAY_MS);
  return nights === 0 ? "당일 여행" : `${nights}박 ${nights + 1}일`;
}

export function tripDateLabel(plan) {
  if (
    !parseDate(plan.startDate) ||
    !parseDate(plan.endDate) ||
    plan.endDate < plan.startDate
  )
    return "날짜 미정";
  const label = (value) => value.replaceAll("-", ".");
  return plan.startDate === plan.endDate
    ? label(plan.startDate)
    : `${label(plan.startDate)} ~ ${label(plan.endDate)}`;
}

export function selectTrips(
  plans,
  { filter = "all", query = "", today = localDateKey() } = {},
) {
  const keyword = query.trim().toLocaleLowerCase("ko");
  const priority = { ongoing: 0, upcoming: 1, past: 2, undated: 3 };
  return plans
    .filter((plan) => {
      const phase = tripPhase(plan, today);
      return (
        (filter === "all" ||
          (filter === "upcoming"
            ? ["upcoming", "ongoing"].includes(phase)
            : phase === "past")) &&
        `${plan.region || ""} ${plan.title || ""}`
          .toLocaleLowerCase("ko")
          .includes(keyword)
      );
    })
    .sort((a, b) => {
      const phaseA = tripPhase(a, today);
      const phaseB = tripPhase(b, today);
      if (phaseA !== phaseB) return priority[phaseA] - priority[phaseB];
      const byDate = (a.startDate || "").localeCompare(b.startDate || "");
      return (
        (phaseA === "past" ? -byDate : byDate) || Number(b.id) - Number(a.id)
      );
    });
}
