/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2002, The University of Melbourne, Australia
 */

package gridsim.parallel;

import java.util.ArrayList;

/**
 * SSGridletList maintains a list of SSGridlet's
 *
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public class SSGridletList extends ArrayList<SSGridlet> {
	private static final long serialVersionUID = 359945406929364483L;

	/**
     * Finds the index of a SSGridlet inside the list. This method needs a 
     * combination of gridlet Id and User Id because each Grid User might 
     * have exactly the same gridlet Id.
     * @param gridletId  a gridlet Id
     * @param userId an User Id
     * @return the index in this list of the first occurrence of the 
     * specified Gridlet, or <code>-1</code> if the list does not 
     * contain this Gridlet.
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @post $none
     */
    public int indexOf(int gridletId, int userId) {
        int i = 0;
        for (SSGridlet gl : this) {
        	if (gl.getID() == gridletId && gl.getSenderID() == userId) {
        		return i;
        	}
        	i++;
        }
        
        return -1;
    }

    /**
     * Returns a given SSGridlet. This method needs a combination of gridlet 
     * Id and User Id because each Grid Users might have exactly same gridlet Ids.
     * @param gridletId a gridlet Id
     * @param userId an User Id
     * @return the Gridlet or <code>null</code> if not found.
     * Id and user id is not in the list.
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @post $none
     */
    public SSGridlet get(int gridletId, int userId) {
    	int index = indexOf(gridletId, userId);
    	return (index >= 0) ? super.get(index) : null; 
    }
} 
