#!Dockerfile
FROM sjatgutzmann/docker.centos.javadev8

ENV JENKINS_HOME /var/lib/jenkins/
#VOLUME ${JENKINS_HOME}
RUN wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo \
	&& rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key \
	&& yum -y install initscripts git curl gpg jenkins

ENV JENKINS_SLAVE_AGENT_PORT 50000

ENV user=jenkins

COPY init.groovy /usr/share/jenkins/ref/init.groovy.d/tcp-slave-agent-port.groovy


ENV JENKINS_UC https://updates.jenkins.io

# for main web interface:
EXPOSE 8080

# will be used by attached slave agents:
EXPOSE 50000

ENV COPY_REFERENCE_FILE_LOG $JENKINS_HOME/copy_reference_file.log

RUN mkdir -m 700 ${JENKINS_HOME}/.ssh \
	&& mkdir -m 755 ${JENKINS_HOME}/.m2 \
	&& chown ${user}:${user} ${JENKINS_HOME}/.ssh \
	&& chown ${user}:${user} ${JENKINS_HOME}/.m2 \
	&& chown ${user}:${user} ${JENKINS_HOME} \
	&& touch ${JENKINS_HOME}/.gitconfig \
	&& chown ${user}:${user} ${JENKINS_HOME}/.gitconfig \
	&& chmod 666 ${JENKINS_HOME}/.gitconfig
COPY sshkeys.sh /sshkeys.sh
COPY mvn/settings.xml ${JENKINS_HOME}/.m2/settings.xml
COPY run.sh /run.sh
RUN chown ${user}:${user} ${JENKINS_HOME}/.m2/settings.xml
USER ${user}
RUN ls -la ${JENKINS_HOME}
#accept unsign ssl certificates with git, default user
RUN git config --global http.sslVerify false \
	&& git config --global user.email "jenkins@localhost" \
	&& git config --global user.name "jenkins"
# attention: ssh key without passphrase
RUN /sshkeys.sh
USER root

#ENTRYPOINT /bin/bash
#ENTRYPOINT /etc/init.d/jenkins start && sleep 5 && tail -f /var/log/jenkins/jenkins.log
ENTRYPOINT ["/run.sh"]

# from a derived Dockerfile, can use `RUN plugins.sh active.txt` to setup /usr/share/jenkins/ref/plugins from a support bundle
COPY plugins.sh /usr/local/bin/plugins.sh
COPY install-plugins.sh /usr/local/bin/install-plugins.sh
