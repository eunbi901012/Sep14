import React, { FormEvent, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  message?: string;
  meta?: Row;
  errors?: Row[];
};
type PageData = {
  items: Row[];
  page: number;
  size: number;
  totalElements: number;
};
type Row = Record<string, unknown>;
type CurrentUser = {
  userId: string;
  loginId: string;
  roleCodes: string[];
  menuPaths: string[];
};
type UiStatus =
  | "idle"
  | "loading"
  | "empty"
  | "error"
  | "permission"
  | "success";
type FieldKind =
  | "text"
  | "date"
  | "number"
  | "select"
  | "textarea"
  | "readonly";
type Field = {
  key: string;
  label: string;
  kind?: FieldKind;
  required?: boolean;
  readonlyOnEdit?: boolean;
  options?: string[];
};
type Column = {
  key: string;
  label: string;
  tone?: "id" | "status" | "date" | "role";
};
type Screen = {
  id: string;
  title: string;
  description: string;
  route: string;
  menuPath: string;
  endpoint: string;
  method?: "GET" | "TREE" | "PERMISSIONS" | "DETAIL_CODES";
  columns: Column[];
  filters: Field[];
  formTitle: string;
  formFields: Field[];
  readonlyFields?: Field[];
  emptyText: string;
  primaryAction: string;
  supportsCreate?: boolean;
  supportsDelete?: boolean;
  supportsCancel?: boolean;
};

const useYnOptions = ["Y", "N"];
const roleOptions = [
  "R01",
  "R02",
  "R03",
  "R04",
  "R05",
  "R06",
  "R07",
  "R08",
  "R09",
];
const targetTypeOptions = ["ROLE", "ORG", "USER"];
const assignmentSourceOptions = ["MANUAL", "POSITION"];

