package gridsim.example02;

/*
 * Author Anthony Sulistio
 * Date: April 2003
 * Description: A simple program to demonstrate of how to use GridSim package.
 *              This example shows how to create one or more Grid users.
 *              A Grid user contains one or more Gridlets.
 *              Therefore, this example also shows how to create Gridlets with 
 *              and without using GridSimRandom class.
 *
 * NOTE: The values used from this example are taken from the GridSim paper.
 *       http://www.gridbus.org/gridsim/
 * $Id: Example2.java,v 1.4 2003/05/19 13:17:49 anthony Exp $
 */

import java.util.*;
import gridsim.*;

/**
 * This class shows how to create one or more grid users. In addition, the
 * creation of Gridlets also discussed.
 */
class Example2
{
    /**
     * Main function to run this example 
     */
    public static void main(String[] args) 
    {
        System.out.println("Starting example of how to create Grid users");
        System.out.println();
        
        try 
        {
            // Creates a list of Gridlets 
            GridletList list = createGridlet();
            System.out.println("Creating " + list.size() + " Gridlets");

            ResourceUserList userList = createGridUser(list);
            System.out.println("Creating " + userList.size() + " Grid users");
            
            // print the Gridlets
            printGridletList(list);
            
            System.out.println("Finish the example");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Unwanted error happens");
        }
    }


    /**
     * A Grid user has many Gridlets or jobs to be processed.
     * This method will show you how to create Gridlets with and without
     * GridSimRandom class.
     * @return a GridletList object 
     */
    private static GridletList createGridlet()
    {
        // Creates a container to store Gridlets
        GridletList list = new GridletList();
        
        // We create three Gridlets or jobs/tasks manually without the help
        // of GridSimRandom
        int id = 0;
        double length = 3500.0;
        long file_size = 300;
        long output_size = 300;
        Gridlet gridlet1 = new Gridlet(id, length, file_size, output_size);
        id++;
        Gridlet gridlet2 = new Gridlet(id, 5000, 500, 500);
        id++;
        Gridlet gridlet3 = new Gridlet(id, 9000, 900, 900);

        // Store the Gridlets into a list
        list.add(gridlet1);
        list.add(gridlet2);
        list.add(gridlet3);

        // We create 5 Gridlets with the help of GridSimRandom and
        // GriSimStandardPE class
        Random random = new Random();   

        // sets the PE MIPS Rating 
        GridSimStandardPE.setRating(100);

        // creates 5 Gridlets
        int count = 5;
        double min_range = 0.10;
        double max_range = 0.50;
        for (int i = 1; i < count+1; i++)
        {
            // the Gridlet length determines from random values and the 
            // current MIPS Rating for a PE
            length = GridSimStandardPE.toMIs(random.nextDouble()*output_size);

            // determines the Gridlet file size that varies within the range 
            // 100 + (10% to 50%)
            file_size = (long) GridSimRandom.real(100, min_range, max_range, 
                                    random.nextDouble());

            // determines the Gridlet output size that varies within the range
            // 250 + (10% to 50%)
            output_size = (long) GridSimRandom.real(250, min_range, max_range,
                                    random.nextDouble());

            // creates a new Gridlet object
            Gridlet gridlet = new Gridlet(id + i, length, file_size,
                                    output_size);

            // add the Gridlet into a list
            list.add(gridlet);
        }

        return list;
    }

    
    /**
     * Creates Grid users. In this example, we create 3 users. Then assign
     * these users to Gridlets.
     * @return a list of Grid users
     */
    private static ResourceUserList createGridUser(GridletList list)
    {
        ResourceUserList userList = new ResourceUserList();
        
        userList.add(0);    // user ID starts from 0
        userList.add(1);
        userList.add(2);

        int userSize = userList.size();
        int gridletSize = list.size();
        int id = 0;
        
        // assign user ID to particular Gridlets
        for (int i = 0; i < gridletSize; i++)
        {
            if (i != 0 && i % userSize == 0)
                id++;

            ( (Gridlet) list.get(i) ).setUserID(id);
        }
        
        return userList;
    }

    
    private static void printGridletList(GridletList list)
    {
        int size = list.size();
        Gridlet gridlet;
        
        String indent = "    ";
        System.out.println();
        System.out.println("Gridlet ID" + indent + "User ID" + indent +
                "length" + indent + " file size" + indent +
                "output size");
        
        for (int i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.println(indent + gridlet.getGridletID() + indent + 
                    indent + indent + gridlet.getUserID() + indent + indent +
                    (int) gridlet.getGridletLength() + indent + indent +
                    (int) gridlet.getGridletFileSize() + indent + indent +
                    (int) gridlet.getGridletOutputSize() );
        }
    }
    
} // end class

