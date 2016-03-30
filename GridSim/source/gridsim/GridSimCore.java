/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.*;
import gridsim.net.*;
import gridsim.util.*;
import java.util.Collection;
import gridsim.net.flow.*;
import gridsim.net.fnb.*;

/**
 * Since GridSim version 3.0, this is the overall class of GridSim package,
 * that must be extended by other GridSim
 * entities. It inherits event management and threaded entity features from
 * the {@link eduni.simjava.Sim_entity} class. This class adds networking and
 * event delivery features, which allow synchronous or asynchronous
 * communication for service access or delivery.
 * <p>
 * All classes that extend this class can implement a method called
 * {@link eduni.simjava.Sim_entity#body()}, which is automatically invoked
 * when a simulation runs, since it is expected to be
 * responsible for simulating entity behavior. In addition,
 * {@link eduni.simjava.Sim_entity#body()} method is
 * the place to receive incoming events.
 * <p>
 * The entities that extend this class can be instantiated <i>with</i> or
 * <i>without</i> networked I/O ports. A networked GridSim entity gains
 * communication capability via the objects of GridSim's I/O entity classes,
 * {@link gridsim.net.Input} and {@link gridsim.net.Output} classes. Each I/O
 * entity will have a unique name assuming each GridSim entity that the user
 * creates has a unique name. For example, a resource entity with the name
 * <b>Resource2</b>,
 * will have an input entity whose name is prefixed with <b>Input_</b>,
 * making the input entity's full name <b>Input_Resource2</b>, which is
 * expected to be unique. <b>Input_Resource2</b> entity handles all incoming
 * events. A resource has an output entity whose name is prefixed with
 * <b>Output_</b>, e.g. <b>Output_Resource2</b>. <b>Output_Resource2</b>
 * entity handles all outgoing events.
 * <p>
 * The I/O entities are concurrent entities, but they
 * are visible within the GridSim entity and are able to communicate with
 * other GridSim entities by sending messages.
 * <p>
 * There are two ways to send a message or object to an entity, i.e. with or
 * without I/O port. Below shows the differences:
 * <ul>
 *      <li>using {@link #send(int, double, int, Object)},
 *          {@link #send(int, double, int)}, {@link #send(String,double,int)},
 *          or {@link #send(String, double, int, Object)} method.
 *          <p>
 *          These methods will send an event directly to the destination entity
 *          <b>without</b> using I/O port. Regardless how big the size of the
 *          Object data is, <br>
 *          <i>event arrival time = current simulation time + delay time</i>
 *          <p>
 *          For example: <tt>send(destID, delay, tag, obj)</tt>. <br>
 *          On the destinated entity, by creating a Sim_event object:
 *          <ul>
 *              <li> Sim_event.get_data() is obj of type Object. NOTE:
 *                   Object can be a generic type, such as IO_data, Gridlet,
 *                   String, etc.
 *              <li> Sim_event.get_dest() is destID of type int.
 *              <li> Sim_event.get_src() is sender ID of type int. NOTE:
 *                   sender ID is the entity ID that uses/calls this method.
 *              <li> Sim_event.get_tag() is tag of type int.
 *              <li> Sim_event.get_scheduled_by() is sender ID of type int.
 *          </ul>
 *      <br>
 *
 *      <li>using {@link #send(Sim_port, double, int, Object)} or
 *          {@link #send(Sim_port, double, int)} method.
 *          <p>
 *          These methods will send an event to the destination entity
 *          <b>with</b> I/O port. The size of the Object data sent is taken
 *          into a consideration for determining the event arrival time.
 *          <p>
 *          For example: <tt>send(ouputPort, delay, tag, obj)</tt> where
 *          obj <b>MUST</b> be of type <tt>IO_data</tt>. <br>
 *          On the destinated entity, by creating a Sim_event object:
 *          <ul>
 *              <li> Sim_event.get_data() is (IO_data) obj.getData() of type
 *                   Object. NOTE:
 *                   Object can be a generic type, such as Gridlet, String,
 *                   Integer, etc.
 *              <li> Sim_event.get_dest() is (IO_data) obj.getDestID() of
 *                   type int.
 *              <li> Sim_event.get_src() is an entity ID of type int.
 *                   <b>NOTE:</b> entity ID is not from the sender ID, but from
 *                   <tt>Input_xxx</tt>, where xxx = destinated entity name.
 *                   For example, to send to <tt>Resource2</tt>, the entity ID
 *                   is an ID of <tt>Input_Resource2</tt>.
 *              <li> Sim_event.get_tag() is tag of type int.
 *              <li> Sim_event.get_scheduled_by() is an entity ID of type int.
 *                   Same explanation of Sim_event.get_src().
 *          </ul>
 *          <br>
 *          <b>NOTE:</b> When sending using I/O port, object must be of type
 *             <tt>IO_data</tt>, otherwise <tt>ClassCastException</tt> will be
 *             reported on {@link gridsim.net.Output#body()} method.
 *
 * </ul>
 * <p>
 * Since GridSim 3.1, a network extension has been incorporated into
 * this simulation infrastructure. To make use of this, you need to create a
 * resource entity <b>only</b> using {@link #GridSimCore(String, Link)}
 * constructors.
 * Then you need to attach this entity into the overall network topology, i.e.
 * connecting this entity to a router, etc. See the examples provided
 * in the package for more details.
 * <p>
 * Another important feature of a network extension is the ability to
 * ping to a particular entity by using {@link #ping(int, int)} or
 * {@link #pingBlockingCall(int, int)} overloading methods.
 * <p>
 * However, there are few conditions to be met:
 * <ul>
 * <li> only work for entities which are connected through a network topology
 *      using the network extension entities, such as {@link gridsim.net.Link}
 *      and {@link gridsim.net.Router}.
 * <li> pinging to itself is permitted but the <tt>Round Trip Time</tt> is
 *      always 0 (zero).
 * <li> <b>most importantly</b>, you need to implement the below source code
 *      for the incoming ping request to work. This is required <b>only</b> for
 *      an entity that extends from {@link gridsim.GridSim} or
 *      {@link gridsim.GridSimCore}. <br>
 *      NOTE: {@link gridsim.GridResource} class is not effected as it
 *            already has the below code. <br><br><br>
 *
 * <code>
 * ... // other code <br>
 * <br>
 * public void body() { <br>
 * &nbsp;&nbsp; Sim_event ev = new Sim_event(); <br>
 * &nbsp;&nbsp; while (Sim_system.running()) { <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; super.sim_get_next(ev); <br> <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; // Entity's behaviour for handling ping request <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; if (ev.get_tag() == GridSimTags.INFOPKT_SUBMIT) { <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; processPingRequest(ev); <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; continue; // skip the rest <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; } <br> <br>
 * &nbsp;&nbsp;&nbsp;&nbsp; ... // other code for handling other incoming requests <br>
 * &nbsp;&nbsp; } <br>
 * } <br>
 * <br>
 * private void processPingRequest(Sim_event ev) { <br>
 * &nbsp;&nbsp; InfoPacket pkt = (InfoPacket) ev.get_data(); <br>
 * &nbsp;&nbsp; pkt.setTag(GridSimTags.INFOPKT_RETURN); <br>
 * &nbsp;&nbsp; pkt.setDestID( pkt.getSrcID() ); <br>
 * <br>
 * &nbsp;&nbsp; // sends back to the sender <br>
 * &nbsp;&nbsp; super.send(super.output, GridSimTags.SCHEDULE_NOW,
 * GridSimTags.INFOPKT_RETURN,
 * &nbsp;&nbsp;&nbsp;&nbsp; new IO_data(pkt,pkt.getSize(),pkt.getSrcID()) ); <br>
 * } <br>
 *
 * </code>
 * </ul>
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.0
 * @see eduni.simjava.Sim_entity
 * @see gridsim.net.Output
 * @see gridsim.net.Input
 * @invariant $none
 */