export const screens: Screen[] = [
  {
    id: "SCR-CMN-USER",
    title: "사용자 관리",
    description:
      "KORUS 원천 사용자 정보를 조건 검색하고 시스템 사용여부와 업무 역할만 변경합니다.",
    route: "/admin/users",
    menuPath: "시스템 관리 > 사용자·조직 관리 > 사용자 관리",
    endpoint: "/api/admin/users",
    columns: [
      { key: "userId", label: "교번", tone: "id" },
      { key: "name", label: "성명" },
      { key: "organization", label: "소속" },
      { key: "position", label: "직급/보직" },
      { key: "employmentStatus", label: "재직상태", tone: "status" },
      { key: "role", label: "역할", tone: "role" },
      { key: "useYn", label: "사용여부", tone: "status" },
      { key: "retirementDate", label: "퇴직일자", tone: "date" },
      { key: "lastSyncedAt", label: "최종 동기화", tone: "date" },
    ],
    filters: [
      { key: "keyword", label: "교번·성명·소속" },
      { key: "filter", label: "직급·재직상태·역할" },
      {
        key: "useYn",
        label: "사용여부",
        kind: "select",
        options: ["", ...useYnOptions],
      },
    ],
    readonlyFields: [
      { key: "userId", label: "교번", kind: "readonly" },
      { key: "name", label: "성명", kind: "readonly" },
      { key: "organization", label: "소속", kind: "readonly" },
      { key: "position", label: "직급/보직", kind: "readonly" },
      { key: "employmentStatus", label: "재직상태", kind: "readonly" },
      { key: "retirementDate", label: "퇴직일자", kind: "readonly" },
      { key: "lastSyncedAt", label: "최종 동기화", kind: "readonly" },
    ],
    formTitle: "시스템 접근 편집",
    formFields: [
      {
        key: "systemUseYn",
        label: "시스템 사용여부",
        kind: "select",
        required: true,
        options: useYnOptions,
      },
      { key: "businessRole", label: "업무 역할", required: true },
    ],
    emptyText: "조건에 맞는 사용자가 없습니다.",
    primaryAction: "접근 저장",
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-ORG",
    title: "조직 관리",
    description:
      "조직 목록과 계층을 함께 확인하고 상위조직 및 적용기간을 저장합니다.",
    route: "/admin/organizations",
    menuPath: "시스템 관리 > 사용자·조직 관리 > 조직 관리",
    endpoint: "/api/admin/organizations",
    method: "TREE",
    columns: [
      { key: "orgCode", label: "조직코드", tone: "id" },
      { key: "orgName", label: "조직명" },
      { key: "orgType", label: "조직유형", tone: "status" },
      { key: "parentOrgCode", label: "상위조직", tone: "id" },
      { key: "effectiveStartDate", label: "적용 시작", tone: "date" },
      { key: "effectiveEndDate", label: "적용 종료", tone: "date" },
    ],
    filters: [
      { key: "keyword", label: "조직코드·조직명" },
      { key: "filter", label: "조직유형" },
    ],
    formTitle: "관계/적용기간 편집",
    formFields: [
      { key: "orgCode", label: "선택 조직코드", kind: "readonly" },
      { key: "parentOrgCode", label: "상위조직", required: true },
      {
        key: "effectiveStartDate",
        label: "적용 시작일",
        kind: "date",
        required: true,
      },
      { key: "effectiveEndDate", label: "적용 종료일", kind: "date" },
    ],
    emptyText: "조회된 조직이 없습니다.",
    primaryAction: "관계 저장",
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-ROLE",
    title: "역할 관리",
    description:
      "R01~R09 역할의 목적을 확인하고 부여 기준과 데이터 범위 기본값을 조정합니다.",
    route: "/admin/roles",
    menuPath: "시스템 관리 > 역할·권한 관리 > 역할 관리",
    endpoint: "/api/admin/roles",
    columns: [
      { key: "roleCode", label: "역할코드", tone: "role" },
      { key: "roleName", label: "역할명" },
      { key: "purpose", label: "목적" },
      { key: "grantCriteria", label: "부여 기준" },
      { key: "defaultDataScope", label: "데이터 범위" },
    ],
    filters: [{ key: "keyword", label: "역할코드·역할명" }],
    formTitle: "선택 역할 기준/범위 편집",
    formFields: [
      { key: "roleCode", label: "역할코드", kind: "readonly" },
      { key: "roleName", label: "역할명" },
      {
        key: "grantCriteria",
        label: "부여 기준",
        kind: "textarea",
        required: true,
      },
      {
        key: "defaultDataScope",
        label: "데이터 범위 기본값",
        kind: "textarea",
        required: true,
      },
    ],
    emptyText: "조회된 역할이 없습니다.",
    primaryAction: "역할 기준 저장",
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-USER-ROLE",
    title: "사용자 역할 관리",
    description:
      "사용자별 현재 역할과 유효기간을 조회하고 수동 역할을 부여·변경·회수합니다.",
    route: "/admin/user-roles",
    menuPath: "시스템 관리 > 역할·권한 관리 > 사용자 역할 관리",
    endpoint: "/api/admin/user-roles",
    columns: [
      { key: "assignmentId", label: "assignmentId", tone: "id" },
      { key: "userId", label: "사용자", tone: "id" },
      { key: "userName", label: "성명" },
      { key: "roleCode", label: "역할", tone: "role" },
      { key: "roleName", label: "역할명" },
      { key: "assignmentSource", label: "부여방식", tone: "status" },
      { key: "approverId", label: "승인자", tone: "id" },
      { key: "validFrom", label: "시작일", tone: "date" },
      { key: "validTo", label: "종료일", tone: "date" },
    ],
    filters: [
      { key: "keyword", label: "교번·성명" },
      { key: "filter", label: "역할" },
    ],
    formTitle: "역할 부여/변경 폼",
    formFields: [
      { key: "assignmentId", label: "assignmentId", kind: "readonly" },
      { key: "userId", label: "사용자", required: true },
      {
        key: "roleCode",
        label: "역할",
        kind: "select",
        required: true,
        options: roleOptions,
      },
      {
        key: "assignmentSource",
        label: "부여방식",
        kind: "readonly",
        required: true,
        options: assignmentSourceOptions,
      },
      { key: "approverId", label: "승인자" },
      { key: "validFrom", label: "시작일", kind: "date", required: true },
      { key: "validTo", label: "종료일", kind: "date" },
    ],
    emptyText: "조회된 사용자 역할이 없습니다.",
    primaryAction: "부여/변경 저장",
    supportsCreate: true,
    supportsDelete: true,
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-MENU-PERM",
    title: "메뉴 권한 관리",
    description:
      "ROLE/ORG/USER 대상을 선택해 메뉴 접근 허용 여부를 matrix로 저장합니다.",
    route: "/admin/menu-permissions",
    menuPath: "시스템 관리 > 역할·권한 관리 > 메뉴 권한 관리",
    endpoint: "/api/admin/menu-permissions",
    method: "PERMISSIONS",
    columns: [
      { key: "targetType", label: "대상유형", tone: "status" },
      { key: "targetId", label: "대상ID", tone: "id" },
      { key: "menuLevel", label: "메뉴 레벨", tone: "status" },
      { key: "menuId", label: "메뉴ID", tone: "id" },
      { key: "menuName", label: "메뉴명" },
      { key: "allowYn", label: "접근 허용", tone: "status" },
    ],
    filters: [
      {
        key: "targetType",
        label: "대상유형",
        kind: "select",
        options: targetTypeOptions,
        required: true,
      },
      { key: "targetId", label: "대상ID", required: true },
    ],
    formTitle: "메뉴 접근권한 matrix",
    formFields: [
      {
        key: "targetType",
        label: "대상유형",
        kind: "select",
        required: true,
        options: targetTypeOptions,
      },
      { key: "targetId", label: "대상ID", required: true },
    ],
    emptyText: "조회된 메뉴 권한이 없습니다.",
    primaryAction: "접근 허용 저장",
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-MENU-STRUCT",
    title: "메뉴 구조 관리",
    description:
      "대메뉴·중메뉴·소메뉴 계층을 확인하고 선택 메뉴의 부모와 표시순서를 조정합니다.",
    route: "/admin/menu-structure",
    menuPath: "시스템 관리 > 메뉴 관리 > 메뉴 구조 관리",
    endpoint: "/api/admin/menus/tree",
    method: "TREE",
    columns: [
      { key: "menuId", label: "메뉴ID", tone: "id" },
      { key: "menuName", label: "메뉴명" },
      { key: "parentMenuId", label: "부모메뉴", tone: "id" },
      { key: "displayOrder", label: "표시순서", tone: "status" },
    ],
    filters: [],
    formTitle: "선택 메뉴 구조 편집",
    formFields: [
      { key: "menuId", label: "메뉴ID", kind: "readonly" },
      { key: "parentMenuId", label: "변경 부모메뉴" },
      {
        key: "orderedMenuIds",
        label: "동일 계층 순서(menuId 쉼표 구분)",
        kind: "textarea",
      },
    ],
    emptyText: "조회된 메뉴 계층이 없습니다.",
    primaryAction: "부모/순서 저장",
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-MENU-INFO",
    title: "메뉴 정보 관리",
    description:
      "메뉴명·화면ID·URL·아이콘·업무구분·설명을 등록·수정해 실행 화면 연결을 관리합니다.",
    route: "/admin/menus",
    menuPath: "시스템 관리 > 메뉴 관리 > 메뉴 정보 관리",
    endpoint: "/api/admin/menus",
    columns: [
      { key: "menuId", label: "메뉴ID", tone: "id" },
      { key: "menuName", label: "메뉴명" },
      { key: "screenId", label: "화면ID", tone: "id" },
      { key: "url", label: "URL", tone: "id" },
      { key: "icon", label: "아이콘" },
      { key: "businessType", label: "업무구분", tone: "status" },
      { key: "description", label: "설명" },
      { key: "useYn", label: "사용여부", tone: "status" },
    ],
    filters: [
      { key: "keyword", label: "메뉴명·업무구분" },
      {
        key: "useYn",
        label: "사용여부",
        kind: "select",
        options: ["", ...useYnOptions],
      },
    ],
    formTitle: "실행정보 등록/수정",
    formFields: [
      { key: "menuId", label: "메뉴ID", kind: "readonly" },
      { key: "menuName", label: "메뉴명", required: true },
      { key: "screenId", label: "화면ID", required: true },
      { key: "url", label: "URL", required: true },
      { key: "icon", label: "아이콘" },
      { key: "businessType", label: "업무구분" },
      { key: "description", label: "설명", kind: "textarea" },
      {
        key: "useYn",
        label: "사용여부",
        kind: "select",
        options: useYnOptions,
      },
    ],
    emptyText: "조회된 메뉴 실행정보가 없습니다.",
    primaryAction: "메뉴 정보 저장",
    supportsCreate: true,
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-CODE-GROUP",
    title: "코드그룹 관리",
    description:
      "코드그룹을 조회·등록·수정하고 선택 그룹의 상세코드 화면으로 이동합니다.",
    route: "/admin/code-groups",
    menuPath: "시스템 관리 > 공통코드 관리 > 코드그룹 관리",
    endpoint: "/api/admin/code-groups",
    columns: [
      { key: "groupId", label: "그룹ID", tone: "id" },
      { key: "groupName", label: "명칭" },
      { key: "description", label: "설명" },
      { key: "managingDepartment", label: "관리부서" },
      { key: "useYn", label: "사용여부", tone: "status" },
    ],
    filters: [
      { key: "keyword", label: "그룹ID·명칭" },
      { key: "filter", label: "관리부서" },
    ],
    formTitle: "코드그룹 등록/수정",
    formFields: [
      { key: "groupId", label: "그룹ID", required: true, readonlyOnEdit: true },
      { key: "groupName", label: "명칭", required: true },
      { key: "description", label: "설명", kind: "textarea" },
      { key: "managingDepartment", label: "관리부서" },
      {
        key: "useYn",
        label: "사용여부",
        kind: "select",
        options: useYnOptions,
      },
    ],
    emptyText: "조회된 코드그룹이 없습니다.",
    primaryAction: "코드그룹 저장",
    supportsCreate: true,
    supportsCancel: true,
  },
  {
    id: "SCR-CMN-DETAIL-CODE",
    title: "상세코드 관리",
    description:
      "선택 코드그룹의 코드값·코드명·상위코드·정렬순서·추가속성을 관리합니다.",
    route: "/admin/detail-codes",
    menuPath: "시스템 관리 > 공통코드 관리 > 상세코드 관리",
    endpoint: "/api/admin/code-groups/{groupId}/detail-codes",
    method: "DETAIL_CODES",
    columns: [
      { key: "groupId", label: "그룹ID", tone: "id" },
      { key: "codeValue", label: "코드값", tone: "id" },
      { key: "codeName", label: "코드명" },
      { key: "parentCodeValue", label: "상위코드", tone: "id" },
      { key: "sortOrder", label: "정렬순서", tone: "status" },
      { key: "additionalAttributes", label: "추가속성" },
      { key: "useYn", label: "사용여부", tone: "status" },
    ],
    filters: [{ key: "groupId", label: "코드그룹", required: true }],
    formTitle: "상세코드 등록/수정",
    formFields: [
      { key: "groupId", label: "그룹ID", kind: "readonly" },
      {
        key: "codeValue",
        label: "코드값",
        required: true,
        readonlyOnEdit: true,
      },
      { key: "codeName", label: "코드명", required: true },
      { key: "parentCodeValue", label: "상위코드" },
      { key: "sortOrder", label: "정렬순서", kind: "number" },
      { key: "additionalAttributes", label: "추가속성", kind: "textarea" },
      {
        key: "useYn",
        label: "사용여부",
        kind: "select",
        options: useYnOptions,
      },
    ],
    emptyText: "선택 코드그룹에 상세코드가 없습니다.",
    primaryAction: "상세코드 저장",
    supportsCreate: true,
    supportsCancel: true,
  },
];

