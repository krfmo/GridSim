/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2008, The University of Melbourne, Australia
 * Author: James Broberg (based on Input.java)
 */

package gridsim.net.flow;

import gridsim.*;
import gridsim.net.*;
import gridsim.util.TrafficGenerator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import eduni.simjava.*;

/**
 * GridSim FlowInput class defines a port through which a simulation entity
 * receives data from the simulated network.
 * <p>
 * It maintains an event queue
 * to serialize the data-in-flow and delivers to its parent entity.
 * It accepts event messages that comes from the 'FlowOutput' entity
 * and passes the event data to the GridSim entity.
 * It simulates Network communication delay depending on current bottleneck Baud rate
 * and data length. Simultaneous inputs can be modeled using multiple
 * instances of this class.
 *
 * @author       James Broberg
 * @since   GridSim Toolkit 4.2
 * @invariant $none
 */
public class FlowInput extends Sim_entity implements NetIO
{
    private Sim_port inPort_;
    private Link link_;
    private double baudRate_;
    private HashMap<Integer, Packet> activeFlows_; // stores a list of active Flows


    /**
     * Allocates a new FlowInput object
     * @param name         the name of this object
     * @param baudRate     the communication speed
     * @throws NullPointerException This happens when creating this entity
     *                  before initializing GridSim package or this entity name
     *                  is <tt>null</tt> or empty
     * @pre name != null
     * @pre baudRate >= 0.0
     * @post $none
     */
    public FlowInput(String name, double baudRate) throws NullPointerException
    {
        super(name);
        this.baudRate_ = baudRate;
        link_= null;

        inPort_ = new Sim_port("input_buffer");
        super.add_port(inPort_);
        
        activeFlows_ = null;

    }

    /**
     * Sets the FlowInput entities link. This should be used only if the network
     * extensions are being used.
     * @param link the link to which this FlowInput entity should send data
     * @pre link != null
     * @post $none
     */
    public void addLink(Link link)
    {
        this.link_ = link;
        baudRate_ = link_.getBaudRate();
        activeFlows_ = new HashMap();

    }

    /**
     * Gets the baud rate
     * @return the baud rate
     * @pre $none
     * @post $result >= 0.0
     */
    public double getBaudRate() {
        return baudRate_;
    }

    /**
     * Gets the I/O real number based on a given value
     * @param value   the specified value
     * @return real number
     * @pre value >= 0.0
     * @post $result >= 0.0
     */
    public double realIO(double value) {
        return GridSimRandom.realIO(value);
    }

    /**
     * This is an empty method and only applicable to
     * {@link gridsim.net.flow.FlowOutput} class.
     * @param gen       a background traffic generator
     * @param userName  a collection of user entity name (in String object).
     * @return <tt>false</tt> since this method is not used by this class.
     * @pre gen != null
     * @pre userName != null
     * @post $none
     * @see gridsim.net.flow.FlowOutput
     */
    public boolean setBackgroundTraffic(TrafficGenerator gen,
                                        Collection userName)
    {
        return false;
    }

    /**
     * This is an empty method and only applicable to
     * {@link gridsim.net.Output} class.
     * @param gen   a background traffic generator
     * @return <tt>false</tt> since this method is not used by this class.
     * @pre gen != null
     * @post $none
     * @see gridsim.net.Output
     */
    public boolean setBackgroundTraffic(TrafficGenerator gen)
    {
        return false;
    }

