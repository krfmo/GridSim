package datagrid.example04;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import gridsim.GridSim;
import gridsim.datagrid.DataGridUser;
import gridsim.datagrid.File;
import gridsim.datagrid.FileAttribute;
import gridsim.net.SimpleLink;
import java.util.ArrayList;
import java.util.Iterator;
import gridsim.net.flow.*;  // To use the new flow network package - GridSim 4.2

/**
 * This class defines a user which executes a set of commands.
 * @author Uros Cibej and Anthony Sulistio
 */
class SimUser extends DataGridUser {
    private String name_;
    private ArrayList tasks;

    // constructor
    SimUser(String name, double baud_rate, double delay, int MTU) throws Exception {

        super(name, new SimpleLink(name + "_link", baud_rate, delay, MTU));

        // NOTE: uncomment this if you want to use the new Flow extension
        //super(name, new FlowLink(name + "_link", baud_rate, delay, MTU));

        this.name_ = name;
        this.tasks = new ArrayList();

        // Gets an ID for this entity
        System.out.println("Creating a grid user entity with name = " + name);
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body() {

        //wait for all the entities to register
        super.gridSimHold(100.0);

        //execute all tasks
        Iterator it = tasks.iterator();
        while (it.hasNext()) {
            this.executeTask((Object[]) it.next());
        }

        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();
        System.out.println(this.name_ + ":%%%% Exiting body() at time " +
            GridSim.clock());
    }

    /**
     * Execute a user task, which can be one of the following:<br>
     * Get a file<br>
     * Replicate a file<br>
     * Delete a file<br>
     * Or get the attribute of a file
     * @param task the task to be executed
     */
    private void executeTask(Object[] task) {
        int taskID = ((Integer) task[0]).intValue();
        File f = null;
        int location = -1;

        switch (taskID) {
        case 0: //getFile

            String name = getFullFilename((String) task[1]);
            if (name != null) {
                location = this.getReplicaLocation(name);

                if (location != -1) {
                    f = this.getFile(name, location);
                    System.out.println(this.get_name() +
                        ":- Transfer of file " + name + " succesful");
                }
            } else {
                System.out.println("No such file: " + (String) task[1]);
            }

            break;

        case 1: //replicateFile
            name = getFullFilename((String) task[1]);
            String resourceName = (String) task[2];

            if (name != null) {
                location = this.getReplicaLocation(name);
            } else {
                location = -1;
            }

            if (location != -1) {
                f = this.getFile(name, location);

                if (f != null) {
                    replicateFile(f, GridSim.getEntityId(resourceName));
                }
            }

            break;

        case 2: //deleteReplica
            name = getFullFilename((String) task[1]);
            resourceName = (String) task[2];

            if (name != null) {
                this.deleteFile(name, GridSim.getEntityId(resourceName));
            } else {
                System.out.println("Could not delete " + (String) task[1]);
            }

            break;

        case 3: //getAttribute
            name = getFullFilename((String) task[1]);

            if (name != null) {
                FileAttribute attr = this.getFileAttribute(name);
                System.out.println(this.get_name() +
                    ":- Received attribute for file " + attr.getName());
            } else {
                System.out.println("Could not retrieve attribute for " +
                    (String) task[1]);
            }

            break;

        default:
            System.out.println("Not a valid task for the user");
            break;
        }
    }

    /**
     * Setter method for the array of tasks, which need to be executed.
     *
     * @param l the array of tasks which the user has to execute during
     *          the simulation
     */
    public void setTasks(ArrayList l) {
        tasks = l;
    }

} // end class
