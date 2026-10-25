from __future__ import annotations

import time
from threading import Lock


class AttemptLimiter:
    """In-memory failed-attempt counter; a single LAN server process is the deployment target."""

    def __init__(self, max_failures: int, lock_seconds: int):
        self.max_failures = max_failures
        self.lock_seconds = lock_seconds
        self._failures: dict[str, tuple[int, float]] = {}
        self._lock = Lock()

    def is_locked(self, key: str) -> bool:
        with self._lock:
            count, locked_until = self._failures.get(key, (0, 0.0))
            if locked_until and time.monotonic() >= locked_until:
                self._failures.pop(key, None)
                return False
            return count >= self.max_failures

    def fail(self, key: str) -> None:
        with self._lock:
            count, _ = self._failures.get(key, (0, 0.0))
            count += 1
            locked_until = time.monotonic() + self.lock_seconds if count >= self.max_failures else 0.0
            self._failures[key] = (count, locked_until)

    def reset(self, key: str) -> None:
        with self._lock:
            self._failures.pop(key, None)

    def clear(self) -> None:
        with self._lock:
            self._failures.clear()
