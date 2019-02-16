FROM java:8-alpine
MAINTAINER Ed Sweeney <ed@onextent.com>

# App
RUN mkdir -p /app
COPY target/scala-2.12/*.jar /app/
WORKDIR /app
#CMD java -jar ./AkkaEventhubsExample.jar
# override CMD from your run command, or k8s yaml, or marathon json, etc...

# YourKit profiling
RUN apk add --no-cache libc6-compat wget ca-certificates
RUN wget -nv https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2019.1-docker.zip -P /tmp/ && \
  unzip /tmp/YourKit-JavaProfiler-2019.1-docker.zip -d /usr/local && \
  rm /tmp/YourKit-JavaProfiler-2019.1-docker.zip
EXPOSE 10001
CMD java -agentpath:/usr/local/YourKit-JavaProfiler-2019.1/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all -jar ./AkkaEventhubsExample.jar
