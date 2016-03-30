/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

import gridsim.GridSim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class represents the profile containing the ranges of PEs
 * available at given simulation times. This class is different from 
 * {@link SingleProfile} by the fact that it controls the availability
 * at multiple resource partitions.
 * 
 * @author Marcos Dias de Assuncao
 * 
 * @since 5.0
 * 
 * @see Profile
 * @see PartProfileEntry
 * @see PERangeList
 * @see TimeSlot
 * @see ResourcePartition
 */

public class PartProfile extends Profile implements Iterable<PartProfileEntry> {
	private ResourcePartition[] partitions;
	
	/**
	 * Creates a new {@link PartProfile} object. This constructor receives a
	 * collection of {@link ResourcePartition} objects which contain the IDs
	 * of the partitions and the initial assignments of processing elements. 
	 * @param parts the collection of resource partitions.
	 * @see ResourcePartition
	 * @see PartitionPredicate
	 */
	public PartProfile(Collection<ResourcePartition> parts) {
		this((ResourcePartition[]) parts.toArray());
	}
	
	/**
	 * Creates a new {@link PartProfile} object. This constructor receives a
	 * collection of {@link ResourcePartition} objects which contain the IDs
	 * of the partitions and the initial assignments of processing elements. 
	 * @param parts the collection of resource partitions.
	 * @see ResourcePartition
	 * @see PartitionPredicate
	 */
	public PartProfile(ResourcePartition[] parts) {
		this(parts.length);
		PartProfileEntry fe = new PartProfileEntry(0L, parts.length);
		int firstPE = 0;
		int lastPE = 0;
		
		for (ResourcePartition part: parts) {
			int partId = part.getPartitionId();
			if(partId >= partitions.length || partId < 0) {
				throw new IndexOutOfBoundsException("It is not possible to " +
						"add a partition with index: " + partId + ".");
			}
			else {
				partitions[partId] = part;
			}
			lastPE = firstPE + part.getInitialNumPEs() - 1;
			PERangeList pesPart = new PERangeList(firstPE, lastPE);
			fe.setAvailRanges(partId, pesPart);
			firstPE = lastPE + 1;
		}
		
		super.add(fe);
	}
	
	/**
	 * Protected constructor used by the cloning operations.
	 * @param avail the availability information map.
	 * @param parts the array containing the resource partition objects.
	 * @see PartProfileEntry
	 */
	private PartProfile(LinkedTreeMap<Double,ProfileEntry> avail,
										ResourcePartition[] parts) {
		avail.putAll(avail);
		partitions = new ResourcePartition[parts.length];
		
		for(int i=0; i<parts.length; i++) {
			partitions[i] = parts[i];
		}
	}
	
	/**
	 * Creates a new {@link PartProfile} object.<br>
	 * <b>NOTE:</b> if you use this constructor, you will have to add an initial
	 * entry to the profile.
	 * @param numParts the number of partitions in this profile.
	 */
	private PartProfile(int numParts) {
		partitions = new ResourcePartition[numParts];
	}
	
	/**
	 * Returns a shallow copy of this object.<br>
	 * <b>NOTE:</b> this method does not clone the entries.
	 * @return the cloned object
	 * @see PartProfile#copy()
	 */
	public PartProfile clone() {
		return new PartProfile(avail, partitions);
	}
	
	/**
	 * Returns a copy of this object.<br>
	 * <b>NOTE:</b> this method clones the entries, but does not clone the
	 * partition and predicates information.
	 * @return the copy object
	 */
	public PartProfile copy() {
		PartProfile copy = new PartProfile(partitions.length);
		copy.partitions = partitions;
		for(ProfileEntry entry: avail.values()) {
			copy.add(entry.clone(entry.getTime()));
		}
		return copy;
	}
	
	/**
	 * Returns the ID of the partition whose predicate matches the schedule
	 * item provided. The method will return <code>-1</code> if no partition can
	 * handle the job/reservation.
	 * @param item the item to be scheduled.
	 * @return the partition ID or <code>-1</code> if no partition 
	 * can handle the job.
	 */
	public int matchPartition(ScheduleItem item) {
		for (ResourcePartition part: partitions) {
			if(part.getPredicate().match(item)) {
				return part.getPartitionId();
			}
		}
		return -1;
	}
	
