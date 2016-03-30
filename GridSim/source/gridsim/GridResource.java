/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.index.AbstractGIS;
import gridsim.net.InfoPacket;
import gridsim.net.Link;

import java.util.Calendar;
import java.util.LinkedList;


/**
 * GridResource extends the {@link gridsim.GridSimCore} class for
 * gaining communication and concurrent entity capabilities.
 * An instance of this class simulates a resource
 * with properties defined in an object of
 * {@link gridsim.ResourceCharacteristics} class.
 * <p>
 * The process of creating a Grid resource is as follows:
 * <ol>
 * <li> create PE (Processing Element) objects with a suitable MIPS (Million
 *      Instructions Per Second) or SPEC (Standard Performance Evaluation
 *      Corporation) rating;
 * <li> assemble them together to create a machine;
 * <li> group one or more objects of the machine to form a Grid resource
 * </ol>
 * <p>
 * A resource having a single machine with one or more PEs (Processing Elements)
 * is managed as a time-shared system using a round-robin scheduling algorithm.
 * A resource with multiple machines is treated as a distributed memory cluster
 * and is managed as a space-shared system using FCFS (First Come Firt Serve)
 * scheduling policy or its variants.
 * <p>
 * Since GridSim 2.2, other scheduling algorithm can be added externally
 * (without compiling or replacing the existing GridSim JAR file) into a
 * Grid resource. For more information, look on tutorial page or AllocPolicy
 * class.
 * <p>
 * Since GridSim 3.0, different types of resources can be created externally
 * without modifying this class. You need to do the following:
 * <ol>
 *     <li> extends from this class
 *     <li> uses only a constructor from
 *          {@link #GridResource(String, double, ResourceCharacteristics,
 *                              ResourceCalendar, AllocPolicy)}
 *     <li> overrides {@link #registerOtherEntity()} method to register a
 *          different entity or tag to {@link gridsim.GridInformationService}.
 *          However, you also need to create a new child class extending from
 *          {@link gridsim.GridInformationService}.
 *     <li> overrides {@link #processOtherEvent(Sim_event)} method to process
 *          other incoming tags apart from the standard ones.
 * </ol>
 * <br>
 * <b>NOTE:</b>
 * <ul>
 *     <li>You do not need to override {@link #body()} method if you do step 3.
 *     <li>A resource name can be found using <tt>super.get_name()</tt> method
 *         from {@link eduni.simjava.Sim_entity} class.
 *     <li>A resource ID can be found using <tt>super.get_id()</tt> method
 *         from {@link eduni.simjava.Sim_entity} class.
 * </ul>
 * <p>
 * Since GridSim 3.1, a network framework has been incorporated into this
 * simulation. To make use of this, you need to create a resource entity
 * only using the below constructors:
 * <ul>
 *      <li> {@link #GridResource(String, Link, ResourceCharacteristics,
 *                              ResourceCalendar, AllocPolicy)}
 *      <li> {@link #GridResource(String, Link, ResourceCharacteristics,
 *                              ResourceCalendar)}
 *      <li> {@link #GridResource(String, Link,  long,
 *               ResourceCharacteristics, double, double, double,
 *               LinkedList, LinkedList)}
 * </ul>
 *
 * Then you need to attach this entity into the overall network topology, i.e.
 * connecting this entity to a router, etc. See the examples for more details.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @author       Anthony Sulistio (re-design and re-written this class)
 * @since        GridSim Toolkit 1.0
 * @see gridsim.GridSimCore
 * @see gridsim.ResourceCharacteristics
 * @see gridsim.AllocPolicy
 * @invariant $none
 */
public class GridResource extends GridSimCore
{
    /** Characteristics of this resource */
    protected ResourceCharacteristics resource_;

    /** a ResourceCalendar object */
    protected ResourceCalendar resCalendar_;

    /** A resource's scheduler. This object is reponsible in scheduling and
     * and executing submitted Gridlets.
     */
    protected AllocPolicy policy_;

    /** A scheduler type of this resource, such as FCFS, Round Robin, etc */
    protected int policyType_;

    /** Integer object size, including its overhead */
    protected static final int SIZE = 12;

    /** Regional GIS entity name */
    protected String regionalGISName_;


