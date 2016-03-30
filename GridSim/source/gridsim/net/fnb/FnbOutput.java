/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero (based on Output.java)
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 *
 * Based on Output  class, by Manzur Murshed and Rajkumar Buyya
 * Things added or modifyed:
 *    -submitToLink (...)
 *    - packetsGridletsList_, getPacketsGridletsList(...)
 *    - processPacketDropped(...), PACKET_DROPPED.
 */

package gridsim.net.fnb;

import gridsim.*;
import gridsim.net.*;
import gridsim.util.*;

import eduni.simjava.*;
import java.util.*;
import gridsim.net.fnb.*;
import gridsim.datagrid.*;


/**
 * This class defines a port through which a simulation entity sends
 * data to the simulated network. Note that this class is based on
 * {@link gridsim.net.Output} class.
 * <p>
 * It maintains an event queue to serialize
 * the data-out-flow and delivers to the destination entity.
 * It works along with Input entity to simulate network
 * communication delay. Simultaneous outputs can be modeled by using multiple
 * instances of this class
 *
 * @since GridSim Toolkit 4.2
 * @author  Agustin Caminero, Universidad de Castilla-La Mancha (UCLM) (Spain)
 */
public class FnbOutput extends Sim_entity implements NetIO
{
    private Sim_port outPort_; // output port
    private Link link_; // a link to this output entity
    private double baudRate_; // baud rate of this entity
    private final int SIZE = 8; // 1 byte in bits
    private int pktID_; // packet ID counter
    private Vector packetList_; // store a list of packets
    private Random random_; // selects to which junk packets go to
    private TrafficGenerator gen_; // background traffic generator
    private ArrayList list_; // list of resources + user entities
    private boolean hasStarted_; // a flag for background traffic has started
    private static final int BITS = 8; // 1 byte = 8 bits

    // private ArrayList packetsGridletsList_; // list of firstLastPacketsGridlet objects
    // This list contains the first/last packets belonging to each gridlet

    private ArrayList filesname_fileMyIDs_; // keep a list of FileName_FileMyID  objects


    ////////////////////////// INTERNAL CLASS /////////////////////////

    private class FileName_FileMyID
    {
        // Since files don't have an id until they are registered, we need an id for them.
        String filename;
        int fileMyID;

        FileName_FileMyID(String f, int fid)
        {
            filename = f;
            fileMyID = fid;

        }


        String getFileName()
        {
            return filename;
        }

        int getFileId()
        {
            return fileMyID;
        }
    }


    ////////////////////////// INTERNAL CLASS /////////////////////////


    /**
     * Allocates a new Output object
     * @param name         the name of this object
     * @param baudRate     the communication speed
     * @throws NullPointerException This happens when creating this entity
     *                  before initializing GridSim package or this entity name
     *                  is <tt>null</tt> or empty
     * @pre name != null
     * @pre baudRate >= 0.0
     * @post $none
     */
    public FnbOutput(String name, double baudRate) throws
            NullPointerException
    {
        super(name);
        this.baudRate_ = baudRate;
        link_ = null;
        packetList_ = null;
        pktID_ = 0;

        outPort_ = new Sim_port("output_buffer");
        super.add_port(outPort_);

        // for sending background traffic
        gen_ = null;
        list_ = null;
        random_ = null;
        hasStarted_ = false;
        // packetsGridletsList_ = new ArrayList();

        filesname_fileMyIDs_ = new ArrayList();

        //System.out.println(super.get_name());
    }

    /**
     * Sets the background traffic generator for this entity.
     * <p>
     * When simulation starts, this entity will automatically sends junk
     * packets to resource entities.
     * @param gen   a background traffic generator
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre gen != null
     * @post $none
     */
    public boolean setBackgroundTraffic(TrafficGenerator gen)
    {
        if (gen == null)
        {
            return false;
        }

        gen_ = gen;
        if (list_ == null)
        {
            list_ = new ArrayList();
        }

        return true;
    }

