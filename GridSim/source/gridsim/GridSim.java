/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.Sim_event;

import eduni.simjava.Sim_exception;
import eduni.simjava.Sim_port;
import eduni.simjava.Sim_system;
import eduni.simjava.Sim_type_p;
import gridsim.filter.FilterGridlet;
import gridsim.filter.FilterResult;
import gridsim.net.Link;
import gridsim.parallel.gui.ParallelVisualizer;
import gridsim.parallel.gui.Visualizer;
import gridsim.parallel.gui.VisualizerAdaptor;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

/**
 * This class is mainly responsible in initialization, running and stopping of
 * the overall simulation.
 * <p>
 * GridSim must be initialized to set-up the
 * simulation environment before creating any other GridSim entities at the
 * user level. This method also prepares the system for simulation by creating
 * three GridSim internal entities - {@link gridsim.GridInformationService},
 * {@link gridsim.GridSimShutdown}, {@link gridsim.GridStatistics}. Invoking the
 * {@link #startGridSimulation()} method starts the Grid simulation.
 * All the resource and user entities must be instantiated in between invoking
 * the above two methods.
 * <p>
 * Since GridSim version 3.0, all of the I/O methods have been moved
 * into {@link gridsim.GridSimCore}.
 * As a result, this class only concentrates on recording statistics and
 * managing Gridlets. In addition, there are three different ways to initialize
 * GridSim simulation. These methods are:
 * <ul>
 *     <li> by using
 *          {@link #init(int, Calendar, boolean, String[], String[], String)}
 *          method. <br>
 *          This method will create {@link gridsim.GridStatistics},
 *          {@link gridsim.GridSimRandom}, {@link gridsim.GridSimShutdown} and
 *          {@link gridsim.GridInformationService} entity. <br>
 *
 *     <li> by using {@link #init(int, Calendar, boolean)} method. <br>
 *          This method will create {@link gridsim.GridSimRandom},
 *          {@link gridsim.GridSimShutdown} and
 *          {@link gridsim.GridInformationService} entity. <br>
 *
 *     <li> by using {@link #init(int, Calendar, boolean, boolean)} method.<br>
 *          This method will create {@link gridsim.GridSimRandom} and
 *          {@link gridsim.GridSimShutdown}. A different type of
 *          {@link gridsim.GridInformationService} entity needs to be entered
 *          using {@link #setGIS(GridInformationService)} method before running
 *          the simulation.
 * </ul>
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @author       Anthony Sulistio (re-design this class)
 * @since        GridSim Toolkit 1.0
 * @see eduni.simjava.Sim_entity
 * @see gridsim.GridSimCore
 * @see gridsim.GridInformationService
 * @see gridsim.GridSimShutdown
 * @see gridsim.GridStatistics
 * @invariant $none
 */
public class GridSim extends GridSimCore
{
    // array[0] = gridlet id, [1] = user Id, and [2] = destinated resource id
    private static final int ARRAY_SIZE = 3;
    private static final int SIZE = 12;     // Integer object size incl. overhead
    private static final int RESULT = 1;    // array[0] = gridlet id, [1] = result
    
    // Indicates whether the simulation should run in debug mode (i.e. with GUI)
    private static boolean debugMode_ = false;
    
    private static int gisID_ = -1;         // id of GIS entity
    private static int shutdownID_ = -1;    // id of GridSimShutdown entity
    private static int statsID_ = -1;       // id of GridStatistics entity
    private static Calendar calendar_ = null;    // a Calendar object
    private static GridInformationService gis_ = null;   // a GIS object
    private static final int NOT_FOUND = -1;     // a constant
    private static boolean traceFlag_ = false; // trace events or other activities

    /** Pause for a certain time delay (in seconds) before a resource
     * registers to a Regional GIS entity.
     * By default, a resource will pause for 10 seconds before registering.
     * As a rule of thumb, if a network topology is huge (involving several
     * routers), then a resource needs to pause much longer.
     */
    public static int PAUSE = 10;  // pause before registering to regional GIS
    
    /**
     * Contains the release number of GridSim
     */
    public static final String GRIDSIM_VERSION_STRING = "5.0";
    private static Visualizer visualizer = new VisualizerAdaptor();
    
    ////////////////////////////////////////////////////////////////////////

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
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see eduni.simjava.Sim_entity
     * @pre name != null
     * @post $none
     */
    public GridSim(String name) throws Exception {
        super(name);
    }

    /**
     * Allocates a new GridSim object
     * <b>with</b> NETWORK communication channels: "input" and
     * "output" Sim_port. In addition, this method will create <tt>Input</tt>
     * and <tt>Output</tt> object.
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baudRate   network communication or bandwidth speed
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see eduni.simjava.Sim_entity
     * @see gridsim.net.Input
     * @see gridsim.net.Output
     * @pre name != null
     * @pre baudRate > 0.0
     * @post $none
     */
    public GridSim(String name, double baudRate) throws Exception {
        super(name, baudRate);
    }

    /**
     * Allocates a new GridSim object
     * <b>with</b> NETWORK communication channels: "input" and
     * "output" Sim_port. In addition, this method will create <tt>Input</tt>
     * and <tt>Output</tt> object.
     * <p>
     * Use this constructor in a wired network.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the physical link that connects this entity
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see eduni.simjava.Sim_entity
     * @see gridsim.net.Input
     * @see gridsim.net.Output
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public GridSim(String name, Link link) throws Exception {
        super(name, link);
    }

    /**
     * Gets a new copy of simulation start date. If the return object
     * is <tt>null</tt>, then need to initialize it by calling
     * {@link #init(int, Calendar, boolean, String[], String[], String)} or
     * {@link #init(int, Calendar, boolean)}
     *
     * @return a new copy of Date object or <tt>null</tt> if GridSim hasn't
     *         been initialized
     * @see java.util.Date
     * @deprecated as of GridSim 5.0, this method has been deprecated. 
     * Use {@link #getSimulationCalendar()} instead.
     * @pre $none
     * @post $none
     */
    public static Date getSimulationStartDate()
    {
        return calendar_.getTime();
    }

    /**
     * Gets a new copy of initial simulation Calendar.
     * @return a new copy of Calendar object or <tt>null</tt> if GridSim hasn't
     *         been initialized
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *      String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @pre $none
     * @post $none
     */
    public static Calendar getSimulationCalendar()
    {
        // make a new copy
        Calendar clone = calendar_;
        if (calendar_ != null) {
            clone = (Calendar) calendar_.clone();
        }

        return clone;
    }