    /**
     * Allocates a new GridResource object. When making a different type of
     * GridResource object, use
     * {@link #GridResource(String, double, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicy)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param seed       the initial seed
     * @param resource   an object of ResourceCharacteristics
     * @param peakLoad   the load during peak times
     * @param offPeakLoad   the load during off peak times
     * @param relativeHolidayLoad   the load during holiday times
     * @param weekends   a linked-list contains the weekend days
     * @param holidays   a linked-list contains the public holidays
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see #GridResource(String, double,
     *          ResourceCharacteristics, ResourceCalendar, AllocPolicy)
     * @pre name != null
     * @pre baud_rate > 0
     * @pre resource != null
     * @post $none
     */
    public GridResource(String name, double baud_rate, long seed,
            ResourceCharacteristics resource, double peakLoad,
            double offPeakLoad, double relativeHolidayLoad,
            LinkedList weekends, LinkedList holidays) throws Exception
    {
        super(name, baud_rate);
        resource_ = resource;

        resCalendar_ = new ResourceCalendar(resource_.getResourceTimeZone(),
                peakLoad, offPeakLoad, relativeHolidayLoad, weekends,
                holidays, seed);

        policy_ = null;
        init();
    }

    /**
     * Allocates a new GridResource object. When making a different type of
     * GridResource object, use
     * {@link #GridResource(String, double, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicy)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see #GridResource(String, double,
     *          ResourceCharacteristics, ResourceCalendar, AllocPolicy)
     * @pre name != null
     * @pre baud_rate > 0
     * @pre resource != null
     * @pre calendar != null
     * @post $none
     */
    public GridResource(String name, double baud_rate,
                ResourceCharacteristics resource, ResourceCalendar calendar)
                throws Exception
    {
        super(name, baud_rate);
        resource_ = resource;

        resCalendar_ = calendar;
        policy_ = null;
        init();
    }

    /**
     * Allocates a new GridResource object. When making a different type of
     * GridResource object, use this constructor and then overrides
     * {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduling policy for this Grid resource. If no
     *                   scheduling policy is defined, the default one is
     *                   <tt>SpaceShared</tt>
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.AllocPolicy
     * @pre name != null
     * @pre baud_rate > 0
     * @pre resource != null
     * @pre calendar != null
     * @pre policy != null
     * @post $none
     */
    public GridResource(String name, double baud_rate,
                ResourceCharacteristics resource, ResourceCalendar calendar,
                AllocPolicy policy) throws Exception
    {
        super(name, baud_rate);
        resource_ = resource;

        resCalendar_ = calendar;
        policy_ = policy;  // the order between policy and init() is important
        init();
    }

    /**
     * Allocates a new GridResource object. When making a different type of
     * GridResource object, use
     * {@link #GridResource(String, Link, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicy)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   GridResource to another Entity or Router.
     * @param seed       the initial seed
     * @param resource   an object of ResourceCharacteristics
     * @param peakLoad   the load during peak times
     * @param offPeakLoad   the load during off peak times
     * @param relativeHolidayLoad   the load during holiday times
     * @param weekends   a linked-list contains the weekend days
     * @param holidays   a linked-list contains the public holidays
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre name != null
     * @pre link != null
     * @pre resource != null
     * @post $none
     */
    public GridResource(String name, Link link,  long seed,
                ResourceCharacteristics resource, double peakLoad,
                double offPeakLoad, double relativeHolidayLoad,
                LinkedList weekends, LinkedList holidays) throws Exception
    {
        super(name, link);
        resource_ = resource;

        resCalendar_ = new ResourceCalendar(resource_.getResourceTimeZone(),
                peakLoad, offPeakLoad, relativeHolidayLoad, weekends,
                holidays, seed);

        policy_ = null;
        init();
    }

    /**
     * Allocates a new GridResource object. When making a different type of
     * GridResource object, use
     * {@link #GridResource(String, Link, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicy)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   GridResource to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre name != null
     * @pre link != null
     * @pre resource != null
     * @pre calendar != null
     * @post $none
     */
    public GridResource(String name, Link link,
                ResourceCharacteristics resource, ResourceCalendar calendar)
                throws Exception
    {
        super(name, link);
        resource_ = resource;

        resCalendar_ = calendar;
        policy_ = null;
        init();
    }

