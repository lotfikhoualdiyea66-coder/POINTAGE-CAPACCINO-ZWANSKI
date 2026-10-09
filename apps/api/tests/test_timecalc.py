from datetime import datetime

from app.services.timecalc import compute_minutes_from_events


def test_worked_hours_are_calculated():
    events = [
        {"event_type": "CLOCK_IN", "event_time": datetime(2025, 1, 10, 8, 0)},
        {"event_type": "BREAK_START", "event_time": datetime(2025, 1, 10, 12, 0)},
        {"event_type": "BREAK_END", "event_time": datetime(2025, 1, 10, 12, 30)},
        {"event_type": "CLOCK_OUT", "event_time": datetime(2025, 1, 10, 17, 0)},
    ]
    result = compute_minutes_from_events(events)
    assert result["worked_minutes"] == 420
    assert result["hours"] == 7.0
