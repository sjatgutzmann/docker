#!/bin/bash
set +x
if [ ! -d $HOME/.ssh ] ;
then 
	mkdir $HOME/.ssh
fi
# attention: ssh key without passphrase
echo "create key in ${HOME}/.ssh/"
cd $HOME/.ssh \
        && cat /dev/zero | ssh-keygen -q -N "" \
        && ssh-agent \
	&& sleep 5 \
        && ssh-add 
cat $HOME/.ssh/id_rsa.pub
# host key
# ssh -oStrictHostKeyChecking=no jenkins@git -p 29418
set -x
