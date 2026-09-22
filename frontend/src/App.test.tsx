import { describe, expect, it, vi } from "vitest";
import React from "react";
import { render, screen } from "@testing-library/react";

import {
  buildEndpoint,
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
});
