/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Author: Gokul Poduval & Chen-Khong Tham
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 *
 * Based on SCFQScheduler class, by Gokul Poduval & Chen-Khong Tham,
 * National University of Singapore.
 * Things added or modifyed:
 *    - init(...)
 *    - enque()
 *    - MIN, INTERVAL, nextInterval
 *    - MAX_BUFF_SIZE_PK
 *    - extends GridSim
 *    - insertPacketIntoQueue(...), checkAndInsertPacketIntoQueue(...)
 *    - enque(...) does not insert packet into the queue, now this function only
 *      checks if we have enough room for a new packet.
 *    - deque()
 *    - makeRoomForPacket(...)
 *    - FIFO, RED, DROPPING_ALG, setDroppingAlg(...), getDroppingAlg()
 *    - Everything regarding RED dropping algorithm
 *    - DROPPED_PKTS_COUNTER
 *    - getMaxBufferSize()
 *    - setMaxBufferSize()
 */

package gridsim.net.fnb;

import eduni.simjava.*;
import gridsim.*;
import java.util.*;
import gridsim.net.Packet;
import java.io.FileWriter;
import gridsim.net.*;
import gridsim.net.fnb.*;


/**
 * FnbSCFQScheduler implements a Self Clocked Fair Queueing Scheduler. A SCFQ is a
 * variation of Weighted Fair Queueing (WFQ), which is easier to implement than
 * WFQ because it does not need to compute round numbers at every iteration.
 * For more details refer to <b>S. R. Golestani's</b> INFOCOM '94 paper
 * <i>A self-clocked fair queueing scheme for broadband applications</i>.
 * Note that this class is based on {@link gridsim.net.SCFQScheduler} class.
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
 * @since GridSim Toolkit 4.2
 * @author  Agustin Caminero, Universidad de Castilla-La Mancha (UCLM) (Spain)
 */
public abstract class FnbSCFQScheduler extends Sim_entity implements PacketScheduler
{
    private String name_;       // this scheduler name
    private double baudRate_;   // baud rate of this scheduler
    private Vector pktList;     // Sorted List of all Packets
    private Vector timeList;    // Sorted list of finish times
    private double[] weights;   // weights for different ToS packets
    private double CF ;         // current finish number
    private Hashtable flowTable;

    private int maxBufferSize = 0; // max buffer size used this scheduler in the experiment
    private boolean storeStats;    // record stats or not

    private int DROPPED_PKTS_COUNTER = 0;
    private int MAX_BUFF_SIZE_PK;  // max number of packets that fit into a buffer

    private ArrayList droppedGl_user;
    // in this array, we keep the list of FnbDroppedPacketInfo objects, showing the (gridlets, userID)
    // already dropped in this scheduler.
    // This way, we will only send a PACKET_DROPPED event for the first dropped packet of a gridlet.
    // This way, we will avoid a lot of events, thus saving memory.

    //public double MAX_AVG = 0.0;


    /**
    * Creates a new SCFQ packet scheduler with the specified name and baud rate
    * (in bits/s). The name can be useful for debugging purposes, but serves
    * no functional purposes.
    *
    * @param name       Name of this scheduler
    * @param baudRate   baud rate in bits/s of the port that is using
    *                   this scheduler.
    * @param max_buf_size maximum buffer size for routers
    * @param stats whether we want to store stats or not
    * @throws Exception This happens when the name is null or
    *                   the baud rate <= 0
    * @pre name != null
    * @pre baudRate > 0
    * @post $none
    */
    public FnbSCFQScheduler(String name, double baudRate, int max_buf_size, 
                            boolean stats) throws Exception
    {
        super(name);

        if (name == null) {
            throw new ParameterException("FnbSCFQScheduler(): Name is null.");
        }

        if (baudRate <= 0) {
            throw new ParameterException("FnbSCFQScheduler(): Baudrate <= 0.");
        }

        name_ = name;
        baudRate_ = baudRate;
        MAX_BUFF_SIZE_PK = max_buf_size;
        storeStats = stats;
        droppedGl_user = new ArrayList();
        init();
    }

