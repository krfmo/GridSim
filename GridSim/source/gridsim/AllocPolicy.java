/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import eduni.simjava.Sim_entity;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_port;


/**
 * AllocPolicy is an abstract class that handles the internal
 * {@link gridsim.GridResource}
 * allocation policy. New scheduling algorithms can be added into a GridResource
 * entity by extending this class and implement the required abstract methods.
 * <p>
 * All the implementation details and the data structures chosen are up to
 * the child class. All the protected methods and attributes are available
 * to code things easier.
 * <p>
 * Since GridSim 3.0, scheduling algorithm or allocation policy that requires
 * Advanced Reservation functionalities need to extend from
 * {@link gridsim.ARPolicy} class instead.
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 2.2
 * @see gridsim.ARPolicy
 * @see gridsim.GridSim
 * @see gridsim.ResourceCharacteristics
 * @invariant $none
 */
public abstract class AllocPolicy extends Sim_entity
{
    /** The GridResource characteristics object, same as the one in
     * GridResource class
     */
    protected ResourceCharacteristics resource_;

    /** The GridResource Calendar, same as the one in
     * GridResource class
     */
    protected ResourceCalendar resCalendar_;

    /** The GridResource output port. This port is mainly used to send
     * Gridlets or any other messages by this Allocation Policy class.
     * This is because an Allocation Policy class doesn't have networked
     * entities (Input and Output).
     */
    protected Sim_port outputPort_;

    /** The total number of PEs that this resource has. */
    protected int totalPE_;

    /** This GridResource ID */
    protected int resId_;

    /** This class entity ID */
    protected final int myId_;

    /** This GridResource name */
    protected final String resName_;

    /** Initial simulation time as given in <tt>GridSim.init().
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *      String)
     */
    protected long initTime_;

    // for statistical purposes to determine the load of this scheduler
    private Accumulator accTotalLoad_;
    private boolean endSimulation_;  // denotes the end of simulation
    private static final int ARRAY_SIZE = 2;  // [0] = gridlet id and [1] = result

    ///////////////////// ABSTRACT METHODS /////////////////////////////

    /**
     * An <tt>abstract</tt> method that schedules a new Gridlet
     * received by a GridResource entity.
     * <p>
     * For a Gridlet that requires many Processing Elements (PEs) or CPUs,
     * the gridlet length is calculated only for 1 PE. <br>
     * For example, a Gridlet has a length of 500 MI and requires 2 PEs.
     * This means each PE will execute 500 MI of this job.
     * If this scheduler can only execute 1 Gridlet per PE, then it is up
     * to the scheduler to either double the gridlet length up to 1,000 MI
     * or to leave the length as it is.
     * <p>
     * In the beginning of this code, a <tt>ResGridlet</tt> object should be
     * created. The <tt>ResGridlet</tt> object is very useful since it keeps
     * track of related time information during execution of this Gridlet.
     * <p>
     * If an acknowledgement is required, then at the end of this method,
     * should include the following code:
     * <code>
     * <br><br>
     * ... // other code <br> <br>
     * // sends back an ack if required <br>
     * boolean success = true; // If this method success, false otherwise <br>
     * if (ack == true) { <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;sendAck(GridSimTags.GRIDLET_SUBMIT_ACK,
     *        success, gl.getGridletID(), gl.getUserID() ); <br>
     * } <br>
     * <br>
     * </code>
     * @param   gl    a Gridlet object that is going to be executed
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @see gridsim.ResGridlet
     * @see gridsim.ResGridletList
     * @pre gl != null
     * @post $none
     */
    public abstract void gridletSubmit(Gridlet gl, boolean ack);

