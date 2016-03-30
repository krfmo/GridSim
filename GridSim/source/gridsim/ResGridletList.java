/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * GridSim ResGridletList maintains a linked-list of Gridlet
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class ResGridletList extends LinkedList<ResGridlet>
{

    /**
     * Finds the index of a Gridlet inside the list. This method needs a 
     * combination of Gridlet Id and User Id because each Grid User might 
     * have exactly the same Gridlet Id.
     * @param gridletId  a Gridlet Id
     * @param userId an User Id
     * @return the index in this list of the first occurrence of the 
     * specified Gridlet, or <code>-1</code> if the list does not 
     * contain this Gridlet.
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @post $none
     */
    public int indexOf(int gridletId, int userId) {
        ResGridlet gl = null;    
        int i = 0;
        
        Iterator<ResGridlet> iter = super.iterator();
        while (iter.hasNext()){
        	gl = iter.next();

        	if (gl.getGridletID()==gridletId && gl.getUserID()==userId) {
        		return i;
        	}

        	i++;
        }
        
        return -1;
    }

    /**
     * Returns a given Gridlet. This method needs a combination of Gridlet 
     * Id and User Id because each Grid Users might have exactly same Gridlet Ids.
     * @param gridletId  a Gridlet Id
     * @param userId an User Id
     * @return the Gridlet.
     * @throws IndexOutOfBoundsException - if a Gridlet with specified 
     * Id and user id is not in the list.
     * @pre gridletId >= 0
     * @pre userId >= 0
     * @post $none
     */
    public ResGridlet get(int gridletId, int userId) {
        ResGridlet gl = null;
        
        Iterator<ResGridlet> iter = super.iterator();
        while (iter.hasNext()){
        	gl = iter.next();

        	if (gl.getGridletID()==gridletId && gl.getUserID()==userId) {
        		return gl;
        	}
        }
        
        return gl;
    }
	
    /**
     * Move a ResGridlet object from this linked-list into a specified one
     * @param obj a ResGridlet object to be moved
     * @param list a ResGridletList object to store the new ResGridlet object
     * @return <b>true</b> if the moving operation successful,
     *         otherwise return <b>false</b>
     * @pre obj != null
     * @pre list != null
     * @post $result == true || $result == false
     */
    public boolean move(ResGridlet obj, ResGridletList list)
    {
        boolean success = false;
        if (super.remove(obj))
        {
            list.add(obj);
            success = true;
        }

        return success;
    }

} 