    /**
     * Sets the background traffic generator for this entity.
     * <p>
     * When simulation starts, this entity will automatically sends junk
     * packets to resource entities and other entities. <br>
     * NOTE: Sending background traffic to itself is not supported.
     *
     * @param gen       a background traffic generator
     * @param userName  a collection of user entity name (in String object).
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre gen != null
     * @pre userName != null
     * @post $none
     */
    public boolean setBackgroundTraffic(TrafficGenerator gen,
                                        Collection userName)
    {
        if (gen == null || userName == null)
        {
            return false;
        }

        boolean flag = true;
        try
        {
            gen_ = gen;
            if (list_ == null)
            {
                list_ = new ArrayList();
            }

            // iterates through each list to check whether it is a valid
            // entity name or not
            Iterator it = userName.iterator();
            int id = -1;
            while (it.hasNext())
            {
                String name = (String) it.next();

                // check whether it is sending to itself
                id = GridSim.getEntityId("Output_" + name);
                if (id == super.get_id())
                {
                    System.out.println(super.get_name() +
                                       ".setBackgroundTraffic(): Warning - can not send " +
                                       "junk packets to itself.");
                    continue;
                }

                // get the ID of other entity
                id = GridSim.getEntityId(name);
                if (id > 0)
                {
                    Integer obj = new Integer(id);
                    list_.add(obj);
                }
                // ignore for invalid entity
                else
                {
                    System.out.println(super.get_name() +
                                       ".setBackgroundTraffic(): Warning - invalid entity " +
                                       "name for \"" + name + "\".");
                }
            }
        } catch (Exception e)
        {
            flag = false;
        }

        return flag;
    }

    /**
     * Sets this entity's link. This should be used only if the network
     * extensions are being used.
     *
     * @param link the link to which this Output entity should send data
     * @pre link != null
     * @post $none
     */
    public void addLink(Link link)
    {
        this.link_ = link;
        packetList_ = new Vector();
    }

    /**
     * Gets the baud rate
     * @return the baud rate
     * @pre $none
     * @post $result >= 0.0
     */
    public double getBaudRate()
    {
        return baudRate_;
    }

    /**
     * Gets the I/O real number based on a given value
     * @param value  the specified value
     * @return real number
     * @pre $none
     * @post $result >= 0.0
     */
    public double realIO(double value)
    {
        return GridSimRandom.realIO(value);
    }

