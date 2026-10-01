import { describe, expect, it, vi } from "vitest";
import React from "react";
import { render, screen } from "@testing-library/react";

import {
  App,
  buildDataScopeExportEndpoint,
  buildEndpoint,
  buildExportEndpoint,
  formatApiErrorMessage,
  rowToForm,
  saveScreen,
  screens,
} from "./main";

describe("frontend contract smoke", () => {
  it("renders Korean application title in a React component", () => {
    render(<h1>교수업적평가시스템 공통기능</h1>);
    expect(screen.getByText("교수업적평가시스템 공통기능")).toBeInTheDocument();
  });
});

describe("UI contract drift guards", () => {
  it("binds 사용자 관리 교번 to OpenAPI userId instead of an unmapped facultyNo field", () => {
    const userScreen = screens.find(
      (screen) => screen.route === "/admin/users",
    );

    expect(userScreen?.columns[0]).toMatchObject({
      key: "userId",
      label: "교번",
    });
    expect(userScreen?.readonlyFields?.[0]).toMatchObject({
      key: "userId",
      label: "교번",
    });
  });

  it("preserves field-level ApiError details in the status message", () => {
    expect(
      formatApiErrorMessage({
        success: false,
        meta: { message: "요청 오류" },
        errors: [
          { field: "roleCode", message: "필수입니다" },
          { field: "validFrom", reason: "기간 오류" },
        ],
      }),
    ).toBe("요청 오류 — roleCode: 필수입니다 / validFrom: 기간 오류");
  });

  it("does not send create-only user role identity fields when updating an assignment", async () => {
    const userRoleScreen = screens.find(
      (screen) => screen.route === "/admin/user-roles",
    );
    if (!userRoleScreen) throw new Error("user role screen missing");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      userRoleScreen,
      { assignmentId: "A-1", userId: "U-1", assignmentSource: "POSITION" },
      {
        assignmentId: "A-1",
        userId: "U-2",
        roleCode: "R03",
        assignmentSource: "MANUAL",
        approverId: "APR-1",
        validFrom: "2026-09-01",
        validTo: "2026-12-31",
      },
      {},
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(fetchMock.mock.calls[0][0]).toBe("/api/admin/user-roles/A-1");
    expect(init.method).toBe("PATCH");
    expect(JSON.parse(String(init.body))).toEqual({
      roleCode: "R03",
      approverId: "APR-1",
      validFrom: "2026-09-01",
      validTo: "2026-12-31",
    });
  });

  it("serializes detail code additionalAttributes as an object payload and form textarea JSON", async () => {
    const detailScreen = screens.find(
      (screen) => screen.route === "/admin/detail-codes",
    );
    if (!detailScreen) throw new Error("detail code screen missing");

    expect(
      rowToForm(
        detailScreen,
        {
          groupId: "ROLE_TYPE",
          codeValue: "R09",
          codeName: "시스템 관리자",
          additionalAttributes: { scope: "ALL" },
        },
        { groupId: "ROLE_TYPE" },
      ).additionalAttributes,
    ).toBe('{"scope":"ALL"}');

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      detailScreen,
      null,
      {
        groupId: "ROLE_TYPE",
        codeValue: "R09",
        codeName: "시스템 관리자",
        sortOrder: "9",
        additionalAttributes: '{"scope":"ALL"}',
      },
      { groupId: "ROLE_TYPE" },
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(JSON.parse(String(init.body))).toMatchObject({
      groupId: "ROLE_TYPE",
      codeValue: "R09",
      codeName: "시스템 관리자",
      sortOrder: 9,
      additionalAttributes: { scope: "ALL" },
    });
  });

  it("passes selected useYn filters as dynamic query parameters", () => {
    const menuScreen = screens.find(
      (screen) => screen.route === "/admin/menus",
    );
    if (!menuScreen) throw new Error("menu screen missing");

    expect(buildEndpoint(menuScreen, { keyword: "권한", useYn: "N" })).toBe(
      "/api/admin/menus?keyword=%EA%B6%8C%ED%95%9C&useYn=N",
    );
  });

  it("adds common settings as UI-006 modal-backed screen using the relative API contract", () => {
    const commonSettingScreen = screens.find(
      (screen) => screen.route === "/admin/common-settings",
    );

    expect(commonSettingScreen).toMatchObject({
      id: "UI-006",
      title: "공통 환경설정",
      endpoint: "/api/admin/common-settings",
      detailMode: "modal",
      primaryAction: "설정값 저장",
    });
    expect(commonSettingScreen?.columns.map((column) => column.key)).toEqual([
      "settingKey",
      "settingName",
      "settingValue",
      "valueUnit",
      "description",
    ]);
  });

  it("serializes common setting modal saves to PATCH /api/admin/common-settings/{settingKey}", async () => {
    const commonSettingScreen = screens.find(
      (screen) => screen.route === "/admin/common-settings",
    );
    if (!commonSettingScreen) throw new Error("common setting screen missing");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      commonSettingScreen,
      { settingKey: "PAGE_SIZE", settingValue: "20" },
      { settingKey: "PAGE_SIZE", settingValue: "50", valueUnit: "ROW" },
      {},
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/admin/common-settings/PAGE_SIZE",
    );
    expect(init.method).toBe("PATCH");
    expect(JSON.parse(String(init.body))).toEqual({ settingValue: "50" });
  });

  it("adds batch operation screens using relative admin API routes", () => {
    expect(
      screens
        .filter((screen) => screen.route.includes("/admin/batch"))
        .map((screen) => [screen.route, screen.endpoint]),
    ).toEqual([
      ["/admin/batch-definitions", "/api/admin/batch-definitions"],
      ["/admin/batch-executions", "/api/admin/batch-executions"],
      ["/admin/batch-results", "/api/admin/batch-results"],
      ["/admin/batch-reprocess", "/api/admin/batch-reprocess-targets"],
    ]);
  });

  it("serializes batch definition saves with parsed JSON parameters and numeric timeout", async () => {
    const batchScreen = screens.find(
      (screen) => screen.route === "/admin/batch-definitions",
    );
    if (!batchScreen) throw new Error("batch definition screen missing");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      batchScreen,
      null,
      {
        batchId: "BATCH-TEST",
        batchType: "SYSTEM",
        schedule: "0 4 * * *",
        executionParameters: '{"mode":"TEST"}',
        maxExecutionSeconds: "600",
        ownerUserId: "U-ADMIN",
      },
      {},
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(fetchMock.mock.calls[0][0]).toBe("/api/admin/batch-definitions");
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toMatchObject({
      executionParameters: { mode: "TEST" },
      maxExecutionSeconds: 600,
    });
  });

  it("T001 adds 보직 관리 browser route and relative admin API bindings", async () => {
    const positionScreen = screens.find(
      (screen) => screen.route === "/admin/positions",
    );

    expect(positionScreen).toMatchObject({
      id: "SCR-CMN-POSITION",
      title: "보직 관리",
      endpoint: "/api/admin/positions",
      menuPath: "시스템 관리 > 사용자·조직 관리 > 보직 관리",
      primaryAction: "보직 저장",
      supportsExport: true,
    });
    expect(positionScreen?.columns.map((column) => column.key)).toEqual([
      "positionAssignmentId",
      "positionCode",
      "userId",
      "userName",
      "organizationCode",
      "organizationName",
      "validFrom",
      "validTo",
    ]);
    expect(buildEndpoint(positionScreen!, { keyword: "학과장" })).toBe(
      "/api/admin/positions?keyword=%ED%95%99%EA%B3%BC%EC%9E%A5",
    );
    expect(buildExportEndpoint(positionScreen!, { keyword: "학과장" })).toBe(
      "/api/admin/positions/export?keyword=%ED%95%99%EA%B3%BC%EC%9E%A5",
    );

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      positionScreen!,
      { positionAssignmentId: "PA-1" },
      {
        positionCode: "DEPT_HEAD",
        userId: "U-F1002",
        organizationCode: "CSE",
        validFrom: "2026-10-01",
        validTo: "2026-12-31",
      },
      {},
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(fetchMock.mock.calls[0][0]).toBe("/api/admin/positions/PA-1");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toMatchObject({
      positionCode: "DEPT_HEAD",
      userId: "U-F1002",
      organizationCode: "CSE",
      validFrom: "2026-10-01",
      validTo: "2026-12-31",
    });
  });

  it("T009 renders 보직 관리 loading empty error permission success states from the browser route", async () => {
    window.history.pushState(null, "", "/admin/positions");
    let listResolver: (value: unknown) => void = () => undefined;
    const authResponse = {
      ok: true,
      json: async () => ({
        success: true,
        data: {
          userId: "U-ADMIN",
          loginId: "admin",
          roleCodes: ["R09"],
          menuPaths: ["/admin/positions"],
        },
        meta: {},
      }),
    };
    const fetchMock = vi.fn((url: string) => {
      if (String(url).includes("/api/admin/positions")) {
        return new Promise((resolve) => {
          listResolver = resolve;
        });
      }
      return Promise.resolve(authResponse);
    });
    vi.stubGlobal("fetch", fetchMock);

    const firstRender = render(<App />);
    expect(await screen.findByText(/상태: 로딩/)).toBeInTheDocument();
    listResolver({
      ok: true,
      json: async () => ({
        success: true,
        data: { items: [], page: 0, size: 20, totalElements: 0 },
        meta: {},
      }),
    });
    expect(
      await screen.findByText(/조회된 보직 지정이 없습니다/),
    ).toBeInTheDocument();
    firstRender.unmount();

    fetchMock.mockReset();
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes("/api/admin/positions"))
        return Promise.resolve({
          ok: false,
          json: async () => ({
            success: false,
            message: "서버 오류",
            meta: {},
          }),
        });
      return Promise.resolve(authResponse);
    });
    const errorRender = render(<App />);
    expect(
      await screen.findByText(/상태: 오류 — 서버 오류/),
    ).toBeInTheDocument();
    errorRender.unmount();

    fetchMock.mockReset();
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes("/api/admin/positions"))
        return Promise.resolve({
          ok: false,
          json: async () => ({
            success: false,
            message: "권한 없음",
            meta: {},
          }),
        });
      return Promise.resolve(authResponse);
    });
    const permissionRender = render(<App />);
    expect(
      await screen.findByText(/상태: 권한 필요 — 권한 없음/),
    ).toBeInTheDocument();
    permissionRender.unmount();

    fetchMock.mockReset();
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes("/api/admin/positions"))
        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            data: {
              items: [
                {
                  positionAssignmentId: "POS-1",
                  positionCode: "DEPT_HEAD",
                  userId: "U-F1002",
                  userName: "이학과장",
                  organizationCode: "CSE",
                  organizationName: "컴퓨터교육과",
                  validFrom: "2026-10-01",
                  validTo: "2026-12-31",
                },
              ],
              page: 0,
              size: 20,
              totalElements: 1,
            },
            meta: {},
          }),
        });
      return Promise.resolve(authResponse);
    });
    render(<App />);
    expect(
      await screen.findByText(/상태: 성공 — 1건을 조회했습니다/),
    ).toBeInTheDocument();
    expect(screen.getAllByText("DEPT_HEAD").length).toBeGreaterThan(0);
  });

  it("T010 sends baseDate only when 보직 기준일 유효 조회 is requested", () => {
    const positionScreen = screens.find(
      (screen) => screen.route === "/admin/positions",
    );
    if (!positionScreen) throw new Error("position screen missing");

    expect(
      buildEndpoint(positionScreen, {
        keyword: "학과장",
        baseDate: "2026-10-01",
      }),
    ).toBe("/api/admin/positions/effective?baseDate=2026-10-01");
  });

  it("T002 adds 업무담당자 관리 browser route and assignment save binding", async () => {
    const ownerScreen = screens.find(
      (screen) => screen.route === "/admin/business-owners",
    );

    expect(ownerScreen).toMatchObject({
      id: "SCR-CMN-BUSINESS-OWNER",
      title: "업무담당자 관리",
      endpoint: "/api/admin/business-owners",
      menuPath: "시스템 관리 > 사용자·조직 관리 > 업무담당자 관리",
      primaryAction: "담당자 지정 저장",
      supportsExport: true,
    });
    expect(ownerScreen?.formFields.map((field) => field.key)).toEqual([
      "businessOwnerAssignmentId",
      "businessOrganizationCode",
      "userId",
      "workArea",
      "assignedFrom",
      "assignedTo",
      "dataScope",
      "processPermission",
    ]);
    expect(buildExportEndpoint(ownerScreen!, { keyword: "담당자" })).toBe(
      "/api/admin/business-owners/export?keyword=%EB%8B%B4%EB%8B%B9%EC%9E%90",
    );

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      ownerScreen!,
      null,
      {
        businessOrganizationCode: "COLLEGE-EDU",
        userId: "U-F1002",
        workArea: "ACHIEVEMENT_REVIEW",
        assignedFrom: "2026-10-01",
        assignedTo: "2026-12-31",
        dataScope: "COLLEGE",
        processPermission: "APPROVE",
      },
      {},
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(fetchMock.mock.calls[0][0]).toBe("/api/admin/business-owners");
    expect(init.method).toBe("POST");
  });

  it("T012 renders 업무담당자 관리 loading empty error permission success states from the browser route", async () => {
    window.history.pushState(null, "", "/admin/business-owners");
    let listResolver: (value: unknown) => void = () => undefined;
    const authResponse = {
      ok: true,
      json: async () => ({
        success: true,
        data: {
          userId: "U-ADMIN",
          loginId: "admin",
          roleCodes: ["R09"],
          menuPaths: ["/admin/business-owners"],
        },
        meta: {},
      }),
    };
    const fetchMock = vi.fn((url: string) => {
      if (String(url).includes("/api/admin/business-owners")) {
        return new Promise((resolve) => {
          listResolver = resolve;
        });
      }
      return Promise.resolve(authResponse);
    });
    vi.stubGlobal("fetch", fetchMock);

    const firstRender = render(<App />);
    expect(await screen.findByText(/상태: 로딩/)).toBeInTheDocument();
    listResolver({
      ok: true,
      json: async () => ({
        success: true,
        data: { items: [], page: 0, size: 20, totalElements: 0 },
        meta: {},
      }),
    });
    expect(
      await screen.findByText(/조회된 업무담당자 지정이 없습니다/),
    ).toBeInTheDocument();
    firstRender.unmount();

    fetchMock.mockReset();
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes("/api/admin/business-owners"))
        return Promise.resolve({
          ok: false,
          json: async () => ({
            success: false,
            message: "서버 오류",
            meta: {},
          }),
        });
      return Promise.resolve(authResponse);
    });
    const errorRender = render(<App />);
    expect(
      await screen.findByText(/상태: 오류 — 서버 오류/),
    ).toBeInTheDocument();
    errorRender.unmount();

    fetchMock.mockReset();
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes("/api/admin/business-owners"))
        return Promise.resolve({
          ok: false,
          json: async () => ({
            success: false,
            message: "권한 없음",
            meta: {},
          }),
        });
      return Promise.resolve(authResponse);
    });
    const permissionRender = render(<App />);
    expect(
      await screen.findByText(/상태: 권한 필요 — 권한 없음/),
    ).toBeInTheDocument();
    permissionRender.unmount();

    fetchMock.mockReset();
    fetchMock.mockImplementation((url: string) => {
      if (String(url).includes("/api/admin/business-owners"))
        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            data: {
              items: [
                {
                  businessOwnerAssignmentId: "BOA-1",
                  businessOrganizationCode: "EDU",
                  businessOrganizationName: "교육대학",
                  userId: "U-F1001",
                  userName: "김교원",
                  workArea: "FACULTY_EVALUATION",
                  assignedFrom: "2026-01-01",
                  assignedTo: "2026-12-31",
                  dataScope: "COLLEGE",
                  processPermission: "WRITE",
                },
              ],
              page: 0,
              size: 20,
              totalElements: 1,
            },
            meta: {},
          }),
        });
      return Promise.resolve(authResponse);
    });
    render(<App />);
    expect(
      await screen.findByText(/상태: 성공 — 1건을 조회했습니다/),
    ).toBeInTheDocument();
    expect(screen.getByText("FACULTY_EVALUATION")).toBeInTheDocument();
  });

  it("T003 adds 데이터 범위 권한 browser route and role-scoped save binding", async () => {
    const dataScopeScreen = screens.find(
      (screen) => screen.route === "/admin/data-scopes",
    );

    expect(dataScopeScreen).toMatchObject({
      id: "SCR-CMN-DATA-SCOPE",
      title: "데이터 범위 권한",
      endpoint: "/api/admin/data-scopes",
      menuPath: "시스템 관리 > 역할·권한 관리 > 데이터 범위 권한",
      primaryAction: "데이터 범위 저장",
    });
    expect(dataScopeScreen?.formFields.map((field) => field.key)).toEqual([
      "roleCode",
      "dataScopeType",
      "organizationCode",
      "workArea",
    ]);

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      dataScopeScreen!,
      { roleCode: "R03" },
      {
        roleCode: "R03",
        dataScopeType: "COLLEGE",
        organizationCode: "COLLEGE-EDU",
        workArea: "ACHIEVEMENT_REVIEW",
      },
      {},
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(fetchMock.mock.calls[0][0]).toBe("/api/admin/data-scopes/R03");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      dataScopeType: "COLLEGE",
      organizationCode: "COLLEGE-EDU",
      workArea: "ACHIEVEMENT_REVIEW",
    });
  });
  it("adds data scope permission screen and saves selected role scope with relative API", async () => {
    const dataScopeScreen = screens.find(
      (screen) => screen.route === "/admin/data-scopes",
    );
    if (!dataScopeScreen) throw new Error("data scope screen missing");

    expect(dataScopeScreen).toMatchObject({
      id: "SCR-CMN-DATA-SCOPE",
      endpoint: "/api/admin/data-scopes",
      primaryAction: "데이터 범위 저장",
      supportsExport: true,
    });
    expect(buildEndpoint(dataScopeScreen, { keyword: "PHASE5_SCOPE" })).toBe(
      "/api/admin/data-scopes?keyword=PHASE5_SCOPE",
    );
    expect(buildDataScopeExportEndpoint({ keyword: "PHASE5_SCOPE" })).toBe(
      "/api/admin/data-scopes/export?keyword=PHASE5_SCOPE",
    );

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await saveScreen(
      dataScopeScreen,
      { roleCode: "R04" },
      {
        roleCode: "R04",
        dataScopeType: "BUSINESS_OWNER",
        organizationCode: "EDU",
        workArea: "PHASE5_SCOPE",
      },
      {},
      [],
    );

    const [, init] = fetchMock.mock.calls[0];
    expect(fetchMock.mock.calls[0][0]).toBe("/api/admin/data-scopes/R04");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      dataScopeType: "BUSINESS_OWNER",
      organizationCode: "EDU",
      workArea: "PHASE5_SCOPE",
    });
  });
});
