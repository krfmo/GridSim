package gridsim.regionalGIS;

/*
 * Author: Anthony Sulistio
 * Date: March 2005
 * Description:
 *      This class shows how a Grid user can communicate to its designated
 *      regional GIS entity. Sending jobs or Gridlets are not important in this
 *      example, hence, they are excluded.
 */

import java.util.*;
import gridsim.*;
import gridsim.net.*;

/**
 * To communicate with a designated regional GIS, a user must extend from
 * the GridUser entity. This allows the user to use the available methods.
 */
public class NetUserGIS extends GridUser
{
    private int myId_;          // my entity ID
    private String name_;       // my entity name


    /**
     * Creates a new user that is part of the network topology
     */
    NetUserGIS(String name, double baud_rate, double delay, int MTU)
               throws Exception
    {
        super(name, new SimpleLink(name + "_link", baud_rate, delay, MTU));
        this.name_ = name;

        // Gets an ID for this entity
        this.myId_ = super.getEntityId(name);
        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + this.myId_);
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        ///////////////////////////
        // wait for a little while
        // to give more time for GridResource entities to register their
        // services to a regional GIS (GridInformationService) entity.
        super.gridSimHold(30.0);    // wait for time in seconds
        Object[] obj = null;

        // get a list of regional GIS entities
        System.out.println("///////////////////////////////////");
        obj = super.getRegionalGISList();
        printArray("Regional GIS", obj);    // print the result

        // query about local resources within the region
        System.out.println("///////////////////////////////////");
        obj = super.getLocalResourceList();
        printArray("local Resource", obj);  // print the result

        // query about local resources that support advance reservation
        // within the region
        System.out.println("///////////////////////////////////");
        obj = super.getLocalResourceARList();
        printArray("local AR Resource", obj);  // print the result

        // query about global resources from outside the region
        System.out.println("///////////////////////////////////");
        obj = super.getGlobalResourceList();
        printArray("Global Resource", obj);   // print the result

        // query about global resources that support advance reservation
        // from outside the region
        System.out.println("///////////////////////////////////");
        obj = super.getGlobalResourceARList();
        printArray("Global AR Resource", obj);  // print the result

        ////////////////////////////////////////////////////////
        // Try to ping RegionalGIS
        int size = 100;   // packet size
        InfoPacket pkt = null;

        String gisName = super.getRegionalGISName();
        System.out.println("///////////////////////////////////");
        System.out.println(super.get_name() + ": trying to ping " + gisName);
        pkt = super.pingBlockingCall(super.getRegionalGISName(),size,0.0,0);

        // print the result
        System.out.println("\n-------- " + name_ + " ----------------");
        System.out.println(pkt);
        System.out.println("-------- " + name_ + " ----------------\n");

        ////////////////////////////////////////////////////////
        // signal the end of a simulation for this user entity
        super.finishSimulation();
        System.out.println(this.name_ + ":%%%% Exiting body() at time " +
                           GridSim.clock() );
    }

    /**
     * Prints the result
     */
    private void printArray(String msg, Object[] globalArray)
    {
        // if array is empty
        if (globalArray == null)
        {
            System.out.println(super.get_name()+ ": number of "+ msg+ " = 0.");
            return;
        }

        System.out.println(super.get_name() + ": number of " + msg + " = " +
                globalArray.length);

        for (int i = 0; i < globalArray.length; i++)
        {
            Integer num = (Integer) globalArray[i];
            System.out.println(super.get_name() + ": receiving info about " +
                msg + ", name = " + GridSim.getEntityName(num.intValue()) +
                " (id: " + num + ")");
        }
        System.out.println();
    }

} // end class