public class GridSimCore extends Sim_entity
{
    private boolean networkedFlag_;  // true, if networked entity, other false

    // false means NOT invoked
    private boolean terminateIOEntitiesFlag_ = false;

    // If this GridSim uses Network extensions, then the link that is joined to
    // this entity.
    private Link link_;

    /** Reading data received via input port */
    protected Sim_port input;

    /** Sending data via output port to external entities */
    protected Sim_port output;

    // Output port but only for a network extension.
    private NetIO out_ = null;
    
    /** Specifies which type of network to be used. 
     * By default, the simulation uses the gridsim.net package or differentiated
     * network service. To change this default property, please use the 
     * {@link gridsim.GridSim#initNetworkType(int)} method <b>before</b> the
     * simulation starts.
     * @see gridsim.GridSim#initNetworkType(int)
     * @see gridsim.GridSimTags#NET_PACKET_LEVEL
     * @see gridsim.GridSimTags#NET_FLOW_LEVEL
     * @see gridsim.GridSimTags#NET_BUFFER_PACKET_LEVEL
     */
    protected static int NETWORK_TYPE = GridSimTags.NET_PACKET_LEVEL;


    /**
     * Allocates a new GridSim object
     * <b>without</b> NETWORK communication channels: "input" and
     * "output" Sim_port. In summary, this object has <tt>NO</tt>
     * network communication or bandwidth speed.
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_entity
     * @pre name != null
     * @post $none
     */
    protected GridSimCore(String name) throws Exception
    {
        super(name);
        networkedFlag_ = false;
        input = null;
        output = null;
        link_ = null;
    }

