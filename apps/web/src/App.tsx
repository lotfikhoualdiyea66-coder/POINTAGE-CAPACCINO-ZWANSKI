import { useEffect, useState } from "react";

const apiBase = "http://127.0.0.1:8000";

type Employee = {
  id: string;
  employee_code: string;
  full_name: string;
  start_date: string;
  active: boolean;
  enrollment_state: string;
};

type AttendanceEvent = {
  id: string;
  employee_id: string;
  event_type: string;
  event_time: string;
  status: string;
  source: string;
};

const eventLabels: Record<string, string> = {
  CLOCK_IN: "Arrivée",
  CLOCK_OUT: "Départ",
  BREAK_START: "Début de pause",
  BREAK_END: "Fin de pause",
};

export default function App() {
  const [health, setHealth] = useState("Checking");
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [events, setEvents] = useState<AttendanceEvent[]>([]);

  useEffect(() => {
    const loadData = async () => {
      try {
        const healthRes = await fetch(`${apiBase}/api/v1/health`);
        const healthData = await healthRes.json();
        setHealth(healthData.status ?? "offline");
      } catch {
        setHealth("offline");
      }

      try {
        const [employeesRes, attendanceRes] = await Promise.all([
          fetch(`${apiBase}/api/v1/employees`),
          fetch(`${apiBase}/api/v1/attendance`),
        ]);

        const employeeData = await employeesRes.json();
        const attendanceData = await attendanceRes.json();

        setEmployees(Array.isArray(employeeData) ? employeeData : []);
        setEvents(Array.isArray(attendanceData) ? attendanceData : []);
      } catch {
        setEmployees([]);
        setEvents([]);
      }
    };

    loadData();
  }, []);

  const presentCount = events.filter((event) => event.event_type === "CLOCK_IN").length;

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-wrap">
          <img src="/branding/cappuccino-logo.png" alt="Cappuccino Lac 1" className="brand-logo" />
          <div>
            <p className="eyebrow">Zwanski Vulzero</p>
            <h1>Cappuccino Lac 1</h1>
          </div>
        </div>
        <div className={`status-pill ${health === "ok" ? "ok" : "warning"}`}>
          {health === "ok" ? "Serveur en ligne" : "Serveur hors ligne"}
        </div>
      </header>

      <main className="main-grid">
        <section className="card panel-wide">
          <h2>Vue d’ensemble</h2>
          <div className="metric-grid">
            <div className="metric-box">
              <span>Employés</span>
              <strong>{employees.length}</strong>
            </div>
            <div className="metric-box">
              <span>Présents</span>
              <strong>{presentCount}</strong>
            </div>
            <div className="metric-box">
              <span>En attente</span>
              <strong>0</strong>
            </div>
            <div className="metric-box">
              <span>Exceptions</span>
              <strong>0</strong>
            </div>
          </div>
        </section>

        <section className="card">
          <h2>Employés</h2>
          <ul className="list">
            {employees.length === 0 ? (
              <li>Aucun employé trouvé pour le moment.</li>
            ) : (
              employees.map((employee) => (
                <li key={employee.id}>
                  <strong>{employee.full_name}</strong>
                  <small>
                    {employee.employee_code} · {employee.enrollment_state}
                  </small>
                </li>
              ))
            )}
          </ul>
        </section>

        <section className="card">
          <h2>Pointages récents</h2>
          <table>
            <thead>
              <tr>
                <th>Action</th>
                <th>Heure</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              {events.length === 0 ? (
                <tr>
                  <td colSpan={3}>Aucun événement enregistré.</td>
                </tr>
              ) : (
                events.slice(0, 5).map((event) => (
                  <tr key={event.id}>
                    <td>{eventLabels[event.event_type] ?? event.event_type}</td>
                    <td>{new Date(event.event_time).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}</td>
                    <td>{event.status}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </section>
      </main>
    </div>
  );
}
