package cs455.scaling.client;

/**
 * Class to deal with responses from server
 * @author Owner Theresa Wellington
 *
 */
public class TransmissionHandler {
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized String waitForResponse() {
		while(rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
		
		return new String(rsp);
	}
}