package datagrid.example04;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import gridsim.*;
import gridsim.datagrid.DataGridResource;
import gridsim.datagrid.File;
import gridsim.datagrid.SimpleReplicaManager;
import gridsim.datagrid.index.TopRegionalRC;
import gridsim.datagrid.storage.HarddriveStorage;
import gridsim.datagrid.storage.Storage;
import gridsim.net.*;
import gridsim.util.NetworkReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.StringTokenizer;


/**
 * Creates one or more DataGrid resources with a default set of parameters,
 * such as num of CPUs, CPU rating, etc.
 * @author Uros Cibej and Anthony Sulistio
 */
public class ResourceReader {

    /**
     * Reads a description of resources from a file and creates the
     * DataGridResources.
     *
     * @param filename
     *            the name of the file where the resources are specified
     * @param routers
     *            the list of routers which have already been created
     * @param files
     *            the list of files which have already been created
     * @return the list of resources which have been read from the file
     * @throws Exception
     */
    public static LinkedList read(String filename, LinkedList routers,
        LinkedList files) throws Exception {
        LinkedList resourceList = null;

        try {
            FileReader fRead = new FileReader(filename);
            BufferedReader b = new BufferedReader(fRead);
            resourceList = createResources(b, routers, files);
        } catch (IOException exp) {
            System.out.println("File not found");
        }

        return resourceList;
    }

    /**
     * Create a set of DataGridResources, described in a files.
     *
     * @param buf
     *            buffer with the description of resources
     * @param routerList
     *            the list of available routers
     * @param files
     *            the list of available files
     * @return a list of created DataGridResources
     * @throws Exception
     */
    private static LinkedList createResources(BufferedReader buf,
        LinkedList routerList, LinkedList files) throws Exception {
        String line;
        String routerName;
        String resourceName;
        String regionalRC;
        double storage_size;
        double bandwidth;

        DataGridResource r1;
        DataGridResource r2;
        Router tempRouter;
        Link tempLink;
        LinkedList resourceList = new LinkedList();
        StringTokenizer str;

        while ((line = buf.readLine()) != null) {
            str = new StringTokenizer(line);
            resourceName = str.nextToken();

            if (!(resourceName.startsWith("#"))) {
                storage_size = Double.parseDouble(str.nextToken());
                bandwidth = Double.parseDouble(str.nextToken());
                routerName = str.nextToken(); // read the router name

                if (ParameterReader.useLocalRC) {
                    regionalRC = null;
                } else {
                    regionalRC = str.nextToken();
                }

                r1 = createStandardResource(resourceName, storage_size,
                        bandwidth, regionalRC);

                // attach the resource to a router
                tempRouter = NetworkReader.getRouter(routerName, routerList);

                if (tempRouter != null) {
                     tempRouter.attachHost(r1, new FIFOScheduler(r1.get_name()
                     + "_scheduler"));

                } else {
                    System.out.println(
                        "ERROR - Resource reader- non existing router");
                }

                // read and add the files to the resource
                while (str.hasMoreTokens()) {
                    String filename = str.nextToken();
                    System.out.println("Adding file " + filename);

                    File tempFile = findFile(filename, files);
                    r1.addFile(tempFile);
                }

                // add resource to the list
                resourceList.add(r1);
            }
        }

        return resourceList;
    }

    /**
     * Find a file with filename in a List
     *
     * @param filename
     *            the name of the file
     * @param files
     *            the list of files
     * @return the searched File or null if not found
     */
    private static File findFile(String filename, LinkedList files) {
        File temp;
        int i = 1;
        temp = (File) files.get(0);

        while ((i < files.size()) && (!filename.equals(temp.getName()))) {
            temp = (File) files.get(i);
            i++;
        }

        if (temp.getName().equals(filename)) {
            return temp;
        } else {
            return null;
        }
    }