    /**
     * A method that gets one process event at one time until the end
     * of a simulation, then delivers an event to the entity (its parent)
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // find out ids for entities that are not part of simulation network
        // topology, such as GIS, GridSimShutdown and GridStatistics
        int gisID = GridSim.getGridInfoServiceEntityId();
        int statID = GridSim.getGridStatisticsEntityId();
        int shutdownID = GridSim.getGridSimShutdownEntityId();

        // start generating some junk packets or background traffic
        startBackgroundTraffic();

        // Process incoming events
        while (Sim_system.running())
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev); // get the next event in the queue

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                break;
            }

            // handle different types of incoming events
            switch (ev.get_tag())
            {
                case GridSimTags.SEND_PACKET:
                    sendPacket();
                    break;

                    // submit ping() request
                case GridSimTags.INFOPKT_SUBMIT:
                    sendInfoPacket(ev);
                    break;

                    // replying ping() request from another entity
                case GridSimTags.INFOPKT_RETURN:
                    returnInfoPacket(ev);
                    break;

                    // activate background traffic
                case GridSimTags.JUNK_PKT:
                    generateBackgroundTraffic();
                    break;

                case GridSimTags.FNB_PACKET_DROPPED:
                    processPacketDropped(ev);
                    break;

                default:
                    //System.out.println("346: "+super.get_name()+": tag: "+ev.get_tag());
                    defaultSend(ev, gisID, statID, shutdownID);
                    break;
            }
        }
    }

    /**
     * This function takes care of the packets which are dropped at the routers.
     * What we do is just set the gridlet as failed. We do not resubmit the gridlet.
     * @param ev an incoming event
     * */
    private void processPacketDropped(Sim_event ev)
    {
        FnbDroppedUserObject userPkt = (FnbDroppedUserObject) ev.get_data();
        
        // the id of object (gridlet/file) to which the dropped pkt belonged
        int object_dropped_id = userPkt.getID(); 
        int user_id = userPkt.getUserID();

        boolean isFile = userPkt.getIsFile();
        FnbMessage msgDrop;        
        //FnbMessage msgDrop = lookForEntity(gl_dropped_id);
        
        if (isFile)
        {
            // Tell the user that the file is failed because of the dropping of a packet
            msgDrop = new FnbMessageDropFile(object_dropped_id, getFilename(object_dropped_id));

            /****************
            System.out.println(super.get_name() + 
                    ": Sending FNB_FILE_FAILED_BECAUSE_PACKET_DROPPED to " +
                    GridSim.getEntityName(user_id) + ". File: " + object_dropped_id);
            ****************/
            
            super.sim_schedule(user_id, GridSimTags.SCHEDULE_NOW,
                    GridSimTags.FNB_FILE_FAILED_BECAUSE_PACKET_DROPPED, msgDrop);
        }
        else
        {
            // we dropped a gridlet
            // Tell the user that the gridlet is failed because of the dropping of a packet
            msgDrop = new FnbMessageDropGridlet(object_dropped_id);

            /****************
            System.out.println(super.get_name() +
                    ": Sending FNB_GRIDLET_FAILED_BECAUSE_PACKET_DROPPED to " +
                    GridSim.getEntityName(user_id) + ". GL: " + object_dropped_id);
            ****************/
            
            super.sim_schedule(user_id, GridSimTags.SCHEDULE_NOW,
                    GridSimTags.FNB_GRIDLET_FAILED_BECAUSE_PACKET_DROPPED, msgDrop);
        }

        ev = null; // new, to call the garbage collector
    }


    /**
     * Generates few junk packets at the given interval
     * @pre $none
     * @post $none
     */
    private synchronized void generateBackgroundTraffic()
    {
        // get the next inter-arrival time for these junk packets
        long time = gen_.getNextPacketTime();

        // get the sending pattern
        int pattern = gen_.getPattern();

        // for initial start-up, get the list of all resources first
        if (hasStarted_ == false)
        {
            // get list of resource IDs from GIS
            LinkedList resList = GridSim.getGridResourceList();

            // if the list is empty then schedule the next time
            if (resList == null && list_.size() == 0)
            {
                super.sim_schedule(super.get_id(), time, GridSimTags.JUNK_PKT);
                return;
            }

            hasStarted_ = true;
            list_.addAll(resList); // add resource IDs into the current list

            // sets the sending pattern
            if (pattern == TrafficGenerator.SEND_ONE_ONLY && random_ == null)
            {
                random_ = new Random();
            }
        }

        // get the required info for generating this background traffic
        long size = gen_.getNextPacketSize(); // packet size
        long freq = gen_.getNextPacketFreq(); // packet freq
        int type = gen_.getServiceType(); // packet type
        int tag = GridSimTags.JUNK_PKT; // packet tag

        // we need to packetsize the data, all packets are sent with size MTU.
        // only the last packet contains the data, the receiver should
        // throw away all other packets
        int MTU = link_.getMTU();
        int numPackets = (int) Math.ceil(size / (MTU * 1.0));

        /*********   // DEBUG info
                 System.out.println();
                 System.out.println(super.get_name() +
                ": START GENERATE BG traffic... at time "+ GridSim.clock());
                 System.out.println(super.get_name() +
                ": NEXT background traffic will start at " + time);
                 System.out.println(super.get_name() +
                " num PACKETS = " + numPackets + ", freq = " + freq);
         *********/

        int i = 0;
        int destId = -1;

        // send to one of the entity using uniform distribution
        if (pattern == TrafficGenerator.SEND_ONE_ONLY)
        {
            int index = random_.nextInt(list_.size());
            destId = ((Integer) list_.get(index)).intValue();

            /*********   // DEBUG info
             System.out.println(super.get_name() + ": Destination id = " +
                destId + " = " + GridSim.getEntityName(destId) );
             *********/

            // create junk packets or empty NetPacket.
            FnbEndToEndPath conn = new FnbEndToEndPath(destId, super.get_id(), type, numPackets);
            for (i = 0; i < freq; i++)
            {
                convertIntoPacket(MTU, numPackets + 1, tag, conn);
            }
        }
        // send to all resources + other entities
        else if (pattern == TrafficGenerator.SEND_ALL)
        {
            // send to all resources and user entities
            for (int k = 0; k < list_.size(); k++)
            {
                destId = ((Integer) list_.get(k)).intValue();

                /*********   // DEBUG info
                 System.out.println(super.get_name() + ": Destination id = " +
                    destId + " = " + GridSim.getEntityName(destId) );
                 *********/

                // create junk packets or empty NetPacket.
                // make dummy packets with null data
                FnbEndToEndPath conn = new FnbEndToEndPath(destId, super.get_id(), type, numPackets);
                for (i = 0; i < freq; i++)
                {
                    convertIntoPacket(MTU, numPackets + 1, tag, conn);
                }
            }
        }

        // sends the next junk packets
        super.sim_schedule(super.get_id(), time, GridSimTags.JUNK_PKT);
    }

