package network.flow.example01;

/*
 * Author: Anthony Sulistio
 * Author: James Broberg (adapted from NetEx01)
 * Date: March 2008
 * Description: A simple program to demonstrate of how to use GridSim 
 *              network extension package.
 *              This example shows how to create two GridSim entities and
 *              connect them via a link. NetUser entity sends messages to
 *              Test entity and Test entity sends back these messages.
 */
 
import java.util.*;
import gridsim.*;
import gridsim.net.*;
import gridsim.net.flow.*;
import gridsim.util.SimReport;
import eduni.simjava.*;


/**
 * This class handles incoming requests and sends back an ack.
 * In addition, this class logs every activities.
 */
public class FlowTest extends GridSim
{
    private int myID_;          // my entity ID
    private String name_;       // my entity name
    private String destName_;   // destination name
    private int destID_;        // destination id
    private SimReport report_;  // logs every activity

    /**
     * Creates a new NetUser object
     * @param name      this entity name
     * @param destName  the destination entity's name
     * @param link      the physical link that connects this entity to destName
     * @throws Exception    This happens when name is null or haven't 
     *                      initialized GridSim.
     */
    public FlowTest(String name, String destName, Link link) throws Exception
    {
        super(name, link);

        // get this entity name from Sim_entity
        this.name_ = super.get_name();

        // get this entity ID from Sim_entity
        this.myID_ = super.get_id();

        // get the destination entity name
        this.destName_ = destName;
        
        // logs every activity. It will automatically create name.csv file
        report_ = new SimReport(name);
        report_.write("Creates " + name);
    }

    public FlowTest(String name, String destName) throws Exception
    {
    	// 10,485,760 baud = 10Mb/s
        super(name, new FlowLink(name+"_link",10485760,250,Integer.MAX_VALUE));

        // get this entity name from Sim_entity
        this.name_ = super.get_name();

        // get this entity ID from Sim_entity
        this.myID_ = super.get_id();
        
        // get the destination entity name
        this.destName_ = destName;
  
        
        // logs every activity. It will automatically create name.csv file
        report_ = new SimReport(name);
        report_.write("Creates " + name);
    }

	/**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        // get the destination entity ID
        this.destID_ = GridSim.getEntityId(destName_);

        int packetSize = 1500;   // packet size in bytes
        Sim_event ev = new Sim_event();     // an event
        
        // a loop waiting for incoming events
        while ( Sim_system.running() )
        {
            // get the next event from the Input buffer
            super.sim_get_next(ev);
            
            // if an event denotes end of simulation
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                System.out.println();
                write(super.get_name() + ".body(): exiting ...");
                break;
            }
            
            // if an event denotes another event type
            else if (ev.get_tag() == GridSimTags.FLOW_SUBMIT)
            {
                System.out.println();
                write(super.get_name() + ".body(): receive " +
                      ev.get_data() + ", at time = " + GridSim.clock());

                // No need for an ack, it is handled in FlowBuffer now on our behalf
                // sends back an ack
                IO_data data = new IO_data(ev.get_data(), packetSize, destID_);
                write(name_ + ".body(): Sending back " +
                      ev.get_data() + ", at time = " + GridSim.clock() );

                // sends through Output buffer of this entity
                super.send(super.output, GridSimTags.SCHEDULE_NOW,
                		GridSimTags.FLOW_ACK, data);
                
            }
            
            // handle a ping requests. You need to write the below code
            // for every class that extends from GridSim or GridSimCore.
            // Otherwise, the ping functionality is not working.
            else if (ev.get_tag() ==  GridSimTags.INFOPKT_SUBMIT)
            {
                processPingRequest(ev);                
            }
        }

        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();

        // don't forget to close the file
        if (report_ != null) {
            report_.finalWrite();
        }
        
        System.out.println(this.name_ + ":%%%% Exiting body() at time " +
                           GridSim.clock() );
    }

    /**
     * Handles ping request
     * @param ev    a Sim_event object
     */
    private void processPingRequest(Sim_event ev)
    {
        InfoPacket pkt = (InfoPacket) ev.get_data();
        pkt.setTag(GridSimTags.INFOPKT_RETURN);
        pkt.setDestID( pkt.getSrcID() );

        // sends back to the sender
        super.send(super.output, GridSimTags.SCHEDULE_NOW,
                   GridSimTags.INFOPKT_RETURN,
                   new IO_data(pkt,pkt.getSize(),pkt.getSrcID()) );
    }
    
    /**
     * Prints out the given message into stdout.
     * In addition, writes it into a file.
     * @param msg   a message
     */
    private void write(String msg)
    {
        System.out.println(msg);
        if (report_ != null) {
            report_.write(msg);
        }        
    }
    
} // end class

