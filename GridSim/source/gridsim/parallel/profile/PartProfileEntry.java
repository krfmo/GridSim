/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

/**
 * The {@link PartProfileEntry} class represents an entry in the availability 
 * profile. It contains the list of ranges of PEs available at a particular
 * time. This time may represent either the start time or completion of
 * a job or advance reservation. It differs from {@link SingleProfileEntry} by
 * having information about multiple resource partitions.
 * 
 * @author  Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see PERange
 * @see PERangeList
 */

public class PartProfileEntry extends ProfileEntry {
	private PERangeList[] rangesParts;
	
	/**
	 * Creates a new instance of {@link PartProfileEntry}
	 * @param time the time associated with this entry
	 * @param numPart the number of partitions
	 */
	public PartProfileEntry(double time, int numPart) {
		super(time);
		rangesParts = new PERangeList[numPart];
	}
	
	/**
	 * Returns the list of ranges available for a given partition at this entry
	 * @param partId the id of the partition.
	 * @return the list of ranges available or <tt>null</tt> if either
	 * the partition does not have ranges set.
	 * @throws IndexOutOfBoundsException if the partition id is 
	 * 			out of the bounds.
	 */
	public PERangeList getAvailRanges(int partId) {
		if (partId < 0 || partId >= rangesParts.length) {
			throw new IndexOutOfBoundsException("Partition " + 
					partId + " does not exist");
		}
		return rangesParts[partId];
	}
	
	/**
	 * Returns the overall list of ranges available at this entry in all 
	 * partitions.   
	 * @return the list of ranges available or <tt>null</tt> if no ranges
	 * have been set previously.
	 */
	public PERangeList getAvailRanges() {
		return new PrivPERangeList();
	}

	/**
	 * Sets the ranges of PEs available at this entry
	 * @param partId the id of the partition.
	 * @param availRanges the list of ranges of PEs available
	 * @throws IndexOutOfBoundsException if the partition id is 
	 * 			out of the bounds.
	 */
	public void setAvailRanges(int partId, PERangeList availRanges) {
		if (partId < 0 || partId >= rangesParts.length) {
			throw new IndexOutOfBoundsException("Partition " + 
					partId + " does not exist");
		}
		rangesParts[partId] = availRanges;
	}
	
	/**
	 * Adds the ranges provided to the list of ranges available
	 * @param partId the id of the partition. 
	 * @param list the list to be added
	 * @throws IndexOutOfBoundsException if the partition id is 
	 * 			out of the bounds.
	 */
	public void addRanges(int partId, PERangeList list) {
		if (partId < 0 || partId >= rangesParts.length) {
			throw new IndexOutOfBoundsException("Partition " + 
					partId + " does not exist");
		}
		if(rangesParts[partId] == null) {
			rangesParts[partId] = new PERangeList();
		}
		rangesParts[partId].addAll(list);
	}

	/**
	 * Gets the number of PEs associated with this entry
	 * @return the number of PEs
	 */
	public int getNumPE() {
		if(rangesParts == null) {
			return 0;
		}
		else {
			int numPE = 0;
			
			for(PERangeList list: rangesParts) {
				if (list != null) {
					numPE += list.getNumPE();
				}
			}
			
			return numPE;
		}
	}
	
	/**
	 * Gets the number of PEs at a partition associated with this entry.
	 * @param partId the id of the partition.
	 * @return the number of PEs
	 */
	public int getNumPE(int partId) {
		if (partId < 0 || partId >= rangesParts.length) {
			throw new IndexOutOfBoundsException("Partition " + 
					partId + " does not exist");
		}
		return rangesParts[partId] == null ? 0 : rangesParts[partId].getNumPE();
	}
		
	/**
	 * Creates a string representation of this entry
	 * @return a representation of this entry
	 */
	public String toString() {
		String result = "ProfileEntry={time="+ getTime() + "; gridlets=" + getNumJobs();
		String rangeStr = "";
		int numPE = 0;
		for(int i=0; i<rangesParts.length; i++) {
			PERangeList rg = rangesParts[i];
			if(rg != null) {
				numPE += rg.getNumPE();
				rangeStr += "; queue " + i + "=" + rg;
			}
		}
		return result + "; numPE=" + numPE + rangeStr + "}";	
	}
	
	/**
	 * Returns a clone of this entry. The ranges are cloned, but the time
	 * and the number of requests relying on this entry are not.
	 * @param time the time for the new entry
	 * @return the new entry with the number of requests set to default.
	 */
	@Override
	public PartProfileEntry clone(double time) {
		PartProfileEntry clone = new PartProfileEntry(time, rangesParts.length);
		for(int i=0; i<rangesParts.length; i++) {
			clone.rangesParts[i] = rangesParts[i] == null ? null : rangesParts[i].clone();
		}
		return clone;
	}
	
	
	/**
	 * Transfers PEs from partitions to one selected partition
	 * @param partId the partition receiving the ranges
	 * @param list the list of ranges
	 */
	public void transferPEs(int partId, PERangeList list) {
		if (partId < 0 || partId >= rangesParts.length) {
			throw new IndexOutOfBoundsException("Partition " + 
					partId + " does not exist");
		}

		for(PERangeList range : rangesParts) {
			if(range != null) {
				range.remove(list);
			}
		}
		if(rangesParts[partId] == null) {
			rangesParts[partId] = new PERangeList();
		}
		rangesParts[partId].addAll(list.clone());
		rangesParts[partId].mergePERanges();
	}
	
	// ------------------ PRIVATE METHODS AND CLASSES ---------------------
	
	/**
	 * This class is a modified version of the PERangeList to consider the
	 * array of lists of ranges maintained by this entry. There should be a 
	 * more elegant way to handle the multiple partitions, but this will suffice
	 * for the moment.
	 *  
	 * @author Marcos Dias de Assuncao
	 * 
	 * @see PERangeList
	 */
	private class PrivPERangeList extends PERangeList {

		private PrivPERangeList() {
			for(PERangeList list : PartProfileEntry.this.rangesParts) {
				super.addAll(list.clone());
			}
		}
		
		/**
		 * The method below makes sure that the ranges deleted, are in
		 * fact deleted from the ranges of all partitions.
		 */
		@Override
		public void remove(PERangeList list) {
			for(PERangeList rgs : PartProfileEntry.this.rangesParts) {
				if(rgs != null) {
					rgs.remove(list);
				}
			}
			
			// updates the general ranges
			super.remove(list);
		}
		
		/**
		 * The user cannot insert ranges in the list. There should be a better
		 * way to insure that this object will not be modified by including
		 * additional ranges.
		 * @return <code>false</code> 
		 */
		@Override
		public boolean add(PERange rg) {
			throw new UnsupportedOperationException();
		}
		
		/**
		 * The user cannot insert ranges in the list. There should be a better
		 * way to insure that this object will not be modified by including
		 * additional ranges.
		 * @return <code>false</code>
		 */
		@Override
		public boolean addAll(PERangeList rl) {
			throw new UnsupportedOperationException();
		}
	}
}