    /**
    * Creates a new SCFQ packet scheduler with the specified baud rate
    * (bits/s). The name is set to a generic name: <b>"FnbSCFQScheduler"</b>.
    *
    * @param baudRate   baud rate in bits/s of the port that is using
    *                   this scheduler.
    * @param max_buf_size maximum buffer size for routers
    * @param stats whether we want to store stats or not
    * @throws Exception This happens when the baud rate <= 0
    * @pre baudRate > 0
    * @post $none
    */
    public FnbSCFQScheduler(double baudRate, int max_buf_size, boolean stats) throws Exception
    {
        super("FnbSCFQScheduler");

        if (baudRate <= 0) {
            throw new ParameterException("FnbSCFQScheduler(): Baudrate <= 0.");
        }

        name_ = "FnbSCFQScheduler";
        baudRate_ = baudRate;
        MAX_BUFF_SIZE_PK = max_buf_size;
        storeStats = stats;
        droppedGl_user = new ArrayList();
        init();
    }

    /**
    * Creates a new SCFQ packet scheduler with the specified name.
    * The baud rate is left at 0, and should be set with
    * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
    * before the simulation starts.
    *
    * @param name Name of this scheduler
    * @param max_buf_size maximum buffer size for routers
    * @param stats whether we want to store stats or not
    * @throws Exception This happens when the name is null
    * @see gridsim.net.PacketScheduler#setBaudRate(double)
    * @pre name != null
    * @post $none
    */
    public FnbSCFQScheduler(String name, int max_buf_size, boolean stats) throws Exception
    {
        super(name);

        if (name == null) {
            throw new ParameterException("FnbSCFQScheduler(): Name is null.");
        }

        name_ = name;
        baudRate_ = 0;
        MAX_BUFF_SIZE_PK = max_buf_size;
        storeStats = stats;
        droppedGl_user = new ArrayList();
        init();
    }

    /**
    * Creates a new packet scheduler with the name <b>"FnbSCFQScheduler"</b>.
    * The baud rate is left at 0, and should be set with
    * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
    * before the simulation starts.
    * @param stats whether we want to store stats or not
    * @throws Exception This happens when the name is null
    * @see gridsim.net.PacketScheduler#setBaudRate(double)
    * @pre $none
    * @post $none
    */
    public FnbSCFQScheduler(boolean stats) throws Exception
    {
        super("FnbSCFQScheduler");

        name_ = "FnbSCFQScheduler";
        baudRate_ = 0;
        storeStats = stats;
        droppedGl_user = new ArrayList();
        init();
    }

    /**
    * Creates a new SCFQ packet scheduler with the specified baud rate
    * (bits/s). The name is set to a generic name: <b>"FnbSCFQScheduler"</b>.
    *
    * @param baudRate   baud rate in bits/s of the port that is using
    *                   this scheduler.
    * @param max_buf_size maximum buffer size for routers
    * @throws Exception This happens when the baud rate <= 0
    * @pre baudRate > 0
    * @post $none
    */
    public FnbSCFQScheduler(double baudRate, int max_buf_size) throws Exception
    {
        super("FnbSCFQScheduler");

        if (baudRate <= 0) {
            throw new ParameterException("FnbSCFQScheduler(): Baudrate <= 0.");
        }

        name_ = "FnbSCFQScheduler";
        baudRate_ = baudRate;
        MAX_BUFF_SIZE_PK = max_buf_size;
        droppedGl_user = new ArrayList();
        init();
    }

