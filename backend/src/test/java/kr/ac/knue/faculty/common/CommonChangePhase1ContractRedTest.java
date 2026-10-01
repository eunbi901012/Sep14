package kr.ac.knue.faculty.common;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
    "DELETE FROM app_session",
    "DELETE FROM business_owner_assignment",
    "DELETE FROM position_assignment",
    "UPDATE user_account SET use_yn='Y', business_role='교원', updated_at=CURRENT_TIMESTAMP WHERE user_id='U-F1001'",
    "UPDATE user_account SET use_yn='Y', business_role='학과장', updated_at=CURRENT_TIMESTAMP WHERE user_id='U-F1002'",
    "UPDATE system_common_setting SET setting_value=default_value, updated_at=CURRENT_TIMESTAMP WHERE setting_key IN ('SESSION_IDLE_MINUTES','PAGE_SIZE','DEFAULT_SEARCH_PERIOD_DAYS','BULK_QUERY_THRESHOLD_COUNT','LONG_TASK_NOTICE_SECONDS')",
    "UPDATE role_data_scope_rule SET data_scope_type='SELF', organization_code=NULL, work_area=NULL, condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code='R01'",
    "UPDATE role_data_scope_rule SET data_scope_type='COLLEGE', organization_code='EDU', work_area=NULL, condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code='R03'"
})
class CommonChangePhase1ContractRedTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("T001: /admin/positions route and position assignment API contract are executable")
    void t001_positions_route_and_api_contract_are_executable() throws Exception {
        assertOpenApiOperations(
            "/api/admin/positions",
            "listPositions",
            "createPositionAssignment",
            "updatePositionAssignment",
            "getEffectivePositions",
            "exportPositions"
        );
        Cookie session = loginCookie();

        mockMvc.perform(get("/api/admin/positions?baseDate=2026-10-01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.size", is(20)));

        MvcResult created = mockMvc.perform(post("/api/admin/positions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1002\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-10-01\",\"validTo\":\"2026-12-31\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.positionAssignmentId").exists())
            .andExpect(jsonPath("$.data.positionCode", is("DEPT_HEAD")))
            .andReturn();
        String assignmentId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.data.positionAssignmentId");

        mockMvc.perform(put("/api/admin/positions/" + assignmentId).cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1002\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-10-01\",\"validTo\":\"2027-02-28\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.positionAssignmentId", is(assignmentId)));

        mockMvc.perform(get("/api/admin/positions/effective?baseDate=2026-11-01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/admin/positions/export?keyword=DEPT_HEAD").cookie(session))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("T002: /admin/business-owners route and business owner assignment API contract are executable")
    void t002_business_owners_route_and_api_contract_are_executable() throws Exception {
        assertOpenApiOperations(
            "/api/admin/business-owners",
            "listBusinessOwners",
            "createBusinessOwnerAssignment",
            "updateBusinessOwnerAssignment",
            "exportBusinessOwners"
        );
        Cookie session = loginCookie();

        mockMvc.perform(get("/api/admin/business-owners?baseDate=2026-10-01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.size", is(20)));

        MvcResult created = mockMvc.perform(post("/api/admin/business-owners").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessOrganizationCode\":\"EDU\",\"userId\":\"U-F1002\",\"workArea\":\"ACHIEVEMENT_REVIEW\",\"assignedFrom\":\"2026-10-01\",\"assignedTo\":\"2026-12-31\",\"dataScope\":\"COLLEGE\",\"processPermission\":\"APPROVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessOwnerAssignmentId").exists())
            .andReturn();
        String assignmentId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.data.businessOwnerAssignmentId");

        mockMvc.perform(put("/api/admin/business-owners/" + assignmentId).cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessOrganizationCode\":\"EDU\",\"userId\":\"U-F1002\",\"workArea\":\"ACHIEVEMENT_REVIEW\",\"assignedFrom\":\"2026-10-01\",\"assignedTo\":\"2027-02-28\",\"dataScope\":\"COLLEGE\",\"processPermission\":\"APPROVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessOwnerAssignmentId", is(assignmentId)));

        mockMvc.perform(get("/api/admin/business-owners/export?keyword=ACHIEVEMENT_REVIEW").cookie(session))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("T003: /admin/data-scopes route and role data scope API contract are executable")
    void t003_data_scopes_route_and_api_contract_are_executable() throws Exception {
        assertOpenApiOperations(
            "/api/admin/data-scopes",
            "listDataScopes",
            "saveDataScopeRule",
            "exportDataScopes"
        );
        Cookie session = loginCookie();

        mockMvc.perform(get("/api/admin/data-scopes").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.size", is(20)));

        mockMvc.perform(put("/api/admin/data-scopes/R03").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataScopeType\":\"COLLEGE\",\"organizationCode\":\"EDU\",\"workArea\":\"ACHIEVEMENT_REVIEW\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode", is("R03")))
            .andExpect(jsonPath("$.data.dataScopeType", is("COLLEGE")));

        mockMvc.perform(get("/api/admin/data-scopes/export?keyword=R03").cookie(session))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("T004: access guard rejects in function permission, data scope, period, certification order")
    void t004_access_guard_rejects_in_declared_order() throws Exception {
        assertOpenApiOperations("/api/admin/data-scopes/{roleCode}", "saveDataScopeRule");
        Cookie facultySession = loginCookie("f1001", "password");
        Cookie adminSession = loginCookie();

        mockMvc.perform(get("/api/admin/data-scopes").cookie(facultySession))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.meta.denialStep", is("FUNCTION_PERMISSION")));

        mockMvc.perform(put("/api/admin/data-scopes/R01").cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataScopeType\":\"DEPARTMENT\",\"organizationCode\":\"OUT-OF-SCOPE\",\"workArea\":\"ACHIEVEMENT_REVIEW\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.meta.denialStep", is("DATA_SCOPE")));

        mockMvc.perform(post("/api/admin/business-owners").cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessOrganizationCode\":\"EDU\",\"userId\":\"U-F1001\",\"workArea\":\"ACHIEVEMENT_REVIEW\",\"assignedFrom\":\"2025-01-01\",\"assignedTo\":\"2025-01-31\",\"dataScope\":\"COLLEGE\",\"processPermission\":\"APPROVE\",\"requestedBaseDate\":\"2026-10-01\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.meta.denialStep", is("PERIOD")));

        mockMvc.perform(post("/api/admin/positions").cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1001\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-10-01\",\"validTo\":\"2026-12-31\",\"targetCertificationStatus\":\"UNCERTIFIED\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.meta.denialStep", is("CERTIFICATION_STATUS")));
    }

    private void assertOpenApiOperations(String path, String... operationIds) throws Exception {
        String openapi = new ClassPathResource("contracts/openapi.yaml").getContentAsString(StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(openapi).contains(path);
        for (String operationId : operationIds) {
            org.assertj.core.api.Assertions.assertThat(openapi).contains("operationId: " + operationId);
        }
        org.assertj.core.api.Assertions.assertThat(openapi).contains("x-roles:").contains("R09");
        if (path.contains("data-scopes")) {
            org.assertj.core.api.Assertions.assertThat(openapi)
                .contains("역할별 기능 권한 이후 데이터 범위, 기간 조건, 대상 데이터 인증상태를 판정한다.");
        }
    }

    private Cookie loginCookie() throws Exception {
        return loginCookie("admin", "admin");
    }

    private Cookie loginCookie(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("APPSESSION"))
            .andReturn();
        return result.getResponse().getCookie("APPSESSION");
    }
}
