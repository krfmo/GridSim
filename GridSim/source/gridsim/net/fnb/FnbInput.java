/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero (based on Input.java)
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 *
 * This class is based on Input class, by Manzur Murshed and Rajkumar Buyya.
 * Things added or modifyed:
 *    - getDataFromLink(...)
 *    - source_PktNum_array
 *    - FnbNetPacket instead of NetPacket
 */

package gridsim.net.fnb;

import gridsim.net.fnb.*;
import gridsim.*;
import gridsim.net.*;
import eduni.simjava.*;
import java.util.*;
import java.io.FileWriter;
import gridsim.util.TrafficGenerator;

/**
 * Thiss class defines a port through which a simulation entity
 * receives data from the simulated network. Note that this class is based on
 * {@link gridsim.net.Input} class.
 * <p>
 * It maintains an event queue
 * to serialize the data-in-flow and delivers to its parent entity.
 * It accepts messages that comes from GridSim entities 
 * {@link gridsim.net.fnb.FnbOutput} entity
 * and passes the same to the GridSim entity.
 * It simulates Network communication delay depending on Baud rate
 * and data length. Simultaneous inputs can be modeled using multiple
 * instances of this class.
 *
 * @author  Agustin Caminero, Universidad de Castilla La Mancha (Spain). 
 * @since GridSim Toolkit 4.2
 * @see gridsim.net.Input
 */
public class FnbInput extends Sim_entity implements NetIO
{
    private Sim_port inPort_;
    private Link link_;
    private double baudRate_;
    private static final int BITS = 8;   // 1 byte = 8 bits

    private ArrayList source_PktNum_array;

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
    public FnbInput(String name, double baudRate) throws NullPointerException
    {
        super(name);
        this.baudRate_ = baudRate;
        link_= null;

        inPort_ = new Sim_port("input_buffer");
        super.add_port(inPort_);

        source_PktNum_array = new ArrayList();
    }

    /**
     * Sets the Input entities link. This should be used only if the network
     * extensions are being used.
     * @param link the link to which this Input entity should send data
     * @pre link != null
     * @post $none
     */
    public void addLink(Link link) {
        this.link_ = link;
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
                    (io.getByteSize() * BITS) / minBaudRate);

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

            /*System.out.println(super.get_name() + ": >>>> FnbInput. PktID: " +
                               ((FnbNetPacket) pkt).getID() + ". glID: " +
                               ((FnbNetPacket) pkt).getGlID());*/

            if (pkt instanceof InfoPacket)
            {
                processPingRequest( (InfoPacket) pkt);
                return;
            }

