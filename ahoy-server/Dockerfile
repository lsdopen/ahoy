FROM openjdk:11-jdk-slim

# install required packages
RUN apt-get -yq update
RUN apt-get -yqq install wget
RUN apt-get -yqq install curl
# kubectl
RUN apt-get install -yqq apt-transport-https gnupg2
RUN curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -
RUN echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | tee -a /etc/apt/sources.list.d/kubernetes.list
RUN apt-get -yq update
RUN apt-get -yqq install kubectl

# kubeseal
RUN wget -nv https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.15.0/kubeseal-linux-amd64 -O kubeseal --no-check-certificate
RUN install -m 755 kubeseal /usr/local/bin/kubeseal

WORKDIR /tmp

# add app jar
ARG JAR_FILE
ADD ${JAR_FILE} app.jar

# setup environment
ENV SPRING_PROFILES=prod,keycloak
EXPOSE 8080

ENTRYPOINT ["sh","-c","java -Dspring.profiles.active=$SPRING_PROFILES -jar app.jar", "--spring.config.location=/tmp/config/"]
