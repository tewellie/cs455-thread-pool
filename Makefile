# Makefile for CDN

JFLAGS       = -g




default: 
	javac $(JFLAGS) cs455/scaling/client/Client.java
	javac $(JFLAGS) cs455/scaling/client/TransmissionHandler.java
	javac $(JFLAGS) cs455/scaling/pool/Task.java
	javac $(JFLAGS) cs455/scaling/pool/ThreadPoolManager.java
	javac $(JFLAGS) cs455/scaling/pool/Worker.java
	javac $(JFLAGS) cs455/scaling/server/MessageInfo.java
	javac $(JFLAGS) cs455/scaling/server/Server.java
	javac $(JFLAGS) cs455/scaling/test/PoolTest.java
	javac $(JFLAGS) cs455/scaling/util/ChangeRequest.java
	javac $(JFLAGS) cs455/scaling/util/RandomData.java
	javac $(JFLAGS) cs455/scaling/util/SHA1.java
	
all: 
	javac $(JFLAGS) cs455/scaling/client/Client.java
	javac $(JFLAGS) cs455/scaling/client/TransmissionHandler.java
	javac $(JFLAGS) cs455/scaling/pool/Task.java
	javac $(JFLAGS) cs455/scaling/pool/ThreadPoolManager.java
	javac $(JFLAGS) cs455/scaling/pool/Worker.java
	javac $(JFLAGS) cs455/scaling/server/MessageInfo.java
	javac $(JFLAGS) cs455/scaling/server/Server.java
	javac $(JFLAGS) cs455/scaling/test/PoolTest.java
	javac $(JFLAGS) cs455/scaling/util/ChangeRequest.java
	javac $(JFLAGS) cs455/scaling/util/RandomData.java
	javac $(JFLAGS) cs455/scaling/util/SHA1.java
	
clean: 
	rm -f *.class *~


