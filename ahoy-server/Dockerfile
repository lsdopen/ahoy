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
RUN wget -nv https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.12.3/kubeseal-linux-amd64 -O kubeseal
RUN install -m 755 kubeseal /usr/local/bin/kubeseal

# create ahoy user and group
RUN groupadd -g 1000 ahoy && useradd -u 1000 -g ahoy -s /bin/sh ahoy
RUN mkdir /home/ahoy && chown -R ahoy:ahoy /home/ahoy
WORKDIR /home/ahoy
USER ahoy:ahoy

# add app jar
ARG JAR_FILE
ADD ${JAR_FILE} app.jar

# setup environment
ENV SPRING_PROFILES=prod
EXPOSE 8443

ENTRYPOINT ["sh","-c","java -Dspring.profiles.active=$SPRING_PROFILES -jar app.jar", "--spring.config.location=/home/ahoy/config/"]