    /**
     * Allocates a new GridSim object
     * <b>with</b> NETWORK communication channels: "input" and
     * "output" Sim_port. In addition, this method will create <tt>Input</tt>
     * and <tt>Output</tt> object.
     * <p>
     * However, this is the old approach using one-to-all connection where you
     * can not specify a network topology and there is no wired link from
     * this entity to others. Use {@link #GridSimCore(String, Link)} instead.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baudRate   network communication or bandwidth speed
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_entity
     * @see gridsim.net.Input
     * @see gridsim.net.Output
     * @pre name != null
     * @pre baudRate > 0.0
     * @post $none
     */
    protected GridSimCore(String name, double baudRate) throws Exception
    {
        super(name);
        networkedFlag_ = true;
        link_ = null;

        input = new Sim_port("input");
        output = new Sim_port("output");

        super.add_port(input);
        super.add_port(output);

        initNetwork(name, baudRate, null);
    }

    /**
     * Allocates a new GridSim object
     * <b>with</b> NETWORK communication channels: "input" and
     * "output" Sim_port. In addition, this method will create <tt>Input</tt>
     * and <tt>Output</tt> object.
     * <p>
     * You need to manually create the network topology, i.e. connecting
     * this entity to a router/other entity.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that this GridSim entity will use to
     *                   communicate with other GridSim or Network entities.
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_entity
     * @see gridsim.net.Input
     * @see gridsim.net.Output
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    protected GridSimCore(String name, Link link) throws Exception
    {
        super(name);
        networkedFlag_ = true;
        link_ = link;

        input = new Sim_port("input");
        output = new Sim_port("output");

        super.add_port(input);
        super.add_port(output);

        initNetwork(name, link.getBaudRate(), link);
    }
    
    /**
     * Creates Input and Output entities according to the network type
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baudRate   network communication or bandwidth speed
     * @param link       the link that this GridSim entity will use to
     *                   communicate with other GridSim or Network entities.
     */
    private void initNetwork(String name, double baudRate, Link link)
    {
        // Every GridSim entity with network has its own input/output channels.
        // Connect this entity "input" port to its input buffer "in_port"
        NetIO in = null;

        // Packet Level networking
        if (GridSimCore.NETWORK_TYPE == GridSimTags.NET_PACKET_LEVEL)
        {
            in = new Input("Input_" + name, baudRate);
            out_ = new Output("Output_" + name, baudRate);
        }
        // Flow Level networking
        else if (GridSimCore.NETWORK_TYPE == GridSimTags.NET_FLOW_LEVEL)
        {
            in = new FlowInput("Input_" + name, baudRate);
            out_ = new FlowOutput("Output_" + name, baudRate);
        }
        // Use Finite network buffer
        else if (GridSimCore.NETWORK_TYPE == GridSimTags.NET_BUFFER_PACKET_LEVEL)
        {   
            in = new FnbInput("Input_" + name, baudRate);
            out_ = new FnbOutput("Output_" + name, baudRate);
        }

        Sim_system.link_ports(name, "input", "Input_" + name, "input_buffer");
        Sim_system.link_ports(name, "output", "Output_" + name, "output_buffer");
        if (link != null)
        {
            in.addLink(link);
            out_.addLink(link);
        }
    }

    /**
     * Returns the Link that connects this entity to other entities if Network
     * Extensions are being used.
     * @return a Link object
     * @pre $none
     * @post $none
     */
    public Link getLink() {
        return link_;
    }

