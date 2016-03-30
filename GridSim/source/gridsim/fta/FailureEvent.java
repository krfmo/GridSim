package gridsim.fta;
import java.util.ArrayList; 

/**
 * This is the class to define the failure events
 *  
 * @author       Bahman Javadi
 * @since        GridSim Toolkit 5.0
 * 
 * @see FailureFileReader
 * @see FTAllocPolicy
 */

public class FailureEvent {
	private int NodeID = -1;
	private ArrayList<Integer> eventType = null;
	private ArrayList<Double> startTime = null;
	private ArrayList <Double> endTime = null;
	
	public FailureEvent(int NodeID, int eventType, double startTime, double endTime){
		this.NodeID = NodeID;
		if(this.eventType == null) {
	        this.eventType = new ArrayList<Integer>();
		}
		if(this.startTime == null) {
	        this.startTime = new ArrayList<Double>();
		}
		if(this.endTime == null) {
	        this.endTime = new ArrayList<Double>();
		}
		this.eventType.add(eventType);
		this.startTime.add(startTime);
		this.endTime.add(endTime);
	}

	/**
	 * Returns the node ID from the file.
	 * @return the node ID from the file.
	 */
	public int getNodeID(){
		return NodeID;
	}

	public void insertEvent(int NodeID, int eventType, double startTime, double endTime){
		this.NodeID = NodeID;
		this.eventType.add(eventType);
		this.startTime.add(startTime);
		this.endTime.add(endTime);
	}

	/**
	 * Returns the number of events in the node file.
	 * @return the number of events in the node file.
	 */
	public int getnumEvent(){
		return this.eventType.size();
	}

	/**
	 * Returns the type of event at a given index.
	 * @return the type of event at a given index.
	 */
	public int geteventType(int index){
		return this.eventType.get(index);
	}

	/**
	 * Returns the start time of event at a given index.
	 * @return the start time of event at a given index.
	 */
	public double getstartTime(int index){
		return this.startTime.get(index);
	}

}
