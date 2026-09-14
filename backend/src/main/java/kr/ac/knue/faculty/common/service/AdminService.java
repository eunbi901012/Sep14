package kr.ac.knue.faculty.common.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.faculty.common.api.ApiException;
import kr.ac.knue.faculty.common.mapper.CommonMapper;
import kr.ac.knue.faculty.common.model.Requests;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final CommonMapper mapper;

    public AdminService(CommonMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> page(List<Map<String, Object>> items, long total, int page, int size) {
        return Map.of("items", items, "page", page, "size", size, "totalElements", total);
    }

    public Map<String, Object> params(int page, int size, String keyword, String filter) {
        Map<String, Object> params = new HashMap<>();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String value = keyword != null && !keyword.isBlank() ? keyword : filter;
        params.put("page", safePage);
        params.put("size", safeSize);
        params.put("offset", safePage * safeSize);
        params.put("keyword", value);
        params.put("filter", filter);
        return params;
    }

    public Map<String, Object> listUsers(int page, int size, String keyword, String filter) {
        Map<String, Object> p = params(page, size, keyword, filter);
        return page(mapper.listUsers(p), mapper.countUsers(p), page, size);
    }

    public Map<String, Object> listCommonSettings(int page, int size, String keyword, String filter) {
        Map<String, Object> p = params(page, size, keyword, filter);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> item : mapper.listCommonSettings(p)) {
            items.add(toCommonSettingContract(item));
        }
        return page(items, mapper.countCommonSettings(p), page, size);
    }

    @Transactional
    public Map<String, Object> updateCommonSetting(String settingKey, Requests.CommonSettingRequest request) {
        require(settingKey, "settingKey");
        require(request.settingValue(), "settingValue");
        String storageKey = commonSettingStorageKey(settingKey);
        Map<String, Object> current = mapper.findCommonSetting(storageKey);
        if (current == null) notFound("공통 환경설정");
        validateCommonSettingValue(request.settingValue(), current);
        if (mapper.updateCommonSetting(storageKey, request.settingValue()) == 0) notFound("공통 환경설정");
        Map<String, Object> updated = mapper.findCommonSetting(storageKey);
        if (updated == null) notFound("공통 환경설정");
        return toCommonSettingContract(updated);
    }

    @Transactional
    public Map<String, Object> updateUserAccess(String userId, Requests.UserAccessRequest request) {
        require(userId, "userId");
        require(request.systemUseYn(), "systemUseYn");
        require(request.businessRole(), "businessRole");
        if (mapper.updateUserAccess(userId, request.systemUseYn(), request.businessRole()) == 0) notFound("사용자");
        return Map.of("userId", userId, "systemUseYn", request.systemUseYn(), "businessRole", request.businessRole());
    }

    public Map<String, Object> listOrganizations(int page, int size, String keyword, String filter) {
        Map<String, Object> p = params(page, size, keyword, filter);
        return page(mapper.listOrganizations(p), mapper.countOrganizations(p), page, size);
    }

    public Map<String, Object> organizationTree() {
        return Map.of("nodes", mapper.organizationTree());
    }

    @Transactional
    public Map<String, Object> updateOrgRelationship(String orgCode, Requests.OrganizationRelationshipRequest request) {
        require(orgCode, "orgCode");
        require(request.parentOrgCode(), "parentOrgCode");
        require(request.effectiveStartDate(), "effectiveStartDate");
        LocalDate from = LocalDate.parse(request.effectiveStartDate());
        LocalDate to = request.effectiveEndDate() == null || request.effectiveEndDate().isBlank() ? null : LocalDate.parse(request.effectiveEndDate());
        mapper.insertOrgRelationship("REL-" + orgCode + "-" + UUID.randomUUID().toString().substring(0, 8), orgCode, request.parentOrgCode(), from, to, "OQ-001: 확인 필요");
        Map<String, Object> data = new HashMap<>();
        data.put("orgCode", orgCode);
        data.put("parentOrgCode", request.parentOrgCode());
        data.put("effectiveStartDate", request.effectiveStartDate());
        data.put("effectiveEndDate", request.effectiveEndDate());
        return data;
    }

    public Map<String, Object> listRoles(int page, int size, String keyword, String filter) {
        Map<String, Object> p = params(page, size, keyword, filter);
        return page(mapper.listRoles(p), mapper.countRoles(p), page, size);
    }

    @Transactional
    public Map<String, Object> updateRole(String roleCode, Requests.RoleUpdateRequest request) {
        require(roleCode, "roleCode");
        require(request.grantCriteria(), "grantCriteria");
        require(request.defaultDataScope(), "defaultDataScope");
        if (mapper.updateRole(roleCode, request.roleName(), request.grantCriteria(), request.defaultDataScope()) == 0) notFound("역할");
        Map<String, Object> data = new HashMap<>();
        data.put("roleCode", roleCode);
        data.put("roleName", request.roleName());
        data.put("grantCriteria", request.grantCriteria());
        data.put("defaultDataScope", request.defaultDataScope());
        return data;
    }

    public Map<String, Object> listUserRoles(int page, int size, String keyword, String filter) {
        Map<String, Object> p = params(page, size, keyword, filter);
        return page(mapper.listUserRoles(p), mapper.countUserRoles(p), page, size);
    }

    @Transactional
    public Map<String, Object> grantUserRole(Requests.UserRoleRequest request) {
        require(request.userId(), "userId");
        require(request.roleCode(), "roleCode");
        require(request.assignmentSource(), "assignmentSource");
        require(request.validFrom(), "validFrom");
        String id = "URA-" + UUID.randomUUID();
        LocalDate to = parseNullableDate(request.validTo());
        mapper.insertUserRole(id, request.userId(), request.roleCode(), request.assignmentSource(), request.approverId(), LocalDate.parse(request.validFrom()), to);
        return userRoleResponse(id, request.userId(), request.roleCode(), request.assignmentSource(), request.approverId(), request.validFrom(), request.validTo());
    }

    @Transactional
    public Map<String, Object> updateUserRole(String assignmentId, Requests.UserRoleRequest request) {
        require(assignmentId, "assignmentId");
        require(request.roleCode(), "roleCode");
        require(request.validFrom(), "validFrom");
        if (mapper.updateUserRole(assignmentId, request.roleCode(), request.approverId(), LocalDate.parse(request.validFrom()), parseNullableDate(request.validTo())) == 0) notFound("사용자 역할");
        return userRoleResponse(assignmentId, request.userId(), request.roleCode(), request.assignmentSource() == null ? "MANUAL" : request.assignmentSource(), request.approverId(), request.validFrom(), request.validTo());
    }

    @Transactional
    public Map<String, Object> revokeUserRole(String assignmentId) {
        if (mapper.revokeUserRole(assignmentId) == 0) notFound("사용자 역할");
        return Map.of("assignmentId", assignmentId, "revoked", true);
    }

    public Map<String, Object> permissions(String targetType, String targetId) {
        String type = targetType == null || targetType.isBlank() ? "ROLE" : targetType;
        String id = targetId == null || targetId.isBlank() ? "R09" : targetId;
        return Map.of("targetType", type, "targetId", id, "permissions", mapper.permissions(type, id));
    }

    @Transactional
    public Map<String, Object> savePermissions(Requests.MenuPermissionRequest request) {
        require(request.targetType(), "targetType");
        require(request.targetId(), "targetId");
        if (request.permissions() == null) throw bad("permissions는 필수입니다.");
        mapper.deletePermissions(request.targetType(), request.targetId());
        for (Requests.MenuPermissionItem item : request.permissions()) {
            require(item.menuId(), "menuId");
            require(item.allowYn(), "allowYn");
            mapper.insertPermission("PERM-" + request.targetType() + "-" + request.targetId() + "-" + item.menuId(), request.targetType(), request.targetId(), item.menuId(), item.allowYn());
        }
        return permissions(request.targetType(), request.targetId());
    }

    public Map<String, Object> menuTree() { return Map.of("nodes", mapper.menuTree()); }

    @Transactional
    public Map<String, Object> updateMenuStructure(String menuId, Requests.MenuStructureRequest request) {
        require(menuId, "menuId");
        if (mapper.updateMenuParent(menuId, request.parentMenuId()) == 0) notFound("메뉴");
        Map<String, Object> data = new HashMap<>();
        data.put("menuId", menuId);
        data.put("parentMenuId", request.parentMenuId());
        return data;
    }

    @Transactional
    public Map<String, Object> reorderMenus(Requests.MenuOrderRequest request) {
        if (request.orderedMenuIds() == null || request.orderedMenuIds().isEmpty()) throw bad("orderedMenuIds는 필수입니다.");
        int order = 1;
        for (String menuId : request.orderedMenuIds()) mapper.updateMenuOrder(menuId, order++);
        Map<String, Object> data = new HashMap<>();
        data.put("parentMenuId", request.parentMenuId());
        data.put("orderedMenuIds", request.orderedMenuIds());
        return data;
    }

    public Map<String, Object> listMenus(int page, int size, String keyword, String filter) {
        Map<String, Object> p = params(page, size, keyword, filter);
        return page(mapper.listMenus(p), mapper.countMenus(p), page, size);
    }

    @Transactional
    public Map<String, Object> createMenu(Requests.MenuRequest r) {
        require(r.menuName(), "menuName"); require(r.screenId(), "screenId"); require(r.url(), "url");
        String id = "MENU-" + UUID.randomUUID().toString().substring(0, 8);
        mapper.insertMenu(id, r.menuName(), r.screenId(), r.url(), r.icon(), r.businessType(), r.description(), value(r.useYn(), "Y"));
        return menuResponse(id, r);
    }

    @Transactional
    public Map<String, Object> updateMenu(String menuId, Requests.MenuRequest r) {
        require(menuId, "menuId"); require(r.menuName(), "menuName"); require(r.screenId(), "screenId"); require(r.url(), "url");
        if (mapper.updateMenu(menuId, r.menuName(), r.screenId(), r.url(), r.icon(), r.businessType(), r.description(), value(r.useYn(), "Y")) == 0) notFound("메뉴");
        return menuResponse(menuId, r);
    }

    public Map<String, Object> listCodeGroups(int page, int size, String keyword, String filter) {
        Map<String, Object> p = params(page, size, keyword, filter);
        return page(mapper.listCodeGroups(p), mapper.countCodeGroups(p), page, size);
    }

    @Transactional
    public Map<String, Object> createCodeGroup(Requests.CodeGroupRequest r) {
        require(r.groupId(), "groupId"); require(r.groupName(), "groupName");
        int updated = mapper.updateCodeGroup(r.groupId(), r.groupName(), r.description(), r.managingDepartment(), value(r.useYn(), "Y"));
        if (updated == 0) {
            mapper.insertCodeGroup(r.groupId(), r.groupName(), r.description(), r.managingDepartment(), value(r.useYn(), "Y"));
        }
        return codeGroupResponse(r);
    }

    @Transactional
    public Map<String, Object> updateCodeGroup(String groupId, Requests.CodeGroupRequest r) {
        require(groupId, "groupId"); require(r.groupName(), "groupName");
        if (mapper.updateCodeGroup(groupId, r.groupName(), r.description(), r.managingDepartment(), value(r.useYn(), "Y")) == 0) notFound("코드그룹");
        return codeGroupResponse(new Requests.CodeGroupRequest(groupId, r.groupName(), r.description(), r.managingDepartment(), r.useYn()));
    }

    public Map<String, Object> detailCodes(String groupId) {
        require(groupId, "groupId");
        List<Map<String, Object>> items = mapper.detailCodes(groupId);
        return page(items, items.size(), 0, items.size());
    }

    @Transactional
    public Map<String, Object> createDetailCode(String groupId, Requests.DetailCodeRequest r) {
        require(groupId, "groupId"); require(r.codeValue(), "codeValue"); require(r.codeName(), "codeName");
        int sortOrder = r.sortOrder() == null ? 0 : r.sortOrder();
        int updated = mapper.updateDetailCode(groupId, r.codeValue(), r.codeName(), r.parentCodeValue(), sortOrder, value(r.additionalAttributes(), "{}"), value(r.useYn(), "Y"));
        if (updated == 0) {
            mapper.insertDetailCode(groupId, r.codeValue(), r.codeName(), r.parentCodeValue(), sortOrder, value(r.additionalAttributes(), "{}"), value(r.useYn(), "Y"));
        }
        return detailCodeResponse(groupId, r);
    }

    @Transactional
    public Map<String, Object> updateDetailCode(String groupId, String codeValue, Requests.DetailCodeRequest r) {
        require(groupId, "groupId"); require(codeValue, "codeValue"); require(r.codeName(), "codeName");
        if (mapper.updateDetailCode(groupId, codeValue, r.codeName(), r.parentCodeValue(), r.sortOrder() == null ? 0 : r.sortOrder(), value(r.additionalAttributes(), "{}"), value(r.useYn(), "Y")) == 0) notFound("상세코드");
        return detailCodeResponse(groupId, new Requests.DetailCodeRequest(codeValue, r.codeName(), r.parentCodeValue(), r.sortOrder(), r.additionalAttributes(), r.useYn()));
    }

    private Map<String, Object> userRoleResponse(String id, String userId, String roleCode, String source, String approverId, String from, String to) {
        Map<String, Object> data = new HashMap<>();
        data.put("assignmentId", id); data.put("userId", userId); data.put("roleCode", roleCode); data.put("assignmentSource", source); data.put("approverId", approverId); data.put("validFrom", from); data.put("validTo", to);
        return data;
    }

    private Map<String, Object> menuResponse(String id, Requests.MenuRequest r) {
        Map<String, Object> data = new HashMap<>();
        data.put("menuId", id); data.put("menuName", r.menuName()); data.put("screenId", r.screenId()); data.put("url", r.url()); data.put("icon", r.icon()); data.put("businessType", r.businessType()); data.put("description", r.description()); data.put("useYn", value(r.useYn(), "Y"));
        return data;
    }

    private Map<String, Object> codeGroupResponse(Requests.CodeGroupRequest r) {
        return Map.of("groupId", r.groupId(), "groupName", r.groupName(), "description", value(r.description(), ""), "managingDepartment", value(r.managingDepartment(), ""), "useYn", value(r.useYn(), "Y"));
    }

    private Map<String, Object> detailCodeResponse(String groupId, Requests.DetailCodeRequest r) {
        return Map.of("groupId", groupId, "codeValue", r.codeValue(), "codeName", r.codeName(), "parentCodeValue", value(r.parentCodeValue(), ""), "sortOrder", r.sortOrder() == null ? 0 : r.sortOrder(), "additionalAttributes", value(r.additionalAttributes(), "{}"), "useYn", value(r.useYn(), "Y"));
    }

    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private LocalDate parseNullableDate(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
    private Map<String, Object> toCommonSettingContract(Map<String, Object> row) {
        Map<String, Object> data = new LinkedHashMap<>(row);
        data.put("settingKey", commonSettingContractKey(String.valueOf(row.get("settingKey"))));
        return data;
    }
    private String commonSettingStorageKey(String settingKey) {
        return switch (settingKey) {
            case "sessionIdleMinutes" -> "SESSION_IDLE_MINUTES";
            case "pageSize" -> "PAGE_SIZE";
            case "defaultSearchPeriodDays" -> "DEFAULT_SEARCH_PERIOD_DAYS";
            case "bulkQueryThresholdCount" -> "BULK_QUERY_THRESHOLD_COUNT";
            case "longRunningNoticeThresholdSeconds" -> "LONG_TASK_NOTICE_SECONDS";
            default -> settingKey;
        };
    }
    private String commonSettingContractKey(String settingKey) {
        return switch (settingKey) {
            case "SESSION_IDLE_MINUTES" -> "sessionIdleMinutes";
            case "PAGE_SIZE" -> "pageSize";
            case "DEFAULT_SEARCH_PERIOD_DAYS" -> "defaultSearchPeriodDays";
            case "BULK_QUERY_THRESHOLD_COUNT" -> "bulkQueryThresholdCount";
            case "LONG_TASK_NOTICE_SECONDS" -> "longRunningNoticeThresholdSeconds";
            default -> settingKey;
        };
    }
    private void validateCommonSettingValue(String settingValue, Map<String, Object> setting) {
        int parsed;
        try {
            parsed = Integer.parseInt(settingValue.trim());
        } catch (NumberFormatException ex) {
            throw bad("settingValue는 " + setting.get("valueUnit") + " 단위의 정수여야 합니다.");
        }
        int min = ((Number) setting.get("minValue")).intValue();
        int max = ((Number) setting.get("maxValue")).intValue();
        if (parsed < min || parsed > max) {
            throw bad("settingValue는 " + min + " 이상 " + max + " 이하이어야 합니다.");
        }
    }
    private void require(String value, String field) { if (value == null || value.isBlank()) throw bad(field + "는 필수입니다."); }
    private ApiException bad(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    private void notFound(String name) { throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", name + "을 찾을 수 없습니다."); }
}
