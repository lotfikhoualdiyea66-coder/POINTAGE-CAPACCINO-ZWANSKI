from datetime import datetime

from app.services.timecalc import compute_minutes_from_events


def test_worked_hours_are_calculated():
    events = [
        {"event_type": "CLOCK_IN", "event_time": datetime(2025, 1, 10, 8, 0)},
        {"event_type": "BREAK_START", "event_time": datetime(2025, 1, 10, 12, 0)},
        {"event_type": "BREAK_END", "event_time": datetime(2025, 1, 10, 13, 0)},
        {"event_type": "CLOCK_OUT", "event_time": datetime(2025, 1, 10, 17, 0)},
    ]
    result = compute_minutes_from_events(events)
    assert result["worked_minutes"] == 480
    assert result["break_minutes"] == 60
    assert result["hours"] == 8.0
    assert result["is_clocked_in"] is False


def test_multiple_sessions_are_accumulated():
    events = [
        {"event_type": "CLOCK_IN", "event_time": datetime(2025, 1, 10, 8, 0)},
        {"event_type": "CLOCK_OUT", "event_time": datetime(2025, 1, 10, 12, 0)},
        {"event_type": "CLOCK_IN", "event_time": datetime(2025, 1, 10, 17, 0)},
        {"event_type": "CLOCK_OUT", "event_time": datetime(2025, 1, 10, 21, 0)},
    ]

    result = compute_minutes_from_events(events)

    assert result["worked_minutes"] == 480
    assert result["is_clocked_in"] is False


def test_open_shift_counts_work_until_as_of_time():
    events = [
        {"event_type": "CLOCK_IN", "event_time": datetime(2025, 1, 10, 8, 0)},
        {"event_type": "BREAK_START", "event_time": datetime(2025, 1, 10, 9, 30)},
    ]

    result = compute_minutes_from_events(events, as_of=datetime(2025, 1, 10, 10, 0))

    assert result["worked_minutes"] == 90
    assert result["break_minutes"] == 30
    assert result["is_clocked_in"] is True
    assert result["is_on_break"] is True
