insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SQL检测任务导入', (select a.menu_id from ( select menu_id from sys_menu where perms = 'monitor:sqlJob:view' and url = '/monitor/sqlJob') a), '6',  '#',  'F', '0', 'monitor:sqlJob:import',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('ElasticSearch任务导入', (select a.menu_id from ( select menu_id from sys_menu where perms = 'monitor:elasticJob:view' and url = '/monitor/elasticJob') a), '6',  '#',  'F', '0', 'monitor:elasticJob:import',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('API检测任务导入', (select a.menu_id from ( select menu_id from sys_menu where perms = 'monitor:apiJob:view' and url = '/monitor/apiJob') a), '6',  '#',  'F', '0', 'monitor:apiJob:import',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('自动报表任务导入', (select a.menu_id from ( select menu_id from sys_menu where perms = 'monitor:exportJob:view' and url = '/monitor/exportJob') a), '6',  '#',  'F', '0', 'monitor:exportJob:import',       '#', 'admin', sysdate(), '', null, '');