	/**
	 * Returns a profile entry with the currently available PEs at a given
	 * partition. It does not scan the profile to check if the PEs will be 
	 * available until the completion of a given job. It just returns an 
	 * entry with the PEs available right now.
	 * @param partId the partition from which the ranges will be obtained
	 * 
	 * @return a {@link ProfileEntry} with the start time equals to the current 
	 * time and the ranges available at the current time.
	 */
	public ProfileEntry checkPartImmediateAvailability(int partId) {
		double now = currentTime();
		PartProfileEntry entry = (PartProfileEntry)avail.getPrecValue(now, true);
		if(entry != null) {
			return new Profile.Entry(now, entry.getAvailRanges(partId));
		}
		else {
			return new Profile.Entry(now);
		}
	}
	
	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled. It will return <code>null</code> if it is not 
	 * possible to schedule the job.
	 * @param partId the id of the partition in which the job will be scheduled.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * 
	 * @return a {@link ProfileEntry} with the start time provided and the 
	 * ranges available at that time + the duration.
	 */
	public ProfileEntry checkPartAvailability(int partId,  
			double startTime, long duration) {
		
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("Partition " + partId + 
					" does not exist.");
		}
		
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		if (!it.hasNext()) {
			return new Profile.Entry(startTime);
		}
		
		PartProfileEntry entry = (PartProfileEntry)it.next();
		PERangeList intersec = entry.getAvailRanges(partId).clone(); 
		double finishTime = startTime + duration;
            
        // Scans the availability profile until the expected termination
        // of the job to check whether enough PEs will be available for it. 
        while(it.hasNext()) {
        	entry = (PartProfileEntry)it.next();
        	if(entry.getTime() >= finishTime || intersec.getNumPE() == 0) {
        		break;
        	}
        	intersec = intersec.intersection(entry.getAvailRanges(partId));
        }
        
