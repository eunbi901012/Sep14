package kr.ac.knue.faculty.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.faculty.common.api.ApiException;
import kr.ac.knue.faculty.common.auth.AuthService;
import kr.ac.knue.faculty.common.mapper.CommonMapper;
import kr.ac.knue.faculty.common.model.Requests;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final CommonMapper mapper;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AdminService(CommonMapper mapper, AuthService authService, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> page(List<Map<String, Object>> items, long total, int page, int size) {
        return Map.of("items", items, "page", page, "size", size, "totalElements", total);
    }

    public Map<String, Object> params(int page, int size, String keyword, String filter) {
        Map<String, Object> params = new HashMap<>();
        int safePage = Math.max(page, 0);
        int safeSize = validatePageSize(size);
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

    public Map<String, Object> listBatchDefinitions(int page, int size, String keyword) {
        Map<String, Object> p = params(page, size, keyword, null);
        return page(mapper.listBatchDefinitions(p), mapper.countBatchDefinitions(p), page, size);
    }

    @Transactional
    public Map<String, Object> createBatchDefinition(Requests.BatchDefinitionRequest r) {
        validateBatchDefinition(r.batchId(), r.batchType(), r.schedule(), r.maxExecutionSeconds(), r.ownerUserId());
        if (mapper.findBatchDefinition(r.batchId()) != null) throw bad("batchId가 이미 존재합니다.");
        mapper.insertBatchDefinition(r.batchId(), r.batchType(), r.schedule(), r.predecessorBatchId(), r.successorBatchId(), json(r.executionParameters()), r.maxExecutionSeconds(), r.ownerUserId());
        return mapper.findBatchDefinition(r.batchId());
    }

    @Transactional
    public Map<String, Object> updateBatchDefinition(String batchId, Requests.BatchDefinitionRequest r) {
        validateBatchDefinition(batchId, r.batchType(), r.schedule(), r.maxExecutionSeconds(), r.ownerUserId());
        if (mapper.updateBatchDefinition(batchId, r.batchType(), r.schedule(), r.predecessorBatchId(), r.successorBatchId(), json(r.executionParameters()), r.maxExecutionSeconds(), r.ownerUserId()) == 0) notFound("배치 정의");
        return mapper.findBatchDefinition(batchId);
    }

    public Map<String, Object> listBatchExecutions(int page, int size, String keyword) {
        Map<String, Object> p = params(page, size, keyword, null);
        return page(mapper.listBatchExecutions(p), mapper.countBatchExecutions(p), page, size);
    }

    @Transactional
    public Map<String, Object> createBatchExecution(HttpServletRequest req, Requests.BatchExecutionRequest r) {
        require(r.batchId(), "batchId");
        require(r.actionReason(), "actionReason");
        if (mapper.findBatchDefinition(r.batchId()) == null) notFound("배치 정의");
        String executionId = nextId("EXEC");
        mapper.insertBatchExecution(executionId, r.batchId(), json(r.executionParameters()), "START", r.actionReason(), currentUserId(req), "RUNNING", null);
        mapper.insertBatchResult(executionId, null, 0, 0, 0, 0, null, "batch-log://" + executionId);
        return mapper.findBatchExecution(executionId);
    }

    @Transactional
    public Map<String, Object> stopBatchExecution(HttpServletRequest req, String executionId, Requests.BatchActionRequest r) {
        require(executionId, "executionId");
        require(r == null ? null : r.actionReason(), "actionReason");
        Map<String, Object> current = mapper.findBatchExecution(executionId);
        if (current == null) notFound("배치 실행");
        if (!"RUNNING".equals(String.valueOf(current.get("executionStatus")))) {
            throw bad("RUNNING 상태의 배치 실행만 중지할 수 있습니다.");
        }
        mapper.updateBatchExecutionStatus(executionId, "STOP", r.actionReason(), currentUserId(req), "STOPPED");
        return mapper.findBatchExecution(executionId);
    }

    @Transactional
    public Map<String, Object> rerunBatchExecution(HttpServletRequest req, String executionId, Requests.BatchActionRequest r) {
        Map<String, Object> original = mapper.findBatchExecution(executionId);
        if (original == null) notFound("배치 실행");
        require(r == null ? null : r.actionReason(), "actionReason");
        String nextExecutionId = nextId("RERUN");
        String parameters = r == null || r.executionParameters() == null ? String.valueOf(original.get("executionParameters")) : json(r.executionParameters());
        mapper.insertBatchExecution(nextExecutionId, String.valueOf(original.get("batchId")), parameters, "RERUN", r.actionReason(), currentUserId(req), "RERUN_REQUESTED", executionId);
        mapper.insertBatchResult(nextExecutionId, null, 0, 0, 0, 0, null, "batch-log://" + nextExecutionId);
        return mapper.findBatchExecution(nextExecutionId);
    }

    public Map<String, Object> listBatchResults(int page, int size, String keyword, String executionId) {
        Map<String, Object> p = params(page, size, keyword, null);
        p.put("executionId", executionId);
        return page(mapper.listBatchResults(p), mapper.countBatchResults(p), page, size);
    }

    public Map<String, Object> getBatchResultLog(String executionId) {
        require(executionId, "executionId");
        Map<String, Object> log = mapper.findBatchResultLog(executionId);
        if (log == null) notFound("배치 결과");
        return log;
    }

    public Map<String, Object> listBatchReprocessTargets(int page, int size, String keyword) {
        Map<String, Object> p = params(page, size, keyword, null);
        return page(mapper.listBatchReprocessTargets(p), mapper.countBatchReprocessTargets(p), page, size);
    }

    @Transactional
    public Map<String, Object> createBatchReprocess(HttpServletRequest req, Requests.BatchReprocessRequest r) {
        require(r.originalExecutionId(), "originalExecutionId");
        require(r.failedTargetId(), "failedTargetId");
        require(r.reprocessReason(), "reprocessReason");
        Map<String, Object> originalResult = mapper.findBatchResultSummary(r.originalExecutionId());
        if (originalResult == null) notFound("원본 배치 결과");
        if (((Number) originalResult.get("failureCount")).intValue() <= 0) throw bad("실패 대상이 있는 실행 결과만 재처리할 수 있습니다.");
        String reprocessExecutionId = nextId("REPROC");
        mapper.insertBatchExecution(reprocessExecutionId, String.valueOf(originalResult.get("batchId")), String.valueOf(originalResult.get("executionParameters")), "REPROCESS", r.reprocessReason(), currentUserId(req), "RERUN_REQUESTED", r.originalExecutionId());
        mapper.insertBatchResult(reprocessExecutionId, null, 0, 0, 0, 0, null, "batch-log://" + reprocessExecutionId);
        mapper.insertBatchReprocess(reprocessExecutionId, r.originalExecutionId(), r.failedTargetId(), r.reprocessReason(), "REQUESTED");
        return Map.of("reprocessExecutionId", reprocessExecutionId, "originalExecutionId", r.originalExecutionId(), "failedTargetId", r.failedTargetId(), "reprocessResult", "REQUESTED");
    }

    public Map<String, Object> listBatchReprocessResults(int page, int size, String keyword) {
        Map<String, Object> p = params(page, size, keyword, null);
        return page(mapper.listBatchReprocessResults(p), mapper.countBatchReprocessResults(p), page, size);
    }

    public Map<String, Object> listPositions(int page, int size, String keyword, String baseDate) {
        Map<String, Object> p = params(page, size, keyword, null);
        if (baseDate != null && !baseDate.isBlank()) p.put("baseDate", LocalDate.parse(baseDate));
        return page(mapper.listPositions(p), mapper.countPositions(p), page, size);
    }

    public Map<String, Object> getEffectivePositions(String baseDate) {
        require(baseDate, "baseDate");
        return Map.of("items", mapper.effectivePositions(LocalDate.parse(baseDate)));
    }

    public byte[] exportPositions(String keyword) {
        Map<String, Object> p = params(0, 100, keyword, null);
        StringBuilder out = new StringBuilder("positionAssignmentId,positionCode,userId,organizationCode,validFrom,validTo\n");
        for (Map<String, Object> row : mapper.listPositions(p)) {
            out.append(row.get("positionAssignmentId")).append(',')
                .append(row.get("positionCode")).append(',')
                .append(row.get("userId")).append(',')
                .append(row.get("organizationCode")).append(',')
                .append(row.get("validFrom")).append(',')
                .append(row.get("validTo")).append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public Map<String, Object> createPositionAssignment(Requests.PositionAssignmentRequest r) {
        validatePositionAssignment(r);
        LocalDate from = LocalDate.parse(r.validFrom());
        LocalDate to = LocalDate.parse(r.validTo());
        rejectIfPositionOverlaps("", r.positionCode(), r.userId(), r.organizationCode(), from, to);
        String id = nextId("POS");
        mapper.insertPositionAssignment(id, r.positionCode(), r.userId(), r.organizationCode(), from, to);
        return mapper.findPositionAssignment(id);
    }

    @Transactional
    public Map<String, Object> updatePositionAssignment(String id, Requests.PositionAssignmentRequest r) {
        require(id, "positionAssignmentId");
        validatePositionAssignment(r);
        LocalDate from = LocalDate.parse(r.validFrom());
        LocalDate to = LocalDate.parse(r.validTo());
        rejectIfPositionOverlaps(id, r.positionCode(), r.userId(), r.organizationCode(), from, to);
        if (mapper.updatePositionAssignment(id, r.positionCode(), r.userId(), r.organizationCode(), from, to) == 0) notFound("보직 지정");
        return mapper.findPositionAssignment(id);
    }

    public Map<String, Object> listBusinessOwners(int page, int size, String keyword, String baseDate) {
        Map<String, Object> p = params(page, size, keyword, null);
        if (baseDate != null && !baseDate.isBlank()) p.put("baseDate", LocalDate.parse(baseDate));
        return page(mapper.listBusinessOwners(p), mapper.countBusinessOwners(p), page, size);
    }

    public byte[] exportBusinessOwners(String keyword) {
        Map<String, Object> p = params(0, 100, keyword, null);
        StringBuilder out = new StringBuilder("businessOwnerAssignmentId,businessOrganizationCode,userId,workArea,assignedFrom,assignedTo,dataScope,processPermission\n");
        for (Map<String, Object> row : mapper.listBusinessOwners(p)) {
            out.append(row.get("businessOwnerAssignmentId")).append(',')
                .append(row.get("businessOrganizationCode")).append(',')
                .append(row.get("userId")).append(',')
                .append(row.get("workArea")).append(',')
                .append(row.get("assignedFrom")).append(',')
                .append(row.get("assignedTo")).append(',')
                .append(row.get("dataScope")).append(',')
                .append(row.get("processPermission")).append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public Map<String, Object> createBusinessOwnerAssignment(Requests.BusinessOwnerAssignmentRequest r) {
        validateBusinessOwnerAssignment(r);
        LocalDate from = LocalDate.parse(r.assignedFrom());
        LocalDate to = LocalDate.parse(r.assignedTo());
        rejectIfBusinessOwnerOverlaps("", r.businessOrganizationCode(), r.userId(), r.workArea(), from, to);
        String id = nextId("BOA");
        mapper.insertBusinessOwnerAssignment(id, r.businessOrganizationCode(), r.userId(), r.workArea(), from, to, r.dataScope(), r.processPermission());
        return mapper.findBusinessOwnerAssignment(id);
    }

    @Transactional
    public Map<String, Object> updateBusinessOwnerAssignment(String id, Requests.BusinessOwnerAssignmentRequest r) {
        require(id, "businessOwnerAssignmentId");
        validateBusinessOwnerAssignment(r);
        LocalDate from = LocalDate.parse(r.assignedFrom());
        LocalDate to = LocalDate.parse(r.assignedTo());
        rejectIfBusinessOwnerOverlaps(id, r.businessOrganizationCode(), r.userId(), r.workArea(), from, to);
        if (mapper.updateBusinessOwnerAssignment(id, r.businessOrganizationCode(), r.userId(), r.workArea(), from, to, r.dataScope(), r.processPermission()) == 0) notFound("업무담당자 지정");
        return mapper.findBusinessOwnerAssignment(id);
    }

    public Map<String, Object> listDataScopes(int page, int size, String keyword) {
        Map<String, Object> p = params(page, size, keyword, null);
        return page(mapper.listDataScopes(p), mapper.countDataScopes(p), page, size);
    }

    public byte[] exportDataScopes(String keyword) {
        Map<String, Object> p = params(0, 100, keyword, null);
        StringBuilder out = new StringBuilder("roleCode,roleName,dataScopeType,organizationCode,organizationName,workArea,conditionJson\n");
        for (Map<String, Object> row : mapper.listDataScopes(p)) {
            out.append(row.get("roleCode")).append(',')
                .append(row.get("roleName")).append(',')
                .append(row.get("dataScopeType")).append(',')
                .append(value(row.get("organizationCode"), "")).append(',')
                .append(value(row.get("organizationName"), "")).append(',')
                .append(value(row.get("workArea"), "")).append(',')
                .append(value(row.get("conditionJson"), ""))
                .append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public Map<String, Object> saveDataScopeRule(String roleCode, Requests.DataScopeRuleRequest r) {
        require(roleCode, "roleCode");
        require(r.dataScopeType(), "dataScopeType");
        validateDataScopeType(r.dataScopeType());
        validateRoleDataScopeTarget(roleCode, r);
        String conditionJson = json(r.condition());
        int updated = mapper.updateDataScopeRule(roleCode, r.dataScopeType(), r.organizationCode(), r.workArea(), conditionJson);
        if (updated == 0) mapper.insertDataScopeRule(roleCode, r.dataScopeType(), r.organizationCode(), r.workArea(), conditionJson);
        Map<String, Object> data = new HashMap<>();
        data.put("roleCode", roleCode);
        data.put("dataScopeType", r.dataScopeType());
        data.put("organizationCode", r.organizationCode());
        data.put("workArea", r.workArea());
        data.put("conditionJson", conditionJson);
        return data;
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
    private Map<String, Object> positionResponse(String id, Requests.PositionAssignmentRequest r) {
        return Map.of("positionAssignmentId", id, "positionCode", r.positionCode(), "userId", r.userId(), "organizationCode", r.organizationCode(), "validFrom", r.validFrom(), "validTo", r.validTo());
    }
    private Map<String, Object> businessOwnerResponse(String id, Requests.BusinessOwnerAssignmentRequest r) {
        return Map.of("businessOwnerAssignmentId", id, "businessOrganizationCode", r.businessOrganizationCode(), "userId", r.userId(), "workArea", r.workArea(), "assignedFrom", r.assignedFrom(), "assignedTo", r.assignedTo(), "dataScope", r.dataScope(), "processPermission", r.processPermission());
    }

    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private String value(Object value, String fallback) { return value == null || value.toString().isBlank() ? fallback : value.toString(); }
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
    private void validateBatchDefinition(String batchId, String batchType, String schedule, Integer maxExecutionSeconds, String ownerUserId) {
        require(batchId, "batchId");
        require(batchType, "batchType");
        require(schedule, "schedule");
        require(ownerUserId, "ownerUserId");
        if (maxExecutionSeconds == null || maxExecutionSeconds <= 0) throw bad("maxExecutionSeconds는 1 이상이어야 합니다.");
    }
    private int validatePageSize(int size) {
        if (size == 10 || size == 20 || size == 50 || size == 100) return size;
        throw bad("목록 표시 건수는 10, 20, 50, 100 중 하나여야 합니다.");
    }
    private void validatePositionAssignment(Requests.PositionAssignmentRequest r) {
        require(r.positionCode(), "positionCode"); require(r.userId(), "userId"); require(r.organizationCode(), "organizationCode"); require(r.validFrom(), "validFrom"); require(r.validTo(), "validTo");
        LocalDate from = LocalDate.parse(r.validFrom()); LocalDate to = LocalDate.parse(r.validTo());
        if (from.isAfter(to)) throw bad("validFrom은 validTo보다 늦을 수 없습니다.");
        if ("UNCERTIFIED".equals(r.targetCertificationStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "대상 데이터 인증상태가 미인증이면 처리할 수 없습니다.", "CERTIFICATION_STATUS");
        }
    }
    private void rejectIfPositionOverlaps(String id, String positionCode, String userId, String organizationCode, LocalDate from, LocalDate to) {
        if (mapper.countOverlappingPositionAssignments(id, positionCode, userId, organizationCode, from, to) > 0) throw bad("동일 보직코드·대상 사용자·소속조직의 유효기간이 겹칩니다.");
    }
    private void validateBusinessOwnerAssignment(Requests.BusinessOwnerAssignmentRequest r) {
        require(r.businessOrganizationCode(), "businessOrganizationCode"); require(r.userId(), "userId"); require(r.workArea(), "workArea"); require(r.assignedFrom(), "assignedFrom"); require(r.assignedTo(), "assignedTo"); require(r.dataScope(), "dataScope"); require(r.processPermission(), "processPermission");
        LocalDate from = LocalDate.parse(r.assignedFrom()); LocalDate to = LocalDate.parse(r.assignedTo());
        if (from.isAfter(to)) throw bad("assignedFrom은 assignedTo보다 늦을 수 없습니다.");
        if (r.requestedBaseDate() != null && !r.requestedBaseDate().isBlank()) {
            LocalDate baseDate = LocalDate.parse(r.requestedBaseDate());
            if (baseDate.isBefore(from) || baseDate.isAfter(to)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "요청 기준일이 지정기간 밖입니다.", "PERIOD");
            }
        }
        validateDataScopeType(r.dataScope());
        if (!List.of("READ", "WRITE", "APPROVE", "EXPORT").contains(r.processPermission())) throw bad("processPermission 값이 올바르지 않습니다.");
    }
    private void rejectIfBusinessOwnerOverlaps(String id, String organizationCode, String userId, String workArea, LocalDate from, LocalDate to) {
        if (mapper.countOverlappingBusinessOwnerAssignments(id, organizationCode, userId, workArea, from, to) > 0) throw bad("동일 업무조직·담당자·담당 업무영역의 지정기간이 겹칩니다.");
    }
    private void validateDataScopeType(String dataScopeType) {
        if (!List.of("SELF", "DEPARTMENT", "COLLEGE", "BUSINESS_OWNER", "ALL").contains(dataScopeType)) throw bad("dataScopeType 값이 올바르지 않습니다.");
    }
    private void validateRoleDataScopeTarget(String roleCode, Requests.DataScopeRuleRequest r) {
        if (mapper.countActiveRole(roleCode) == 0) notFound("역할");
        if (r.organizationCode() != null && !r.organizationCode().isBlank() && mapper.countActiveOrganization(r.organizationCode()) == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "데이터 범위 밖 조직코드는 저장할 수 없습니다.", "DATA_SCOPE");
        }
    }
    private String currentUserId(HttpServletRequest req) {
        return String.valueOf(authService.requireSession(req).get("userId"));
    }
    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw bad("executionParameters는 JSON으로 변환할 수 있어야 합니다.");
        }
    }
    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    private void require(String value, String field) { if (value == null || value.isBlank()) throw bad(field + "는 필수입니다."); }
    private ApiException bad(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    private void notFound(String name) { throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", name + "을 찾을 수 없습니다."); }
}
