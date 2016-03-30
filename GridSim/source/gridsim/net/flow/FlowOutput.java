/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2008, The University of Melbourne, Australia
 * Author: James Broberg (based on Output.java)
 */

package gridsim.net.flow;

import gridsim.*;
import gridsim.net.*;
import gridsim.util.*;

import eduni.simjava.*;

import java.util.*;
import java.util.Random;


/**
 * GridSim FlowOutput defines a port through which a simulation entity sends
 * data to the simulated network.
 * <p>
 * It maintains an event queue to serialize
 * the data-out-flow and delivers to the destination entity.
 * It works along with FlowInput entity to simulate network
 * communication delay. Simultaneous outputs can be modeled by using multiple
 * instances of this class
 *
 * @author       James Broberg
 * @since        GridSim Toolkit 4.2
 * @invariant $none
 */
public class FlowOutput extends Sim_entity implements NetIO
{
    private Sim_port outPort_;      // output port
    private Link link_;             // a link to this output entity
    private double baudRate_;       // baud rate of this entity
    private final int SIZE = 8;     // 1 byte in bits
    private static int pktID_ = 0;             // packet ID counter
    private Vector flowList_;     // store a list of packets
    private Random random_;         // selects to which junk packets go to
    private TrafficGenerator gen_;  // background traffic generator
    private ArrayList list_;        // list of resources + user entities
    private boolean hasStarted_;    // a flag for background traffic has started

    private Random rnd;				// Random number generator to generate unique
                                    // flow ID's