    /**
     * Initial start for the background traffic
     * @pre $none
     * @post $none
     */
    private synchronized void startBackgroundTraffic()
    {
        // if no background traffic generator, then skip the rest
        if (gen_ == null)
        {
            return;
        }

        // get the next inter-arrival time
        long time = gen_.getNextPacketTime();
        System.out.println(super.get_name() +
                           ": background traffic will start at time " + time);

        // starts background traffic if the inter-arrival time is valid
        if (time == -1)
        {
            return;
        }

        super.sim_schedule(super.get_id(), time, GridSimTags.JUNK_PKT);
    }

    /**
     * This method processes outgoing data without a network extension.
     * @param ev        a Sim_event object
     * @param gisID     the central/default GIS entity ID
     * @param statID    the GridStatistic entity ID
     * @param shutdownID    the GridSimShutdown entity ID
     * @pre ev != null
     * @post $none
     */
    private synchronized void defaultSend(Sim_event ev, int gisID, int statID,
                int shutdownID)
    {
        IO_data io = null;
        try
        {
            io = (IO_data) ev.get_data();

        }catch (ClassCastException e)
        {
           System.out.println("549 " + super.get_name() + ": ClassCastException. ev.tag: " + ev.get_tag());
        }

        int destId = io.getDestID();

        /*****   // DEBUG info
        System.out.println(super.get_name() + ".defaultSend(): Send to " +
            GridSim.getEntityName(destId) + " tag = " + ev.get_tag() );
        *****/

        // if this entity uses a network extension
        if (link_ != null && destId != gisID && destId != statID &&
            destId != shutdownID)
        {
            submitToLink(ev);
            return;
        }

        // Identify ID of an entity which acts as Input/Buffer
        // entity of destination entity
        int id = GridSim.getEntityId( "Input_" +
                 Sim_system.get_entity(destId).get_name() );

        // Send first and then hold
        super.sim_schedule(id, GridSimTags.SCHEDULE_NOW, ev.get_tag(), io);

        double receiverBaudRate = ( (NetIO)   //(Input)
                Sim_system.get_entity(id) ).getBaudRate();

        // NOTE: io is in byte and baud rate is in bits. 1 byte = 8 bits
        // So, convert io into bits
        double minBaudRate = Math.min(baudRate_, receiverBaudRate);
        double communicationDelay = GridSimRandom.realIO(
                (io.getByteSize() * BITS) / minBaudRate);

        // NOTE: Below is a deprecated method for SimJava 2
        //super.sim_hold(communicationDelay);
        super.sim_process(communicationDelay);
    }