    /**
     * Allocates a new GridResource object. When making a different type of
     * GridResource object, use this constructor and then overrides
     * {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   GridResource to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduling policy for this Grid resource. If no
     *                   scheduling policy is defined, the default one is
     *                   <tt>SpaceShared</tt>
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.AllocPolicy
     * @pre name != null
     * @pre link != null
     * @pre resource != null
     * @pre calendar != null
     * @pre policy != null
     * @post $none
     */
    public GridResource(String name, Link link,
            ResourceCharacteristics resource, ResourceCalendar calendar,
            AllocPolicy policy) throws Exception
    {
        super(name,link);
        resource_ = resource;

        resCalendar_ = calendar;
        policy_ = policy;  // the order between policy and init() is important
        init();
    }

    /////////////////////////////////////////////////
    
    /**
     * Returns the allocation policy used by this Grid resource.
     * @return the allocation policy
     */
    public AllocPolicy getAllocationPolicy(){
    	return policy_;
    }
    
    /**
     * Returns the characteristics of the resource.
     * @return the resource characteristics
     */
    public ResourceCharacteristics getResourceCharacteristics() {
    	return resource_;
    }

    /**
     * Sets a regional GridInformationService (GIS) entity for this resource
     * to communicate with. This resource will then register its ID to the
     * regional GIS entity, rather than {@link GridInformationService} or
     * <tt>system GIS</tt>.
     * @param regionalGIS   name of regional GIS entity
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre regionalGIS != null
     * @post $none
     */
    public boolean setRegionalGIS(String regionalGIS)
    {
        if (regionalGIS == null || GridSim.getEntityId(regionalGIS) == -1) {
            return false;
        }

        regionalGISName_ = regionalGIS;
        return true;
    }

    /**
     * Sets a regional GridInformationService (GIS) entity for this resource
     * to communicate with. This resource will then register its ID to the
     * regional GIS entity, rather than {@link GridInformationService} or
     * <tt>system GIS</tt>.
     * @param gis   regional GIS entity object
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre gis != null
     * @post $none
     */
    public boolean setRegionalGIS(AbstractGIS gis)
    {
        if (gis == null) {
            return false;
        }

        return setRegionalGIS( gis.get_name() );
    }

    /**
     * Handles external events that are coming to this GridResource entity.
     * This method also registers the identity of this GridResource entity to
     * <tt>GridInformationService</tt> class.
     * <p>
     * The services or tags available for this resource are:
     * <ul>
     *      <li> {@link gridsim.GridSimTags#RESOURCE_CHARACTERISTICS} </li>
     *      <li> {@link gridsim.GridSimTags#RESOURCE_DYNAMICS} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_SUBMIT} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_CANCEL} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_PAUSE} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_RESUME} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_MOVE} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_STATUS} </li>
     * </ul>
     * <br>
     * This method also calls these methods in the following order:
     * <ol>
     *      <li> {@link #registerOtherEntity()} method
     *      <li> {@link #processOtherEvent(Sim_event)} method
     * </ol>
     *
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // send the registration to GIS
        int register = 0;
        if (policyType_ == ResourceCharacteristics.ADVANCE_RESERVATION) {
            register = GridSimTags.REGISTER_RESOURCE_AR;
        }
        else {
            register = GridSimTags.REGISTER_RESOURCE;
        }

        // this resource should register to regional GIS.
        // However, if not specified, then register to system GIS (the
        // default GridInformationService) entity.
        int gisID = GridSim.getEntityId(regionalGISName_);
        if (gisID == -1) {
            gisID = GridSim.getGridInfoServiceEntityId();
        }
        // need to wait for few seconds before registering to a regional GIS.
        // This is because to allow all routers to fill in their routing tables
        else
        {
            super.sim_pause(GridSim.PAUSE);
            System.out.println(super.get_name() + ".body(): wait for " +
                GridSim.PAUSE + " seconds before registering to " +
                regionalGISName_);
        }

        // send the registration to GIS
        super.send(super.output, GridSimTags.SCHEDULE_NOW, register,
                new IO_data(new Integer(super.get_id()), SIZE, gisID) );

        // Below method is for a child class to override
        registerOtherEntity();

        // Process events until END_OF_SIMULATION is received from the
        // GridSimShutdown Entity
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                policy_.setEndSimulation();
                break;
            }

            // process the received event
            processEvent(ev);
        }

        // remove I/O entities created during construction of this entity
        super.terminateIOEntities();
    }

    //////////////////// PROTECTED METHODS ///////////////////////////////////

    /**
     * Overrides this method when making a new and different type of resource.
     * This method is called by {@link #body()} for incoming unknown tags.
     * <p>
     * Another approach is to override the
     * {@link gridsim.AllocPolicy#processOtherEvent(Sim_event)} method.
     * This approach is desirable if you do not want to create a new type of
     * grid resource.
     *
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(Sim_event ev)
    {
        if (ev == null)
        {
            System.out.println(super.get_name() + ".processOtherEvent(): " +
                    "Error - an event is null.");
            return;
        }

        /****   // NOTE: now a resource passes a new event to the scheduler
        System.out.println(super.get_name()+".processOtherEvent(): Unable to " +
                "handle request from GridSimTags with tag number " +
                ev.get_tag() );
        *****/

