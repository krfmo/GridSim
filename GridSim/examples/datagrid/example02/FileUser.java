package datagrid.example02;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import gridsim.GridSim;
import gridsim.IO_data;
import gridsim.ParameterException;
import gridsim.datagrid.DataGridUser;
import gridsim.datagrid.File;
import gridsim.net.SimpleLink;

import java.util.ArrayList;

/**
 * This is an implementation of a user which performs a set of file managing
 * operations on the Data Grid.
 *
 * @author Uros Cibej and Anthony Sulistio
 */
class FileUser extends DataGridUser {

    private String name_;   // user name

    // Constructor
    FileUser(String name, double baud_rate, double delay, int MTU)
            throws Exception {

        super(name, new SimpleLink(name + "_link", baud_rate, delay, MTU));
        this.name_ = name;

        // Gets an ID for this entity
        int userID = super.getEntityId(name);
        System.out.println("Creating a grid user entity with name = " + name
                + ", and id = " + userID);
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body() {

        experiment(); // execute a set of demands

        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();
        System.out.println(this.name_ + ":%%%% Exiting body() at time "
                + GridSim.clock());
    }

    private void experiment() {

        // wait for all entities to register
        super.gridSimHold(50.0);
        int location = 0;

        // create three new files and add them as master files to resource Res_2
        try {
            ArrayList fList = new ArrayList();
            for (int i = 0; i < 3; i++) {
                File fNew = new File("file_" + i, 10);
                addMaster(fNew, GridSim.getEntityId("Res_2"));
            }
        } catch (ParameterException e) {
            e.printStackTrace();
        }

        // get the full name of file1, i.e. file1+ID, where ID is a unique
        // number assigned to the file by the top replica catalogue.
        String name = getFullFilename("file_0");
        File f = null;
        int fileLocation;
        if (name != null) {
            // get the location of the file
            location = this.getReplicaLocation(name);
            if (location != -1) {
                // transfer the file
                f = this.getFile(name, location);
                System.out.println("user:" + this.get_name()
                        + ":-Transfer of file " + name + " succesful");
            }
        }

        // replicate this file to Res_0
        replicateFile(f, GridSim.getEntityId("Res_0"));

        // replicate this file to Res_1
        replicateFile(f, GridSim.getEntityId("Res_1"));

        // delete replica of file from Res_0
        deleteFile(f.getName(), GridSim.getEntityId("Res_0"));

        // trying to delete a master file will not succeed
        // for deleting the master file one should use deleteMaster()
        deleteFile(f.getName(), GridSim.getEntityId("Res_2"));

        // but a master file cannot be deleted while there are replicas of the
        // file on the Data Grid, so the following will also produce an error
        deleteMaster(f.getName(), GridSim.getEntityId("Res_2"));

        // so we first have to delete the replica of file from Res_1
        deleteFile(f.getName(), GridSim.getEntityId("Res_1"));

        // and then we can delete the master file
        deleteMaster(f.getName(), GridSim.getEntityId("Res_2"));
    }

} // end class

