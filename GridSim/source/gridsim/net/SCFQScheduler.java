/*
 * ** Network and Service Differentiation Extensions to GridSim 3.0 **
 *
 * Gokul Poduval & Chen-Khong Tham
 * Computer Communication Networks (CCN) Lab
 * Dept of Electrical & Computer Engineering
 * National University of Singapore
 * August 2004
 *
 * License: GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2004, The University of Melbourne, Australia and National
 * University of Singapore
 * SCFQ.java - Implements a Self Clocked Fair Queuing scheduler
 *
 */

package gridsim.net;

import eduni.simjava.*;
import gridsim.*;
import java.util.*;


/**
 * SCFQScheduler implements a Self Clocked Fair Queueing Scheduler. A SCFQ is a
 * variation of Weighted Fair Queueing (WFQ), which is easier to implement than
 * WFQ because it does not need to compute round numbers at every iteration.
 * For more details refer to <b>S. R. Golestani's</b> INFOCOM '94 paper
 * <i>A self-clocked fair queueing scheme for broadband applications</i>.
 * <p>
 * A SCFQ scheduler can provide differentiated service to traffic by changing
 * the weights associated with a certain class of traffic. The higher the weight
 * of a class of traffic, the better treatment it receives. In this class, you
 * can set the weights by calling setWeights() with a linear array of weights.
 * Traffic that is class <tt>0 (default)</tt>, are assigned the first element
 * of the array as its weight.
 * <p>
 * For example: <br>
 * String userName = { "User_0", "User_1", User_2" };  // list of user names<br>
 * int[] trafficClass = { 0, 1, 2 };  // a list of class for each user<br>
 * int[] weights = { 1, 2, 3 };       // a list of weights for each class<br>
 * <br>
 * From the above example, <i>User_0</i> has a traffic class of 0 (low
 * priority), whereas <i>User_2</i> has a traffic class of 2 (high priority)
 * judging from their respective weights.
 *
 * @invariant $none
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 */
public class SCFQScheduler implements PacketScheduler
{
    private String name_;       // this scheduler name
	private double baudRate_;   // baud rate of this scheduler
    private Vector pktList;     // Sorted List of all Packets
    private Vector timeList;    // Sorted list of finish times
    private double[] weights;   // weights for different ToS packets
    private double CF ;         // current finish number
    private Hashtable flowTable;


    /**
     * Creates a new SCFQ packet scheduler with the specified name and baud rate
     * (in bits/s). The name can be useful for debugging purposes, but serves
     * no functional purposes.
     *
     * @param name       Name of this scheduler
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @throws ParameterException This happens when the name is null or
     *                   the baud rate <= 0
     * @pre name != null
     * @pre baudRate > 0
     * @post $none
     */
    public SCFQScheduler(String name, double baudRate)
                         throws ParameterException
    {
        if (name == null) {
            throw new ParameterException("SCFQScheduler(): Name is null.");
        }

        if (baudRate <= 0) {
		    throw new ParameterException("SCFQScheduler(): Baudrate <= 0.");
        }

        name_ = name;
        baudRate_ = baudRate;
        init();
    }

    /**
     * Creates a new SCFQ packet scheduler with the specified baud rate
     * (bits/s). The name is set to a generic name: <b>"SCFQScheduler"</b>.
     *
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @throws ParameterException This happens when the baud rate <= 0
     * @pre baudRate > 0
     * @post $none
     */
    public SCFQScheduler(double baudRate) throws ParameterException
    {
        if (baudRate <= 0) {
		    throw new ParameterException("SCFQScheduler(): Baudrate <= 0.");
        }

        name_ = "SCFQScheduler";
        baudRate_ = baudRate;
        init();
    }

    /**
     * Creates a new SCFQ packet scheduler with the specified name.
     * The baud rate is left at 0, and should be set with
     * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
     * before the simulation starts.
     *
     * @param name Name of this scheduler
     * @throws ParameterException This happens when the name is null
     * @see gridsim.net.PacketScheduler#setBaudRate(double)
     * @pre name != null
     * @post $none
     */
    public SCFQScheduler(String name) throws ParameterException
    {
        if (name == null) {
            throw new ParameterException("SCFQScheduler(): Name is null.");
        }

        name_ = name;
        baudRate_ = 0;
        init();
    }

    /**
     * Creates a new packet scheduler with the name <b>"SCFQScheduler"</b>.
     * The baud rate is left at 0, and should be set with
     * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
     * before the simulation starts.
     * @throws ParameterException This happens when the name is null
     * @see gridsim.net.PacketScheduler#setBaudRate(double)
     * @pre $none
     * @post $none
     */
    public SCFQScheduler() throws ParameterException
    {
        name_ = "SCFQScheduler";
        baudRate_ = 0;
        init();
    }

    /**
     * Initialises all the attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        flowTable = new Hashtable();
        pktList = new Vector();
        timeList = new Vector();
        weights = null;
        CF = 0;
    }

    /**
     * This method allows you to set different weights for different types of
     * traffic. Traffic of class <tt>n</tt> are assigned a weight of
     * <tt>weights[n]</tt>. The higher the weight of a class, the better the
     * service it receives. <br>
     * <b>NOTE</b>: Do not set a weight to be <tt>0</tt> or a negative number.
     *
     * @param weights a linear array of the weights to be assigned to different
     *                classes of traffic.
     * @return <tt>true</tt> if it is successful, <tt>false</tt>otherwise
     * @pre weights != null
     * @post $none
     */
    public boolean setWeights(double[] weights)
    {
        if (weights == null) {
            return false;
        }

        // check whether the value of weights are correct
        for (int i = 0; i < weights.length; i++)
        {
            if (weights[i] <= 0)
            {
                System.out.println(name_ +
                    ".setWeights(): Error - weight must be a positive number.");
                return false;
            }
        }

        this.weights = weights;
        return true;
    }

