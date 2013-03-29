Theresa Wellington
CS 455 - ThreadPool

Classes:
	cs455/scaling/client/Client.java - send random data to server at set interval
	cs455/scaling/client/TransmissionHandler.java - deals with responses from server
	cs455/scaling/pool/Task.java - stores information for sending a message over a socketchannel
	cs455/scaling/pool/ThreadPoolManager.java - creates a pool of worker threads and manages the assigment of tasks to these threads
	cs455/scaling/pool/Worker.java - one of the threads in the pool, computes hash and sends back to clients
	cs455/scaling/server/MessageInfo.java - stores information for use while server prints received messages
	cs455/scaling/server/Server.java - accepts in coming connections, uses threadpool to manage incoming traffic and send back hashes
	cs455/scaling/test/PoolTest.java - tests the ThreadPoolManager class
	cs455/scaling/util/ChangeRequest.java - stores information about changes to be made on a specific channel
	cs455/scaling/util/RandomData.java - generates a random byte array
	cs455/scaling/util/SHA1.java - creates a hash of the byte[] passed in

Other Information:
	Used NIO tutorial (found here http://rox-xmlrpc.sourceforge.net/niotut/) as a guide for creating and using Selectors and Socket Channels
