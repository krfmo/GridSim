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

import gridsim.*;
import gridsim.net.*;
import gridsim.net.flow.*;
import eduni.simjava.*;
import java.util.*;


/**
 * This class basically sends one or more messages to the other
 * entity over a link. Then, it waits for an ack.
 * Finally, before finishing the simulation, it pings the other
 * entity.
 */
public class FlowNetUser extends GridSim
{
    private int myID_;          // my entity ID
    private String name_;       // my entity name
    private String destName_;   // destination name
    private int destID_;        // destination id
    private double wait_;		// Delay until I begin sending
    
    /** Custom tag that denotes sending a message */
    public static final int SEND_MSG = 1;    
    public static final int ACK_MSG = 2;


    /**
     * Creates a new NetUser object
     * @param name      this entity name
     * @param destName  the destination entity's name
     * @param link      the physical link that connects this entity to destName
     * @throws Exception    This happens when name is null or haven't 
     *                      initialized GridSim.
     */
    public FlowNetUser(String name, String destName, Link link, double wait) throws Exception
    {
        super(name, link);

        // get this entity name from Sim_entity
        this.name_ = super.get_name();

        // get this entity ID from Sim_entity
        this.myID_ = super.get_id();

        // get the destination entity name
        this.destName_ = destName;
        
        // get the waiting time before sending
        this.wait_ = wait;
    }
    
    public FlowNetUser(String name, String destName, double wait) throws Exception
    {
    	// 10,485,760 baud = 10Mb/s
        super(name, new FlowLink(name+"_link",10485760,450,Integer.MAX_VALUE));

        // get this entity name from Sim_entity
        this.name_ = super.get_name();

        // get this entity ID from Sim_entity
        this.myID_ = super.get_id();

        // get the destination entity name
        destName_ = destName;
        
        // get the waiting time before sending
        this.wait_ = wait;
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        int packetSize = 524288000;   // packet size in bytes [5MB]
        //int packetSize = 52428800;   // packet size in bytes [50MB]
        //int packetSize = 524288000;   // packet size in bytes [500MB]
        //int packetSize = 5242880000;   // packet size in bytes [5000MB]
        int size = 3;           // number of packets sent
        int i = 0;

        // get the destination entity ID
        this.destID_ = GridSim.getEntityId(destName_);
        
    	//super.sim_pause(this.wait_);
        this.gridSimHold(this.wait_);


        // sends messages over the other side of the link
        for (i = 0; i < size; i++)
        {

            String msg = "Message_" + i;
            IO_data data = new IO_data(msg, packetSize, destID_);
            System.out.println(name_ + ".body(): Sending " + msg +
                ", at time = " + GridSim.clock() );

            // sends through Output buffer of this entity
            super.send(super.output, GridSimTags.SCHEDULE_NOW,
                       GridSimTags.FLOW_SUBMIT, data);
         
            //super.sim_pause();
            super.sim_pause(10.0);
            //this.gridSimHold((Math.random()*10)+1.0);
            
        }

        ////////////////////////////////////////////////////////
        // get the ack back
        Object obj = null;
        for (i = 0; i < size; i++)
        {
            // waiting for incoming event in the Input buffer
            obj = super.receiveEventObject();
            System.out.println(name_ + ".body(): Receives Ack for " + obj);
        }         

        
        // Wait for other FlowNetUser instances to finish
        this.gridSimHold(1000.0);
        
   
        super.send(destID_, GridSimTags.SCHEDULE_NOW,
                   GridSimTags.END_OF_SIMULATION);
        

        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();

        System.out.println(this.name_ + ":%%%% Exiting body() at time " +
                           GridSim.clock() );
    }

} // end class

