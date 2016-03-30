package network.example01;

/*
 * Author: Anthony Sulistio
 * Date: November 2004
 * Description: A simple program to demonstrate of how to use GridSim 
 *              network extension package.
 *              This example shows how to create two GridSim entities and
 *              connect them via a link. NetUser entity sends messages to
 *              Test entity and Test entity sends back these messages.
 */
 
import gridsim.*;
import gridsim.net.*;
import java.util.*;


/**
 * Test Driver class for this example
 */
public class NetEx01
{
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting network example ...");

        try
        {
            //////////////////////////////////////////
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            int num_user = 2;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace GridSim events

            // Initialize the GridSim package without any statistical
            // functionalities. Hence, no GridSim_stat.txt file is created.
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Second step: Creates a physical link

            double baud_rate = 100; // bits/sec
            double propDelay = 10;   // propagation delay in millisecond
            int mtu = 50;          // max. transmission unit in byte

            Link link = new SimpleLink("link", baud_rate, propDelay, mtu);

            // OR ...
            // use a default value
            // Link link = new SimpleLink("link");

            //////////////////////////////////////////
            // Third step: Creates one or more entities.
            // This can be users or resources. In this example,
            // we create user's entities only.
            
            String sender = "user";
            String receipient = "test";
            
            // this entity is the sender
            NetUser user = new NetUser(sender, receipient, link);

            // this entity is the receipient
            Test test = new Test(receipient, sender, link);

            //////////////////////////////////////////
            // Fourth step: Builds the network topology among entities.

            // In this example, the topology is:
            // NetUser -- link -- Test

            link.attach(user, test);

            //////////////////////////////////////////
            // Final step: Starts the simulation
            GridSim.startGridSimulation();

            System.out.println("\nFinish network example ...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }

} // end class

