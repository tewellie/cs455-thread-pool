This program was written for an upper level computer science course in March 2012. The main two classes are Server.java and Client.java. There is exactly one server node in the system that accepts incoming network connects, incoming traffic from these connects and replies to clients by sending back a hash code for each message. It relies on a thread pool to perform these three functions. Clients expected to send messages to the server containing random data at a regular interval.

Running from the command line:
java cs455.scaling.server.Server portnum thread-pool-size
java cs455.scaling.client.Client server-host server-port message-rate