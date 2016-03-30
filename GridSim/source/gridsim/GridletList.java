/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Gridsim GridletList class is used to maintain a list of Gridlets
 * (in linked-list) and support methods for organizing them
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class GridletList extends LinkedList<Gridlet> {
	private static final long serialVersionUID = -8384130571519277068L;

	/**
     * Sorts the Gridlets in a list based on their lengths
     * @pre $none
     * @post $none
     */
    public void sort() {
        Collections.sort( this, new OrderLength() );
    }
    
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
        Gridlet gl = null;    
        int i = 0;
        
        Iterator<Gridlet> iter = super.iterator();
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
    public Gridlet get(int gridletId, int userId) {
        Gridlet gl = null;
        
        Iterator<Gridlet> iter = super.iterator();
        while (iter.hasNext()){
        	gl = iter.next();

        	if (gl.getGridletID()==gridletId && gl.getUserID()==userId) {
        		return gl;
        	}
        }
        
        return gl;
    }

    ///////////// INTERNAL CLASS //////////////////////////////

    /**
     * A class that compares the order of Gridlet length
     * @invarian $none
     */
    private class OrderLength implements Comparator<Gridlet>
    {

        /**
         * Compares two objects
         * @param a     the first Object to be compared
         * @param b     the second Object to be compared
         * @return the value 0 if both Objects are numerically equal;
         *         a value less than 0 if the first Object is
         *         numerically less than the second Object;
         *         and a value greater than 0 if the first Object is
         *         numerically greater than the second Object.
         * @pre a != null
         * @pre b != null
         * @post $none
         */
        public int compare(Gridlet a, Gridlet b) 
        {
            Double jla = new Double( a.getGridletLength() );
            Double jlb = new Double( b.getGridletLength() );

            return jla.compareTo(jlb);
        }

    } 

} 