    /**
     * Puts a packet into the queue
     *
     * @param pnp    A Packet to be enqued by this scheduler.
     * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
     * @pre pnp != null
     * @post $none
     */
    public synchronized boolean enque(Packet pnp)
    {
        int srcID = pnp.getSrcID();     // source entity id
        int destID = pnp.getDestID();   // destination entity id
        int type = pnp.getNetServiceType();     // packet service type

        Double nextTime = (Double) flowTable.get("" + srcID + destID + type);
        if (nextTime == null)
        {
            nextTime = new Double(CF);
            flowTable.put("" + srcID + destID + type, nextTime);
        }

        double pktTime = calculateFinishTime( pnp, nextTime.doubleValue() );
        flowTable.put("" + srcID + destID + type, new Double(pktTime) );
        insert(pnp, pktTime);   // Sort the queue list
        return true;
    }

    /**
     * Calculates the finish time of a particular packet
     * @param np  a network packet
     * @param nextTime  the next available time
     * @pre np != null
     * @pre nextTime >= 0
     * @post $none
     */
    private double calculateFinishTime(Packet np, double nextTime)
    {
        double time = 0;
        double ratio = 0;

        try
        {
            int type = np.getNetServiceType();
            if (type >= weights.length)   // out of bound array error checking
            {
                System.out.println(name_ +".calculateFinishTime(): Warning - " +
                    " packet class = " + type + ", weight.length = " +
                    weights.length);
                type = 0;
            }

            ratio = np.getSize() / weights[type];
        }
        catch(Exception e) {
            ratio = 1;  // default ratio if an error occurs
        }

        if (nextTime > CF) {
                time = nextTime + ratio;
        }
        else {
            time = CF + ratio;
        }

        return time;
    }

    /**
     * This function inserts a WFQClass into the sorted
     * queue list (using binary search)
     *
     * @param np    a network packet
     * @param time  simulation time
     * @pre np != null
     * @pre time >= 0
     * @post $none
     */
    private synchronized void insert(Packet np, double time)
    {
        if ( timeList.isEmpty() )
        {
            timeList.add( new Double(time) );
            pktList.add(np);
            return;
        }

        for (int i = 0; i < timeList.size(); i++)
        {
            double next = ( (Double) timeList.get(i) ).doubleValue();
            if (next > time)
            {
                timeList.add(i, new Double(time));
                pktList.add(i, np);
                return;
            }
        }

        // add to end
        timeList.add( new Double(time) );
        pktList.add(np);
        return;
    }

    /**
     * The method deque() has to decide which queue is to be
     * served next. In the original WFQ algorithm, this is always the
     * packet with lowest finish time. We also need to update the CF
     * (current finish no.) to that of the packet being served.
     *
     * @return the packet to be sent out
     * @pre $none
     * @post $none
     */
    public synchronized Packet deque()
    {
        Packet p = null;
        if (pktList.size() > 0 && timeList.size() > 0)
        {
            p = (Packet) pktList.remove(0);
            CF = ( (Double) timeList.remove(0) ).doubleValue();
        }

        return p;
    }

    /**
     * Determines whether the scheduler is currently keeping any packets in
     * its queue(s).
     *
     * @return <tt>true</tt> if no packets are enqueued, <tt>false</tt>
     *         otherwise
     * @pre $none
     * @post $none
     */
    public synchronized boolean isEmpty() {
        return pktList.isEmpty();
    }

    /**
     * Determines the number of packets that are currently enqueued in this
     * scheduler.
     *
     * @return the number of packets enqueud by this scheduler.
     * @pre $none
     * @post $none
     */
    public synchronized int size() {
        return pktList.size();
    }

    /**
     * Gets the ID of this scheduler.
     * @return the ID of this scheduler or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public int getSchedID()
    {
        System.out.println(name_ + ".getID(): No ID is set for this object.");
        return -1;
    }

    /**
     * Gets the name of this scheduler.
     * @return the name of this scheduler
     * @pre $none
     * @post $none
     */
    public String getSchedName() {
        return name_;
    }

    /**
     * Sets the baud rate that this scheduler will be sending packets at.
     * @param rate the baud rate of this scheduler (in bits/s)
     * @pre rate > 0
     * @post $none
     */
    public boolean setBaudRate(double rate)
    {
        if (rate <= 0.0) {
            return false;
        }
        baudRate_ = rate;
        return true;
    }

    /**
     * Returns the baud rate of the egress port that is using this scheduler.
     * If the baud rate is zero, it means you haven't set it up.
     * @return the baud rate in bits/s
     * @see gridsim.net.PacketScheduler#setBaudRate(double)
     * @pre $none
     * @post $result >= 0
     */
    public double getBaudRate() {
        return baudRate_;
    }

    /**
     * Sets the router ID that hosts this scheduler.
     * @param routerID  the router ID that hosts this scheduler
     * @return <tt>true</tt> if successful or <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public boolean setRouterID(int routerID)
    {
        System.out.println(name_ + ".setRouterID(): Router ID is not required");
        return false;
    }

    /**
     * Gets the router ID that hosts this scheduler.
     * @return the router ID or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public int getRouterID() {
        return -1;
    }

} // end class

