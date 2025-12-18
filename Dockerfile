FROM tomcat:10.1-jdk17

RUN rm -rf /usr/local/tomcat/webapps/*

# The WAR is built as ROOT.war to ensure the context path is "/"
COPY target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080

CMD ["catalina.sh", "run"]