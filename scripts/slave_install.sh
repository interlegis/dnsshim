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


#
# Date:        2009-08-07
# Description: Installation file for configuring slave server to use
#              DNSSHIM software.
#

# Help
usage()
{
  echo "usage: $0 [-c namedConf] <-p pathToDns> <-k keyName>"
  echo "  [-a keyAlgorithm] <-s keySecret> <-S dnsshimServer> [-u user]"
  echo ""
  exit
}

# Make sure only root can run our script
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root"
   exit
fi

# Reading all inputs
while getopts c:p:k:a:s:S:u: opt; do
  case $opt in
    c)  namedConf="$OPTARG"
        ;;
    p)  baseDns="$OPTARG"
        ;;
    k)  keyName="$OPTARG"
        ;;
    a)  keyAlgorithm="$OPTARG"
        ;;
    s)  keySecret="$OPTARG"
        ;;
    S)  dnsshimServer="$OPTARG"
        ;;
    u)  user="$OPTARG"
        ;;
    *)  usage
        ;;
  esac
done

if [ "$baseDns" = "" ] || \
   [ "$keyName" = "" ] || \
   [ "$keySecret" = "" ] || \
   [ "$dnsshimServer" = "" ]; then
  usage
fi

# Check if the user inserted the last slash
last=`expr substr $baseDns ${#baseDns} 1`
if [ "$last" != "/" ]; then
  baseDns="${baseDns}/"
fi

if [ "$namedConf" = "" ]; then
  namedConf="${baseDns}named.conf"
fi

# Check if the named.conf file is in the given path
if [ ! -f "$namedConf" ]; then
  echo "named.conf is not located in the given path!"
  exit
fi

if [ "$keyAlgorithm" = "" ]; then
  keyAlgorithm="hmac-md5"
fi

if [ "$user" = "" ]; then
  user="dnsshim"
fi

# Creating user / group
adduser --no-create-home --system --group $user

# Fix path permissions
chown -R $user:$user $baseDns
chmod -R ug+rwx $baseDns
chmod -R o+rx $baseDns

# Create slave script
cat > "${baseDns}dnsshim_slave.sh" <<EOF
#!/bin/sh

for dir in \`awk '{ print \$13 }' named.zones | tr -d '";' | awk -F '/' '{ for (i = 1; i < NF; i++) { if (i > 1) { printf ( "/" ) } printf ( \$i ) } print ""}'\`
do
if [ ! -d \$dir ]
then
mkdir -p \$dir
fi
done

for zone in \`cat zones.removed\`
do
rm -f dnsshim/\$zone
done
EOF

# Give owner permissions to new user / group for the slave script
chown $user:$user "${baseDns}dnsshim_slave.sh"
chmod a+x "${baseDns}dnsshim_slave.sh"

# Create zones store file and give permissions to new user / group
touch "${baseDns}named.zones"
chown $user:$user "${baseDns}named.zones"

# Check if named.conf was already changed
grep $keyName $namedConf > /dev/null
if [ $? -eq 1 ]; then
  # Add references to the new zone in named.conf
  cat >> "$namedConf" <<EOF

include "${baseDns}named.zones";

key "$keyName" {
  algorithm $keyAlgorithm;
  secret "$keySecret";
};

server $dnsshimServer { keys "$keyName"; };

controls {
  inet * allow { localhost; } keys { $keyName; };
};

EOF
fi

# Restart service
killall named
named -u $user -c "${namedConf}"

# Update the property in DNSSHIM server
echo "Remember to set the property slave_path with the value '$baseDns'"\
  "in DNSSHIM server."
