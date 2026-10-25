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
  has_pin: boolean;
};

type FaceCheckStatus = {
  enabled: boolean;
  encryption_configured: boolean;
};

type KioskDevice = {
  id: string;
  device_name: string;
  active: boolean;
  last_seen_at: string | null;
  created_at: string;
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
  device_id: string | null;
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

const eventLabels: Record<"fr" | "ar", Record<string, string>> = {
  fr: {
    CLOCK_IN: "Arrivée",
    CLOCK_OUT: "Départ",
    BREAK_START: "Début de pause",
    BREAK_END: "Fin de pause",
  },
  ar: {
    CLOCK_IN: "دخول",
    CLOCK_OUT: "خروج",
    BREAK_START: "بداية الاستراحة",
    BREAK_END: "نهاية الاستراحة",
  },
};

const pinPattern = "\\d{4,8}";
const digitsOnly = (value: string) => value.replace(/\D/g, "").slice(0, 8);

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
        pin: "الرمز السري (4 إلى 8 أرقام)",
        pinSet: "الرمز السري مُعدّ",
        pinMissing: "بدون رمز سري",
        setPin: "تعيين الرمز السري",
        savePin: "حفظ الرمز",
        pinSaved: "تم حفظ الرمز السري.",
        pinFailed: "تعذر حفظ الرمز السري (4 إلى 8 أرقام).",
        employee: "الموظف",
        kiosks: "أجهزة الحضور",
        noKiosks: "لا يوجد جهاز مقترن. أدخل رمز الاقتران في التطبيق.",
        lastSeen: "آخر اتصال",
        revoke: "إلغاء الاقتران",
        revoked: "ملغى",
        active: "نشط",
        source: "المصدر",
        sourceKiosk: "جهاز الحضور",
        sourceAdmin: "المسؤول",
        sourceKioskFace: "جهاز الحضور · رمز + وجه",
        faceCheck: "التحقق من الوجه",
        faceCheckOn: "مفعّل: الموظفون المسجلون يؤكدون هويتهم بالوجه بعد الرمز السري.",
        faceCheckOff: "غير مفعّل: الرمز السري فقط.",
        enable: "تفعيل",
        disable: "إيقاف",
        faceKeyMissing: "مفتاح تشفير بيانات الوجه (FACE_DATA_KEY) غير مُعدّ على الخادم.",
        faceLegal: "بيانات الوجه بيانات حساسة: احصل على موافقة كتابية من كل موظف وتحقق من متطلبات الهيئة الوطنية لحماية المعطيات الشخصية (INPDP).",
        faceEnrolled: "الوجه مسجّل",
        facePending: "تسجيل الوجه بانتظار الجهاز (30 دقيقة)",
        faceNone: "بدون وجه",
        authorizeFace: "السماح بتسجيل الوجه",
        deleteFace: "حذف بيانات الوجه",
        faceConsentPrompt: "هل قدّم هذا الموظف موافقة كتابية على تسجيل وجهه؟ بعد التأكيد، لديه 30 دقيقة لتسجيل وجهه على جهاز الحضور.",
        faceDeletePrompt: "حذف بيانات الوجه لهذا الموظف نهائياً؟",
        faceAuthorized: "تم السماح بالتسجيل. اطلب من الموظف إدخال رمزه على جهاز الحضور.",
        faceDeleted: "تم حذف بيانات الوجه.",
        faceFailed: "تعذر تنفيذ العملية. تحقق من أن للموظف رمزاً سرياً.",
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
        pin: "PIN (4 à 8 chiffres)",
        pinSet: "PIN défini",
        pinMissing: "Sans PIN",
        setPin: "Définir le PIN",
        savePin: "Enregistrer le PIN",
        pinSaved: "PIN enregistré.",
        pinFailed: "Impossible d’enregistrer le PIN (4 à 8 chiffres).",
        employee: "Employé",
        kiosks: "Bornes de pointage",
        noKiosks: "Aucune borne jumelée. Saisissez le code de jumelage dans l’application.",
        lastSeen: "Dernière activité",
        revoke: "Révoquer",
        revoked: "Révoquée",
        active: "Active",
        source: "Source",
        sourceKiosk: "Borne",
        sourceAdmin: "Administrateur",
        sourceKioskFace: "Borne · PIN + visage",
        faceCheck: "Vérification faciale",
        faceCheckOn: "Activée : les employés enrôlés confirment leur identité par le visage après le PIN.",
        faceCheckOff: "Désactivée : PIN uniquement.",
        enable: "Activer",
        disable: "Désactiver",
        faceKeyMissing: "La clé de chiffrement des visages (FACE_DATA_KEY) n’est pas configurée sur le serveur.",
        faceLegal: "Les données faciales sont sensibles : obtenez le consentement écrit de chaque employé et vérifiez les exigences de l’INPDP.",
        faceEnrolled: "Visage enrôlé",
        facePending: "Enrôlement en attente sur la borne (30 min)",
        faceNone: "Sans visage",
        authorizeFace: "Autoriser l’enrôlement du visage",
        deleteFace: "Supprimer les données faciales",
        faceConsentPrompt: "Cet employé a-t-il donné son consentement écrit à l’enregistrement de son visage ? Après confirmation, il a 30 minutes pour s’enrôler sur la borne.",
        faceDeletePrompt: "Supprimer définitivement les données faciales de cet employé ?",
        faceAuthorized: "Enrôlement autorisé. Demandez à l’employé de saisir son code sur la borne.",
        faceDeleted: "Données faciales supprimées.",
        faceFailed: "Opération impossible. Vérifiez que l’employé a un PIN.",
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
  const [employeePin, setEmployeePin] = useState("");
  const [pinEmployeeId, setPinEmployeeId] = useState<string | null>(null);
  const [pinValue, setPinValue] = useState("");
  const [kioskDevices, setKioskDevices] = useState<KioskDevice[]>([]);
  const [liveRefresh, setLiveRefresh] = useState(0);
  const [faceCheck, setFaceCheck] = useState<FaceCheckStatus | null>(null);

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
        const [employeesResponse, functionsResponse, attendanceResponse, shiftTemplatesResponse, workdayResponse, kioskResponse, faceResponse] = await Promise.all([
          fetch(`${apiBase}/api/v1/employees`, { headers }),
          fetch(`${apiBase}/api/v1/functions`, { headers }),
          fetch(`${apiBase}/api/v1/attendance`, { headers }),
          fetch(`${apiBase}/api/v1/shift-templates`, { headers }),
          fetch(`${apiBase}/api/v1/dashboard/workday?work_date=${reportDate}`, { headers }),
          fetch(`${apiBase}/api/v1/kiosk/devices`, { headers }),
          fetch(`${apiBase}/api/v1/settings/face-check`, { headers }),
        ]);

        if ([employeesResponse, functionsResponse, attendanceResponse, shiftTemplatesResponse, workdayResponse, kioskResponse, faceResponse].some((response) => response.status === 401)) {
          sessionStorage.removeItem("auth_token");
          if (!cancelled) setToken(null);
          return;
        }

        const [employeeData, functionData, attendanceData, shiftTemplateData, workdayData, kioskData, faceData] = await Promise.all([
          employeesResponse.json(),
          functionsResponse.json(),
          attendanceResponse.json(),
          shiftTemplatesResponse.json(),
          workdayResponse.json(),
          kioskResponse.json(),
          faceResponse.json(),
        ]);

        if (!cancelled) {
          setEmployees(Array.isArray(employeeData) ? employeeData : []);
          setJobFunctions(Array.isArray(functionData) ? functionData : []);
          setEvents(Array.isArray(attendanceData) ? attendanceData : []);
          setShiftTemplates(Array.isArray(shiftTemplateData) ? shiftTemplateData : []);
          setWorkdays(Array.isArray(workdayData.employees) ? workdayData.employees : []);
          setKioskDevices(Array.isArray(kioskData) ? kioskData : []);
          setFaceCheck(faceResponse.ok ? faceData : null);
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
  }, [token, reportDate, scheduleRefresh, liveRefresh]);

  useEffect(() => {
    if (!token) return;
    const intervalId = window.setInterval(() => setLiveRefresh((current) => current + 1), 15_000);
    return () => window.clearInterval(intervalId);
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
          pin: employeePin,
        }),
      });
      if (!response.ok) throw new Error(text.employeeFailed);
      const employee = (await response.json()) as Employee;
      setEmployees((current) => [employee, ...current]);
      setEmployeeCode("");
      setEmployeePin("");
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

  const handlePinSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token || !pinEmployeeId) return;
    setEmployeeError("");
    setEmployeeSuccess("");
    try {
      const response = await fetch(`${apiBase}/api/v1/employees/${pinEmployeeId}/pin`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ pin: pinValue }),
      });
      if (!response.ok) throw new Error(text.pinFailed);
      setEmployees((current) => current.map((item) => item.id === pinEmployeeId ? { ...item, has_pin: true } : item));
      setPinEmployeeId(null);
      setPinValue("");
      setEmployeeSuccess(text.pinSaved);
    } catch {
      setEmployeeError(text.pinFailed);
    }
  };

  const handleFaceCheckToggle = async () => {
    if (!token || !faceCheck) return;
    const response = await fetch(`${apiBase}/api/v1/settings/face-check`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ enabled: !faceCheck.enabled }),
    });
    if (response.ok) setFaceCheck((await response.json()) as FaceCheckStatus);
  };

  const handleFaceAuthorize = async (employeeId: string) => {
    if (!token || !window.confirm(text.faceConsentPrompt)) return;
    setEmployeeError("");
    setEmployeeSuccess("");
    const response = await fetch(`${apiBase}/api/v1/employees/${employeeId}/face/authorize`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ consent_confirmed: true }),
    });
    if (!response.ok) {
      setEmployeeError(text.faceFailed);
      return;
    }
    setEmployees((current) => current.map((item) => item.id === employeeId ? { ...item, enrollment_state: "pending" } : item));
    setEmployeeSuccess(text.faceAuthorized);
  };

  const handleFaceDelete = async (employeeId: string) => {
    if (!token || !window.confirm(text.faceDeletePrompt)) return;
    setEmployeeError("");
    setEmployeeSuccess("");
    const response = await fetch(`${apiBase}/api/v1/employees/${employeeId}/face`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) {
      setEmployeeError(text.faceFailed);
      return;
    }
    setEmployees((current) => current.map((item) => item.id === employeeId ? { ...item, enrollment_state: "not_enrolled" } : item));
    setEmployeeSuccess(text.faceDeleted);
  };

  const handleKioskRevoke = async (deviceId: string) => {
    if (!token) return;
    const response = await fetch(`${apiBase}/api/v1/kiosk/devices/${deviceId}/revoke`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.ok) {
      setKioskDevices((current) => current.map((device) => device.id === deviceId ? { ...device, active: false } : device));
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
  const employeeNames = new Map(employees.map((employee) => [employee.id, employee.full_name]));
  const kioskNames = new Map(kioskDevices.map((device) => [device.id, device.device_name]));
  const formatDateTime = (value: string) => new Date(value).toLocaleString(isArabic ? "ar" : "fr-FR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
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
                <span>{text.pin}</span>
                <input
                  type="password"
                  inputMode="numeric"
                  autoComplete="new-password"
                  pattern={pinPattern}
                  value={employeePin}
                  onChange={(event) => setEmployeePin(digitsOnly(event.target.value))}
                  required
                />
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
                  <small>
                    {employee.employee_code} · <span className={employee.has_pin ? "pin-ok" : "pin-missing"}>{employee.has_pin ? text.pinSet : text.pinMissing}</span>
                    {" · "}
                    <span className={employee.enrollment_state === "enrolled" ? "pin-ok" : ""}>
                      {employee.enrollment_state === "enrolled"
                        ? text.faceEnrolled
                        : employee.enrollment_state === "pending" ? text.facePending : text.faceNone}
                    </span>
                  </small>
                  {pinEmployeeId === employee.id ? (
                    <form className="pin-form" onSubmit={handlePinSave}>
                      <input
                        type="password"
                        inputMode="numeric"
                        autoComplete="new-password"
                        aria-label={text.pin}
                        placeholder={text.pin}
                        pattern={pinPattern}
                        value={pinValue}
                        onChange={(event) => setPinValue(digitsOnly(event.target.value))}
                        required
                        autoFocus
                      />
                      <button className="primary-action compact-action" type="submit">{text.savePin}</button>
                      <button className="text-button" type="button" onClick={() => setPinEmployeeId(null)}>{text.cancel}</button>
                    </form>
                  ) : (
                    <div className="inline-actions">
                      <button
                        className="text-button inline-action"
                        type="button"
                        onClick={() => {
                          setPinEmployeeId(employee.id);
                          setPinValue("");
                        }}
                      >
                        {text.setPin}
                      </button>
                      {faceCheck?.encryption_configured && employee.has_pin && employee.enrollment_state !== "pending" && (
                        <button className="text-button inline-action" type="button" onClick={() => void handleFaceAuthorize(employee.id)}>
                          {text.authorizeFace}
                        </button>
                      )}
                      {employee.enrollment_state !== "not_enrolled" && (
                        <button className="text-button inline-action danger-action" type="button" onClick={() => void handleFaceDelete(employee.id)}>
                          {text.deleteFace}
                        </button>
                      )}
                    </div>
                  )}
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
              <tr><th>{text.employee}</th><th>{text.action}</th><th>{text.time}</th><th>{text.source}</th></tr>
            </thead>
            <tbody>
              {events.length === 0 ? (
                <tr><td colSpan={4}>{text.noEvents}</td></tr>
              ) : (
                events.slice(0, 10).map((event) => (
                  <tr key={event.id}>
                    <td>{employeeNames.get(event.employee_id) ?? event.employee_id}</td>
                    <td>{eventLabels[language][event.event_type] ?? event.event_type}</td>
                    <td>{formatDateTime(event.event_time)}</td>
                    <td>
                      {event.source.startsWith("kiosk_pin")
                        ? `${event.source === "kiosk_pin_face" ? text.sourceKioskFace : text.sourceKiosk}${event.device_id ? ` · ${kioskNames.get(event.device_id) ?? ""}` : ""}`
                        : text.sourceAdmin}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </section>

        <section className="card">
          <div className="section-heading">
            <h2>{text.faceCheck}</h2>
            {faceCheck?.encryption_configured && (
              <button
                className={`compact-action ${faceCheck.enabled ? "text-button" : "primary-action"}`}
                type="button"
                onClick={() => void handleFaceCheckToggle()}
              >
                {faceCheck.enabled ? text.disable : text.enable}
              </button>
            )}
          </div>
          <p className={faceCheck?.enabled ? "form-success" : ""}>
            {faceCheck?.enabled ? text.faceCheckOn : text.faceCheckOff}
          </p>
          {faceCheck && !faceCheck.encryption_configured && <p className="form-error">{text.faceKeyMissing}</p>}
          <small className="muted-note">{text.faceLegal}</small>
        </section>

        <section className="card">
          <h2>{text.kiosks}</h2>
          <ul className="list">
            {kioskDevices.length === 0 ? (
              <li>{text.noKiosks}</li>
            ) : (
              kioskDevices.map((device) => (
                <li key={device.id}>
                  <strong>{device.device_name}</strong>
                  <small>
                    {device.active ? text.active : text.revoked}
                    {device.last_seen_at ? ` · ${text.lastSeen}: ${formatDateTime(`${device.last_seen_at}Z`)}` : ""}
                  </small>
                  {device.active && (
                    <button className="text-button inline-action" type="button" onClick={() => void handleKioskRevoke(device.id)}>
                      {text.revoke}
                    </button>
                  )}
                </li>
              ))
            )}
          </ul>
        </section>
      </main>
    </div>
  );
}