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

type ShiftTemplate = {
  id: string;
  name: string;
  start_time: string;
  end_time: string;
  break_minutes: number;
};

type WorkdaySummary = {
  employee_id: string;
  employee_code: string;
  full_name: string;
  work_date: string;
  days_worked_this_month: number;
  worked_minutes: number;
  break_minutes: number;
  target_minutes: number;
  remaining_minutes: number;
  overtime_minutes: number;
  shift_count: number;
  is_double_shift: boolean;
  shifts: ShiftTemplate[];
  status: string;
};

const localDateValue = () => {
  const current = new Date();
  return `${current.getFullYear()}-${String(current.getMonth() + 1).padStart(2, "0")}-${String(current.getDate()).padStart(2, "0")}`;
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
        diagnostics: "تشخيص النظام",
        apiStatus: "واجهة API",
        databaseStatus: "قاعدة البيانات",
        latency: "زمن الاستجابة",
        lastChecked: "آخر تحقق",
        checking: "جارٍ التحقق",
        online: "متاح",
        offline: "غير متاح",
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
        workday: "ملخص يوم العمل",
        workDate: "التاريخ",
        worked: "ساعات العمل",
        daysWorked: "أيام العمل هذا الشهر",
        target: "الهدف",
        remaining: "المتبقي",
        schedule: "المناوبات",
        doubleShift: "مناوبتان",
        noShift: "بدون مناوبة محددة",
        notStarted: "لم يبدأ",
        working: "يعمل الآن",
        onBreak: "في استراحة",
        complete: "مكتمل",
        shortDay: "يوم غير مكتمل",
        targetReached: "اكتمل الهدف",
        shiftSetup: "إعداد المناوبات",
        shiftName: "اسم المناوبة",
        shiftStart: "بداية المناوبة",
        shiftEnd: "نهاية المناوبة",
        breakMinutes: "دقائق الاستراحة",
        createShift: "إنشاء مناوبة",
        assignShift: "إسناد مناوبة",
        chooseEmployee: "اختر موظفاً",
        chooseShift: "اختر مناوبة",
        validFrom: "سارية من",
        validTo: "سارية حتى (اختياري)",
        shiftCreated: "تم إنشاء المناوبة.",
        assignmentSaved: "تم حفظ المناوبة للموظف.",
        scheduleFailed: "تعذر حفظ إعداد المناوبة.",
        noShifts: "أنشئ مناوبة أولاً.",
        overtime: "وقت إضافي",
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
        diagnostics: "Diagnostic système",
        apiStatus: "API",
        databaseStatus: "Base de données",
        latency: "Latence",
        lastChecked: "Dernière vérification",
        checking: "Vérification…",
        online: "Disponible",
        offline: "Indisponible",
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
        workday: "Bilan de la journée",
        workDate: "Date",
        worked: "Temps travaillé",
        daysWorked: "Jours travaillés ce mois",
        target: "Objectif",
        remaining: "Restant",
        schedule: "Planning",
        doubleShift: "Double shift",
        noShift: "Aucun shift assigné",
        notStarted: "Non commencé",
        working: "En cours",
        onBreak: "En pause",
        complete: "Complet",
        shortDay: "Journée incomplète",
        targetReached: "Objectif atteint",
        shiftSetup: "Configuration des shifts",
        shiftName: "Nom du shift",
        shiftStart: "Début du shift",
        shiftEnd: "Fin du shift",
        breakMinutes: "Minutes de pause",
        createShift: "Créer un shift",
        assignShift: "Assigner un shift",
        chooseEmployee: "Choisir un employé",
        chooseShift: "Choisir un shift",
        validFrom: "Valide à partir du",
        validTo: "Valide jusqu’au (facultatif)",
        shiftCreated: "Shift créé.",
        assignmentSaved: "Shift assigné à l’employé.",
        scheduleFailed: "Impossible d’enregistrer le planning.",
        noShifts: "Créez d’abord un shift.",
        overtime: "Heures supplémentaires",
      };

  const [health, setHealth] = useState("Checking");
  const [databaseHealth, setDatabaseHealth] = useState("Checking");
  const [lastDiagnosticCheck, setLastDiagnosticCheck] = useState<string | null>(null);
  const [diagnosticLatency, setDiagnosticLatency] = useState<number | null>(null);
  const [token, setToken] = useState(() => sessionStorage.getItem("auth_token"));
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [jobFunctions, setJobFunctions] = useState<JobFunction[]>([]);
  const [events, setEvents] = useState<AttendanceEvent[]>([]);
  const [shiftTemplates, setShiftTemplates] = useState<ShiftTemplate[]>([]);
  const [workdays, setWorkdays] = useState<WorkdaySummary[]>([]);
  const [reportDate, setReportDate] = useState(localDateValue);
  const [scheduleRefresh, setScheduleRefresh] = useState(0);
  const [shiftName, setShiftName] = useState("");
  const [shiftStart, setShiftStart] = useState("08:00");
  const [shiftEnd, setShiftEnd] = useState("17:00");
  const [shiftBreakMinutes, setShiftBreakMinutes] = useState("60");
  const [assignmentEmployeeId, setAssignmentEmployeeId] = useState("");
  const [assignmentShiftId, setAssignmentShiftId] = useState("");
  const [assignmentValidFrom, setAssignmentValidFrom] = useState(localDateValue);
  const [assignmentValidTo, setAssignmentValidTo] = useState("");
  const [scheduleMessage, setScheduleMessage] = useState("");
  const [scheduleError, setScheduleError] = useState("");
  const [isSavingSchedule, setIsSavingSchedule] = useState(false);
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
    let checkInProgress = false;

    const checkServices = async () => {
      if (checkInProgress) return;
      checkInProgress = true;
      const startedAt = performance.now();
      try {
        const [apiResult, databaseResult] = await Promise.allSettled([
          fetch(`${apiBase}/api/v1/health`).then(async (response) => {
            if (!response.ok) throw new Error("API health check failed");
            return await response.json() as { status: string };
          }),
          fetch(`${apiBase}/api/v1/ready`).then(async (response) => {
            if (!response.ok) throw new Error("Database readiness check failed");
            return await response.json() as { status: string };
          }),
        ]);

        if (cancelled) return;
        setHealth(apiResult.status === "fulfilled" && apiResult.value.status === "ok" ? "ok" : "offline");
        setDatabaseHealth(databaseResult.status === "fulfilled" && databaseResult.value.status === "ready" ? "ok" : "offline");
        setDiagnosticLatency(Math.round(performance.now() - startedAt));
        setLastDiagnosticCheck(new Date().toISOString());
      } finally {
        checkInProgress = false;
      }
    };

    void checkServices();
    const intervalId = window.setInterval(() => void checkServices(), 30_000);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    const loadData = async () => {
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
        const [employeesResponse, functionsResponse, attendanceResponse, shiftTemplatesResponse, workdayResponse] = await Promise.all([
          fetch(`${apiBase}/api/v1/employees`, { headers }),
          fetch(`${apiBase}/api/v1/functions`, { headers }),
          fetch(`${apiBase}/api/v1/attendance`, { headers }),
          fetch(`${apiBase}/api/v1/shift-templates`, { headers }),
          fetch(`${apiBase}/api/v1/dashboard/workday?work_date=${reportDate}`, { headers }),
        ]);

        if ([employeesResponse, functionsResponse, attendanceResponse, shiftTemplatesResponse, workdayResponse].some((response) => response.status === 401)) {
          sessionStorage.removeItem("auth_token");
          if (!cancelled) setToken(null);
          return;
        }

        const [employeeData, functionData, attendanceData, shiftTemplateData, workdayData] = await Promise.all([
          employeesResponse.json(),
          functionsResponse.json(),
          attendanceResponse.json(),
          shiftTemplatesResponse.json(),
          workdayResponse.json(),
        ]);

        if (!cancelled) {
          setEmployees(Array.isArray(employeeData) ? employeeData : []);
          setJobFunctions(Array.isArray(functionData) ? functionData : []);
          setEvents(Array.isArray(attendanceData) ? attendanceData : []);
          setShiftTemplates(Array.isArray(shiftTemplateData) ? shiftTemplateData : []);
          setWorkdays(Array.isArray(workdayData.employees) ? workdayData.employees : []);
          setAssignmentEmployeeId((current) => current || employeeData[0]?.id || "");
          setAssignmentShiftId((current) => current || shiftTemplateData[0]?.id || "");
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
  }, [token, reportDate, scheduleRefresh]);

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

  const handleShiftCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) return;
    setScheduleError("");
    setScheduleMessage("");
    setIsSavingSchedule(true);
    try {
      const response = await fetch(`${apiBase}/api/v1/shift-templates`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          name: shiftName,
          start_time: `${shiftStart}:00`,
          end_time: `${shiftEnd}:00`,
          break_minutes: Number(shiftBreakMinutes),
        }),
      });
      if (!response.ok) throw new Error(text.scheduleFailed);
      const created = (await response.json()) as ShiftTemplate;
      setShiftTemplates((current) => [...current, created].sort((left, right) => left.start_time.localeCompare(right.start_time)));
      setAssignmentShiftId(created.id);
      setShiftName("");
      setScheduleMessage(text.shiftCreated);
    } catch {
      setScheduleError(text.scheduleFailed);
    } finally {
      setIsSavingSchedule(false);
    }
  };

  const handleShiftAssignment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token || !assignmentEmployeeId || !assignmentShiftId) return;
    setScheduleError("");
    setScheduleMessage("");
    setIsSavingSchedule(true);
    try {
      const response = await fetch(`${apiBase}/api/v1/employees/${assignmentEmployeeId}/shift-assignments`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          shift_template_id: assignmentShiftId,
          valid_from: assignmentValidFrom,
          valid_to: assignmentValidTo || null,
        }),
      });
      if (!response.ok) throw new Error(text.scheduleFailed);
      setScheduleMessage(text.assignmentSaved);
      setScheduleRefresh((current) => current + 1);
    } catch {
      setScheduleError(text.scheduleFailed);
    } finally {
      setIsSavingSchedule(false);
    }
  };

  const handleLogout = () => {
    sessionStorage.removeItem("auth_token");
    setToken(null);
    setPassword("");
  };

  const toggleLanguage = () => setLanguage((current) => current === "fr" ? "ar" : "fr");
  const presentCount = workdays.filter((entry) => ["working", "on_break", "target_reached"].includes(entry.status)).length;
  const formatMinutes = (minutes: number) => `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, "0")}`;
  const statusLabel = (status: string) => ({
    not_started: text.notStarted,
    working: text.working,
    on_break: text.onBreak,
    target_reached: text.targetReached,
    complete: text.complete,
    short_day: text.shortDay,
  }[status] ?? status);
  const getStatusLabel = (status: string) => {
    if (status === "ok") return text.online;
    if (status === "offline") return text.offline;
    return text.checking;
  };

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

        <section className="card panel-wide workday-panel">
          <div className="section-heading">
            <h2>{text.workday}</h2>
            <label className="form-field date-filter">
              <span>{text.workDate}</span>
              <input type="date" value={reportDate} onChange={(event) => setReportDate(event.target.value)} />
            </label>
          </div>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>{text.employees}</th>
                  <th>{text.schedule}</th>
                  <th>{text.daysWorked}</th>
                  <th>{text.worked}</th>
                  <th>{text.target}</th>
                  <th>{text.status}</th>
                </tr>
              </thead>
              <tbody>
                {workdays.length === 0 ? (
                  <tr><td colSpan={6}>{text.noEmployees}</td></tr>
                ) : workdays.map((workday) => (
                  <tr key={workday.employee_id}>
                    <td>
                      <strong>{workday.full_name}</strong>
                      <small>{workday.employee_code}</small>
                    </td>
                    <td>
                      {workday.is_double_shift ? <strong className="double-shift-label">{text.doubleShift}</strong> : null}
                      <span>{workday.shifts.length ? workday.shifts.map((shift) => shift.name).join(" + ") : text.noShift}</span>
                    </td>
                    <td>{workday.days_worked_this_month}</td>
                    <td>
                      {formatMinutes(workday.worked_minutes)}
                      <small>{text.remaining}: {formatMinutes(workday.remaining_minutes)}</small>
                    </td>
                    <td>{formatMinutes(workday.target_minutes)}</td>
                    <td><span className={`workday-status status-${workday.status}`}>{statusLabel(workday.status)}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card diagnostics-panel">
          <h2>{text.diagnostics}</h2>
          <dl className="diagnostic-list">
            <div>
              <dt>{text.apiStatus}</dt>
              <dd className={health === "ok" ? "diagnostic-ok" : health === "offline" ? "diagnostic-offline" : ""}>
                {getStatusLabel(health)}
              </dd>
            </div>
            <div>
              <dt>{text.databaseStatus}</dt>
              <dd className={databaseHealth === "ok" ? "diagnostic-ok" : databaseHealth === "offline" ? "diagnostic-offline" : ""}>
                {getStatusLabel(databaseHealth)}
              </dd>
            </div>
            <div>
              <dt>{text.latency}</dt>
              <dd>{diagnosticLatency === null ? "—" : `${diagnosticLatency} ms`}</dd>
            </div>
            <div>
              <dt>{text.lastChecked}</dt>
              <dd>{lastDiagnosticCheck ? new Date(lastDiagnosticCheck).toLocaleTimeString(isArabic ? "ar" : "fr-FR") : "—"}</dd>
            </div>
          </dl>
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

        <section className="card panel-wide schedule-panel">
          <h2>{text.shiftSetup}</h2>
          {scheduleError && <p className="form-error" role="alert">{scheduleError}</p>}
          {scheduleMessage && <p className="form-success" role="status">{scheduleMessage}</p>}
          <div className="schedule-forms">
            <form className="form-stack" onSubmit={handleShiftCreate}>
              <h3>{text.createShift}</h3>
              <label className="form-field">
                <span>{text.shiftName}</span>
                <input value={shiftName} onChange={(event) => setShiftName(event.target.value)} minLength={2} maxLength={255} required />
              </label>
              <div className="time-fields">
                <label className="form-field">
                  <span>{text.shiftStart}</span>
                  <input type="time" value={shiftStart} onChange={(event) => setShiftStart(event.target.value)} required />
                </label>
                <label className="form-field">
                  <span>{text.shiftEnd}</span>
                  <input type="time" value={shiftEnd} onChange={(event) => setShiftEnd(event.target.value)} required />
                </label>
              </div>
              <label className="form-field">
                <span>{text.breakMinutes}</span>
                <input type="number" min={0} max={720} value={shiftBreakMinutes} onChange={(event) => setShiftBreakMinutes(event.target.value)} required />
              </label>
              <button className="primary-action" type="submit" disabled={isSavingSchedule}>{text.createShift}</button>
            </form>

            <form className="form-stack" onSubmit={handleShiftAssignment}>
              <h3>{text.assignShift}</h3>
              <label className="form-field">
                <span>{text.chooseEmployee}</span>
                <select value={assignmentEmployeeId} onChange={(event) => setAssignmentEmployeeId(event.target.value)} required>
                  <option value="">{text.chooseEmployee}</option>
                  {employees.filter((employee) => employee.active).map((employee) => (
                    <option key={employee.id} value={employee.id}>{employee.full_name} · {employee.employee_code}</option>
                  ))}
                </select>
              </label>
              <label className="form-field">
                <span>{text.chooseShift}</span>
                <select value={assignmentShiftId} onChange={(event) => setAssignmentShiftId(event.target.value)} required>
                  <option value="">{text.chooseShift}</option>
                  {shiftTemplates.map((shift) => (
                    <option key={shift.id} value={shift.id}>{shift.name} · {shift.start_time.slice(0, 5)}–{shift.end_time.slice(0, 5)}</option>
                  ))}
                </select>
              </label>
              <div className="time-fields">
                <label className="form-field">
                  <span>{text.validFrom}</span>
                  <input type="date" value={assignmentValidFrom} onChange={(event) => setAssignmentValidFrom(event.target.value)} required />
                </label>
                <label className="form-field">
                  <span>{text.validTo}</span>
                  <input type="date" value={assignmentValidTo} onChange={(event) => setAssignmentValidTo(event.target.value)} min={assignmentValidFrom} />
                </label>
              </div>
              <button className="primary-action" type="submit" disabled={isSavingSchedule || shiftTemplates.length === 0}>
                {text.assignShift}
              </button>
              {shiftTemplates.length === 0 && <small>{text.noShifts}</small>}
            </form>
          </div>
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