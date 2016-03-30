/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2002, The University of Melbourne, Australia 
 */

package gridsim.net;

import gridsim.*;
import gridsim.net.*;
import gridsim.util.TrafficGenerator;
import java.util.Collection;
import eduni.simjava.*;

/**
 * GridSim Input class defines a port through which a simulation entity
 * receives data from the simulated network.
 * <p>
 * It maintains an event queue
 * to serialize the data-in-flow and delivers to its parent entity.
 * It accepts event messages that comes from the 'Output' entity,
 * and passes the event data to the GridSim entity.
 * It simulates Network communication delay depending on Baud rate
 * and data length. Simultaneous inputs can be modeled using multiple
 * instances of this class.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since   GridSim Toolkit 1.0
 * @invariant $none
 */
public class Input extends Sim_entity implements NetIO
{
    private Sim_port inPort_;
    private Link link_;
    private double baudRate_;

    /**
     * Allocates a new Input object
     * @param name         the name of this object
     * @param baudRate     the communication speed
     * @throws NullPointerException This happens when creating this entity
     *                  before initializing GridSim package or this entity name
     *                  is <tt>null</tt> or empty
     * @pre name != null
     * @pre baudRate >= 0.0
     * @post $none
     */
    public Input(String name, double baudRate) throws NullPointerException
    {
        super(name);
        this.baudRate_ = baudRate;
        link_= null;

        inPort_ = new Sim_port("input_buffer");
        super.add_port(inPort_);
    }