    /**
    * Creates a new SCFQ packet scheduler with the specified name.
    * The baud rate is left at 0, and should be set with
    * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
    * before the simulation starts.
    *
    * @param name Name of this scheduler
    * @param max_buf_size maximum buffer size for routers
    * @throws Exception This happens when the name is null
    * @see gridsim.net.PacketScheduler#setBaudRate(double)
    * @pre name != null
    * @post $none
    */
    public FnbSCFQScheduler(String name, int max_buf_size) throws Exception
    {
        super(name);

        if (name == null) {
            throw new ParameterException("FnbSCFQScheduler(): Name is null.");
        }

        name_ = name;
        baudRate_ = 0;
        MAX_BUFF_SIZE_PK = max_buf_size;
        droppedGl_user = new ArrayList();
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

        if (storeStats)
        {
            fw_write("Interval, BufferSize, AvgBufferSize, MaxBufferSize\n",
            getSchedName() + "_MaxBufferSize.csv", false);

            fw_write("Interval, DroppedPackets\n",
            this.getSchedName() + "_DroppedPkts.csv", false);

            fw_write("Clock, MAX_P, MIN_TH, MAX_TH, AVG, QUEUE_SIZE\n",
            this.getSchedName() + "_Buffers.csv", false);
        }
    }


    /**This function initializes the parameters of the buffers policies (RED, ARED)
    */
    protected abstract void initialize();


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
    * Checks queue size and puts a packet into the queue
    *
    * @param pnp    A Packet to be enqued by this scheduler.
    * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
    * @pre pnp != null
    * @post $none
    */
    public abstract boolean enque(Packet pnp);


    /**
    * Calculates the finish time of a particular packet
    * @param np  a network packet
    * @param nextTime  the next available time
    * @pre np != null
    * @pre nextTime >= 0
    * @post $none
    * @return finish time for the network paket
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
                    " packet class = " + type + ", weight.length = " + weights.length);
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
    * Puts a packet into the queue
    *
    * @param pnp    A Packet to be enqued by this scheduler.
    * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
    * @pre pnp != null
    * @post $none
    */
    protected synchronized boolean insertPacketIntoQueue(Packet pnp)
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

        double pktTime = calculateFinishTime(pnp, nextTime.doubleValue());
        flowTable.put("" + srcID + destID + type, new Double(pktTime));
        insert(pnp, pktTime); // Sort the queue list

        // Keep an statistic regarding the size of the buffers.
        int bufferSize = this.size();

        if (storeStats)
        {
            if (bufferSize > maxBufferSize)
            {
                maxBufferSize = bufferSize;

                fw_write(GridSim.clock() + ", " + bufferSize + ", " +
                        getAvg() + ", " + maxBufferSize + "\n",
                        this.getSchedName() + "_MaxBufferSize.csv");
            }
        }

        return true;
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
            CF = ((Double) timeList.remove(0)).doubleValue();

