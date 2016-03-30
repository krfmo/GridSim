package datagrid.example03;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_type_p;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.GridletList;
import gridsim.ParameterException;
import gridsim.datagrid.DataGridTags;
import gridsim.datagrid.DataGridUser;
import gridsim.datagrid.DataGridlet;
import gridsim.datagrid.File;
import gridsim.net.SimpleLink;

/**
 * This class implements a user which creates a set of data gridlets, i.e. a set
 * of jobs that require a certain amount of computational power and some data in
 * order to execute. The required data is described as a set of files that have
 * to be available on the site when the gridlet starts to execute. When the
 * gridlets are created, the user submits them to a set of resources and waits
 * until all the gridlets have finished executing.
 *
 * @author Uros Cibej and Anthony Sulistio
 */
public class DataGridletUser extends DataGridUser {


    GridletList outList;        // the list of outgoing gridlets
    GridletList receiveList;    //the list of executed gridlets

    // Constructor
    DataGridletUser(String name, double baud_rate, double delay, int MTU)
            throws Exception {

        super(name, new SimpleLink(name + "_link", baud_rate, delay, MTU));
        this.receiveList = new GridletList();
        this.outList = new GridletList();

        System.out.println("Creating a Gridlet user entity with name = " + name
                + ", and id = " + super.getEntityId(name));
    }

    public void body() {

        experiment(); // execute a set of gridlets

        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();
        System.out.println(this.get_name() + ":%%%% Exiting body() at time "
                + GridSim.clock());
    }

    private void experiment() {
        //wait for all the entities to register
        super.gridSimHold(50.0);
        int location;
        File f1 = null;
        File f2 = null;

        //1. create new files
        try {
            f1 = new File("file1", 5);
            f2 = new File("file2", 1);
        } catch (ParameterException e) {
            System.out.println("Error creating files");
        }

        //2. add files to a resource
        addMaster(f1, GridSim.getEntityId("Res_0"));
        addMaster(f2, GridSim.getEntityId("Res_1"));

        //3. create 3 data gridlets
        for(int i=0; i < 3; i++){
            // set the ID of this gridlet, the gridlet length in MI, the size of
            // the file before and after execution and whether we would like to
            // log the history of this object
            DataGridlet g = new DataGridlet(i, 1000, 10, 10, false);

            //the gridlet requires two files in order to execute
            g.addRequiredFile(f1.getName());
            g.addRequiredFile(f2.getName());

            // the gridlet needs the user id, so that the resource will be able
            // to send it back when finished
            g.setUserID(this.get_id());
            this.outList.add(g);
        }

        //4. submit the data gridlets
        for(int i=0; i<3;i++){
            DataGridlet g = (DataGridlet)outList.get(i);
            System.out.println("Submitting gridlet "+i+" to resource "+i);
            super.send(GridSim.getEntityId("Res_"+i), 0,
                    DataGridTags.DATAGRIDLET_SUBMIT, g);
        }

        //5. wait for the gridlets to finish the execution
        for(int i=0; i<3;i++){

            Sim_type_p tag = new Sim_type_p(GridSimTags.GRIDLET_RETURN);
            Sim_event ev = new Sim_event();

            // only look for this type of ack
            super.sim_get_next(tag, ev);

            DataGridlet dg = (DataGridlet)ev.get_data();
            this.receiveList.add(dg);
            System.out.println("Received back gridlet "+dg.getGridletID());
        }
    }

} // end class