    /**
     * Creates one Grid resource. A Grid resource contains one or more Machines.
     * Similarly, a Machine contains one or more PEs (Processing Elements or
     * CPUs).
     * <p>
     * In this simple example, we are simulating one Grid resource with three
     * Machines that contains one or more PEs.
     *
     * @param name
     *            a Grid Resource name
     * @return a GridResource object
     */
    private static DataGridResource createStandardResource(String name,
        double storage_size, double bandwidth, String regionalRC) {
        System.out.println();
        System.out.println("Starting to create one Grid resource with " +
            "3 Machines");

        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        // Machines
        MachineList mList = new MachineList();

        // System.out.println("Creates a Machine list");
        // 2. A Machine contains one or more PEs or CPUs. Therefore, should
        // create an object of PEList to store these PEs before creating
        // a Machine.
        PEList peList1 = new PEList();

        // System.out.println("Creates a PE list for the 1st Machine");
        // 3. Create PEs and add these into an object of PEList.
        // In this example, we are using a resource from
        // hpc420.hpcc.jp, AIST, Tokyo, Japan
        // Note: these data are taken the from GridSim paper, page 25.
        // In this example, all PEs has the same MIPS (Millions
        // Instruction Per Second) Rating for a Machine.
        peList1.add(new PE(0, 377)); // need to store PE id and MIPS Rating
        peList1.add(new PE(1, 377));
        peList1.add(new PE(2, 377));
        peList1.add(new PE(3, 377));

        // System.out.println("Creates 4 PEs with same MIPS Rating and put
        // them"+
        // " into the PE list");
        // 4. Create one Machine with its id and list of PEs or CPUs
        mList.add(new Machine(0, peList1)); // First Machine

        // System.out.println("Creates the 1st Machine that has 4 PEs and " +
        // "stores it into the Machine list");
        // System.out.println();
        // 5. Repeat the process from 2 if we want to create more Machines
        // In this example, the AIST in Japan has 3 Machines with same
        // MIPS Rating but different PEs.
        // NOTE: if you only want to create one Machine for one Grid resource,
        // then you could skip this step.
        PEList peList2 = new PEList();

        // System.out.println("Creates a PE list for the 2nd Machine");
        peList2.add(new PE(0, 377));
        peList2.add(new PE(1, 377));
        peList2.add(new PE(2, 377));
        peList2.add(new PE(3, 377));

        // System.out.println("Creates 4 PEs with same MIPS Rating and put
        // them"+
        // " into the PE list");
        mList.add(new Machine(1, peList2)); // Second Machine

        // System.out.println("Creates the 2nd Machine that has 4 PEs and " +
        // "stores it into the Machine list");
        // System.out.println();
        PEList peList3 = new PEList();

        // System.out.println("Creates a PE list for the 3rd Machine");
        peList3.add(new PE(0, 377));
        peList3.add(new PE(1, 377));

        // System.out.println("Creates 2 PEs with same MIPS Rating and put
        // them"+
        // " into the PE list");
        mList.add(new Machine(2, peList3)); // Third Machine

        // System.out.println("Creates the 3rd Machine that has 2 PEs and " +
        // "stores it into the Machine list");
        // System.out.println();
        // 6. Create a ResourceCharacteristics object that stores the
        // properties of a Grid resource: architecture, OS, list of
        // Machines, allocation policy: time- or space-shared, time zone
        // and its price (G$/PE time unit).
        String arch = "Sun Ultra"; // system architecture
        String os = "Solaris"; // operating system
        double time_zone = 9.0; // time zone this resource located
        double cost = 3.0; // the cost of using this resource

        ResourceCharacteristics resConfig = new ResourceCharacteristics(arch,
                os, mList, ResourceCharacteristics.TIME_SHARED, time_zone, cost);

        // System.out.println("Creates the properties of a Grid resource and " +
        // "stores the Machine list");
        // 7. Finally, we need to create a GridResource object.
        long seed = (11L * 13 * 17 * 19 * 23) + 1;
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

            // create a storage, which demands the storage size in MB, but the
            // description we get is in GB. (We need to multiply by 1000 to get
            // MB)
            Storage storage = new HarddriveStorage("storage",
                    storage_size * 1000);

            // create a grid resource, connected to a router. The bandwith is
            // defined as bit/s, but we
            // get the bandwidth ad GB/s (multiply by 10^9)
            gridRes = new DataGridResource(name,
                    new SimpleLink(name + "_link", bandwidth * 1000000000, 10,
                        1500), resConfig, cal, rm);
            gridRes.addStorage(storage);

            // create a local replica catalogue if needed
            // else set the regional RC for this resource
            if (ParameterReader.useLocalRC) {
                gridRes.createLocalRC();
                gridRes.setHigherReplicaCatalogue(TopRegionalRC.DEFAULT_NAME);
            } else {
                gridRes.setReplicaCatalogue(regionalRC);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Finally, creates one Grid resource (name: " + name +
            " - id: " + gridRes.get_id() + ")");
        System.out.println();

        return gridRes;
    }
}
