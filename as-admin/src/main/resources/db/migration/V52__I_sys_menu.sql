-- 菜单 SQL
insert into sys_menu (menu_name, menu_name_tw, menu_name_us, parent_id, order_num, url, target, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('快捷查询', '快捷查詢', 'Quick Query', '0', '2', '/as/sqlTemplate', 'menuItem', 'C', '0', 'as:sqlTemplate:view', 'fa fa-search', 'admin', sysdate(), '', null, 'SQL模板菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL模板查询', @parentId, '1',  '#',  'F', '0', 'as:sqlTemplate:list',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL模板新增', @parentId, '2',  '#',  'F', '0', 'as:sqlTemplate:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL模板修改', @parentId, '3',  '#',  'F', '0', 'as:sqlTemplate:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL模板删除', @parentId, '4',  '#',  'F', '0', 'as:sqlTemplate:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL模板导出', @parentId, '5',  '#',  'F', '0', 'as:sqlTemplate:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL模板状态修改', @parentId, '6',  '#',  'F', '0', 'as:sqlTemplate:changeStatus',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL模板使用', @parentId, '7',  '#',  'F', '0', 'as:sqlTemplate:use',       '#', 'admin', sysdate(), '', null, '');
