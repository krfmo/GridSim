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

import java.util.*;



/**
 * Test Driver class for this example
 */
public class FlowNetEx01
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
            int num_user = 4;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace GridSim events

            // Initialize the GridSim package without any statistical
            // functionalities. Hence, no GridSim_stat.txt file is created.
            System.out.println("Initializing GridSim package");
            
            // It is essential to set the network type before calling GridSim.init()
            GridSim.initNetworkType(GridSimTags.NET_FLOW_LEVEL);
            GridSim.init(num_user, calendar, trace_flag);
            
            // In this example, the topology is:
            // user(s) --10Mb/s-- r1 --1.5Mb/s-- r2 --10Mb/s-- GridResource(s)

            // create the routers.
            // If trace_flag is set to "true", then this experiment will create
            // the following files (apart from sim_trace and sim_report):
            // - router1_report.csv
            // - router2_report.csv
            Router r1 = new FlowRouter("router1", trace_flag);   // router 1
            Router r2 = new FlowRouter("router2", trace_flag);   // router 2
            
            String sender1 = "user1";
            String receipient1 = "test1";
            String sender2 = "user2";
            String receipient2 = "test2";
            
            // these entities are the senders
            FlowNetUser user1 = new FlowNetUser(sender1, receipient2, 5.0);
            FlowNetUser user2 = new FlowNetUser(sender2, receipient1, 20.0);
            
            // these entities are the receipients
            FlowTest test1 = new FlowTest(receipient1, sender2);
            FlowTest test2 = new FlowTest(receipient2, sender1);
            
            // The schedulers are redundent and will be stripped out soon
            FIFOScheduler userSched1 = new FIFOScheduler("NetUserSched_0");
            r1.attachHost(user1, userSched1);
            
            FIFOScheduler userSched2 = new FIFOScheduler("NetUserSched_1");
            r1.attachHost(user2, userSched2);
            
            FIFOScheduler testSched1 = new FIFOScheduler("FlowTestSched_0");
            r2.attachHost(test1, testSched1);
            
            FIFOScheduler testSched2 = new FIFOScheduler("FlowTestSched_1");
            r2.attachHost(test2, testSched2);
            
            //////////////////////////////////////////
            // Second step: Creates a physical link
            double baud_rate = 1572864; // bits/sec (baud) [1.5Mb/s]
            double propDelay = 300;   // propagation delay in millisecond
            int mtu = Integer.MAX_VALUE;;          // max. transmission unit in byte
            
            Link link = new FlowLink("r1_r2_link", baud_rate, propDelay, mtu);
            FIFOScheduler r1Sched = new FIFOScheduler("r1_Sched");
            FIFOScheduler r2Sched = new FIFOScheduler("r2_Sched");
            
            r1.attachRouter(r2, link, r1Sched, r2Sched);

            //////////////////////////////////////////
            // Final step: Starts the simulation
            GridSim.startGridSimulation();

            System.out.println("\nFinish network example ...");
        }
        catch (Exception e)
        {
        	
            e.printStackTrace();
        	System.err.print(e.toString());
            System.out.println("Unwanted errors happen");
        }
    }

} // end class

