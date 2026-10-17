from __future__ import annotations

from datetime import datetime


def compute_minutes_from_events(events: list[dict], as_of: datetime | None = None) -> dict:
    sorted_events = sorted(events, key=lambda item: item["event_time"])
    worked_seconds = 0.0
    break_seconds = 0.0
    work_segment_start = None
    break_start = None
    is_clocked_in = False

    for event in sorted_events:
        event_type = str(event["event_type"]).upper()
        event_time = event["event_time"]

        if event_type == "CLOCK_IN":
            if not is_clocked_in:
                is_clocked_in = True
                work_segment_start = event_time
            continue

        if event_type == "BREAK_START":
            if is_clocked_in and break_start is None and work_segment_start is not None:
                worked_seconds += max(0.0, (event_time - work_segment_start).total_seconds())
                work_segment_start = None
                break_start = event_time
            continue

        if event_type == "BREAK_END":
            if break_start is not None:
                break_seconds += max(0.0, (event_time - break_start).total_seconds())
                break_start = None
                work_segment_start = event_time
            continue

        if event_type == "CLOCK_OUT" and is_clocked_in:
            if break_start is not None:
                break_seconds += max(0.0, (event_time - break_start).total_seconds())
                break_start = None
            elif work_segment_start is not None:
                worked_seconds += max(0.0, (event_time - work_segment_start).total_seconds())
            work_segment_start = None
            is_clocked_in = False

    is_on_break = is_clocked_in and break_start is not None
    if as_of is not None and is_clocked_in:
        if break_start is not None:
            break_seconds += max(0.0, (as_of - break_start).total_seconds())
        elif work_segment_start is not None:
            worked_seconds += max(0.0, (as_of - work_segment_start).total_seconds())

    total_worked_minutes = int(worked_seconds // 60)
    total_break_minutes = int(break_seconds // 60)

    return {
        "worked_minutes": total_worked_minutes,
        "break_minutes": total_break_minutes,
        "hours": total_worked_minutes / 60,
        "is_clocked_in": is_clocked_in,
        "is_on_break": is_on_break,
    }
