package datagrid.example01;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import gridsim.*;
import gridsim.datagrid.*;
import gridsim.datagrid.index.DataGIS;
import gridsim.datagrid.index.TopRegionalRC;
import gridsim.datagrid.storage.HarddriveStorage;
import gridsim.datagrid.storage.Storage;
import gridsim.net.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

/**
 * A simple example of usage of the datagrid package. In this example three
 * DataGridResources are created, connected by a network. Each resource has a
 * harddisk and a file stored to it. The file is registered to the centralized
 * replica catalogue. A dummy user is also created and connected to the network.
 *
 * @author Uros Cibej and Anthony Sulistio
 */
public class DataExample1 {

    public static void main(String[] args) {

        DataGIS gis = null;
        TopRegionalRC rc = null;
        DataGridUser user = null;

        //FIRST STEP---------------------------------------------
        //Initialize the GridSim package
        int num_user = 1; // number of grid users
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // means trace GridSim events
        boolean gisFlag = false;    // means using DataGIS instead
        GridSim.init(num_user, calendar, trace_flag, gisFlag);

        // set the GIS into DataGIS that handles specifically for data grid
        // scenarios
        try {
            gis = new DataGIS();
            GridSim.setGIS(gis);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //SECOND STEP---------------------------------------------
        //Create a top level Replica Catalogue
        double baud_rate = 1000000; // bits/sec
        double propDelay = 10; // propagation delay in millisecond
        int mtu = 1500; // max. transmission unit in byte
        try {
            SimpleLink l = new SimpleLink("rc_link", baud_rate, propDelay, mtu);
            rc = new TopRegionalRC(l);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        //THIRD STEP---------------------------------------------
        //Create resources
        int i = 0;
        int totalResource = 3;
        ArrayList resList = new ArrayList(totalResource);
        for (i = 0; i < totalResource; i++) {
            DataGridResource res = createGridResource("Res_" + i, baud_rate,
                    propDelay, mtu);
            resList.add(res);
        }

        //FOURTH STEP---------------------------------------------
        //Create user(s)
        try {
            user = new DummyUser("Dummy_User", baud_rate, propDelay, mtu);
            // set a replica catalogue, if not set the TopReplica RC will be
            // used
            user.setReplicaCatalogue(TopRegionalRC.DEFAULT_NAME);
        } catch (Exception e2) {
            e2.printStackTrace();
        }

        // FIFTH STEP---------------------------------------------
        //Create network
        Router r1 = new RIPRouter("router1"); // router 1
        Router r2 = new RIPRouter("router2"); // router 2

        try {
            //connect the routers
            baud_rate = 10000000;
            Link link = new SimpleLink("r1_r2_link", baud_rate, propDelay, mtu);
            FIFOScheduler r1Sched = new FIFOScheduler();
            FIFOScheduler r2Sched = new FIFOScheduler();

            // attach r2 to r1
            r1.attachRouter(r2, link, r1Sched, r2Sched);
        } catch (Exception e3) {
            e3.printStackTrace();
        }

        //FIFTH STEP---------------------------------------------
        //Connect resources, users and catalogues to the network

        try {
            //connect resources
            GridResource resObj = null;
            for (i = 0; i < resList.size(); i++) {
                FIFOScheduler resSched = new FIFOScheduler();
                resObj = (GridResource) resList.get(i);
                r2.attachHost(resObj, resSched); //attach resource to router r2
            }

            //connect user
            FIFOScheduler userSched = new FIFOScheduler();
            r1.attachHost(user, userSched); //atach the user to router r1

            //connect rc
            FIFOScheduler rcSched = new FIFOScheduler();
            r2.attachHost(rc, rcSched); // attach RC

        } catch (ParameterException e4) {
            e4.printStackTrace();
        }

        //SIXTH STEP---------------------------------------------
        //Finally start the simulation
        GridSim.startGridSimulation();
    }

    private static DataGridResource createGridResource(String name,
            double baud_rate, double delay, int MTU) {

        System.out.println();
        System.out.println("Starting to create one Grid resource with "
                + "3 Machines");

        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();
        //System.out.println("Creates a Machine list");

        // 2. Create a Machine with id, number of PEs and MIPS rating per PE
        mList.add(new Machine(0, 4, 377)); // First Machine

        // 3. Repeat the process from 2 if we want to create more Machines
        //    In this example, the AIST in Japan has 3 Machines with same
        //    MIPS Rating but different PEs.
        // NOTE: if you only want to create one Machine for one Grid resource,
        //       then you could skip this step.
        mList.add(new Machine(1, 4, 377)); // Second Machine
        mList.add(new Machine(2, 2, 377)); // Third Machine

        // 4. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra"; // system architecture
        String os = "Solaris"; // operating system
        double time_zone = 9.0; // time zone this resource located
        double cost = 3.0; // the cost of using this resource

        ResourceCharacteristics resConfig = new ResourceCharacteristics(arch,
                os, mList, ResourceCharacteristics.TIME_SHARED, time_zone, cost);


        // 5. Finally, we need to create a GridResource object.
        long seed = 11L * 13 * 17 * 19 * 23 + 1;
        double peakLoad = 0.0; // the resource load during peak hour
        double offPeakLoad = 0.0; // the resource load during off-peak hr
        double holidayLoad = 0.0; // the resource load during holiday

        // incorporates weekends so the grid resource is on 7 days a week
        LinkedList Weekends = new LinkedList();
        Weekends.add(new Integer(Calendar.SATURDAY));
        Weekends.add(new Integer(Calendar.SUNDAY));

        // incorporates holidays. However, no holidays are set in this example
        LinkedList Holidays = new LinkedList();
        DataGridResource gridRes = null;
        try {
            // create the replica manager
            SimpleReplicaManager rm = new SimpleReplicaManager("RM_" + name,
                    name);

            // create the resource calendar
            ResourceCalendar cal = new ResourceCalendar(time_zone, peakLoad,
                    offPeakLoad, holidayLoad, Weekends, Holidays, seed);

            // create a storage
            Storage storage = new HarddriveStorage("storage", 100000); //100GB

            // create the DataGrid resource
            gridRes = new DataGridResource(name, new SimpleLink(name + "_link",
                    baud_rate, delay, MTU), resConfig, cal, rm);

            // add a storage to the resource
            gridRes.addStorage(storage);

            // tell the resource about a replica catalogue
            gridRes.setReplicaCatalogue(TopRegionalRC.DEFAULT_NAME);

            // in this example, we create 1 master file
            File f = new File("testFile1", 100); //100 MB
            gridRes.addFile(f);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Finally, creates one Grid resource (name: " + name
                + " - id: " + gridRes.get_id() + ")");
        System.out.println();

        return gridRes;
    }

}
