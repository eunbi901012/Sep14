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
}
