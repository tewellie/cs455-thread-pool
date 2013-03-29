package cs455.scaling.server;

/**
 * Stores information for use while server prints received messages
 * @author Owner Theresa Wellington
 *
 */
public class MessageInfo {
	
	public String clientName;
	public String hash;
	public int workerID;
	
	public MessageInfo(String client, String hashCode, int id){
		int end = client.indexOf('.');
		clientName = client.substring(0,end);
		hash = hashCode;
		workerID =id;
	}

}
