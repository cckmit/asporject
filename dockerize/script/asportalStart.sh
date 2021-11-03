#!/bin/bash

image=`docker images | grep -v 'IMAGE ID' | grep 'asportal' | head -n 1 | awk '{print$3}'`
containerId=`docker ps | grep 'as-portal' | awk '{print $1}'`
if [ -n "${containerId}" ];then
        sh /usr/local/dockerize/script/asportalStop.sh
else
        echo 'no running process'
fi

docker run -itd \
--network=host \
--name as-portal \
--rm \
-v /var/log/as_portal/excelfile:/usr/local/backend/uploadPath/export \
-v /var/log/as_portal/telegramPhoto:/usr/local/backend/uploadPath/photo \
-v /var/log/as_portal/avatar:/usr/local/backend/uploadPath/avatar \
-v /var/log/as_portal/log:/usr/local/backend/as_logs \
${image}