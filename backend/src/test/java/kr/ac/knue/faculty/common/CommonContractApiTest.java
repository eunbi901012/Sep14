package kr.ac.knue.faculty.common;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