    /**
     * This method takes data from an entity. If the size of the data is larger
     * than the MTU of the link, then the packet is split into mutiple size
     * units. After this it calls enque() to queue these packets into its
     * buffer.
     *
     * @param ev    A Sim_event data that contains all the data for this method
     *              to do its task.
     * @pre ev != null
     * @post $none
     */
    private synchronized void submitToLink(Sim_event ev)
    {
        //Object[] packet = new Object[2];
        IO_data data = (IO_data) ev.get_data();
        Object obj = data.getData();
        long size = data.getByteSize();
        int tag = ev.get_tag();
        int destId = data.getDestID();
        int netServiceType = data.getNetServiceLevel();

        // we need to packetsize the data, all packets are sent with size MTU.
        // only the last packet contains the data, the receiver should
        // throw away all other packets
        int MTU = link_.getMTU();
        int numPackets = (int) Math.ceil(size / (MTU * 1.0));
        int glID = 9999;
        int fileID = 9999;
        //firstLastPacketsGridlet packGl = new firstLastPacketsGridlet();
        FnbEndToEndPath conn = null;
        try{

            if (obj instanceof Gridlet)
            {
                glID = ((Gridlet) obj).getGridletID();
                // Here we will keep the packets belonging to each gridlet

                //packGl.setFirstPacketID(pktID_); // the first packet of this gridlet
                //packGl.setGridletID(glID);

                conn = new FnbEndToEndPath(destId, super.get_id(),
                                           netServiceType, numPackets, glID, false);

            }
            else if (obj instanceof DataGridlet)  // For the datagrid extension
            {
                glID = ((DataGridlet) obj).getGridletID();

                // Here we will keep the packets belonging to each gridlet

               //packGl.setFirstPacketID(pktID_); // the first packet of this gridlet
               //packGl.setGridletID(glID);

               conn = new FnbEndToEndPath(destId, super.get_id(),
                                          netServiceType, numPackets, glID, false);

            }
            else if (obj instanceof Object[]) // For the datagrid extension (File)
            {


                String fileName = null;

                try
                {
                    // Add master (from user to resource)

                    fileName = ((File) ((Object[]) obj)[0]).getName();

                }catch(Exception e)
                {

                     fileName = ((String) ((Object[]) obj)[0]);

                }


                /*
                int length = ((Object[])obj).length;
                if (length == 2)
                {
                    // Add master (from user to resource)

                    fileName = ((File) ((Object[]) obj)[0]).getName();

                }// if (length == 2)
                else if (length == 3)
                {
                     // Add master (from res to user)
                     fileName = ((String) ((Object[]) obj)[0]);

                }*/

                glID = checkFilename(fileName); // get a id for the file
                //packGl.setFirstPacketID(pktID_);    // the first packet of this file
                //packGl.setFileID(glID);  // the name of the file

                conn = new FnbEndToEndPath(destId, super.get_id(),
                                           netServiceType, numPackets, glID, true);
                                           //GridSimTags.FNB_PKT_CONTAINS_FILE);

                //System.out.println(super.get_name() + ": fileName: " + fileName);



            }
            else
            {
                conn = new FnbEndToEndPath(destId, super.get_id(),
                                           netServiceType, numPackets);
            }

        }catch(Exception e)
        {
            System.out.println(super.get_name() + ":ERROR.");
            e.printStackTrace();


        }
        // make dummy packets with null data

        convertIntoPacket(MTU, numPackets, tag, conn);

        // last packet contains the actual data
        FnbNetPacket np = null;

        np = new FnbNetPacket(obj, pktID_, size - MTU * (numPackets - 1), tag,
                           conn.getSrc());

        np.setPath(conn);


        /*np = new FnbNetPacket(obj, pktID_, size - MTU * (numPackets - 1), tag,
                           super.get_id(),
                           destId, netServiceType, numPackets, numPackets);*/

        /*if ((obj instanceof Gridlet) || (obj instanceof DataGridlet) ||
            (obj instanceof Object[]))
        {
            packGl.setLastPacketID(pktID_); // the last packet of this gridlet

            packetsGridletsList_.add(packGl); // put this firstLastPacketsGridlet entity into the list

            //System.out.println(">>>>>>>>>> "+super.get_name() +
            //                   ": submitting entity: " + glID + " .(First, last): (" +
            //                   packGl.getFirst() + ", " + packGl.getLast() + ")");

        }*/

        enque(np, GridSimTags.SCHEDULE_NOW);

        pktID_++; // increments packet ID
    }

