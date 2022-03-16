FROM ubuntu:20.04
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y locales
RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    dpkg-reconfigure --frontend=noninteractive locales && \
    update-locale LANG=en_US.UTF-8
ENV LANG en_US.UTF-8
RUN apt-get install -y openjdk-11-jdk
RUN apt-get install -y maven
RUN apt-get install -y openjfx
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/" >> /root/.bashrc
