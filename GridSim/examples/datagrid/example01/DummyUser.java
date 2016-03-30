package datagrid.example01;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import gridsim.GridSim;
import gridsim.datagrid.DataGridUser;
import gridsim.net.SimpleLink;

/**
 * In this example, a user does not perform any activities.
 * This example only shows how to create a DataGrid resource.
 * @author Uros Cibej and Anthony Sulistio
 */
public class DummyUser extends DataGridUser {

    // constructor
    DummyUser(String name, double baud_rate, double delay, int MTU)
        throws Exception {
        super(name, new SimpleLink(name + "_link", baud_rate, delay, MTU));
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body() {

        // A real user would at this point execute some activities, such as
        // make a query about file locations, request a file, replicate a file.
        System.out.println(super.get_name() + " idling ....");

        // At the end, it is necessary to shutdown all the entities
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();
        System.out.println(super.get_name() + ": %%%% Exiting body() at time " +
            GridSim.clock());
    }
}