            source_pktNum srcPktNum;
            // all except last packet in a data session are null packets
            if (pkt instanceof FnbNetPacket)
            {
                FnbNetPacket np = (FnbNetPacket) pkt;
                int tag = np.getTag();

                // ignore incoming junk packets
                if (tag == GridSimTags.JUNK_PKT) {
                    return;
                }


                // We have to count the gridlet packets arriving at a resource/user,
                // so that we make sure all the packets belonging to a gridlet arrive.
                // If any of those packets of a gridlet don't arrive, then the gridlet is failed.
                // In that case, the router where those packets have been dropped will have told
                // the user about the dropping.
                String name = super.get_name();


                int src_outputPort = ((FnbNetPacket) np).getSrcID();
                //String src_outputPort_str = GridSim.getEntityName(src_outputPort);

                /********
                // Uncomment this for more info on the progress of sims
                if (name.compareTo("Input_SIM_0_Res_0") == 0)
                    System.out.println(super.get_name() +
                                       ": packet arrived to the res" +
                                       ". Pkt num: " +
                                       ((FnbNetPacket) np).getPacketNum() +
                                       " from " + src_outputPort_str);
                ***********/

                //int pktID = ((FnbNetPacket) np).getID();
                //int PrevPktNum; // The pkt Num of the previous packet
                int pktNum = ((FnbNetPacket) np).getPacketNum();
                int glID = ((FnbNetPacket) np).getObjectID();
                srcPktNum = lookForSrcPktNum(src_outputPort, glID);

                if (srcPktNum == null)
                {
                    // Not correct anymore
                    // Remove form the source_PktNum_array the items whose src is the
                    // src_outputPort, as those gridlets will be failed (dropped packets)
                    // removeFromSrcPktNum(src_outputPort);

                    // We create a new source_pktNum object only if the packet is the first in this gridlet.
                    // This means that if the gridlet is failed (as some packets have been dropped),
                    // no source_pktNum wil be created.

                    if (pktNum == 1)
                    {
                        srcPktNum = new source_pktNum(src_outputPort, glID);

                        source_PktNum_array.add(srcPktNum);

                        /*System.out.println(super.get_name() +
                                ": >>>> FnbInput. First pkt of a gl has just arrived . PktID: " +
                                           ((FnbNetPacket) pkt).getID() + ". glID: " +
                                           ((FnbNetPacket) pkt).getGlID());*/

                    }
                }//if (srcPktNum == null)

                if (srcPktNum != null)
                {
                    // If srcPktNum != null this means that the srcPktNum object is correct.
                    // We have not lost any packet, so the gridlet is ok up to now.
                    // Hence, update the pktNum in the array.
                    //srcPktNum.setPktNum(pktNum);
                    //srcPktNum.setPktID(pktID);

                    // Increase the number of pkts already received
                    //srcPktNum.setNumOfPkts(srcPktNum.getNumOfPkts() + 1);
                    srcPktNum.addNumOfArrivedPkts();

                    // If this is the last packet of the gridlet, then the gridlet is ok
                    int totalPkt = ((FnbNetPacket) np).getTotalPackets();
                    if (srcPktNum.getNumOfPacket() == totalPkt)
                        srcPktNum.setStatus(true);



                    // ignore incoming null dummy packets
                    if (tag == GridSimTags.EMPTY_PKT && np.getData() == null)
                    {
                        return;
                    }
                    else
                    {
                        // This is the last packet in the gridlet, so we have to check
                        // if the previous packets have arrived.
                        if (srcPktNum.getStatus() == true)
                        {
                            // The gridlet has arrived perfect, with no packet lost

                            // convert the packets into IO_data
                            Object data = np.getData();
                            IO_data io = new IO_data(data, np.getSize(),
                                    inPort_.get_dest());

                            // send the data into entity input port
                            super.sim_schedule(inPort_,
                                               GridSimTags.SCHEDULE_NOW,
                                               tag,
                                               io.getData());

                           /*// REMOVE!!!
                            System.out.println("\n*********" + super.get_name() +
                                               ": Data (maybe a gridlet) arrived. Pkt num: " +
                                               ((FnbNetPacket) np).getPacketNum() +
                                               " from " + src_outputPort_str +
                                               "\n");*/

                            /***********  // NOTE: redundant
                            name = super.get_name();
                            if (name.indexOf("Input_SIM_0_Res_5") != -1)
                            {
                                fw_write(
                                        "Data (maybe a gridlet) arrived at the Resource\n",
                                        super.get_name());

                                System.out.println("\n*********" +
                                        super.get_name() +
                                        ": Data (maybe a gridlet) arrived at the Resource. Pkt num: " +
                                        ((FnbNetPacket) np).getPacketNum() +
                                        " from " + src_outputPort_str +
                                        "\n");
                            }
                            ***********************/

                        } // if (srcPktNum.getOk() == true)

                    } // else of the if (tag == GridSimTags.EMPTY_PKT && np.getData() == null)

                }//if (srcPktNum != null)

            }//  if (pkt instanceof FnbNetPacket)

        }// if (obj instanceof Packet)

    }

    /**
     * Look for a especific source_pktNum object in the source_PktNum_array
     * @param src the source of the packet
     * @param glID the id of the girdlet this packet belongs to.
     * @return a source_pktNum object whose source is src, null otherwise
     * */
    public source_pktNum lookForSrcPktNum(int src, int glID)
    {

        source_pktNum srcPktNum;
        for (int i = 0; i < source_PktNum_array.size(); i++)
        {
            srcPktNum = (source_pktNum) source_PktNum_array.get(i);

            if ((srcPktNum.getSourceID() == src) && (srcPktNum.getGridletID() == glID))
                return srcPktNum;
        }

        return null;

    }

    /**
     * Look for a especific source_pktNum object in the source_PktNum_array
     * @param src the source of the packet
     * */
    public void removeFromSrcPktNum(int src)
    {

        source_pktNum srcPktNum;
        for (int i = 0; i < source_PktNum_array.size(); i++)
        {
            srcPktNum = (source_pktNum) source_PktNum_array.get(i);

            if (srcPktNum.getSourceID() == src)
               source_PktNum_array.remove(i);
        }

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

