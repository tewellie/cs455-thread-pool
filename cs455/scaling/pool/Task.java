package cs455.scaling.pool;

import java.nio.channels.SocketChannel;

import cs455.scaling.server.Server;

/**
 * Stores information for sending a message over a socketchannel
 * @author Owner Theresa Wellington
 *
 */
public class Task {
	
	private String id;
	private SocketChannel socketChannel;
	private byte[] array;
	int arraySize;
	
	public Task(SocketChannel socketChannel, byte[] array, int arraySize){
		this.socketChannel = socketChannel;
		this.array = array;
		this.arraySize = arraySize;
	}
	
	public Task(String identifier){
		id = identifier;
	}
	
	public String getID(){
		return id;
	}
	
	public SocketChannel getSocketChannel(){
		return socketChannel;
	}
	
	public byte[] getArray(){
		return array;
	}
	
	public int getArraySize(){
		return arraySize;
	}

	
}
