package kr.ac.knue.faculty.common.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommonMapper {
    @Select("SELECT u.user_id as \"userId\", u.login_id as \"loginId\", u.password_hash as \"passwordHash\", u.display_name as \"displayName\", u.use_yn as \"useYn\" FROM user_account u WHERE u.login_id = #{loginId}")
    Map<String, Object> findLoginUser(@Param("loginId") String loginId);

    @Insert("INSERT INTO app_session (session_id, user_id, expires_at, status) VALUES (#{sessionId}, #{userId}, #{expiresAt}, 'ACTIVE')")
    int createSession(@Param("sessionId") String sessionId, @Param("userId") String userId, @Param("expiresAt") LocalDateTime expiresAt);

    @Update("UPDATE app_session SET status='LOGGED_OUT', updated_at=CURRENT_TIMESTAMP WHERE session_id=#{sessionId}")
    int logout(@Param("sessionId") String sessionId);

    @Select("SELECT s.session_id as \"sessionId\", s.user_id as \"userId\", u.login_id as \"loginId\", u.display_name as \"displayName\" FROM app_session s JOIN user_account u ON u.user_id=s.user_id WHERE s.session_id=#{sessionId} AND s.status='ACTIVE' AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP) AND u.use_yn='Y'")
    Map<String, Object> findSession(@Param("sessionId") String sessionId);

    @Select("SELECT role_code FROM user_role_assignment WHERE user_id=#{userId} AND status='ACTIVE' ORDER BY role_code")
    List<String> roleCodes(@Param("userId") String userId);

    @Select("SELECT m.url FROM menu_permission p JOIN menu m ON m.menu_id=p.menu_id WHERE p.target_type='ROLE' AND p.target_id IN (SELECT role_code FROM user_role_assignment WHERE user_id=#{userId} AND status='ACTIVE') AND p.allow_yn='Y' AND m.url IS NOT NULL ORDER BY m.display_order")
    List<String> menuPaths(@Param("userId") String userId);

    @SelectProvider(type = SqlProvider.class, method = "listUsers")
    List<Map<String, Object>> listUsers(Map<String, Object> params);

    @SelectProvider(type = SqlProvider.class, method = "countUsers")
    long countUsers(Map<String, Object> params);

    @Update("UPDATE user_account SET use_yn=#{systemUseYn}, business_role=#{businessRole}, updated_at=CURRENT_TIMESTAMP WHERE user_id=#{userId}")
    int updateUserAccess(@Param("userId") String userId, @Param("systemUseYn") String systemUseYn, @Param("businessRole") String businessRole);

    @SelectProvider(type = SqlProvider.class, method = "listOrganizations")
    List<Map<String, Object>> listOrganizations(Map<String, Object> params);

    @SelectProvider(type = SqlProvider.class, method = "countOrganizations")
    long countOrganizations(Map<String, Object> params);

    @Select("SELECT o.organization_code as \"orgCode\", o.organization_name as \"orgName\", o.organization_type as \"orgType\", h.parent_organization_code as \"parentOrgCode\", h.valid_from as \"effectiveStartDate\", h.valid_to as \"effectiveEndDate\" FROM organization o LEFT JOIN organization_relation_history h ON h.organization_code=o.organization_code WHERE o.use_yn='Y' ORDER BY coalesce(h.parent_organization_code,''), o.organization_code")
    List<Map<String, Object>> organizationTree();

    @Insert("INSERT INTO organization_relation_history (relation_id, organization_code, parent_organization_code, valid_from, valid_to, change_reason) VALUES (#{relationId}, #{orgCode}, #{parentOrgCode}, #{from}, #{to}, #{reason})")
    int insertOrgRelationship(@Param("relationId") String relationId, @Param("orgCode") String orgCode, @Param("parentOrgCode") String parentOrgCode, @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("reason") String reason);

    @SelectProvider(type = SqlProvider.class, method = "listRoles")
    List<Map<String, Object>> listRoles(Map<String, Object> params);

    @SelectProvider(type = SqlProvider.class, method = "countRoles")
    long countRoles(Map<String, Object> params);

    @Update("UPDATE role SET role_name=coalesce(#{roleName}, role_name), assignment_criteria=#{grantCriteria}, data_scope_default=#{defaultDataScope}, updated_at=CURRENT_TIMESTAMP WHERE role_code=#{roleCode}")
    int updateRole(@Param("roleCode") String roleCode, @Param("roleName") String roleName, @Param("grantCriteria") String grantCriteria, @Param("defaultDataScope") String defaultDataScope);

    @SelectProvider(type = SqlProvider.class, method = "listUserRoles")
    List<Map<String, Object>> listUserRoles(Map<String, Object> params);

    @SelectProvider(type = SqlProvider.class, method = "countUserRoles")
    long countUserRoles(Map<String, Object> params);

    @Insert("INSERT INTO user_role_assignment (assignment_id, user_id, role_code, assignment_source, approver_id, valid_from, valid_to, status) VALUES (#{assignmentId}, #{userId}, #{roleCode}, #{assignmentSource}, #{approverId}, #{validFrom}, #{validTo}, 'ACTIVE')")
    int insertUserRole(@Param("assignmentId") String assignmentId, @Param("userId") String userId, @Param("roleCode") String roleCode, @Param("assignmentSource") String assignmentSource, @Param("approverId") String approverId, @Param("validFrom") LocalDate validFrom, @Param("validTo") LocalDate validTo);

    @Update("UPDATE user_role_assignment SET role_code=#{roleCode}, approver_id=#{approverId}, valid_from=#{validFrom}, valid_to=#{validTo}, updated_at=CURRENT_TIMESTAMP WHERE assignment_id=#{assignmentId} AND status='ACTIVE'")
    int updateUserRole(@Param("assignmentId") String assignmentId, @Param("roleCode") String roleCode, @Param("approverId") String approverId, @Param("validFrom") LocalDate validFrom, @Param("validTo") LocalDate validTo);

    @Update("UPDATE user_role_assignment SET status='REVOKED', updated_at=CURRENT_TIMESTAMP WHERE assignment_id=#{assignmentId}")
    int revokeUserRole(@Param("assignmentId") String assignmentId);

    @Select("SELECT p.target_type as \"targetType\", p.target_id as \"targetId\", p.menu_id as \"menuId\", CASE WHEN m.parent_menu_id IS NULL THEN 'ROOT' WHEN m.url IS NULL THEN 'GROUP' ELSE 'SCREEN' END as \"menuLevel\", m.menu_name as \"menuName\", p.allow_yn as \"allowYn\" FROM menu_permission p JOIN menu m ON m.menu_id=p.menu_id WHERE p.target_type=#{targetType} AND p.target_id=#{targetId} ORDER BY m.display_order")
    List<Map<String, Object>> permissions(@Param("targetType") String targetType, @Param("targetId") String targetId);

    @Delete("DELETE FROM menu_permission WHERE target_type=#{targetType} AND target_id=#{targetId}")
    int deletePermissions(@Param("targetType") String targetType, @Param("targetId") String targetId);

    @Insert("INSERT INTO menu_permission (permission_id, target_type, target_id, menu_id, allow_yn) VALUES (#{permissionId}, #{targetType}, #{targetId}, #{menuId}, #{allowYn})")
    int insertPermission(@Param("permissionId") String permissionId, @Param("targetType") String targetType, @Param("targetId") String targetId, @Param("menuId") String menuId, @Param("allowYn") String allowYn);

    @Select("SELECT menu_id as \"menuId\", menu_name as \"menuName\", parent_menu_id as \"parentMenuId\", display_order as \"displayOrder\" FROM menu ORDER BY coalesce(parent_menu_id,''), display_order")
    List<Map<String, Object>> menuTree();

    @Update("UPDATE menu SET parent_menu_id=#{parentMenuId}, updated_at=CURRENT_TIMESTAMP WHERE menu_id=#{menuId}")
    int updateMenuParent(@Param("menuId") String menuId, @Param("parentMenuId") String parentMenuId);

    @Update("UPDATE menu SET display_order=#{displayOrder}, updated_at=CURRENT_TIMESTAMP WHERE menu_id=#{menuId}")
    int updateMenuOrder(@Param("menuId") String menuId, @Param("displayOrder") int displayOrder);

    @SelectProvider(type = SqlProvider.class, method = "listMenus")
    List<Map<String, Object>> listMenus(Map<String, Object> params);

    @SelectProvider(type = SqlProvider.class, method = "countMenus")
    long countMenus(Map<String, Object> params);

    @Insert("INSERT INTO menu (menu_id, menu_name, screen_id, url, icon, business_type, description, display_order, use_yn) VALUES (#{menuId}, #{menuName}, #{screenId}, #{url}, #{icon}, #{businessType}, #{description}, 99, #{useYn})")
    int insertMenu(@Param("menuId") String menuId, @Param("menuName") String menuName, @Param("screenId") String screenId, @Param("url") String url, @Param("icon") String icon, @Param("businessType") String businessType, @Param("description") String description, @Param("useYn") String useYn);

    @Update("UPDATE menu SET menu_name=#{menuName}, screen_id=#{screenId}, url=#{url}, icon=#{icon}, business_type=#{businessType}, description=#{description}, use_yn=#{useYn}, updated_at=CURRENT_TIMESTAMP WHERE menu_id=#{menuId}")
    int updateMenu(@Param("menuId") String menuId, @Param("menuName") String menuName, @Param("screenId") String screenId, @Param("url") String url, @Param("icon") String icon, @Param("businessType") String businessType, @Param("description") String description, @Param("useYn") String useYn);

    @SelectProvider(type = SqlProvider.class, method = "listCodeGroups")
    List<Map<String, Object>> listCodeGroups(Map<String, Object> params);

    @SelectProvider(type = SqlProvider.class, method = "countCodeGroups")
    long countCodeGroups(Map<String, Object> params);

    @Insert("INSERT INTO code_group (group_id, group_name, description, managing_department, use_yn) VALUES (#{groupId}, #{groupName}, #{description}, #{managingDepartment}, #{useYn})")
    int insertCodeGroup(@Param("groupId") String groupId, @Param("groupName") String groupName, @Param("description") String description, @Param("managingDepartment") String managingDepartment, @Param("useYn") String useYn);

    @Update("UPDATE code_group SET group_name=#{groupName}, description=#{description}, managing_department=#{managingDepartment}, use_yn=#{useYn}, updated_at=CURRENT_TIMESTAMP WHERE group_id=#{groupId}")
    int updateCodeGroup(@Param("groupId") String groupId, @Param("groupName") String groupName, @Param("description") String description, @Param("managingDepartment") String managingDepartment, @Param("useYn") String useYn);

    @Select("SELECT group_id as \"groupId\", code_value as \"codeValue\", code_name as \"codeName\", parent_code_value as \"parentCodeValue\", sort_order as \"sortOrder\", additional_attributes as \"additionalAttributes\", use_yn as \"useYn\" FROM detail_code WHERE group_id=#{groupId} ORDER BY sort_order, code_value")
    List<Map<String, Object>> detailCodes(@Param("groupId") String groupId);

    @Insert("INSERT INTO detail_code (group_id, code_value, code_name, parent_code_value, sort_order, additional_attributes, use_yn) VALUES (#{groupId}, #{codeValue}, #{codeName}, #{parentCodeValue}, #{sortOrder}, #{additionalAttributes}, #{useYn})")
    int insertDetailCode(@Param("groupId") String groupId, @Param("codeValue") String codeValue, @Param("codeName") String codeName, @Param("parentCodeValue") String parentCodeValue, @Param("sortOrder") int sortOrder, @Param("additionalAttributes") String additionalAttributes, @Param("useYn") String useYn);

    @Update("UPDATE detail_code SET code_name=#{codeName}, parent_code_value=#{parentCodeValue}, sort_order=#{sortOrder}, additional_attributes=#{additionalAttributes}, use_yn=#{useYn}, updated_at=CURRENT_TIMESTAMP WHERE group_id=#{groupId} AND code_value=#{codeValue}")
    int updateDetailCode(@Param("groupId") String groupId, @Param("codeValue") String codeValue, @Param("codeName") String codeName, @Param("parentCodeValue") String parentCodeValue, @Param("sortOrder") int sortOrder, @Param("additionalAttributes") String additionalAttributes, @Param("useYn") String useYn);
}
