/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.Sim_event;

import java.util.Calendar;


/**
 * GridSimShutdown waits for termination of all GridSim user entities to
 * determine the end of simulation.
 * <p>
 * This class will be created by GridSim upon initialization of the simulation,
 * i.e. done via <tt>GridSim.init()</tt> method. Hence, do not need to worry
 * about creating an object of this class.
 * <p>
 * This class signals the user-defined report-writer
 * entity to interact with the GridStatistics entity to generate a report.
 * Finally, it signals the end of simulation to GridInformationService (GIS)
 * entity.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 * @see gridsim.GridSim#init(int, Calendar, boolean)
 * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[], String)
 */
public class GridSimShutdown extends GridSimCore
{
    private int numUser_;
    private String reportWriterName_;


    /**
     * Allocates a new GridSimShutdown object.
     * <p>
     * The total number of
     * grid user entity plays an important role to determine whether all
     * resources should be shut down or not.
     * If one or more users are still not finish, then the resources will not
     * be shut down. Therefore, it is important to give a correct number of
     * total grid user entity. Otherwise, GridSim program will hang or encounter
     * a weird behaviour.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param numUser    total number of grid user entity
     * @param reportWriterName  a <tt>ReportWriter</tt> entity name. This entity
     *                          can be found inside a gridbroker package.
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see eduni.simjava.Sim_entity
     * @pre name != null
     * @pre numUser >= 0
     * @post $none
     */
    public GridSimShutdown(String name, int numUser,
                String reportWriterName) throws Exception
    {
        // NOTE: This entity doesn't use any I/O port.
        //super(name, GridSimTags.DEFAULT_BAUD_RATE);
        super(name);
        this.numUser_ = numUser;
        this.reportWriterName_ = reportWriterName;
    }

    /**
     * Allocates a new GridSimShutdown object.
     * <p>
     * The total number of
     * grid user entity plays an important role to determine whether all
     * resources should be shut down or not.
     * If one or more users are still not finish, then the resources will not
     * be shut down. Therefore, it is important to give a correct number of
     * total grid user entity. Otherwise, GridSim program will hang or encounter
     * a weird behaviour.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param numUser    total number of grid user entity
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see eduni.simjava.Sim_entity
     * @pre name != null
     * @pre numUser >= 0
     * @post $none
     */
    public GridSimShutdown(String name, int numUser) throws Exception {
        this(name, numUser, null);
    }

    /**
     * The main method that shuts down resources and Grid Information
     * Service (GIS). In addition, this method writes down a report at the
     * end of a simulation based on <tt>reportWriterName</tt> defined in
     * the Constructor.
     * <br>
     * <b>NOTE:</b> This method shuts down grid resources and GIS entities
     *              <tt>AFTER</tt> all grid users have been shut down.
     *              Therefore, the number of grid users given in the
     *              Constructor <tt>must</tt> be correct. Otherwise, GridSim
     *              package hangs forever or it does not terminate properly.
     * @pre $none
     * @post $none
     */
    public void body()
    {
        Sim_event ev = new Sim_event();

        // wait for shutdown message from all users.
        // NOTE: this can cause GridSim to be hanged if numUser_ doesn't match
        // with number of user entities given during GridSim.init().
        for (int i = 0; i < numUser_; i++) {
            super.sim_get_next(ev);
        }

        // Shutdown GIS - now GIS is responsible for informing end of simulation
        // This is to simplify design and coding, without introducing any static
        // methods for GridSim and GIS.
        super.send(GridSim.getGridInfoServiceEntityId(),
                   GridSimTags.SCHEDULE_NOW, GridSimTags.END_OF_SIMULATION);

        super.sim_pause(100);

        // Invoke report Writer and shutdown
        if (reportWriterName_ != null)
        {
            int repWriterID = GridSim.getEntityId(reportWriterName_);
            if (repWriterID != -1)
            {
                super.send(repWriterID, GridSimTags.SCHEDULE_NOW,
                        GridSimTags.END_OF_SIMULATION);
            }
            else
            {
                System.out.println(super.get_name() +
                        ": User defined Report Writer entity " +
                        reportWriterName_ + " does not exist.");
            }
        }
        else
        {
            // check whether GridStatistics entity has been created or not
            int id = GridSim.getGridStatisticsEntityId();
            if (id != -1) {
                super.send(id, 0.0, GridSimTags.END_OF_SIMULATION);
            }
        }

        super.terminateIOEntities();
    }

} 
