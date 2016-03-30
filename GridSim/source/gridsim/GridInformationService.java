/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collection;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;

/**
 * A Grid Information Service (GIS) is an entity that provides grid
 * resource registration, indexing and discovery services. The Grid resources
 * tell their readiness to process Gridlets by registering themselves with
 * this entity, done via {@link gridsim.GridResource#body()} method.
 * In addition, GIS is now responsible for notifying all the registered
 * entities, such as GridResource and network entities for shutting down
 * at the end of a simulation.
 * {@link gridsim.GridSimShutdown} entity is not responsible anymore to
 * simplify the overall design, hence coding effort.
 * <p>
 * Other entities such as the resource broker can contact
 * this class for resource discovery service, which returns a list of
 * registered resource IDs. In summary, it acts like a yellow page service.
 * <p>
 * This class will be created by GridSim upon initialization of the simulation,
 * i.e. done via {@link gridsim.GridSim#init(int, Calendar, boolean)} method.
 * Hence, do not need to worry about creating an object of this class.
 * <p>
 * Since <tt>GridSim version 3.0</tt>, you can create your own GIS entity based
 * on your requirements. Here are the steps needed
 * (see example/Example09 directory for more details):
 * <ol>
 *     <li> extends this class
 *     <li> overrides {@link #processOtherEvent(Sim_event)} method if you want
 *          to register other tags apart from {@link gridsim.GridSimTags}.
 *     <li> during initialization of simulation, calls
 *          {@link gridsim.GridSim#init(int, Calendar, boolean, boolean)} with
 *          <tt>gis</tt> parameter is set to false.
 *     <li> uses {@link gridsim.GridSim#setGIS(GridInformationService)} method
 *          before running or starting the simulation.
 * </ol>
 * <br>
 * <b>NOTE:</b> You do not need to override {@link #body()} method because
 * this method calls {@link #processOtherEvent(Sim_event)} for new tags.
 * <br>
 * <p>
 * Since <tt>GridSim version 3.2</tt>, this entity is referred as
 * <b>system GIS</b>. If you want to have multiple GIS entities or a regional
 * GIS entity linked as part of a network topology, then you should look at
 * {@link gridsim.index.AbstractGIS} entity (see examples/RegionalGIS
 * directory for more details).<br>
 * Therefore, the roles of this system GIS in the overall simulation are:
 * <ul>
 * <li> storing a regional GIS entity ID.
 * <li> storing a network entity ID from Routers and Links.
 * <li> storing a resource entity ID that is sent by its regional GIS.
 * <li> notifying all registered entities, as mentioned in the above points,
 *      regarding to the end of a simulation.
 * <li> sending a list of regional GIS IDs.
 * </ul>
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @author       Anthony Sulistio (re-design this class)
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 * @see gridsim.GridSimTags
 * @see gridsim.GridSimShutdown
 * @see gridsim.GridSim#init(int, Calendar, boolean)
 * @see gridsim.GridSim#init(int, Calendar, boolean, boolean)
 * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[], String)
 * @see gridsim.GridSim#setGIS(GridInformationService)
 * @see gridsim.GridResource#body()
 */
public class GridInformationService extends GridSimCore
{
    private LinkedList resList_;    // for all type of resources
    private LinkedList arList_;     // only for AR resources
    private LinkedList gisList_;    // list of all regional GIS
    private ArrayList netList_;     // for all network connections

    /**
     * Allocates a new GridInformationService object with networked I/O ports
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  communication speed
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_entity
     * @pre name != null
     * @pre baud_rate > 0.0
     * @post $none
     */
    public GridInformationService(String name, double baud_rate)
                                  throws Exception
    {
        super(name, baud_rate);
        resList_ = new LinkedList();
        arList_ = new LinkedList();
        netList_ = new ArrayList();
        gisList_ = new LinkedList();
    }

