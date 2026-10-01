package kr.ac.knue.faculty.common.mapper;

import java.util.Map;

public class SqlProvider {
    private static boolean has(Map<String, Object> p, String key) {
        Object value = p.get(key);
        return value != null && !value.toString().isBlank();
    }

    private static String keywordWhere(Map<String, Object> p, String expr) {
        if (!has(p, "keyword") && !has(p, "filter")) return "";
        return " AND (" + expr + ")";
    }

    private static String baseDateWhere(Map<String, Object> p, String fromColumn, String toColumn) {
        if (!has(p, "baseDate")) return "";
        return " AND " + fromColumn + " <= #{baseDate} AND " + toColumn + " >= #{baseDate}";
    }

    public String listUsers(Map<String, Object> p) {
        return "SELECT u.user_id as \"userId\", k.faculty_no as \"facultyNo\", k.name as \"name\", o.organization_name as \"organization\", " +
            "k.position_title as \"position\", k.employment_status as \"employmentStatus\", coalesce(u.business_role, string_agg(r.role_name, ',')) as \"role\", " +
            "u.use_yn as \"useYn\", k.retirement_date as \"retirementDate\", k.last_synced_at as \"lastSyncedAt\" " +
            "FROM user_account u JOIN korus_faculty_snapshot k ON u.faculty_no=k.faculty_no JOIN organization o ON k.organization_code=o.organization_code " +
            "LEFT JOIN user_role_assignment ura ON ura.user_id=u.user_id AND ura.status='ACTIVE' LEFT JOIN role r ON r.role_code=ura.role_code " +
            "WHERE 1=1" + keywordWhere(p, "lower(k.name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(k.faculty_no) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_name) LIKE lower(concat('%', #{keyword}, '%'))") +
            " GROUP BY u.user_id,k.faculty_no,k.name,o.organization_name,k.position_title,k.employment_status,u.business_role,u.use_yn,k.retirement_date,k.last_synced_at ORDER BY k.faculty_no LIMIT #{size} OFFSET #{offset}";
    }