    /**
     * An <tt>abstract</tt> method that cancels a Gridlet in an execution list.
     * When writing this code, there are few things to consider:
     * <ul>
     *     <li> if a Gridlet can't be found in any data structures
     *     <li> if a Gridlet has finished executing upon canceling
     *     <li> if a Gridlet can be canceled
     * </ul>
     * <p>
     * This method is always required to send back the Gridlet to sender.
     * If the Gridlet is not found, then send back a <tt>null</tt> Gridlet.
     * Therefore, at the end of this method, should include the following
     * code:
     * <code>
     * <br><br>
     * ... // other code <br> <br>
     * // A ResGridlet object stored in a container or other data structure<br>
     * // before exit, finalize all the Gridlet's relevant time information<br>
     * resGridlet.finalizeGridlet(); <br>
     * <br>
     * // sends the Gridlet back to sender <br>
     * // Here, <tt>gridlet</tt> can be <tt>null</tt> if not found <br>
     * Gridlet gridlet = resGridlet.getGridlet(); <br>
     * sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, gridlet,
     *                          gridletId, userId);
     *
     * <br>
     * </code>
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    public abstract void gridletCancel(int gridletId, int userId);

    /**
     * An <tt>abstract</tt> method that pauses a Gridlet during an execution.
     * <p>
     * If an acknowledgement is required, then at the end of this method,
     * should include the following code:
     * <code>
     * <br><br>
     * ... // other code <br> <br>
     * // sends back an ack if required <br>
     * boolean success = true; // If this method success, false otherwise <br>
     * if (ack == true) { <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;sendAck(GridSimTags.GRIDLET_PAUSE_ACK,
     *        success, gl.getGridletID(), gl.getUserID() ); <br>
     * } <br>
     * <br>
     * </code>
     *
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    public abstract void gridletPause(int gridletId, int userId, boolean ack);

    /**
     * An <tt>abstract</tt> method that resumes a previously paused Gridlet.
     * <p>
     * If an acknowledgement is required, then at the end of this method,
     * should include the following code:
     * <code>
     * <br><br>
     * ... // other code <br> <br>
     * // sends back an ack if required <br>
     * boolean success = true; // If this method success, false otherwise <br>
     * if (ack == true) { <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;sendAck(GridSimTags.GRIDLET_RESUME_ACK,
     *        success, gl.getGridletID(), gl.getUserID() ); <br>
     * } <br>
     * <br>
     * </code>
     *
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    public abstract void gridletResume(int gridletId, int userId, boolean ack);

    /**
     * An <tt>abstract</tt> method that finds the status of a Gridlet.
     * This method doesn't need to send back anything since it will
     * automatically handle by GridResource class once the status is found.
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @return the Gridlet status or <tt>-1</tt> if not found
     * @see gridsim.Gridlet
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    public abstract int gridletStatus(int gridletId, int userId);

    /**
     * An <tt>abstract</tt> method that moves a Gridlet to another
     * GridResource entity.
     * When writing this code, there are few things to consider:
     * <ul>
     *     <li> if a Gridlet can't be found in any data structures. <br>
     *          Hence, only need to send an ack with a <tt>false</tt>
     *          boolean value by using
     *          {@link #sendAck(int, boolean, int, int)}.<br><br>
     *
     *     <li> if a Gridlet has finished executing upon moving. <br>
     *          Then need to send an ack with a <tt>false</tt>
     *          boolean value by using
     *          {@link #sendAck(int, boolean, int, int)}. <br>
     *          Moreover, need to send the completed Gridlet by using
     *          {@link #sendFinishGridlet(Gridlet)}.<br><br>
     *
     *     <li> if a Gridlet can be moved, then need to use
     *          {@link #gridletMigrate(Gridlet, int, boolean)}.
     * </ul>
     *
     *
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @param destId       a new destination GridResource ID for this Gridlet
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @pre gridletId > 0
     * @pre userId > 0
     * @pre destId > 0
     * @post $none
     */
    public abstract void gridletMove(int gridletId, int userId, int destId,
                                     boolean ack);

    ///////////////////// End of Abstract methods ///////////////////////////

    /**
     * Overrides this method when executing or scheduling newly-defined tags.
     * This method is called by
     * {@link gridsim.GridResource#processOtherEvent(Sim_event)}
     * for an event with an unknown tag.
     * This approach is desirable if you do not want to create a new type of
     * grid resource.
     *
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    public void processOtherEvent(Sim_event ev)
    {
        if (ev == null)
        {
            System.out.println(resName_ + ".processOtherEvent(): " +
                    "Error - an event is null.");
            return;
        }

        System.out.println(resName_ + ".processOtherEvent(): Unable to " +
                "handle request from an event with a tag number " +
                ev.get_tag() );
    }

    /**
     * Gets the total load for this GridResource
     * @return an Accumulator object
     * @pre $none
     * @post $result != null
     */
    public Accumulator getTotalLoad() {
        return accTotalLoad_;
    }

