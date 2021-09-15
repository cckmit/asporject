ALTER TABLE moni_api_log ADD IS_ALERT VARCHAR(1) DEFAULT 'N';
ALTER TABLE moni_elastic_log ADD IS_ALERT VARCHAR(1) DEFAULT 'N';
ALTER TABLE moni_job_log ADD IS_ALERT VARCHAR(1) DEFAULT 'N';
ALTER TABLE moni_api_log ADD RESPONSE TEXT;

INSERT INTO sys_dict_type values(23, '推送模板','job_push_template','0','admin',sysdate(),'',NULL,'推送模板');
insert into sys_dict_data values(300, 1,  'job',     'descr_template_job',       'job_push_template',   '',   '',  'N', '0', 'admin', sysdate(), '', null, '`MonitorID\\({id}\\)`/ `{asid}`/`{priority}`/`{platform}`/`{env}`
*__JobName:__* `{en_name}`/`{zh_name}`
*__Result:__* `{result}`
_{descr}_');
insert into sys_dict_data values(301, 2,  'elastic',     'descr_template_elastic',       'job_push_template',   '',   '',  'N', '0', 'admin', sysdate(), '', null, '`MonitorID\\({id}\\)`/ `{asid}`/`{priority}`/`{platform}`/`{env}`
*__JobName:__* `{en_name}`/`{zh_name}`
*__Result:__* `{result}`
*__Export\\(LAST hit\\):__*
`{export}`
_{descr}_');
insert into sys_dict_data values(302, 3,  'api',     'descr_template_api',       'job_push_template',   '',   '',  'N', '0', 'admin', sysdate(), '', null, '`MonitorID\\({id}\\)`/ `{asid}`/`{priority}`/`{platform}`/`{env}`
*__JobName:__* `{en_name}`/`{zh_name}`
*__Result:__* `{result}`
_{descr}_');

insert into sys_dict_data values(303, 4,  'webhook',     'descr_template_webhook',       'job_push_template',   '',   '',  'N', '0', 'admin', sysdate(), '', null, '*_\\[webhook\\]_*__*{title}*__
*CreateTime:* `{time}\\({reporter}\\)`
*Descr:* `{descr}`
*Remark:* `{remark}`');

update moni_job set TELEGRAM_INFO = '{descr_template_job}';


update moni_elastic set TELEGRAM_INFO = '{descr_template_elastic}';


update moni_api set TELEGRAM_INFO = '{descr_template_api}';

