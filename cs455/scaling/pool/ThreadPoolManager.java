package cs455.scaling.pool;


import cs455.scaling.util.ChangeRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

import cs455.scaling.server.Server;


/**
 * 
 * @author Theresa Wellington
 * March, 2012
 *
 * 2. Interaction between the components
 * 		Threads in thread pool should be created just once
 * 		Thread pool needs methods that allow:
 * 			(1) spare worker thread to be retieved
 * 			(2) worker thread to return itself to the pool after it has finished its task
 * 		Thread pool manager also maintains a list of work that it needs to perform
 * 		Maintains these work units in a FIFO queue implemented using linked list
 * 		Work units are added to the tail of this work queue
 * 		When spare workers are available, they are assigned work form the top of the queue
 *
 */
public class ThreadPoolManager {
	
	private Worker[] workers;
	private Thread[] workerThreads;
	
	private Queue<Task> taskQueue = new LinkedList<Task>();
	private Queue<Worker> workerQueue = new LinkedList<Worker>();
	private Server server;
	

	
	/**
	 * Create a new ThreadPoolManager
	 * @param poolSize number of threads in the pool
	 */
	public ThreadPoolManager(int poolSize, Server server){
		workers = new Worker[poolSize];
		workerThreads = new Thread[poolSize];
		
		for(int i=0; i<poolSize; i++){
			Worker worker = new Worker(this, i);
			workers[i] = worker;
			workerThreads[i] = new Thread(worker);
		}
		this.server = server;
	
	}
	
	
	
	/**
	 * Begin accepting Tasks and executing them on the thread pool. This method must be run before any Tasks will run
	 */
	public void start(){
		for(Thread thread:workerThreads){
			thread.start();
		}
		
		for(Worker worker:workers){
			workerQueue.offer(worker);
		}
		
	}
	
	
	/**
	 * Shut down the thread pool
	 */
	public void stop(){
		for(Worker worker:workers){
			worker.stop();
		}
	}
	
	/**
	 * Submits a Task for execution in the thread pool. Tasks are executed in FIFO order as worker threads become available
	 * @param task The Task to be executed by the thread pool
	 */
	public synchronized void addTask(Task task){
		taskQueue.offer(task);
		
		if(taskQueue.size()>0){
			assignTasks();
		}
	}
	
	private synchronized void assignTasks(){
		// need to implement this method (does this actually need to be sychronized?)
		// if there is an available worker
		if(workerQueue.size() > 0 ){
			// assign the first worker in the workerQueue the first task in the taskQueue
			// remove the worker from the workerQueue and the task from the taskQueue
			
			//get first worker from queue (and remove it)
			Worker currWorker = workerQueue.remove();
			Task currTask = taskQueue.remove();
			currWorker.assign(currTask);
			
		}
		 
	}
	
	/**
	 * Called by worker threads when their Task has completed
	 * @param worker
	 */
	public synchronized void workerFinished(Worker worker){
		workerQueue.offer(worker);
		
		if(taskQueue.size() > 0){
			assignTasks();
		}
	}
	
	public Server getServer(){
		return server;
	}
	
	
}