    /**
     * Sets the end of simulation for this entity. Normally, the GridResource
     * entity will set this flag.
     * @pre $none
     * @post $none
     */
    public void setEndSimulation() {
        endSimulation_ = true;
    }

    /**
     * Checks whether it is the end of a simulation or not
     * @return <tt>true</tt> if it is the end of a simulation, <tt>false</tt>
     *         otherwise
     * @pre $none
     * @post $none
     */
    protected boolean isEndSimulation() {
        return endSimulation_;
    }

    /**
     * Initializes all important attributes.  Normally, the GridResource
     * entity will call this method upon its constructor.
     * @param res   a ResourceCharacteristics object
     * @param cal   a ResourceCalendar object
     * @param port  a Sim_port object
     * @pre res != null
     * @pre cal != null
     * @pre port != null
     * @post $none
     */
    public void init(ResourceCharacteristics res, ResourceCalendar cal,
                     Sim_port port)
    {
        // default values
        resource_ = res;
        resCalendar_ = cal;
        outputPort_ = port;
        totalPE_ = resource_.getNumPE();
        resId_ = resource_.getResourceID();

        double load = calculateTotalLoad(0);
        accTotalLoad_.add(load);

        // looking at the init simulation time
        Calendar calendar = GridSim.getSimulationCalendar();
        long simTime = calendar.getTimeInMillis();
        int simTimeZone = calendar.getTimeZone().getRawOffset() /
                          AdvanceReservation.HOUR;

        // then convert into the local resource time
        initTime_ = AdvanceReservation.convertTimeZone( simTime, simTimeZone,
                           resource_.getResourceTimeZone() );
    }


    ////////////////////// PROTECTED METHODS //////////////////////////////

    /**
     * Allocates a new AllocPolicy object. A child class should call this method
     * during its constructor. The name of this entity (or the child class that
     * inherits this class) will be <tt>"resName_entityName"</tt>.
     *
     * @param resName    the GridResource entity name that will contain
     *                   this allocation policy
     * @param entityName      this object entity name
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
     *
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre resName != null
     * @pre entityName != null
     * @post $none
     */
    protected AllocPolicy(String resName, String entityName) throws Exception
    {
        super(resName + "_" + entityName);

        myId_ = GridSim.getEntityId(resName + "_" + entityName);
        resName_ = resName;
        initTime_ = 0;

        // default value
        outputPort_ = null;
        resource_ = null;
        resCalendar_ = null;
        endSimulation_ = false;
        totalPE_ = 0;
        accTotalLoad_ = new Accumulator();
    }

    /**
     * Adds the given load into the overall total load for this entity
     * @param load   current GridResource load
     * @pre load >= 0.0
     * @post $none
     */
    protected void addTotalLoad(double load) {
        accTotalLoad_.add(load);
    }

    /**
     * Finds a Gridlet inside a given list. This method needs a combination of
     * Gridlet Id and User Id because each Grid User might have exactly the
     * same Gridlet Id submitted to this GridResource.
     *
     * @param obj   a Collection object that contains a list of ResGridlet
     * @param gridletId    a Gridlet Id
     * @param userId       an User Id
     * @return the location of a Gridlet or <tt>-1</tt> if not found
     * @deprecated As GridSim Version 5.0 this method has been replaced by: 
     * {@link GridletList#indexOf(int, int)} and 
     * {@link ResGridletList#indexOf(int, int)}
     * @pre obj != null
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @post $none
     */
    protected int findGridlet(Collection obj, int gridletId, int userId)
    {
        ResGridlet rgl = null;
        int found = -1;     // means the Gridlet is not in the list

        try
        {
            // Search through the list to find the given Gridlet object
            int i = 0;
            Iterator iter = obj.iterator();
            while ( iter.hasNext() )
            {
                rgl = (ResGridlet) iter.next();

                // Need to check against the userId as well since
                // each user might have same Gridlet id submitted to
                // same GridResource
                if (rgl.getGridletID()==gridletId && rgl.getUserID()==userId)
                {
                    found = i;
                    break;
                }

                i++;
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                               ".findGridlet(): Exception error occurs.");
            System.out.println( e.getMessage() );
        }

        return found;
    }

