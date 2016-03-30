/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

/**
 * This class represents a range of PEs. This is used by allocation policies to 
 * keep track of PEs available at a particular time and the PEs allocated to
 * Gridlets. For example, a Gridlet is using a range of PEs (0..4).
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see PERangeList
 */

public class PERange implements Cloneable, Comparable<PERange> {
	private int begin;
	private int end;
	
	/**
	 * Creates a new <tt>PERange</tt> object
	 * @param beginning the start of the range of PEs
	 * @param end the end of the range
	 */
	public PERange(int beginning, int end) {
		this.begin = beginning;
		this.end = end;
	}

	/**
	 * Returns the beginning of the range
	 * @return the number corresponding to the beginning
	 */
	public int getBegin() {
		return begin;
	}

	/**
	 * Returns the end of the range of PEs
	 * @return the end of the range
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * Returns the number of PEs in this range
	 * @return the number of PEs
	 */
	public int getNumPE() {
		return (end - begin) + 1;
	}

	/**
	 * Returns a clone of this range
	 * @return the cloned range
	 */
	public PERange clone() {
		return new PERange(begin, end);
	}
	
	/**
	 * Compares this range against another range of PEs.
	 * @param range the range to compare this range with
	 * @return <code>-1</code> if the  beginning of this range is
	 * smaller than the other range, <code>0</code> if they are
	 * the same and <code>1</code> the beginning of this range is bigger  
	 */
	public int compareTo(PERange range) {
		if(begin < range.begin) {
			return -1;
		}
		else if (begin > range.begin) {
			return 1;
		}
		return 0;
	}
	
	/**
	 * Creates a string representation of this class
	 * @return the string representation
	 */
	public String toString() {
		return "[" + begin + ".." + end + "]";
	}
	
	
	// ----------------- PACKAGE LEVEL METHODS -------------------
	
    /**
     * Returns the common range of this range with another
     * @param rangeb the second range
     * @return the common range of PEs
     */
    PERange intersection(PERange rangeb) {
    	if(rangeb == null) {
    		return null;
    	}
    	
    	int s = (this.begin < rangeb.begin) ? rangeb.begin : this.begin;
    	int e = (this.end < rangeb.getEnd()) ? this.end : rangeb.getEnd();
    	return (s > e) ? null : new PERange(s, e);
    }
    
    /**
     * Returns the list of ranges resulting from subtracting the 
     * given range from this range
     * @param rangeb the range to compare this range against
     * @return the range corresponding to the difference
     */
	PERangeList difference(PERange rangeb) {
		PERangeList difference = new PERangeList();

		int s = (this.begin < rangeb.begin) ? this.begin : rangeb.begin;
		int e = (this.end < rangeb.begin) ? this.end : rangeb.begin - 1;

		if (s <= e) {
			difference.add(new PERange(s, e));
		}

		s = (this.begin <= rangeb.end) ? rangeb.end + 1 : this.begin;
		e = (this.end > rangeb.end) ? this.end : rangeb.end;

		if (s <= e) {
			difference.add(new PERange(s, e));
		}

		return difference.size() == 0 ? null : difference;
	}
	
    /**
     * Checks whether this range intersects with the given range
     * @param rangeb the range to compare this range against
     * @return <tt>true</tt> if the two ranges have an intersection
     * or <tt>false</tt> otherwise.
     */
    boolean intersect(PERange rangeb) {
    	return (intersection(rangeb) == null) ? false : true;
    }
    
	/**
	 * Sets the beginning of the range
	 * @param beginning the beginning
	 */
	void setBegin(int beginning) {
		this.begin = beginning;
	}
	
	/**
	 * Sets the end of the PE range
	 * @param end the end of the range
	 */
	void setEnd(int end) {
		this.end = end;
	}
}
