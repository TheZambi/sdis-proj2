

# SDIS Project

SDIS Project for group T5G23.

Group members:

1. Ricardo Fontão (up201806317@edu.fe.up.pt)
2. Davide Castro (up201806512@edu.fe.up.pt)
3. Henrique Ribeiro (up201806529@edu.fe.up.pt)
4. Diogo Rosário (up201806582@edu.fe.up.pt)


# How to run

### Compiling the code

This script should be run in the g23 directory:
./scripts/compile.sh

It should be run anytime a change has been made to the source code.

All the following scripts should be run in the src/build directory:

### Setup folder structure
To setup the folder structure of a peer, run the following scripts:

../../scripts/setup.sh <peer_id>

These scripts will create the folder structure of a peer of id peer_id (this must be an integer).
This must be run before executing a peer, or the program will not run correctly.

## Start the RMI
start rmiregistry or just rmiregistry should be run in the src/build folder

### Execute a peer

../../scripts/peer.sh <peer_id> <peer_address>:<peer_port> [peer_to_connect_address]:[peer_to_connect_port]

[peer_to_connect_address] and [peer_to_connect_port] are optional. In case they aren't present it is considered that the peer will start the chord system.

For example: 
../../scripts/peer.sh 1 localhost:8000 
../../scripts/peer.sh 2 localhost:8001 localhost:8000 
../../scripts/peer.sh 3 localhost:8002 localhost:8000 

In this case peer 2 will join the chord using peer 1 as entry point. Peer 3 will join the chord using peer 1 as an entry point also.


### Testing the service

../../scripts/test.sh <peer_hash> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<optnd_2]]

peer_hash will be printed by the peer when starting.

For example:
../../scripts/test.sh 77772382924 STATE