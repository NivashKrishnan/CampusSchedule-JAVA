FROM node:22-bookworm-slim AS files
WORKDIR /opt/blob-bridge
COPY deployment/blob-bridge/package*.json ./
RUN npm ci --ignore-scripts --omit=dev
COPY deployment/blob-bridge/server.mjs ./

FROM tomcat:9.0-jdk8-temurin AS build
WORKDIR /build
COPY src ./src
COPY WebContent ./WebContent
COPY deployment/CreateAdmin.java ./CreateAdmin.java
RUN mkdir -p WebContent/WEB-INF/classes && javac -encoding UTF-8 -source 8 -target 8 -cp /usr/local/tomcat/lib/servlet-api.jar -d WebContent/WEB-INF/classes src/app/*.java
# Public deployment requires HTTPS, including secure session cookies.
RUN sed -i 's@<http-only>true</http-only>@<http-only>true</http-only><secure>true</secure>@' WebContent/WEB-INF/web.xml
RUN jar cf /build/Timetable.war -C WebContent .
RUN mkdir -p /build/bootstrap && javac -cp WebContent/WEB-INF/classes -d /build/bootstrap CreateAdmin.java

FROM tomcat:9.0-jdk8-temurin
COPY --from=files /usr/local/bin/node /usr/local/bin/node
COPY --from=files /opt/blob-bridge /opt/blob-bridge
COPY --from=build /build/Timetable.war /usr/local/tomcat/webapps/Timetable.war
COPY --from=build /build/bootstrap /opt/timetable-bootstrap
COPY deployment/server.xml /usr/local/tomcat/conf/server.xml
COPY deployment/root/ /usr/local/tomcat/webapps/ROOT/
COPY deployment/start-cloud.sh /opt/start-cloud.sh
ENV TIMETABLE_PUBLIC_DEPLOYMENT=true
ENV JAVA_OPTS="-Xms64m -Xmx384m -Dfile.encoding=UTF-8"
EXPOSE 8080
CMD ["sh", "/opt/start-cloud.sh"]