            /************
            System.out.println(super.get_name() + ": <<<< deque function. PktID: " +
                    ((FnbNetPacket) p).getID() + ". glID: " +
                    ((FnbNetPacket) p).getGlID());
            ************/
        }

        return p;
    }

    /**Returns the size of the packet list.
    * @return size of the list of packets*/
    protected double pktListSize()
    {
        return pktList.size();
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
    * @return true if the baud rate has been set properly, false otherwise
    */
    public boolean setBaudRateSCFQ(double rate)
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

    /**
    * Prints out the given message into stdout.
    * In addition, writes it into a file.
    * @param msg   a message
    * @param file  file where we want to write
    */
    private void fw_write(String msg, String file)
    {
        fw_write(msg, file, true);
    }

    /**
    * Prints out the given message into stdout.
    * In addition, writes it into a file.
    * @param msg   a message
    * @param file  file where we want to write
    * @param append     append the message at the end of the file or not
    */
    private void fw_write(String msg, String file, boolean append)
    {
        //System.out.print(msg);
        FileWriter fwriter = null;

        try
        {
            fwriter = new FileWriter(file, append);
        } 
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while opening file " + file);
        }

        try
        {
            fwriter.write(msg);
        } 
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while writing on file " + file);
        }

        try
        {
            fwriter.close();
        } 
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while closing file " + file);
        }
    }

    /**This function tries to drop a data packet out of a full queue, so that a
    * control packet can be stored in that queue.
    * @return <tt>true</tt> if we have been able to find a data packet 
    *         to be dropped, <tt>false</tt> otherwise.
    */
    protected synchronized boolean makeRoomForPacket()
    {
        Packet pkt;

        int src_outputPort;
        String src_outputPort_str;

        int dst;
        String dst_str;

        boolean isFile = false;
        int glID;
        //String filename;

        /***********
        System.out.println("\n" + super.get_name() + ":(make) Size(): " +
                        size() + " pkts. Time: " + GridSim.clock());
        ************/

        for (int i = 0; i < size() ; i++)
        {
            pkt = (Packet) pktList.get(i);

            if (pkt instanceof FnbNetPacket)
            isFile = ((FnbNetPacket) pkt).isFile();

            int pktID = pkt.getID();

            src_outputPort = ((FnbNetPacket) pkt).getSrcID();
            src_outputPort_str = GridSim.getEntityName(src_outputPort);

            String src_str = src_outputPort_str.substring(7);
            // for example, src_str = SIM_0_User_0


            src_outputPort = ((FnbNetPacket) pkt).getSrcID();
            src_outputPort_str = GridSim.getEntityName(src_outputPort);
            // for example, src_outputPort_str = Output_SIM_0_User_0

            // Check the destination, as I'm not sure if it works like that
            dst = ((FnbNetPacket) pkt).getDestID();
            dst_str = GridSim.getEntityName(dst);

            // We must tell the user, not the other end of the transmission
            int entity;
            if (dst_str.indexOf("User") == -1)
            {
                entity = GridSim.getEntityId(src_str);
            }
            else
            {
                entity = GridSim.getEntityId(dst_str);
            }

            /************* // This works
            // If none of the ends of this packet is the broker or the GIS
            if ((src_outputPort_str.indexOf("Broker") == -1) &&
                (dst_str.indexOf("Broker") == -1) &&
                (src_outputPort_str.indexOf("GIS") == -1) &&
                (dst_str.indexOf("GIS") == -1) ||
                (src_outputPort_str.indexOf("RC") != -1) ||
                (dst_str.indexOf("RC") != -1))
            ***************/


            // if the src and the dest of the packet are not in the whitelist, 
            // then we can drop the pkt
            FnbWhiteList whiteList = FnbWhiteList.getInstance();
            if (!whiteList.checkList(dst) &&
                !whiteList.checkList(src_outputPort))
            {
                // To remove a packet form this queue we have to do some stuff
                // I've copied this from the deque method
                pktList.remove(i);
                if (i == 0)
                {
                    CF = ((Double) timeList.remove(i)).doubleValue();
                }
                else
                {
                    timeList.remove(i);
                }

                // Also, we have to tell the user involved in this transmission
                // that this packet it's been dropped.
                glID = ((FnbNetPacket) pkt).getObjectID();
                if (checkDroppedGlList(glID, entity) == false)
                {
                    //super.send(src_outputPort, GridSimTags.SCHEDULE_NOW,
                    super.sim_schedule(src_outputPort, GridSimTags.SCHEDULE_NOW,
                            GridSimTags.FNB_PACKET_DROPPED, 
                            new FnbDroppedUserObject(entity, glID, isFile));

                    /******
                    System.out.println("\n" + super.get_name() +
                        ":(make) A packet has been dropped, and an ACK has been sent.\n" +
                        "  src.output: " + src_outputPort_str +
                        ". dst: " + dst_str + "\n  Time: " +
                        GridSim.clock() + ". PktID: " + pkt.getID() +". Gl: " + glID);
                    ******/

                }
                /*****
                else
                {
                    System.out.println("\n" + super.get_name() +
                        ":(make) A packet has been dropped.\n" +
                        "  src.output: " + src_outputPort_str +
                        ". dst: " + dst_str +
                        "\n  Time: " + GridSim.clock() + ". PktID: " + pkt.getID() +
                        ". Gl: " + glID);
                   
                }
                *****/

                return true;
            }


        }// for (int i = 0; i < size() ; i++)

        //System.out.println("\n" + super.get_name() +
        //    ":(make) No packet could be dropped.\nTime: " + GridSim.clock() );

        return false;
    }

    /**
    * Returns the DROPPED_PKTS_COUNTER
    * @return the counter of dropped packets*/
    protected double getCounterDroppedPkts()
    {
        return DROPPED_PKTS_COUNTER;
    }

    /**
    * Resets the DROPPED_PKTS_COUNTER */
    protected void resetCounterDroppedPkts()
    {
        DROPPED_PKTS_COUNTER = 0;
    }


    /**This function returns maximum buffer size, 
    * up to this moment along the experiment.
    * Note that this is not the max allowed buffer size in pkts.
    * @return the maximum buffer size, up this moment
    * */
    protected double getMaxBufferSize()
    {
        return maxBufferSize;
    }

    /**This function returns the max buffer size in pkts.
    * @return the maximum number of packets that fit into this buffer*/
    protected double getMaxBufferSizeInPkts()
    {
        return MAX_BUFF_SIZE_PK;
    }

    /**This function sets the maximum buffer size
    * @param maxSize the new maximum buffer size */
    protected void setMaxBufferSize(int maxSize)
    {
        maxBufferSize = maxSize;
    }

    /**
    * A method that gets one process event at one time until the end
    * of a simulation, then delivers an event to the entity (its parent)
    * @pre $none
    * @post $none
    */
    public void body()
    {
        //super.send(super.get_id(),
        //    Math.floor(GridSimTags.SCHEDULE_NOW + 21600000), // 216000 secs= 10h
        //    GridSimTags.COUNT_DROPPED_PKTS);

        /**********  // NOTE: an empty body() method
        // Process events
        Object obj = null;
        while ( Sim_system.running() )
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev); // get the next event in the queue
            obj = ev.get_data(); // get the incoming data

            //if (ev.get_tag() == GridSimTags.UPDATE_ARED_PARAMETERS)
            //    updateAREDParameters();
            
            // if the simulation finishes then exit the loop
            if ((ev.get_tag() == GridSimTags.END_OF_SIMULATION) ||
                (ev.get_tag() == GridSimTags.COUNT_DROPPED_PKTS))
            {
                fw_write(GridSim.clock() + ", " + DROPPED_PKTS_COUNTER + "\n",
                        this.getSchedName() + "_DroppedPkts.csv");

                DROPPED_PKTS_COUNTER = 0;
                // System.out.println(super.get_name() + ": max_avg: " + MAX_AVG);

                if (ev.get_tag() == GridSimTags.COUNT_DROPPED_PKTS)
                {
                    System.out.println(super.get_name() +
                                    ": COUNT_DROPPED_PKTS event arrived. Clock: " +
                                    GridSim.clock() + "Next event will be in (delay) " +
                                    (GridSimTags.SCHEDULE_NOW + 3600));

                    super.send(super.get_id(),
                            Math.floor(GridSimTags.SCHEDULE_NOW + 3600), // 3600 secs= 1h
                            GridSimTags.COUNT_DROPPED_PKTS);
                }

                break;
            }            
        }
        **********/
    }

    /** Returns the avg buffer size
    * @return the avg buffer size */
    public abstract double getAvg();

    /** Increases the counter for the dropped packets by 1 */
    protected void increaseDroppedPktCounter()
    {
        DROPPED_PKTS_COUNTER++;
    }

    /** Adds the packet info to the dropped list
    * @param info   a dropped packet info
    */
    protected void insertGlID_userID(FnbDroppedPacketInfo info)
    {
        droppedGl_user.add(info);
    }

    /**Checks if there is an existing gridletID_userID in the droppedGl_user array.
    * We consider gridlets, datagridlets and files
    * @param gl the gridlet
    * @param user user to which the gridlet belongs
    * @return true, if there is a gridletID_userID object, false otherwise */
    // NOTE: redundant?
    protected boolean checkDroppedGlList(int gl, int user)
    {
        int i =0;
        FnbDroppedPacketInfo glID_uID;
        while (i < droppedGl_user.size())
        {
            glID_uID = (FnbDroppedPacketInfo) droppedGl_user.get(i);
            if ((glID_uID.getGridletID() == gl) && (glID_uID.getUserID() == user))
                return true;
            else
                i++;
        }
        return false;
    }

    /** Update the statistics of this scheduler to a file.
    */
    public abstract void updateStats();

} // end class

