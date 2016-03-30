/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */

package gridsim.net.fnb;

import java.util.*;
import gridsim.GridSim;


/**
 * This class stores a (white) list of entity IDs, such that their messages
 * are guaranteed not to be dropped in the network.
 * @author Agustin Caminero
 * @since GridSim Toolkit 4.2
 */
public class FnbWhiteList extends ArrayList {
	private static FnbWhiteList whiteList = null;
	
	/**
	 * Returns an instance of this white list.
	 * @return an instance of this white list.
	 */
	public static FnbWhiteList getInstance() {
		if(whiteList == null) {
			whiteList = new FnbWhiteList(); 
		}
		
		return whiteList;
	}

    /**
     * Creates a new object of this class.
     */
    private FnbWhiteList() {
        super();
    }
    
    /**
     * Returns <tt>true</tt> if the given id is in the white list.
     * @param   id  an entity id
     * @return <tt>true</tt> if the given id is in the white list,
     *         <tt>false</tt> otherwise.
     */
    public boolean checkList(int id)
    {
        int id_tmp;
        for (int i = 0; i < super.size(); i++)
        {
            id_tmp = ((Integer) super.get(i)).intValue();
            if (id == id_tmp)
            {
                //System.out.println("----- checking : " + 
                //      GridSim.getEntityName(id) + " TRUE");
                return true;
            }
        }

        //System.out.println("----- checking : " + GridSim.getEntityName(id) 
        //      + " FALSE");
        return false;
    }

    /**
     * Adds this entity ID to the white list.
     * Note that this method also adds the IDs of {@link gridsim.net.Input} and 
     * {@link gridsim.net.Output} ports associated with this entity ID.
     * @param   id  an entity ID
     * @return <tt>true</tt> if the entity ID has been added to the white list,
     *         <tt>false</tt> otherwise.
     * @see gridsim.net.Input
     * @see gridsim.net.Output
     */
    public boolean addEntityID(Integer id)
    {
        if ( (id == null) || (id.intValue() <= 0) ) {
            return false;
        }
        
        // check if already exist in the list
        if (checkList(id.intValue()) == true) {
            return false;
        }
        
        String name = GridSim.getEntityName(id);
        String input =  "Input_" + name;
        String output = "Output_" + name;

        int input_id = GridSim.getEntityId(input);
        int output_id = GridSim.getEntityId(output);

        super.add(id);
        super.add(new Integer(input_id));
        super.add(new Integer(output_id));

        return true;
    }

    /**
     * Adds this entity ID to the white list.
     * Note that this method also adds the IDs of {@link gridsim.net.Input} and 
     * {@link gridsim.net.Output} ports associated with this entity ID.
     * @param   id  an entity ID
     * @return <tt>true</tt> if the entity ID has been added to the white list,
     *         <tt>false</tt> otherwise.
     * @see gridsim.net.Input
     * @see gridsim.net.Output
     */
    public boolean addEntityID(int id)
    {
        return addEntityID(new Integer(id));
    }

    /**
     * Removes the given entity id from the white list.
     * @param  id   the entity id to be removed from the white list
     * @return <tt>true</tt> if the entity has been removed successfully, 
     *         <tt>false</tt> otherwise.
     */
    public boolean removeID(int id)
    {
        boolean flag = false;
        int id_tmp;
        for (int i = 0; i < super.size(); i++)
        {
            id_tmp = ((Integer) super.get(i)).intValue();
            if (id == id_tmp) 
            {
                super.remove(i);
                flag = true;
                break;
            }
        }

        return flag;
    }

}