    /**
     * Initializes GridSim parameters.
     * This method should be called before creating any entities.
     * <p>
     * Inside this method, it will create the following GridSim entities:
     * <ul>
     *     <li>GridSimRandom
     *     <li>GridStatistics
     *     <li>GridInformationService
     *     <li>GridSimShutdown
     * </ul>
     * <p>
     * The Calendar object can be specified using
     * <tt>Calendar.getInstance()</tt> to denote the start of the simulation
     * time.
     * This simulation time is <b>very important</b> in handling
     * advanced reservations functionalities.
     * <p>
     * Since GridSim version 5.0, the simulation does not initialise 
     * a GridSimRandom object.
     *
     * @param numUser  the number of User Entities created.
     *                 This parameters indicates that
     *                 {@link gridsim.GridSimShutdown} first waits for
     *                 User Entities's END_OF_SIMULATION signal before
     *                 issuing terminate signal to other entities
     * @param cal          starting time for this simulation. If it is
     *        <tt>null</tt>, then the time will be taken from
     *        <tt>Calendar.getInstance()</tt>.
     * @param traceFlag    true if GridSim trace need to be written
     * @param excludeFromFile  an array of String containing list of files to
     *                         be excluded from statistics
     * @param excludeFromProcessing   an array of String containing list of
     *                                processings to be excluded from writing
     *                                into a file
     * @param reportWriterName  a <tt>ReportWriter</tt> entity name. This entity
     *                          can be found inside a gridbroker package.
     * @see gridsim.GridSimShutdown
     * @see gridsim.GridStatistics
     * @see gridsim.GridInformationService
     * @see gridsim.GridSimRandom
     * @pre numUser >= 0
     * @post $none
     */
    public static void init(int numUser, Calendar cal, boolean traceFlag,
            String[] excludeFromFile, String[] excludeFromProcessing,
            String reportWriterName)
    {
        try
        {
            initCommonVariable(cal, traceFlag, numUser, reportWriterName);
            GridStatistics stat = null;

            // creates a GridStatistics object
            stat = new GridStatistics("GridStatistics", "GridSim_stat.txt",true,
                                      excludeFromFile, excludeFromProcessing);

            // create a GIS object
            gis_ = new GridInformationService("GridInformationService",
                                              GridSimTags.DEFAULT_BAUD_RATE);

            // set all the above entity IDs
            gisID_ = gis_.get_id();
            statsID_ = stat.get_id();
        }
        catch (Sim_exception s)
        {
            System.out.println("GridSim.init(): Unwanted errors happen");
            System.out.println( s.getMessage() );
        }
        catch (Exception e)
        {
            System.out.println("GridSim.init(): Unwanted errors happen");
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Initializes GridSim parameters <b>without</b> any statistical
     * entities. Therefore, if a simulation requires to record any statistical
     * data, then need to use
     * {@link #init(int, Calendar, boolean, String[], String[], String)}
     * instead. This method should be called before creating any entities.
     * <p>
     * Inside this method, it will create the following GridSim entities:
     * <ul>
     *     <li>GridSimRandom
     *     <li>GridInformationService
     *     <li>GridSimShutdown
     * </ul>
     * <p>
     * The Calendar object can be specified using
     * <tt>Calendar.getInstance()</tt> to denote the start of the simulation
     * time.
     * This simulation time is <b>very important</b> in handling
     * advanced reservations functionalities.
     * <p>
     * Since GridSim version 5.0, the simulation does not initialise 
     * a GridSimRandom object.
	 *
     * @param numUser  the number of User Entities created.
     *                 This parameters indicates that
     *                 {@link gridsim.GridSimShutdown} first waits for
     *                 User Entities's END_OF_SIMULATION signal before
     *                 issuing terminate signal to other entities
     * @param cal          starting time for this simulation. If it is
     *        <tt>null</tt>, then the time will be taken from
     *        <tt>Calendar.getInstance()</tt>
     * @param traceFlag    true if GridSim trace need to be written
     * @see gridsim.GridSimShutdown
     * @see gridsim.GridInformationService
     * @see gridsim.GridSimRandom
     * @see gridsim.GridSim#init(int,Calendar,boolean,String[],String[],String)
     * @pre numUser >= 0
     * @post $none
     */
    public static void init(int numUser, Calendar cal, boolean traceFlag)
    {
        try
        {
            initCommonVariable(cal, traceFlag, numUser, null);

            // create a GIS object
            gis_ = new GridInformationService("GridInformationService",
                                              GridSimTags.DEFAULT_BAUD_RATE);

            // set all the above entity IDs
            gisID_ = gis_.get_id();
        }
        catch (Sim_exception s)
        {
            System.out.println("GridSim.init(): Unwanted errors happen");
            System.out.println( s.getMessage() );
        }
        catch (Exception e)
        {
            System.out.println("GridSim.init(): Unwanted errors happen");
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Initializes GridSim parameters <b>without</b> any statistical
     * entities. Therefore, if a simulation requires to record any statistical
     * data, then need to use
     * {@link #init(int, Calendar, boolean, String[], String[], String)}
     * instead. This method should be called before creating any entities.
     * <p>
     * Inside this method, it will create the following GridSim entities:
     * <ul>
     *     <li>GridSimRandom
     *     <li>GridInformationService -- if the parameter <tt>gis</tt> set to
     *         <tt>true</tt>. <br>
     *         NOTE: If you want to use your own GIS entity, you need
     *         to set <tt>gis</tt> parameter to <tt>false</tt>. Then, use
     *         {@link #setGIS(GridInformationService)} method before running
     *         or starting the simulation.
     *     <li>GridSimShutdown
     * </ul>
     * <p>
     * The Calendar object can be specified using
     * <tt>Calendar.getInstance()</tt> to denote the start of the simulation
     * time.
     * This simulation time is <b>very important</b> in handling
     * advanced reservations functionalities.
     * <p>
     * Since GridSim version 5.0, the simulation does not initialise 
     * a GridSimRandom object.
     * 
     * @param numUser  the number of User Entities created.
     *                 This parameters indicates that
     *                 {@link gridsim.GridSimShutdown} first waits for
     *                 all user entities's END_OF_SIMULATION signal before
     *                 issuing terminate signal to other entities
     * @param cal          starting time for this simulation. If it is
     *        <tt>null</tt>, then the time will be taken from
     *        <tt>Calendar.getInstance()</tt>
     * @param traceFlag  <tt>true</tt> if GridSim trace need to be written
     * @param gis        <tt>true</tt> if you want to use a <b>DEFAULT</b>
     *                   {@link gridsim.GridInformationService} entity.
     * @see gridsim.GridSimShutdown
     * @see gridsim.GridInformationService
     * @see gridsim.GridSimRandom
     * @see gridsim.GridSim#setGIS(GridInformationService)
     * @see gridsim.GridSim#init(int,Calendar,boolean,String[],String[],String)
     * @pre numUser >= 0
     * @post $none
     */
    public static void init(int numUser, Calendar cal, boolean traceFlag,
                            boolean gis)
    {
        try
        {
            initCommonVariable(cal, traceFlag, numUser, null);
            if (gis)
            {
                // create a GIS object
                gis_ = new GridInformationService("GridInformationService",
                                    GridSimTags.DEFAULT_BAUD_RATE);

                // set all the above entity IDs
                gisID_ = gis_.get_id();
            }
        }
        catch (Sim_exception s)
        {
            System.out.println("GridSim.init(): Unwanted errors happen");
            System.out.println( s.getMessage() );
        }
        catch (Exception e)
        {
            System.out.println("GridSim.init(): Unwanted errors happen");
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Sets a <tt>GridInformationService</tt> (GIS) entity.
     * This method is useful when you want a different type of GIS entity.
     * This method must be called before {@link #startGridSimulation()} method.
     * @param gis  a GIS object
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre gis != null
     * @post $none
     * @see gridsim.GridSim#startGridSimulation()
     */
    public static boolean setGIS(GridInformationService gis)
    {
        // ignore if an existing GIS object has already been created
        if (gis == null || gis_ != null) {
            return false;
        }

        gis_ = gis;
        gisID_ = gis.get_id();
        return true;
    }

    /**
     * Initializes all the common attributes
     * @param cal   the starting time for this simulation. If it is
     *        <tt>null</tt>, then the time will be taken from
     *        <tt>Calendar.getInstance()</tt>.
     * @param traceFlag   true if GridSim trace need to be written
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @pre $none
     * @post $none
     */
    private static void initCommonVariable(Calendar cal, boolean traceFlag,
               int numUser, String reportWriterName) throws Exception
    {
        // NOTE: the order for the below 3 lines are important
        Sim_system.initialise();
        Sim_system.set_trc_level(1);
        Sim_system.set_auto_trace(traceFlag);

        traceFlag_ = traceFlag;

        // Set the current Wall clock time as the starting time of simulation
        calendar_ = cal;
        if (cal == null) {
            calendar_ = Calendar.getInstance();
        }

        // creates a GridSimShutdown object
        GridSimShutdown shutdown = new GridSimShutdown("GridSimShutdown",
                                              numUser, reportWriterName);
        shutdownID_ = shutdown.get_id();
    }

    /**
     * Starts the execution of GridSim simulation.
     * It waits for complete execution of all entities, i.e. until
     * all entities threads reach non-RUNNABLE state by exiting from the
     * <tt>body()</tt> method. Then, it kills threads of all entities.
     * <p>
     * <b>Note</b>: This method should be called after all the entities
     *              have been setup and added, and their ports are linked.
     * @throws NullPointerException This happens when creating this entity
     *              before initializing GridSim package or this entity name is
     *              <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre $none
     * @post $none
     */
    public static void startGridSimulation() throws NullPointerException
    {
    	System.out.println("Starting GridSim version " + GRIDSIM_VERSION_STRING);
        try {
            Sim_system.run();
        }
        catch (Sim_exception e)
        {
            throw new NullPointerException("GridSim.startGridSimulation() :" +
                    " Error - you haven't initialized GridSim.");
        }
    }
    
    /**
     * Enables the debug mode (used by the GUI)
     */
    public static void enableDebugMode() {
    	debugMode_ = true;
    }
    
    /**
     * Disables the debug mode (used by the GUI)
     */
    public static void disableDebugMode() {
    	debugMode_ = false;
    }
    
    /**
     * Returns <code>true</code> if the debug mode is enabled
     * @return <code>true</code> if the debug mode is enabled; 
     * <code>false<code> otherwise
     */
    public static boolean isDebugModeEnabled() {
    	return debugMode_;
    }

    /**
     * This method should be invoked when the user wants to debug a
     * simulation and would like to use GridSim visualiser to view the status
     * of the resource queues and the actions performed by the resource allocation
     * policies. Note that if the visualiser is used, this method does not 
     * start the simulation in fact, it starts the visualisation tool. 
     * The visualisation tool in turn, can start the simulation.
     * <b>Note</b>: This method should be called after all the entities
     *              have been setup and added, and their ports are linked.
     * @param debug <code>true</code> if the visualizer is used or 
     * 				<code>false</code> otherwise.
     * @throws NullPointerException This happens when creating this entity
     *              before initialising GridSim package or this entity name is
     *              <code>null</code> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     */
    public static void startGridSimulation(boolean debug) throws NullPointerException {
    	if(!debug) {
    		disableDebugMode();
    		startGridSimulation();
    	}
    	else {
    		enableDebugMode();
    		visualizer = new ParallelVisualizer();
    	}
    }
    
    /**
     * Returns a reference to the visualiser to be used.
     * @return a reference to the visualiser to be used.
     */
    public static Visualizer getVisualizer() {
    	return visualizer;
    }
    
    /**
     * Pauses the simulation. This method should be used for 
     * debugging purposes only
     * @return <code>true</code> if the simulation has been paused or 
     * 			<code>false</code> otherwise.
     */
    public static boolean pauseSimulation() {
    	return Sim_system.pauseSimulation();
    }
    
    /**
     * Pauses the simulation at a given simulation time. This method 
     * should be used for debugging purposes only
     * @param time the time when the simulation should be paused
     * @return <code>true</code> if the simulation can be paused or 
     * 			<code>false</code> otherwise.
     */
    public static boolean pauseSimulation(long time) {
    	return Sim_system.pauseSimulation(time);
    }
    
    /**
     * Resumes the simulation. This method should be used for 
     * debugging purposes only
     * @return <code>true</code> if the simulation has been resumed or 
     * 			<code>false</code> otherwise.
     */
    public static boolean resumeSimulation() {
    	return Sim_system.resumeSimulation();
    }

    /**
     * Gets the current simulation time (based on SimJava simulation clock)
     * @return The current simulation time from the simulation clock
     * @see eduni.simjava.Sim_system#clock()
     * @pre $none
     * @post $result >= 0.0
     */
    public static double clock() {
        return Sim_system.clock();
    }

    /**
     * Causes the entity to hold for <tt>duration</tt> units of simulation time
     * @param duration the amount of time to hold
     * @pre $none
     * @post $none
     */
    public void gridSimHold(double duration)
    {
        // if duration is -ve, then no use to hold -ve time.
        if (duration < 0.0) {
            return;
        }

        super.sim_process(duration);
    }

    /**
     * Stops Grid Simulation (based on SimJava Sim_system.run_stop()).
     * This should be ony called if any of the user defined entities
     * <b>explicitly</b> want
     * to terminate simulation during execution.
     * @throws NullPointerException This happens when creating this entity
     *                before initializing GridSim package or this entity name is
     *                <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_system#run_stop()
     * @pre $none
     * @post $none
     */
    public static void stopGridSimulation() throws NullPointerException
    {
        try {
            Sim_system.run_stop();
        }
        catch (Sim_exception e)
        {
            throw new NullPointerException("GridSim.stopGridSimulation() : " +
                    "Error - can't stop Grid Simulation.");
        }
    }

    /**
     * Gets an object belong to the first event <b>CURRENTLY</b> waiting in this
     * entity's deferred queue (incoming buffer).
     * If there are no events, then wait indefinitely for an event to arrive.
     * @return An event's object
     * @pre $none
     * @post $none
     */
    protected Object receiveEventObject()
    {
        Sim_event ev = new Sim_event();
        super.sim_get_next(ev);
        return ev.get_data();
    }

    /**
     * Gets an object belong to the first event <b>CURRENTLY</b> waiting in
     * the given port.
     * If there are no events, then wait indefinitely for an event to arrive.
     * @param sourcePort a Sim_port object which is used to connect entities
     *                   for event passing
     * @return An event's data or <tt>null</tt> if the source port is
     *         empty.
     * @see eduni.simjava.Sim_port
     * @pre sourcePort != null
     * @post $none
     */
    protected Object receiveEventObject(Sim_port sourcePort)
    {
        if (sourcePort == null) {
            return null;
        }

        Sim_event ev = new Sim_event();
        super.sim_get_next( new Sim_from_port(sourcePort), ev );
        return ev.get_data();
    }

    /**
     * Sends a Gridlet to the destination GridResource ID <tt>without</tt>
     * any delay. An acknowledgement to denote the successful of this method
     * is by default <tt>off or false</tt>.
     *
     * @param gl       a Gridlet object to be sent
     * @param resID    an unique resource ID
     * @return <tt>true</tt> if this Gridlet has been submitted to the
     *         destination GridResource, <tt>false</tt> otherwise.
     *         Submitting a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if the acknowledgment status in the parameter of this
     *                   method is set to <tt>false</tt>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if a Gridlet object is <tt>null</tt> or empty;
     *              <li> if a Gridlet object has <tt>finished</tt> executing
     *                   beforehand
     *         </ul>
     * @pre gl != null
     * @pre resID >= 0
     * @post $none
     */
    protected boolean gridletSubmit(Gridlet gl, int resID) {
        return gridletSubmit(gl, resID, GridSimTags.SCHEDULE_NOW, false);
    }

    /**
     * Sends a Gridlet to the destination GridResource ID <tt>with</tt>
     * a specified delay.
     *
     * @param gl            a Gridlet object to be sent
     * @param resourceID    an unique resource ID
     * @param delay         delay time or <tt>0.0</tt> if want to execute NOW
     * @param ack           an acknowledgment status. <tt>true</tt> if want to
     *                      know the result of this method, <tt>false</tt>
     *                      otherwise or don't care.
     * @return <tt>true</tt> if this Gridlet has been submitted to the
     *         destination GridResource, <tt>false</tt> otherwise.
     *         Submitting a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if the acknowledgment status in the parameter of this
     *                   method is set to <tt>false</tt>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *              <li> if a Gridlet object is <tt>null</tt> or empty;
     *              <li> if a Gridlet object has <tt>finished</tt> executing
     *                   beforehand
     *         </ul>
     * @see gridsim.Gridlet#isFinished()
     * @see gridsim.GridSim#gridletReceive()
     * @pre gl != null
     * @pre resourceID >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected boolean gridletSubmit(Gridlet gl, int resourceID,
                                    double delay, boolean ack)
    {
        // with default net service level, i.e. 0
        return gridletSubmit(gl, resourceID, delay, ack, 0);
    }

    /**
     * Sends a Gridlet to the destination GridResource ID <tt>with</tt>
     * a specified delay.
     *
     * @param gl            a Gridlet object to be sent
     * @param resourceID    an unique resource ID
     * @param delay         delay time or <tt>0.0</tt> if want to execute NOW
     * @param ack           an acknowledgment status. <tt>true</tt> if want to
     *                      know the result of this method, <tt>false</tt>
     *                      otherwise or don't care.
     * @param netServiceLevel this can be set so that network entities can
     * provide differentiated services if they are supported.
     * @return <tt>true</tt> if this Gridlet has been submitted to the
     *         destination GridResource, <tt>false</tt> otherwise.
     *         Submitting a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if the acknowledgment status in the parameter of this
     *                   method is set to <tt>false</tt>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *              <li> if a Gridlet object is <tt>null</tt> or empty;
     *              <li> if a Gridlet object has <tt>finished</tt> executing
     *                   beforehand
     *         </ul>
     * @see gridsim.Gridlet#isFinished()
     * @see gridsim.GridSim#gridletReceive()
     * @pre gl != null
     * @pre resourceID >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected boolean gridletSubmit(Gridlet gl, int resourceID,
                                    double delay, boolean ack,
                                    int netServiceLevel)
    {
        // checks whether a Gridlet is empty or delay is -ve
        if (gl == null || delay < 0.0 || netServiceLevel < 0) {
            return false;
        }

        // checks whether a Gridlet has finished executing before
        String errorMsg = super.get_name() + ".gridletSubmit(): ";
        if (gl.isFinished())
        {
            System.out.println(errorMsg + "Error - Gridlet #" +
                               gl.getGridletID() + " for User #" +
                               gl.getUserID() + " is already finished.");
            return false;
        }

        boolean valid = false;
        try
        {
            valid = validateValue(errorMsg, gl.getGridletID(),
                                  gl.getUserID(), resourceID);

            // if any of the above value invalid, then return
            if (!valid) {
                return false;
            }

            // sends the gridlet to a destination GridResource id with ACK
            if (ack)
            {
                send(super.output, delay, GridSimTags.GRIDLET_SUBMIT_ACK,
                     new IO_data(gl, gl.getGridletFileSize(), resourceID,
                     netServiceLevel)
                );

                valid = getBooleanResult(gl.getGridletID(),
                                         GridSimTags.GRIDLET_SUBMIT_ACK);
            }
            else   // sends without ACK
            {
                valid = false;
                send(super.output, delay, GridSimTags.GRIDLET_SUBMIT,
                     new IO_data(gl, gl.getGridletFileSize(), resourceID,
                     netServiceLevel)
                );
            }
        }
        catch (Sim_exception sim)
        {
            valid = false;
            System.out.println(errorMsg + "Error from SimJava occurs.");
            System.out.println( sim.getMessage() );
        }
        catch (Exception e)
        {
            valid = false;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }

        return valid;
    }

    /**
     * Gets the result from an incoming event buffer that matches
     * the given Gridlet ID and tag name
     * @param gridletID     a gridlet ID
     * @param matchingTag   a matching tag name
     * @return result of the selected event that matches the criteria
     * @pre $none
     * @post $none
     */
    private boolean getBooleanResult(int gridletID, int matchingTag)
    {
        // waiting for a response back from the GridResource
        FilterResult tag = new FilterResult(gridletID, matchingTag);

        // only look for ack for same Gridlet ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        boolean result = false;
        try
        {
            int[] array = (int[]) ev.get_data();
            if (array[RESULT] == GridSimTags.TRUE) {
                result = true;
            }
        }
        catch (Exception e) {
            result = false;
        }

        return result;
    }

    /**
     * Gets the result from an incoming event buffer that matches
     * the given Gridlet ID and tag name
     * @param gridletID     a gridlet ID
     * @param matchingTag   a matching tag name
     * @return result of the selected event that matches the criteria
     * @pre $none
     * @post $none
     */
    private int getIntResult(int gridletID, int matchingTag)
    {
        // waiting for a response back from the GridResource
        FilterResult tag = new FilterResult(gridletID, matchingTag);

        // only look for ack for same Gridlet ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        int result = -1;
        try
        {
            int[] array = (int[]) ev.get_data();
            result = array[RESULT];
        }
        catch (Exception e) {
            result = -1;
        }

        return result;
    }

    /**
     * Gets a Gridlet belong to the first event <b>CURRENTLY</b> waiting in this
     * entity's deferred queue (incoming buffer).
     * If there are no events, then wait indefinitely for an event to arrive.
     * @return A Gridlet object or <tt>null</tt> if an error occurs.
     * @pre $none
     * @post $none
     */
    protected Gridlet gridletReceive()
    {
        Sim_event ev = new Sim_event();

        // waiting for a response from the GridResource entity
        Sim_type_p tag = new Sim_type_p(GridSimTags.GRIDLET_RETURN);
        super.sim_get_next(tag, ev);   // wait for the correct event type

        Gridlet gl = null;
        try {
            gl = (Gridlet) ev.get_data();
        }
        catch (ClassCastException c) {
            gl = null;
        }
        catch (Exception e) {
            gl = null;
        }

        return gl;
    }

    /**
     * Gets a Gridlet belong to the first event <b>CURRENTLY</b> waiting in this
     * entity's deferred queue (incoming buffer).
     * If there are no events, then wait indefinitely for an event to arrive.
     * @param gridletId   a Gridlet ID
     * @param userId      a user ID
     * @param resId       a grid resource ID
     * @return A Gridlet object or <tt>null</tt> if an error occurs.
     * @pre gridletId >= 0
     * @pre userId > 0
     * @pre resId > 0
     * @post $none
     */
    protected Gridlet gridletReceive(int gridletId, int userId, int resId)
    {
        String errorMsg = super.get_name() + ".gridletReceive(): ";
        boolean valid = validateValue(errorMsg, gridletId, userId, resId);
        if (!valid) {
            return null;
        }

        // waiting for a response from the GridResource entity
        FilterGridlet tag = new FilterGridlet(gridletId, userId, resId);

        // only look for this type of ack for same Gridlet ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        Gridlet gl = null;
        try {
            gl = (Gridlet) ev.get_data();
        }
        catch (ClassCastException c) {
            gl = null;
        }
        catch (Exception e) {
            gl = null;
        }

        return gl;
    }

    /**
     * Gets a Gridlet belong to the first event <b>CURRENTLY</b> waiting in this
     * entity's deferred queue (incoming buffer).
     * If there are no events, then wait indefinitely for an event to arrive.
     * @param gridletId   a Gridlet ID
     * @param resId       a grid resource ID
     * @return A Gridlet object or <tt>null</tt> if an error occurs.
     * @pre gridletId >= 0
     * @pre resId > 0
     * @post $none
     */
    protected Gridlet gridletReceive(int gridletId, int resId)
    {
        String errorMsg = super.get_name() + ".gridletReceive(): ";
        boolean valid = validateValue(errorMsg,gridletId,super.get_id(),resId);
        if (!valid) {
            return null;
        }

        // waiting for a response from the GridResource entity
        FilterGridlet tag = new FilterGridlet(gridletId, resId);

        // only look for this type of ack for same Gridlet ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        Gridlet gl = null;
        try {
            gl = (Gridlet) ev.get_data();
        }
        catch (ClassCastException c) {
            gl = null;
        }
        catch (Exception e) {
            gl = null;
        }

        return gl;
    }

    /**
     * Sends a Gridlet based on their event tag to the destination resource ID.
     * @param errorMsg   a message containing which operation it belongs to
     * @param gridletId  a Gridlet ID
     * @param userId     a Gridlet's user or owner ID
     * @param resourceId    a GridResource ID
     * @param delay      sending delay time
     * @param tag        event tag (such as GridSimTags.GRIDLET_PAUSE, etc...)
     * @param ack        denotes whether want to have an acknowledgment or not
     * @return <tt>true</tt> if the Gridlet has been sent successfully,
     *         <tt>false</tt> otherwise
     * @pre errorMsg != null
     * @pre gridletId > 0
     * @pre userId > 0
     * @pre resourceId > 0
     * @pre delay >= 0.0
     * @pre tag > 0
     * @post $result = true || false
     */
    private boolean sendGridlet(String errorMsg, int gridletId, int userId,
                           int resourceId, double delay, int tag, boolean ack)
    {
        boolean valid = validateValue(errorMsg, gridletId, userId, resourceId);
        if (!valid || delay < 0.0) {
            return false;
        }

        int size = 14;  // size of having 3 ints + 2 bytes overhead
        int[] array = new int[ARRAY_SIZE];
        array[0] = gridletId;
        array[1] = userId;
        array[2] = NOT_FOUND;  // this index is only used by gridletMove()

        // if an ack is required, then change the tag
        int newTag = tag;
        if (ack)
        {
            switch (tag)
            {
                case GridSimTags.GRIDLET_PAUSE:
                    newTag = GridSimTags.GRIDLET_PAUSE_ACK;
                    break;

                case GridSimTags.GRIDLET_RESUME:
                    newTag = GridSimTags.GRIDLET_RESUME_ACK;
                    break;

                default:
                    break;
            }
        }

        // send this Gridlet
        send(super.output, delay, newTag, new IO_data(array, size, resourceId));
        return true;
    }

    /**
     * Performs validation of the given parameters before sending a Gridlet
     * to a GridResource entity
     * @param msg        a message containing which operation it belongs to
     * @param gridletId  a Gridlet ID
     * @param userId     a Gridlet's user or owner ID
     * @param resourceId    a GridResource ID
     * @return <tt>true</tt> if the validation has passed successfully,
     *         <tt>false</tt> otherwise
     * @pre msg != null
     * @pre gridletId > 0
     * @pre userId > 0
     * @pre resourceId > 0
     * @post $result = true || false
     */
    private boolean validateValue(String msg, int gridletId, int userId,
                int resourceId)
    {
        boolean valid = true;

        // Gridlet ID must be 0 or positive
        if (gridletId < 0)
        {
            valid = false;
            System.out.println(msg + "Error - Gridlet ID must be >= 0, not " +
                    gridletId);
        }

        // User ID must be 0 or positive
        if (userId < 0)
        {
            valid = false;
            System.out.println(msg + "Error - User ID must be >= 0, not " +
                    userId);
        }

        // GridResource ID must be 0 or positive
        if (resourceId < 0)
        {
            valid = false;
            System.out.println(msg +
                    "Error - GridResource ID must be >= 0, not " + resourceId);
        }

        // if a grid resource ID doesn't exist in GIS list
        if (!gis_.isResourceExist(resourceId))
        {
            valid = false;
            System.out.println(msg + "Error - GridResource ID #" + resourceId +
                               " doesn't exist");
        }

        return valid;
    }

    /**
     * Cancels a Gridlet that is currently executing in a given GridResource
     * ID <tt>with</tt> a delay. <br>
     * <b>NOTE:</b> Canceling a Gridlet operation can take a long time over a
     *              slow network if the Gridlet size is big.
     * @param gl            a Gridlet object to be canceled
     * @param resourceId    an unique resource ID
     * @param delay         delay time or <tt>0.0</tt> if want to cancel NOW
     * @return the canceled Gridlet or <tt>null</tt if this operation fails.
     *         If a Gridlet has <tt>finished</tt> in time of cancellation, then
     *         this method will return the finished Gridlet.
     *         Canceling a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *              <li> if a Gridlet object is <tt>null</tt> or empty;
     *         </ul>
     * @pre gl != null
     * @pre resourceId >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected Gridlet gridletCancel(Gridlet gl, int resourceId, double delay)
    {
        if (gl == null || delay < 0.0) {
            return null;
        }

        Gridlet obj = gridletCancel( gl.getGridletID(), gl.getUserID(),
                                    resourceId, delay );
        return obj;
    }

    /**
     * Cancels a Gridlet that is currently executing in a given GridResource
     * ID <tt>with</tt> a delay. <br>
     * <b>NOTE:</b> Canceling a Gridlet operation can be slow over a slow
     *              network if the Gridlet size is big.
     * @param gridletId     a Gridlet ID
     * @param userId        the user or owner ID of this Gridlet
     * @param resourceId    an unique resource ID to which this Gridlet was
     *                      previously sent to
     * @param delay         delay time or <tt>0.0</tt> if want to cancel NOW
     * @return the canceled Gridlet or <tt>null</tt if this operation fails.
     *         If a Gridlet has <tt>finished</tt> in time of cancellation, then
     *         this method will return the finished Gridlet.
     *         Canceling a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *         </ul>
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @pre resourceId >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected Gridlet gridletCancel(int gridletId, int userId, int resourceId,
                                    double delay)
    {
        Gridlet gl = null;
        String errorMsg = super.get_name() + ".gridletCancel(): ";
        try
        {
            boolean valid = sendGridlet(errorMsg, gridletId, userId, resourceId,
                                    delay, GridSimTags.GRIDLET_CANCEL, false);

            if (valid)
            {
                // waiting for a response from the GridResource entity
                FilterGridlet tag = new FilterGridlet(gridletId, resourceId);
                tag.setTag(GridSimTags.GRIDLET_CANCEL);

                // only look for this type of ack for same Gridlet ID
                Sim_event ev = new Sim_event();
                super.sim_get_next(tag, ev);
                gl = (Gridlet) ev.get_data();

                // if a gridlet comes with a failed status, it means that
                // a resource could not find the gridlet
                if (gl.getGridletStatus() == Gridlet.FAILED) {
                    gl = null;
                }
            }
        }
        catch (Sim_exception e)
        {
            gl = null;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }
        catch (Exception e)
        {
            gl = null;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }

        return gl;
    }

    /**
     * Pauses a Gridlet that is currently executing in a given GridResource
     * ID <tt>with</tt> a delay.
     * @param gl            a Gridlet object to be sent
     * @param resId         an unique resource ID
     * @param delay         delay time or <tt>0.0</tt> if want to execute NOW
     * @return <tt>true</tt> if this Gridlet has been paused successfully in the
     *         destination GridResource, <tt>false</tt> otherwise.
     *         Pausing a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *              <li> if a Gridlet object is <tt>null</tt> or empty;
     *              <li> if a Gridlet object has <tt>finished</tt> executing
     *                   beforehand. The Gridlet needs to be retrieved by
     *                   using {@link #gridletReceive()}
     *         </ul>
     * @pre gl != null
     * @pre resId >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected boolean gridletPause(Gridlet gl, int resId, double delay)
    {
        if (gl == null || delay < 0.0) {
            return false;
        }

        return gridletPause(gl.getGridletID(),gl.getUserID(),resId,delay,true);
    }

    /**
     * Pauses a Gridlet that is currently executing in a given GridResource
     * ID <tt>with</tt> a delay.
     * @param gridletId     a Gridlet ID
     * @param userId        the user or owner ID of this Gridlet
     * @param resourceId    an unique resource ID
     * @param delay         delay time or <tt>0.0</tt> if want to execute NOW
     * @param ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @return <tt>true</tt> if this Gridlet has been paused successfully in the
     *         destination GridResource, <tt>false</tt> otherwise.
     *         Pausing a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *              <li> if a Gridlet object has <tt>finished</tt> executing
     *                   beforehand. The Gridlet needs to be retrieved by using
     *                   {@link #gridletReceive()}
     *         </ul>
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @pre resourceId >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected boolean gridletPause(int gridletId, int userId, int resourceId,
                                   double delay, boolean ack)
    {
        boolean valid = false;
        String errorMsg = super.get_name() + ".gridletPause(): ";
        try
        {
            valid = sendGridlet(errorMsg, gridletId, userId, resourceId, delay,
                                GridSimTags.GRIDLET_PAUSE, ack);

            if (valid && ack)
            {
                valid = getBooleanResult(gridletId,
                                         GridSimTags.GRIDLET_PAUSE_ACK);
            }
        }
        catch (Sim_exception e)
        {
            valid = false;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }
        catch (Exception e)
        {
            valid = false;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }

        return valid;
    }

    /**
     * Gets the current status of this Gridlet in a given GridResource ID
     * @param gl    a Gridlet object
     * @param resourceId   a GridResource ID that executes this Gridlet object
     * @return the current Gridlet status or <tt>-1</tt> if not found.
     *         The various Gridlet status can be found in Gridlet class.
     * @see gridsim.Gridlet
     * @pre gl != null
     * @pre resourceId > 0
     * @post $none
     */
    protected int gridletStatus(Gridlet gl, int resourceId)
    {
        if (gl == null) {
            return NOT_FOUND;
        }

        return gridletStatus(gl.getGridletID(), gl.getUserID(), resourceId);
    }

    /**
     * Gets the current status of this Gridlet in a given GridResource ID
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner of this Gridlet object
     * @param resourceId   a GridResource ID that executes this Gridlet object
     * @return the current Gridlet status or <tt>-1</tt> if not found.
     *         The various Gridlet status can be found in Gridlet class.
     * @see gridsim.Gridlet
     * @pre gridletId > 0
     * @pre userId > 0
     * @pre resourceId > 0
     * @post $none
     */
    protected int gridletStatus(int gridletId, int userId, int resourceId)
    {
        int status = NOT_FOUND;
        String errorMsg = super.get_name() + "gridletStatus(): ";
        try
        {
            boolean valid = sendGridlet(errorMsg, gridletId, userId, resourceId,
                                        0.0, GridSimTags.GRIDLET_STATUS, false);

            if (!valid) {
                return status;
            }

            // waiting for a response from the GridResource
            status = getIntResult(gridletId, GridSimTags.GRIDLET_STATUS);
        }
        catch (Sim_exception e)
        {
            status = NOT_FOUND;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }
        catch (Exception e)
        {
            status = NOT_FOUND;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }

        return status;
    }

    /**
     * Resumes a Gridlet that is currently pausing in a given GridResource
     * ID <tt>with</tt> a delay.<br>
     * <b>NOTE:</b> Resuming a Gridlet only works if it is currently on paused.
     * @param gl            a Gridlet object to be sent
     * @param resId         an unique resource ID
     * @param delay         delay time or <tt>0.0</tt> if want to execute NOW
     * @return <tt>true</tt> if this Gridlet has been resumed successfully in
     *         the destination GridResource, <tt>false</tt> otherwise.
     *         Resuming a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *              <li> if a Gridlet object is <tt>null</tt> or empty
     *              <li> if a Gridlet is not currently on paused
     *         </ul>
     * @pre gl != null
     * @pre resId >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected boolean gridletResume(Gridlet gl, int resId, double delay)
    {
        if (gl == null || delay < 0.0) {
            return false;
        }

        return gridletResume(gl.getGridletID(),gl.getUserID(),resId,delay,true);
    }

    /**
     * Resumes a Gridlet that is currently pausing in a given GridResource
     * ID <tt>with</tt> a delay. <br>
     * <b>NOTE:</b> Resuming a Gridlet only works if it is currently on paused.
     * @param gridletId     a Gridlet ID
     * @param userId        the user or owner ID of this Gridlet
     * @param resourceId    an unique resource ID
     * @param delay         delay time or <tt>0.0</tt> if want to execute NOW
     * @param ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @return <tt>true</tt> if this Gridlet has been resumed successfully in
     *         the destination GridResource, <tt>false</tt> otherwise.
     *         Resuming a Gridlet can be failed for the one or more
     *         following reasons:
     *         <ul>
     *              <li> if a GridResource ID doesn't exist
     *              <li> if a Gridlet ID doesn't exist
     *              <li> if a Gridlet's user ID doesn't exist
     *              <li> if the delay time is negative
     *              <li> if a Gridlet is not currently on paused
     *         </ul>
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @pre resourceId >= 0
     * @pre delay >= 0.0
     * @post $none
     */
    protected boolean gridletResume(int gridletId, int userId, int resourceId,
                                    double delay, boolean ack)
    {
        boolean valid = false;
        String errorMsg = super.get_name() + ".gridletResume(): ";
        try
        {
            valid = sendGridlet(errorMsg, gridletId, userId, resourceId, delay,
                                GridSimTags.GRIDLET_RESUME, ack);

            if (valid && ack)
            {
                // waiting for a response from the GridResource
                valid = getBooleanResult(gridletId,
                                         GridSimTags.GRIDLET_RESUME_ACK);
            }
        }
        catch (Sim_exception e)
        {
            valid = false;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }
        catch (Exception e)
        {
            valid = false;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }

        return valid;
    }

    /**
     * Moves a Gridlet to the destination GridResource ID
     * @param gl   a Gridlet object
     * @param srcId   the GridResource ID that is currently executing this Gridlet
     * @param destId  the new GridResource ID
     * @param delay   simulation delay
     * @return <tt>true</tt> if this Gridlet has moved successfully,
     *         <tt>false</tt> otherwise. Moving a Gridlet can be failed, due
     *         to one or more following reasons:
     *      <ul>
     *         <li> if a Gridlet object is null
     *         <li> if one or both GridResource ID don't exist
     *         <li> if the delay is negative
     *         <li> if a Gridlet in the GridResource has completed beforehand.
     *              The Gridlet can be retrieved by using
     *              {@link #gridletReceive()}
     *      </ul>
     * @pre gl != null
     * @pre srcId > 0
     * @pre destId > 0
     * @pre delay >= 0.0
     * @post $result = true || false
     */
    protected boolean gridletMove(Gridlet gl,int srcId,int destId,double delay)
    {
        if (gl == null || delay < 0.0) {
            return false;
        }

        boolean success = gridletMove(gl.getGridletID(), gl.getUserID(),
                                      srcId, destId, delay, true);

        return success;
    }

    /**
     * Moves a Gridlet to the destination GridResource ID
     * @param gridletId   a Gridlet ID
     * @param userId      the owner or user ID of this Gridlet
     * @param srcId   the GridResource ID that is currently executing this
     *                Gridlet
     * @param destId  the new GridResource ID
     * @param delay   simulation delay
     * @param ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @return <tt>true</tt> if this Gridlet has moved successfully,
     *         <tt>false</tt> otherwise. Moving a Gridlet can be failed, due
     *         to one or more following reasons:
     *      <ul>
     *         <li> if a Gridlet ID doesn't exist
     *         <li> if the owner of user ID of this Gridlet doesn't exist
     *         <li> if one or both GridResource ID don't exist
     *         <li> if the delay is negative
     *         <li> if a Gridlet in the GridResource has completed beforehand.
     *              The Gridlet can be retrieved by using
     *              {@link #gridletReceive()}
     *      </ul>
     * @pre gridletId > 0
     * @pre userId > 0
     * @pre srcId > 0
     * @pre destId > 0
     * @pre delay >= 0.0
     * @post $result = true || false
     */
    protected boolean gridletMove(int gridletId, int userId, int srcId,
                                  int destId, double delay, boolean ack)
    {
        String errorMsg = super.get_name() + ".gridletMove(): ";

        // check whether the source Id is the same as destination Id
        if (srcId == destId)
        {
            System.out.println(errorMsg + "Error - Can't move a Gridlet to " +
                   "the same GridResource.");
            return false;
        }

        boolean valid = validateValue(errorMsg, gridletId, userId, srcId);
        if (!valid || delay < 0.0) {
            return false;
        }

        // if a destination grid resource ID doesn't exist in GIS list
        if (!gis_.isResourceExist(destId))
        {
            System.out.println(errorMsg + "Error - GridResource ID #" + destId +
                   " doesn't exist. Hence, can't move Gridlet #" + gridletId);

            return false;
        }

        try
        {
            int size = 14;  // size of having 3 ints + 2 bytes overhead
            int[] array = new int[ARRAY_SIZE];
            array[0] = gridletId;
            array[1] = userId;
            array[2] = destId;

            if (ack)
            {
                // sends the info to the destination GridResource
                send(super.output, delay, GridSimTags.GRIDLET_MOVE_ACK,
                     new IO_data(array, size, srcId)
                );

                // waiting for a response from the GridResource
                valid = getBooleanResult(gridletId,
                                         GridSimTags.GRIDLET_SUBMIT_ACK);
            }
            else
            {
                valid = false;
                send(super.output, delay, GridSimTags.GRIDLET_MOVE,
                     new IO_data(array, size, srcId)
                );
            }
        }
        catch (Sim_exception e)
        {
            valid = false;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( e.getMessage() );
        }
        catch (Exception ex)
        {
            valid = false;
            System.out.println(errorMsg + "Error occurs.");
            System.out.println( ex.getMessage() );
        }

        return valid;
    }

    /**
     * Gets the name of this entity
     * @return the Entity name or <tt>null</tt> if this object does not have
     *         one
     * @pre $none
     * @post $none
     */
    public String getEntityName()
    {
        try {
            return super.get_name();
        }
        catch (Sim_exception e) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets name of the entity given its entity ID
     * @param entityID  the entity ID
     * @return the Entity name or <tt>null</tt> if this object does not have
     *         one
     * @pre entityID > 0
     * @post $none
     */
    public static String getEntityName(int entityID)
    {
        try {
            return Sim_system.get_entity(entityID).get_name();
        }
        catch (Sim_exception e) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets name of the entity given its entity ID
     * @param entityID  the entity ID
     * @return the Entity name or <tt>null</tt> if this object does not have
     *         one
     * @pre entityID > 0
     * @post $none
     */
    public static String getEntityName(Integer entityID)
    {
        if (entityID != null) {
            return GridSim.getEntityName( entityID.intValue() );
        }

        return null;
    }

    /**
     * Gets the entity ID given its name
     * @param entityName    an Entity name
     * @return the Entity ID or <tt>-1</tt> if it is not found
     * @pre entityName != null
     * @post $result >= -1
     */
    public static int getEntityId(String entityName)
    {
        if (entityName == null) {
            return NOT_FOUND;
        }

        try {
            return Sim_system.get_entity_id(entityName);
        }
        catch (Sim_exception e) {
            return NOT_FOUND;
        }
        catch (Exception e) {
            return NOT_FOUND;
        }
    }

    /**
     * Gets the entity ID of <tt>GridStatistics</tt>
     * @return the Entity ID or <tt>-1</tt> if it is not found
     * @pre $none
     * @post $result >= -1
     */
    public static int getGridStatisticsEntityId() {
        return statsID_;
    }

    /**
     * Gets the entity ID of <tt>GridInformationService</tt>
     * @return the Entity ID or <tt>-1</tt> if it is not found
     * @pre $none
     * @post $result >= -1
     */
    public static int getGridInfoServiceEntityId() {
        return gisID_;
    }

    /**
     * Gets the entity ID of <tt>GridInformationService</tt>
     * @return the GIS entity ID or <tt>-1</tt> if it is not found
     * @pre $none
     * @post $result >= -1
     */
    public static int getGISId() {
        return gisID_;
    }

    /**
     * Gets the entity id of {@link gridsim.GridSimShutdown}
     * @return the Entity ID or <tt>-1</tt> if it is not found
     * @pre $none
     * @post $result >= -1
     */
    public static int getGridSimShutdownEntityId() {
        return shutdownID_;
    }

    /**
     * Tells all user entities to shut down the simulation.
     * {@link gridsim.GridSimShutdown} entity waits for all users
     * termination before shuting down other entities.
     * @see gridsim.GridSimShutdown
     * @pre $none
     * @post $none
     */
    protected void shutdownUserEntity()
    {
        if (shutdownID_ != NOT_FOUND) {
            send(shutdownID_, 0.0, GridSimTags.END_OF_SIMULATION);
        }
    }

    /**
     * Tells the <tt>GridStatistics</tt> entity the end of the simulation
     * @pre $none
     * @post $none
     */
    protected void shutdownGridStatisticsEntity()
    {
        if (statsID_ != NOT_FOUND) {
            send(statsID_, 0.0, GridSimTags.END_OF_SIMULATION);
        }
    }

    /**
     * Sends a request to Grid Information Service (GIS) entity to get the
     * list of all Grid resources
     * @return A LinkedList containing GridResource ID (as an Integer object)
     *         or <tt>null</tt> if a GIS entity hasn't been created before
     * @pre $none
     * @post $none
     */
    public static LinkedList getGridResourceList()
    {
        if (gis_ == null) {
            return null;
        }

        return gis_.getList();
    }

    /**
     * Checks whether a particular resource supports Advanced Reservation
     * functionalities or not.
     * @param resourceID    a resource ID
     * @return <tt>true</tt> if a resource supports Advanced Reservation
     *         functionalities, <tt>false</tt> otherwise
     * @pre resourceID > 0
     * @post $none
     */
    public static boolean resourceSupportAR(int resourceID)
    {
        if (gis_ == null) {
            return false;
        }

        return gis_.resourceSupportAR(resourceID);
    }

    /**
     * Checks whether a particular resource supports Advanced Reservation
     * functionalities or not.
     * @param resourceID    a resource ID
     * @return <tt>true</tt> if a resource supports Advanced Reservation
     *         functionalities, <tt>false</tt> otherwise
     * @pre resourceID != null
     * @post $none
     */
    public static boolean resourceSupportAR(Integer resourceID)
    {
        if (gis_ == null || resourceID == null) {
            return false;
        }

        return gis_.resourceSupportAR(resourceID);
    }

    /**
     * Sends a request to Grid Information Service (GIS) entity to get the
     * list of Grid resources only that support <b>Advanced Reservation</b>
     * @return A LinkedList containing GridResource ID (as an Integer object)
     *         or <tt>null</tt> if a GIS entity hasn't been created before
     * @pre $none
     * @post $none
     */
    public static LinkedList getAdvancedReservationList()
    {
        if (gis_ == null) {
            return null;
        }

        return gis_.getAdvReservList();
    }

    /**
     * Checks whether the given GridResource ID exists or not
     * @param id    a GridResource id
     * @return <tt>true</tt> if the given ID exists, <tt>false</tt> otherwise
     * @pre id >= 0
     * @post $none
     */
    public static boolean isResourceExist(int id)
    {
        if (gis_ == null) {
            return false;
        }

        return gis_.isResourceExist(id);
    }

    /**
     * Checks whether the given GridResource ID exists or not
     * @param id    a GridResource id
     * @return <tt>true</tt> if the given ID exists, <tt>false</tt> otherwise
     * @pre id != null
     * @post $none
     */
    public static boolean isResourceExist(Integer id)
    {
        if (gis_ == null || id == null) {
            return false;
        }

        return GridSim.isResourceExist(id);
    }

    /**
     * Checks whether simulation's statistics of other log should be created
     * @return <tt>true</tt> if the information should be logged
     * or <tt>false</tt> otherwise.
     */
    public static boolean isTraceEnabled() {
    	return traceFlag_;
    }

    /**
     * Gets the total number of PEs (Processing Elements) from a resource
     * @param resourceID  a resource ID
     * @return total number of PE or <tt>-1</tt> if invalid resource ID
     * @pre resourceID > 0
     * @post $none
     */
    public int getNumPE(int resourceID) {
        return getResourcePE(resourceID, GridSimTags.RESOURCE_NUM_PE);
    }

    /**
     * Gets the number of PEs (Processing Elements) from a resource
     * @param resourceID  a resource ID
     * @return total number of PEs or <tt>-1</tt> if invalid resource ID
     * @pre resourceID != null
     * @post $none
     */
    public int getNumPE(Integer resourceID)
    {
        if (resourceID == null) {
            return NOT_FOUND;
        }

        return getNumPE( resourceID.intValue() );
    }

    /**
     * Gets the total number of free PEs (Processing Elements) from a resource.
     * <br>
     * NOTE: Due to the dynamic nature of a Grid environment, the number of
     *       free PEs only reflect to the current status of a resource at the
     *       time of this request received.
     *
     * @param resourceID  a resource ID
     * @return total number of free PE or <tt>-1</tt> if invalid resource ID
     * @pre resourceID > 0
     * @post $none
     */
    public int getNumFreePE(int resourceID) {
        return getResourcePE(resourceID, GridSimTags.RESOURCE_NUM_FREE_PE);
    }

    /**
     * Gets the total number of free PEs (Processing Elements) from a resource.
     * <br>
     * NOTE: Due to the dynamic nature of a Grid environment, the number of
     *       free PEs only reflect to the current status of a resource at the
     *       time of this request received.
     *
     * @param resourceID  a resource ID
     * @return total number of free PE or <tt>-1</tt> if invalid resource ID
     * @pre resourceID != null
     * @post $none
     */
    public int getNumFreePE(Integer resourceID)
    {
        if (resourceID == null) {
            return NOT_FOUND;
        }

        return getNumFreePE( resourceID.intValue() );
    }

    /**
     * Gets the total number of PEs (Processing Elements) from a resource
     * @param resourceID  a resource ID
     * @param eventTag    an event tag name or type of request
     * @return total number of PE or <tt>-1</tt> if invalid resource ID
     * @pre resourceID > 0
     * @post $none
     */
    private int getResourcePE(int resourceID, int eventTag)
    {
        if (!isResourceExist(resourceID)) {
            return NOT_FOUND;
        }

        super.send(super.output, 0.0, eventTag,
                   new IO_data(new Integer(super.get_id()), SIZE, resourceID));

        // waiting for a response from the GridResource
        Sim_event ev = new Sim_event();
        Sim_type_p tag = new Sim_type_p(eventTag);

        // only look for this type of ack
        super.sim_get_next(tag, ev);

        int result = -1;
        try
        {
            Integer obj = (Integer) ev.get_data();
            result = obj.intValue();
        }
        catch (Exception e) {
            System.out.println(super.get_name() +
                    ".getNumPE(): Exception error.");
        }

        return result;
    }

    /**
     * Gets a ResourceCharacteristics object for a given GridResource ID. <br>
     * NOTE: This method returns a reference of ResourceCharacteristics object,
     *       NOT a copy of it. As a result, some of ResourceCharacteristics
     *       attributes, such as {@link gridsim.PE} availability might
     *       change over time.
     *       Use {@link #getNumFreePE(int)} to determine number of free PE
     *       at the time of a request instead.
     *
     * @param resourceID   the resource ID
     * @return An object of ResourceCharacteristics or <tt>null</tt> if a
     *         GridResource doesn't exist or an error occurs
     * @see gridsim.ResourceCharacteristics
     * @pre resourceID > 0
     * @post $none
     */
    public ResourceCharacteristics getResourceCharacteristics(int resourceID)
    {
        if (!gis_.isResourceExist(resourceID)) {
            return null;
        }

        // Get Resource Characteristic Info: Send Request and Receive Event/Msg
        send(super.output, 0.0, GridSimTags.RESOURCE_CHARACTERISTICS,
             new IO_data( new Integer(super.get_id()), SIZE, resourceID)
        );

        try
        {
            // waiting for a response from system GIS
            Sim_type_p tag=new Sim_type_p(GridSimTags.RESOURCE_CHARACTERISTICS);

            // only look for this type of ack
            Sim_event ev = new Sim_event();
            super.sim_get_next(tag, ev);
            return (ResourceCharacteristics) ev.get_data();
        }
        catch (Exception e) {
            System.out.println(super.get_name() +
                    ".getResourceCharacteristics(): Exception error.");
        }

        return null;
    }

    /**
     * Gets the GridResource dynamic fnformation
     * @param resourceID   the resource ID
     * @return An object of Accumulator containing the GridResource load or
     *         <tt>null</tt> if a GridResource doesn't exist or an error occurs
     * @pre resourceID > 0
     * @post $none
     */
    public Accumulator getResourceDynamicInfo(int resourceID)
    {
        if (!gis_.isResourceExist(resourceID)) {
            return null;
        }

        // Get Resource Dynamic information
        send(super.output, 0.0, GridSimTags.RESOURCE_DYNAMICS,
             new IO_data( new Integer(super.get_id()), SIZE, resourceID )
        );

        try
        {
            // waiting for a response from system GIS
            Sim_type_p tag = new Sim_type_p(GridSimTags.RESOURCE_DYNAMICS);

            // only look for this type of ack
            Sim_event ev = new Sim_event();
            super.sim_get_next(tag, ev);
            return (Accumulator) ev.get_data();

        }
        catch (Exception e) {
            System.out.println(super.get_name() +
                    ".getResourceDynamicInfo(): Exception error.");
        }

        return null;
    }


    ////////////  METHOD FOR RECORDING Statistics Information BEGIN ////////

    /**
     * Records statistics during the event
     * @param category  a category name
     * @param data      a value to be recorded
     * @pre category != null
     * @pre data >= 0.0
     * @post $none
     */
    public void recordStatistics(String category, double data)
    {
        if (statsID_ == NOT_FOUND || category == null) {
            return;
        }

        super.send(statsID_, 0.0, GridSimTags.RECORD_STATISTICS,
             new Stat( GridSim.clock(), category, super.get_name(), data )
        );
    }

    /**
     * Records statistics during the event
     * @param category  a category name
     * @param data      a value to be recorded
     * @pre category != null
     * @pre data >= 0.0
     * @post $none
     */
    public void recordStatistics(String category, int data)
    {
        if (statsID_ == NOT_FOUND || category == null) {
            return;
        }

        super.send(statsID_, 0.0, GridSimTags.RECORD_STATISTICS,
             new Stat( GridSim.clock(), category, super.get_name(), data )
        );
    }

    /**
     * Records statistics during the event
     * @param category  a category name
     * @param data      a value to be recorded
     * @pre category != null
     * @pre data != null
     * @post $none
     */
    public void recordStatistics(String category, String data)
    {
        if (statsID_ == NOT_FOUND || category == null || data == null) {
            return;
        }

        super.send(statsID_, 0.0, GridSimTags.RECORD_STATISTICS,
             new Stat( GridSim.clock(), category, super.get_name(), data )
        );
    }

    /**
     * Records statistics during the event
     * @param category  a category name
     * @param data      a value to be recorded
     * @pre category != null
     * @pre data == true || false
     * @post $none
     */
    public void recordStatistics(String category, boolean data)
    {
        if (statsID_ == NOT_FOUND || category == null) {
            return;
        }

        super.send(statsID_, 0.0, GridSimTags.RECORD_STATISTICS,
             new Stat( GridSim.clock(), category, super.get_name(), data )
        );
    }

    /**
     * Initializes the {@link gridsim.GridSimCore#NETWORK_TYPE} to be used in
     * the simulation. By default, the {@link gridsim.GridSimCore#NETWORK_TYPE}
     * is set to {@link gridsim.GridSimTags#NET_PACKET_LEVEL}.
     *
     * @param networkType  network type
     * @return <tt>true</tt> if the network type has been initialized successfully
     * or <tt>false</tt> otherwise.
     *
     * @see gridsim.GridSimCore#NETWORK_TYPE
     * @see gridsim.GridSimTags#NET_PACKET_LEVEL
     * @see gridsim.GridSimTags#NET_FLOW_LEVEL
     * @see gridsim.GridSimTags#NET_BUFFER_PACKET_LEVEL
     */
    public static boolean initNetworkType(int networkType)
    {
        boolean result = true;
        switch(networkType)
        {
            case GridSimTags.NET_PACKET_LEVEL:
                GridSimCore.NETWORK_TYPE = GridSimTags.NET_PACKET_LEVEL;
                break;

            case GridSimTags.NET_FLOW_LEVEL:
                GridSimCore.NETWORK_TYPE = GridSimTags.NET_FLOW_LEVEL;
                break;

            case GridSimTags.NET_BUFFER_PACKET_LEVEL:
                GridSimCore.NETWORK_TYPE = GridSimTags.NET_BUFFER_PACKET_LEVEL;
                break;

            default:
                result = false;
                break;
        }

        return result;
    }

    /** Returns the network type used in this simulation.
     * @return the network type
     * @see gridsim.GridSimCore#NETWORK_TYPE
     * @see gridsim.GridSimTags#NET_PACKET_LEVEL
     * @see gridsim.GridSimTags#NET_FLOW_LEVEL
     * @see gridsim.GridSimTags#NET_BUFFER_PACKET_LEVEL
     */
    public static int getNetworkType() {
        return GridSimCore.NETWORK_TYPE;
    }

} 

