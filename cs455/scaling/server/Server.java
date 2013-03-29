package cs455.scaling.server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.util.*;

import cs455.scaling.pool.*;
import cs455.scaling.util.*;

/**
 * 
 * @author Theresa Wellington
 * March, 2012
 * 
 * Server Node: There is exactly one server node in the system. It provides the following functionality
 * 		A. accepts incoming network connections from the clients
 * 		B. accepts incoming traffic from these connections
 * 		C. replies to clients by sending back a hash code for each message received
 * 		D. performs functions A, B, and C by relying on a thread pool
 * 
 * Interactions between components
 * 		Upon receiving data, server computes hash code and sends back to client
 * 		Server relies on thread pool to perform all tasks
 *
 */

public class Server implements Runnable {
	
	private int port;
	private int poolSize;
	private String hostName;
	private InetAddress hostAddress;
	private ThreadPoolManager threadPool;
	private int packetCount;
	private Long startTime;
	private int clientCount;
	private Queue<MessageInfo> handledMessages = new LinkedList<MessageInfo>();
	

	// The channel on which we'll accept connections
	private ServerSocketChannel serverChannel;

	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(8);

	// A list of PendingChange instances
	private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
	
	
	//instance variables for printing every 60 seconds
	private long delay = 60*1000; // delay in ms : 60 * 1000 ms = 60 sec.
	private LoopTask task = new LoopTask();
    private Timer timer = new Timer("printServerStats");
	
	
	public Server(int portNumber, int threadPoolSize){
		port = portNumber;
		poolSize = threadPoolSize;
		threadPool = new ThreadPoolManager(poolSize, this);
		hostName = "";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		hostAddress = null;
		try {
			hostAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		packetCount = 0;
		startTime = System.currentTimeMillis();
		clientCount = 0;
		try {
			this.selector = this.initSelector();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		threadPool.start();
	}
	
	public InetAddress getHostAddress(){
		return hostAddress;
	}
	
	public int getPort(){
		return port;
	}
	
	public void addHandledMessage(MessageInfo message){
		handledMessages.offer(message);
	}

	
	
	//methods for server output (every 60 seconds)
    public void start() {
	    timer.cancel();
	    timer = new Timer("printServerStats");
	    Date executionDate = new Date(); // no params = now
	    timer.scheduleAtFixedRate(task, executionDate, delay);
    }

    private class LoopTask extends TimerTask {
	    public void run() {
	    	long curTime = System.currentTimeMillis();
	    	long elapsedSeconds = (curTime - startTime)/1000;
	    	String upTimeString = "";
	    	String minutes = "";
	    	String hours = Long.toString(elapsedSeconds/(3600));
	    	long tempForSeconds = elapsedSeconds%3600;
	    	
	    	
	    	if((tempForSeconds/(60))<10){
	    		minutes = "0";
	    		minutes += Long.toString(tempForSeconds/(60));	
	    	}
	    	
	    	else{
	    		minutes = Long.toString(tempForSeconds/(60));
	    	}
	    	
	    	
	    	upTimeString += hours + ":";
	    	upTimeString += minutes;
	    	
	    	
	        System.out.println("Server at " + hostName + " running");
	        System.out.println("Thread pool size: " + poolSize);
	        if(elapsedSeconds!=0){
		        System.out.println("Total Clients connected: " + clientCount);
		        System.out.println("Packets/Second: " + packetCount/elapsedSeconds);
		        System.out.println("Server Uptime: " + upTimeString);
		        
		        while(!handledMessages.isEmpty()){
		        	System.out.println();
		        	MessageInfo temp = handledMessages.remove();
		        	System.out.println("[ClientMessage-" + temp.clientName +
		        			"] Hash: " + temp.hash);
		        	System.out.println("[ServerStatus] Message from Client at " +
		        			temp.clientName + " was handled by thread-" + temp.workerID);
		        }
		        
	        }
	    }
    }
    
    /**
	 * initialize socketSelector
	 * @return
	 * @throws IOException
	 */
	private Selector initSelector() throws IOException {
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
		serverChannel.socket().bind(isa);

		// Register the server socket channel, indicating an interest in 
		// accepting new connections
		serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

		return socketSelector;
	}
	
	private void write(SelectionKey key) throws IOException {
		
		SocketChannel socketChannel = (SocketChannel) key.channel();

		
		
		synchronized (pendingData) {
			List queue = (List) pendingData.get(socketChannel);

			// Write until there's no more data
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					// or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// done writing data, switch back to reading
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}
	
	private void accept(SelectionKey key) throws IOException {
		//increment client Count
		clientCount++;
		// For an accept to be pending the channel must be a server socket channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socket = socketChannel.socket();
		socketChannel.configureBlocking(false);

		// Register the new SocketChannel with Selector
		//will be notified when there's data to be read
		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	private void read(SelectionKey key) throws IOException {
		
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Clear out read buffer so it's ready for new data
		readBuffer.clear();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = socketChannel.read(readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from server end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}

		// Hand the data off to threadPool
		Task task = new Task(socketChannel, readBuffer.array(), numRead);
		threadPool.addTask(task);
		packetCount++;
	}

	public void send(SocketChannel socket, byte[] data) {
		synchronized (pendingChanges) {
			// change interest ops set
			pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (pendingData) {
				List<ByteBuffer> queue = this.pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList<ByteBuffer>();
					pendingData.put(socket, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}
		
		//  wake up selecting thread so it can make required changes
		selector.wakeup();
	}
	
	public void run() {
		while (true) {
			try {
				// Process any pending changes
				synchronized (pendingChanges) {
					Iterator<ChangeRequest> changes = pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(selector);
							key.interestOps(change.ops);
						}
					}
					this.pendingChanges.clear();
				}

				// Wait for an event one of the registered channels
				this.selector.select();

				// Iterate over the set of keys for which events are available
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					if (key.isAcceptable()) {
						this.accept(key);
					} else if (key.isReadable()) {
						this.read(key);
					} else if (key.isWritable()) {
						this.write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	//java cs455.scaling.server.Server portnum thread-pool-size
		public static void main(String args[]){
			if(args.length!=2){ 
				System.out.println("Usage: java cs455.scaling.server.Server portnum thread-pool-size");
				return;
			}
			
			Server server = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
			//start run method
			new Thread(server).start();
			//start start method
			server.start();
			
		}

	

}
