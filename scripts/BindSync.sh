#!/bin/sh
# Copyright (C) 2009 Registro.br. All rights reserved. 
# 
# Redistribution and use in source and binary forms, with or without 
# modification, are permitted provided that the following conditions are 
# met:
# 1. Redistribution of source code must retain the above copyright 
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
# 
# THIS SOFTWARE IS PROVIDED BY REGISTRO.BR ``AS IS'' AND ANY EXPRESS OR
# IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIE OF FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
# EVENT SHALL REGISTRO.BR BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
# OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
# TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
# USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
# DAMAGE.


added=0
removed=0
ERRCOUNTER=0
rndcExists=0

rndcPath="/usr/bin/rndc"
rndcPort="953"

if [ -n "$1" -a -n "$2" -a -n "$3" -a -n "$4" ]
	then
	addedZones=$1
	removedZones=$2
	serverIp=$3
	timestamp=$4

	if [ -x "$rndcPath" ]
		then
		rndcExists=1
	fi

	filename="${addedZones}${timestamp}"
    # Make sure file exists and is not empty
    if [ -s $filename ]
    	then
    	while read LINE
    	do
    		if [ $rndcExists -eq 1 ]
    			then
    			err=`eval $rndcPath -s $serverIp -p $rndcPort addzone $LINE 2>&1`
    			if [ $? -eq 0 ]
    				then
    				added=`expr $added + 1`
    			else
    				echo $err | grep "already exists"
    				if [ $? -eq 1 ]
    					then
    					zone=`echo $LINE | awk '{ print $1 }'`
    					echo $zone >> ${addedZones}.err
    				fi
    				ERRCOUNTER=`expr $ERRCOUNTER + 1`
    			fi
    		else
    			zone=`echo $LINE | awk '{ print $1 }'`
    			echo $zone >> ${addedZones}.err
    			ERRCOUNTER=`expr $ERRCOUNTER + 1`
    		fi
    	done < $filename
    fi

    filename="${removedZones}${timestamp}"
    if [ -s $filename ]
    	then
    	while read LINE
    	do
    		if [ $rndcExists -eq 1 ]
    			then
    			err=`eval $rndcPath -s $serverIp -p $rndcPort delzone $LINE 2>&1`
    			if [ $? -eq 0 ]
    				then
    				removed=`expr $removed + 1`
    			else
    				echo $err | grep "not found"
    				if [ $? -eq 1 ]
    					then
    					echo $LINE >> ${removedZones}.err
    				fi
    				ERRCOUNTER=`expr $ERRCOUNTER + 1`

    			fi
    		else
    			echo $LINE >> ${removedZones}.err
    			ERRCOUNTER=`expr $ERRCOUNTER + 1`
    		fi
    	done < $filename
    fi

    echo "SlaveSync $serverIp finished"
    echo "Added: $added"
    echo "Removed: $removed"
    echo "Errors: $ERRCOUNTER"
fi
