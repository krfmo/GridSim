/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 *
 * Based on NetworkReader class.
 * Things added or modifyed:
 *    - createNetworkSCFQ(...)
 *    - Use finiteBufferSCFQScheduler instead of SCFQScheduler
 */

package gridsim.net.fnb;

import gridsim.net.fnb.*;
import gridsim.net.*;
import java.util.*;
import java.io.*;
import gridsim.*;


/**
 * This is an utility class, which parses a file and constructs the
 * network topology automatically, using the SCFQ packet scheduler
 * for network buffers ({@link gridsim.net.fnb.FnbSCFQScheduler}). 
 * Note that this class is based on the {@link gridsim.util.NetworkReader} class.
 * <p>
 * The file that defines the network has the following form: <br> <br>
 * <tt>
 * # specify the number of routers<br>
 * number_of_routers<br>
 * <br>
 * # specify the name of each router and (optional) logging facility<br>
 * router_name1 [true/false]<br>
 * router_name2 [true/false]<br>
 * router_name3 [true/false]<br>
 * ... // other router names<br>
 * <br>
 * # linking two routers. NOTE: the router name is case sensitive!<br>
 * router_name1 router_name2 baud_rate(GB/s) prop_delay(ms) mtu(byte) <br>
 * router_name1 router_name3 baud_rate(GB/s) prop_delay(ms) mtu(byte) <br>
 * ... // linking other routers<br>
 * </tt>
 * <br>
 * NOTE: <tt>[]</tt> means an optional parameter for logging activities
 * inside a router.
 * If it is not given, then by default the value is false.
 *
 * @see gridsim.net.fnb.FnbSCFQScheduler
 * @since GridSim Toolkit 4.2
 * @author  Agustin Caminero, Universidad de Castilla-La Mancha (UCLM) (Spain). 
 */
public class FnbNetworkReader
{
    /**
     * Creates a network topology that uses a SCFQ packet scheduler
     * @param filename  the name of the file containing the description of
     *                  the network topology
     * @param weight    a linear array of the weights to be assigned to
     *                  different classes of traffic.
     * @param max_buf_size maximum buffer size for routers
     * @param drop_alg the algorithm used to drop packets at routers
     * @param min_th minimum threshold for RED
     * @param max_th maximum threshold for RED
     * @param max_p  maximum drop probability for RED
     * @param queue_weight queue weight for RED
     * @param stats whether we want to store stats or not
     * @return the list of finiteBufferRouters of the network or 
     *         <tt>null</tt> if an error occurs
     * @see gridsim.net.fnb.FnbSCFQScheduler
     */
    public static LinkedList createSCFQ(String filename, double[] weight,
                                        int max_buf_size, int drop_alg,
                                        int min_th, int max_th, double max_p,
                                        double queue_weight, boolean stats)
    {
        if (weight == null)
        {
            return null;
        }

        LinkedList routerList = null;
        try
        {
            FileReader fileReader = new FileReader(filename);
            BufferedReader buffer = new BufferedReader(fileReader);

            routerList = createNetworkSCFQ(buffer, weight, max_buf_size,
                                           drop_alg, min_th, max_th, max_p,
                                           queue_weight, stats);
        }
        catch (Exception exp)
        {
            System.out.println("NetworkReader: File not found or " +
                               "weight[] is null.");
            routerList = null;
        }

        return routerList;
    }



    /**
     * Gets a {@link gridsim.net.Router} object from the list
     * @param name          a router name
     * @param routerList    a list containing the finiteBufferRouter objects
     * @return a {@link gridsim.net.Router} object or 
     *         <tt>null</tt> if not found
     */
    public static Router getRouter(String name, LinkedList routerList)
    {
        if (name == null || routerList == null || name.length() == 0) {
            return null;
        }

        Router router = null;
        try
        {
            Iterator it = routerList.iterator();
            while ( it.hasNext() )
            {
                router = (Router) it.next();
                if (router.get_name().equals(name)) {
                    break;
                }
                else {
                    router = null;
                }
            }
        }
        catch (Exception e) {
            router = null;
        }

        return router;
    }

    /**
     * Creates a number of routers from a given buffered reader
     * @param buf   a Buffered Reader object
     * @param rate  a flag denotes the type of Router will be using
     * @param stats true if we want to store statistics
     * @return a list of {@link gridsim.net.fnb.FnbRIPRouter} objects or 
     *         <tt>null</tt> if an error occurs
     */
    private static LinkedList createRouter(BufferedReader buf, boolean rate, 
                                           boolean stats) throws Exception
    {
        String line = null;
        StringTokenizer str = null;
        int num_router = -1;    // num of routers

        // parse each line
        while ((line = buf.readLine()) != null)
        {
            str = new StringTokenizer(line);
            if (!str.hasMoreTokens()) {     // ignore newlines
                continue;
            }

            String num = str.nextToken();           // get next token
            if (num.startsWith("#")) {      // ignore comments
                continue;
            }

            // get the num of router
            if (num_router == -1)
            {
                num_router = (new Integer(num)).intValue();
                break;
            }
        }

        LinkedList routerList = new LinkedList();
        Router router = null;   // a Router object
        String name = null;     // router name
        String flag = null;     // a flag to denote logging router or not
        boolean log = false;

        // then for each line, get the router name
        for (int i = 1; i <= num_router; i++)
        {
            log = false;
            line = buf.readLine();
            str = new StringTokenizer(line);
            if (!str.hasMoreTokens())       // ignore newlines
            {
                i--;
                continue;
            }

            name = str.nextToken();                 // get next token
            if (name.startsWith("#"))       // ignore comments
            {
                i--;
                continue;
            }

            if (str.hasMoreTokens())
            {
                flag = str.nextToken(); // get the optional entry
                if (flag.equalsIgnoreCase("true"))
                {
                    log = true;
                }
            }

            // The name of a roputer is "Router10", so the my_id is 10
            int endRouter = name.lastIndexOf('r');
            String my_id_str = name.substring(endRouter + 1);

            int my_id = (Integer.decode(my_id_str)).intValue();

            // create a specific Router object
            if (rate)
            {
                System.out.println(" ****** FnbNetworkReader.createRouter(): " +
                    "RateControlledScheduler is not available for the finite "
                    + "network buffers functionality");

            }
            else {
                router = new FnbRIPRouter(name, log, my_id, stats);
            }
            routerList.add(router);     // add the router into the list
        }

        return routerList;
    }

