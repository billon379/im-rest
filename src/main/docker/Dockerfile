FROM registry.cn-beijing.aliyuncs.com/billon379/jdk-alpine:8
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=""
VOLUME /tmp
ADD im-rest.jar im-rest.jar
ENTRYPOINT exec java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /im-rest.jar