package cs455.scaling.test;

import cs455.scaling.pool.*;

/**
 * 
 * @author Owner Theresa Wellington
 * March 2012
 *
 * A simple class to test classes in the cs.scaling.pool package
 *
 */
public class PoolTest {
	
	private int poolSize;
	private ThreadPoolManager manager;
	
	/**
	 * Creates a new PoolTest
	 * @param threadPoolSize - the size of the thread pool
	 */
	public PoolTest(int threadPoolSize){
		poolSize = threadPoolSize;
		manager = new ThreadPoolManager(poolSize, null);
	}
	
	public static void main(String args[]){
		if(args.length!=1){ 
			System.out.println("Usage: java cs455.scaling.test.PoolTest thread-pool-size");
			return;
		}
		
		
		
		PoolTest poolTest = new PoolTest(Integer.parseInt(args[0]));
		
		
		System.out.println("Starting Pool Test");
		poolTest.manager.start();
		Task testTask = new Task("A");
		Task testTask2 = new Task("B");
		Task testTask3 = new Task("C");
		Task testTask4 = new Task("D");
		Task testTask5 = new Task("E");
		Task testTask6 = new Task("F");
		Task testTask7 = new Task("G");
		
		poolTest.manager.addTask(testTask);
		poolTest.manager.addTask(testTask2);
		poolTest.manager.addTask(testTask3);
		poolTest.manager.addTask(testTask4);
		poolTest.manager.addTask(testTask5);
		poolTest.manager.addTask(testTask6);
		poolTest.manager.addTask(testTask7);
		

		
		System.out.println("Stopping Pool Test");
		poolTest.manager.stop();
		
		
		
	}


}
