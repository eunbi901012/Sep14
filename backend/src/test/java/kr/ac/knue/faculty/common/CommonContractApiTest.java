package kr.ac.knue.faculty.common;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
    "DELETE FROM app_session",
    "DELETE FROM business_owner_assignment",
    "DELETE FROM position_assignment",
    "DELETE FROM detail_code WHERE (group_id = 'USE_YN' AND code_value = 'T') OR group_id IN ('TEST_GROUP', 'CONTRACT_GROUP')",
    "DELETE FROM code_group WHERE group_id IN ('TEST_GROUP', 'CONTRACT_GROUP')",
    "DELETE FROM user_role_assignment WHERE assignment_id NOT IN ('URA-ADMIN-R09','URA-F1001-R01','URA-F1002-R02')",
    "UPDATE user_role_assignment SET user_id='U-ADMIN', role_code='R09', assignment_source='MANUAL', approver_id='U-ADMIN', valid_from=CURRENT_DATE, valid_to=NULL, status='ACTIVE', updated_at=CURRENT_TIMESTAMP WHERE assignment_id='URA-ADMIN-R09'",
    "UPDATE user_role_assignment SET user_id='U-F1001', role_code='R01', assignment_source='POSITION', approver_id='U-ADMIN', valid_from=CURRENT_DATE, valid_to=NULL, status='ACTIVE', updated_at=CURRENT_TIMESTAMP WHERE assignment_id='URA-F1001-R01'",
    "UPDATE user_role_assignment SET user_id='U-F1002', role_code='R02', assignment_source='POSITION', approver_id='U-ADMIN', valid_from=CURRENT_DATE, valid_to=NULL, status='ACTIVE', updated_at=CURRENT_TIMESTAMP WHERE assignment_id='URA-F1002-R02'",
    "UPDATE user_account SET use_yn='Y', business_role='시스템관리자', updated_at=CURRENT_TIMESTAMP WHERE user_id='U-ADMIN'",
    "UPDATE user_account SET use_yn='Y', business_role='교원', updated_at=CURRENT_TIMESTAMP WHERE user_id='U-F1001'",
    "UPDATE user_account SET use_yn='Y', business_role='학과장', updated_at=CURRENT_TIMESTAMP WHERE user_id='U-F1002'",
    "UPDATE role SET role_name='교원', assignment_criteria='재직 교원', data_scope_default='본인', updated_at=CURRENT_TIMESTAMP WHERE role_code='R01'",
    "UPDATE role SET role_name='학과장', assignment_criteria='학과장 보직자', data_scope_default='소속 학과', updated_at=CURRENT_TIMESTAMP WHERE role_code='R02'",
    "UPDATE role SET role_name='단과대학(원) 행정실', assignment_criteria='행정실 담당자', data_scope_default='소속 단과대학', updated_at=CURRENT_TIMESTAMP WHERE role_code='R03'",
    "UPDATE role SET role_name='교수지원과', assignment_criteria='교수지원과 담당자', data_scope_default='전체', updated_at=CURRENT_TIMESTAMP WHERE role_code='R04'",
    "UPDATE system_common_setting SET setting_value=default_value, updated_at=CURRENT_TIMESTAMP WHERE setting_key IN ('SESSION_IDLE_MINUTES','PAGE_SIZE','DEFAULT_SEARCH_PERIOD_DAYS','BULK_QUERY_THRESHOLD_COUNT','LONG_TASK_NOTICE_SECONDS')",
    "UPDATE role_data_scope_rule SET data_scope_type='SELF', organization_code=NULL, work_area=NULL, condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code='R01'",
    "UPDATE role_data_scope_rule SET data_scope_type='DEPARTMENT', organization_code='CSE', work_area=NULL, condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code='R02'",
    "UPDATE role_data_scope_rule SET data_scope_type='COLLEGE', organization_code='EDU', work_area=NULL, condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code='R03'",
    "UPDATE role_data_scope_rule SET data_scope_type='ALL', organization_code=NULL, work_area=NULL, condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code='R04'",
    "UPDATE role_data_scope_rule SET data_scope_type='BUSINESS_OWNER', organization_code=NULL, work_area='ACADEMIC_GRANT', condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code='R05'",
    "UPDATE role_data_scope_rule SET data_scope_type='BUSINESS_OWNER', organization_code=NULL, work_area='FACULTY_EVALUATION', condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code IN ('R06','R07')",
    "UPDATE role_data_scope_rule SET data_scope_type='ALL', organization_code=NULL, work_area=NULL, condition_json='{}', updated_at=CURRENT_TIMESTAMP WHERE role_code IN ('R08','R09')"
})
class CommonContractApiTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void openapi_contract_fixture_is_available_on_classpath() throws Exception {
        ClassPathResource resource = new ClassPathResource("contracts/openapi.yaml");
        org.assertj.core.api.Assertions.assertThat(resource.exists()).isTrue();
    }

    @Test
    void login_me_logout_and_unauthenticated_admin_guard_follow_contract() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie session = loginCookie();

        mockMvc.perform(get("/api/auth/me").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.loginId", is("admin")))
            .andExpect(jsonPath("$.data.roleCodes[0]", is("R09")));

        mockMvc.perform(post("/api/auth/logout").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.loggedOut", is(true)));
    }

    @Test
    void admin_read_flows_return_seeded_data_and_contract_fields() throws Exception {
        Cookie session = loginCookie();
        mockMvc.perform(get("/api/admin/common-settings").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(5)))
            .andExpect(jsonPath("$.data.items[0].settingKey").exists())
            .andExpect(jsonPath("$.data.items[0].settingValue").exists())
            .andExpect(jsonPath("$.data.items[0].valueUnit").exists());
        mockMvc.perform(get("/api/admin/users").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].lastSyncedAt").exists());
        mockMvc.perform(get("/api/admin/organizations").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].orgCode").exists());
        mockMvc.perform(get("/api/admin/organizations/tree").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nodes").isArray());
        mockMvc.perform(get("/api/admin/roles").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(9)));
        mockMvc.perform(get("/api/admin/user-roles").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].assignmentSource").exists());
        mockMvc.perform(get("/api/admin/menu-permissions?targetType=ROLE&targetId=R09").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.permissions[0].allowYn", is("Y")));
        mockMvc.perform(get("/api/admin/menus/tree").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nodes").isArray());
        mockMvc.perform(get("/api/admin/menus").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].screenId").exists());
        mockMvc.perform(get("/api/admin/code-groups").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].groupId").exists());
        mockMvc.perform(get("/api/admin/code-groups/USE_YN/detail-codes").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].codeValue").exists());
    }

    @Test
    void admin_write_flows_validate_and_persist_changes() throws Exception {
        Cookie session = loginCookie();

        mockMvc.perform(patch("/api/admin/common-settings/sessionIdleMinutes").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"settingValue\":\"45\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.settingKey", is("sessionIdleMinutes")))
            .andExpect(jsonPath("$.data.settingValue", is("45")));

        mockMvc.perform(get("/api/admin/common-settings?keyword=SESSION_IDLE").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].settingValue", is("45")));

        mockMvc.perform(patch("/api/admin/users/U-F1001/system-access").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"systemUseYn\":\"N\",\"businessRole\":\"검증역할\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.systemUseYn", is("N")));

        mockMvc.perform(put("/api/admin/roles/R01").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleName\":\"교원\",\"grantCriteria\":\"검증 기준\",\"defaultDataScope\":\"본인\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode", is("R01")));

        mockMvc.perform(post("/api/admin/user-roles").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"U-F1001\",\"roleCode\":\"R04\",\"assignmentSource\":\"MANUAL\",\"approverId\":\"U-ADMIN\",\"validFrom\":\"2026-01-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignmentSource", is("MANUAL")));

        mockMvc.perform(put("/api/admin/organizations/CSE/relationship").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentOrgCode\":\"EDU\",\"effectiveStartDate\":\"2026-01-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orgCode", is("CSE")));

        mockMvc.perform(post("/api/admin/code-groups").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\":\"TEST_GROUP\",\"groupName\":\"테스트그룹\",\"description\":\"검증\",\"managingDepartment\":\"시스템관리\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupId", is("TEST_GROUP")));

        mockMvc.perform(post("/api/admin/code-groups/TEST_GROUP/detail-codes").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codeValue\":\"A\",\"codeName\":\"테스트\",\"sortOrder\":1,\"additionalAttributes\":\"{}\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codeValue", is("A")));
    }

    @Test
    void validation_errors_return_400_without_sensitive_leakage() throws Exception {
        Cookie session = loginCookie();
        mockMvc.perform(patch("/api/admin/common-settings/pageSize").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"settingValue\":\"문자\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/code-groups").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupName\":\"식별자누락\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void batch_definition_execution_result_and_reprocess_flows_follow_contract() throws Exception {
        Cookie session = loginCookie();
        String batchId = "BATCH-TEST-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(get("/api/admin/batch-definitions").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].batchId").exists())
            .andExpect(jsonPath("$.data.items[0].maxExecutionSeconds").exists());

        mockMvc.perform(post("/api/admin/batch-definitions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"" + batchId + "\",\"batchType\":\"SYSTEM\",\"schedule\":\"0 4 * * *\",\"executionParameters\":{\"mode\":\"TEST\"},\"maxExecutionSeconds\":600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.batchId", is(batchId)));

        MvcResult execution = mockMvc.perform(post("/api/admin/batch-executions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"" + batchId + "\",\"executionParameters\":{\"mode\":\"ONCE\"},\"actionReason\":\"계약 테스트\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.batchId", is(batchId)))
            .andReturn();
        String executionId = com.jayway.jsonpath.JsonPath.read(execution.getResponse().getContentAsString(), "$.data.executionId");

        mockMvc.perform(post("/api/admin/batch-executions/" + executionId + "/stop").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"사용자 중지\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.executionStatus", is("STOPPED")));

        mockMvc.perform(post("/api/admin/batch-executions/" + executionId + "/rerun").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"executionParameters\":{\"mode\":\"RERUN\"},\"actionReason\":\"재실행\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.originalExecutionId", is(executionId)));

        mockMvc.perform(get("/api/admin/batch-results/EXEC-SEED-FAILED/log").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.logFileRef", is("batch-log://EXEC-SEED-FAILED")));

        mockMvc.perform(get("/api/admin/batch-reprocess-targets").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].failureCount", is(5)));

        mockMvc.perform(post("/api/admin/batch-reprocess").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalExecutionId\":\"EXEC-SEED-FAILED\",\"failedTargetId\":\"TARGET-1\",\"reprocessReason\":\"오류 보정 후 재처리\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reprocessResult", is("REQUESTED")));
    }

    @Test
    void batch_write_validation_and_not_found_cases_return_safe_errors() throws Exception {
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/admin/batch-definitions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchType\":\"SYSTEM\",\"schedule\":\"0 4 * * *\",\"maxExecutionSeconds\":600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/batch-executions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"UNKNOWN\",\"actionReason\":\"존재하지 않는 배치 실행\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void phase2_data_foundation_rejects_overlapping_position_and_business_owner_periods() throws Exception {
        Cookie session = loginCookie();

        MvcResult position = mockMvc.perform(post("/api/admin/positions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1002\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-03-01\",\"validTo\":\"2026-12-31\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.positionCode", is("DEPT_HEAD")))
            .andReturn();
        String positionId = com.jayway.jsonpath.JsonPath.read(position.getResponse().getContentAsString(), "$.data.positionAssignmentId");

        mockMvc.perform(post("/api/admin/positions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1002\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-12-31\",\"validTo\":\"2027-02-01\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(put("/api/admin/positions/" + positionId).cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1002\",\"organizationCode\":\"CSE\",\"validFrom\":\"2027-01-01\",\"validTo\":\"2027-12-31\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.validFrom", is("2027-01-01")));

        mockMvc.perform(post("/api/admin/business-owners").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessOrganizationCode\":\"EDU\",\"userId\":\"U-F1001\",\"workArea\":\"FACULTY_EVALUATION\",\"assignedFrom\":\"2026-01-01\",\"assignedTo\":\"2026-06-30\",\"dataScope\":\"COLLEGE\",\"processPermission\":\"WRITE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.workArea", is("FACULTY_EVALUATION")));

        mockMvc.perform(post("/api/admin/business-owners").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessOrganizationCode\":\"EDU\",\"userId\":\"U-F1001\",\"workArea\":\"FACULTY_EVALUATION\",\"assignedFrom\":\"2026-06-30\",\"assignedTo\":\"2026-12-31\",\"dataScope\":\"COLLEGE\",\"processPermission\":\"READ\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void phase3_position_management_filters_effective_assignments_and_keeps_existing_values_on_rejected_updates() throws Exception {
        Cookie session = loginCookie();

        MvcResult first = mockMvc.perform(post("/api/admin/positions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1001\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-01-01\",\"validTo\":\"2026-06-30\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andReturn();
        String firstId = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(), "$.data.positionAssignmentId");

        mockMvc.perform(post("/api/admin/positions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1001\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-07-01\",\"validTo\":\"2026-12-31\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.positionCode", is("DEPT_HEAD")));

        mockMvc.perform(get("/api/admin/positions/effective?baseDate=2026-06-30").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.items[0].positionAssignmentId", is(firstId)))
            .andExpect(jsonPath("$.data.items[0].validFrom", is("2026-01-01")))
            .andExpect(jsonPath("$.data.items[0].validTo", is("2026-06-30")));

        mockMvc.perform(put("/api/admin/positions/" + firstId).cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1001\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-05-01\",\"validTo\":\"2026-07-01\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/admin/positions/effective?baseDate=2026-06-30").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.items[0].positionAssignmentId", is(firstId)))
            .andExpect(jsonPath("$.data.items[0].validFrom", is("2026-01-01")))
            .andExpect(jsonPath("$.data.items[0].validTo", is("2026-06-30")));

        mockMvc.perform(post("/api/admin/positions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"U-F1001\",\"organizationCode\":\"CSE\",\"validFrom\":\"2026-12-31\",\"validTo\":\"2026-01-01\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void phase4_business_owner_management_applies_only_in_period_and_work_area_and_rejects_overlap() throws Exception {
        Cookie session = loginCookie();
        String workArea = "PHASE4_" + UUID.randomUUID().toString().substring(0, 8);
        String createBody = "{\"businessOrganizationCode\":\"EDU\",\"userId\":\"U-F1002\",\"workArea\":\"" + workArea + "\",\"assignedFrom\":\"2026-01-01\",\"assignedTo\":\"2026-06-30\",\"dataScope\":\"COLLEGE\",\"processPermission\":\"WRITE\"}";

        MvcResult created = mockMvc.perform(post("/api/admin/business-owners").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessOwnerAssignmentId").exists())
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andReturn();
        String assignmentId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.data.businessOwnerAssignmentId");

        mockMvc.perform(get("/api/admin/business-owners?keyword=" + workArea + "&baseDate=2026-03-01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.items[0].businessOwnerAssignmentId", is(assignmentId)))
            .andExpect(jsonPath("$.data.items[0].processPermission", is("WRITE")));

        mockMvc.perform(get("/api/admin/business-owners?keyword=" + workArea + "&baseDate=2026-12-31").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(0)));

        mockMvc.perform(get("/api/admin/business-owners?keyword=" + workArea + "_OTHER&baseDate=2026-03-01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(0)));

        mockMvc.perform(post("/api/admin/business-owners").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessOrganizationCode\":\"EDU\",\"userId\":\"U-F1002\",\"workArea\":\"" + workArea + "\",\"assignedFrom\":\"2026-06-30\",\"assignedTo\":\"2026-12-31\",\"dataScope\":\"COLLEGE\",\"processPermission\":\"READ\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/admin/business-owners?keyword=" + workArea + "&baseDate=2026-03-01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.items[0].businessOwnerAssignmentId", is(assignmentId)))
            .andExpect(jsonPath("$.data.items[0].assignedTo", is("2026-06-30")));
    }

    @Test
    void phase2_data_scope_and_page_size_contracts_are_enforced() throws Exception {
        Cookie session = loginCookie();

        mockMvc.perform(get("/api/admin/data-scopes?size=50").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size", is(50)))
            .andExpect(jsonPath("$.data.items[0].dataScopeType").exists());

        mockMvc.perform(get("/api/admin/users?size=30").cookie(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(put("/api/admin/data-scopes/R03").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataScopeType\":\"COLLEGE\",\"organizationCode\":\"EDU\",\"workArea\":\"FACULTY_EVALUATION\",\"condition\":{\"source\":\"phase2-test\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.dataScopeType", is("COLLEGE")))
            .andExpect(jsonPath("$.data.organizationCode", is("EDU")));
    }

    @Test
    void phase5_data_scope_rules_are_saved_exported_and_reject_out_of_scope_requests() throws Exception {
        Cookie adminSession = loginCookie();
        Cookie facultySession = loginCookie("f1001", "password");

        mockMvc.perform(get("/api/admin/data-scopes").cookie(facultySession))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.meta.denialStep", is("FUNCTION_PERMISSION")));

        mockMvc.perform(put("/api/admin/data-scopes/R04").cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataScopeType\":\"BUSINESS_OWNER\",\"organizationCode\":\"EDU\",\"workArea\":\"PHASE5_SCOPE\",\"condition\":{\"source\":\"phase5-test\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode", is("R04")))
            .andExpect(jsonPath("$.data.dataScopeType", is("BUSINESS_OWNER")))
            .andExpect(jsonPath("$.data.organizationCode", is("EDU")))
            .andExpect(jsonPath("$.data.workArea", is("PHASE5_SCOPE")));

        mockMvc.perform(get("/api/admin/data-scopes?keyword=PHASE5_SCOPE").cookie(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.items[0].roleCode", is("R04")))
            .andExpect(jsonPath("$.data.items[0].workArea", is("PHASE5_SCOPE")));

        mockMvc.perform(get("/api/admin/data-scopes/export?keyword=PHASE5_SCOPE").cookie(adminSession))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("PHASE5_SCOPE")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("R01,"))));

        mockMvc.perform(put("/api/admin/data-scopes/R04").cookie(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataScopeType\":\"DEPARTMENT\",\"organizationCode\":\"OUT-OF-SCOPE\",\"workArea\":\"PHASE5_SCOPE\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.meta.denialStep", is("DATA_SCOPE")));

        mockMvc.perform(get("/api/admin/data-scopes?keyword=PHASE5_SCOPE").cookie(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].dataScopeType", is("BUSINESS_OWNER")))
            .andExpect(jsonPath("$.data.items[0].organizationCode", is("EDU")));
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

    private Cookie loginCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"admin\",\"password\":\"admin\"}"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("APPSESSION"))
            .andReturn();
        return result.getResponse().getCookie("APPSESSION");
    }
}