    /** Returns my_id of the file.
     * @param fname the name of the file
     * @return my_id of the file
     */
    private int checkFilename(String fname)
    {
        FileName_FileMyID fname_fid;
        int last_fileID = 0;
        for (int i =0; i< filesname_fileMyIDs_.size(); i++)
        {
            fname_fid = (FileName_FileMyID) filesname_fileMyIDs_.get(i);

            last_fileID = fname_fid.getFileId();

            if (fname_fid.getFileName().compareTo(fname) == 0)
            {
                return last_fileID;
            }
        }

        fname_fid = new FileName_FileMyID(fname, last_fileID + 1);

        filesname_fileMyIDs_.add(fname_fid);


        return last_fileID + 1;
    }

    /** Returns the file name of the file with the given my_id
     * @param fileID the id of the file
     * @return the name of the file
     */
    private String getFilename(int fileID)
    {
        FileName_FileMyID fname_fid;
        int last_fileID = 0;

        /*System.out.println(super.get_name() + ": getFilename(). fileID " + fileID +
                           ". filesname_fileMyIDs_.size() : " +
                           filesname_fileMyIDs_.size());*/

        for (int i = 0; i < filesname_fileMyIDs_.size(); i++)
        {
            fname_fid = (FileName_FileMyID) filesname_fileMyIDs_.get(i);

            last_fileID = fname_fid.getFileId();

            //System.out.println(super.get_name() + ": last_fileID: " + last_fileID + ". filename: " + fname_fid.getFileName());

            if (last_fileID == fileID)
            {
                return fname_fid.getFileName();
            }
        }

        return null;

    }

    /**
     * Creates many dummy or null packets
     * @param size          packet size (in bytes)
     * @param numPackets    total number of packets to be created
     * @param tag           packet tag
     * @param conn a FnbEndToEndPath defining the end points of the transmission
     * @pre $none
     * @post $none
     */
    private synchronized void convertIntoPacket(long size, int numPackets,
                                                int tag, FnbEndToEndPath conn)
    {
        FnbNetPacket np = null;

        for (int i = 0; i < numPackets - 1; i++)
        {
            // change the tag name for dummy packets, apart from junk packets
            if (tag != GridSimTags.JUNK_PKT)
            {
                tag = GridSimTags.EMPTY_PKT;
            }


            np = new FnbNetPacket(null, pktID_, size, tag,
                           conn.getSrc());

            np.setPath(conn);

            //np = new NetPacket(null, pktID_, size, tag, super.get_id(), destId, netServiceType, i + 1, numPackets);

            pktID_++; // increments packet ID
            enque(np, GridSimTags.SCHEDULE_NOW);
        }
    }

