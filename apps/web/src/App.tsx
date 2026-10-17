import { FormEvent, useEffect, useState } from "react";

const apiBase = `${window.location.protocol}//${window.location.hostname}:8000`;

type Employee = {
  id: string;
  employee_code: string;
  full_name: string;
  job_function_id: string | null;
  start_date: string;
  active: boolean;
  enrollment_state: string;
};

type JobFunction = {
  id: string;
  name: string;
  active: boolean;
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
  const [language, setLanguage] = useState<"fr" | "ar">("fr");
  const isArabic = language === "ar";
  const text = isArabic
    ? {
        title: "كابوتشينو لاك 1",
        loginTitle: "دخول المسؤول",
        email: "البريد الإلكتروني",
        password: "كلمة المرور",
        signIn: "تسجيل الدخول",
        logout: "تسجيل الخروج",
        serverOnline: "الخادم متصل",
        serverOffline: "الخادم غير متصل",
        overview: "نظرة عامة",
        employees: "الموظفون",
        present: "الحاضرون",
        pending: "قيد الانتظار",
        exceptions: "استثناءات",
        addEmployee: "إضافة موظف",
        employeeCode: "رمز الموظف",
        fullName: "الاسم الكامل",
        jobFunction: "الوظيفة",
        noFunction: "بدون وظيفة",
        startDate: "تاريخ البدء",
        saveEmployee: "حفظ الموظف",
        cancel: "إلغاء",
        enrollment: "حالة التسجيل",
        notEnrolled: "غير مسجل بيومترياً",
        recentEvents: "أحدث الحركات",
        action: "الحركة",
        time: "الوقت",
        status: "الحالة",
        noEmployees: "لا يوجد موظفون مسجلون.",
        noEvents: "لا توجد حركات مسجلة.",
        loginFailed: "تعذر تسجيل الدخول. تحقق من البيانات والإعدادات.",
        employeeFailed: "تعذر حفظ الموظف. تحقق من الرمز والحقول.",
        employeeSaved: "تم حفظ الموظف.",
      }
    : {
        title: "Cappuccino Lac 1",
        loginTitle: "Connexion administrateur",
        email: "Adresse e-mail",
        password: "Mot de passe",
        signIn: "Se connecter",
        logout: "Déconnexion",
        serverOnline: "Serveur en ligne",
        serverOffline: "Serveur hors ligne",
        overview: "Vue d’ensemble",
        employees: "Employés",
        present: "Présents",
        pending: "En attente",
        exceptions: "Exceptions",
        addEmployee: "Ajouter un employé",
        employeeCode: "Code employé",
        fullName: "Nom complet",
        jobFunction: "Fonction",
        noFunction: "Sans fonction",
        startDate: "Date de début",
        saveEmployee: "Enregistrer l’employé",
        cancel: "Annuler",
        enrollment: "Enrôlement",
        notEnrolled: "Non enrôlé",
        recentEvents: "Pointages récents",
        action: "Action",
        time: "Heure",
        status: "Statut",
        noEmployees: "Aucun employé enregistré.",
        noEvents: "Aucun événement enregistré.",
        loginFailed: "Connexion impossible. Vérifiez les identifiants et la configuration.",
        employeeFailed: "Enregistrement impossible. Vérifiez le code et les champs.",
        employeeSaved: "Employé enregistré.",
      };

  const [health, setHealth] = useState("Checking");
  const [token, setToken] = useState(() => sessionStorage.getItem("auth_token"));
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [jobFunctions, setJobFunctions] = useState<JobFunction[]>([]);
  const [events, setEvents] = useState<AttendanceEvent[]>([]);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [showRegistration, setShowRegistration] = useState(false);
  const [employeeCode, setEmployeeCode] = useState("");
  const [fullName, setFullName] = useState("");
  const [jobFunctionId, setJobFunctionId] = useState("");
  const [startDate, setStartDate] = useState(new Date().toISOString().slice(0, 10));
  const [employeeError, setEmployeeError] = useState("");
  const [employeeSuccess, setEmployeeSuccess] = useState("");
  const [isSavingEmployee, setIsSavingEmployee] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const loadData = async () => {
      try {
        const healthResponse = await fetch(`${apiBase}/api/v1/health`);
        const healthData = await healthResponse.json();
        if (!cancelled) setHealth(healthData.status ?? "offline");
      } catch {
        if (!cancelled) setHealth("offline");
      }

      if (!token) {
        if (!cancelled) {
          setEmployees([]);
          setJobFunctions([]);
          setEvents([]);
        }
        return;
      }

      const headers = { Authorization: `Bearer ${token}` };
      try {
        const [employeesResponse, functionsResponse, attendanceResponse] = await Promise.all([
          fetch(`${apiBase}/api/v1/employees`, { headers }),
          fetch(`${apiBase}/api/v1/functions`, { headers }),
          fetch(`${apiBase}/api/v1/attendance`, { headers }),
        ]);

        if ([employeesResponse, functionsResponse, attendanceResponse].some((response) => response.status === 401)) {
          sessionStorage.removeItem("auth_token");
          if (!cancelled) setToken(null);
          return;
        }

        const [employeeData, functionData, attendanceData] = await Promise.all([
          employeesResponse.json(),
          functionsResponse.json(),
          attendanceResponse.json(),
        ]);

        if (!cancelled) {
          setEmployees(Array.isArray(employeeData) ? employeeData : []);
          setJobFunctions(Array.isArray(functionData) ? functionData : []);
          setEvents(Array.isArray(attendanceData) ? attendanceData : []);
        }
      } catch {
        if (!cancelled) {
          setEmployees([]);
          setJobFunctions([]);
          setEvents([]);
        }
      }
    };

    void loadData();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoginError("");
    try {
      const response = await fetch(`${apiBase}/api/v1/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      if (!response.ok) throw new Error(text.loginFailed);
      const result = (await response.json()) as { access_token: string };
      sessionStorage.setItem("auth_token", result.access_token);
      setToken(result.access_token);
      setPassword("");
    } catch {
      setLoginError(text.loginFailed);
    }
  };

  const handleEmployeeCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) return;
    setEmployeeError("");
    setEmployeeSuccess("");
    setIsSavingEmployee(true);
    try {
      const response = await fetch(`${apiBase}/api/v1/employees`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          employee_code: employeeCode,
          full_name: fullName,
          job_function_id: jobFunctionId || null,
          start_date: startDate,
          active: true,
        }),
      });
      if (!response.ok) throw new Error(text.employeeFailed);
      const employee = (await response.json()) as Employee;
      setEmployees((current) => [employee, ...current]);
      setEmployeeCode("");
      setFullName("");
      setJobFunctionId("");
      setStartDate(new Date().toISOString().slice(0, 10));
      setShowRegistration(false);
      setEmployeeSuccess(text.employeeSaved);
    } catch {
      setEmployeeError(text.employeeFailed);
    } finally {
      setIsSavingEmployee(false);
    }
  };

  const handleLogout = () => {
    sessionStorage.removeItem("auth_token");
    setToken(null);
    setPassword("");
  };

  const toggleLanguage = () => setLanguage((current) => current === "fr" ? "ar" : "fr");
  const presentCount = events.filter((event) => event.event_type === "CLOCK_IN").length;

  if (!token) {
    return (
      <div className="app-shell login-shell" dir={isArabic ? "rtl" : "ltr"}>
        <header className="topbar">
          <div className="brand-wrap">
            <img src="/branding/cappuccino-logo.png" alt={text.title} className="brand-logo" />
            <div>
              <p className="eyebrow">Zwanski Vulzero</p>
              <h1>{text.title}</h1>
            </div>
          </div>
          <div className="topbar-actions">
            <button className="text-button" type="button" onClick={toggleLanguage}>
              {isArabic ? "Français" : "العربية"}
            </button>
            <div className={`status-pill ${health === "ok" ? "ok" : "warning"}`}>
              {health === "ok" ? text.serverOnline : text.serverOffline}
            </div>
          </div>
        </header>
        <main className="login-main">
          <section className="card login-card">
            <h2>{text.loginTitle}</h2>
            <form className="form-stack" onSubmit={handleLogin}>
              <label className="form-field">
                <span>{text.email}</span>
                <input
                  autoComplete="username"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </label>
              <label className="form-field">
                <span>{text.password}</span>
                <input
                  autoComplete="current-password"
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </label>
              {loginError && <p className="form-error" role="alert">{loginError}</p>}
              <button className="primary-action" type="submit">{text.signIn}</button>
            </form>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="app-shell" dir={isArabic ? "rtl" : "ltr"}>
      <header className="topbar">
        <div className="brand-wrap">
          <img src="/branding/cappuccino-logo.png" alt={text.title} className="brand-logo" />
          <div>
            <p className="eyebrow">Zwanski Vulzero</p>
            <h1>{text.title}</h1>
          </div>
        </div>
        <div className="topbar-actions">
          <button className="text-button" type="button" onClick={toggleLanguage}>
            {isArabic ? "Français" : "العربية"}
          </button>
          <div className={`status-pill ${health === "ok" ? "ok" : "warning"}`}>
            {health === "ok" ? text.serverOnline : text.serverOffline}
          </div>
          <button className="text-button" type="button" onClick={handleLogout}>{text.logout}</button>
        </div>
      </header>

      <main className="main-grid">
        <section className="card panel-wide">
          <h2>{text.overview}</h2>
          <div className="metric-grid">
            <div className="metric-box"><span>{text.employees}</span><strong>{employees.length}</strong></div>
            <div className="metric-box"><span>{text.present}</span><strong>{presentCount}</strong></div>
            <div className="metric-box"><span>{text.pending}</span><strong>0</strong></div>
            <div className="metric-box"><span>{text.exceptions}</span><strong>0</strong></div>
          </div>
        </section>

        <section className="card employee-panel">
          <div className="section-heading">
            <h2>{text.employees}</h2>
            <button
              className="primary-action compact-action"
              type="button"
              onClick={() => {
                setEmployeeError("");
                setEmployeeSuccess("");
                setShowRegistration((current) => !current);
              }}
            >
              {text.addEmployee}
            </button>
          </div>

          {employeeError && <p className="form-error" role="alert">{employeeError}</p>}
          {employeeSuccess && <p className="form-success" role="status">{employeeSuccess}</p>}
          {showRegistration && (
            <form className="form-stack employee-form" onSubmit={handleEmployeeCreate}>
              <label className="form-field">
                <span>{text.employeeCode}</span>
                <input value={employeeCode} onChange={(event) => setEmployeeCode(event.target.value)} minLength={2} maxLength={64} required />
              </label>
              <label className="form-field">
                <span>{text.fullName}</span>
                <input value={fullName} onChange={(event) => setFullName(event.target.value)} minLength={2} maxLength={255} required />
              </label>
              <label className="form-field">
                <span>{text.jobFunction}</span>
                <select value={jobFunctionId} onChange={(event) => setJobFunctionId(event.target.value)}>
                  <option value="">{text.noFunction}</option>
                  {jobFunctions.filter((item) => item.active).map((item) => (
                    <option key={item.id} value={item.id}>{item.name}</option>
                  ))}
                </select>
              </label>
              <label className="form-field">
                <span>{text.startDate}</span>
                <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} required />
              </label>
              <div className="form-actions">
                <button className="primary-action" type="submit" disabled={isSavingEmployee}>
                  {isSavingEmployee ? "…" : text.saveEmployee}
                </button>
                <button className="text-button" type="button" onClick={() => setShowRegistration(false)}>
                  {text.cancel}
                </button>
              </div>
            </form>
          )}

          <ul className="list">
            {employees.length === 0 ? (
              <li>{text.noEmployees}</li>
            ) : (
              employees.map((employee) => (
                <li key={employee.id}>
                  <strong>{employee.full_name}</strong>
                  <small>{employee.employee_code} · {text.enrollment}: {text.notEnrolled}</small>
                </li>
              ))
            )}
          </ul>
        </section>

        <section className="card">
          <h2>{text.recentEvents}</h2>
          <table>
            <thead>
              <tr><th>{text.action}</th><th>{text.time}</th><th>{text.status}</th></tr>
            </thead>
            <tbody>
              {events.length === 0 ? (
                <tr><td colSpan={3}>{text.noEvents}</td></tr>
              ) : (
                events.slice(0, 5).map((event) => (
                  <tr key={event.id}>
                    <td>{eventLabels[event.event_type] ?? event.event_type}</td>
                    <td>{new Date(event.event_time).toLocaleTimeString(isArabic ? "ar" : "fr-FR", { hour: "2-digit", minute: "2-digit" })}</td>
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