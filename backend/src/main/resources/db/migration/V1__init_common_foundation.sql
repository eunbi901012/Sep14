CREATE TABLE IF NOT EXISTS organization (
  organization_code varchar(30) PRIMARY KEY,
  organization_name varchar(200) NOT NULL,
  organization_type varchar(30) NOT NULL,
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE organization IS '대학·대학원·단과대학·학과·부서 조직 기준정보.';
COMMENT ON COLUMN organization.use_yn IS 'Y:사용|N:미사용';

CREATE TABLE IF NOT EXISTS korus_faculty_snapshot (
  faculty_no varchar(30) PRIMARY KEY,
  name varchar(100) NOT NULL,
  organization_code varchar(30) NOT NULL REFERENCES organization(organization_code),
  position_title varchar(100),
  employment_status varchar(30) NOT NULL DEFAULT 'ACTIVE',
  retirement_date date,
  last_synced_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE korus_faculty_snapshot IS 'KORUS 원천 인사 정보를 로컬 실행용 읽기 전용 스냅샷으로 보관한다.';
COMMENT ON COLUMN korus_faculty_snapshot.employment_status IS 'ACTIVE:재직|RETIRED:퇴직|LEAVE:휴직';

CREATE TABLE IF NOT EXISTS user_account (
  user_id varchar(40) PRIMARY KEY,
  faculty_no varchar(30) NOT NULL REFERENCES korus_faculty_snapshot(faculty_no),
  login_id varchar(80) NOT NULL UNIQUE,
  password_hash varchar(200) NOT NULL,
  display_name varchar(100) NOT NULL,
  business_role varchar(200),
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE user_account IS '내부 시스템 로그인 계정과 시스템 사용여부, 업무 역할을 관리한다.';
COMMENT ON COLUMN user_account.use_yn IS 'Y:사용|N:미사용';

CREATE TABLE IF NOT EXISTS organization_relation_history (
  relation_id varchar(40) PRIMARY KEY,
  organization_code varchar(30) NOT NULL REFERENCES organization(organization_code),
  parent_organization_code varchar(30) REFERENCES organization(organization_code),
  valid_from date NOT NULL,
  valid_to date,
  change_reason varchar(500),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE organization_relation_history IS '조직 상하위 관계와 적용기간 변경 이력을 보존한다.';

CREATE TABLE IF NOT EXISTS organization_user_mapping (
  mapping_id varchar(40) PRIMARY KEY,
  faculty_no varchar(30) NOT NULL REFERENCES korus_faculty_snapshot(faculty_no),
  organization_code varchar(30) NOT NULL REFERENCES organization(organization_code),
  position_title varchar(100),
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE organization_user_mapping IS '조직별 보직 또는 소속 사용자 매핑을 관리한다.';
COMMENT ON COLUMN organization_user_mapping.use_yn IS 'Y:사용|N:미사용';

CREATE TABLE IF NOT EXISTS role (
  role_code varchar(3) PRIMARY KEY CHECK (role_code IN ('R01','R02','R03','R04','R05','R06','R07','R08','R09')),
  role_name varchar(100) NOT NULL,
  purpose varchar(500) NOT NULL,
  assignment_criteria varchar(500),
  data_scope_default varchar(200),
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE role IS 'R01~R09 업무 역할의 목적, 부여 기준, 기본 데이터 범위를 관리한다.';
COMMENT ON COLUMN role.use_yn IS 'Y:사용|N:미사용';

CREATE TABLE IF NOT EXISTS user_role_assignment (
  assignment_id varchar(40) PRIMARY KEY,
  user_id varchar(40) NOT NULL REFERENCES user_account(user_id),
  role_code varchar(3) NOT NULL REFERENCES role(role_code),
  assignment_source varchar(20) NOT NULL CHECK (assignment_source IN ('POSITION','MANUAL')),
  approver_id varchar(40),
  valid_from date NOT NULL,
  valid_to date,
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE user_role_assignment IS '사용자별 역할 부여, 유효기간, 승인자와 부여방식을 관리한다.';
COMMENT ON COLUMN user_role_assignment.status IS 'ACTIVE:활성|REVOKED:회수';
COMMENT ON COLUMN user_role_assignment.approver_id IS 'user_account.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS menu (
  menu_id varchar(40) PRIMARY KEY,
  parent_menu_id varchar(40),
  menu_name varchar(150) NOT NULL,
  screen_id varchar(80),
  url varchar(200),
  icon varchar(80),
  business_type varchar(80),
  description varchar(500),
  display_order integer NOT NULL DEFAULT 0,
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE menu IS '시스템 관리 메뉴 계층과 실행 화면 연결 정보를 관리한다.';
COMMENT ON COLUMN menu.parent_menu_id IS 'menu.menu_id 참조 의도 (FK 미선언: seed 순환과 루트 메뉴 허용)';
COMMENT ON COLUMN menu.use_yn IS 'Y:사용|N:미사용';

CREATE TABLE IF NOT EXISTS menu_permission (
  permission_id varchar(80) PRIMARY KEY,
  target_type varchar(10) NOT NULL CHECK (target_type IN ('ROLE','ORG','USER')),
  target_id varchar(80) NOT NULL,
  menu_id varchar(40) NOT NULL REFERENCES menu(menu_id),
  allow_yn char(1) NOT NULL DEFAULT 'N' CHECK (allow_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (target_type, target_id, menu_id)
);
COMMENT ON TABLE menu_permission IS '역할·조직·사용자 대상별 메뉴 접근 허용 여부를 관리한다.';
COMMENT ON COLUMN menu_permission.allow_yn IS 'Y:허용|N:차단';

CREATE TABLE IF NOT EXISTS code_group (
  group_id varchar(60) PRIMARY KEY,
  group_name varchar(150) NOT NULL,
  description varchar(500),
  managing_department varchar(150),
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE code_group IS '공통코드 그룹 식별자, 명칭, 설명, 관리부서를 관리한다.';
COMMENT ON COLUMN code_group.use_yn IS 'Y:사용|N:미사용';

CREATE TABLE IF NOT EXISTS detail_code (
  group_id varchar(60) NOT NULL REFERENCES code_group(group_id),
  code_value varchar(60) NOT NULL,
  code_name varchar(150) NOT NULL,
  parent_code_value varchar(60),
  sort_order integer NOT NULL DEFAULT 0,
  additional_attributes text,
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id, code_value)
);
COMMENT ON TABLE detail_code IS '코드그룹별 상세코드, 계층, 정렬순서와 연계 속성을 관리한다.';
COMMENT ON COLUMN detail_code.use_yn IS 'Y:사용|N:미사용';

CREATE TABLE IF NOT EXISTS app_session (
  session_id varchar(80) PRIMARY KEY,
  user_id varchar(40) NOT NULL REFERENCES user_account(user_id),
  expires_at timestamp,
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE app_session IS '로그인 세션 상태와 만료 시각을 관리한다.';
COMMENT ON COLUMN app_session.status IS 'ACTIVE:활성|EXPIRED:만료|LOGGED_OUT:로그아웃';

CREATE TABLE IF NOT EXISTS system_common_setting (
  setting_key varchar(60) PRIMARY KEY,
  setting_name varchar(150) NOT NULL,
  setting_value varchar(60) NOT NULL,
  value_unit varchar(20) NOT NULL CHECK (value_unit IN ('MINUTE','ROW','DAY','COUNT','SECOND')),
  default_value varchar(60) NOT NULL,
  min_value integer NOT NULL,
  max_value integer NOT NULL,
  description varchar(500) NOT NULL,
  display_order integer NOT NULL DEFAULT 0,
  use_yn char(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE system_common_setting IS '시스템 전체에 공통 적용되는 세션·조회·검색기간·대량조회·장시간작업 안내 기준 환경설정 값을 관리한다.';
COMMENT ON COLUMN system_common_setting.value_unit IS 'MINUTE:분|ROW:행|DAY:일|COUNT:건|SECOND:초';
COMMENT ON COLUMN system_common_setting.use_yn IS 'Y:사용|N:미사용';

CREATE INDEX IF NOT EXISTS idx_user_account_login_id ON user_account(login_id);
CREATE INDEX IF NOT EXISTS idx_user_role_user_status ON user_role_assignment(user_id, status);
CREATE INDEX IF NOT EXISTS idx_menu_parent_order ON menu(parent_menu_id, display_order);
CREATE INDEX IF NOT EXISTS idx_menu_permission_target ON menu_permission(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_detail_code_group_order ON detail_code(group_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_session_user_status ON app_session(user_id, status);
CREATE INDEX IF NOT EXISTS idx_system_common_setting_order ON system_common_setting(display_order, setting_key);

INSERT INTO organization (organization_code, organization_name, organization_type)
SELECT v.organization_code, v.organization_name, v.organization_type
FROM (VALUES
  ('KNUE','한국교원대학교','UNIVERSITY'),
  ('EDU','교육대학','COLLEGE'),
  ('CSE','컴퓨터교육과','DEPARTMENT')
) AS v(organization_code, organization_name, organization_type)
WHERE NOT EXISTS (
  SELECT 1 FROM organization o WHERE o.organization_code = v.organization_code
);

INSERT INTO korus_faculty_snapshot (faculty_no, name, organization_code, position_title, employment_status, last_synced_at)
SELECT v.faculty_no, v.name, v.organization_code, v.position_title, v.employment_status, CURRENT_TIMESTAMP
FROM (VALUES
  ('F0001','관리자','CSE','시스템관리자','ACTIVE'),
  ('F1001','김교원','CSE','교수','ACTIVE'),
  ('F1002','이학과장','CSE','학과장','ACTIVE')
) AS v(faculty_no, name, organization_code, position_title, employment_status)
WHERE NOT EXISTS (
  SELECT 1 FROM korus_faculty_snapshot k WHERE k.faculty_no = v.faculty_no
);

INSERT INTO user_account (user_id, faculty_no, login_id, password_hash, display_name, business_role)
SELECT v.user_id, v.faculty_no, v.login_id, v.password_hash, v.display_name, v.business_role
FROM (VALUES
  ('U-ADMIN','F0001','admin','admin','관리자','시스템관리자'),
  ('U-F1001','F1001','f1001','password','김교원','교원'),
  ('U-F1002','F1002','f1002','password','이학과장','학과장')
) AS v(user_id, faculty_no, login_id, password_hash, display_name, business_role)
WHERE NOT EXISTS (
  SELECT 1 FROM user_account u WHERE u.user_id = v.user_id
);

INSERT INTO role (role_code, role_name, purpose, assignment_criteria, data_scope_default)
SELECT v.role_code, v.role_name, v.purpose, v.assignment_criteria, v.data_scope_default
FROM (VALUES
  ('R01','교원','본인 관련 업무를 수행하는 일반 사용자 역할','재직 교원','본인'),
  ('R02','학과장','소속 학과 교원 관련 업무 확인 역할','학과장 보직자','소속 학과'),
  ('R03','단과대학(원) 행정실','단과대학 또는 대학원 행정 처리 역할','행정실 담당자','소속 단과대학'),
  ('R04','교수지원과','기준정보와 평가 관련 행정 관리 역할','교수지원과 담당자','전체'),
  ('R05','산학협력단','연구비·간접비·지식재산 관련 자료 관리 역할','산학협력단 담당자','연구 관련'),
  ('R06','입학인재관리과','입학·취업률 관련 자료 관리 역할','입학인재관리과 담당자','입학/취업'),
  ('R07','실적부서','담당 실적 자료 관리 역할','실적 담당 부서','담당 실적'),
  ('R08','점수산출 감사자','산출 과정과 근거를 조회하는 감사 역할','감사 담당자','감사 조회'),
  ('R09','시스템관리자','사용자·조직·메뉴·권한·코드 관리를 수행하는 관리자 역할','시스템 관리자','전체')
) AS v(role_code, role_name, purpose, assignment_criteria, data_scope_default)
WHERE NOT EXISTS (
  SELECT 1 FROM role r WHERE r.role_code = v.role_code
);

INSERT INTO user_role_assignment (assignment_id, user_id, role_code, assignment_source, approver_id, valid_from, status)
SELECT v.assignment_id, v.user_id, v.role_code, v.assignment_source, v.approver_id, CURRENT_DATE, v.status
FROM (VALUES
  ('URA-ADMIN-R09','U-ADMIN','R09','MANUAL','U-ADMIN','ACTIVE'),
  ('URA-F1001-R01','U-F1001','R01','POSITION','U-ADMIN','ACTIVE'),
  ('URA-F1002-R02','U-F1002','R02','POSITION','U-ADMIN','ACTIVE')
) AS v(assignment_id, user_id, role_code, assignment_source, approver_id, status)
WHERE NOT EXISTS (
  SELECT 1 FROM user_role_assignment ura WHERE ura.assignment_id = v.assignment_id
);

INSERT INTO organization_relation_history (relation_id, organization_code, parent_organization_code, valid_from)
SELECT v.relation_id, v.organization_code, v.parent_organization_code, CURRENT_DATE
FROM (VALUES
  ('REL-EDU','EDU','KNUE'),
  ('REL-CSE','CSE','EDU')
) AS v(relation_id, organization_code, parent_organization_code)
WHERE NOT EXISTS (
  SELECT 1 FROM organization_relation_history h WHERE h.relation_id = v.relation_id
);

INSERT INTO menu (menu_id, parent_menu_id, menu_name, screen_id, url, icon, business_type, description, display_order)
SELECT v.menu_id, v.parent_menu_id, v.menu_name, v.screen_id, v.url, v.icon, v.business_type, v.description, v.display_order
FROM (VALUES
  ('MENU-SYS',NULL,'시스템 관리',NULL,NULL,'settings','COMMON','시스템 관리 루트',1),
  ('MENU-USER-ORG','MENU-SYS','사용자·조직 관리',NULL,NULL,'users','COMMON','사용자와 조직 관리',1),
  ('MENU-AUTH','MENU-SYS','역할·권한 관리',NULL,NULL,'shield','COMMON','역할과 권한 관리',2),
  ('MENU-MENU','MENU-SYS','메뉴 관리',NULL,NULL,'menu','COMMON','메뉴 관리',3),
  ('MENU-CODE','MENU-SYS','공통코드 관리',NULL,NULL,'code','COMMON','공통코드 관리',4),
  ('MENU-COMMON-SETTINGS','MENU-SYS','공통 환경설정','UI-006','/admin/common-settings','settings','COMMON','공통 환경설정',5),
  ('MENU-USERS','MENU-USER-ORG','사용자 관리','SCR-CMN-USER','/admin/users','user','COMMON','사용자 관리',1),
  ('MENU-ORGS','MENU-USER-ORG','조직 관리','SCR-CMN-ORG','/admin/organizations','tree','COMMON','조직 관리',2),
  ('MENU-ROLES','MENU-AUTH','역할 관리','SCR-CMN-ROLE','/admin/roles','role','COMMON','역할 관리',1),
  ('MENU-USER-ROLES','MENU-AUTH','사용자 역할 관리','SCR-CMN-USER-ROLE','/admin/user-roles','user-role','COMMON','사용자 역할 관리',2),
  ('MENU-PERMS','MENU-AUTH','메뉴 권한 관리','SCR-CMN-MENU-PERM','/admin/menu-permissions','permission','COMMON','메뉴 권한 관리',3),
  ('MENU-STRUCT','MENU-MENU','메뉴 구조 관리','SCR-CMN-MENU-STRUCT','/admin/menu-structure','sitemap','COMMON','메뉴 구조 관리',1),
  ('MENU-INFOS','MENU-MENU','메뉴 정보 관리','SCR-CMN-MENU-INFO','/admin/menus','info','COMMON','메뉴 정보 관리',2),
  ('MENU-CODE-GROUPS','MENU-CODE','코드그룹 관리','SCR-CMN-CODE-GROUP','/admin/code-groups','folder','COMMON','코드그룹 관리',1),
  ('MENU-DETAIL-CODES','MENU-CODE','상세코드 관리','SCR-CMN-DETAIL-CODE','/admin/detail-codes','list','COMMON','상세코드 관리',2)
) AS v(menu_id, parent_menu_id, menu_name, screen_id, url, icon, business_type, description, display_order)
WHERE NOT EXISTS (
  SELECT 1 FROM menu m WHERE m.menu_id = v.menu_id
);

INSERT INTO menu_permission (permission_id, target_type, target_id, menu_id, allow_yn)
SELECT 'PERM-R09-' || m.menu_id, 'ROLE', 'R09', m.menu_id, 'Y'
FROM menu m
WHERE NOT EXISTS (
  SELECT 1 FROM menu_permission p WHERE p.target_type = 'ROLE' AND p.target_id = 'R09' AND p.menu_id = m.menu_id
);

INSERT INTO system_common_setting (setting_key, setting_name, setting_value, value_unit, default_value, min_value, max_value, description, display_order)
SELECT v.setting_key, v.setting_name, v.setting_value, v.value_unit, v.default_value, v.min_value, v.max_value, v.description, v.display_order
FROM (VALUES
  ('SESSION_IDLE_MINUTES','세션 유휴시간','30','MINUTE','30',5,240,'사용자 활동이 없을 때 인증 세션을 유지하는 최대 분 단위 시간',1),
  ('PAGE_SIZE','페이지당 조회건수','20','ROW','20',10,100,'목록 화면에서 한 페이지에 표시하는 기본 행 수',2),
  ('DEFAULT_SEARCH_PERIOD_DAYS','기본 검색기간','30','DAY','30',1,365,'기간 검색 조건의 기본 일 단위 범위',3),
  ('BULK_QUERY_THRESHOLD_COUNT','대량조회 기준건수','1000','COUNT','1000',100,10000,'대량 조회로 판단하는 결과 건수 기준',4),
  ('LONG_TASK_NOTICE_SECONDS','장시간작업 안내 기준','10','SECOND','10',1,300,'처리가 길어질 때 사용자 안내를 표시하는 초 단위 기준',5)
) AS v(setting_key, setting_name, setting_value, value_unit, default_value, min_value, max_value, description, display_order)
WHERE NOT EXISTS (
  SELECT 1 FROM system_common_setting s WHERE s.setting_key = v.setting_key
);

INSERT INTO code_group (group_id, group_name, description, managing_department)
SELECT v.group_id, v.group_name, v.description, v.managing_department
FROM (VALUES
  ('USE_YN','사용여부','공통 사용여부 코드','시스템관리'),
  ('ORG_TYPE','조직유형','조직 구분 코드','시스템관리'),
  ('ASSIGNMENT_SOURCE','역할부여방식','사용자 역할 부여방식 코드','시스템관리')
) AS v(group_id, group_name, description, managing_department)
WHERE NOT EXISTS (
  SELECT 1 FROM code_group cg WHERE cg.group_id = v.group_id
);

INSERT INTO detail_code (group_id, code_value, code_name, sort_order, additional_attributes)
SELECT v.group_id, v.code_value, v.code_name, v.sort_order, v.additional_attributes
FROM (VALUES
  ('USE_YN','Y','사용',1,'{}'),
  ('USE_YN','N','미사용',2,'{}'),
  ('ORG_TYPE','UNIVERSITY','대학',1,'{}'),
  ('ORG_TYPE','COLLEGE','단과대학',2,'{}'),
  ('ORG_TYPE','DEPARTMENT','학과',3,'{}'),
  ('ASSIGNMENT_SOURCE','POSITION','보직 기반',1,'{}'),
  ('ASSIGNMENT_SOURCE','MANUAL','수동 부여',2,'{}')
) AS v(group_id, code_value, code_name, sort_order, additional_attributes)
WHERE NOT EXISTS (
  SELECT 1 FROM detail_code dc WHERE dc.group_id = v.group_id AND dc.code_value = v.code_value
);