    /**
     * Sends an InfoPacket for ping request
     * @param ev  a Sim_Event object
     * @pre ev != null
     * @post $none
     */
    private synchronized void sendInfoPacket(Sim_event ev)
    {
        IO_data data = (IO_data) ev.get_data();
        Object obj = data.getData();

        // gets all the relevant info
        long size = data.getByteSize();
        int destId = data.getDestID();
        int netServiceType = data.getNetServiceLevel();
        int tag = ev.get_tag();
        String name = GridSim.getEntityName(outPort_.get_dest());

        // we need to packetsize the data, all packets are sent with size MTU
        // only the last packet contains the ping data, the receiver should
        // throw away all other data
        int MTU = link_.getMTU();
        int numPackets = (int) Math.ceil(size / (MTU * 1.0));

        // break ping size into smaller pieces
        // Also, make sure that it is not for pinging itself
        FnbEndToEndPath conn; // = new FnbEndToEndPath(destId, super.get_id(), netServiceType, numPackets);
        int glID;
        if (obj instanceof Gridlet)
        {
            glID = ((Gridlet) obj).getGridletID();
            conn = new FnbEndToEndPath(destId, super.get_id(), netServiceType, numPackets, glID, false);
        }
        else
        {
            conn = new FnbEndToEndPath(destId, super.get_id(), netServiceType, numPackets);
        }


        if (size > MTU && outPort_.get_dest() != destId)
        {
            // make dummy packets with null data
            convertIntoPacket(MTU, numPackets, tag, conn);
        }

        // get the remaining ping size
        size = data.getByteSize() - MTU * (numPackets - 1);

        // create the real InfoPacket
        InfoPacket pkt = new InfoPacket(name, pktID_, size, outPort_.get_dest(),
                                        destId, netServiceType);

        // set the required info
        pkt.setLast(super.get_id());
        pkt.addHop(outPort_.get_dest());
        pkt.addEntryTime(GridSim.clock());
        pkt.setOriginalPingSize(data.getByteSize());

        pktID_++; // increments packet ID
        enque(pkt, GridSimTags.SCHEDULE_NOW);
    }

    /**
     * Sends back the ping() request to the next hop or destination
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void returnInfoPacket(Sim_event ev)
    {
        IO_data data = (IO_data) ev.get_data();
        Object obj = data.getData();

        // use the original ping size rather than IO_data or InfoPacket size
        // since the latter is restricted to MTU
        InfoPacket pkt = (InfoPacket) data.getData();
        long size = pkt.getOriginalPingSize();

        // get other relevant info
        int tag = ev.get_tag();
        int destId = pkt.getSrcID();
        int netServiceType = data.getNetServiceLevel();

        // we need to packetsize the data, all packets are sent with size MTU.
        // only the last packet contains the data, the receiver should
        // throw away all other packets
        int MTU = link_.getMTU();
        int numPackets = (int) Math.ceil(size / (MTU * 1.0));

        // make dummy packets with null data
        //FnbEndToEndPath conn = new FnbEndToEndPath(destId, super.get_id(), netServiceType, numPackets);

        FnbEndToEndPath conn; // = new FnbEndToEndPath(destId, super.get_id(), netServiceType, numPackets);
        int glID;
        if (obj instanceof Gridlet)
        {
            glID = ((Gridlet) obj).getGridletID();
            conn = new FnbEndToEndPath(destId, super.get_id(), netServiceType, numPackets, glID, false);
        }
        else
        {
            conn = new FnbEndToEndPath(destId, super.get_id(), netServiceType, numPackets);
        }



        convertIntoPacket(MTU, numPackets, tag, conn);

        // set the original packet of last hop into this entity id
        pkt.setLast(super.get_id());
        enque(pkt, GridSimTags.SCHEDULE_NOW);
    }

    /**
     * Takes a packet, adds it into a buffer and schedules it to be sent out at
     * an appropriate time.
     *
     * @param pkt   The packet to be buffered
     * @param delay The length of time this packet should be delayed (exclusive
     *              of the transmission time)
     * @pre pkt != null
     * @pre delay > 0
     * @post $none
     */
    private synchronized void enque(Packet pkt, double delay)
    {
        packetList_.add(pkt);
        if (packetList_.size() == 1)
        {
            double total = delay + (pkt.getSize() * SIZE / link_.getBaudRate());
            super.sim_schedule(super.get_id(), total, GridSimTags.SEND_PACKET);
        }
    }