    /**
     * A method that gets one event at a time, and serves it based
     * on its request.
     * The services or tags available to be processed are:
     * <ul>
     *      <li> GridSimTags.REGISTER_RESOURCE
     *      <li> GridSimTags.REGISTER_RESOURCE_AR. Registers only a resource
     *           that supports AR.
     *      <li> GridSimTags.RESOURCE_LIST
     *      <li> GridSimTags.RESOURCE_LIST_AR
     * </ul>
     * <p>
     * To process other tags apart from the above, you need to override
     * {@link #processOtherEvent(Sim_event)} method. You do not need to
     * override this method.
     *
     * @pre $none
     * @post $none
     */
    public void body()
    {
        int SIZE = 8; // 8 bytes including some overheads
        int id = -1;  // requester id

        // Process events
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);     // get the next incoming event

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                processEndSimulation();
                notifyAllEntity();
                break;
            }

            switch ( ev.get_tag() )
            {
                    // storing regional GIS id
                case GridSimTags.REGISTER_REGIONAL_GIS:
                    gisList_.add( (Integer) ev.get_data() );
                    break;

                    // request for all regional GIS list
                case GridSimTags.REQUEST_REGIONAL_GIS:

                    // Get ID of an entity that send this event
                    id = ( (Integer) ev.get_data() ).intValue();

                    // Send the regional GIS list back to sender
                    super.send(super.output, 0.0, ev.get_tag(),
                            new IO_data(gisList_, SIZE*gisList_.size(), id) );
                    break;

                    // A resource is requesting to register.
                case GridSimTags.REGISTER_RESOURCE:
                    resList_.add( (Integer) ev.get_data() );
                    break;

                    // A resource that can support Advance Reservation
                case GridSimTags.REGISTER_RESOURCE_AR:
                    resList_.add( (Integer) ev.get_data() );
                    arList_.add( (Integer) ev.get_data() );
                    break;

                    // A Broker is requesting for a list of all resources.
                case GridSimTags.RESOURCE_LIST:

                    // Get ID of an entity that send this event
                    id = ( (Integer) ev.get_data() ).intValue();

                    // Send the resource list back to the sender
                    super.send(super.output, 0.0, ev.get_tag(),
                            new IO_data(resList_, SIZE*resList_.size(), id) );
                    break;

                    // A Broker is requesting for a list of all resources.
                case GridSimTags.RESOURCE_AR_LIST:

                    // Get ID of an entity that send this event
                    id = ( (Integer) ev.get_data() ).intValue();

                    // Send the resource AR list back to the sender
                    super.send(super.output, 0.0, ev.get_tag(),
                            new IO_data(arList_, SIZE* arList_.size(), id) );
                    break;

                    // registration from network entity
                case GridSimTags.REGISTER_LINK:
                    netList_.add( (Integer) ev.get_data() );
                    break;

                    // registration from network entity
                case GridSimTags.REGISTER_ROUTER:
                    netList_.add( (Integer) ev.get_data() );
                    break;

                default:
                    processOtherEvent(ev);
                    break;
            }
        }

        // after finish the simulation, disconnect I/O ports
        super.terminateIOEntities();
    }

    /**
     * Gets the list of all GridResource IDs, including resources that support
     * Advance Reservation.
     * @return LinkedList containing resource IDs. Each ID is represented by
     *         an Integer object.
     * @pre $none
     * @post $none
     */
    public LinkedList getList() {
        return resList_;
    }

    /**
     * Gets the list of GridResource IDs that <b>only</b> support
     * Advanced Reservation.
     * @return LinkedList containing resource IDs. Each ID is represented by
     *         an Integer object.
     * @pre $none
     * @post $none
     */
    public LinkedList getAdvReservList() {
        return arList_;
    }

    /**
     * Checks whether a given resource ID supports Advanced Reservations or not
     * @param id  a resource ID
     * @return <tt>true</tt> if this resource supports Advanced Reservations,
     *         <tt>false</tt> otherwise
     * @pre id != null
     * @post $none
     */
    public boolean resourceSupportAR(Integer id)
    {
        if (id == null) {
            return false;
        }

        return resourceSupportAR( id.intValue() );
    }

    /**
     * Checks whether a given resource ID supports Advanced Reservations or not
     * @param id  a resource ID
     * @return <tt>true</tt> if this resource supports Advanced Reservations,
     *         <tt>false</tt> otherwise
     * @pre id >= 0
     * @post $none
     */
    public boolean resourceSupportAR(int id)
    {
        boolean flag = false;
        if (id < 0) {
            flag = false;
        }
        else {
            flag = checkResource(arList_, id);
        }

        return flag;
    }

    /**
     * Checks whether the given GridResource ID exists or not
     * @param id    a GridResource id
     * @return <tt>true</tt> if the given ID exists, <tt>false</tt> otherwise
     * @pre id >= 0
     * @post $none
     */
    public boolean isResourceExist(int id)
    {
        boolean flag = false;
        if (id < 0) {
            flag = false;
        }
        else {
            flag = checkResource(resList_, id);
        }

        return flag;
    }

    /**
     * Checks whether the given GridResource ID exists or not
     * @param id    a GridResource id
     * @return <tt>true</tt> if the given ID exists, <tt>false</tt> otherwise
     * @pre id != null
     * @post $none
     */
    public boolean isResourceExist(Integer id)
    {
        if (id == null) {
            return false;
        }
        return isResourceExist( id.intValue() );
    }

    ////////////////////////// PROTECTED METHODS ////////////////////////////

    /**
     * This method needs to override by a child class for processing
     * other events.
     * These events are based on tags that are not mentioned in
     * {@link #body()} method.
     *
     * @param ev   a Sim_event object
     * @see gridsim.GridInformationService#body()
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(Sim_event ev)
    {
        if (ev == null)
        {
            System.out.println("GridInformationService.processOtherEvent(): " +
                    "Unable to handle a request since the event is null.");
            return;
        }

        System.out.println("GridInformationSevice.processOtherEvent(): " +
                "Unable to handle a request from " +
                GridSim.getEntityName( ev.get_src() ) +
                " with event tag = " + ev.get_tag() );
    }

    /**
     * Notifies the registered entities about the end of simulation.
     * This method should be overriden by the child class
     */
    protected void processEndSimulation() {
        // this should be overridden by the child class
    }


    //////////////////// End of PROTECTED METHODS ///////////////////////////

    /**
     * Checks for a list for a particular resource id
     * @param list   list of resources
     * @param id     a resource ID
     * @return true if a resource is in the list, otherwise false
     * @pre list != null
     * @pre id > 0
     * @post $none
     */
    private boolean checkResource(Collection list, int id)
    {
        boolean flag = false;
        if (list == null || id < 0) {
            return flag;
        }

        Integer obj = null;
        Iterator it = list.iterator();

        // a loop to find the match the resource id in a list
        while ( it.hasNext() )
        {
            obj = (Integer) it.next();
            if (obj.intValue() == id)
            {
                flag = true;
                break;
            }
        }

        return flag;
    }

    /**
     * Tells all registered entities the end of simulation
     * @pre $none
     * @post $none
     */
    private void notifyAllEntity()
    {
        System.out.println(super.get_name() +
                ": Notify all GridSim entities for shutting down.");

        signalShutdown(resList_);
        signalShutdown(gisList_);
        signalShutdown(netList_);

        // reset the values
        resList_.clear();
        gisList_.clear();
        netList_.clear();
    }

    /**
     * Sends a signal to all entity IDs mentioned in the given list.
     * @param list  LinkedList storing entity IDs
     * @pre list != null
     * @post $none
     */
    protected void signalShutdown(Collection list)
    {
        // checks whether a list is empty or not
        if (list == null) {
            return;
        }

        Iterator it = list.iterator();
        Integer obj = null;
        int id = 0;     // entity ID

        // Send END_OF_SIMULATION event to all entities in the list
        while ( it.hasNext() )
        {
            obj = (Integer) it.next();
            id = obj.intValue();
            super.send(id, 0.0, GridSimTags.END_OF_SIMULATION);
        }
    }

} 

