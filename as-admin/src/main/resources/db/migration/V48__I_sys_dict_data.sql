ALTER TABLE sys_dict_data CHANGE remark remark TEXT;

insert into sys_dict_data values(304, 5,  'webhook mail',     'mail_template_webhook',       'job_push_template',   '',   '',  'N', '0', 'admin', sysdate(), '', null, '[webhook]{title}
CreateTime: {time}({reporter})
Descr: {descr}
Remark: {remark}');