    /**
     * Sets the Input entities link. This should be used only if the network
     * extensions are being used.
     * @param link the link to which this Input entity should send data
     * @pre link != null
     * @post $none
     */
    public void addLink(Link link)
    {
        this.link_ = link;
        baudRate_ = link_.getBaudRate();
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
     * @param value   the specified value
     * @return real number
     * @pre value >= 0.0
     * @post $result >= 0.0
     */
    public double realIO(double value) {
        return GridSimRandom.realIO(value);
    }

    /**
     * This is an empty method and only applicable to
     * {@link gridsim.net.Output} class.
     * @param gen       a background traffic generator
     * @param userName  a collection of user entity name (in String object).
     * @return <tt>false</tt> since this method is not used by this class.
     * @pre gen != null
     * @pre userName != null
     * @post $none
     * @see gridsim.net.Output
     */
    public boolean setBackgroundTraffic(TrafficGenerator gen,
                                        Collection userName)
    {
        return false;
    }

    /**
     * This is an empty method and only applicable to
     * {@link gridsim.net.Output} class.
     * @param gen   a background traffic generator
     * @return <tt>false</tt> since this method is not used by this class.
     * @pre gen != null
     * @post $none
     * @see gridsim.net.Output
     */
    public boolean setBackgroundTraffic(TrafficGenerator gen)
    {
        return false;
    }

    /**
     * A method that gets one process event at one time until the end
     * of a simulation, then delivers an event to the entity (its parent)
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // Process events
        Object obj = null;
        while ( Sim_system.running() )
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev);     // get the next event in the queue
            obj = ev.get_data();        // get the incoming data

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                break;
            }

            // if this entity is not connected in a network topology
            if (obj != null && obj instanceof IO_data) {
                getDataFromEvent(ev);
            }

            // if this entity belongs to a network topology
            else if (obj != null && link_ != null) {
                getDataFromLink(ev);
            }

            ev = null;   // reset to null for gc to collect
        }
    }

    /**
     * Process incoming event for data without using the network extension
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void getDataFromEvent(Sim_event ev)
    {
        IO_data io = (IO_data) ev.get_data();

        // if the sender is not part of the overall network topology
        // whereas this entity is, then need to return back the data,
        // since it is not compatible.
        if (link_ != null)
        {
            // outName = "Output_xxx", where xxx = sender entity name
            String outName = GridSim.getEntityName( ev.get_src() );

            // NOTE: this is a HACK job. "Output_" has 7 chars. So,
            // the idea is to get only the entity name by removing
            // "Output_" word in the outName string.
            String name = outName.substring(7);

            // if the sender is not system GIS then ignore the message
            if (GridSim.getEntityId(name) != GridSim.getGridInfoServiceEntityId())
            {
                // sends back the data to "Input_xxx", where
                // xxx = sender entity name. If not sent, then the sender
                // will wait forever to receive this data. As a result,
                // the whole simulation program will be hanged or does not
                // terminate successfully.
                int id = GridSim.getEntityId("Input_" + name);
                super.sim_schedule(id, 0.0, ev.get_tag(), io);

                // print an error message
                System.out.println(super.get_name() + ".body(): Error - " +
                    "incompatible message protocol.");
                System.out.println("    Sender: " + name + " is not part " +
                    "of this entity's network topology.");
                System.out.println("    Hence, sending back the received data.");
                System.out.println();
                return;
            }
        }

        // NOTE: need to have a try-catch statement. This is because,
        // if the above if statement holds, then Input_receiver will send
        // back to Input_sender without going through Output_receiver entity.
        // Hence, a try-catch is needed to prevent exception of wrong casting.
        try
        {
            // Simulate Transmission Time after Receiving
            // Hold first then dispatch
            double senderBaudRate = ( (Output)
                    Sim_system.get_entity(ev.get_src()) ).getBaudRate();

            // NOTE: io is in byte and baud rate is in bits. 1 byte = 8 bits
            // So, convert io into bits
            double minBaudRate = Math.min(baudRate_, senderBaudRate);
            double communicationDelay = GridSimRandom.realIO(
                    (io.getByteSize() * NetIO.BITS) / minBaudRate);

            // NOTE: Below is a deprecated method for SimJava 2
            //super.sim_hold(communicationDelay);
            super.sim_process(communicationDelay);   // receiving time
        }
        catch (Exception e) {
            // .... empty
        }

        // Deliver Event to the entity (its parent) to which
        // it is acting as buffer
        super.sim_schedule( inPort_, GridSimTags.SCHEDULE_NOW,
                ev.get_tag(), io.getData() );
    }

    /**
     * Process incoming events from senders that are using the network
     * extension
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void getDataFromLink(Sim_event ev)
    {
        Object obj = ev.get_data();
        if (obj instanceof Packet)
        {
            // decrypt the packet into original format
            Packet pkt = (Packet) ev.get_data();

            if (pkt instanceof InfoPacket)
            {
                processPingRequest( (InfoPacket) pkt);
                return;
            }

            // all except last packet in a data session are null packets
            if (pkt instanceof NetPacket)
            {
                NetPacket np = (NetPacket) pkt;
                int tag = np.getTag();

                // ignore incoming junk packets
                if (tag == GridSimTags.JUNK_PKT) {
                    np = null;
                    return;
                }

                // ignore incoming null dummy packets
                if (tag == GridSimTags.EMPTY_PKT && np.getData() == null) {
                    np = null;
                    return;
                }

                // convert the packets into IO_data
                Object data = np.getData();
                IO_data io = new IO_data( data, np.getSize(),
                                          inPort_.get_dest() );

                // send the data into entity input port
                super.sim_schedule(inPort_, GridSimTags.SCHEDULE_NOW, tag,
                                   io.getData() );
            }
        }
    }

    /**
     * Processes a ping request
     * @param   pkt     a packet for pinging
     * @pre pkt != null
     * @post $none
     */
    private void processPingRequest(InfoPacket pkt)
    {
        // add more information to ping() packet
        pkt.addHop( inPort_.get_dest() );
        pkt.addEntryTime( GridSim.clock() );

        IO_data io = new IO_data( pkt, pkt.getSize(), inPort_.get_dest() );

        // send this ping() packet to the entity
        super.sim_schedule(inPort_, GridSimTags.SCHEDULE_NOW,
                           pkt.getTag(), io.getData());
    }

} // end class

