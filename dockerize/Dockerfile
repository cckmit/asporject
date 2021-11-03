FROM adoptopenjdk/openjdk15
RUN mkdir -p /usr/local/backend/uploadPath/export
RUN mkdir -p /usr/local/backend/uploadPath/photo
RUN mkdir -p /usr/local/backend/uploadPath/avatar
RUN mkdir -p /usr/local/backend/as_logs
RUN mkdir -p /usr/share/fonts/
COPY ./simhei.ttf /usr/share/fonts/simhei.ttf
ENV LANG C.UTF-8
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/ /etc/localtime && echo  > /etc/timezone
WORKDIR /app
COPY ./build/libs/*.jar asportal.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/asportal.jar"]