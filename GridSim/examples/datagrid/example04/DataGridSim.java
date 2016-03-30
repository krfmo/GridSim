package datagrid.example04;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import eduni.simjava.Sim_system;
import gridsim.*;
import gridsim.datagrid.index.*;
import gridsim.net.FIFOScheduler;
import gridsim.net.Link;
import gridsim.net.Router;
import gridsim.net.SimpleLink;
import gridsim.util.NetworkReader;
import java.util.Calendar;
import java.util.LinkedList;
import gridsim.net.flow.*;  // To use the new flow network package - GridSim 4.2

/**
 * This is the main class of the simulation package. It reads all the parameters
 * from a file, constructs the simulation defined in the configuration files,
 * and runs the simulation.
 * @author Uros Cibej and Anthony Sulistio
 */
public class DataGridSim {

    public static void main(String[] args) {
        System.out.println("Starting data grid simulation ...");

        try {
            if (args.length != 1) {
                System.out.println("Usage: java Main parameter_file");
                return;
            }

            //read parameters
            ParameterReader.read(args[0]);

            int num_user = ParameterReader.numUsers; // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // means trace GridSim events
            boolean gisFlag = false; // means using DataGIS instead

            // Initialize the GridSim package
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag, gisFlag);

            // NOTE: uncomment this if you want to use the new Flow extension
            //GridSim.initNetworkType(GridSimTags.NET_FLOW_LEVEL);

            // set the GIS into DataGIS that handles specifically for data grid
            // scenarios
            DataGIS gis = new DataGIS();
            GridSim.setGIS(gis);

            //some default values
            double baud_rate = 100000000; // 100MB/sec
            double propDelay = 10; // propagation delay in millisecond
            int mtu = 1500; // max. transmission unit in bytes

            //read available files
            LinkedList files = FilesReader.read(ParameterReader.filesFilename);

            //-------------------------------------------
            //read topology
            LinkedList routerList = NetworkReader.createFIFO(ParameterReader.networkFilename);

            //attach central RC entity to one of the routers
            //Create a central RC
            Link l = new SimpleLink("rc_link", baud_rate, propDelay, mtu);

            // NOTE: uncomment this if you want to use the new Flow extension
            //LinkedList routerList = NetworkReader.createFlow(ParameterReader.networkFilename);
            //Link l = new FlowLink("rc_link", baud_rate, propDelay, mtu);
            //-------------------------------------------

            TopRegionalRC rc = new TopRegionalRC(l);

            //connect the TopRC to a router specified in the parameters file
            Router r1 = NetworkReader.getRouter(ParameterReader.topRCrouter,
                    routerList);
            FIFOScheduler gisSched = new FIFOScheduler();
            r1.attachHost(rc, gisSched); // attach RC

            //create resources
            LinkedList resList = ResourceReader.read(ParameterReader.resourceFilename,
                    routerList, files);

            //create users
            LinkedList users = UserReader.read(ParameterReader.usersFilename,
                    routerList, resList);

            GridSim.startGridSimulation();
            System.out.println("\nFinish data grid simulation ...");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }

}
