-- 菜单 SQL
insert into sys_menu (menu_name, menu_name_tw, menu_name_us, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Webhook Record', 'Webhook Record', 'Webhook Record', (select a.menu_id from ( select menu_id from sys_menu where menu_name = 'AS Monitor' and  parent_id =0) a), '99', '/as/webhook', 'C', '0', 'as:webhook:view', 'fa fa-navicon', 'admin', sysdate(), '', null, 'webhook请求记录菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('webhook请求记录查询', @parentId, '1',  '#',  'F', '0', 'as:webhook:list',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('webhook请求记录删除', @parentId, '2',  '#',  'F', '0', 'as:webhook:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('webhook请求记录导出', @parentId, '3',  '#',  'F', '0', 'as:webhook:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('webhook请求记录详情', @parentId, '4',  '#',  'F', '0', 'as:webhook:detail',       '#', 'admin', sysdate(), '', null, '');
