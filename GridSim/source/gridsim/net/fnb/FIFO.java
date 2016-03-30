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

import gridsim.net.fnb.*;
import gridsim.net.Packet;
import gridsim.GridSimTags;
import gridsim.ParameterException;
import gridsim.GridSim;
import java.io.FileWriter;
import gridsim.net.*;

/**
  * This class implements the FIFO policy for the management
  * of network buffers at routers.
  * Its basic functionality is as follows:
  * <ul>
  * <li> There is a FIFO object at each outport in routers.
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
public class FIFO extends FnbSCFQScheduler
{
    private double QUEUE_WEIGHT; // the queue weigth
    private double AVG;
    private double Q_TIME;
    private double S;
    private double MAX_AVG = 0.0;


    /**
     * Creates a new FIFO policy with the specified name and baud rate
     * (in bits/sec). The name can be useful for debugging purposes, but serves
     * no functional purposes.
     *
     * @param name       Name of this scheduler
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @param max_buf_size maximum buffer size for routers
     * @param queue_weight this parameter reflects how important is the last
     * @param stats whether we want to store stats or not
     * @throws Exception This happens when the name is null or
     *                   the baud rate <= 0
     * @pre name != null
     * @pre baudRate > 0
     * @post $none
     */
    public FIFO(String name, double baudRate, int max_buf_size,
                double queue_weight, boolean stats) throws Exception
    {
        super(name, baudRate, max_buf_size, stats);

        QUEUE_WEIGHT = queue_weight;

        if (name == null)
        {
            throw new ParameterException("fnb.FIFO(): Name is null.");
        }

        if (baudRate <= 0)
        {
            throw new ParameterException("fnb.FIFO(): Baudrate <= 0.");
        }

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
        /*double clock = GridSim.clock();
        if (clock > nextInterval)
        {
            updateAFIFOParameters();
            nextInterval = clock + INTERVAL;
        }*/

        checkAndInsertPacketIntoQueue(pnp); /// insert the pkt into the queue
        //double avgQueueSize = avgQueueSize();
        return true;
    }


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
            dropPacketFIFO(pnp);
            return false;
        }
    }

    /** If the queue is full, we have to drop a packet. Note that packets going
     * to/coming from entities that are listed in 
     * the {@link gridsim.GridSim#fnbWhiteList_} will not be dropped.
     * @param pnp the new incoming packet
     */
    private synchronized void dropPacketFIFO(Packet pnp)
    {
        // If the queue is full, we have to drop a packet, giving priority to the control packets
        // Control packets will be those packets sent between the broker or the GIS.
        // Normal packets are those belonging to a gridlet.
        // Control packets can only be dropped when the wue is full of control packets

        increaseDroppedPktCounter(); // increase the counter of dropped packets

        // First: check if the new incoming packet is a control packet or not.

        // Check the source of the packet
        int src_outputPort = ((FnbNetPacket) pnp).getSrcID();

        boolean isFile = ((FnbNetPacket) pnp).isFile();

        String src_outputPort_str = GridSim.getEntityName(src_outputPort);
        // for example, src_outputPort_str = Output_SIM_0_User_0

        // Check the destination, as I'm not sure if it works like that
        int dst_inputPort = ((FnbNetPacket) pnp).getDestID();
        String dst_inputPort_str = GridSim.getEntityName(dst_inputPort);

        // if neither the src or the dest of the packet are in the whitelist, then drop the packet
        FnbWhiteList whiteList = FnbWhiteList.getInstance();
        if (!whiteList.checkList(dst_inputPort) &&
            !whiteList.checkList(src_outputPort))
        {

            /***********
            // This was the very first version of the Fnbs
            insertPacketIntoQueue(pnp);
            return true;
            ***********/

            if (makeRoomForPacket())
            {
                // If we have been able of dropping a data packet, then we have room for this control packet
                insertPacketIntoQueue(pnp);
            }
            else
            {
                System.out.println("\n" + super.get_name() +
                    ": 271 A control packet has been dropped.\n" +
                    "src_output: " + src_outputPort_str +
                    ". dst_inputPort: " + dst_inputPort_str +
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
                           src_outputPort_str + ", Dst: " + dst_inputPort_str +
                           ". Pkt num: " + ((FnbNetPacket) pnp).getPacketNum());
        ********************/

        // In this case, we will have to remove the packet, and
        // also, we will have to tell the source of the packet what has happened.
        // The source of a packet is the user/resource which sent it.
        // So, in order to contact the entity, we do it through its output port.
        // This is done through an event, not through the network

        int entity;
        if (src_outputPort_str.indexOf("User") != -1)
        {
            // if the src of the packet is an user, tell him what has happened

            // NOTE: this is a HACK job. "Output_" has 7 chars. So,
            // the idea is to get only the entity name by removing
            // "Output_" word in the outName string.
            String src_str = src_outputPort_str.substring(7);
            // for example, src_str = SIM_0_User_0

            entity = GridSim.getEntityId(src_str);
        }
        else
        {
            // If the destination of the packet is the user, tell the user
            entity = GridSim.getEntityId(dst_inputPort_str);
        }

        int pktID = ((FnbNetPacket) pnp).getID();

        // String input_src_str = "Input_" + src_str;
        // for example, input_src_str = Input_SIM_0_User_0
        // int input_src = GridSim.getEntityId(input_src_str);

        int glID;
        if (pnp instanceof InfoPacket)
        {
            glID = 99999;
        }
        else
        {
            // check the user and the gl this packet belongs to
            glID = ((FnbNetPacket) pnp).getObjectID();
            //filename = ((FnbNetPacket) pnp).getFileName();
        }

        //super.send(src_outputPort, GridSimTags.SCHEDULE_NOW,
        super.sim_schedule(src_outputPort, GridSimTags.SCHEDULE_NOW,
                   GridSimTags.FNB_PACKET_DROPPED,
                   new FnbDroppedUserObject(entity, glID, isFile));
        // We tell the output entity of the sender of this packet
        // that the packet has been dropped.

        pnp = null; // remove the packet.
    }


    /** Calculate the avg queue size for the FIFO algorithm.
     *
     * @return the average queue size of this queue, 
     * which has been calculated in this function*/
    public double avgQueueSize()
    {

        int q = size();
        double time = GridSim.clock();

        // Only this if is necesary for the algo
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

    /**This function initializes the parameters of FIFO policy
     */
    protected void initialize()
    {
        // empty
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
     * Sets the baud rate that this scheduler will be sending packets at.
     * @param rate the baud rate of this scheduler (in bits/s)
     * @return true if the baud rate has been set properly
     * @pre rate > 0
     * @post $none
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

    /** Returns the avg buffer size
     * @return the average queue size, which was already calculated*/
    public double getAvg()
    {
        return AVG;
    }

    /** Update the statistics of this scheduler to a file. <br>
     * The file name is schedulerName_Buffers.csv.<br>
     * The format is "Clock, MAX_P, MIN_TH, MAX_TH, AVG, QUEUE_SIZE". <br>
     * Note that for the FIFO, MAX_P, MIN_TH, and MAX_TH values are empty.
     */
    public void updateStats()
    {
        fw_write(GridSim.clock() + ", , , , " + AVG + ", " + super.size() + "\n",
                 super.getSchedName() + "_Buffers.csv");
    }
}

