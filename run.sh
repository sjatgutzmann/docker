#!/bin/bash
# Script to run the docker container in diffenent ways
# using this as ENTRYPOINT
# Default is to start the server 

# show all JENKINS  ENVs
set | grep JENKINS
#set -x
start_jenkins() {
	echo "try starting jenkins"
	/etc/init.d/jenkins start \
	&& sleep 5 \
	&& echo "jenkins is ready" 
	#&& trap "/etc/init.d/jenkins stop" EXIT
}



ARG1=$1;


if [ -z ${ARG1} ]; then
	echo -n "setting default start arg to "
	ARG1="logtail"		
	echo $ARG1
fi

echo "starting this container with ${ARG1}"

case "$ARG1" in
	"bash")
		start_jenkins \
		&& echo "entering bash mode" \
		&& /bin/bash
	;;
	"logtail")
        	start_jenkins \
		&& tail -f ${JENKINS_LOG_PATH:-/var/log/jenkins}/${JENKINS_LOG_FILE:-jenkins.log} 
		/etc/init.d/jenkins stop
	;;
esac