    /**
     * Removes a single packet from the buffer and sends it down the link.
     * Then, schedules the next packet in the list.
     * @pre $none
     * @post $none
     */
    private synchronized void sendPacket()
    {
        if (packetList_ == null || packetList_.isEmpty() == true)
        {
            return;
        }

        // submits the first packet in the list
        Packet np = (Packet) packetList_.remove(0);

        boolean ping = false; // a flag to determine ping packet or not
        int tag = -1; // event tag ID
        int dest = -1; // destination ID

        // if a packet belongs to a ping packet
        if (np instanceof InfoPacket)
        {
            ((InfoPacket) np).addExitTime(GridSim.clock());
            ((InfoPacket) np).addBaudRate(link_.getBaudRate());
            ping = true;
        }

        // if an entity tries to send a packet to itself
        if (np.getDestID() == outPort_.get_dest())
        {
            // then change the destination name and id
            String destName = super.get_name();
            destName = destName.replaceFirst("Output", "Input");
            dest = Sim_system.get_entity_id(destName);

            // for a ping packet, change the tag
            if (ping == true)
            {
                // don't forget to change the packet tag
                tag = GridSimTags.INFOPKT_RETURN;
                ((InfoPacket) np).setTag(tag);
            }
            else
            {
                tag = np.getTag();
            }
        }
        else // if an entity tries to send to other entity
        {
            // change or keep the event tag
            tag = GridSimTags.PKT_FORWARD;
            if (np.getTag() == GridSimTags.JUNK_PKT)
            {
                tag = GridSimTags.JUNK_PKT;
            }

            // sends the packet into the link
            dest = link_.get_id();
        }

        // send the packet
        super.sim_schedule(dest, GridSimTags.SCHEDULE_NOW, tag, np);

        /*****   // DEBUG info
                 System.out.println(super.get_name() + " send to " +
                GridSim.getEntityName(dest) + " tag = " + tag);
         ****/

        // if the list is not empty, then schedule the next packet in the list
        if (packetList_.isEmpty() != true)
        {
            double delay = np.getSize() * SIZE / link_.getBaudRate();
            super.sim_schedule(super.get_id(), delay, GridSimTags.SEND_PACKET);
        }
    }

    /** This functions returns the packetsGridletsList_. This is used by the user
     * to know which gridlet a packet belongs to.
     * @return the packetsGridletsList_ of this output entity
     */
    /*********************
    private ArrayList getPacketsGridletsList()
    {
        return packetsGridletsList_;
    }
    *********************/

    /** This function returns the entity (gridlet/file) to which a packet belongs
     * @param pktID the ID of the packet of interest
     * @return a FnbMessage object
     */
    /*********************
    private FnbMessage lookForEntity(int pktID)
    {
        FnbMessage msgDrop = null; // = new FnbMessage(9999, false);
        for (int i = 0; i < packetsGridletsList_.size(); i++)
        {
            firstLastPacketsGridlet flPktGl = (firstLastPacketsGridlet)
                                              packetsGridletsList_.get(i);

            if ( (flPktGl.getFirstPacketID() <= pktID) &&
                 (flPktGl.getLastPacketID() > pktID) )
            {
                if (flPktGl.isFile() == true) {
                    msgDrop = new FnbMessageDropFile(flPktGl.getID(),
                                        getFilename(flPktGl.getID()) );
                }
                //else {
                //    msgDrop = new FnbMessageDropGridlet( flPktGl.getID() );
                //}


                //System.out.println(super.get_name() + ": lookForEntity: entiyID: " +
                //                   flPktGl.getGridletID() + ". isFile " + flPktGl.getIsFile() +
                //                   ". filename " + getFilename(msgDrop.getEntityID()));


                return msgDrop;
            }
        }

        return msgDrop;// if there is no entity, return this
    }
    *********************/

} // end class


