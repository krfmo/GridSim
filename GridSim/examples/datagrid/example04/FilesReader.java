package datagrid.example04;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

import gridsim.datagrid.File;
import java.io.*;
import java.util.*;


/**
 * Creates a list of master files from a given file
 * @author Uros Cibej
 */
public class FilesReader {

    public static LinkedList read(String filename) {
        LinkedList files = null;

        try {
            FileReader fRead = new FileReader(filename);
            BufferedReader b = new BufferedReader(fRead);
            files = createFiles(b);
        } catch (Exception exp) {
            System.out.println("FilesReader:-File not found");
            System.exit(-1);
        }

        return files;
    }

    private static LinkedList createFiles(BufferedReader buf)
        throws Exception {
        String line;
        String name;
        String size;
        StringTokenizer str;
        File f;
        LinkedList files = new LinkedList();

        while ((line = buf.readLine()) != null) {
            str = new StringTokenizer(line);

            // parse the name and size of file
            name = str.nextToken();

            if (!name.startsWith("#")) {
                size = str.nextToken();

                // size is given in Mb
                f = new File(name, Integer.parseInt(size));
                files.add(f);
            }
        }

        return files;
    }
}