        return new Profile.Entry(startTime, intersec);
	}
	
	/**
	 * Selects an entry able to provide enough PEs to handle a job. In this 
	 * case, the starting time is not provided, so the method iterates the
	 * profile until it finds enough PEs for the job, starting from the current
	 * simulation time.
	 * @param partId the partition in which the job will be scheduled.
	 * @param reqPE the number of PEs
	 * @param duration the duration in seconds to execute the job
	 * 
	 * @return an {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public ProfileEntry findPartStartTime(int partId, int reqPE, long duration) {
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("Partition " + partId + 
					" does not exist.");
		}
		
		double now = currentTime(); 
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(now);
        PERangeList intersect = null;

        double potStartTime = now;
        double potFinishTime = -1;
        PartProfileEntry anchor = null;
        
        // scans entries until enough PEs are found
        while(it.hasNext()) { 
          	anchor = (PartProfileEntry)it.next();
           	if(anchor.getNumPE(partId) < reqPE) { 	
           		continue;
           	}
           	
           	potStartTime = Math.max(now, anchor.getTime());
			potFinishTime = potStartTime + duration;
			intersect = anchor.getAvailRanges(partId);
			Iterator<ProfileEntry> ita = avail.itValuesAfter(potStartTime);
			
			// Now scan the profile from potStartTime onwards analysing the
			// intersection of the ranges available in the entries until the
			// job's expected completion time.
			while (ita.hasNext()) {
				PartProfileEntry nextEntry = (PartProfileEntry) ita.next();
				if (nextEntry.getTime() >= potFinishTime) {
					break;
				}

				PERangeList nextRanges = nextEntry.getAvailRanges(partId);
				if (nextRanges.getNumPE() < reqPE) {
					intersect = null;
					break;
				}

				intersect = intersect.intersection(nextEntry.getAvailRanges(partId));
				if (intersect.getNumPE() < reqPE) {
					break;
				}
			}

			if (intersect != null && intersect.getNumPE() >= reqPE) {
				break;
			}
        }
        
        if(intersect == null || intersect.getNumPE() < reqPE) {
        	return null;
        }
        
        return new Profile.Entry(potStartTime, intersect.clone());
	}
	
	/**
	 * Allocates a list of PE ranges from a partition to a job/reservation and 
	 * updates the availability profile accordingly. If the time of the last 
	 * entry is equals to finish time than another entry is not required. 
	 * In this case we just increase the number of jobs that rely on 
	 * that entry to mark either its completion time or start time. 
	 * The same is valid to the anchor entry, that is the entry that 
	 * represents the job's start time.
	 * @param partId the partition in which the job will be scheduled.
	 * @param selected the list of PE ranges selected
	 * @param startTime the start time of the job/reservation
	 * @param finishTime the finish time of the job/reservation
	 * 
	 * @see PERangeList
	 */
	public void allocatePartPERanges(int partId, PERangeList selected, 
			double startTime, double finishTime) {
		
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("Partition " + partId + 
					" does not exist.");
		}

		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		PartProfileEntry last = (PartProfileEntry)it.next();
		PartProfileEntry newAnchor = null;
        
        // The following is to avoid having to iterate the 
        // profile more than one time to update the entries. 
        if(Double.compare(last.getTime(),startTime) == 0) {
        	last.increaseJob();
        }
        else {
        	newAnchor = last.clone(startTime);
        	last = newAnchor;
        }

        PartProfileEntry nextEntry = null;
        while(it.hasNext()) {
       		nextEntry = (PartProfileEntry)it.next();
       		if(nextEntry.getTime() <= finishTime) {
       			last.getAvailRanges(partId).remove(selected);
       			last = nextEntry;
       			continue;
       		}
   			break;
        }

        if(Double.compare(last.getTime(),finishTime) == 0) {
        	last.increaseJob();
        }
        else {
        	add(last.clone(finishTime));
        	last.getAvailRanges(partId).remove(selected);
        }
        
        if(newAnchor != null) {
        	add(newAnchor);
        }
	}
	
	/**
	 * Returns the time slots contained in a given partition of this 
	 * availability profile within a specified period of time. <br>
	 * <b>NOTE:</b> The time slots returned by this method do not overlap.
	 * That is, they are not the scheduling options of a given job. They are
	 * the windows of availability. Also, they are sorted by start time.
	 * For example, obtaining the free time slots of partition 0 of the 
	 * scheduling queue below will result in time slots 1, 3 and 3: <br>
	 * <pre><br>
	 *   |
	 *   |                                       
	 *   |------------------                    
	 *   |                 |                    Part. 1
	 *   |      Job 5      |                         
	 * P |                 |                       
	 * E |===================================== 
  	 * s |    Job 3     |     Time Slot 3     | 
  	 *   |------------------------------------- 
	 *   |    Job 2  |      Time Slot 2       | Part. 0
	 *   |------------------------------------- 
  	 *   |  Job 1 |  Time Slot 1  |   Job 4   | 
  	 *   +------------------------------------- 
	 *  Start             Time              Finish 
	 *  Time                                 Time 
	 * <br></pre>
	 * 
	 * @param partId the partition from which the time slots are obtained.
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @return a collection with the time slots contained in the given 
	 * partition of the this availability information object within a 
	 * specified period of time.
	 */
	public Collection<TimeSlot> getPartTimeSlots(int partId, double startTime, 
			double finishTime) {
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("Partition " + partId + 
					" does not exist.");
		}
		ArrayList<Entry> subProfile = toArrayList(partId, startTime, finishTime);
		return super.getTimeSlots(finishTime, subProfile);
	}
	
	/**
	 * Returns the scheduling options for a job in a giving partition of this 
	 * availability profile within the specified period of time. 
	 * <b>NOTE:</b> In contrast to {@link PartProfile#getPartTimeSlots(int, double, double)},
	 * the time slots returned by this method <b>OVERLAP</b> because they are 
	 * the scheduling options for jobs.
	 * 
	 * @param partId the id of the partition from which the scheduling options 
	 * will be obtained.
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @param duration the minimum duration of the free time slots. Free time 
	 * slots whose time frames are smaller than <code>duration</code> will be ignored.
	 * If you choose <code>1</code>, then all scheduling options will be returned.
	 * @param reqPEs the minimum number of PEs of the free time slots. Free 
	 * time slots whose numbers of PEs are smaller than <tt>numPEs</tt> will be 
	 * ignored. If you choose <code>1</code>, then all scheduling options will be returned.
	 * @return a collection with the time scheduling options in this availability 
	 * information object within the specified period of time.
	 */
	public Collection<TimeSlot> getPartSchedulingOptions(int partId, double startTime, 
			double finishTime, int duration, int reqPEs) {
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("Partition " + partId + 
					" does not exist.");
		}
		
		ArrayList<TimeSlot> slots = new ArrayList<TimeSlot>();
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		PartProfileEntry ent = null;
		PartProfileEntry nxtEnt = null;
		PERangeList slRgs = null;
		PERangeList its = null;

		while(it.hasNext()) {
			ent = (PartProfileEntry)it.next();
			if(ent.getTime() >= finishTime) {
				break;
			}
			else if (ent.getNumPE(partId) == 0) {
				continue;
			}

			slRgs = ent.getAvailRanges(partId);
			double sStart = Math.max(ent.getTime(), startTime);

			while(slRgs != null && slRgs.getNumPE() > 0) {
				int initialPE = slRgs.getNumPE();
				Iterator<ProfileEntry> ita = avail.itValuesAfter(sStart);
				boolean changed = false;
				
				while(ita.hasNext() && !changed) {
					nxtEnt = (PartProfileEntry)ita.next();
					
					if(nxtEnt.getTime() >= finishTime) {
						break;
					}
					
					its = slRgs.intersection(nxtEnt.getAvailRanges(partId));
					if(its.getNumPE() == slRgs.getNumPE()) {
						continue;
					}
					 
					// if there was a change in the number of PEs, so that less 
					// PEs are available after the next entry, then considers 
					// the next entry as the end of the current time slot
					double slEnd = Math.min(nxtEnt.getTime(), finishTime);
					if((slEnd - sStart) >= duration && slRgs.getNumPE() >= reqPEs) {
						TimeSlot slot = new TimeSlot(sStart, slEnd, slRgs.clone());
						slots.add(slot);
					}
					changed = true;
					slRgs = its;
				}

				if(slRgs.getNumPE() == initialPE) {
					if((finishTime - sStart) >= duration && slRgs.getNumPE() >= reqPEs) {
						TimeSlot slot = new TimeSlot(sStart, finishTime, slRgs.clone());
						slots.add(slot);
					}
					
					slRgs = null;
				}
			} 
		}
		return slots;
	}
	
	/**
	 * Includes a time slot in this availability profile. This is useful if 
	 * your scheduling strategy cancels a job and you want to update the 
	 * availability profile.
	 * @param partId the partition to which the time slot will be added.
	 * @param startTime the start time of the time slot.
	 * @param finishTime the finish time of the time slot.
	 * @param list the list of ranges of PEs in the slot.
	 * @return <tt>true</tt> if the slot was included; <tt>false</tt> otherwise.
	 */
	public boolean addPartTimeSlot(int partId, double startTime, 
			double finishTime, PERangeList list) {
		double now = currentTime();
		startTime = Math.max(startTime, now);
		if(finishTime <= startTime) {
			return false;
		}
		
		boolean rmRedundant = startTime > now ? true : false;
        Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
        PartProfileEntry last = (PartProfileEntry)it.next();
        PartProfileEntry newAnchor = null;

		// Redundant entries can be removed only if their time is greater than
		// current simulation clock because one entry before or at the
		// simulation clock time is required as the starting point of the profile
        if(Double.compare(last.getTime(),startTime) == 0) {
        	if(last.decreaseJob() <= 0 && rmRedundant) {
        		it.remove();
        	}
        }
        else {
        	newAnchor = last.clone(startTime);
        	last = newAnchor;
        }

        PartProfileEntry nextEntry = null;
        while(it.hasNext()) {
       		nextEntry = (PartProfileEntry)it.next();
       		if(nextEntry.getTime() <= finishTime) {
       			last.getAvailRanges().remove(list);
       			last.getAvailRanges(partId).addAll(list.clone());
       			last = nextEntry;
       			continue;
       		}
   			break;
        }

        if(Double.compare(last.getTime(),finishTime) == 0) {
        	if(last.decreaseJob() <= 0 && rmRedundant) {
        		it.remove();
        	}
        }
        else {
        	add(last.clone(finishTime));
        	last.getAvailRanges().remove(list);
        	last.getAvailRanges(partId).addAll(list.clone());
        }
        
        if(newAnchor != null) {
        	add(newAnchor);
        }
        
        return true;
	}
	
	/**
	 * Creates an string representation of the profile
	 * @return an string representation
	 */
	public String toString() {
		String result = "Profile={\n";
		for(ProfileEntry entry : avail.values()){
			result += entry + "\n";
		}
		result += "}";
		return result;
	}
	
	/**
	 * Returns an iterator in case someone needs to iterate this
	 * object. <br>
	 * <b>NOTE:</b> Removing objects from this profile via its iterator
	 * may make it behave in an unexpected way.
	 *  
	 * @return an iterator for the {@link SingleProfileEntry} objects in this profile.
	 */
	public Iterator<PartProfileEntry> iterator() {
		return new PrivateValueIterator();
	}
	
	/**
	 * This method returns the current time. 
	 * @return the current time.
	 * @see Profile#currentTime()
	 */
	protected double currentTime() {
		return GridSim.clock();
	}
	
	/**
	 * Returns the part of the availability profile's entries in an 
	 * {@link ArrayList}. It returns only the ranges of a given partition.<br>
	 * <b>NOTE:</b> The entries of the sub-profile are clones of the original 
	 * profile object's entries. Therefore, the changes made to the object 
	 * returned by this method will not impact this availability profile.
	 * @param partId the partition of interest
	 * @param startTime the start time of the resulting part
	 * @param finishTime the finish time of the resulting part
	 * 
	 * @return part of the availability profile. 
	 */    
	private ArrayList<Profile.Entry> toArrayList(int partId,
			double startTime, double finishTime) {
		
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("It is not possible to " +
					"add a partition with index: " + partId + ".");
		}
		
		ArrayList<Entry> subProfile = new ArrayList<Entry>();
		startTime = Math.max(startTime, currentTime());
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		Profile.Entry fe = null;

		// get first entry or create one if the profile is empty
		if(it.hasNext()) {
			PartProfileEntry ent = (PartProfileEntry)it.next();
			PERangeList list = ent.getAvailRanges(partId) == null ? 
					null : ent.getAvailRanges(partId).clone();
			double entTime = Math.max(startTime, ent.getTime());
			fe = new Profile.Entry(entTime, list);
		}
		else {
			fe = new Profile.Entry(startTime);
		}
		subProfile.add(fe);
		
		while(it.hasNext()) {
			PartProfileEntry entry = (PartProfileEntry)it.next();
           	if(entry.getTime() > finishTime) {
           		break;
   			}
           	
			PERangeList list = entry.getAvailRanges(partId) == null ? 
					null : entry.getAvailRanges(partId).clone();
           	
			Profile.Entry newEntry = new Profile.Entry(entry.getTime(), list);
			subProfile.add(newEntry);
        }

		return subProfile;
    }
	
	/**
	 * A delegation based iterator in case someone needs to iterate this
	 * object. <br>
	 * <b>NOTE:</b> Removing objects from this profile via its iterator
	 * may make it behave in an unexpected way.
	 * 
	 * @author Marcos Dias de Assuncao
	 */
	private class PrivateValueIterator implements Iterator<PartProfileEntry> {
		private Iterator<ProfileEntry> it = null;

        PrivateValueIterator() {
        	it = avail.values().iterator();
	    }

	    public boolean hasNext() {
	    	return it.hasNext();
	    }

	    public PartProfileEntry next() {
	    	if(!it.hasNext()) {
	    		throw new NoSuchElementException("Element does not exist");
	    	}
	    	return (PartProfileEntry)it.next();
	    }

	    public void remove() {
	    	it.remove();
	    }
	}
}