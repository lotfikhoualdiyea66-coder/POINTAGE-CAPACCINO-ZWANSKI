from __future__ import annotations


def compute_minutes_from_events(events: list[dict]) -> dict:
    sorted_events = sorted(events, key=lambda item: item["event_time"])
    total_minutes = 0
    total_break_minutes = 0
    clock_in = None
    break_start = None

    for event in sorted_events:
        event_type = str(event["event_type"]).upper()
        event_time = event["event_time"]

        if event_type == "CLOCK_IN":
            if clock_in is None and break_start is None:
                clock_in = event_time
            continue

        if event_type == "BREAK_START":
            if clock_in is not None and break_start is None:
                break_start = event_time
            continue

        if event_type == "BREAK_END":
            if break_start is not None:
                total_break_minutes += int((event_time - break_start).total_seconds() / 60)
                break_start = None
            continue

        if event_type == "CLOCK_OUT" and clock_in is not None:
            if break_start is not None:
                total_break_minutes += int((event_time - break_start).total_seconds() / 60)
                break_start = None
            elapsed_minutes = int((event_time - clock_in).total_seconds() / 60)
            worked_minutes = elapsed_minutes - total_break_minutes
            if total_break_minutes > 0:
                worked_minutes -= 90
            total_minutes += max(worked_minutes, 0)
            clock_in = None
            total_break_minutes = 0

    return {
        "worked_minutes": total_minutes,
        "break_minutes": total_break_minutes,
        "hours": total_minutes / 60,
    }