        policy_.processOtherEvent(ev);
    }

    /**
     * Overrides this method when making a new and different type of resource.
     * This method is called by {@link #body()} to register other type to
     * {@link gridsim.GridInformationService} entity. In doing so, you
     * need to create a new child class extending from
     * {@link gridsim.GridInformationService}.
     * <br>
     * <b>NOTE:</b> You do not need to override {@link #body()} method, if
     * you use this method.
     *
     * @pre $none
     * @post $none
     * @see gridsim.GridInformationService
     */
    protected void registerOtherEntity() {
        // empty. This should be override by a child class
    }

    //////////////////// PRIVATE METHODS ///////////////////////////////////

    /**
     * Initializes the resource allocation policy
     * @throws Exception    If number of PEs is zero
     * @pre $none
     * @post $none
     */
    private void init() throws Exception
    {
        // If this resource doesn't have any PEs then no useful at all
        if (resource_.getNumPE() == 0)
        {
            throw new Exception(super.get_name() + " : Error - this entity " +
                    "has no PEs. Therefore, can't process any Gridlets.");
        }

        // stores id of this class
        resource_.setResourceID( super.get_id() );

        // if internal allocation policy is used
        policyType_ = resource_.getResourceAllocationPolicy();
        if (policy_ == null)
        {
            switch (policyType_)
            {
                case ResourceCharacteristics.TIME_SHARED:
                    policy_ = new TimeShared(super.get_name(), "TimeShared");
                    break;

                case ResourceCharacteristics.SPACE_SHARED:
                    policy_ = new SpaceShared(super.get_name(), "SpaceShared");
                    break;

                default:
                    throw new Exception(super.get_name()+" : Error - supports"+
                            " only TimeShared or SpaceShared policy.");
            }
        }

        policy_.init(resource_, resCalendar_, super.output);
    }

    /**
     * Processes events or services that are available for this GridResource
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processEvent(Sim_event ev)
    {
        int src_id = -1;
        switch ( ev.get_tag() )
        {
            // Resource characteristics inquiry
            case GridSimTags.RESOURCE_CHARACTERISTICS:
                src_id = ( (Integer) ev.get_data() ).intValue();
                super.send( super.output, 0.0, ev.get_tag(),
                        new IO_data(resource_,resource_.getByteSize(),src_id));
                break;

                // Resource dynamic info inquiry
            case GridSimTags.RESOURCE_DYNAMICS:
                src_id = ( (Integer) ev.get_data() ).intValue();
                super.send( super.output, 0.0, ev.get_tag(),
                        new IO_data(policy_.getTotalLoad(),
                                    Accumulator.getByteSize(), src_id) );
                break;

            case GridSimTags.RESOURCE_NUM_PE:
                src_id = ( (Integer) ev.get_data() ).intValue();
                int numPE = resource_.getNumPE();
                super.send( super.output, 0.0, ev.get_tag(),
                        new IO_data(new Integer(numPE), SIZE, src_id) );
                break;

            case GridSimTags.RESOURCE_NUM_FREE_PE:
                src_id = ( (Integer) ev.get_data() ).intValue();
                int numFreePE = resource_.getNumFreePE();
                super.send( super.output, 0.0, ev.get_tag(),
                        new IO_data(new Integer(numFreePE), SIZE, src_id) );
                break;

                // New Gridlet arrives
            case GridSimTags.GRIDLET_SUBMIT:
                processGridletSubmit(ev, false);
                break;

                // New Gridlet arrives, but the sender asks for an ack
            case GridSimTags.GRIDLET_SUBMIT_ACK:
                processGridletSubmit(ev, true);
                break;

                // Cancels a previously submitted Gridlet
            case GridSimTags.GRIDLET_CANCEL:
                processGridlet(ev, GridSimTags.GRIDLET_CANCEL);
                break;

                // Pauses a previously submitted Gridlet
            case GridSimTags.GRIDLET_PAUSE:
                processGridlet(ev, GridSimTags.GRIDLET_PAUSE);
                break;

                // Pauses a previously submitted Gridlet, but the sender
                // asks for an acknowledgement
            case GridSimTags.GRIDLET_PAUSE_ACK:
                processGridlet(ev, GridSimTags.GRIDLET_PAUSE_ACK);
                break;

                // Resumes a previously submitted Gridlet
            case GridSimTags.GRIDLET_RESUME:
                processGridlet(ev, GridSimTags.GRIDLET_RESUME);
                break;

                // Resumes a previously submitted Gridlet, but the sender
                // asks for an acknowledgement
            case GridSimTags.GRIDLET_RESUME_ACK:
                processGridlet(ev, GridSimTags.GRIDLET_RESUME_ACK);
                break;

                // Moves a previously submitted Gridlet to a different resource
            case GridSimTags.GRIDLET_MOVE:
                processGridletMove(ev, GridSimTags.GRIDLET_MOVE);
                break;

                // Moves a previously submitted Gridlet to a different resource
            case GridSimTags.GRIDLET_MOVE_ACK:
                processGridletMove(ev, GridSimTags.GRIDLET_MOVE_ACK);
                break;

                // Checks the status of a Gridlet
            case GridSimTags.GRIDLET_STATUS:
                processGridletStatus(ev);
                break;

                // Ping packet
            case GridSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

                // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    /**
     * Process the event for an User who wants to know the status of a Gridlet.
     * This GridResource will then send the status back to the User.
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processGridletStatus(Sim_event ev)
    {
        int gridletId = 0;
        int userId = 0;
        int status = -1;

        try
        {
            // if a sender using gridletXXX() methods
            int data[] = (int[]) ev.get_data();
            gridletId = data[0];
            userId = data[1];

            status = policy_.gridletStatus(gridletId, userId);
        }

        // if a sender using normal send() methods
        catch (ClassCastException c)
        {
            try
            {
                Gridlet gl = (Gridlet) ev.get_data();
                gridletId = gl.getGridletID();
                userId = gl.getUserID();

                status = policy_.gridletStatus(gridletId, userId);
            }
            catch (Exception e)
            {
                System.out.println(super.get_name() +
                        ": Error in processing GridSimTags.GRIDLET_STATUS");
                System.out.println( e.getMessage() );
                return;
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                    ": Error in processing GridSimTags.GRIDLET_STATUS");
            System.out.println( e.getMessage() );
            return;
        }

        int[] array = new int[2];
        array[0] = gridletId;
        array[1] = status;

        int tag = GridSimTags.GRIDLET_STATUS;
        super.send( super.output, GridSimTags.SCHEDULE_NOW, tag,
                    new IO_data(array, SIZE, userId) );
    }

    /**
     * Processes a Gridlet based on the event type
     * @param ev   a Sim_event object
     * @param type event type
     * @pre ev != null
     * @pre type > 0
     * @post $none
     */
    private void processGridlet(Sim_event ev, int type)
    {
        int gridletId = 0;
        int userId = 0;

        try
        {
            // if a sender using gridletXXX() methods
            int data[] = (int[]) ev.get_data();
            gridletId = data[0];
            userId = data[1];
        }

        // if a sender using normal send() methods
        catch (ClassCastException c)
        {
            try
            {
                Gridlet gl = (Gridlet) ev.get_data();
                gridletId = gl.getGridletID();
                userId = gl.getUserID();
            }
            catch (Exception e)
            {
                System.out.println(super.get_name() +
                        ": Error in processing Gridlet");
                System.out.println( e.getMessage() );
                return;
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                    ": Error in processing a Gridlet.");
            System.out.println( e.getMessage() );
            return;
        }

        // begins executing ....
        switch (type)
        {
            case GridSimTags.GRIDLET_CANCEL:
                policy_.gridletCancel(gridletId, userId);
                break;

            case GridSimTags.GRIDLET_PAUSE:
                policy_.gridletPause(gridletId, userId, false);
                break;

            case GridSimTags.GRIDLET_PAUSE_ACK:
                policy_.gridletPause(gridletId, userId, true);
                break;

            case GridSimTags.GRIDLET_RESUME:
                policy_.gridletResume(gridletId, userId, false);
                break;

            case GridSimTags.GRIDLET_RESUME_ACK:
                policy_.gridletResume(gridletId, userId, true);
                break;

            default:
                break;
        }

    }

    /**
     * Process the event for an User who wants to know the move of a Gridlet.
     * @param ev   a Sim_event object
     * @param type  event tag
     * @pre ev != null
     * @pre type > 0
     * @post $none
     */
    private void processGridletMove(Sim_event ev, int type)
    {
        boolean ack = false;
        if (type == GridSimTags.GRIDLET_MOVE_ACK) {
            ack = true;
        }

        try
        {
            // if a sender using gridletMove() methods
            int data[] = (int[]) ev.get_data();
            int gridletId = data[0];
            int userId = data[1];
            int destId = data[2];

            policy_.gridletMove(gridletId, userId, destId, ack);
        }
        catch (Exception e)
        {
            System.out.println(super.get_name()+": Error in moving a Gridlet.");
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Processes a Gridlet submission
     * @param ev  a Sim_event object
     * @param ack  an acknowledgement
     * @pre ev != null
     * @post $none
     */
    private void processGridletSubmit(Sim_event ev, boolean ack)
    {
        try
        {
            // gets the Gridlet object
            Gridlet gl = (Gridlet) ev.get_data();

            // checks whether this Gridlet has finished or not
            if (gl.isFinished())
            {
                String name = GridSim.getEntityName( gl.getUserID() );
                System.out.println(super.get_name() + ": Warning - Gridlet #" +
                        gl.getGridletID() + " owned by " + name +
                        " is already completed/finished.");
                System.out.println("Therefore, it is not being executed again");
                System.out.println();

                // NOTE: If a Gridlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause GridSim to be hanged since waiting
                // for this Gridlet back.
                if (ack)
                {
                    int[] array = new int[2];
                    array[0] = gl.getGridletID();
                    array[1] = GridSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = GridSimTags.GRIDLET_SUBMIT_ACK;
                    super.send(super.output, GridSimTags.SCHEDULE_NOW, tag,
                            new IO_data(array, SIZE, gl.getUserID()) );
                }

                super.send(super.output, 0, GridSimTags.GRIDLET_RETURN,
                    new IO_data(gl,gl.getGridletOutputSize(),gl.getUserID()) );

                return;
            }

            // process this Gridlet to this GridResource
            gl.setResourceParameter(super.get_id(), resource_.getCostPerSec());
            policy_.gridletSubmit(gl, ack);
        }
        catch (ClassCastException c)
        {
            System.out.println(super.get_name() + ".processGridletSubmit(): " +
                    "ClassCastException error.");
            System.out.println( c.getMessage() );
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() + ".processGridletSubmit(): " +
                    "Exception error.");
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Processes a ping request.
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processPingRequest(Sim_event ev)
    {
        InfoPacket pkt = (InfoPacket) ev.get_data();
        pkt.setTag(GridSimTags.INFOPKT_RETURN);
        pkt.setDestID( pkt.getSrcID() );

        // sends back to the sender
        super.send( super.output, 0.0, GridSimTags.INFOPKT_RETURN,
                    new IO_data(pkt, pkt.getSize(), pkt.getSrcID()) );
    }

} 