    /**
     * Allocates a new FlowOutput object
     * @param name         the name of this object
     * @param baudRate     the communication speed
     * @throws NullPointerException This happens when creating this entity
     *                  before initializing GridSim package or this entity name
     *                  is <tt>null</tt> or empty
     * @pre name != null
     * @pre baudRate >= 0.0
     * @post $none
     */
    public FlowOutput(String name, double baudRate) throws NullPointerException
    {
        super(name);
        this.baudRate_ = baudRate;
        link_ = null;
        flowList_ = null;
        pktID_ = 0;

        outPort_ = new Sim_port("output_buffer");
        super.add_port(outPort_);

        // for sending background traffic
        gen_  = null;
        list_ = null;
        random_ = null;
        hasStarted_ = false;

        rnd = new Random();
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
        if (gen == null) {
            return false;
        }

        gen_ = gen;
        if (list_ == null) {
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
        if (gen == null || userName == null) {
            return false;
        }

        boolean flag = true;
        try
        {
            gen_ = gen;
            if (list_ == null) {
                list_ = new ArrayList();
            }

            // iterates through each list to check whether it is a valid
            // entity name or not
            Iterator it = userName.iterator();
            int id = -1;
            while( it.hasNext() )
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
        }
        catch(Exception e) {
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
        baudRate_ = link_.getBaudRate();
        flowList_ = new Vector();
    }

    /**
     * Gets the baud rate
     * @return the baud rate
     * @pre $none
     * @post $result >= 0.0
     */
    public double getBaudRate() {
        return baudRate_;
    }

    /**
     * Gets the I/O real number based on a given value
     * @param value  the specified value
     * @return real number
     * @pre $none
     * @post $result >= 0.0
     */
    public double realIO(double value) {
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
        while ( Sim_system.running() )
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev);     // get the next event in the queue

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                break;
            }

            //System.out.println(super.get_name() + ".body(): ev.get_tag() is " + ev.get_tag());
            //System.out.println(super.get_name() + ".body(): ev.get_src() is " + ev.get_src());

            // handle different types of incoming events
            switch ( ev.get_tag() )
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

                default:
                    defaultSend(ev, gisID, statID, shutdownID);
                    break;
            }
        }
    }

    /**
     * Generates few junk flows at the given interval
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
            list_.addAll(resList);  // add resource IDs into the current list

            // sets the sending pattern
            if (pattern == TrafficGenerator.SEND_ONE_ONLY && random_ == null) {
                random_ = new Random();
            }
        }

        // get the required info for generating this background traffic
        long size = gen_.getNextPacketSize();   // packet size
        long freq = gen_.getNextPacketFreq();   // packet freq
        int type = gen_.getServiceType();       // packet type
        int tag = GridSimTags.JUNK_PKT;         // packet tag


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
            int index = random_.nextInt( list_.size() );
            destId = ((Integer) list_.get(index)).intValue();

            /*********   // DEBUG info
            System.out.println(super.get_name() + ": Destination id = " +
                destId + " = " + GridSim.getEntityName(destId) );
            *********/

            convertIntoPacket(size, 1, tag, destId, type);


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

                convertIntoPacket(size, 1, tag, destId, type);

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
        if (gen_ == null) {
            return;
        }

        // get the next inter-arrival time
        long time = gen_.getNextPacketTime();
        System.out.println(super.get_name() +
                ": background traffic will start at time " + time);

        // starts background traffic if the inter-arrival time is valid
        if (time == -1) {
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
        IO_data io = (IO_data) ev.get_data();
        int destId = io.getDestID();

        /*****   // DEBUG info
        System.out.println(super.get_name() + ".defaultSend(): Send to " +
            GridSim.getEntityName(destId) + " tag = " + ev.get_tag() + " at time = " + GridSim.clock());
        /*****/

        // if this entity uses a network extension
        if (link_ != null && destId != gisID && destId != statID &&
            destId != shutdownID)
        {
            //System.out.println(super.get_name() + ".defaultSend(): submitToLink() + at time = " + GridSim.clock());
            submitToLink(ev);
            return;
        }

        // Identify ID of an entity which acts as Input/Buffer
        // entity of destination entity
        int id = GridSim.getEntityId( "Input_" +
                 Sim_system.get_entity(destId).get_name() );

        // Send first and then hold
        super.sim_schedule(id, GridSimTags.SCHEDULE_NOW, ev.get_tag(), io);

        double receiverBaudRate = ( (FlowInput)
                Sim_system.get_entity(id) ).getBaudRate();

        // NOTE: io is in byte and baud rate is in bits. 1 byte = 8 bits
        // So, convert io into bits
        double minBaudRate = Math.min(baudRate_, receiverBaudRate);
        double communicationDelay = GridSimRandom.realIO(
                (io.getByteSize() * NetIO.BITS) / minBaudRate);

        // NOTE: Below is a deprecated method for SimJava 2
        //super.sim_hold(communicationDelay);
        super.sim_process(communicationDelay);
    }

    /**
     * This method takes data from an entity. The data is encapsulated in a single FlowPacket.
     * After this it calls enque() to queue these flows into its
     * buffer.
     *
     * @param ev    A Sim_event data that contains all the data for this method
     *              to do its task.
     * @pre ev != null
     * @post $none
     */
    private synchronized void submitToLink(Sim_event ev)
    {
        IO_data data = (IO_data) ev.get_data();
        Object obj = data.getData();
        long size = data.getByteSize();
        int tag = ev.get_tag();
        int destId = data.getDestID();
        int netServiceType = data.getNetServiceLevel();

        // last packet contains the actual data
        FlowPacket np = null;
        np = new FlowPacket(obj,rnd.nextInt(Integer.MAX_VALUE),size,tag,super.get_id(),
                           destId, netServiceType, 1, 1);


        //System.out.println("Sending flow packet to link at time = " + GridSim.clock() + " id is " + np.getID());
        enque(np, GridSimTags.SCHEDULE_NOW);
    }

    /**
     * Creates many dummy or null packets
     * @param size          packet size (in bytes)
     * @param numPackets    total number of packets to be created
     * @param tag           packet tag
     * @param destId        destination ID for sending the packet
     * @param netServiceType    level type of service for the packet
     * @pre $none
     * @post $none
     */
    private synchronized void convertIntoPacket(long size, int numPackets,
            int tag, int destId, int netServiceType) {
    FlowPacket np = null;
    for (int i = 0; i < numPackets - 1; i++)
    {
        // change the tag name for dummy packets, apart from junk packets
        if (tag != GridSimTags.JUNK_PKT) {
            tag = GridSimTags.EMPTY_PKT;
        }

        np = new FlowPacket(null, rnd.nextInt(10000000), size, tag, super.get_id(), destId,
                           netServiceType, i+1, numPackets);

        //pktID_++;     // increments packet ID
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

        // gets all the relevant info
        long size = data.getByteSize();
        int destId = data.getDestID();
        int netServiceType = data.getNetServiceLevel();
        int tag = ev.get_tag();
        String name = GridSim.getEntityName( outPort_.get_dest() );

        // we need to packetsize the data, all packets are sent with size MTU
        // only the last packet contains the ping data, the receiver should
        // throw away all other data
        int MTU = link_.getMTU();
        int numPackets = (int) Math.ceil( size / (MTU * 1.0) );

        // break ping size into smaller pieces
        // Also, make sure that it is not for pinging itself
        if (size > MTU && outPort_.get_dest() != destId)
        {
            // make dummy packets with null data
            convertIntoPacket(MTU, numPackets, tag, destId, netServiceType);
        }

        // get the remaining ping size
        size = data.getByteSize() - MTU*(numPackets-1);

        // create the real InfoPacket
        InfoPacket pkt = new InfoPacket(name,pktID_,size,outPort_.get_dest(),
                                        destId,netServiceType);

        // set the required info
        pkt.setLast( super.get_id() );
        pkt.addHop( outPort_.get_dest() );
        pkt.addEntryTime( GridSim.clock() );
        pkt.setOriginalPingSize( data.getByteSize() );

        pktID_++;   // increments packet ID
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
        int numPackets = (int) Math.ceil( size / (MTU * 1.0) );

        // make dummy packets with null data
        convertIntoPacket(MTU, numPackets, tag, destId, netServiceType);

        // set the original packet of last hop into this entity id
        pkt.setLast( super.get_id() );
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
        flowList_.add(pkt);
        if (flowList_.size() == 1)
        {
            //System.out.println(super.get_name() + ".enque() Size is " + pkt.getSize() + " baud " + link_.getBaudRate());
            double total = 0.0;

            //System.out.println(super.get_name() + ".enque() Time now " + GridSim.clock() + " delay is " + total);
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
        if (flowList_ == null || flowList_.isEmpty() == true) {
            return;
        }

        // submits the first packet in the list
        Packet np = (Packet) flowList_.remove(0);

        boolean ping = false;   // a flag to determine ping packet or not
        int tag = -1;       // event tag ID
        int dest = -1;      // destination ID

        // if a packet belongs to a ping packet
        if (np instanceof InfoPacket)
        {
            ((InfoPacket)np).addExitTime( GridSim.clock() );
            ((InfoPacket)np).addBaudRate( link_.getBaudRate() );
            ping = true;
        }

        // if an entity tries to send a packet to itself
        if ( np.getDestID() == outPort_.get_dest() )
        {
            //System.out.println("Sending packet to self!");
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
            else {
                tag = np.getTag();
            }
        }
        else  // if an entity tries to send to other entity
        {
            // change or keep the event tag
            tag = GridSimTags.PKT_FORWARD;
            if (np.getTag() == GridSimTags.JUNK_PKT) {
                tag = GridSimTags.JUNK_PKT;
            }

            // sends the packet into the link
            dest = link_.get_id();
        }

        // send the packet
        super.sim_schedule(dest, GridSimTags.SCHEDULE_NOW, tag, np);

        /*****   // DEBUG info **
        System.out.println(super.get_name() + " send to " +
                GridSim.getEntityName(dest) + " tag = " + tag + " time " + GridSim.clock());
        /****/

        // if the list is not empty, then schedule the next packet in the list
        if (flowList_.isEmpty() != true)
        {
            //double delay = np.getSize() * SIZE / link_.getBaudRate();
            double delay = 0.0;
            super.sim_schedule(super.get_id(), delay, GridSimTags.SEND_PACKET);
        }
    }

} // end class