const screenByRoute = (route: string) =>
  screens.find((item) => item.route === route) ?? screens[0];
const valueOf = (row: Row | null, key: string) =>
  row?.[key] == null ? "" : String(row[key]);
const hasPermissionError = (text: string) =>
  /권한|FORBIDDEN|UNAUTHORIZED|인증/.test(text);

async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
    ...init,
  });
  const body = (await res.json()) as ApiEnvelope<T>;
  if (!res.ok || !body.success)
    throw new Error(formatApiErrorMessage(body) || `HTTP ${res.status}`);
  return body.data as T;
}

export function formatApiErrorMessage(body: ApiEnvelope<unknown>): string {
  const meta = body.meta ?? {};
  const base = String(
    body.message ?? meta.message ?? meta.error ?? meta.reason ?? "요청 오류",
  );
  const details = (body.errors ?? [])
    .map((item) => {
      const field = String(item.field ?? item.name ?? item.path ?? "").trim();
      const message = String(
        item.message ?? item.reason ?? item.defaultMessage ?? item.code ?? "",
      ).trim();
      if (!field && !message) return "";
      return field && message ? `${field}: ${message}` : field || message;
    })
    .filter(Boolean);
  return details.length ? `${base} — ${details.join(" / ")}` : base;
}

function extractItems(data: unknown): Row[] {
  const value = data as { items?: Row[]; nodes?: Row[]; permissions?: Row[] };
  return value.items ?? value.nodes ?? value.permissions ?? [];
}

