package gridsim.example07;

/*
 * Author Anthony Sulistio
 * Date: December 2003
 * $Id: TestCase2.java,v 1.4 2004/10/31 05:31:54 anthony Exp $
 */

import java.util.*;
import gridsim.*;



/**
 * This Test Case is about creating Gridlets and submit them to GridResources.
 * Then cancel some of the Gridlets.
 */
class TestCase2 extends GridSim
{
    private int myId_;
    private String name_;
    private GridletList list_;
    private GridletList receiveList_;
    private double delay_;

    /**
     * Allocates a new TestCase2 object
     * @param name  the Entity name of this object
     * @param bandwidth     the communication speed
     * @param delay    simulation delay
     * @param totalGridlet    the number of Gridlets should be created
     * @param glLength  an array that contains various Gridlet's lengths
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or the entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#Init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    TestCase2(String name, double bandwidth, double delay, int totalGridlet,
             int[] glLength) throws Exception
    {
        super(name, bandwidth);
        this.name_ = name;
        this.delay_ = delay;

        this.receiveList_ = new GridletList();
        this.list_ = new GridletList();

        // Gets an ID for this entity
        this.myId_ = super.getEntityId(name);
        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + this.myId_);

        // Creates a list of Gridlets or Tasks for this grid user
        System.out.println(name + ":Creating "+ totalGridlet +" Gridlets");
        this.createGridlet(myId_, totalGridlet, glLength);
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

            // For even number of Gridlets, send with an acknowledgement
            if (i % 2 == 0)
            {
                success = super.gridletSubmit(gl, resourceID[index],0.0,true);
                System.out.println(name_ + ": Sending Gridlet #" +
                       gl.getGridletID() + " with status = " + success +
                       " to " + resourceName[index]);
            }

            // For odd number of Gridlets, send without an acknowledgement
            else
            {
                success = super.gridletSubmit(gl, resourceID[index],0.0, false);
                System.out.println(name_ + ": Sending Gridlet #" +
                       gl.getGridletID() + " with NO ACK so status = " +
                       success + " to " + resourceName[index]);
            }
        }

        //////////////////////////////////////////
        // CANCELING Gridlets

        // hold for few period -- 100 seconds
        super.gridSimHold(15);
        System.out.println("<<<<<<<<< pause for 15 >>>>>>>>>>>");

        // a loop that cancels an even number of Gridlet
        for (i = 0; i < list_.size(); i++)
        {
            if (i % 2 == 0)
            {
                gl = super.gridletCancel(i, myId_, resourceID[index], 0.0);
                System.out.print(name_ + ": Canceling Gridlet #" + i +
                           " at time = " + GridSim.clock() );

                if (gl == null) {
                    System.out.println(" result = NULL");
                }
                else  // if Cancel is successful, then add it into the list
                {
                    System.out.println(" result = NOT null");
                    receiveList_.add(gl);
                }
            }
        }

        ////////////////////////////////////////////////////////
        // RECEIVES Gridlets back

        // hold for few period - 1000 seconds since the Gridlets length are
        // quite huge for a small bandwidth
        super.gridSimHold(1000);
        System.out.println("<<<<<<<<< pause for 1000 >>>>>>>>>>>");

        // receives the gridlet back
        int size = list_.size() - receiveList_.size();
        for (i = 0; i < size; i++)
        {
            gl = (Gridlet) super.receiveEventObject();  // gets the Gridlet
            receiveList_.add(gl);   // add into the received list

            System.out.println(name_ + ": Receiving Gridlet #" +
                  gl.getGridletID() + " at time = " + GridSim.clock() );
        }

        System.out.println(this.name_ + ":%%%% Exiting body() at time " +
                           GridSim.clock() );

        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();

        // Prints the simulation output
        printGridletList(receiveList_, name_);
    }

    /**
     * This method will show you how to create Gridlets
     */
    private void createGridlet(int userID, int numGridlet, int[] data)
    {
        int k = 0;
        for (int i = 0; i < numGridlet; i++)
        {
            if (k == data.length) {
                k = 0;
            }

            // Creates a Gridlet
            Gridlet gl = new Gridlet(i, data[k], data[k], data[k]);
            gl.setUserID(userID);
            this.list_.add(gl);

            k++;
        }
    }

    /**
     * Prints the Gridlet objects
     */
    private void printGridletList(GridletList list, String name)
    {
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

        // a loop to print each Gridlet's history
        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.println( gridlet.getGridletHistory() );

            System.out.print("Gridlet #" + gridlet.getGridletID() );
            System.out.println(", length = " + gridlet.getGridletLength()
                    + ", finished so far = " +
                    gridlet.getGridletFinishedSoFar() );
            System.out.println("===========================================\n");
        }
    }

} // end class

