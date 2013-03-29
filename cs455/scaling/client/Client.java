package cs455.scaling.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

import cs455.scaling.util.*;

/**
 * 
 * @author Theresa Wellington
 * March, 2012
 * 
 * The Clients: multiple clients (minimum of 100) in the system. A client provides the following functionality
 * 		(1) Connect and maintain an active connection to the server
 * 		(2) regularly send data packets to the server.
 * 			Payloads for these data parckts are around 8KB and hte values for these bytes are randomly generated.
 * 			The rate at which each connection will generate packets is R per-second; 
 * 				include a Thread.sleep(1000/R) in the clinet which ensusures you achieve targeted production rate.
 * 			Typical value of R is between 2-4
 * 		(3) The client should track haschodes of the data packets it has sent to the server
 * 			A server will acknowledge every packet it has received by sending the computed hash code back to the client.
 * 			Since these connections are TCP based and the links are FIFO, if there is a mismatch it will indicate either
 * 				a bug in your code or that data was corrupted en route to the server.
 * 
 * Interactions between components
 * 		Client is expected to send messages at rate specified during start-up. Client sends a byte[] to the server
 * 		Size of this array is 8KB and contents are randomly generated
 * 		Client generates a new byte array for every transmission and tracks the hash codes associated with transmitted data
 * 		Hashes generated with SHA-1 algorithm (cs44.scaling.util.SHA1.java)
 * 		Client maintains hash codes in a linked list
 * 		For every data packet that is published, client adds corresponding hash code to tail of linked list
 * 		Upon receiving data, server computes hash code and sends back to client
 * 		Client should receive acknoledgements in order that it was sent
 * 		When acknowledgment received, client checks hascode and matches with the one at the top of the linked list - should match since links are FIFO
 * 		
 *
 */

public class Client implements Runnable {
	//hostAddress/port to connect to
	private InetAddress hostAddress;
	private int port;

	// The selector to monitor
	private Selector selector;

	// The buffer to read data when available
	private ByteBuffer readBuffer = ByteBuffer.allocate(40);

	// A list of PendingChange instances
	private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
	
	// Maps a SocketChannel to a TransmissionHandler
	private Map<SocketChannel, TransmissionHandler> transmissionHandlers = Collections.synchronizedMap(new HashMap<SocketChannel, TransmissionHandler>());
	
	private int messageRate;
	private String hostName;
	private Queue<String> sentData = new LinkedList<String>();

	private int messageCount;
	
	/**
	 * Creates a new client
	 * @param hostAddress
	 * @param port
	 * @param messagerate
	 * @throws IOException
	 */
	public Client(InetAddress hostAddress, int port, int messagerate) throws IOException {
		this.hostAddress = hostAddress;
		this.port = port;
		this.selector = this.initSelector();
		
		hostName = "";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		messageCount = 0;
		this.messageRate = messagerate;
	}

	/**
	 * Prints information about sent/received messages
	 * @param messageNumber
	 * @param response - String received from server
	 */
	private void printMessageInfo(int messageNumber, String response){
		String sentHash = sentData.remove();
		System.out.println();
		System.out.println("[Msg-" + messageCount+ "] Sent: " + sentHash);
		System.out.println("[Msg-" + messageCount+ "] Received: " + response);
		if(sentHash.equals(response)){
			System.out.println("[ClientStatus] Message " + messageNumber + " transmitted correctly");
		}
		else System.out.println("[ClientStatus] Message " + messageNumber + " transmitted incorrectly");
		
	}
	
	/**
	 * Sends data to the server at messageRate
	 */
	private void sendData(){
		TransmissionHandler handler = new TransmissionHandler();
		//generate random data
		messageCount++;
		byte[] data = RandomData.generateData(8);
		String hashedData = SHA1.SHA1FromBytes(data);
		//append to tail of linkedlist
		sentData.offer(hashedData);
		
		

		
		try {
			send(data, handler);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String response = handler.waitForResponse();
		
		printMessageInfo(messageCount, response);
		response = "";
		
		
		//sleep till it's time to send more data
		try {
			Thread.sleep(1000/messageRate);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendData();
	}
	
	
	/**
	 * Send data to server
	 * @param data - random data to send
	 * @param handler - ResponseHandler (waits for server response)
	 * @throws IOException
	 */
	public void send(byte[] data, TransmissionHandler handler) throws IOException {
		// Start a new connection
		SocketChannel socket = this.initiateConnection();

		// Register the response handler
		this.transmissionHandlers.put(socket, handler);
		
		// queue the data to be written
		synchronized (this.pendingData) {
			List<ByteBuffer> queue = this.pendingData.get(socket);
			if (queue == null) {
				queue = new ArrayList<ByteBuffer>();
				this.pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}

		// wake up selecting thread so it can make required changes
		this.selector.wakeup();
	}

	/**
	 * Receive data
	 * @param key
	 * @throws IOException
	 */
	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Clear out read buffer so it's ready for new data
		this.readBuffer.clear();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = socketChannel.read(this.readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			return;
		}

		if (numRead == -1) {
			// Remote server entity shut the socket down cleanly. Do the
			// same from client end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}

		
		handleResponse(socketChannel, readBuffer.array(), numRead);
	}

	private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
		// Make a correctly sized copy of the data before handing it
		// to the client
		byte[] rspData = new byte[numRead];
		System.arraycopy(data, 0, rspData, 0, numRead);
		
		// Look up the handler for this channel
		TransmissionHandler handler = (TransmissionHandler) transmissionHandlers.get(socketChannel);
		
		// And pass the response to it
		if (handler.handleResponse(rspData)) {
			// The handler has seen enough, close the connection
			socketChannel.close();
			socketChannel.keyFor(selector).cancel();
		}
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (pendingData) {
			List<ByteBuffer> queue =  pendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// No more data to write so switch back to reading
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void finishConnection(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
	
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			// Cancel the channel's registration with selector
			System.out.println(e);
			key.cancel();
			return;
		}
	
		// Register an interest in writing on this channel
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private SocketChannel initiateConnection() throws IOException {
		// Create a non-blocking socket channel
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
	
		// start connection
		socketChannel.connect(new InetSocketAddress(hostAddress, port));
	
		// Queue a channel registration
		synchronized(this.pendingChanges) {
			pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
		}
		
		return socketChannel;
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		return SelectorProvider.provider().openSelector();
	}

	
	/**
	 * processes any pending messages and waits for events
	 */
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
							break;
						case ChangeRequest.REGISTER:
							change.socket.register(selector, change.ops);
							break;
						}
					}
					pendingChanges.clear();
				}

				// Wait for an event one of the registered channels
				selector.select();

				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what type of event is available and deal with it
					if (key.isConnectable()) {
						finishConnection(key);
					} else if (key.isReadable()) {
						read(key);
					} else if (key.isWritable()) {
						write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	public static void main(String[] args) {
		
		if(args.length!=3){ 
			System.out.println("Usage: java cs455.scaling.client.Client server-host server-port message-rate");
			return;
		}
		
		
		
			Client client = null;
			
			try {
				client = new Client(InetAddress.getByName(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Thread thread = new Thread(client);
			//start thread
			thread.start();
			//start sending Data
			client.sendData();

	}
}