function App() {
  const [route, setRoute] = useState(
    window.location.pathname === "/" ? "/login" : window.location.pathname,
  );
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [sessionState, setSessionState] = useState<UiStatus>("loading");

  const navigate = (next: string) => {
    window.history.pushState(null, "", next);
    setRoute(window.location.pathname);
  };

  useEffect(() => {
    const handler = () => setRoute(window.location.pathname);
    window.addEventListener("popstate", handler);
    api<CurrentUser>("/api/auth/me")
      .then((current) => {
        setUser(current);
        setSessionState("success");
      })
      .catch(() => setSessionState("error"));
    return () => window.removeEventListener("popstate", handler);
  }, []);

  if (route === "/login") {
    return (
      <Login
        onLogin={(u) => {
          setUser(u);
          setSessionState("success");
          navigate("/admin/users");
        }}
      />
    );
  }

  return (
    <Shell
      user={user}
      sessionState={sessionState}
      navigate={navigate}
      onLogout={() =>
        api("/api/auth/logout", { method: "POST" }).finally(() => {
          setUser(null);
          navigate("/login");
        })
      }
      route={route}
    >
      <AdminScreen
        key={route + window.location.search}
        screen={screenByRoute(route)}
        navigate={navigate}
      />
    </Shell>
  );
}

function Login({ onLogin }: { onLogin: (user: CurrentUser) => void }) {
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<UiStatus>("idle");
  const [message, setMessage] = useState(
    "아이디와 password를 입력해 인증 세션을 생성하세요.",
  );
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setStatus("loading");
    setMessage("로그인 요청을 처리 중입니다.");
    try {
      await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ loginId, password }),
      });
      const current = await api<CurrentUser>("/api/auth/me");
      setStatus("success");
      setMessage("로그인되었습니다. 사용자 정보를 확인했습니다.");
      onLogin(current);
    } catch (error) {
      const text =
        error instanceof Error
          ? error.message
          : "로그인 중 오류가 발생했습니다.";
      setStatus(hasPermissionError(text) ? "permission" : "error");
      setMessage(text);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-hero" aria-label="서비스 소개">
        <div className="brand-mark">KNUE</div>
        <p className="eyebrow">FACULTY ACHIEVEMENT CMS</p>
        <h1>교수업적평가시스템 공통기능</h1>
        <p>
          사용자·조직·역할·메뉴·공통코드를 하나의 관리 콘솔에서 안전하게
          운영합니다.
        </p>
        <div className="hero-metrics">
          <span>R09 전용 관리</span>
          <span>세션 기반 보호 API</span>
          <span>PostgreSQL backed</span>
        </div>
      </section>
      <form className="login-card" onSubmit={submit}>
        <p className="eyebrow">인증 &gt; 로그인</p>
        <h2>관리 콘솔 로그인</h2>
        <p className="helper">
          로그인 성공 시 현재 사용자와 권한 메뉴를 확인한 뒤 사용자 관리로
          이동합니다.
        </p>
        <label>
          아이디
          <input
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            autoComplete="username"
            placeholder="아이디 입력"
          />
        </label>
        <label>
          password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            placeholder="password 입력"
          />
        </label>
        <StateNotice status={status} message={message} />
        <button disabled={loading}>
          {loading ? "로그인 중..." : "로그인"}
        </button>
      </form>
    </main>
  );
}

function Shell({
  user,
  sessionState,
  navigate,
  onLogout,
  route,
  children,
}: {
  user: CurrentUser | null;
  sessionState: UiStatus;
  navigate: (route: string) => void;
  onLogout: () => void;
  route: string;
  children: React.ReactNode;
}) {
  const groups = useMemo(() => {
    return screens.reduce<Record<string, Screen[]>>((acc, screen) => {
      const [, group = "기타"] = screen.menuPath.split(" > ");
      acc[group] = [...(acc[group] ?? []), screen];
      return acc;
    }, {});
  }, []);

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-mark small">K</div>
          <div>
            <strong>시스템 관리</strong>
            <span>Common Admin</span>
          </div>
        </div>
        <nav aria-label="관리 메뉴">
          {Object.entries(groups).map(([group, items]) => (
            <div className="nav-group" key={group}>
              <p>{group}</p>
              {items.map((screen) => (
                <button
                  key={screen.route}
                  className={screen.route === route ? "active" : ""}
                  onClick={() => navigate(screen.route)}
                >
                  <span className="nav-icon">{iconFor(screen.route)}</span>
                  {screen.title}
                </button>
              ))}
            </div>
          ))}
        </nav>
      </aside>
      <section className="workspace">
        <header className="topbar">
          <div>
            <strong>한국교원대학교</strong>
            <span>교수업적평가시스템 공통기능</span>
          </div>
          <div className="user-area">
            <span className={`session-dot ${sessionState}`} />
            {user
              ? `${user.loginId} (${user.roleCodes.join(", ")})`
              : "세션 확인 중"}
            <button className="secondary" onClick={onLogout}>
              로그아웃
            </button>
          </div>
        </header>
        {children}
      </section>
    </div>
  );
}

