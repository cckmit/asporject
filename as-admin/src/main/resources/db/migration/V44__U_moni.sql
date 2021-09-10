update sys_dict_data set dict_value = 'asp_db_p1' where dict_type = 'telegram_notice_group' and dict_value = '1';
update sys_dict_data set dict_value = 'asp_db_p2' where dict_type = 'telegram_notice_group' and dict_value = '2';
update sys_dict_data set dict_value = 'asp_log_p1' where dict_type = 'telegram_notice_group' and dict_value = '3';
update sys_dict_data set dict_value = 'asp_log_p2' where dict_type = 'telegram_notice_group' and dict_value = '4';
update sys_dict_data set dict_value = 'asp_api' where dict_type = 'telegram_notice_group' and dict_value = '5';
update sys_dict_data set dict_value = 'as_test' where dict_type = 'telegram_notice_group' and dict_value = '6';

ALTER TABLE moni_job MODIFY TELEGRAM_CONFIG VARCHAR(100);
ALTER TABLE moni_elastic MODIFY TELEGRAM_CONFIG VARCHAR(100);
ALTER TABLE moni_api MODIFY TELEGRAM_CONFIG VARCHAR(100);

update moni_job set TELEGRAM_CONFIG = 'asp_db_p1' where TELEGRAM_CONFIG = '1';
update moni_job set TELEGRAM_CONFIG = 'asp_db_p2' where TELEGRAM_CONFIG = '2';
update moni_job set TELEGRAM_CONFIG = 'asp_log_p1' where TELEGRAM_CONFIG = '3';
update moni_job set TELEGRAM_CONFIG = 'asp_log_p2' where TELEGRAM_CONFIG = '4';
update moni_job set TELEGRAM_CONFIG = 'asp_api' where TELEGRAM_CONFIG = '5';
update moni_job set TELEGRAM_CONFIG = 'as_test' where TELEGRAM_CONFIG = '6';

update moni_elastic set TELEGRAM_CONFIG = 'asp_db_p1' where TELEGRAM_CONFIG = '1';
update moni_elastic set TELEGRAM_CONFIG = 'asp_db_p2' where TELEGRAM_CONFIG = '2';
update moni_elastic set TELEGRAM_CONFIG = 'asp_log_p1' where TELEGRAM_CONFIG = '3';
update moni_elastic set TELEGRAM_CONFIG = 'asp_log_p2' where TELEGRAM_CONFIG = '4';
update moni_elastic set TELEGRAM_CONFIG = 'asp_api' where TELEGRAM_CONFIG = '5';
update moni_elastic set TELEGRAM_CONFIG = 'as_test' where TELEGRAM_CONFIG = '6';

update moni_api set TELEGRAM_CONFIG = 'asp_db_p1' where TELEGRAM_CONFIG = '1';
update moni_api set TELEGRAM_CONFIG = 'asp_db_p2' where TELEGRAM_CONFIG = '2';
update moni_api set TELEGRAM_CONFIG = 'asp_log_p1' where TELEGRAM_CONFIG = '3';
update moni_api set TELEGRAM_CONFIG = 'asp_log_p2' where TELEGRAM_CONFIG = '4';
update moni_api set TELEGRAM_CONFIG = 'asp_api' where TELEGRAM_CONFIG = '5';
update moni_api set TELEGRAM_CONFIG = 'as_test' where TELEGRAM_CONFIG = '6';