/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

/**
 * The {@link SingleProfileEntry} class represents an entry in the availability 
 * profile. It contains the list of ranges of PEs available at a particular
 * time. This time may represent either the start time or completion of
 * a job or advance reservation.
 * 
 * @author  Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see PERange
 * @see PERangeList
 * @see SingleProfile
 */

public class SingleProfileEntry extends ProfileEntry {
	private PERangeList ranges;
	
	/**
	 * Creates a new instance of {@link SingleProfileEntry}
	 * @param time the time associated with this entry
	 */
	public SingleProfileEntry(double time) {
		super(time);
		ranges = null;
	}
	
	/**
	 * Creates a new instance of {@link SingleProfileEntry}
	 * @param time the time associated with this entry
	 * @param ranges the list of ranges of PEs available
	 */
	public SingleProfileEntry(double time, PERangeList ranges) {
		super(time);
		this.ranges = ranges;
	}

	/**
	 * Returns the list of ranges available at this entry
	 * @return the list of ranges available
	 */
	public PERangeList getAvailRanges() {
		return ranges;
	}

	/**
	 * Sets the ranges of PEs available at this entry
	 * @param availRanges the list of ranges of PEs available
	 */
	public void setAvailRanges(PERangeList availRanges) {
		ranges = availRanges;
	}
	
	/**
	 * Adds the ranges provided to the list of ranges available 
	 * @param list the list to be added
	 * @return <tt>true</tt> if the ranges changed as result of this call
	 */
	public boolean addRanges(PERangeList list) {
		if(ranges == null) {
			ranges = new PERangeList();
		}
		
		return ranges.addAll(list);
	}

	/**
	 * Gets the number of PEs associated with this entry
	 * @return the number of PEs
	 */
	public int getNumPE() {
		if(ranges == null) {
			return 0;
		}
		else {
			return ranges.getNumPE();
		}
	}
		
	/**
	 * Creates a string representation of this entry
	 * @return a representation of this entry
	 */
	public String toString() {
		return "{time="+ super.getTime() +"; numPEs="  
			+ ( (ranges!=null) ? ranges.getNumPE() + "; " + ranges : 
				"0; {[]}") + "}";
	}
	
	/**
	 * Returns a clone of this entry. The ranges are cloned, but the time
	 * and the number of requests relying on this entry are not.
	 * @param time the time for the new entry
	 * @return the new entry with the number of requests set to default.
	 */
	public SingleProfileEntry clone(double time) {
		SingleProfileEntry entry = new SingleProfileEntry(time);
		entry.ranges = ranges == null ? null : ranges.clone();
		return entry;
	}
}