function AdminScreen({
  screen,
  navigate,
}: {
  screen: Screen;
  navigate: (route: string) => void;
}) {
  const initialFilters = getInitialFilters(screen);
  const [filters, setFilters] = useState<Row>(initialFilters);
  const [items, setItems] = useState<Row[]>([]);
  const [selected, setSelected] = useState<Row | null>(null);
  const [form, setForm] = useState<Row>({});
  const [status, setStatus] = useState<UiStatus>("loading");
  const [message, setMessage] = useState("조회 중입니다.");
  const [page, setPage] = useState<PageData | null>(null);
  const [dirtyPermissions, setDirtyPermissions] = useState<Row[] | null>(null);

  const endpoint = useMemo(
    () => buildEndpoint(screen, filters),
    [screen, filters],
  );

  async function load() {
    if (
      screen.method === "DETAIL_CODES" &&
      !String(filters.groupId ?? "").trim()
    ) {
      setItems([]);
      setSelected(null);
      setForm({ groupId: "" });
      setPage(null);
      setStatus("empty");
      setMessage("코드그룹을 선택하면 상세코드를 조회합니다.");
      return;
    }
    setStatus("loading");
    setMessage("API에서 데이터를 조회하는 중입니다.");
    try {
      const data = await api<unknown>(endpoint);
      const rows = extractItems(data);
      const pageData = data as PageData;
      setItems(rows);
      setDirtyPermissions(null);
      setPage(pageData.items ? pageData : null);
      const nextSelected = rows[0] ?? null;
      setSelected(nextSelected);
      setForm(rowToForm(screen, nextSelected, filters));
      setStatus(rows.length ? "success" : "empty");
      setMessage(
        rows.length ? `${rows.length}건을 조회했습니다.` : screen.emptyText,
      );
    } catch (error) {
      const text =
        error instanceof Error ? error.message : "조회 중 오류가 발생했습니다.";
      setStatus(hasPermissionError(text) ? "permission" : "error");
      setMessage(text);
    }
  }

  useEffect(() => {
    load();
  }, [screen.route]);

  function selectRow(row: Row) {
    setSelected(row);
    setForm(rowToForm(screen, row, filters));
  }

  function resetForm() {
    setSelected(null);
    setForm(emptyForm(screen, filters));
    setMessage("신규 입력 또는 행 선택 후 저장할 수 있습니다.");
  }

  async function save() {
    setStatus("loading");
    setMessage("저장 요청을 처리 중입니다.");
    try {
      await saveScreen(
        screen,
        selected,
        form,
        filters,
        dirtyPermissions ?? items,
      );
      setStatus("success");
      setMessage("저장되었습니다. 같은 조건으로 목록을 재조회합니다.");
      await load();
    } catch (error) {
      const text =
        error instanceof Error ? error.message : "저장 중 오류가 발생했습니다.";
      setStatus(hasPermissionError(text) ? "permission" : "error");
      setMessage(text);
    }
  }

  async function revoke() {
    if (!selected?.assignmentId) return;
    setStatus("loading");
    setMessage("역할 회수 요청을 처리 중입니다.");
    try {
      await api(`/api/admin/user-roles/${selected.assignmentId}`, {
        method: "DELETE",
      });
      setStatus("success");
      setMessage("회수되었습니다. 현재 역할 목록을 재조회합니다.");
      await load();
    } catch (error) {
      const text =
        error instanceof Error ? error.message : "회수 중 오류가 발생했습니다.";
      setStatus(hasPermissionError(text) ? "permission" : "error");
      setMessage(text);
    }
  }

  function updatePermission(row: Row, allowYn: string) {
    const source = dirtyPermissions ?? items;
    const next = source.map((item) =>
      item.menuId === row.menuId ? { ...item, allowYn, changed: true } : item,
    );
    setDirtyPermissions(next);
    setItems(next);
    setSelected({ ...row, allowYn });
    setForm(rowToForm(screen, { ...row, allowYn }, filters));
    setMessage("저장 전 변경 행이 강조됩니다.");
  }

  const total = page?.totalElements ?? items.length;
  const isPermission = status === "permission";
  const isLoading = status === "loading";

  return (
    <main className="screen">
      <section className="page-hero">
        <div>
          <p className="breadcrumb">{screen.menuPath}</p>
          <p className="eyebrow">{screen.id}</p>
          <h1>{screen.title}</h1>
          <p>{screen.description}</p>
        </div>
        <div className="hero-actions">
          {screen.supportsCreate && (
            <button className="secondary" onClick={resetForm}>
              신규
            </button>
          )}
          <button onClick={load}>
            {screen.method === "TREE" ? "새로고침" : "조회"}
          </button>
        </div>
      </section>

      <section className="search-panel">
        <div className="section-heading">
          <span className="icon-capsule">⌕</span>
          <div>
            <h2>조회 조건</h2>
            <p>선택 필터는 값이 있을 때만 query로 전달합니다.</p>
          </div>
        </div>
        <div className="filter-grid">
          {screen.filters.length ? (
            screen.filters.map((field) => (
              <FieldControl
                key={field.key}
                field={field}
                value={String(filters[field.key] ?? "")}
                onChange={(value) =>
                  setFilters((prev) => ({ ...prev, [field.key]: value }))
                }
              />
            ))
          ) : (
            <span className="helper">
              트리 조회 화면입니다. 새로고침으로 최신 계층을 확인합니다.
            </span>
          )}
        </div>
      </section>

      <StateNotice status={status} message={message} />

      <section className="insight-row">
        <InfoCard
          label="조회 건수"
          value={`${total}건`}
          helper="API response totalElements 기준"
        />
        <InfoCard
          label="현재 상태"
          value={stateLabel(status)}
          helper="loading/error/empty/permission/success"
        />
        <InfoCard
          label="주요 CTA"
          value={screen.primaryAction}
          helper="저장 성공 후 같은 조건 재조회"
        />
      </section>

      <section className="content-grid">
        <div className="card table-card">
          <div className="card-header-row">
            <div>
              <h2>{screen.title} 목록</h2>
              <p>{screen.emptyText}</p>
            </div>
            <span className="badge neutral">API-backed</span>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {screen.columns.map((column) => (
                    <th key={column.key}>{column.label}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {isLoading && <SkeletonRows columns={screen.columns.length} />}
                {!isLoading &&
                  items.map((row, index) => (
                    <tr
                      key={`${screen.route}-${index}-${valueOf(row, screen.columns[0].key)}`}
                      onClick={() => selectRow(row)}
                      className={`${selected === row ? "selected" : ""} ${row.changed ? "changed" : ""}`}
                    >
                      {screen.columns.map((column) => (
                        <td key={column.key}>
                          {screen.method === "PERMISSIONS" &&
                          column.key === "allowYn" ? (
                            <select
                              value={String(row.allowYn ?? "N")}
                              onChange={(event) =>
                                updatePermission(row, event.target.value)
                              }
                              onClick={(event) => event.stopPropagation()}
                              disabled={isPermission}
                            >
                              {useYnOptions.map((option) => (
                                <option key={option}>{option}</option>
                              ))}
                            </select>
                          ) : (
                            <CellValue
                              value={row[column.key]}
                              column={column}
                            />
                          )}
                        </td>
                      ))}
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
          {!isLoading && status === "empty" && (
            <EmptyState
              title={screen.emptyText}
              actionLabel="다시 조회"
              onAction={load}
            />
          )}
          {!isLoading && status === "permission" && (
            <EmptyState
              title="권한이 필요합니다"
              description="R09 역할 및 menu_permission allow_yn=Y 조건을 확인하세요."
            />
          )}
        </div>

        <div className="detail-stack">
          {screen.readonlyFields && (
            <div className="card readonly-card">
              <h2>KORUS 원천정보</h2>
              <p>직접 수정하지 않고 조회 전용으로 표시합니다.</p>
              <div className="readonly-grid">
                {screen.readonlyFields.map((field) => (
                  <div key={field.key}>
                    <span>{field.label}</span>
                    <strong>{valueOf(selected, field.key) || "-"}</strong>
                  </div>
                ))}
              </div>
            </div>
          )}
          <div className="card detail">
            <div className="card-header-row">
              <div>
                <h2>{screen.formTitle}</h2>
                <p>
                  {selected
                    ? "선택 행을 편집합니다."
                    : screen.supportsCreate
                      ? "신규 데이터를 입력합니다."
                      : "행을 선택하면 편집값이 채워집니다."}
                </p>
              </div>
              {selected && <span className="badge success">selected</span>}
            </div>
            <div className="form-grid">
              {screen.formFields.map((field) => (
                <FieldControl
                  key={field.key}
                  field={{
                    ...field,
                    kind:
                      field.readonlyOnEdit && selected
                        ? "readonly"
                        : field.kind,
                  }}
                  value={String(form[field.key] ?? "")}
                  onChange={(value) =>
                    setForm((prev) => ({ ...prev, [field.key]: value }))
                  }
                />
              ))}
            </div>
            <div className="actions">
              <button onClick={save} disabled={isPermission || isLoading}>
                {screen.primaryAction}
              </button>
              {screen.supportsDelete && Boolean(selected?.assignmentId) && (
                <button
                  className="danger"
                  onClick={revoke}
                  disabled={isPermission || isLoading}
                >
                  회수
                </button>
              )}
              {screen.supportsCancel && (
                <button
                  className="secondary"
                  onClick={() => {
                    setForm(rowToForm(screen, selected, filters));
                    setMessage("편집값을 초기화했습니다.");
                  }}
                >
                  취소
                </button>
              )}
              {screen.route === "/admin/code-groups" && (
                <button
                  className="secondary"
                  onClick={() =>
                    selected?.groupId
                      ? navigate(
                          `/admin/detail-codes?groupId=${encodeURIComponent(String(selected.groupId))}`,
                        )
                      : setMessage("상세코드 이동은 groupId 선택이 필요합니다.")
                  }
                >
                  상세코드 이동
                </button>
              )}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}

function FieldControl({
  field,
  value,
  onChange,
}: {
  field: Field;
  value: string;
  onChange: (value: string) => void;
}) {
  const kind = field.kind ?? "text";
  return (
    <label className={kind === "textarea" ? "span-2" : undefined}>
      {field.label}
      {field.required && <em> *</em>}
      {kind === "select" ? (
        <select value={value} onChange={(e) => onChange(e.target.value)}>
          {(field.options ?? []).map((option) => (
            <option key={option} value={option}>
              {option || "전체"}
            </option>
          ))}
        </select>
      ) : kind === "textarea" ? (
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          rows={3}
        />
      ) : (
        <input
          type={
            kind === "number" ? "number" : kind === "date" ? "date" : "text"
          }
          value={value}
          onChange={(e) => onChange(e.target.value)}
          readOnly={kind === "readonly"}
          placeholder={`${field.label} 입력`}
        />
      )}
    </label>
  );
}

function CellValue({ value, column }: { value: unknown; column: Column }) {
  const text = value == null || value === "" ? "-" : String(value);
  if (column.tone === "status" || column.tone === "role")
    return <span className={`badge ${badgeTone(text)}`}>{text}</span>;
  if (column.tone === "id") return <code>{text}</code>;
  if (column.tone === "date")
    return <span className="muted">{text.replace("T", " ")}</span>;
  return <span>{text}</span>;
}

function StateNotice({
  status,
  message,
}: {
  status: UiStatus;
  message: string;
}) {
  return (
    <p className={`state ${status}`}>
      상태: {stateLabel(status)} — {message}
    </p>
  );
}

function EmptyState({
  title,
  description,
  actionLabel,
  onAction,
}: {
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">∅</div>
      <h3>{title}</h3>
      <p>{description ?? "조회 조건을 변경하거나 새로고침해 주세요."}</p>
      {actionLabel && onAction && (
        <button className="secondary" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}

function InfoCard({
  label,
  value,
  helper,
}: {
  label: string;
  value: string;
  helper: string;
}) {
  return (
    <div className="info-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{helper}</p>
    </div>
  );
}

function SkeletonRows({ columns }: { columns: number }) {
  return (
    <>
      {Array.from({ length: 5 }).map((_, row) => (
        <tr key={row}>
          {Array.from({ length: columns }).map((__, col) => (
            <td key={col}>
              <span className="skeleton" />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

function getInitialFilters(screen: Screen): Row {
  if (screen.method === "PERMISSIONS")
    return { targetType: "ROLE", targetId: "R09" };
  if (screen.method === "DETAIL_CODES")
    return {
      groupId: new URLSearchParams(window.location.search).get("groupId") ?? "",
    };
  return {};
}

export function buildEndpoint(screen: Screen, filters: Row) {
  if (screen.method === "PERMISSIONS") {
    const targetType = encodeURIComponent(String(filters.targetType || "ROLE"));
    const targetId = encodeURIComponent(String(filters.targetId || "R09"));
    return `${screen.endpoint}?targetType=${targetType}&targetId=${targetId}`;
  }
  if (screen.method === "DETAIL_CODES") {
    return `/api/admin/code-groups/${encodeURIComponent(String(filters.groupId ?? ""))}/detail-codes`;
  }
  const params = new URLSearchParams();
  for (const key of ["keyword", "filter", "useYn"]) {
    const value = String(filters[key] ?? "").trim();
    if (value) params.set(key, value);
  }
  return params.toString()
    ? `${screen.endpoint}?${params.toString()}`
    : screen.endpoint;
}

function emptyForm(screen: Screen, filters: Row): Row {
  const base: Row = {};
  for (const field of screen.formFields)
    base[field.key] =
      field.key === "groupId" ? String(filters.groupId ?? "") : "";
  if (screen.route === "/admin/user-roles") base.assignmentSource = "MANUAL";
  if (
    ["/admin/menus", "/admin/code-groups", "/admin/detail-codes"].includes(
      screen.route,
    )
  )
    base.useYn = "Y";
  return base;
}

export function rowToForm(screen: Screen, row: Row | null, filters: Row): Row {
  if (!row) return emptyForm(screen, filters);
  if (screen.route === "/admin/users")
    return {
      systemUseYn: valueOf(row, "useYn"),
      businessRole: valueOf(row, "role"),
    };
  if (screen.route === "/admin/menu-structure")
    return { ...row, orderedMenuIds: "" };
  if (screen.method === "DETAIL_CODES")
    return {
      ...row,
      groupId: valueOf(row, "groupId") || String(filters.groupId ?? ""),
      additionalAttributes: formatAdditionalAttributes(
        row.additionalAttributes,
      ),
    };
  return { ...row };
}

export async function saveScreen(
  screen: Screen,
  selected: Row | null,
  form: Row,
  filters: Row,
  rows: Row[],
) {
  const body = compact(form);
  if (screen.route === "/admin/users") {
    if (!selected?.userId) throw new Error("userId는 필수입니다.");
    return api(`/api/admin/users/${selected.userId}/system-access`, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
  }
  if (screen.route === "/admin/organizations") {
    const orgCode = selected?.orgCode ?? form.orgCode;
    if (!orgCode) throw new Error("orgCode는 필수입니다.");
    return api(`/api/admin/organizations/${orgCode}/relationship`, {
      method: "PUT",
      body: JSON.stringify({
        parentOrgCode: form.parentOrgCode,
        effectiveStartDate: form.effectiveStartDate,
        effectiveEndDate: form.effectiveEndDate,
      }),
    });
  }
  if (screen.route === "/admin/roles") {
    if (!selected?.roleCode) throw new Error("roleCode는 필수입니다.");
    return api(`/api/admin/roles/${selected.roleCode}`, {
      method: "PUT",
      body: JSON.stringify({
        roleName: form.roleName,
        grantCriteria: form.grantCriteria,
        defaultDataScope: form.defaultDataScope,
      }),
    });
  }
  if (screen.route === "/admin/user-roles") {
    const assignmentId = selected?.assignmentId || form.assignmentId;
    const payload = assignmentId
      ? {
          roleCode: form.roleCode,
          approverId: form.approverId,
          validFrom: form.validFrom,
          validTo: form.validTo,
        }
      : {
          userId: form.userId,
          roleCode: form.roleCode,
          assignmentSource: form.assignmentSource || "MANUAL",
          approverId: form.approverId,
          validFrom: form.validFrom,
          validTo: form.validTo,
        };
    return api(
      assignmentId
        ? `/api/admin/user-roles/${assignmentId}`
        : "/api/admin/user-roles",
      {
        method: assignmentId ? "PATCH" : "POST",
        body: JSON.stringify(payload),
      },
    );
  }
  if (screen.route === "/admin/menu-permissions") {
    const targetType = String(filters.targetType || form.targetType || "ROLE");
    const targetId = String(filters.targetId || form.targetId || "R09");
    return api("/api/admin/menu-permissions", {
      method: "PUT",
      body: JSON.stringify({
        targetType,
        targetId,
        permissions: rows.map((row) => ({
          menuId: row.menuId,
          allowYn: row.allowYn,
        })),
      }),
    });
  }
  if (screen.route === "/admin/menu-structure") {
    if (!selected?.menuId) throw new Error("menuId는 필수입니다.");
    const parentValue = String(form.parentMenuId ?? "").trim();
    const currentParentValue = String(selected.parentMenuId ?? "").trim();
    if (parentValue !== currentParentValue)
      await api(`/api/admin/menus/${selected.menuId}/structure`, {
        method: "PATCH",
        body: JSON.stringify({ parentMenuId: parentValue || null }),
      });
    const orderedMenuIds = String(form.orderedMenuIds ?? "")
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
    if (orderedMenuIds.length)
      return api("/api/admin/menus/order", {
        method: "PATCH",
        body: JSON.stringify({
          parentMenuId: selected.parentMenuId ?? null,
          orderedMenuIds,
        }),
      });
    return selected;
  }
  if (screen.route === "/admin/menus") {
    const method = selected?.menuId ? "PUT" : "POST";
    const path = selected?.menuId
      ? `/api/admin/menus/${selected.menuId}`
      : "/api/admin/menus";
    return api(path, { method, body: JSON.stringify(body) });
  }
  if (screen.route === "/admin/code-groups") {
    const method = selected?.groupId ? "PUT" : "POST";
    const path = selected?.groupId
      ? `/api/admin/code-groups/${selected.groupId}`
      : "/api/admin/code-groups";
    return api(path, { method, body: JSON.stringify(body) });
  }
  if (screen.route === "/admin/detail-codes") {
    const groupId = String(filters.groupId || form.groupId || "");
    if (!groupId) throw new Error("groupId는 필수입니다.");
    const method = selected?.codeValue ? "PUT" : "POST";
    const path = selected?.codeValue
      ? `/api/admin/code-groups/${groupId}/detail-codes/${selected.codeValue}`
      : `/api/admin/code-groups/${groupId}/detail-codes`;
    return api(path, {
      method,
      body: JSON.stringify(normalizeDetailCodeBody(body)),
    });
  }
}

function formatAdditionalAttributes(value: unknown): string {
  if (value == null || value === "") return "";
  if (typeof value === "string") return value;
  return JSON.stringify(value);
}

function normalizeDetailCodeBody(body: Row): Row {
  const next = { ...body };
  if (typeof next.sortOrder === "string" && next.sortOrder.trim()) {
    next.sortOrder = Number(next.sortOrder);
  }
  if (typeof next.additionalAttributes === "string") {
    const text = next.additionalAttributes.trim();
    if (text) next.additionalAttributes = JSON.parse(text) as Row;
    else delete next.additionalAttributes;
  }
  return next;
}

function compact(row: Row): Row {
  return Object.fromEntries(
    Object.entries(row).filter(
      ([, value]) => value !== undefined && value !== null,
    ),
  );
}

function stateLabel(status: UiStatus) {
  return {
    idle: "대기",
    loading: "로딩",
    empty: "빈 결과",
    error: "오류",
    permission: "권한 필요",
    success: "성공",
  }[status];
}

function badgeTone(text: string) {
  if (["Y", "ACTIVE", "MANUAL", "R09"].includes(text)) return "success";
  if (["N", "REVOKED"].includes(text)) return "danger";
  if (
    ["POSITION", "ROOT", "GROUP", "SCREEN"].includes(text) ||
    /^R0/.test(text)
  )
    return "info";
  return "neutral";
}

function iconFor(route: string) {
  if (route.includes("users")) return "👥";
  if (route.includes("organizations")) return "▦";
  if (route.includes("role")) return "🛡";
  if (route.includes("permission")) return "🔐";
  if (route.includes("menu")) return "☰";
  if (route.includes("code")) return "{}";
  return "•";
}

const root = document.getElementById("root");
if (root) createRoot(root).render(<App />);
