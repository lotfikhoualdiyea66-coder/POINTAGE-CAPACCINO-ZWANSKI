from __future__ import annotations

VALID_TRANSITIONS = {
    "clocked_out": {"CLOCK_IN"},
    "clocked_in": {"CLOCK_OUT", "BREAK_START"},
    "on_break": {"BREAK_END"},
}


def validate_transition(current_state: str, requested_type: str) -> tuple[bool, str]:
    allowed = VALID_TRANSITIONS.get(current_state, set())
    if requested_type in allowed:
        return True, "ok"
    return False, f"Invalid transition from {current_state} to {requested_type}."


def state_from_events(events: list[dict]) -> str:
    if not events:
        return "clocked_out"

    current_state = "clocked_out"
    clock_in_time = None
    break_start_time = None

    for event in sorted(events, key=lambda item: item["event_time"]):
        event_type = str(event["event_type"]).upper()
        event_time = event["event_time"]

        if event_type == "CLOCK_IN":
            clock_in_time = event_time
            break_start_time = None
            current_state = "clocked_in"
        elif event_type == "BREAK_START":
            if clock_in_time is not None:
                break_start_time = event_time
                current_state = "on_break"
        elif event_type == "BREAK_END":
            if clock_in_time is not None:
                break_start_time = None
                current_state = "clocked_in"
        elif event_type == "CLOCK_OUT":
            clock_in_time = None
            break_start_time = None
            current_state = "clocked_out"

    return current_state
