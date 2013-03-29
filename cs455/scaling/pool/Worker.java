package cs455.scaling.pool;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import cs455.scaling.server.MessageInfo;
import cs455.scaling.util.SHA1;


/**
 * one of the threads in the threadpool - processes tasks
 * @author Owner Theresa Wellington
 *
 */
public class Worker implements Runnable {
	
	private ThreadPoolManager manager;
	private int id;
	private List<Task> queue = new LinkedList<Task>();
	
	/**
	 * Creates a new Worker object
	 * @param poolManager - the ThreadPoolManager the Worker belongs to
	 * @param idNumber
	 */
	public Worker(ThreadPoolManager poolManager, int idNumber){
		manager = poolManager;
		id = idNumber;
	}
	

	@Override
	public void run() {
		Task task;
	    
	    while(true) {
	      // Wait for data to become available
	      synchronized(queue) {
	        while(queue.isEmpty()) {
	          try {
	            queue.wait();
	          } catch (InterruptedException e) {
	          }
	        }
	        task = queue.remove(0);
	      }
	      
	      
	      // Return to sender
	      manager.getServer().send(task.getSocketChannel(), task.getArray());
	      manager.workerFinished(this);
	    }
	  }
		
	
	
	public void stop(){
	
		//need to actually stop threads?
		
	}
	
	/**
	 * Create a hash for the task's data, then create a new task to send the hash value back to the client
	 * @param task - the assigned task
	 */
	public void assign(Task task){
		String hashed = SHA1.SHA1FromBytes(task.getArray());

		byte [] hashedBytes = new byte[hashed.getBytes().length];
		addMessageInfo(task, hashed);
	    System.arraycopy(hashed.getBytes(), 0, hashedBytes, 0, hashed.getBytes().length);
	    synchronized(queue) {
	      queue.add(new Task(task.getSocketChannel(), hashedBytes, hashedBytes.length));
	      queue.notify();
	    }
	}
	
	/**
	 * Add messageInfo to the server's queue (for use in printing stats)
	 * @param task
	 * @param hash
	 */
	private void addMessageInfo(Task task, String hash){
		String client = task.getSocketChannel().socket().getInetAddress().getHostName();
		MessageInfo message = new MessageInfo(client, hash, id);
		manager.getServer().addHandledMessage(message);
	}
	

}
