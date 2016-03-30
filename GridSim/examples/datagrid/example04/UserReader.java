package datagrid.example04;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import gridsim.net.*;
import gridsim.util.NetworkReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Creates a list of users from a given file
 * @author Uros Cibej and Anthony Sulistio
 */
public class UserReader {

    /**
     * @param filename
     *            the name of the file containing the descriptions of Users
     * @param experiments
     *            the list of DataGridlets that can be used in the simulation
     * @return the list of Users
     * @throws Exception
     */
    public static LinkedList read(String filename, LinkedList routers,
        LinkedList resources) throws Exception {
        LinkedList userList = null;

        try {
            FileReader fRead = new FileReader(filename);
            BufferedReader b = new BufferedReader(fRead);
            userList = createUsers(b, routers);
        } catch (Exception exp) {
            System.out.println("User file not found");
        }

        return userList;
    }

    /**
     *
     * @param buf
     * @param experiments
     *            the list of requests executed by the users
     * @return a list of Users initialized with the requests by the users.
     * @throws Exception
     */
    private static LinkedList createUsers(BufferedReader buf, LinkedList routers)
        throws Exception {
        String line;
        String name;
        String baudRate;
        String router_name;
        StringTokenizer str;
        LinkedList users = new LinkedList();

        while ((line = buf.readLine()) != null) {
            str = new StringTokenizer(line);
            name = str.nextToken();

            if (!name.startsWith("#")) {
                router_name = str.nextToken();

                String resource_name = str.nextToken(); //for the RC assignement
                baudRate = str.nextToken(); //baud rate is given in MB/s

                Router r = NetworkReader.getRouter(router_name, routers);

                if (r == null) {
                    System.out.println("Problem with ROUTER " + router_name);
                }

                SimUser dUser = new SimUser(name,
                        Double.parseDouble(baudRate) * 1000000, 10, 1500);
                dUser.setReplicaCatalogue(resource_name);

                r.attachHost(dUser, new FIFOScheduler(name+"_scheduler"));

                int index = 0;
                ArrayList tasks = new ArrayList();

                while (str.hasMoreTokens()) {
                    Object[] tempTask = parseTask(str);

                    if (tempTask != null) {
                        tasks.add(tempTask);
                    }
                }

                dUser.setTasks(tasks);
                users.add(dUser);
            }
        }

        return users;
    }

    private static Object[] parseTask(StringTokenizer str) {
        String taskName = str.nextToken();
        Object[] task = null;

        if (taskName.equals("get")) {
            task = new Object[2];
            task[0] = new Integer(0);
            task[1] = str.nextToken();
        } else if (taskName.equals("replicate")) {
            task = new Object[3];
            task[0] = new Integer(1);
            task[1] = str.nextToken();
            task[2] = str.nextToken();
        } else if (taskName.equals("attribute")) {
            task = new Object[2];
            task[0] = new Integer(3);
            task[1] = str.nextToken();
        } else if (taskName.equals("delete")) {
            task = new Object[3];
            task[0] = new Integer(2);
            task[1] = str.nextToken();
            task[2] = str.nextToken();
        }

        return task;
    }
}