    /**
     * Sets the background traffic generator for this entity.
     * <p>
     * When simulation starts, the Output entity will automatically sends junk
     * packets to resource entities.
     * @param gen   a background traffic generator
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre gen != null
     * @post $none
     */
    public boolean setBackgroundTraffic(TrafficGenerator gen)
    {
        if (gen == null || out_ == null) {
            return false;
        }

        return out_.setBackgroundTraffic(gen);
    }

    /**
     * Sets the background traffic generator for this entity.
     * <p>
     * When simulation starts, the Output entity will automatically sends junk
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
        if (gen == null || userName == null || out_ == null) {
            return false;
        }

        return out_.setBackgroundTraffic(gen, userName);
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a non-blocking call</i>, meaning you need
     * to get the ping result by calling {@link #getPingResult()}.
     * The return value of this method is just an indication whether
     * ping() has successfully sent or not.
     *
     * @param entityID   the destination entity ID
     * @param size       the ping packet size
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre entityID > 0
     * @pre size >= 0
     * @post $none
     */
    protected boolean ping(int entityID, int size) {
        return ping(entityID, size, 0.0, 0);
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a non-blocking call</i>, meaning you need
     * to get the ping result by calling {@link #getPingResult()}.
     * The return value of this method is just an indication whether
     * ping() has successfully sent or not.
     *
     * @param entityName the destination entity name
     * @param size       the ping packet size
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre entityName != null
     * @pre size >= 0
     * @post $none
     */
    protected boolean ping(String entityName, int size) {
        return ping(entityName, size, 0.0, 0);
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a non-blocking call</i>, meaning you need
     * to get the ping result by calling {@link #getPingResult()}.
     * The return value of this method is just an indication whether
     * ping() has successfully sent or not.
     *
     * @param entityName the destination entity name
     * @param size       the ping packet size
     * @param delay      the delay time for submitting this ping request
     * @param netServiceLevel   level of service for ping packet (only
     *                   applicable to certain PacketScheduler, such as
     *                   {@link gridsim.net.SCFQScheduler}.
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre entityName != null
     * @pre size >= 0
     * @pre delay >= 0
     * @pre netServiceLevel >= 0
     * @post $none
     */
    protected boolean ping(String entityName, int size, double delay,
                              int netServiceLevel)
    {
        int id = GridSim.getEntityId(entityName);
        return ping(id, size, delay, netServiceLevel);
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a non-blocking call</i>, meaning you need
     * to get the ping result by calling {@link #getPingResult()}.
     * The return value of this method is just an indication whether
     * ping() has successfully sent or not.
     *
     * @param entityID   the destination entity ID
     * @param size       the ping packet size
     * @param delay      the delay time for submitting this ping request
     * @param netServiceLevel   level of service for ping packet (only
     *                   applicable to certain PacketScheduler, such as
     *                   {@link gridsim.net.SCFQScheduler}.
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre entityID > 0
     * @pre size >= 0
     * @pre delay >= 0
     * @pre netServiceLevel >= 0
     * @post $none
     */
    protected boolean ping(int entityID, int size, double delay,
                           int netServiceLevel)
    {
        if (entityID < 0)
        {
            System.out.println(super.get_name() + ".ping(): Error - " +
                "invalid entity ID or name.");
            return false;
        }

        if (size < 0)
        {
            System.out.println(super.get_name() + ".ping(): Error - " +
                "invalid packet size.");
            return false;
        }

        if (delay < 0) {
            delay = 0.0;
        }

        // send this ping to the destination
        send( this.output, delay, GridSimTags.INFOPKT_SUBMIT,
              new IO_data(null, size, entityID, netServiceLevel) );

        return true;
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a blocking call</i>, meaning it will keep
     * waiting until it gets the ping result back.
     *
     * @param entityID   the destination entity ID
     * @param size       the ping packet size
     * @param delay      the delay time for submitting this ping request
     * @param netServiceLevel   level of service for ping packet (only
     *                   applicable to certain PacketScheduler, such as
     *                   {@link gridsim.net.SCFQScheduler}.
     * @return a ping result in InfoPacket object or <tt>null</tt>
     *          if unexpected error happens.
     * @pre entityID > 0
     * @pre size >= 0
     * @pre delay >= 0
     * @pre netServiceLevel >= 0
     * @post $none
     */
    protected InfoPacket pingBlockingCall(int entityID, int size, double delay,
                                          int netServiceLevel)
    {
        boolean result = ping(entityID, size, delay, netServiceLevel);
        if (!result) {
            return null;
        }

        return getPingResult();
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a blocking call</i>, meaning it will keep
     * waiting until it gets the ping result back.
     *
     * @param entityID   the destination entity ID
     * @param size       the ping packet size
     * @return a ping result in InfoPacket object or <tt>null</tt>
     *          if unexpected error happens.
     * @pre entityID > 0
     * @pre size >= 0
     * @post $none
     */
    protected InfoPacket pingBlockingCall(int entityID, int size)
    {
        return pingBlockingCall(entityID, size, 0.0, 0);
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a blocking call</i>, meaning it will keep
     * waiting until it gets the ping result back.
     *
     * @param entityName the destination entity name
     * @param size       the ping packet size
     * @return a ping result in InfoPacket object or <tt>null</tt>
     *          if unexpected error happens.
     * @pre entityName != null
     * @pre size >= 0
     * @post $none
     */
    protected InfoPacket pingBlockingCall(String entityName, int size)
    {
        int id = GridSim.getEntityId(entityName);
        return pingBlockingCall(id, size, 0.0, 0);
    }

    /**
     * Pings to a particular entity ID with a given packet size.
     * <p>
     * This method is <i>a blocking call</i>, meaning it will keep
     * waiting until it gets the ping result back.
     *
     * @param entityName the destination entity name
     * @param size       the ping packet size
     * @param delay      the delay time for submitting this ping request
     * @param netServiceLevel   level of service for ping packet (only
     *                   applicable to certain PacketScheduler, such as
     *                   {@link gridsim.net.SCFQScheduler}.
     * @return a ping result in InfoPacket object or <tt>null</tt>
     *          if unexpected error happens.
     * @pre entityName != null
     * @pre size >= 0
     * @pre delay >= 0
     * @pre netServiceLevel >= 0
     * @post $none
     */
    protected InfoPacket pingBlockingCall(String entityName, int size,
                                          double delay, int netServiceLevel)
    {
        int id = GridSim.getEntityId(entityName);
        return pingBlockingCall(id, size, delay, netServiceLevel);
    }

    /**
     * Gets the first available ping result in the event queue.
     * @return  a ping result in InfoPacket object or <tt>null</tt>
     *          if unexpected error happens.
     * @pre $none
     * @post $none
     */
    protected InfoPacket getPingResult()
    {
        Sim_event ev = new Sim_event();

        // waiting for a response from the GridResource
        Sim_type_p tag = new Sim_type_p(GridSimTags.INFOPKT_RETURN);

        // only look for this type of ack
        super.sim_get_next(tag, ev);

        InfoPacket pkt = null;
        try {
            pkt = (InfoPacket) ev.get_data();
        }
        catch(Sim_exception sim)
        {
            System.out.print(super.get_name() + ".getPingResult(): Error - ");
            System.out.println("exception occurs. See the below message:");
            System.out.println( sim.getMessage() );
            pkt = null;
        }
        catch(Exception e)
        {
            System.out.print(super.get_name() + ".getPingResult(): Error - ");
            System.out.println("exception occurs. See the below message:");
            System.out.println( e.getMessage() );
            pkt = null;
        }

        return pkt;
    }

    /**
     * It terminates Entities managing NETWORK communication channels.
     * It can be invoked explicity to shutdown NETWORK communication channels.
     * It is advisable for all entities extending GridSim class, explicitly
     * invoke this method to terminate <tt>Input</tt> and
     * <tt>Output</tt> entities created
     * by the constructor: {@link GridSim#GridSim(String, double)}
     * @pre $none
     * @post $none
     */
    protected void terminateIOEntities()
    {
        // If it is Networked entity and Not yet terminated, then terminate.
        if ( isNetworked() && !terminateIOEntitiesFlag_ )
        {
            // Send END_OF_SIMULATION to Input entity
            send(input, 0.0, GridSimTags.END_OF_SIMULATION);

            // Send END_OF_SIMULATION to Output entity
            send(output, 0.0, GridSimTags.END_OF_SIMULATION);

            terminateIOEntitiesFlag_ = true;
        }
    }

    /**
     * It terminates the entities of this object that manage <tt>NETWORK</tt>
     * communication channels
     * @see gridsim.GridSim#terminateIOEntities()
     * @pre $none
     * @post $none
     */
    protected void finalizeGridSimulation() {
        terminateIOEntities();
    }

    /**
     * Check type of entity
     * @return true if entity has NETWORK communication channel, otherwise
     *          it returns false
     * @pre $none
     * @post $result == true || false
     */
    protected boolean isNetworked() {
        return networkedFlag_;
    }

    /**
     * Sends an event/message to another entity by <tt>delaying</tt>
     * the simulation time
     * from the current time, with a tag representing the event type.
     * <p>
     * It is <tt>recommended</tt> to use <tt>send()</tt> method with
     * an output port if the network bandwidth plays an important role in
     * this simulation. However, the entity must have the network entities, i.e.
     * <tt>Input</tt> and <tt>Output</tt> port (specified during the creation
     * of the entity by giving a baud rate or bandwidth speed).
     * Below is an example on how to do:
     * <p>
     * <code>
     * ... // other code <br>
     * // object is the entity or message you want to send <br>
     * // size is the object size in bytes (rough estimation) <br>
     * // destination id is the entity ID you want to send the object to <br>
     * IO_data data = new IO_data(object, size, destinationID); <br>
     * <br> <br>
     * // If this entity extends from GridSim class, then you should use it <br>
     * // otherwise need to create a new <tt>Sim_port</tt> object <br>
     * Sim_port port = super.output; <br> <br>
     * // delay is the simulation time delay <br>
     * // tag is the event type (user-defined or choose one from
     *    <tt>GridSimTags</tt> class)<br>
     * send(port, delay, tag, data);<br>
     * ... // remaining other code <br>
     * </code>
     *
     * @param entityName the name of the destination entity
     * @param delay      how long from the current simulation time the event
     *                   should be sent.
     *                   If delay is a negative number, then it will be
     *                   changed to 0.0
     * @param gridSimTag an user-defined number representing the type of
     *                   an event/message
     * @pre entityName != null
     * @pre delay >= 0.0
     * @post $none
     */
    protected void send(String entityName, double delay, int gridSimTag)
    {
        if (entityName == null) {
            return;
        }

        // if delay is -ve, then it doesn't make sense. So resets to 0.0
        if (delay < 0.0) {
            delay = 0.0;
        }

        int id = GridSim.getEntityId(entityName);
        if (id < 0)
        {
            System.out.println(super.get_name() + ".send(): Error - " +
                "invalid entity name \"" + entityName + "\".");
            return;
        }

        super.sim_schedule(id, delay, gridSimTag);
    }

    /**
     * Sends an event/message to another entity by <tt>delaying</tt>
     * the simulation time
     * from the current time, with a tag representing the event type.
     * <p>
     * It is <tt>recommended</tt> to use <tt>send()</tt> method with
     * an output port if the network bandwidth plays an important role in
     * this simulation. However, the entity must have the network entities, i.e.
     * <tt>Input</tt> and <tt>Output</tt> port (specified during the creation
     * of the entity by giving a baud rate or bandwidth speed).
     * Below is an example on how to do:
     * <p>
     * <code>
     * ... // other code <br>
     * // object is the entity or message you want to send <br>
     * // size is the object size in bytes (rough estimation) <br>
     * // destination id is the entity ID you want to send the object to <br>
     * IO_data data = new IO_data(object, size, destinationID); <br>
     * <br> <br>
     * // If this entity extends from GridSim class, then you should use it <br>
     * // otherwise need to create a new <tt>Sim_port</tt> object <br>
     * Sim_port port = super.output; <br> <br>
     * // delay is the simulation time delay <br>
     * // tag is the event type (user-defined or choose one from
     *    <tt>GridSimTags</tt> class)<br>
     * send(port, delay, tag, data);<br>
     * ... // remaining other code <br>
     * </code>
     *
     * @param entityName the name of the destination entity
     * @param delay      how long from the current simulation time the event
     *                   should be sent.
     *                   If delay is a negative number, then it will be
     *                   changed to 0.0
     * @param gridSimTag an user-defined number representing the type of
     *                   an event/message
     * @param data       A reference to data to be sent with the event
     * @pre entityName != null
     * @pre delay >= 0.0
     * @pre data != null
     * @post $none
     */
    protected void send(String entityName, double delay, int gridSimTag,
                        Object data)
    {
        if (entityName == null) {
            return;
        }

        // if delay is -ve, then it doesn't make sense. So resets to 0.0
        if (delay < 0.0) {
            delay = 0.0;
        }

        int id = GridSim.getEntityId(entityName);
        if (id < 0)
        {
            System.out.println(super.get_name() + ".send(): Error - " +
                "invalid entity name \"" + entityName + "\".");
            return;
        }

        super.sim_schedule(id, delay, gridSimTag, data);
    }

    /**
     * Sends an event/message to another entity by <tt>delaying</tt>
     * the simulation time
     * from the current time, with a tag representing the event type.
     * <p>
     * It is <tt>recommended</tt> to use <tt>send()</tt> method with
     * an output port if the network bandwidth plays an important role in
     * this simulation. However, the entity must have the network entities, i.e.
     * <tt>Input</tt> and <tt>Output</tt> port (specified during the creation
     * of the entity by giving a baud rate or bandwidth speed).
     * Below is an example on how to do:
     * <p>
     * <code>
     * ... // other code <br>
     * // object is the entity or message you want to send <br>
     * // size is the object size in bytes (rough estimation) <br>
     * // destination id is the entity ID you want to send the object to <br>
     * IO_data data = new IO_data(object, size, destinationID); <br>
     * <br> <br>
     * // If this entity extends from GridSim class, then you should use it <br>
     * // otherwise need to create a new <tt>Sim_port</tt> object <br>
     * Sim_port port = super.output; <br> <br>
     * // delay is the simulation time delay <br>
     * // tag is the event type (user-defined or choose one from
     *    <tt>GridSimTags</tt> class)<br>
     * send(port, delay, tag, data);<br>
     * ... // remaining other code <br>
     * </code>
     *
     * @param entityID   the id number of the destination entity
     * @param delay      how long from the current simulation time the event
     *                   should be sent.
     *                   If delay is a negative number, then it will be
     *                   changed to 0.0
     * @param gridSimTag an user-defined number representing the type of
     *                   an event/message
     * @pre entityID > 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected void send(int entityID, double delay, int gridSimTag)
    {
        if (entityID < 0) {
            return;
        }

        // if delay is -ve, then it doesn't make sense. So resets to 0.0
        if (delay < 0.0) {
            delay = 0.0;
        }

        if (entityID < 0)
        {
            System.out.println(super.get_name() + ".send(): Error - " +
                "invalid entity id " + entityID);
            return;
        }

        super.sim_schedule(entityID, delay, gridSimTag);
    }

    /**
     * Sends an event/message to another entity by <tt>delaying</tt>
     * the simulation time
     * from the current time, with a tag representing the event type.
     * <p>
     * It is <tt>recommended</tt> to use <tt>send()</tt> method with
     * an output port if the network bandwidth plays an important role in
     * this simulation. However, the entity must have the network entities, i.e.
     * <tt>Input</tt> and <tt>Output</tt> port (specified during the creation
     * of the entity by giving a baud rate or bandwidth speed).
     * Below is an example on how to do:
     * <p>
     * <code>
     * ... // other code <br>
     * // object is the entity or message you want to send <br>
     * // size is the object size in bytes (rough estimation) <br>
     * // destination id is the entity ID you want to send the object to <br>
     * IO_data data = new IO_data(object, size, destinationID); <br>
     * <br> <br>
     * // If this entity extends from GridSim class, then you should use it <br>
     * // otherwise need to create a new <tt>Sim_port</tt> object <br>
     * Sim_port port = super.output; <br> <br>
     * // delay is the simulation time delay <br>
     * // tag is the event type (user-defined or choose one from
     *    <tt>GridSimTags</tt> class)<br>
     * send(port, delay, tag, data);<br>
     * ... // remaining other code <br>
     * </code>
     *
     * @param entityID   the id number of the destination entity
     * @param delay      how long from the current simulation time the event
     *                   should be sent.
     *                   If delay is a negative number, then it will be
     *                   changed to 0.0
     * @param gridSimTag an user-defined number representing the type of
     *                   an event/message
     * @param data       A reference to data to be sent with the event
     * @pre entityID > 0
     * @pre delay >= 0.0
     * @pre data != null
     * @post $none
     */
    protected void send(int entityID, double delay, int gridSimTag, Object data)
    {
        if (entityID < 0) {
            return;
        }

        // if delay is -ve, then it doesn't make sense. So resets to 0.0
        if (delay < 0.0) {
            delay = 0.0;
        }

        if (entityID < 0)
        {
            System.out.println(super.get_name() + ".send(): Error - " +
                "invalid entity id " + entityID);
            return;
        }

        super.sim_schedule(entityID, delay, gridSimTag, data);
    }

    /**
     * Sends an event/message to another entity by <tt>delaying</tt>
     * the simulation time
     * from the current time, with a tag representing the event type.
     * <p>
     * It is <tt>recommended</tt> to use <tt>send()</tt> method with
     * an output port if the network bandwidth plays an important role in
     * this simulation. However, the entity must have the network entities, i.e.
     * <tt>Input</tt> and <tt>Output</tt> port (specified during the creation
     * of the entity by giving a baud rate or bandwidth speed).
     * Below is an example on how to do:
     * <p>
     * <code>
     * ... // other code <br>
     * // object is the entity or message you want to send <br>
     * // size is the object size in bytes (rough estimation) <br>
     * // destination id is the entity ID you want to send the object to <br>
     * IO_data data = new IO_data(object, size, destinationID); <br>
     * <br> <br>
     * // If this entity extends from GridSim class, then you should use it <br>
     * // otherwise need to create a new <tt>Sim_port</tt> object <br>
     * Sim_port port = super.output; <br> <br>
     * // delay is the simulation time delay <br>
     * // tag is the event type (user-defined or choose one from
     *    <tt>GridSimTags</tt> class)<br>
     * send(port, delay, tag, data);<br>
     * ... // remaining other code <br>
     * </code>
     *
     * @param destPort   A reference to the port to send the event out of
     * @param delay      how long from the current simulation time the event
     *                   should be sent.
     *                   If delay is a negative number, then it will be
     *                   changed to 0.0
     * @param gridSimTag an user-defined number representing the type of
     *                   an event/message
     * @pre destPort != null
     * @pre delay >= 0.0
     * @post $none
     */
    protected void send(Sim_port destPort, double delay, int gridSimTag)
    {
        if (destPort == null)
        {
            System.out.println(super.get_name() + ".send(): Error - " +
                "destination port is null or empty.");
            return;
        }

        // if delay is -ve, then it doesn't make sense. So resets to 0.0
        if (delay < 0.0) {
            delay = 0.0;
        }

        super.sim_schedule(destPort, delay, gridSimTag);
    }

    /**
     * Sends an event/message to another entity by <tt>delaying</tt>
     * the simulation time
     * from the current time, with a tag representing the event type.
     * <p>
     * It is <tt>recommended</tt> to use <tt>send()</tt> method with
     * an output port if the network bandwidth plays an important role in
     * this simulation. However, the entity must have the network entities, i.e.
     * <tt>Input</tt> and <tt>Output</tt> port (specified during the creation
     * of the entity by giving a baud rate or bandwidth speed).
     * Below is an example on how to do:
     * <p>
     * <code>
     * ... // other code <br>
     * // object is the entity or message you want to send <br>
     * // size is the object size in bytes (rough estimation) <br>
     * // destination id is the entity ID you want to send the object to <br>
     * IO_data data = new IO_data(object, size, destinationID); <br>
     * <br> <br>
     * // If this entity extends from GridSim class, then you should use it <br>
     * // otherwise need to create a new <tt>Sim_port</tt> object <br>
     * Sim_port port = super.output; <br> <br>
     * // delay is the simulation time delay <br>
     * // tag is the event type (user-defined or choose one from
     *    <tt>GridSimTags</tt> class)<br>
     * send(port, delay, tag, data);<br>
     * ... // remaining other code <br>
     * </code>
     *
     * @param destPort  A reference to the port to send the event out of
     * @param delay      how long from the current simulation time the event
     *                   should be sent.
     *                   If delay is a negative number, then it will be
     *                   changed to 0.0
     * @param gridSimTag an user-defined number representing the type of
     *                   an event/message
     * @param data       an object of type IO_data
     * @pre destPort != null
     * @pre delay >= 0.0
     * @pre data != null
     * @post $none
     * @see gridsim.IO_data
     */
    protected void send(Sim_port destPort, double delay, int gridSimTag,
            Object data)
    {
        if (destPort == null)
        {
            System.out.println(super.get_name() + ".send(): Error - " +
                "destination port is null or empty.");
            return;
        }

        // if delay is -ve, then it doesn't make sense. So resets to 0.0
        if (delay < 0.0) {
            delay = 0.0;
        }

        super.sim_schedule(destPort, delay, gridSimTag, data);
    }

} 