    public String countUsers(Map<String, Object> p) {
        return "SELECT count(*) FROM user_account u JOIN korus_faculty_snapshot k ON u.faculty_no=k.faculty_no JOIN organization o ON k.organization_code=o.organization_code WHERE 1=1" +
            keywordWhere(p, "lower(k.name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(k.faculty_no) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_name) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listCommonSettings(Map<String, Object> p) {
        return "SELECT setting_key as \"settingKey\", setting_name as \"settingName\", setting_value as \"settingValue\", value_unit as \"valueUnit\", " +
            "default_value as \"defaultValue\", min_value as \"minValue\", max_value as \"maxValue\", description as \"description\", display_order as \"displayOrder\", updated_at as \"updatedAt\" " +
            "FROM system_common_setting WHERE use_yn='Y'" +
            keywordWhere(p, "lower(setting_key) LIKE lower(concat('%', #{keyword}, '%')) OR lower(setting_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(description) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY display_order, setting_key LIMIT #{size} OFFSET #{offset}";
    }

    public String countCommonSettings(Map<String, Object> p) {
        return "SELECT count(*) FROM system_common_setting WHERE use_yn='Y'" +
            keywordWhere(p, "lower(setting_key) LIKE lower(concat('%', #{keyword}, '%')) OR lower(setting_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(description) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listOrganizations(Map<String, Object> p) {
        return "SELECT o.organization_code as \"orgCode\", o.organization_name as \"orgName\", o.organization_type as \"orgType\", " +
            "h.parent_organization_code as \"parentOrgCode\", h.valid_from as \"effectiveStartDate\", h.valid_to as \"effectiveEndDate\" " +
            "FROM organization o LEFT JOIN organization_relation_history h ON h.organization_code=o.organization_code " +
            "WHERE o.use_yn='Y'" + keywordWhere(p, "lower(o.organization_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_type) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY o.organization_code LIMIT #{size} OFFSET #{offset}";
    }

    public String countOrganizations(Map<String, Object> p) {
        return "SELECT count(*) FROM organization o WHERE o.use_yn='Y'" + keywordWhere(p, "lower(o.organization_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_type) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listRoles(Map<String, Object> p) {
        return "SELECT role_code as \"roleCode\", role_name as \"roleName\", purpose as \"purpose\", assignment_criteria as \"grantCriteria\", data_scope_default as \"defaultDataScope\" FROM role WHERE use_yn='Y'" +
            keywordWhere(p, "lower(role_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(role_name) LIKE lower(concat('%', #{keyword}, '%'))") + " ORDER BY role_code LIMIT #{size} OFFSET #{offset}";
    }

    public String countRoles(Map<String, Object> p) {
        return "SELECT count(*) FROM role WHERE use_yn='Y'" + keywordWhere(p, "lower(role_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(role_name) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listUserRoles(Map<String, Object> p) {
        return "SELECT ura.assignment_id as \"assignmentId\", ura.user_id as \"userId\", u.display_name as \"userName\", ura.role_code as \"roleCode\", r.role_name as \"roleName\", ura.assignment_source as \"assignmentSource\", ura.approver_id as \"approverId\", ura.valid_from as \"validFrom\", ura.valid_to as \"validTo\" " +
            "FROM user_role_assignment ura JOIN user_account u ON u.user_id=ura.user_id JOIN role r ON r.role_code=ura.role_code WHERE ura.status='ACTIVE'" +
            keywordWhere(p, "lower(u.display_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(ura.user_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(ura.role_code) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY ura.user_id, ura.role_code LIMIT #{size} OFFSET #{offset}";
    }

    public String countUserRoles(Map<String, Object> p) {
        return "SELECT count(*) FROM user_role_assignment ura JOIN user_account u ON u.user_id=ura.user_id WHERE ura.status='ACTIVE'" + keywordWhere(p, "lower(u.display_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(ura.user_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(ura.role_code) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listMenus(Map<String, Object> p) {
        return "SELECT menu_id as \"menuId\", parent_menu_id as \"parentMenuId\", menu_name as \"menuName\", screen_id as \"screenId\", url as \"url\", icon as \"icon\", business_type as \"businessType\", description as \"description\", display_order as \"displayOrder\", use_yn as \"useYn\" FROM menu WHERE url IS NOT NULL" +
            keywordWhere(p, "lower(menu_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(business_type,'')) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY coalesce(parent_menu_id,''), display_order LIMIT #{size} OFFSET #{offset}";
    }

    public String countMenus(Map<String, Object> p) {
        return "SELECT count(*) FROM menu WHERE url IS NOT NULL" + keywordWhere(p, "lower(menu_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(business_type,'')) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listCodeGroups(Map<String, Object> p) {
        return "SELECT group_id as \"groupId\", group_name as \"groupName\", description as \"description\", managing_department as \"managingDepartment\", use_yn as \"useYn\" FROM code_group WHERE 1=1" +
            keywordWhere(p, "lower(group_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(group_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(managing_department,'')) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY group_id LIMIT #{size} OFFSET #{offset}";
    }

    public String countCodeGroups(Map<String, Object> p) {
        return "SELECT count(*) FROM code_group WHERE 1=1" + keywordWhere(p, "lower(group_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(group_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(managing_department,'')) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listBatchDefinitions(Map<String, Object> p) {
        return "SELECT b.batch_id as \"batchId\", b.batch_type as \"batchType\", b.schedule as \"schedule\", b.predecessor_batch_id as \"predecessorBatchId\", b.successor_batch_id as \"successorBatchId\", b.execution_parameters as \"executionParameters\", b.max_execution_seconds as \"maxExecutionSeconds\", b.owner_user_id as \"ownerUserId\", u.display_name as \"ownerName\", b.created_at as \"createdAt\", b.updated_at as \"updatedAt\" FROM batch_definition b LEFT JOIN user_account u ON u.user_id=b.owner_user_id WHERE 1=1" +
            keywordWhere(p, "lower(b.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(b.batch_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(b.schedule) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(u.display_name,'')) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY b.batch_id LIMIT #{size} OFFSET #{offset}";
    }

    public String countBatchDefinitions(Map<String, Object> p) {
        return "SELECT count(*) FROM batch_definition b LEFT JOIN user_account u ON u.user_id=b.owner_user_id WHERE 1=1" + keywordWhere(p, "lower(b.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(b.batch_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(b.schedule) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(u.display_name,'')) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listBatchExecutions(Map<String, Object> p) {
        return "SELECT e.execution_id as \"executionId\", e.batch_id as \"batchId\", d.batch_type as \"batchType\", e.execution_parameters as \"executionParameters\", e.action_type as \"actionType\", e.action_reason as \"actionReason\", e.operator_user_id as \"operatorUserId\", u.display_name as \"operatorName\", e.execution_status as \"executionStatus\", e.original_execution_id as \"originalExecutionId\", e.created_at as \"createdAt\", e.updated_at as \"updatedAt\" FROM batch_execution e JOIN batch_definition d ON d.batch_id=e.batch_id LEFT JOIN user_account u ON u.user_id=e.operator_user_id WHERE 1=1" +
            keywordWhere(p, "lower(e.execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(d.batch_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.execution_status) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY e.created_at DESC, e.execution_id DESC LIMIT #{size} OFFSET #{offset}";
    }

    public String countBatchExecutions(Map<String, Object> p) {
        return "SELECT count(*) FROM batch_execution e JOIN batch_definition d ON d.batch_id=e.batch_id WHERE 1=1" + keywordWhere(p, "lower(e.execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(d.batch_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.execution_status) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listBatchResults(Map<String, Object> p) {
        return "SELECT r.execution_id as \"executionId\", e.batch_id as \"batchId\", d.batch_type as \"batchType\", r.started_at as \"startedAt\", r.ended_at as \"endedAt\", r.total_count as \"totalCount\", r.success_count as \"successCount\", r.failure_count as \"failureCount\", r.excluded_count as \"excludedCount\", r.elapsed_seconds as \"elapsedSeconds\", r.log_file_ref as \"logFileRef\" FROM batch_result r JOIN batch_execution e ON e.execution_id=r.execution_id JOIN batch_definition d ON d.batch_id=e.batch_id WHERE 1=1" +
            executionIdWhere(p) + keywordWhere(p, "lower(r.execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(d.batch_type) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY r.started_at DESC, r.execution_id DESC LIMIT #{size} OFFSET #{offset}";
    }

    public String countBatchResults(Map<String, Object> p) {
        return "SELECT count(*) FROM batch_result r JOIN batch_execution e ON e.execution_id=r.execution_id JOIN batch_definition d ON d.batch_id=e.batch_id WHERE 1=1" + executionIdWhere(p) + keywordWhere(p, "lower(r.execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(d.batch_type) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listBatchReprocessTargets(Map<String, Object> p) {
        return "SELECT r.execution_id as \"executionId\", e.batch_id as \"batchId\", d.batch_type as \"batchType\", r.failure_count as \"failureCount\", r.log_file_ref as \"logFileRef\", r.started_at as \"startedAt\", r.ended_at as \"endedAt\" FROM batch_result r JOIN batch_execution e ON e.execution_id=r.execution_id JOIN batch_definition d ON d.batch_id=e.batch_id WHERE r.failure_count > 0" +
            keywordWhere(p, "lower(r.execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(d.batch_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(r.log_file_ref,'')) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY r.started_at DESC, r.execution_id DESC LIMIT #{size} OFFSET #{offset}";
    }

    public String countBatchReprocessTargets(Map<String, Object> p) {
        return "SELECT count(*) FROM batch_result r JOIN batch_execution e ON e.execution_id=r.execution_id JOIN batch_definition d ON d.batch_id=e.batch_id WHERE r.failure_count > 0" + keywordWhere(p, "lower(r.execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(e.batch_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(d.batch_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(r.log_file_ref,'')) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listBatchReprocessResults(Map<String, Object> p) {
        return "SELECT rp.reprocess_execution_id as \"reprocessExecutionId\", rp.original_execution_id as \"originalExecutionId\", rp.failed_target_id as \"failedTargetId\", rp.reprocess_reason as \"reprocessReason\", rp.reprocess_result as \"reprocessResult\", rp.created_at as \"createdAt\" FROM batch_reprocess rp WHERE 1=1" +
            keywordWhere(p, "lower(rp.reprocess_execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(rp.original_execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(rp.failed_target_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(rp.reprocess_result) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY rp.created_at DESC, rp.reprocess_execution_id DESC LIMIT #{size} OFFSET #{offset}";
    }

    public String countBatchReprocessResults(Map<String, Object> p) {
        return "SELECT count(*) FROM batch_reprocess rp WHERE 1=1" + keywordWhere(p, "lower(rp.reprocess_execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(rp.original_execution_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(rp.failed_target_id) LIKE lower(concat('%', #{keyword}, '%')) OR lower(rp.reprocess_result) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listPositions(Map<String, Object> p) {
        return "SELECT pa.position_assignment_id as \"positionAssignmentId\", pa.position_code as \"positionCode\", dc.code_name as \"positionName\", pa.user_id as \"userId\", u.display_name as \"userName\", pa.organization_code as \"organizationCode\", o.organization_name as \"organizationName\", pa.valid_from as \"validFrom\", pa.valid_to as \"validTo\", pa.created_at as \"createdAt\", pa.updated_at as \"updatedAt\" FROM position_assignment pa JOIN user_account u ON u.user_id=pa.user_id JOIN organization o ON o.organization_code=pa.organization_code LEFT JOIN detail_code dc ON dc.group_id='POSITION_CODE' AND dc.code_value=pa.position_code WHERE 1=1" +
            baseDateWhere(p, "pa.valid_from", "pa.valid_to") +
            keywordWhere(p, "lower(pa.position_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(u.display_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_name) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY pa.position_code, pa.organization_code, pa.user_id, pa.valid_from LIMIT #{size} OFFSET #{offset}";
    }

    public String countPositions(Map<String, Object> p) {
        return "SELECT count(*) FROM position_assignment pa JOIN user_account u ON u.user_id=pa.user_id JOIN organization o ON o.organization_code=pa.organization_code WHERE 1=1" +
            baseDateWhere(p, "pa.valid_from", "pa.valid_to") +
            keywordWhere(p, "lower(pa.position_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(u.display_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(o.organization_name) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listBusinessOwners(Map<String, Object> p) {
        return "SELECT boa.business_owner_assignment_id as \"businessOwnerAssignmentId\", boa.business_organization_code as \"businessOrganizationCode\", o.organization_name as \"businessOrganizationName\", boa.user_id as \"userId\", u.display_name as \"userName\", boa.work_area as \"workArea\", boa.assigned_from as \"assignedFrom\", boa.assigned_to as \"assignedTo\", boa.data_scope as \"dataScope\", boa.process_permission as \"processPermission\" FROM business_owner_assignment boa JOIN organization o ON o.organization_code=boa.business_organization_code JOIN user_account u ON u.user_id=boa.user_id WHERE 1=1" +
            baseDateWhere(p, "boa.assigned_from", "boa.assigned_to") +
            keywordWhere(p, "lower(boa.business_organization_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(u.display_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(boa.work_area) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY boa.business_organization_code, boa.work_area, boa.user_id, boa.assigned_from LIMIT #{size} OFFSET #{offset}";
    }

    public String countBusinessOwners(Map<String, Object> p) {
        return "SELECT count(*) FROM business_owner_assignment boa JOIN organization o ON o.organization_code=boa.business_organization_code JOIN user_account u ON u.user_id=boa.user_id WHERE 1=1" +
            baseDateWhere(p, "boa.assigned_from", "boa.assigned_to") +
            keywordWhere(p, "lower(boa.business_organization_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(u.display_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(boa.work_area) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    public String listDataScopes(Map<String, Object> p) {
        return "SELECT r.role_code as \"roleCode\", role.role_name as \"roleName\", r.data_scope_type as \"dataScopeType\", r.organization_code as \"organizationCode\", o.organization_name as \"organizationName\", r.work_area as \"workArea\", r.condition_json as \"conditionJson\" FROM role_data_scope_rule r JOIN role ON role.role_code=r.role_code LEFT JOIN organization o ON o.organization_code=r.organization_code WHERE 1=1" +
            keywordWhere(p, "lower(r.role_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(role.role_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(r.data_scope_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(r.work_area,'')) LIKE lower(concat('%', #{keyword}, '%'))") +
            " ORDER BY r.role_code LIMIT #{size} OFFSET #{offset}";
    }

    public String countDataScopes(Map<String, Object> p) {
        return "SELECT count(*) FROM role_data_scope_rule r JOIN role ON role.role_code=r.role_code WHERE 1=1" +
            keywordWhere(p, "lower(r.role_code) LIKE lower(concat('%', #{keyword}, '%')) OR lower(role.role_name) LIKE lower(concat('%', #{keyword}, '%')) OR lower(r.data_scope_type) LIKE lower(concat('%', #{keyword}, '%')) OR lower(coalesce(r.work_area,'')) LIKE lower(concat('%', #{keyword}, '%'))");
    }

    private String executionIdWhere(Map<String, Object> p) {
        if (!has(p, "executionId")) return "";
        return " AND r.execution_id = #{executionId}";
    }
}
