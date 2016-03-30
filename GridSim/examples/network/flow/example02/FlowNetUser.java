package network.flow.example02;

/*
 * Author: Anthony Sulistio
 * Author: James Broberg (adapted from NetEx02)
 * Date: March 2008
 * Description: A simple program to demonstrate of how to use GridSim
 *              network extension package.
 *              This example shows how to create user and resource
 *              entities connected via a network topology, using link
 *              and router.
 *
 */

import java.util.*;
import gridsim.*;
import gridsim.net.*;
import gridsim.net.flow.FlowLink;
import gridsim.util.SimReport;


/**
 * This class basically creates Gridlets and submits them to a
 * particular GridResources in a network topology.
 */
class FlowNetUser extends GridSim
{
    private int myId_;      // my entity ID
    private String name_;   // my entity name
    private GridletList list_;          // list of submitted Gridlets
    private GridletList receiveList_;   // list of received Gridlets
    private SimReport report_;  // logs every events


    /**
     * Creates a new NetUser object
     * @param name  this entity name
     * @param totalGridlet  total number of Gridlets to be created
     * @param baud_rate     bandwidth of this entity
     * @param delay         propagation delay
     * @param MTU           Maximum Transmission Unit
     * @param trace_flag    logs every event or not
     * @throws Exception    This happens when name is null or haven't
     *                      initialized GridSim.
     */
    FlowNetUser(String name, int totalGridlet, double baud_rate, double delay,
            int MTU, boolean trace_flag) throws Exception
    {
        //super( name, new FlowLink(name+"_link",baud_rate,delay, MTU) );
        super(name, new FlowLink(name+"_link",baud_rate,delay,Integer.MAX_VALUE));


        this.name_ = name;
        this.receiveList_ = new GridletList();
        this.list_ = new GridletList();

        // creates a report file
        if (trace_flag == true) {
            report_ = new SimReport(name);
        }

        // Gets an ID for this entity
        this.myId_ = super.getEntityId(name);
        write("Creating a grid user entity with name = " +
              name + ", and id = " + this.myId_);

        // Creates a list of Gridlets or Tasks for this grid user
        write(name + ":Creating " + totalGridlet +" Gridlets");
        this.createGridlet(myId_, totalGridlet);
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        // wait for a little while for about 3 seconds.
        // This to give a time for GridResource entities to register their
        // services to GIS (GridInformationService) entity.
        super.gridSimHold(3.0);
        LinkedList resList = super.getGridResourceList();

        // initialises all the containers
        int totalResource = resList.size();
        int resourceID[] = new int[totalResource];
        String resourceName[] = new String[totalResource];

        // a loop to get all the resources available
        int i = 0;
        for (i = 0; i < totalResource; i++)
        {
            // Resource list contains list of resource IDs
            resourceID[i] = ( (Integer) resList.get(i) ).intValue();

            // get their names as well
            resourceName[i] = GridSim.getEntityName( resourceID[i] );
        }

        ////////////////////////////////////////////////
        // SUBMIT Gridlets

        // determines which GridResource to send to
        int index = myId_ % totalResource;
        if (index >= totalResource) {
            index = 0;
        }

        // sends all the Gridlets
        Gridlet gl = null;
        boolean success;
        for (i = 0; i < list_.size(); i++)
        {
            gl = (Gridlet) list_.get(i);
            write(name_ + "Sending Gridlet #" + i + " to " + resourceName[index]);

            // For even number of Gridlets, send without an acknowledgement
            // whether a resource has received them or not.
            if (i % 2 == 0)
            {
                // by default - send without an ack
                success = super.gridletSubmit(gl, resourceID[index]);
            }

            // For odd number of Gridlets, send with an acknowledgement
            else
            {
                // this is a blocking call
                success = super.gridletSubmit(gl,resourceID[index],0.0,true);
                write("ack = " + success + " for Gridlet #" + i);
            }
        }

        ////////////////////////////////////////////////////////
        // RECEIVES Gridlets back

        // hold for few period - few seconds since the Gridlets length are
        // quite huge for a small bandwidth
        super.gridSimHold(5);

        // receives the gridlet back
        for (i = 0; i < list_.size(); i++)
        {
            gl = (Gridlet) super.receiveEventObject();  // gets the Gridlet
            receiveList_.add(gl);   // add into the received list

            write(name_ + ": Receiving Gridlet #" +
                  gl.getGridletID() + " at time = " + GridSim.clock() );
        }

        ////////////////////////////////////////////////////////
        // ping functionality
        InfoPacket pkt = null;
        int size = 500;

        // There are 2 ways to ping an entity:
        // a. non-blocking call, i.e.
        //super.ping(resourceID[index], size);    // (i)   ping
        //super.gridSimHold(10);        // (ii)  do something else
        //pkt = super.getPingResult();  // (iii) get the result back

        // b. blocking call, i.e. ping and wait for a result
        pkt = super.pingBlockingCall(resourceID[index], size);

        // print the result
        write("\n-------- " + name_ + " ----------------");
        write(pkt.toString());
        write("-------- " + name_ + " ----------------\n");

        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();

        // don't forget to close the file
        if (report_ != null) {
            report_.finalWrite();
        }

        write(this.name_ + ": sending and receiving of Gridlets" +
              " complete at " + GridSim.clock() );
    }

    /**
     * Gets a list of received Gridlets
     * @return a list of received/completed Gridlets
     */
    public GridletList getGridletList() {
        return receiveList_;
    }

    /**
     * This method will show you how to create Gridlets
     * @param userID        owner ID of a Gridlet
     * @param numGridlet    number of Gridlet to be created
     */
    private void createGridlet(int userID, int numGridlet)
    {
        int data = 5000;
        for (int i = 0; i < numGridlet; i++)
        {
            // Creates a Gridlet
            Gridlet gl = new Gridlet(i, data, data, data);
            gl.setUserID(userID);

            // add this gridlet into a list
            this.list_.add(gl);
        }
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

