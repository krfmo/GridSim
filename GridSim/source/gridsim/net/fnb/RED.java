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
 */

package gridsim.net.fnb;

import gridsim.net.Packet;
import gridsim.GridSimTags;
import gridsim.ParameterException;
import java.util.Random;
import gridsim.GridSim;
import java.io.FileWriter;
import gridsim.net.*;


/**
  * This class implements the Random Early Detection (RED) policy for the management
  * of netwrk buffers at routers.
  * Its basic functionality is as follows:
  * <ul>
  * <li> There is a RED object at each outport in routers.
  * <li> For each incoming packet that reaches that outport port, the policy
  *    decides whether it is enqueued or dropped. This is done by calculating the
  *    average buffer size and comparing it with two thresholds.
  * <li> If the packet is dropped, and it is not a junk packet, we must inform the
  *    user involved in the transmission about the dropping.
  * </ul>
  *
  * For more details, please refer to A. Caminero, A. Sulistio, B. Caminero,
  * C. Carrion, and R. Buyya,
  * <a href="http://www.gridbus.org/papers/BufferManagementNetGrids-ANSS41.pdf">
  * Simulation of Buffer Management Policies in Networks for Grids</a>,
  * Proceedings of the 41th Annual Simulation Symposium (ANSS-41, IEEE CS Press,
  * Los Alamitos, CA, USA), April 14-16, 2008, Ottawa, Canada.
  *
  * @author       Agustin Caminero
  * @since GridSim Toolkit 4.2
  * */
public class RED extends FnbSCFQScheduler
{
    // Some parameters for the RED algorithm
    private double MAX_TH; // thresholds for the RED algorithm
    private double MIN_TH;
    private double MAX_P; // maximum value for the dropping probability
    private double QUEUE_WEIGHT; // the queue weigth
    private double AVG;
    private int COUNT;
    private double Q_TIME;
    private double S;
    private int R;
    private Random random;
    private double C1;
    private double C2;
    private double MAX_AVG = 0.0;


    /**
     * Creates a new SCFQ packet scheduler with the specified name and baud rate
     * (in bits/sec). The name can be useful for debugging purposes, but serves
     * no functional purposes.
     *
     * @param name       Name of this scheduler
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @param max_buf_size maximum buffer size for routers
     * @param min_th minimum threshold for RED
     * @param max_th maximum threshold for RED
     * @param max_p  maximum drop probability for RED
     * @param queue_weight queue weight for RED
     * @param storeStats whether we want to store stats or not
     * @throws Exception This happens when the name is null or
     *                   the baud rate <= 0
     * @pre name != null
     * @pre baudRate > 0
     * @post $none
     */
    public RED(String name, double baudRate, int max_buf_size, double min_th,
               double max_th, double max_p, double queue_weight, boolean storeStats) throws
            Exception
    {

        super(name, baudRate, max_buf_size, storeStats);

        if (name == null)
        {
            throw new ParameterException("RED(): Name is null.");
        }

        if (baudRate <= 0)
        {
            throw new ParameterException("RED(): Baudrate <= 0.");
        }

        MAX_TH = max_th;
        MIN_TH = min_th;
        MAX_P = max_p;
        QUEUE_WEIGHT = queue_weight;

        //System.out.println(super.get_name() + ": RED. MIN_TH: " + MIN_TH 
        //    + ". MAX_TH: " +MAX_TH );
        initialize();
    }


    /**
     * Checks queue size and puts a packet into the queue
     *
     * @param pnp    A Packet to be enqued by this scheduler.
     * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
     * @pre pnp != null
     * @post $none
     */
    public synchronized boolean enque(Packet pnp)
    {
        double pb;
        double avgQueueSize = avgQueueSize();

        //System.out.println(super.get_name() + ": enque. MIN_TH: " + MIN_TH);

        if ((MIN_TH <= avgQueueSize) && (avgQueueSize < MAX_TH))
        {
            // when the avgQueueSize is between the thrsholds, then calculate the dropping probability
            COUNT++;

            pb = C1 * AVG - C2;

            if ((COUNT > 0) && (COUNT >= R / pb))
            {
                dropPacket(pnp);

                return true;
            }
            else if (COUNT == 0)
            {
                R = random.nextInt(2); // 2, because the interval is [0,1]
            }

            checkAndInsertPacketIntoQueue(pnp); /// insert the pkt into the queue
            return true;
        }
        else if (avgQueueSize >= MAX_TH)
        {
            // if the avgQueueSize is greater than the max threshold, then drop the packet.
            // If it's a control packet, then the sim will get stopped.
            dropPacket(pnp);
            COUNT = -1;

            return true;
        }
        else
        {
            COUNT = -1;
            checkAndInsertPacketIntoQueue(pnp); /// insert the pkt into the queue
            return true;
        }
    } // public synchronized boolean enque(Packet pnp)