    /**
     * Calculates the current load of a GridResource for a given number of
     * Gridlets currently in execution. This method can be overridden by
     * child class, if the below algorithm isn't suitable for a particular
     * type of scheduling:
     * <code>
     * <br> <br>
     * // totalPE = total PEs that this GridResource has. <br>
     * // It can be found out using ResourceCharacteristics.getNumPE(); <br>
     * int numGridletPerPE = (totalGridletSize + 1) / totalPE;  <br><br>
     * // load is between [0.0, 1.0] where 1.0 is busy and 0.0 is not busy<br>
     * double localLoad = resCalendar_.getCurrentLoad(); <br>
     * double totalLoad = 1.0 - ( (1 - localLoad) / numGridletPerPE ); <br>
     * <br>
     * </code>
     *
     * @param size  total Gridlets in execution size
     * @return total load between range [0.0, 1.0]
     * @see gridsim.ResourceCharacteristics#getNumPE()
     * @see gridsim.ResourceCalendar#getCurrentLoad()
     * @pre $none
     * @post $none
     */
    protected double calculateTotalLoad(int size)
    {
        double val = (size + 1.0) / totalPE_;
        int numGridletPerPE = (int) Math.ceil(val);

        // load is between [0.0, 1.0] where 1.0 is busy and 0.0 is not busy
        double localLoad = resCalendar_ == null ? 0.0 : resCalendar_.getCurrentLoad();
        double load = 1.0 - ( (1 - localLoad) / numGridletPerPE );
        if (load < 0.0) {
            load = 0.0;
        }

        return load;
    }

    /**
     * Sends an acknowledgement to the sender. This method is only suitable
     * for the following tags:
     * <ul>
     *     <li> GridSimTags.GRIDLET_PAUSE_ACK
     *     <li> GridSimTags.GRIDLET_RESUME_ACK
     *     <li> case GridSimTags.GRIDLET_SUBMIT_ACK
     * </ul>
     *
     * <p>
     * At the receiving end, <tt>gridletPause()</tt>, <tt>gridletResume()</tt>
     * and <tt>gridletSubmit()</tt> will be responsible for this acknowledgment
     * data.
     *
     * @param tag   event tag as described above
     * @param status   <tt>true</tt> if the operation has been completed
     *                 successfully, <tt>false</tt> otherwise
     * @param gridletId   the Gridlet ID
     * @param destId      the sender ID. This can also be the user or owner's
     *                    ID for this Gridlet
     * @return <tt>true</tt> if an acknowledgment has been sent successfully,
     *         <tt>false</tt> otherwise
     * @see gridsim.GridSim#gridletPause(int, int, int, double, boolean)
     * @see gridsim.GridSim#gridletResume(int, int, int, double, boolean)
     * @see gridsim.GridSim#gridletSubmit(Gridlet, int, double, boolean)
     * @pre tag >= 0
     * @pre gridletId >= 0
     * @pre destId >= 0
     * @post $none
     */
    protected boolean sendAck(int tag,boolean status,int gridletId,int destId)
    {
        boolean success = false;
        switch (tag)
        {
            case GridSimTags.GRIDLET_PAUSE_ACK:
            case GridSimTags.GRIDLET_RESUME_ACK:
            case GridSimTags.GRIDLET_SUBMIT_ACK:

                int[] array = new int[ARRAY_SIZE];
                array[0] = gridletId;
                if (status) {
                    array[1] = GridSimTags.TRUE;
                }
                else {
                    array[1] = GridSimTags.FALSE;
                }

                super.sim_schedule( outputPort_, GridSimTags.SCHEDULE_NOW, tag,
                                    new IO_data(array, 8, destId) );
                success = true;
                break;

            default:
                System.out.println(super.get_name() +
                                   ".sendAck(): Invalid tag ID.");
                break;
        }

        return success;
    }

