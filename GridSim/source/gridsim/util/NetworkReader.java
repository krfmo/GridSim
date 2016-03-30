/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia
 */

package gridsim.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import gridsim.net.FIFOScheduler;
import gridsim.net.FloodingRouter;
import gridsim.net.Link;
import gridsim.net.RIPRouter;
import gridsim.net.RateControlledRouter;
import gridsim.net.RateControlledScheduler;
import gridsim.net.Router;
import gridsim.net.SCFQScheduler;
import gridsim.net.SimpleLink;
import gridsim.net.flow.FlowLink;
import gridsim.net.flow.FlowRouter;

/**
 * This is an utility class, which parses a file and constructs the
 * network topology automatically. <br>
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
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public class NetworkReader
{
    private static final int RATE_ROUTER = 1;
    private static final int RIP_ROUTER = 2;
    private static final int FLOOD_ROUTER = 3;
    private static final int FLOW_ROUTER = 4;

    /**
     * Creates a network topology that uses a FIFO packet scheduler
     * @param filename  the name of the file containing the description of
     *                  the network topology
     * @return the list of Routers of the network or <tt>null</tt> if an error
     *         occurs
     * @see gridsim.net.FIFOScheduler
     */
    public static LinkedList createFIFO(String filename)
    {
        LinkedList routerList = null;
        try
        {
            FileReader fileReader = new FileReader(filename);
            BufferedReader buffer = new BufferedReader(fileReader);
            routerList = createNetworkFIFO(buffer);
        }
        catch (Exception exp)
        {
            System.out.println("NetworkReader: File not found.");
            routerList = null;
        }

        return routerList;
    }

    /**
     * Creates a network topology that uses a SCFQ packet scheduler
     * @param filename  the name of the file containing the description of
     *                  the network topology
     * @param weight    a linear array of the weights to be assigned to
     *                  different classes of traffic.
     * @return the list of Routers of the network or <tt>null</tt> if an error
     *         occurs
     * @see gridsim.net.SCFQScheduler
     */
    public static LinkedList createSCFQ(String filename, double[] weight)
    {
        if (weight == null) {
            return null;
        }

        LinkedList routerList = null;
        try
        {
            FileReader fileReader = new FileReader(filename);
            BufferedReader buffer = new BufferedReader(fileReader);
            routerList = createNetworkSCFQ(buffer, weight);
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
     * Creates a network topology that uses a Rate controlled packet scheduler
     * @param filename  the name of the file containing the description of
     *                  the network topology
     * @param percentage  a linear array of bandwidth percentage to be assigned
     *                    to different classes of traffic.
     * @return the list of Routers of the network or <tt>null</tt> if an error
     *         occurs
     * @see gridsim.net.RateControlledScheduler
     */
    public static LinkedList createRate(String filename, double[] percentage)
    {
        if (percentage == null) {
            return null;
        }

        LinkedList routerList = null;
        try
        {
            // check whether the total percentage is greater than 100%
            double total = 0;
            int MAX_LIMIT = 100;
            for (int i = 0; i < percentage.length; i++)
            {
                total += percentage[i];
                if (total > MAX_LIMIT)
                {
                    System.out.println("NetworkReader: total percentage = " +
                                        total + ", which is > 100%");
                    return null;
                }
            }

            FileReader fileReader = new FileReader(filename);
            BufferedReader buffer = new BufferedReader(fileReader);
            routerList = createNetworkRate(buffer,percentage.length,percentage);
        }
        catch (Exception exp)
        {
            System.out.println("NetworkReader: File not found or " +
                               "percentage[] is null.");
            routerList = null;
        }

        return routerList;
    }

    /**
     * Gets a Router object from the list
     * @param name          a router name
     * @param routerList    a list containing the Router objects
     * @return a Router object or <tt>null</tt> if not found
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
     * @return a list of Router objects or <tt>null</tt> if an error occurs
     */
    private static LinkedList createRouter(BufferedReader buf,
                                           int routerType) throws Exception
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
                flag = str.nextToken();     // get the optional entry
                if (flag.equalsIgnoreCase("true")) {
                    log = true;
                }
            }

            // create a specific Router object
            if (routerType == RATE_ROUTER) {
                router = new RateControlledRouter(name, log);
            }
            else if (routerType == RIP_ROUTER) {
                router = new RIPRouter(name, log);
            }
            else if (routerType == FLOOD_ROUTER) {
                router = new FloodingRouter(name, log);
            }
            else if (routerType == FLOW_ROUTER) {
                router = new FlowRouter(name, log);
            }

            routerList.add(router);     // add the router into the list
        }

        return routerList;
    }

    /**
     * Creates a network topology from a given buffered reader
     * @param buf   a Buffered Reader object
     * @return a list of Router objects or <tt>null</tt> if an error occurs
     */
    private static LinkedList createNetworkFIFO(BufferedReader buf)
                                                throws Exception
    {
        if (buf == null) {
            return null;
        }

        // create the Router objects first
        LinkedList routerList = createRouter(buf, NetworkReader.RIP_ROUTER);

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
                System.out.println("NetworkReader.createNetworkFIFO(): " +
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

            FIFOScheduler r1Sched = new FIFOScheduler(r1.get_name()
                    + "_to_" + r2.get_name());

            FIFOScheduler r2Sched = new FIFOScheduler(r2.get_name()
                    + "_to_" + r1.get_name());

            r1.attachRouter(r2, tempLink, r1Sched, r2Sched);
        }

        return routerList;
    }

    /**
     * Creates a network topology from a given buffered reader
     * @param buf   a Buffered Reader object
     * @param weight    a linear array of the weights to be assigned to
     *                  different classes of traffic.
     * @return a list of Router objects or <tt>null</tt> if an error occurs
     */
    private static LinkedList createNetworkSCFQ(BufferedReader buf,
                                    double[] weight) throws Exception
    {
        if (buf == null) {
            return null;
        }

        // create the Router objects first
        LinkedList routerList = createRouter(buf, NetworkReader.RIP_ROUTER);

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

            SCFQScheduler r1Sched = new SCFQScheduler(r1.get_name()
                    + "_to_" + r2.get_name());

            SCFQScheduler r2Sched = new SCFQScheduler(r2.get_name()
                    + "_to_" + r1.get_name());

            r1Sched.setWeights(weight);
            r2Sched.setWeights(weight);
            r1.attachRouter(r2, tempLink, r1Sched, r2Sched);
        }
        return routerList;
    }

    /**
     * Creates a network topology from a given buffered reader
     * @param buf   a Buffered Reader object
     * @param numClass  the number of classes
     * @param percentage  a linear array of bandwidth percentage to be assigned
     *                    to different classes of traffic.
     * @return a list of Router objects or <tt>null</tt> if an error occurs
     */
    private static LinkedList createNetworkRate(BufferedReader buf,
                        int numClass, double[] percentage) throws Exception
    {
        if (buf == null) {
            return null;
        }

        // create the Router objects first
        LinkedList routerList = createRouter(buf, NetworkReader.RATE_ROUTER);

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
                System.out.println("NetworkReader.createNetworkRate(): " +
                    "Warning - unable to connect both "+name1+" and "+name2);
                continue;
            }

            // get baud rate of the link
            String baud = str.nextToken();       // bandwidth (Gbps)
            String propDelay = str.nextToken();  // latency (in millisec)
            String mtu = str.nextToken();        // link MTU (in byte)
            double baud_rate = Double.parseDouble(baud) * GB;

            tempLink = new SimpleLink(r1.get_name() + "_" + r2.get_name(),
                                baud_rate, Double.parseDouble(propDelay),
                                Integer.parseInt(mtu));

            RateControlledScheduler r1Sched = new RateControlledScheduler(
                r1.get_name() + "_to_" + r2.get_name(), numClass);

            RateControlledScheduler r2Sched = new RateControlledScheduler(
                r2.get_name() + "_to_" + r1.get_name(), numClass);

            // calculates the exact rate based on the given percentage
            double[] rate = new double[numClass];
            for (int k = 0; k < numClass; k++) {
                double value = baud_rate * percentage[k] / 100;
                rate[k] = value;
            }

            r1Sched.setRates(rate);
            r2Sched.setRates(rate);
            r1.attachRouter(r2, tempLink, r1Sched, r2Sched);
        }

        return routerList;
    }

    /**
     * Creates a network topology that uses the flow network functionality.
     * @param filename  the name of the file containing the description of
     *                  the network topology
     * @return the list of Routers of the network or <tt>null</tt> if an error
     *         occurs
     * @see gridsim.net.flow.FlowRouter
     * @see gridsim.net.flow.FlowLink
     */
    public static LinkedList createFlow(String filename)
    {
        LinkedList routerList = null;
        try
        {
            FileReader fileReader = new FileReader(filename);
            BufferedReader buffer = new BufferedReader(fileReader);
            routerList = createNetworkFlow(buffer);
        }
        catch (Exception exp)
        {
            System.out.println("NetworkReader: File not found.");
            routerList = null;
        }

        return routerList;
    }

    /**
     * Creates a network topology from a given buffered reader
     * @param buf   a Buffered Reader object
     * @return a list of Router objects or <tt>null</tt> if an error occurs
     */
    private static LinkedList createNetworkFlow(BufferedReader buf)
                                                throws Exception
    {
        if (buf == null) {
            return null;
        }

        // create the Router objects first
        LinkedList routerList = createRouter(buf, NetworkReader.FLOW_ROUTER);

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
                System.out.println("NetworkReader.createNetworkFlow(): " +
                    "Warning - unable to connect both "+name1+" and "+name2);
                continue;
            }

            // get baud rate of the link
            String baud = str.nextToken();       // bandwidth (Gbps)
            String propDelay = str.nextToken();  // latency (in millisec)
            String mtu = str.nextToken();        // link MTU (in byte)

            tempLink = new FlowLink(r1.get_name() + "_" + r2.get_name(),
                    Double.parseDouble(baud) * GB,
                    Double.parseDouble(propDelay), Integer.parseInt(mtu));

            FIFOScheduler r1Sched = new FIFOScheduler(r1.get_name()
                    + "_to_" + r2.get_name());

            FIFOScheduler r2Sched = new FIFOScheduler(r2.get_name()
                    + "_to_" + r1.get_name());

            r1.attachRouter(r2, tempLink, r1Sched, r2Sched);
        }

        return routerList;
    }

} 

