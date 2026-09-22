package kr.ac.knue.faculty.common;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(statements = {
    "DELETE FROM app_session",
    "DELETE FROM batch_reprocess",
    "DELETE FROM batch_result WHERE execution_id NOT IN ('EXEC-SEED-FAILED','EXEC-SEED-DONE')",
    "DELETE FROM batch_execution WHERE execution_id NOT IN ('EXEC-SEED-FAILED','EXEC-SEED-DONE')",
    "DELETE FROM batch_definition WHERE batch_id NOT IN ('BATCH-EVAL-DATA','BATCH-SCORE-CALC')",
    "UPDATE batch_definition SET batch_type='EVALUATION_DATA', schedule='0 1 * * *', predecessor_batch_id=NULL, successor_batch_id='BATCH-SCORE-CALC', execution_parameters='{\"year\":\"2026\"}', max_execution_seconds=3600, owner_user_id='U-ADMIN', updated_at=CURRENT_TIMESTAMP WHERE batch_id='BATCH-EVAL-DATA'",
    "UPDATE batch_definition SET batch_type='SCORE_CALCULATION', schedule='0 3 * * *', predecessor_batch_id='BATCH-EVAL-DATA', successor_batch_id=NULL, execution_parameters='{\"round\":\"REGULAR\"}', max_execution_seconds=5400, owner_user_id='U-ADMIN', updated_at=CURRENT_TIMESTAMP WHERE batch_id='BATCH-SCORE-CALC'",
    "UPDATE batch_execution SET batch_id='BATCH-EVAL-DATA', execution_parameters='{\"year\":\"2026\"}', action_type='START', action_reason='초기 검증 실패 이력', operator_user_id='U-ADMIN', execution_status='FAILED', original_execution_id=NULL, updated_at=CURRENT_TIMESTAMP WHERE execution_id='EXEC-SEED-FAILED'",
    "UPDATE batch_execution SET batch_id='BATCH-SCORE-CALC', execution_parameters='{\"round\":\"REGULAR\"}', action_type='START', action_reason='초기 검증 완료 이력', operator_user_id='U-ADMIN', execution_status='COMPLETED', original_execution_id=NULL, updated_at=CURRENT_TIMESTAMP WHERE execution_id='EXEC-SEED-DONE'",
    "UPDATE batch_result SET ended_at=CURRENT_TIMESTAMP, total_count=25, success_count=20, failure_count=5, excluded_count=0, elapsed_seconds=120, log_file_ref='batch-log://EXEC-SEED-FAILED', updated_at=CURRENT_TIMESTAMP WHERE execution_id='EXEC-SEED-FAILED'",
    "UPDATE batch_result SET ended_at=CURRENT_TIMESTAMP, total_count=30, success_count=30, failure_count=0, excluded_count=0, elapsed_seconds=180, log_file_ref='batch-log://EXEC-SEED-DONE', updated_at=CURRENT_TIMESTAMP WHERE execution_id='EXEC-SEED-DONE'",
    "UPDATE user_role_assignment SET user_id='U-ADMIN', role_code='R09', assignment_source='MANUAL', approver_id='U-ADMIN', valid_from=CURRENT_DATE, valid_to=NULL, status='ACTIVE', updated_at=CURRENT_TIMESTAMP WHERE assignment_id='URA-ADMIN-R09'",
    "UPDATE user_role_assignment SET user_id='U-F1002', role_code='R02', assignment_source='POSITION', approver_id='U-ADMIN', valid_from=CURRENT_DATE, valid_to=NULL, status='ACTIVE', updated_at=CURRENT_TIMESTAMP WHERE assignment_id='URA-F1002-R02'",
    "UPDATE user_account SET use_yn='Y', business_role='시스템관리자', updated_at=CURRENT_TIMESTAMP WHERE user_id='U-ADMIN'",
    "UPDATE user_account SET use_yn='Y', business_role='학과장', updated_at=CURRENT_TIMESTAMP WHERE user_id='U-F1002'"
})
class BatchVendorObligationApiTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/admin/batch-executions literal contract coverage")
    void get_api_admin_batch_executions_returns_contract_body() throws Exception {
        Cookie session = loginCookie();

        mockMvc.perform(get("/api/admin/batch-executions").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items[0].executionId").exists())
            .andExpect(jsonPath("$.data.items[0].batchId").exists())
            .andExpect(jsonPath("$.data.items[0].executionStatus").exists());
    }

    @Test
    @DisplayName("GET /api/admin/batch-results literal contract coverage")
    void get_api_admin_batch_results_returns_contract_body() throws Exception {
        Cookie session = loginCookie();

        mockMvc.perform(get("/api/admin/batch-results").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items[0].executionId").exists())
            .andExpect(jsonPath("$.data.items[0].totalCount").exists())
            .andExpect(jsonPath("$.data.items[0].failureCount").exists());
    }

    @Test
    @DisplayName("GET /api/admin/batch-reprocess-results literal contract coverage")
    void get_api_admin_batch_reprocess_results_returns_contract_body() throws Exception {
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/admin/batch-reprocess").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalExecutionId\":\"EXEC-SEED-FAILED\",\"failedTargetId\":\"TARGET-1\",\"reprocessReason\":\"결과 조회 전 재처리 요청\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reprocessResult", is("REQUESTED")));

        mockMvc.perform(get("/api/admin/batch-reprocess-results").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items[0].reprocessExecutionId").exists())
            .andExpect(jsonPath("$.data.items[0].originalExecutionId", is("EXEC-SEED-FAILED")))
            .andExpect(jsonPath("$.data.items[0].reprocessResult", is("REQUESTED")));
    }

    @Test
    @DisplayName("POST /api/admin/batch-definitions x-required-tests:business,side-effect x-side-effects:listbatchdefinitions,batch_definition x-state-transitions:defined,none")
    void post_api_admin_batch_definitions_enforces_business_and_side_effect_obligations() throws Exception {
        assertBatchVendorObligation("/api/admin/batch-definitions", "none -> defined", "listBatchDefinitions");
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/admin/batch-definitions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-VENDOR-CREATE\",\"batchType\":\"SYSTEM\",\"schedule\":\"0 5 * * *\",\"executionParameters\":{\"mode\":\"VENDOR\"},\"maxExecutionSeconds\":600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.batchId", is("BATCH-VENDOR-CREATE")))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists());

        mockMvc.perform(get("/api/admin/batch-definitions?keyword=BATCH-VENDOR-CREATE").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].batchId", is("BATCH-VENDOR-CREATE")))
            .andExpect(jsonPath("$.data.items[0].schedule", is("0 5 * * *")));

        mockMvc.perform(post("/api/admin/batch-definitions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-VENDOR-CREATE\",\"batchType\":\"SYSTEM\",\"schedule\":\"0 6 * * *\",\"executionParameters\":{},\"maxExecutionSeconds\":600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/admin/batch-definitions?keyword=BATCH-EVAL-DATA").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].schedule", is("0 1 * * *")));
    }

    @Test
    @DisplayName("PUT /api/admin/batch-definitions/{batchId} x-required-tests:auth,business,happy,side-effect,validation x-side-effects:batch_definition,listbatchdefinitions x-state-transitions:defined,defined_updated")
    void put_api_admin_batch_definitions_enforces_required_vendor_obligations() throws Exception {
        assertBatchVendorObligation("/api/admin/batch-definitions/{batchId}", "defined -> defined_updated", "listBatchDefinitions");

        mockMvc.perform(put("/api/admin/batch-definitions/BATCH-EVAL-DATA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-EVAL-DATA\",\"batchType\":\"EVALUATION_DATA\",\"schedule\":\"0 7 * * *\",\"executionParameters\":{\"year\":\"2026\"},\"maxExecutionSeconds\":3600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie nonR09Session = loginCookie("f1002", "password");
        mockMvc.perform(put("/api/admin/batch-definitions/BATCH-EVAL-DATA").cookie(nonR09Session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-EVAL-DATA\",\"batchType\":\"EVALUATION_DATA\",\"schedule\":\"0 7 * * *\",\"executionParameters\":{\"year\":\"2026\"},\"maxExecutionSeconds\":3600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie session = loginCookie();
        mockMvc.perform(put("/api/admin/batch-definitions/BATCH-EVAL-DATA").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-EVAL-DATA\",\"batchType\":\"EVALUATION_DATA\",\"executionParameters\":{\"year\":\"2026\"},\"maxExecutionSeconds\":3600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(put("/api/admin/batch-definitions/BATCH-EVAL-DATA").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-SCORE-CALC\",\"batchType\":\"EVALUATION_DATA\",\"schedule\":\"0 7 * * *\",\"executionParameters\":{\"year\":\"2026\"},\"maxExecutionSeconds\":3600,\"ownerUserId\":\"U-ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.batchId", is("BATCH-EVAL-DATA")))
            .andExpect(jsonPath("$.data.schedule", is("0 7 * * *")))
            .andExpect(jsonPath("$.data.updatedAt").exists());

        mockMvc.perform(get("/api/admin/batch-definitions?keyword=BATCH-EVAL-DATA").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].batchId", is("BATCH-EVAL-DATA")))
            .andExpect(jsonPath("$.data.items[0].schedule", is("0 7 * * *")));

        mockMvc.perform(get("/api/admin/batch-definitions?keyword=BATCH-SCORE-CALC").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].schedule", is("0 3 * * *")));
    }

    @Test
    @DisplayName("POST /api/admin/batch-executions x-required-tests:business,side-effect x-side-effects:batch_execution,running x-state-transitions:running,waiting")
    void post_api_admin_batch_executions_enforces_business_and_side_effect_obligations() throws Exception {
        assertBatchVendorObligation("/api/admin/batch-executions", "WAITING -> RUNNING", "batch_execution");
        Cookie session = loginCookie();

        MvcResult result = mockMvc.perform(post("/api/admin/batch-executions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-EVAL-DATA\",\"executionParameters\":{\"year\":\"2026\"},\"actionReason\":\"수동 실행 계약 검증\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.executionId").exists())
            .andExpect(jsonPath("$.data.batchId", is("BATCH-EVAL-DATA")))
            .andExpect(jsonPath("$.data.executionStatus", is("RUNNING")))
            .andExpect(jsonPath("$.data.operatorUserId", is("U-ADMIN")))
            .andExpect(jsonPath("$.data.actionReason", is("수동 실행 계약 검증")))
            .andReturn();
        String executionId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.executionId");

        mockMvc.perform(get("/api/admin/batch-executions?keyword=" + executionId).cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].executionStatus", is("RUNNING")));

        mockMvc.perform(post("/api/admin/batch-executions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"UNKNOWN\",\"executionParameters\":{},\"actionReason\":\"존재하지 않는 배치\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/admin/batch-executions/{executionId}/stop x-required-tests:auth,business,happy,side-effect,validation x-side-effects:action_type,batch_execution,stop,stopped x-state-transitions:running,stopped")
    void post_api_admin_batch_executions_stop_enforces_required_vendor_obligations() throws Exception {
        assertBatchVendorObligation("/api/admin/batch-executions/{executionId}/stop", "RUNNING -> STOPPED", "STOPPED");
        Cookie session = loginCookie();

        MvcResult running = mockMvc.perform(post("/api/admin/batch-executions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"batchId\":\"BATCH-EVAL-DATA\",\"executionParameters\":{\"year\":\"2026\"},\"actionReason\":\"중지 대상 실행\"}"))
            .andExpect(status().isOk())
            .andReturn();
        String executionId = com.jayway.jsonpath.JsonPath.read(running.getResponse().getContentAsString(), "$.data.executionId");

        mockMvc.perform(post("/api/admin/batch-executions/" + executionId + "/stop")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"미인증 중지\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie nonR09Session = loginCookie("f1002", "password");
        mockMvc.perform(post("/api/admin/batch-executions/" + executionId + "/stop").cookie(nonR09Session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"권한 없는 중지\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/batch-executions/" + executionId + "/stop").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/batch-executions/EXEC-SEED-DONE/stop").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"완료 건 중지 시도\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/batch-executions/" + executionId + "/stop").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"사용자 요청 중지\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.executionId", is(executionId)))
            .andExpect(jsonPath("$.data.actionType", is("STOP")))
            .andExpect(jsonPath("$.data.executionStatus", is("STOPPED")))
            .andExpect(jsonPath("$.data.actionReason", is("사용자 요청 중지")));

        mockMvc.perform(get("/api/admin/batch-executions?keyword=" + executionId).cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].executionStatus", is("STOPPED")));
    }

    @Test
    @DisplayName("POST /api/admin/batch-executions/{executionId}/rerun x-required-tests:auth,business,happy,side-effect,validation x-side-effects:batch_execution,original_execution_id x-state-transitions:completed,failed,rerun_requested")
    void post_api_admin_batch_executions_rerun_enforces_required_vendor_obligations() throws Exception {
        assertBatchVendorObligation("/api/admin/batch-executions/{executionId}/rerun", "COMPLETED/FAILED -> RERUN_REQUESTED", "original_execution_id");

        mockMvc.perform(post("/api/admin/batch-executions/EXEC-SEED-FAILED/rerun")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"미인증 재실행\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie nonR09Session = loginCookie("f1002", "password");
        mockMvc.perform(post("/api/admin/batch-executions/EXEC-SEED-FAILED/rerun").cookie(nonR09Session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"권한 없는 재실행\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie session = loginCookie();
        mockMvc.perform(post("/api/admin/batch-executions/EXEC-SEED-FAILED/rerun").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/batch-executions/EXEC-NOT-FOUND/rerun").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionReason\":\"없는 원실행\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)));

        MvcResult rerun = mockMvc.perform(post("/api/admin/batch-executions/EXEC-SEED-FAILED/rerun").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"executionParameters\":{\"year\":\"2026\",\"mode\":\"RERUN\"},\"actionReason\":\"실패 건 재실행\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.originalExecutionId", is("EXEC-SEED-FAILED")))
            .andExpect(jsonPath("$.data.executionStatus", is("RERUN_REQUESTED")))
            .andExpect(jsonPath("$.data.actionType", is("RERUN")))
            .andReturn();
        String rerunExecutionId = com.jayway.jsonpath.JsonPath.read(rerun.getResponse().getContentAsString(), "$.data.executionId");

        mockMvc.perform(get("/api/admin/batch-executions?keyword=" + rerunExecutionId).cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].originalExecutionId", is("EXEC-SEED-FAILED")));
    }

    @Test
    @DisplayName("POST /api/admin/batch-reprocess x-required-tests:auth,business,side-effect,validation x-side-effects:batch_reprocess x-state-transitions:failed_target,reprocess_requested")
    void post_api_admin_batch_reprocess_enforces_missing_vendor_obligations() throws Exception {
        assertBatchVendorObligation("/api/admin/batch-reprocess", "FAILED_TARGET -> REPROCESS_REQUESTED", "batch_reprocess");

        mockMvc.perform(post("/api/admin/batch-reprocess")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalExecutionId\":\"EXEC-SEED-FAILED\",\"failedTargetId\":\"TARGET-1\",\"reprocessReason\":\"미인증 재처리\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie nonR09Session = loginCookie("f1002", "password");
        mockMvc.perform(post("/api/admin/batch-reprocess").cookie(nonR09Session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalExecutionId\":\"EXEC-SEED-FAILED\",\"failedTargetId\":\"TARGET-1\",\"reprocessReason\":\"권한 없는 재처리\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success", is(false)));

        Cookie session = loginCookie();
        mockMvc.perform(post("/api/admin/batch-reprocess").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalExecutionId\":\"EXEC-SEED-FAILED\",\"failedTargetId\":\"TARGET-1\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/batch-reprocess").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalExecutionId\":\"EXEC-SEED-DONE\",\"failedTargetId\":\"TARGET-1\",\"reprocessReason\":\"성공 건 재처리 차단\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(post("/api/admin/batch-reprocess").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalExecutionId\":\"EXEC-SEED-FAILED\",\"failedTargetId\":\"TARGET-1\",\"reprocessReason\":\"실패 대상 재처리\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.originalExecutionId", is("EXEC-SEED-FAILED")))
            .andExpect(jsonPath("$.data.failedTargetId", is("TARGET-1")))
            .andExpect(jsonPath("$.data.reprocessResult", is("REQUESTED")));

        mockMvc.perform(get("/api/admin/batch-reprocess-results").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].originalExecutionId", is("EXEC-SEED-FAILED")));
    }

    private void assertBatchVendorObligation(String path, String stateTransition, String sideEffectToken) throws Exception {
        String openapi = new ClassPathResource("contracts/openapi.yaml").getContentAsString(StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(openapi)
            .contains(path)
            .contains("x-required-tests:")
            .contains("happy:")
            .contains("auth:")
            .contains("validation:")
            .contains("business:")
            .contains("side-effect:")
            .contains("x-side-effects:")
            .contains(sideEffectToken)
            .contains("x-state-transitions:")
            .contains(stateTransition);
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