    /**
     * Sends the canceled Gridlet back to sender. This method is only valid
     * for GridSimTags.GRIDLET_CANCEL.
     *
     * @param tag   event tag as described above
     * @param gl    a Gridlet object
     * @param gridletId   the Gridlet ID
     * @param destId      the sender ID. This can also be the user or owner's
     *                    ID for this Gridlet
     * @return <tt>true</tt> if the Gridlet has been sent successfully,
     *         <tt>false</tt> otherwise
     * @see gridsim.GridSim#gridletCancel(int, int, int, double)
     * @pre tag >= 0
     * @pre gridletId >= 0
     * @pre destId >= 0
     * @post $none
     */
    protected boolean sendCancelGridlet(int tag, Gridlet gl, int gridletId,
                                        int destId)
    {
        if (tag != GridSimTags.GRIDLET_CANCEL) {
            return false;
        }

        long gridletSize = 0;
        if (gl != null) {
            gridletSize = gl.getGridletOutputSize();
        }

        // if no Gridlet found, then create a new Gridlet but set its status
        // to FAILED. Then, most importantly, set the resource parameters
        // because the user will search/filter based on a resource ID.
        else if (gl == null)
        {
            try
            {
                gridletSize = 100;
                gl = new Gridlet(gridletId, 0, gridletSize, gridletSize);
                gl.setGridletStatus(Gridlet.FAILED);
                gl.setResourceParameter(resId_, resource_.getCostPerSec());
            }
            catch(Exception e) {
                // empty ...
            }
        }

        super.sim_schedule( outputPort_, GridSimTags.SCHEDULE_NOW, tag,
                            new IO_data(gl, gridletSize, destId) );

        return true;
    }

    /**
     * Migrates a Gridlet from this GridResource ID to the destination ID
     * @param   gl    a Gridlet object that is going to be executed
     * @param destId  a new destination GridResource Id
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @return <tt>true</tt> if the Gridlet has been sent successfully,
     *         <tt>false</tt> otherwise
     * @see gridsim.GridSim#gridletMove(int, int, int, int, double, boolean)
     * @pre gl != null
     * @pre destId >= 0
     * @post $none
     */
    protected boolean gridletMigrate(Gridlet gl, int destId, boolean ack)
    {
        if (gl == null) {
            return false;
        }

        IO_data data = new IO_data(gl, gl.getGridletOutputSize(), destId);

        int tag = 0;
        if (ack) {
            tag = GridSimTags.GRIDLET_SUBMIT_ACK;
        }
        else {
            tag = GridSimTags.GRIDLET_SUBMIT;
        }

        super.sim_schedule(outputPort_, GridSimTags.SCHEDULE_NOW, tag, data);
        return true;
    }

    /**
     * Sends the completed Gridlet back to sender or Gridlet's user ID
     * @param gl  a completed Gridlet object
     * @return <tt>true</tt> if the Gridlet has been sent successfully,
     *         <tt>false</tt> otherwise
     * @pre gl != null
     * @post $none
     */
    protected boolean sendFinishGridlet(Gridlet gl)
    {
        IO_data obj = new IO_data(gl,gl.getGridletOutputSize(),gl.getUserID());
        super.sim_schedule(outputPort_, 0, GridSimTags.GRIDLET_RETURN, obj);

        return true;
    }

    /**
     * Sends an internal event to itself
     * @param time   the simulation delay time
     * @return <tt>true</tt> if the event has been sent successfully,
     *         <tt>false</tt> otherwise
     * @pre time >= 0.0
     * @post $none
     */
    protected boolean sendInternalEvent(double time)
    {
        if (time < 0.0) {
            time = 0.0;
        }

        super.sim_schedule(myId_, time, GridSimTags.INSIGNIFICANT);
        return true;
    }

    /**
     * Sends an internal event to itself with a certain tag
     * @param time    the simulation delay time
     * @param tag     a tag ID
     * @return <tt>true</tt> if the event has been sent successfully,
     *         <tt>false</tt> otherwise
     * @pre time >= 0.0
     * @post $none
     */
    protected boolean sendInternalEvent(double time, int tag)
    {
        if (time < 0.0) {
            time = 0.0;
        }

        super.sim_schedule(myId_, time, tag);
        return true;
    }

} 

