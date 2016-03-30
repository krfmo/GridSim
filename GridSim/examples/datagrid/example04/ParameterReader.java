package datagrid.example04;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;


/**
 * Reads the parameter file and pass each line to its respective reader.
 * @author Uros Cibej and Anthony Sulistio
 */
public class ParameterReader {

    public static String filesFilename;
    public static String networkFilename;
    public static String resourceFilename;
    public static String usersFilename;
    public static String catalogueFilename;
    public static int numUsers;
    public static String topRCrouter;
    public static boolean useLocalRC = true;

    public static void read(String filename) {
        try {
            FileReader fRead = new FileReader(filename);
            BufferedReader buf = new BufferedReader(fRead);
            String line;
            String name;
            String value;
            StringTokenizer str;

            while ((line = buf.readLine()) != null) {
                if (!line.startsWith("#")) { //ignore comments
                    str = new StringTokenizer(line);

                    //parse the name and size of file
                    name = str.nextToken("=");
                    value = str.nextToken();

                    if (name.equals("files")) {
                        filesFilename = value;
                    } else if (name.equals("network")) {
                        networkFilename = value;
                    } else if (name.equals("resources")) {
                        resourceFilename = value;
                    } else if (name.equals("users")) {
                        usersFilename = value;
                    } else if (name.equals("numUsers")) {
                        numUsers = Integer.parseInt(value);
                    } else if (name.equals("topRCrouter")) {
                        topRCrouter = value;
                    } else if (name.equals("useLocalRC")) {
                        useLocalRC = Boolean.getBoolean(value);
                    } else {
                        System.out.println("Unknown parameter " + name);
                    }
                }
            }
        } catch (Exception exp) {
            System.out.println("File not found");
        }
    }
}
