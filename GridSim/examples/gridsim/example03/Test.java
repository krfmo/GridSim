package gridsim.example03;

/*
 * Author Anthony Sulistio
 * Date: April 2003
 * Description: A simple program to demonstrate of how to use GridSim package.
 *              This example shows how two GridSim entities interact with each
 *              other.
 * $Id: Test.java,v 1.5 2005/09/16 07:02:15 anthony Exp $
 */

import java.util.*;
import gridsim.*;
import eduni.simjava.Sim_event;


/**
 * Test class establishes Input and Output entities. Then Test class listens
 * to the event simulation waiting to receive one Gridlet from the other
 * GridSim entity, in this case is Example3 class. Afterwards, this class
 * sends back the Gridlet to Example3 class.
 */
class Test extends GridSim
{
    /**
     * Allocates a new Test object
     * @param name  the Entity name
     * @param baud_rate  the communication speed
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or the entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#Init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    Test(String name, double baud_rate) throws Exception
    {
        // Don't need to create Input and Output entities if baud_rate is
        // known. GridSim will create them during the super() call.
        super(name, baud_rate);
        System.out.println("... Creating a new Test object");
    }

    /**
     * Processes one event at one time. Receives a Gridlet object from the
     * other GridSim entity. Modifies the Gridlet's status and sends it back
     * to the sender.
     */
    public void body()
    {
        int entityID;
        Sim_event ev = new Sim_event();
        Gridlet gridlet;

        // Gets one event at a time
        for ( sim_get_next(ev); ev.get_tag() != GridSimTags.END_OF_SIMULATION;
                sim_get_next(ev) )
        {
            // Gets the Gridlet object sent by Example3 class
            gridlet = (Gridlet) ev.get_data();

            // Change the Gridlet status, meaning that the Gridlet has been
            // received successfully
            try {
                gridlet.setGridletStatus(Gridlet.SUCCESS);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("... Inside Test.body() => Receiving Gridlet "+
                    gridlet.getGridletID() + " from Example3 object");

            // get the sender ID, i.e Example3 class
            entityID = ev.get_src();

            // sends back the modified Gridlet to the sender
            super.send(entityID, GridSimTags.SCHEDULE_NOW,
                       GridSimTags.GRIDLET_RETURN, gridlet);
        }

        // when simulation ends, terminate the Input and Output entities
        super.terminateIOEntities();
    }
}

