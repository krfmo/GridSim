/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

import java.util.Iterator;
import java.util.NoSuchElementException;

import gridsim.GridSim;

/**
 * This class represents the profile containing the ranges of PEs
 * available at given simulation times. This availability profile does not
 * provide features to manage multiple resource partitions.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see Profile
 * @see SingleProfileEntry
 * @see PERangeList
 * @see TimeSlot
 */

public class SingleProfile extends Profile implements Cloneable, 
				Iterable<SingleProfileEntry> {

	/**
	 * Creates an availability profile for a resource providing the number
	 * of PEs in the resource. This constructor will create an initial 
	 * {@link SingleProfileEntry} with time <tt>0</tt> and an initial PE range 
	 * of [0, numPE-1].
	 * @param numPE the number of PEs in the resource.
	 * @see SingleProfileEntry
	 */
	public SingleProfile(int numPE) {
        PERangeList ranges = new PERangeList();
        ranges.add(new PERange(0, numPE - 1));
        SingleProfileEntry entry = new SingleProfileEntry(0.0D, ranges);
        add(entry);
	}
	
	// ------------------- PROTECTED CONSTRUCTORS -----------------------
	
	/**
	 * Protected constructor used by the cloning operations.
	 * @param avail the availability information map.
	 * @see SingleProfileEntry
	 */
	protected SingleProfile(LinkedTreeMap<Double,ProfileEntry> avail) {
		avail.putAll(avail);
	}
	
	/**
	 * Creates a new Profile object.<br>
	 * <b>NOTE:</b> if you use this constructor, you need to insert an initial
	 * entry with the number of PEs.
	 * @see SingleProfileEntry
	 */
	protected SingleProfile() {};
	
	// --------------------------- PUBLIC METHODS -----------------------
	
	/**
	 * Returns shallow copy of this object.<br>
	 * <b>NOTE:</b> this method does not clone the entries.
	 * @return the cloned object
	 * @see SingleProfile#copy()
	 */
	public SingleProfile clone() {
		return new SingleProfile(avail);
	}
	
	/**
	 * Returns copy of this object.<br>
	 * <b>NOTE:</b> this method clones the entries
	 * @return the copy object
	 */
	public SingleProfile copy() {
		SingleProfile copy = new SingleProfile();
		for(ProfileEntry entry : avail.values()) {
			copy.add(entry.clone(entry.getTime()));
		}
		return copy;
	}
	
	/**
	 * Includes a time slot in this availability profile. This is useful if 
	 * your scheduling strategy cancels a job and you want to update the 
	 * availability profile.
	 * @param startTime the start time of the time slot.
	 * @param finishTime the finish time of the time slot.
	 * @param list the list of ranges of PEs in the slot.
	 * @return <tt>true</tt> if the slot was included; <tt>false</tt> otherwise.
	 */
	public boolean addTimeSlot(double startTime, double finishTime, PERangeList list) {
		startTime = Math.max(startTime, currentTime());
		
		if(finishTime <= startTime) {
			return false;
		}
		
        Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
        ProfileEntry last = it.next();
        ProfileEntry newAnchor = null;

        if (last.getTime() < startTime) {
        	newAnchor = last.clone(startTime);
        	last = newAnchor;
        }

        ProfileEntry nextEntry = null;
        while(it.hasNext()) {
       		nextEntry = it.next();
       		if(nextEntry.getTime() > finishTime) {
       			break;
       		}
       			
       		// Remove duplicate entries. That is, entries whose PE ranges 
       		// are the same. This minimises the number of entries required
       		if(nextEntry.getTime() < finishTime && 
       				last.getAvailRanges().equals(nextEntry.getAvailRanges())) {
       			it.remove();
       		}
       		else {
	       		last.getAvailRanges().addAll(list.clone());
	       		last = nextEntry;
       		}
        }

        if (last.getTime() < finishTime) {
        	add(last.clone(finishTime));
        	last.getAvailRanges().addAll(list.clone());
        }
        
        if(newAnchor != null) {
        	add(newAnchor);
        }
        
        return true;
	}
	
	/**
	 * Returns an iterator in case someone needs to iterate this
	 * object. <br>
	 * <b>NOTE:</b> Removing objects from this profile via its iterator
	 * may make it behave in an unexpected way.
	 *  
	 * @return an iterator for the {@link SingleProfileEntry} objects in this profile.
	 */
	public Iterator<SingleProfileEntry> iterator() {
		//TODO: To prevent user from creating inconsistencies by iterating the profile.
		return new PrivateValueIterator();
	}
	
	// ------------------- PROTECTED METHODS -----------------------
	
	/**
	 * This method returns the current time. 
	 * @return the current time.
	 * 
	 * @see Profile#currentTime()
	 */
	protected double currentTime() {
		return GridSim.clock();
	}
	
	// ------------------- PRIVATE METHODS -----------------------
	
	/**
	 * A delegation based iterator in case someone needs to iterate this
	 * object. <br>
	 * <b>NOTE:</b> Removing objects from this profile via its iterator
	 * may make it behave in an unexpected way.
	 * 
	 * @author Marcos Dias de Assuncao
	 */
	private class PrivateValueIterator implements Iterator<SingleProfileEntry> {
		private Iterator<ProfileEntry> it = null;

        PrivateValueIterator() {
        	it = avail.values().iterator();
	    }

	    public boolean hasNext() {
	    	return it.hasNext();
	    }

	    public SingleProfileEntry next() {
	    	if(!it.hasNext()) {
	    		throw new NoSuchElementException();
	    	}
	    	return (SingleProfileEntry)it.next();
	    }

	    public void remove() {
	    	it.remove();
	    }
	}
}