    /**
     * Puts a packet into the queue, checking the queue size before that.
     *
     * @param pnp    A Packet to be enqued by this scheduler.
     * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
     * @pre pnp != null
     * @post $none
     */
    private synchronized boolean checkAndInsertPacketIntoQueue(Packet pnp)
    {
        if (size() < getMaxBufferSizeInPkts())
        {
            insertPacketIntoQueue(pnp);
            return true;
        }
        else
        {
            dropPacket(pnp);
            return false;
        }
    }

    /** If the queue is full, we have to drop a packet. Note that packets going
     * to/coming from entities that are listed in 
     * the {@link gridsim.GridSim#fnbWhiteList_} will not be dropped.
     * @param pnp the new incoming packet
     */
    private synchronized void dropPacket(Packet pnp)
    {
        increaseDroppedPktCounter(); // increase the counter of dropped packets

        // First: check if the new incoming packet is a control packet or not.
        int src_outputPort;
        String src_outputPort_str;
        int dst;
        String dst_str;
        String dst_outputPort_str;

        // Check if it is a ping packet, or a data packet
        if (pnp instanceof InfoPacket)
        {
            // Check the source of the packet
            src_outputPort = ((InfoPacket) pnp).getSrcID();
            src_outputPort_str = GridSim.getEntityName(src_outputPort);
            // for example, src_outputPort_str = Output_SIM_0_User_0

            // Check the destination
            dst = ((InfoPacket) pnp).getDestID();
            dst_str = GridSim.getEntityName(dst);
            dst_outputPort_str = "Output_".concat(dst_str);
        }
        else
        {
            // Check the source of the packet
            src_outputPort = ((FnbNetPacket) pnp).getSrcID();
            src_outputPort_str = GridSim.getEntityName(src_outputPort);
            // for example, src_outputPort_str = Output_SIM_0_User_0

            // Check the destination
            dst = ((FnbNetPacket) pnp).getDestID();
            dst_str = GridSim.getEntityName(dst);
            dst_outputPort_str = "Output_".concat(dst_str);
        }

        /**************************
        // This works
        // If one of the ends of this packet is the broker or the GIS.
        // Also, pkts whose source or dest is a router are protected
        // (monitoring ping packets). And monitoring pkts (from the res monitor)
        if ((src_outputPort_str.indexOf("Broker") != -1) ||
            (dst_str.indexOf("Broker") != -1) ||
            (src_outputPort_str.indexOf("GIS") != -1) ||
            (dst_str.indexOf("GIS") != -1) ||
            (src_outputPort_str.indexOf("RC") != -1) ||
            (dst_str.indexOf("RC") != -1))
        ************************/

        // if the src or the dest of the packet are in the whitelist, then try not to drop the packet
        FnbWhiteList whiteList = FnbWhiteList.getInstance();
        if (whiteList.checkList(dst) || whiteList.checkList(src_outputPort))
        {

            if (makeRoomForPacket())
            {
                // If we have been able of dropping a data packet, then we have room for this control packet
                insertPacketIntoQueue(pnp);

                //return true;
            }
            else
            {
                System.out.println("\n" + super.get_name() +
                    ": 271 A control packet has been dropped.\n" +
                    "src_output: " + src_outputPort_str +
                    ". dst_inputPort: " + dst_str +
                    "\nHENCE, SIMULATION FINISHED AT SIM. TIME " + GridSim.clock());

                //fw_write("MaxBufferSizeDuringSim, " + getMaxBufferSize() + "\n",
                //         this.get_name() + "_MaxBufferSize.csv");

                System.exit(1);
                // super.shutdownUserEntity();
                // super.terminateIOEntities();
            }
        }

        // If u want more info on the progress of sims, uncomment this.
        /********************
        System.out.println(super.get_name() + ": packet dropped. Src: " +
                           src_outputPort_str + ", Dst: " + dst_str +
                           ". Pkt num: " + ((FnbNetPacket) pnp).getPacketNum());
        ********************/

        // If the packet is not a control packet, we will have to remove the packet, and
        // also, we will have to tell the user involved in the transmission about the dropping.
        // The user may be the source or the destination of the dropped packet.
        // This is done through an event, not through the network

        int entity;
        String destination_packetsDroppedEvent;
        if (src_outputPort_str.indexOf("User") != -1)
        {
            // if the src of the packet is an user, tell him what has happened

            // NOTE: this is a HACK job. "Output_" has 7 chars. So,
            // the idea is to get only the entity name by removing
            // "Output_" word in the outName string.
            String src_str = src_outputPort_str.substring(7);
            // for example, src_str = SIM_0_User_0

            entity = GridSim.getEntityId(src_str);
            destination_packetsDroppedEvent = src_outputPort_str;
        }
        else
        {
            // If the destination of the packet is the user, tell the user

            // NOTE: this is a HACK job. "Output_" has 7 chars. So,
            // the idea is to get only the entity name by removing
            // "Output_" word in the outName string.

            entity = GridSim.getEntityId(dst_str);
            destination_packetsDroppedEvent = dst_outputPort_str;
        }

        // Check if it is a ping packet, or a data packet
        int pktID;
        int tag;
        int glID;
        //String filename= null;
        //int userID;
        boolean isFile;
        if (pnp instanceof InfoPacket)
        {
            pktID = ((InfoPacket) pnp).getID();
            tag = 99999;
            glID = 99999;
            isFile = false;
        }
        else
        {
            pktID = ((FnbNetPacket) pnp).getID();
            tag = ((FnbNetPacket) pnp).getTag();

            // check the user and the gl this packet belongs to
            glID = ((FnbNetPacket) pnp).getObjectID();
            //filename = ((FnbNetPacket) pnp).getFileName();
            isFile = ((FnbNetPacket) pnp).isFile();
        }

        if (tag != GridSimTags.JUNK_PKT) // && (tag != GridSimTags.EMPTY_PKT))
        {
            // Only tell the user if this is not a junk packet (a background traffic pkt)

            // String input_src_str = "Input_" + src_str;
            // for example, input_src_str = Input_SIM_0_User_0
            // int input_src = GridSim.getEntityId(input_src_str);

            /****************
            // Uncomment this to get more info on the progress of sims
            System.out.println("\n" + name_ +
                ": A packet has been dropped.\n" +
                "src_output: " + src_outputPort_str +
                ". dst_inputPort: " + dst_str + ". Tag: " + tag + "\nTime: " +
                GridSim.clock());
            ******************/


            if (checkDroppedGlList(glID, entity) == false)
            {
                // if the gridlet has not been already dropped, then 
                // send event. Otherwise, do nothing, to safe memory

                //super.send(GridSim.getEntityId(destination_packetsDroppedEvent), //src_outputPort,
                super.sim_schedule(GridSim.getEntityId(destination_packetsDroppedEvent), //src_outputPort,
                           GridSimTags.SCHEDULE_NOW + GridSimTags.FNB_DROPPING_DELAY,
                           GridSimTags.FNB_PACKET_DROPPED,
                           new FnbDroppedUserObject(entity, glID, isFile));
                // We tell the user involved in the transmission
                // that the packet has been dropped. We do it with some delay to simulate a time out.

                //Moreover, we have to update the dropped gl list.
                FnbDroppedPacketInfo gu = new FnbDroppedPacketInfo(glID, entity);
                insertGlID_userID(gu);


                /*************************
                System.out.println("\n" + super.get_name() +
                       ": 387 A packet has been dropped, and an ACK has been sent.\n" +
                       "  src.output: " + src_outputPort_str +
                       ", dst_inputPort: " + dst_str + "\nTime: " +
                       GridSim.clock() + ". PktID: " + pnp.getID() + ". Gl: " + glID +
                       ". destination_packetsDroppedEvent: " +
                       destination_packetsDroppedEvent);
                **********************/
            }
            /**********************************
            else{
                System.out.println("\n" + super.get_name() +
                                   ": 400 A packet has been dropped.\n" +
                                   " src.output: " + src_outputPort_str +
                                   ". dst_inputPort: " + dst_str +
                                   "\n Time: " +
                                   GridSim.clock() + ". PktID: " + pnp.getID() + ". Gl: " +
                                   glID + ". destination_packetsDroppedEvent: " +
                                   destination_packetsDroppedEvent);

            }
            *********************************/
            pnp = null; // remove the packet.

        }
        else
        {
            /********************
            // Uncomment this to get more info on the progress of sims
            System.out.println("\n" + super.get_name() +
                    ": 416 A junk packet has been dropped.\n" +
                    " src.output: " + src_outputPort_str +
                    ". dst_inputPort: " + dst_str + "\nTime: " + GridSim.clock());
            ********************/
        }

    }

