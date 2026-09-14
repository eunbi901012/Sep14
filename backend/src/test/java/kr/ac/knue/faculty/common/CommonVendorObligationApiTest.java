package kr.ac.knue.faculty.common;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CommonVendorObligationApiTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/auth/login x-side-effects:postgresql")
    void post_api_auth_login_creates_postgresql_session_and_returns_contract_body() throws Exception {
        assertOpenApiVendorObligation("/api/auth/login", true, false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"admin\",\"password\":\"admin\"}"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("APPSESSION"))
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.authenticated", is(true)))
            .andExpect(jsonPath("$.data.loginId", is("admin")))
            .andExpect(jsonPath("$.data.roleCodes[0]", is("R09")));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"admin\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/auth/logout x-side-effects:postgresql")
    void post_api_auth_logout_updates_postgresql_session_status_and_blocks_reuse() throws Exception {
        assertOpenApiVendorObligation("/api/auth/logout", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/auth/logout").cookie(session))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("APPSESSION"))
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.loggedOut", is(true)));

        mockMvc.perform(get("/api/auth/me").cookie(session))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/health")
    void get_api_health_returns_contract_body() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.status", is("UP")));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{userId}/system-access x-side-effects:postgresql")
    void patch_api_admin_users_system_access_persists_postgresql_change() throws Exception {
        assertOpenApiVendorObligation("/api/admin/users/{userId}/system-access", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(patch("/api/admin/users/U-F1001/system-access").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"systemUseYn\":\"N\",\"businessRole\":\"검증역할\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId", is("U-F1001")))
            .andExpect(jsonPath("$.data.systemUseYn", is("N")));

        mockMvc.perform(get("/api/admin/users?keyword=F1001").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].useYn", is("N")))
            .andExpect(jsonPath("$.data.items[0].role", is("검증역할")));
    }

    @Test
    @DisplayName("PUT /api/admin/organizations/{orgCode}/relationship x-side-effects:postgresql")
    void put_api_admin_organizations_relationship_persists_postgresql_history() throws Exception {
        assertOpenApiVendorObligation("/api/admin/organizations/{orgCode}/relationship", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(put("/api/admin/organizations/CSE/relationship").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentOrgCode\":\"EDU\",\"effectiveStartDate\":\"2026-01-01\",\"effectiveEndDate\":\"2026-12-31\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orgCode", is("CSE")))
            .andExpect(jsonPath("$.data.parentOrgCode", is("EDU")));

        mockMvc.perform(get("/api/admin/organizations/tree").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nodes").isArray());
    }

    @Test
    @DisplayName("PUT /api/admin/roles/{roleCode} x-side-effects:postgresql")
    void put_api_admin_roles_persists_postgresql_change_and_keeps_role_code() throws Exception {
        assertOpenApiVendorObligation("/api/admin/roles/{roleCode}", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(put("/api/admin/roles/R01").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleName\":\"교원\",\"grantCriteria\":\"검증 기준\",\"defaultDataScope\":\"본인\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCode", is("R01")))
            .andExpect(jsonPath("$.data.grantCriteria", is("검증 기준")));

        mockMvc.perform(get("/api/admin/roles?keyword=R01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].grantCriteria", is("검증 기준")));
    }

    @Test
    @DisplayName("POST /api/admin/user-roles x-side-effects:postgresql x-state-transitions:active,granted,revoked")
    void post_api_admin_user_roles_persists_postgresql_granted_to_active_transition() throws Exception {
        assertOpenApiVendorObligation("/api/admin/user-roles", true, true);
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/admin/user-roles").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"U-F1001\",\"roleCode\":\"R04\",\"assignmentSource\":\"MANUAL\",\"approverId\":\"U-ADMIN\",\"validFrom\":\"2026-01-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignmentId").exists())
            .andExpect(jsonPath("$.data.assignmentSource", is("MANUAL")));

        mockMvc.perform(get("/api/admin/user-roles?keyword=U-F1001").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].assignmentId").exists());
    }

    @Test
    @DisplayName("PATCH /api/admin/user-roles/{assignmentId} x-side-effects:postgresql x-state-transitions:active,granted,revoked")
    void patch_api_admin_user_roles_persists_postgresql_active_role_change() throws Exception {
        assertOpenApiVendorObligation("/api/admin/user-roles/{assignmentId}", true, true);
        Cookie session = loginCookie();

        mockMvc.perform(patch("/api/admin/user-roles/URA-F1001-R01").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleCode\":\"R04\",\"approverId\":\"U-ADMIN\",\"validFrom\":\"2026-02-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignmentId", is("URA-F1001-R01")))
            .andExpect(jsonPath("$.data.roleCode", is("R04")));

        mockMvc.perform(get("/api/admin/user-roles?keyword=U-F1001").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].roleCode", is("R04")));
    }

    @Test
    @DisplayName("DELETE /api/admin/user-roles/{assignmentId} x-side-effects:postgresql x-state-transitions:active,granted,revoked")
    void delete_api_admin_user_roles_persists_postgresql_active_to_revoked_transition() throws Exception {
        assertOpenApiVendorObligation("/api/admin/user-roles/{assignmentId}", true, true);
        Cookie session = loginCookie();

        mockMvc.perform(delete("/api/admin/user-roles/URA-F1002-R02").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assignmentId", is("URA-F1002-R02")))
            .andExpect(jsonPath("$.data.revoked", is(true)));

        mockMvc.perform(get("/api/admin/user-roles?keyword=U-F1002").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    @Test
    @DisplayName("PUT /api/admin/menu-permissions x-side-effects:postgresql")
    void put_api_admin_menu_permissions_persists_postgresql_permissions() throws Exception {
        assertOpenApiVendorObligation("/api/admin/menu-permissions", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(put("/api/admin/menu-permissions").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"ROLE\",\"targetId\":\"R01\",\"permissions\":[{\"menuId\":\"MENU-USERS\",\"allowYn\":\"Y\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.targetType", is("ROLE")))
            .andExpect(jsonPath("$.data.permissions[0].allowYn", is("Y")));

        mockMvc.perform(get("/api/admin/menu-permissions?targetType=ROLE&targetId=R01").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.permissions[0].menuId", is("MENU-USERS")));
    }

    @Test
    @DisplayName("PATCH /api/admin/menus/{menuId}/structure x-side-effects:postgresql")
    void patch_api_admin_menus_structure_persists_postgresql_parent_change() throws Exception {
        assertOpenApiVendorObligation("/api/admin/menus/{menuId}/structure", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(patch("/api/admin/menus/MENU-INFOS/structure").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentMenuId\":\"MENU-SYS\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuId", is("MENU-INFOS")))
            .andExpect(jsonPath("$.data.parentMenuId", is("MENU-SYS")));

        mockMvc.perform(get("/api/admin/menus/tree").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nodes").isArray());
    }

    @Test
    @DisplayName("PATCH /api/admin/menus/order x-side-effects:postgresql")
    void patch_api_admin_menus_order_persists_postgresql_display_order() throws Exception {
        assertOpenApiVendorObligation("/api/admin/menus/order", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(patch("/api/admin/menus/order").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentMenuId\":\"MENU-SYS\",\"orderedMenuIds\":[\"MENU-CODE\",\"MENU-MENU\",\"MENU-AUTH\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderedMenuIds[0]", is("MENU-CODE")));

        mockMvc.perform(patch("/api/admin/menus/order").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentMenuId\":\"MENU-SYS\",\"orderedMenuIds\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/admin/menus x-side-effects:postgresql")
    void post_api_admin_menus_persists_postgresql_menu_and_lists_it() throws Exception {
        assertOpenApiVendorObligation("/api/admin/menus", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/admin/menus").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"menuName\":\"검증 메뉴\",\"screenId\":\"SCR-TEST-CREATE\",\"url\":\"/admin/test-create\",\"icon\":\"test\",\"businessType\":\"COMMON\",\"description\":\"계약 검증\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuId").exists())
            .andExpect(jsonPath("$.data.screenId", is("SCR-TEST-CREATE")));

        mockMvc.perform(get("/api/admin/menus?keyword=검증 메뉴").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].screenId", is("SCR-TEST-CREATE")));
    }

    @Test
    @DisplayName("PUT /api/admin/menus/{menuId} x-side-effects:postgresql")
    void put_api_admin_menus_persists_postgresql_menu_update() throws Exception {
        assertOpenApiVendorObligation("/api/admin/menus/{menuId}", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(put("/api/admin/menus/MENU-USERS").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"menuName\":\"사용자 관리 검증\",\"screenId\":\"SCR-CMN-USER\",\"url\":\"/admin/users\",\"icon\":\"user\",\"businessType\":\"COMMON\",\"description\":\"사용자 관리 검증\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.menuId", is("MENU-USERS")))
            .andExpect(jsonPath("$.data.menuName", is("사용자 관리 검증")));

        mockMvc.perform(get("/api/admin/menus?keyword=사용자 관리 검증").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].menuId", is("MENU-USERS")));
    }

    @Test
    @DisplayName("POST /api/admin/code-groups x-side-effects:postgresql")
    void post_api_admin_code_groups_persists_postgresql_group_and_lists_it() throws Exception {
        assertOpenApiVendorObligation("/api/admin/code-groups", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/admin/code-groups").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\":\"CONTRACT_GROUP\",\"groupName\":\"계약그룹\",\"description\":\"검증\",\"managingDepartment\":\"시스템관리\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupId", is("CONTRACT_GROUP")));

        mockMvc.perform(get("/api/admin/code-groups?keyword=CONTRACT_GROUP").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].groupName", is("계약그룹")));
    }

    @Test
    @DisplayName("PUT /api/admin/code-groups/{groupId} x-side-effects:postgresql")
    void put_api_admin_code_groups_persists_postgresql_group_update() throws Exception {
        assertOpenApiVendorObligation("/api/admin/code-groups/{groupId}", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(put("/api/admin/code-groups/USE_YN").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupName\":\"사용여부 검증\",\"description\":\"공통 사용여부 코드\",\"managingDepartment\":\"시스템관리\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupId", is("USE_YN")))
            .andExpect(jsonPath("$.data.groupName", is("사용여부 검증")));

        mockMvc.perform(get("/api/admin/code-groups?keyword=USE_YN").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].groupName", is("사용여부 검증")));
    }

    @Test
    @DisplayName("POST /api/admin/code-groups/{groupId}/detail-codes x-side-effects:postgresql")
    void post_api_admin_code_groups_detail_codes_persists_postgresql_detail_code() throws Exception {
        assertOpenApiVendorObligation("/api/admin/code-groups/{groupId}/detail-codes", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(post("/api/admin/code-groups/USE_YN/detail-codes").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codeValue\":\"T\",\"codeName\":\"테스트\",\"sortOrder\":3,\"additionalAttributes\":\"{}\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codeValue", is("T")));

        mockMvc.perform(get("/api/admin/code-groups/USE_YN/detail-codes").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[2].codeValue", is("T")));
    }

    @Test
    @DisplayName("PUT /api/admin/code-groups/{groupId}/detail-codes/{codeValue} x-side-effects:postgresql")
    void put_api_admin_code_groups_detail_codes_persists_postgresql_detail_code_update() throws Exception {
        assertOpenApiVendorObligation("/api/admin/code-groups/{groupId}/detail-codes/{codeValue}", true, false);
        Cookie session = loginCookie();

        mockMvc.perform(put("/api/admin/code-groups/USE_YN/detail-codes/Y").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codeName\":\"사용 검증\",\"sortOrder\":1,\"additionalAttributes\":\"{}\",\"useYn\":\"Y\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codeValue", is("Y")))
            .andExpect(jsonPath("$.data.codeName", is("사용 검증")));

        mockMvc.perform(get("/api/admin/code-groups/USE_YN/detail-codes").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].codeName", is("사용 검증")));
    }

    private void assertOpenApiVendorObligation(String path, boolean sideEffects, boolean stateTransitions) throws Exception {
        String openapi = new ClassPathResource("contracts/openapi.yaml").getContentAsString(StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(openapi).contains(path);
        if (sideEffects) {
            org.assertj.core.api.Assertions.assertThat(openapi).contains("x-side-effects:").contains("PostgreSQL");
        }
        if (stateTransitions) {
            org.assertj.core.api.Assertions.assertThat(openapi).contains("x-state-transitions:").contains("granted -> active 또는 active -> revoked");
        }
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