    /**
     * Creates a network topology from a given buffered reader
     * @param buf   a Buffered Reader object
     * @param weight    a linear array of the weights to be assigned to
     *                  different classes of traffic.
     * @param max_buf_size the maximum size for network buffers
     * @param drop_alg the algorithm used to drop packets at routers
     * @param min_th minimum threshold for RED
     * @param max_th maximum threshold for RED
     * @param max_p  maximum drop probability for RED
     * @param queue_weight queue weight for RED
     * @param stats true if we want to store statistics
     * @return a list of {@link gridsim.net.fnb.FnbRIPRouter} objects or 
     *         <tt>null</tt> if an error occurs
     */
    private static LinkedList createNetworkSCFQ(BufferedReader buf, double[] weight,
                                    int max_buf_size, int drop_alg, int min_th, 
                                    int max_th, double max_p, double queue_weight, 
                                    boolean stats) throws Exception
    {
        if (buf == null)
        {
            return null;
        }

        // create the Router objects first
        LinkedList routerList = createRouter(buf, false, stats);

        int GB = 1000000000;  // 1 GB in bits
        String line;
        String name1, name2;
        StringTokenizer str = null;
        Router r1, r2;
        Link tempLink = null;

        // creating the linking between two routers
        while ((line = buf.readLine()) != null)
        {
            str = new StringTokenizer(line);
            if (!str.hasMoreTokens()) {     // ignore newlines
                continue;
            }

            // parse the name of the connected routers
            name1 = str.nextToken();    // router name
            if (name1.startsWith("#")) {    // ignore comments
                continue;
            }

            name2 = str.nextToken();    // router name
            r1 = getRouter(name1, routerList);
            r2 = getRouter(name2, routerList);

            if (r1 == null || r2 == null)
            {
                System.out.println("NetworkReader.createNetworkSCFQ(): " +
                    "Warning - unable to connect both "+name1+" and "+name2);
                continue;
            }

            // get baud rate of the link
            String baud = str.nextToken();       // bandwidth (Gbps)
            String propDelay = str.nextToken();  // latency (in millisec)
            String mtu = str.nextToken();        // link MTU (in byte)

            tempLink = new SimpleLink(r1.get_name() + "_" + r2.get_name(),
                    Double.parseDouble(baud) * GB,
                    Double.parseDouble(propDelay), Integer.parseInt(mtu));


            FnbSCFQScheduler r1Sched;

            FnbSCFQScheduler r2Sched;

            if (GridSimTags.FNB_ARED == drop_alg)
            {
                r1Sched = (FnbSCFQScheduler)new ARED(r1.get_name()
                                                    + "_to_" + r2.get_name(),
                                                    tempLink.getBaudRate(), max_p,
                                                    max_buf_size, queue_weight, stats);

                r2Sched = (FnbSCFQScheduler)new ARED(r2.get_name()
                                                    + "_to_" + r1.get_name(),
                                                    tempLink.getBaudRate(), max_p,
                                                    max_buf_size,  queue_weight, stats);
            }
            else if (GridSimTags.FNB_RED == drop_alg)
            {
                r1Sched = (FnbSCFQScheduler)new RED(r1.get_name()
                                                    + "_to_" + r2.get_name(),
                                                    tempLink.getBaudRate(),
                                                    max_buf_size, min_th, max_th, max_p,
                                                    queue_weight, stats);

                r2Sched = (FnbSCFQScheduler)new RED(r2.get_name()
                                                    + "_to_" + r1.get_name(),
                                                    tempLink.getBaudRate(),
                                                    max_buf_size, min_th, max_th, max_p,
                                                    queue_weight, stats);
            }
            else //if (GridSimTags.FNB_FIFO == drop_alg)
            {
                r1Sched = (FnbSCFQScheduler)new FIFO(r1.get_name()
                                                     + "_to_" + r2.get_name(),
                                                      tempLink.getBaudRate(),
                                                     max_buf_size,
                                                     queue_weight, stats);

                r2Sched = (FnbSCFQScheduler)new FIFO(r2.get_name()
                                                     + "_to_" + r1.get_name(),
                                                      tempLink.getBaudRate(),
                                                     max_buf_size,
                                                     queue_weight, stats);
            }

            r1Sched.setWeights(weight);
            r2Sched.setWeights(weight);
            r1.attachRouter(r2, tempLink, r1Sched, r2Sched);
        }
        
        return routerList;
    }

} 

