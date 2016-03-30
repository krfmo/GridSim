package WorkloadTrace.example02;

/*
 * Author: Anthony Sulistio
 * Date: November 2004
 * Description: A simple program to demonstrate of how to use GridSim
 *              workload trace functionality.
 */

import java.util.*;
import eduni.simjava.*;
import gridsim.*;


/**
 * This class basically sends one or more Gridlets to a particular
 * grid resource entity.
 */
class User extends GridSim
{
    private String name_;   // my entity name
    private int myId_;      // my entity ID
    private GridletList list_;          // a list storing new Gridlets
    private GridletList receiveList_;   // a list storing completed Gridlets


    /**
     * Creates a new User entity
     * @param name          this entity name
     * @param bandwidth     baud rate of this entity
     * @param totalGridlet  total number of Gridlets created
     * @throws Exception    This happens when name is null or haven't
     *                      initialized GridSim
     */
    User(String name, double bandwidth, int totalGridlet) throws Exception
    {
        super(name, bandwidth);

        this.name_ = super.get_name();
        this.myId_ = super.get_id();
        this.receiveList_ = new GridletList();
        this.list_ = new GridletList();

        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + this.myId_);

        // Creates a list of Gridlets or Tasks for this grid user
        System.out.println(name + ":Creating "+ totalGridlet +" Gridlets");
        this.createGridlet(myId_, totalGridlet);
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        ////////////////////////////////////////////////
        // wait for a little while for few seconds.
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

            // For even number of Gridlets, send with an acknowledgement
            if (i % 2 == 0)
            {
                // by default - send without an ack
                success = super.gridletSubmit(gl, resourceID[index]);
                System.out.println(name_ + ": Sending Gridlet #" +
                       gl.getGridletID() + " with NO ACK so status = " +
                       success + " to " + resourceName[index]);
            }

            // For odd number of Gridlets, send with an acknowledgement
            else
            {
                success = super.gridletSubmit(gl, resourceID[index],0.0,true);
                System.out.println(name_ + ": Sending Gridlet #" +
                       gl.getGridletID() + " with status = " + success +
                       " to " + resourceName[index]);
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

            System.out.println(name_ + ": Receiving Gridlet #" +
                  gl.getGridletID() + " at time = " + GridSim.clock() );
        }

        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();

        System.out.println(this.name_ + ":%%%% Exiting body() at time " +
                           GridSim.clock() );
    }

    /**
     * Gets a list of completed Gridlets
     * @return a list of completed Gridlets
     */
    public GridletList getGridletList() {
        return receiveList_;
    }

    /**
     * This method will show you how to create Gridlets
     * @param userID        owner ID of this Gridlet
     * @param numGridlet    total Gridlets to be created
     */
    private void createGridlet(int userID, int numGridlet)
    {
        int k = 0;
        int data = 5000;   // 5 MB of data

        for (int i = 0; i < numGridlet; i++)
        {
            // Creates a Gridlet
            Gridlet gl = new Gridlet(i, data, data, data);
            gl.setUserID(userID);
            this.list_.add(gl);

            k++;
        }
    }

    /**
     * Prints the Gridlet objects
     * @param detail    whether to print each Gridlet history or not
     */
    public void printGridletList(boolean detail)
    {
        LinkedList list = receiveList_;
        String name = name_;

        int size = list.size();
        Gridlet gridlet = null;

        String indent = "    ";
        System.out.println();
        System.out.println("============= OUTPUT for " + name + " ==========");
        System.out.println("Gridlet ID" + indent + "STATUS" + indent +
                "Resource ID" + indent + "Cost");

        // a loop to print the overall result
        int i = 0;
        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            System.out.print( gridlet.getGridletStatusString() );

            System.out.println( indent + indent + gridlet.getResourceID() +
                    indent + indent + gridlet.getProcessingCost() );
        }

        if (detail == true)
        {
            // a loop to print each Gridlet's history
            for (i = 0; i < size; i++)
            {
                gridlet = (Gridlet) list.get(i);
                System.out.println( gridlet.getGridletHistory() );

                System.out.print("Gridlet #" + gridlet.getGridletID() );
                System.out.println(", length = " + gridlet.getGridletLength()
                        + ", finished so far = " +
                        gridlet.getGridletFinishedSoFar() );
                System.out.println("======================================\n");
            }
        }
    }

} // end class

