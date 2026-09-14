package kr.ac.knue.faculty.common.api;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.faculty.common.auth.AuthService;
import kr.ac.knue.faculty.common.model.ApiEnvelope;
import kr.ac.knue.faculty.common.model.Requests;
import kr.ac.knue.faculty.common.service.AdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final AuthService authService;

    public AdminController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
    }

    @GetMapping("/users")
    public ApiEnvelope<?> listUsers(HttpServletRequest req, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword, @RequestParam(required = false) String filter) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.listUsers(page, size, keyword, filter));
    }

    @PatchMapping("/users/{userId}/system-access")
    public ApiEnvelope<?> updateUserAccess(HttpServletRequest req, @PathVariable String userId, @RequestBody Requests.UserAccessRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateUserAccess(userId, body));
    }

    @GetMapping("/organizations")
    public ApiEnvelope<?> listOrganizations(HttpServletRequest req, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword, @RequestParam(required = false) String filter) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.listOrganizations(page, size, keyword, filter));
    }

    @GetMapping("/organizations/tree")
    public ApiEnvelope<?> getOrganizationTree(HttpServletRequest req) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.organizationTree());
    }

    @PutMapping("/organizations/{orgCode}/relationship")
    public ApiEnvelope<?> updateOrganizationRelationship(HttpServletRequest req, @PathVariable String orgCode, @RequestBody Requests.OrganizationRelationshipRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateOrgRelationship(orgCode, body));
    }

    @GetMapping("/roles")
    public ApiEnvelope<?> listRoles(HttpServletRequest req, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword, @RequestParam(required = false) String filter) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.listRoles(page, size, keyword, filter));
    }

    @PutMapping("/roles/{roleCode}")
    public ApiEnvelope<?> updateRole(HttpServletRequest req, @PathVariable String roleCode, @RequestBody Requests.RoleUpdateRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateRole(roleCode, body));
    }

    @GetMapping("/user-roles")
    public ApiEnvelope<?> listUserRoles(HttpServletRequest req, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword, @RequestParam(required = false) String filter) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.listUserRoles(page, size, keyword, filter));
    }

    @PostMapping("/user-roles")
    public ApiEnvelope<?> grantUserRole(HttpServletRequest req, @RequestBody Requests.UserRoleRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.grantUserRole(body));
    }

    @PatchMapping("/user-roles/{assignmentId}")
    public ApiEnvelope<?> updateUserRole(HttpServletRequest req, @PathVariable String assignmentId, @RequestBody Requests.UserRoleRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateUserRole(assignmentId, body));
    }

    @DeleteMapping("/user-roles/{assignmentId}")
    public ApiEnvelope<?> revokeUserRole(HttpServletRequest req, @PathVariable String assignmentId) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.revokeUserRole(assignmentId));
    }

    @GetMapping("/menu-permissions")
    public ApiEnvelope<?> getMenuPermissions(HttpServletRequest req, @RequestParam(required = false) String targetType, @RequestParam(required = false) String targetId) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.permissions(targetType, targetId));
    }

    @PutMapping("/menu-permissions")
    public ApiEnvelope<?> saveMenuPermissions(HttpServletRequest req, @RequestBody Requests.MenuPermissionRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.savePermissions(body));
    }

    @GetMapping("/menus/tree")
    public ApiEnvelope<?> getMenuTree(HttpServletRequest req) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.menuTree());
    }

    @PatchMapping("/menus/{menuId}/structure")
    public ApiEnvelope<?> updateMenuStructure(HttpServletRequest req, @PathVariable String menuId, @RequestBody Requests.MenuStructureRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateMenuStructure(menuId, body));
    }

    @PatchMapping("/menus/order")
    public ApiEnvelope<?> reorderMenus(HttpServletRequest req, @RequestBody Requests.MenuOrderRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.reorderMenus(body));
    }

    @GetMapping("/menus")
    public ApiEnvelope<?> listMenus(HttpServletRequest req, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword, @RequestParam(required = false) String filter) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.listMenus(page, size, keyword, filter));
    }

    @PostMapping("/menus")
    public ApiEnvelope<?> createMenu(HttpServletRequest req, @RequestBody Requests.MenuRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.createMenu(body));
    }

    @PutMapping("/menus/{menuId}")
    public ApiEnvelope<?> updateMenu(HttpServletRequest req, @PathVariable String menuId, @RequestBody Requests.MenuRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateMenu(menuId, body));
    }

    @GetMapping("/code-groups")
    public ApiEnvelope<?> listCodeGroups(HttpServletRequest req, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword, @RequestParam(required = false) String filter) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.listCodeGroups(page, size, keyword, filter));
    }

    @PostMapping("/code-groups")
    public ApiEnvelope<?> createCodeGroup(HttpServletRequest req, @RequestBody Requests.CodeGroupRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.createCodeGroup(body));
    }

    @PutMapping("/code-groups/{groupId}")
    public ApiEnvelope<?> updateCodeGroup(HttpServletRequest req, @PathVariable String groupId, @RequestBody Requests.CodeGroupRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateCodeGroup(groupId, body));
    }

    @GetMapping("/code-groups/{groupId}/detail-codes")
    public ApiEnvelope<?> listDetailCodes(HttpServletRequest req, @PathVariable String groupId) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.detailCodes(groupId));
    }

    @PostMapping("/code-groups/{groupId}/detail-codes")
    public ApiEnvelope<?> createDetailCode(HttpServletRequest req, @PathVariable String groupId, @RequestBody Requests.DetailCodeRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.createDetailCode(groupId, body));
    }

    @PutMapping("/code-groups/{groupId}/detail-codes/{codeValue}")
    public ApiEnvelope<?> updateDetailCode(HttpServletRequest req, @PathVariable String groupId, @PathVariable String codeValue, @RequestBody Requests.DetailCodeRequest body) {
        authService.requireAdmin(req);
        return ApiEnvelope.ok(adminService.updateDetailCode(groupId, codeValue, body));
    }
}