    /** Calculate the avg queue size for the RED algorithm.
     * @return the avg queue size, which is calculated in this method */
    public double avgQueueSize()
    {
        int q = size();
        double time = GridSim.clock();

        // Only this if is necesary for RED/ARED
        if (q != 0)
        {
            AVG = AVG + QUEUE_WEIGHT * (q - AVG);
        }
        else
        {
            AVG = Math.pow((1 - QUEUE_WEIGHT), (time - Q_TIME) / S) * AVG;
        }

        if (AVG > MAX_AVG)
            MAX_AVG = AVG;

        return AVG;

    } // avgQueueSize()

    /**This function initializes the parameters of the buffers policies (RED, ARED)
     */
    protected void initialize()
    {
        random = new Random();

        AVG = 0;
        COUNT = -1;
        Q_TIME = 0;
        S = 0.010; // 10 milliseconds

        C1 = MAX_P / (MAX_TH - MIN_TH);
        C2 = MAX_P * MIN_TH / (MAX_TH - MIN_TH);

    }

    /**This function initializes the C parameter of the buffers policies (RED, ARED)
     */
    // NOTE: redundant?
    private void initializeC()
    {
        C1 = MAX_P / (MAX_TH - MIN_TH);
        C2 = MAX_P * MIN_TH / (MAX_TH - MIN_TH);
    }

