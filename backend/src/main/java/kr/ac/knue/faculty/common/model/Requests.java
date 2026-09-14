package kr.ac.knue.faculty.common.model;

import java.util.List;

public final class Requests {
    private Requests() {}
    public record LoginRequest(String loginId, String password) {}
    public record UserAccessRequest(String systemUseYn, String businessRole) {}
    public record OrganizationRelationshipRequest(String parentOrgCode, String effectiveStartDate, String effectiveEndDate) {}
    public record RoleUpdateRequest(String roleName, String grantCriteria, String defaultDataScope) {}
    public record UserRoleRequest(String userId, String roleCode, String assignmentSource, String approverId, String validFrom, String validTo) {}
    public record MenuPermissionItem(String menuId, String allowYn) {}
    public record MenuPermissionRequest(String targetType, String targetId, List<MenuPermissionItem> permissions) {}
    public record MenuStructureRequest(String parentMenuId) {}
    public record MenuOrderRequest(String parentMenuId, List<String> orderedMenuIds) {}
    public record MenuRequest(String menuName, String screenId, String url, String icon, String businessType, String description, String useYn) {}
    public record CodeGroupRequest(String groupId, String groupName, String description, String managingDepartment, String useYn) {}
    public record DetailCodeRequest(String codeValue, String codeName, String parentCodeValue, Integer sortOrder, String additionalAttributes, String useYn) {}
}