    /**
     * A method that gets one process event at one time until the end
     * of a simulation, then delivers an event to the entity (its parent)
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // Process events
        Object obj = null;
        while ( Sim_system.running() )
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev);     // get the next event in the queue
            obj = ev.get_data();        // get the incoming data
            
            /**** NOTE: debugging
        	System.out.println(super.get_name() + ".body(): ev.get_tag() is " + ev.get_tag());
        	System.out.println(super.get_name() + ".body(): ev.get_src() is " + ev.get_src());
            *****/
            
            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                break;
            // Check forecast of active flow and pass it onto Entity if still active
            } else if(ev.get_tag() == GridSimTags.FLOW_HOLD) {
            	//System.out.println(super.get_name() + ".body(): checkForecast() + at time = " + GridSim.clock());
                checkForecast(ev);
            // Update flow duration forecast as a flow's bottleneck bandwidth has changed
            } else if (ev.get_tag() == GridSimTags.FLOW_UPDATE) {
            	//System.out.println(super.get_name() + ".body(): updateForecast() + at time = " + GridSim.clock());
                updateForecast(ev);            
            } 
            
            // if this entity is not connected in a network topology
            if (obj != null && obj instanceof IO_data) {
            	//System.out.println(super.get_name() + ".body(): getDataFromEvent() + at time = " + GridSim.clock());
                getDataFromEvent(ev);
            // if this entity belongs to a network topology
            } else if (obj != null && link_ != null) {
            	//System.out.println(super.get_name() + ".body(): getDataFromLink() + at time = " + GridSim.clock());
                getDataFromLink(ev);
            }

            ev = null;   // reset to null for gc to collect
        }
        
        //System.out.println(super.get_name() + ":%%%% Exiting body() at time " +
        //        GridSim.clock() );
    }

    /**
     * Check the forecast of a flow, and send data to output port if flow 
     * still exists
     *
     * @param ev the flow hold notification event 
     * @pre ev != null
     * @post $none
     */
    private void checkForecast(Sim_event ev) {
    	int pktID = (Integer) ev.get_data();	// ID of flow to be checked
    	FlowPacket fp = null;					// Reference to flow packet that needs forecast update
    
        //System.out.println(super.get_name() + ".checkForecast(): checking pkt id # " + pktID);
    	
        // If flow hasn't already finished, send it to inPort
        if ((fp = (FlowPacket) activeFlows_.get(pktID)) != null) {
            Object data = fp.getData();
            IO_data io = new IO_data( data, fp.getSize(),
            		                  inPort_.get_src());
            super.sim_schedule(inPort_, GridSimTags.SCHEDULE_NOW, fp.getTag() , io.getData());
            activeFlows_.remove(pktID);  
            
            //System.out.println(super.get_name() + ".checkForecast(): flow came from " + GridSim.getEntityName(fp.getSrcID())
            //		+ " heading to " + GridSim.getEntityName(fp.getDestID()));
            
            // Deregister flow on all active links
        	Iterator it = (fp.getLinks()).iterator();    	
        	while (it.hasNext()) {
        		FlowLink fl = (FlowLink) it.next();
        		fl.deregisterFlow(fp);
        	}
        
        } else {
            //System.out.println(super.get_name() + ".checkForecast(): pkt id # " + pktID + " already removed");

        }
		
	}
    
    /**
     * Update the forecast of a flow by deleting the old forecast and scheduling a new flow hold 
     * event in the future with the corrected forecast (based on new bottleneck bandwidth and 
     * the amount of flow already sent)
     *
     * @param ev the flow update notification event 
     * @pre ev != null
     * @post $none
     */
    private void updateForecast(Sim_event ev) {
    	int pktID = (Integer) ev.get_data();	// ID of flow to be updated
    	FlowPacket fp = null;					// Reference to flow packet that needs forecast update
    	double duration = 0.0;					// New forecast duration from current Gridsim.clock()
    	long remSizeOld = 0;					// Previous remaining size 
    	double bandwidthOld = 0.0;				// Previous bottleneck BW
    	int sourceID = ev.get_src();			// ID of source of notification (FlowLink)
    	int cancelledFlow = 0;					// Count of canceled future events that match old forecast
    	
        //System.out.println(super.get_name() + ".updateForecast(): updating pkt id # " + pktID);

        // If flow hasn't already finished and been cleared...
        if ((fp = (FlowPacket) activeFlows_.get(pktID)) != null) {
        	remSizeOld = fp.getRemSize();
        	bandwidthOld = fp.getBandwidth();
        	//System.out.println(super.get_name() + "updateForecast(): rem size is " + remSizeOld + 
        	//		"BW old is " + bandwidthOld + " last update " + fp.getUpdateTime());
        	Iterator it = (fp.getLinks()).iterator();
        	
        	// Find the source link of this notification and the associated bottleneck bandwidth
        	while (it.hasNext()) {
        		FlowLink fl = (FlowLink) it.next();
        		if (fl.get_id() == sourceID) {
        			fp.setBandwidth(fl.getBaudRate());
        			fp.setBottleneckID(sourceID);
        		}
        	}
        	
        	fp.setRemSize((long)((remSizeOld) - ((GridSim.clock()-fp.getUpdateTime())*(bandwidthOld/NetIO.BITS))));
        	duration = (fp.getRemSize()*NetIO.BITS)/fp.getBandwidth();
        	//System.out.println(super.get_name() + " new forecast end time is " + (GridSim.clock() + duration));
        	
        	// Find old forecast and delete it!
        	FilterFlow filter = new FilterFlow(fp.getID(), GridSimTags.FLOW_HOLD);  	
            cancelledFlow = this.sim_cancel(filter, null);
        	
            /**** // debugging
        	if (cancelledFlow != 0) {
        		System.out.println(super.get_name() + ".updateForecast(): old forecast cancelled #matches " 
        				+ cancelledFlow);
        	}
            ******/
        	
        	//System.out.println(super.get_name() + " setting updated forecast for packet #" + fp.getID());
        	// Make a note of the time when this forecast update was performed
        	fp.setUpdateTime(GridSim.clock());
        	// Set new future event with the correct finish time
            super.sim_schedule(super.get_id(), duration, GridSimTags.FLOW_HOLD , new Integer(fp.getID()));
            
        }
    }

    /**
     * Process incoming event for data without using the network extension
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void getDataFromEvent(Sim_event ev)
    {
        IO_data io = (IO_data) ev.get_data();

        // if the sender is not part of the overall network topology
        // whereas this entity is, then need to return back the data,
        // since it is not compatible.
        if (link_ != null)
        {
            // outName = "Output_xxx", where xxx = sender entity name
            String outName = GridSim.getEntityName( ev.get_src() );

            // NOTE: this is a HACK job. "Output_" has 7 chars. So,
            // the idea is to get only the entity name by removing
            // "Output_" word in the outName string.
            String name = outName.substring(7);

            // if the sender is not system GIS then ignore the message
            if (GridSim.getEntityId(name) != GridSim.getGridInfoServiceEntityId())
            {
                // sends back the data to "Input_xxx", where
                // xxx = sender entity name. If not sent, then the sender
                // will wait forever to receive this data. As a result,
                // the whole simulation program will be hanged or does not
                // terminate successfully.
                int id = GridSim.getEntityId("Input_" + name);
                super.sim_schedule(id, 0.0, ev.get_tag(), io);

                // print an error message
                System.out.println(super.get_name() + ".body(): Error - " +
                    "incompatible message protocol.");
                System.out.println("    Sender: " + name + " is not part " +
                    "of this entity's network topology.");
                System.out.println("    Hence, sending back the received data.");
                System.out.println();
                return;
            }
        }

        // NOTE: need to have a try-catch statement. This is because,
        // if the above if statement holds, then Input_receiver will send
        // back to Input_sender without going through Output_receiver entity.
        // Hence, a try-catch is needed to prevent exception of wrong casting.
        try
        {
            // Simulate Transmission Time after Receiving
            // Hold first then dispatch
            double senderBaudRate = ( (FlowOutput)
                    Sim_system.get_entity(ev.get_src()) ).getBaudRate();

            // NOTE: io is in byte and baud rate is in bits. 1 byte = 8 bits
            // So, convert io into bits
            double minBaudRate = Math.min(baudRate_, senderBaudRate);
            double communicationDelay = GridSimRandom.realIO(
                    (io.getByteSize() * NetIO.BITS) / minBaudRate);

            // NOTE: Below is a deprecated method for SimJava 2
            //super.sim_hold(communicationDelay);
            super.sim_process(communicationDelay);   // receiving time
        }
        catch (Exception e) {
            // .... empty
        }

        // Deliver Event to the entity (its parent) to which
        // it is acting as buffer
        super.sim_schedule( inPort_, GridSimTags.SCHEDULE_NOW,
                ev.get_tag(), io.getData() );
    }

    /**
     * Process incoming events from senders that are using the network
     * extension
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void getDataFromLink(Sim_event ev)
    {
        Object obj = ev.get_data();
        double duration = 0.0;
        
        if (obj instanceof Packet)
        {
            // decrypt the packet into original format
            Packet pkt = (Packet) ev.get_data();

            if (pkt instanceof InfoPacket)
            {
                processPingRequest( (InfoPacket) pkt);
                return;
            }

            if (pkt instanceof FlowPacket)
            {
                FlowPacket np = (FlowPacket) pkt;
                int tag = np.getTag();
                //System.out.println("Packet id is " + np.getID());

                // ignore incoming junk packets
                if (tag == GridSimTags.JUNK_PKT) {
                    return;
                }

                // ignore incoming null dummy packets
                if (tag == GridSimTags.EMPTY_PKT && np.getData() == null) {
                    return;
                }
                
                //System.out.println(super.get_name() + ".getDataFromLink() Time now " + GridSim.clock() 
                //		+ " bottleneck is " + np.getBandwidth() + " sum lat is " + np.getLatency() );
                
                // if flow terminates at next entity, add to active flows 
                // & hold for appropriate duration
                if (pkt.getTag() == GridSimTags.FLOW_SUBMIT || pkt.getTag() == GridSimTags.GRIDLET_SUBMIT ||
                		pkt.getTag() == GridSimTags.GRIDLET_SUBMIT_ACK || pkt.getTag() == GridSimTags.GRIDLET_RETURN
                		|| pkt.getTag() == GridSimTags.JUNK_PKT) {
                	np.setStartTime(GridSim.clock());
                	np.setUpdateTime(GridSim.clock());
                	duration = np.getSize()*NetIO.BITS / np.getBandwidth();
                	activeFlows_.put(pkt.getID(), pkt);
                	super.sim_schedule(super.get_id(), duration, GridSimTags.FLOW_HOLD, new Integer(pkt.getID()));
                    //System.out.println(super.get_name() + ".getDataFromLink() initial forecast flow end at " + (GridSim.clock()
                    //		+ duration));
                    return;
                }
                
                // convert the packets into IO_data
                Object data = np.getData();
                IO_data io = new IO_data( data, np.getSize(),
                                          inPort_.get_dest() );
                
                // send the data into entity input port
                super.sim_schedule(inPort_, GridSimTags.SCHEDULE_NOW, tag,
                                   io.getData() );
 

            }
        }
    }

    /**
     * Processes a ping request
     * @param   pkt     a packet for pinging
     * @pre pkt != null
     * @post $none
     */
    private void processPingRequest(InfoPacket pkt)
    {
        // add more information to ping() packet
        pkt.addHop( inPort_.get_dest() );
        pkt.addEntryTime( GridSim.clock() );

        IO_data io = new IO_data( pkt, pkt.getSize(), inPort_.get_dest() );

        // send this ping() packet to the entity
        super.sim_schedule(inPort_, GridSimTags.SCHEDULE_NOW,
                           pkt.getTag(), io.getData());
    }

} // end class