    /**
     * Prints out the given message into stdout.
     * In addition, writes it into a file.
     * @param msg   a message
     * @param file  file where we want to write
     */
    private static void fw_write(String msg, String file)
    {
        //System.out.print(msg);
        FileWriter fwriter = null;

        try
        {
            fwriter = new FileWriter(file, true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while opening file " + file);
        }

        try
        {
            fwriter.write(msg);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while writing on file " + file);
        }

        try
        {
            fwriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while closing file " + file);
        }
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
        Packet p = super.deque();

        if (super.pktListSize() == 0)
            Q_TIME = GridSim.clock();

        return p;
    }

    /**
     * Sets the baud rate that this scheduler will be sending packets at.
     * @param rate the baud rate of this scheduler (in bits/s)
     * @pre rate > 0
     * @post $none
     * @return true if the baud rate has been set properly, false otherwise
     */
    public boolean setBaudRate(double rate)
    {

        if (rate <= 0.0)
        {
            return false;
        }
        super.setBaudRateSCFQ(rate);
        return true;
    }

    /** Update the statistics of this scheduler to a file. <br>
     * The file name is schedulerName_Buffers.csv.<br>
     * The format is "Clock, MAX_P, MIN_TH, MAX_TH, AVG, QUEUE_SIZE"
     */
    public void updateStats()
    {
        fw_write(GridSim.clock() + ", " + MAX_P + ", " + MIN_TH + ", " +
                 MAX_TH + ", " + AVG + ", " + super.size() + "\n",
                 super.getSchedName() + "_Buffers.csv");
    }

    /** Returns the <tt>AVG</tt>
     * @return the avg buffer size */
    public double getAvg()
    {
        return AVG;
    }


    /** Returns the <tt>MAX_P</tt>
     * @return the maximum dropping probability */
    public double getMaxP()
    {
        return MAX_P;
    }

    /** Updates the value of <tt>MAX_P</tt>
     * @param m new value for the maximum dropping probability */
    public void setMaxP(double m)
    {
        MAX_P = m;
    }


    /** Returns the <tt>MIN_TH</tt>
     *  @return the minimum threshold*/
    public double getMinTh()
    {
        return MIN_TH;
    }

    /** Returns the <tt>MAX_TH</tt>
     * @return the maximum threshold*/
    public double getMaxTh()
    {
        return MAX_TH;
    }


    /**Updates the value of <tt>QUEUE_WEIGHT</tt>
     * @param q new queue weight */
    public void setQueueWeight(double q)
    {
        QUEUE_WEIGHT = q;
    }


    /** Updates the value of <tt>MIN_TH</tt>
     * @param m new minimum threshold*/
    public void setMinTh(double m)
    {
        MIN_TH = m;
    }


    /** Updates the value of <tt>MAX_TH</tt>
     * @param m new maximum threshold*/
    public void setMaxTh(double m)
    {
        MAX_TH = m;
    }
}

