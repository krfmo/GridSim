/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2008, The University of Melbourne, Australia
 * Author: James Broberg
 */

package gridsim.net.flow;

import eduni.simjava.*;
import gridsim.*;

import java.util.*;
import gridsim.net.Link;
import gridsim.net.Packet;
import gridsim.net.flow.FlowPacket;


/**
 * This class enables flow level networking over a shared link. It is partially based
 * on SimpleLink.java by Gokul Poduval & Chen-Khong Tham
 * 
 * @invariant $none
 * @since GridSim Toolkit 4.2
 * @author James Broberg, The University of Melbourne
 */

public class FlowLink extends Link
{
    private Vector q_;
    private HashMap activeFlows_;	// Stores references to flows that are currently active on this link
    private double lastUpdateTime_; // a timer to denote the last update time
    private int inEnd1_;
    private int outEnd1_;
    private int inEnd2_;
    private int outEnd2_;


    /**
     * Constructs a Link which simulates a physical link between two entities.
     *
     * @param name      Name of this Link
     * @param baudRate  baud rate of this link (bits/s)
     * @param propDelay Propogation delay of the Link in milli seconds
     * @param MTU       Maximum Transmission Unit of the Link in bytes.
     *                  Packets which are larger than the MTU should be split
     *                  up into MTU size units. <br>
     *                  For e.g. a 1024 byte packet trying to cross a 576 byte
     *                  MTU link should get split into 2 packets of 576 bytes
     *                  and 448 bytes.
     * @throws NullPointerException This happens when name is empty or null
     * @throws ParameterException   This happens for the following conditions:
     *      <ul>
     *          <li> name is null
     *          <li> baudRate <= 0
     *          <li> propDelay <= 0
     *          <li> MTU <= 0
     *      </ul>
     *
     * @pre name != null
     * @pre baudRate > 0
     * @pre propDelay > 0
     * @pre MTU > 0
     * @post $none
     */
    public FlowLink(String name, double baudRate, double propDelay, int MTU)
                      throws ParameterException, NullPointerException
    {
        super(name, baudRate, propDelay, MTU);
        init();
    }

    /**
     * Initialises all attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        lastUpdateTime_ = 0.0;
        q_ = new Vector();
        activeFlows_ = new HashMap();
        inEnd1_ = -1;
        outEnd1_ = -1;
        inEnd2_ = -1;
        outEnd2_ = -1;
    }

    /**
     * Connects one entity to another via this link
     * @param end1  an entity
     * @param end2  an entity
     * @pre end1 != null
     * @pre end2 != null
     * @post $none
     */
    public void attach(Sim_entity end1, Sim_entity end2)
    {
        if (end1 == null || end2 == null)
        {
            System.out.println(super.get_name() + ".attach(): Warning - " +
                    "one or both entities are null.");
            return;
        }

        inEnd1_ = GridSim.getEntityId( "Input_" + end1.get_name() );
        outEnd1_ = GridSim.getEntityId( "Output_" + end1.get_name() );

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd1_ == -1 || outEnd1_ == -1)
        {
            inEnd1_ = end1.get_id();
            outEnd1_ = end1.get_id();
        }

