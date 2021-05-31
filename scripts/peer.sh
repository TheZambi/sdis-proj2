#!/bin/bash

# Script for running a peer
# To be run in the root of the build tree
# No jar files used
# Assumes that Peer is the main class 
#  and that it belongs to the peer package
# Modify as appropriate, so that it can be run 
#  from the root of the compiled tree

# Check number input arguments
argc=$#

if (( argc < 2 ))
then
	echo "Usage: $0 <folder_to_run> <rmi_host> <address>:<port> [<address_to_connect>:<port_to_connect>]"
	exit 1
fi

# Assign input arguments to nicely named variables

id=$1
rmi_host=$2
op_address=$3
join_address=$4

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

cd test/"${id}" && java -classpath ../.. \
  -Djavax.net.ssl.keyStore=../../../../keys/server.keys \
  -Djavax.net.ssl.keyStorePassword=123456 \
  -Djavax.net.ssl.keyStore=../../../../keys/client.keys \
  -Djavax.net.ssl.keyStorePassword=123456 \
  -Djavax.net.ssl.trustStore=../../../../keys/truststore \
  -Djavax.net.ssl.trustStorePassword=123456 \
  g23.Peer ${rmi_host} ${op_address} ${join_address}