        inEnd2_ = GridSim.getEntityId( "Input_" + end2.get_name() );
        outEnd2_ = GridSim.getEntityId( "Output_" + end2.get_name() );

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd2_ == -1 || outEnd2_ == -1)
        {
            inEnd2_ = end2.get_id();
            outEnd2_ = end2.get_id();
        }
    }

    /**
     * Connects one entity to another via this link
     * @param end1  an Entity name
     * @param end2  an Entity name
     * @pre end1 != null
     * @pre end2 != null
     * @post $none
     */
    public void attach(String end1, String end2)
    {
        if (end1 == null || end2 == null)
        {
            System.out.println(super.get_name() + ".attach(): Warning - " +
                    "can not connect since one or both entities are null.");
            return;
        }

        if (end1.length() == 0 || end2.length() == 0)
        {
            System.out.println(super.get_name() + ".attach(): Warning - " +
                    "can not connect since one or both entities are null.");
            return;
        }

        inEnd1_ = GridSim.getEntityId("Input_" + end1);
        outEnd1_ = GridSim.getEntityId("Output_" + end1);

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd1_ == -1 || outEnd1_ == -1)
        {
            inEnd1_ = GridSim.getEntityId(end1);
            outEnd1_ = inEnd1_;
        }

        inEnd2_ = GridSim.getEntityId("Input_" + end2);
        outEnd2_ = GridSim.getEntityId("Output_" + end2);

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd2_ == -1 || outEnd2_ == -1)
        {
            inEnd2_ = GridSim.getEntityId(end1);
            outEnd2_ = inEnd2_;
        }
    }

    /**
     * Handles external events that are coming to this link.
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // register oneself to the system GIS
        super.sim_schedule(GridSim.getGridInfoServiceEntityId(),
                           GridSimTags.SCHEDULE_NOW, GridSimTags.REGISTER_LINK,
                           new Integer(super.get_id()) );

        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);
            
        	//System.out.println(super.get_name() + ".body(): ev.get_tag() is " + ev.get_tag());
        	//System.out.println(super.get_name() + ".body(): ev.get_src() is " + ev.get_src());

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                break;
            }

            // process the received event
        	//System.out.println(super.get_name() + ".body(): processEvent() at time = " + GridSim.clock());
            processEvent(ev);
            sim_completed(ev);
        }

        while(sim_waiting() > 0)
        {
            // wait for event and ignore
            //System.out.println(super.get_name() + ".body(): Ignore !!");
            sim_get_next(ev);
        }
        
        //System.out.println(super.get_name() + ":%%%% Exiting body() at time " +
        //        GridSim.clock() );
    }

    /**
     * Processes incoming events
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processEvent(Sim_event ev)
    {
        switch ( ev.get_tag() )
        {
            case GridSimTags.PKT_FORWARD: // for normal packets
            case GridSimTags.JUNK_PKT:    // for background traffic
            	//System.out.println(super.get_name() + ".processEvent(): enque() at time = " + GridSim.clock());
                enque(ev);
                break;

            case GridSimTags.INSIGNIFICANT:
            	//System.out.println(super.get_name() + ".processEvent(): processInternalEvent() + at time = " + GridSim.clock());
                processInternalEvent();
                break;

            default:
                System.out.println(super.get_name() + ".body(): Warning - " +
                        "unable to handle request from GridSimTags " +
                        "with constant number " + ev.get_tag());
                break;
        }
    }

    /**
     * Sends an internal event to itself for a certain time period
     * @param time  the delay time
     * @pre time >= 0
     * @post $none
     */
    private synchronized boolean sendInternalEvent(double time)
    {
    	//System.out.println(super.get_name() + " sendInternalEvent(): called at time = " + GridSim.clock());

        if (time < 0.0) {
        	//System.out.println(super.get_name() + " sendInternalEvent(): false at time = " + GridSim.clock());
            return false;
        }

    	//System.out.println(super.get_name() + " sendInternalEvent(): scheduled for = " + time + " from now");
        super.sim_schedule(super.get_id(), time, GridSimTags.INSIGNIFICANT);
        return true;
    }

    /**
     * Processes internal events
     * @pre $none
     * @post $none
     */
    private synchronized void processInternalEvent()
    {
    	
    	//System.out.println(super.get_name() + "processInternalEvent(): " + GridSim.clock());

        // this is a constraint that prevents an infinite loop
        // Compare between 2 floating point numbers. This might be incorrect
        // for some hardware platform.
        if ( lastUpdateTime_ == GridSim.clock() ) {
            return;
        }

        lastUpdateTime_ = GridSim.clock();

        if (q_.size() == 0) {
            return;
        }
        else if (q_.size() == 1) {
            deque( (Packet) q_.remove(0) );
        }
        else
        {
            deque( (Packet)q_.remove(0) );
            sendInternalEvent(super.delay_ / super.MILLI_SEC);  // delay in ms

        }
        
        //System.out.println("Super.delay_ is " + super.delay_);
    	//System.out.println(super.get_name() + " processInternalEvent(): done at time = " + GridSim.clock());
    }

    /**
     * Puts an event into a queue, sends an internal event to itself and registers the flow
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private synchronized void enque(Sim_event ev)
    {
    	//System.out.println(super.get_name() + " enque() + at time = " + GridSim.clock());
    	int tag = ((Packet)ev.get_data()).getTag();
        // Register passing flow, gridlet or junk as active on this link
        if (tag == GridSimTags.FLOW_SUBMIT || tag == GridSimTags.GRIDLET_SUBMIT || 
        		tag == GridSimTags.GRIDLET_SUBMIT_ACK || tag == GridSimTags.GRIDLET_RETURN ||
        		tag == GridSimTags.JUNK_PKT) {
        	registerFlow((Packet)ev.get_data());
        }
        
        q_.add( ev.get_data() );
        if (q_.size() == 1) {
            sendInternalEvent(super.delay_ / super.MILLI_SEC); // delay in ms
        }
        

    }

    /**
     * Sends a packet to the next destination
     * @param np    a packet
     * @pre np != null
     * @post $none
     */
    private synchronized void deque(Packet np)
    {
    	
    	//System.out.println(super.get_name() + ".deque() for packet " + np.getID() +" here");
    	//System.out.println(super.get_name() + ".deque() packet " + np.toString());


        int dest = getNextHop(np);
        if (dest == -1) {
        	//System.out.println(super.get_name() + ".deque() here3");
            return;
        }

        // other side is a Sim_entity
        int tag = 0;
        if (dest == outEnd2_ || dest == outEnd1_)
        {
        	//System.out.println(super.get_name() + ".deque() here1");

            // for junk packets only
            if (np.getTag() == GridSimTags.JUNK_PKT) {
                tag = GridSimTags.JUNK_PKT;
            }
            // for other packets
            else {
                tag = GridSimTags.PKT_FORWARD;
            }
        }
        // other side is a GridSim entity
        else {
        	//System.out.println(super.get_name() + ".deque() here2");

            tag = np.getTag();
        }

        // sends the packet
        super.sim_schedule(dest, GridSimTags.SCHEDULE_NOW, tag, np);
    	//System.out.println(super.get_name() + ".deque() + at time = " + GridSim.clock());

    }

    /**
     * Determines which end to send the event to
     * since sending entities are of the form Output_entityName.
     * We need to check whether the source name ends with either end1 or end2
     * the other end is the destination
     * @param np    a packet
     * @pre np != null
     * @post $none
     */
    private synchronized int getNextHop(Packet np)
    {
        int dest = -1;
        int src = np.getLast();

        // check if source is from outEnd1_
        if (src == outEnd1_) {
            dest = inEnd2_;
        }
        // or source is from outEnd2_
        else if (src == outEnd2_) {
            dest = inEnd1_;
        }

        return dest;
    }
    
    /**
     * Registers active flow to the link, and the link to flow.
     * Notifies the destination of any active flows if the bottleneck
     * bandwidth is changed.
     * @param np    a packet
     * @pre $none
     * @post $none
     */
    private synchronized void registerFlow(Packet np) {
    	
    	FlowPacket tempFlow = null;
    	
    	// Add flow to link
    	activeFlows_.put(np.getID(), np );
    	
    	//System.out.println(super.get_name() + ".registerFlow(): registering flow #" + np.getID() 
    	//		+ " total of " + activeFlows_.size() + " flows");
    	        
    	// Register link to flow
        ((FlowPacket)np).addLink(this);
        
        // Check if this affects any existing flows
        Iterator<Integer> flowsIter = activeFlows_.keySet().iterator();
        while(flowsIter.hasNext()) {
        	tempFlow = (FlowPacket) activeFlows_.get(flowsIter.next());
        	// If change in bandwidth affects an existing flow i.e. is < current bottleneck
        	if (this.getBaudRate() < tempFlow.getBandwidth() && tempFlow.getID() != np.getID()) {
        		// Need to notify flow
        		//System.out.println(super.get_name() + ".registerFlow():  flow #" + np.getID() 
            	//		+ " bottleneck now " + this.getBaudRate() + " at time " + GridSim.clock());
        		// I can notify directly as I  know the destId's!!!!
        		//System.out.println(super.get_name() + ".registerFlow(): updating flow #" + tempFlow.getID()
        		//		+ " destination " + tempFlow.getDestID());
                super.sim_schedule(GridSim.getEntityId("Input_" + 
                		GridSim.getEntityName(tempFlow.getDestID())), GridSimTags.SCHEDULE_NOW, 
                		GridSimTags.FLOW_UPDATE, new Integer(tempFlow.getID()));  
        	}
        }
    }
    
    /**
     * Deregisters active flow on link. Notifies the destination of any active 
     * flows if the bottleneck bandwidth is changed.
     * @param np    a packet
     * @pre $none
     * @post $none
     */
    // NOTE: this method is called in FlowInput.java line 238
    // inside the checkForecast() method
    public synchronized void deregisterFlow(Packet np) {
    	FlowPacket fp = null;
    	FlowPacket tempFlow;

    	// If the flow hasn't been removed already, remove from active flow list
    	if ((fp = (FlowPacket) activeFlows_.remove(np.getID())) != null) {
    		
    		//System.out.println(super.get_name() + ".deregisterFlow() success flow # " + np.getID() 
    		//		+ " " + fp.getBandwidth());
    		
    	    // Check if this affects any existing flows
            Iterator<Integer> flowsIter = activeFlows_.keySet().iterator();
            while(flowsIter.hasNext()) {
            	tempFlow = (FlowPacket) activeFlows_.get(flowsIter.next());
            	// If change in bandwidth affects an existing flow i.e. is > current bottleneck
            	// AND this link is the particular flow's bottleneck
            	if (this.getBaudRate() > tempFlow.getBandwidth() && tempFlow.getID() != np.getID() && 
            			tempFlow.getBottleneckID() == this.get_id()) {
            		// Need to notify flow
            		//System.out.println(super.get_name() + ".deregisterFlow():  flow #" + np.getID() 
                	//		+ " bottleneck now " + this.getBaudRate() + " at time " + GridSim.clock());
            		// I can notify directly as I  know the destId's!!!!
            		//System.out.println(super.get_name() + ".deregisterFlow(): updating flow #" + tempFlow.getID()
            		//		+ " destination " + tempFlow.getDestID());
                    super.sim_schedule(GridSim.getEntityId("Input_" + 
                    		GridSim.getEntityName(tempFlow.getDestID())), GridSimTags.SCHEDULE_NOW, 
                    		GridSimTags.FLOW_UPDATE, new Integer(tempFlow.getID()));  
            	}
    		
            }
    	}
    }
    
    /**
     * Returns available baudRate depending on number of
     * active flows (MIN_MAX bandwidth sharing model)
     * @pre $none
     * @post $none
     */
    public synchronized double getBaudRate() {
		if (activeFlows_.size() != 0) {
	    	//System.out.println(super.get_name() + ".getBaudRate() Getting latest baud! " + (super.baudRate_)/(activeFlows_.size()));
			return (super.baudRate_)/(activeFlows_.size());
		} else {
	    	//System.out.println(super.get_name() + ".getBaudRate() Getting latest baud! " + (super.baudRate_));
			return super.baudRate_;
		}

    }
    	


} // end class

