/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * This class represents the profile containing the ranges of PEs
 * available at given simulation times. 
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */

public abstract class Profile {
	protected LinkedTreeMap<Double,ProfileEntry> avail = 
		new LinkedTreeMap<Double,ProfileEntry>();
	
	/**
	 * Protected constructor.
	 */
	protected Profile() {};

	/**
	 * Protected constructor used by the cloning operations.
	 * @param avail the availability information map.
	 * @see ProfileEntry
	 */
	protected Profile(LinkedTreeMap<Double,ProfileEntry> avail) {
		avail.putAll(avail);
	}
	
	/**
	 * This method returns the current time (or simulation time if the data 
	 * structure is used for simulation purposes. 
	 * @return the current time.
	 */
	protected abstract double currentTime();
	
	/**
	 * Removes past entries from the availability profile, but keeps the
	 * entry corresponding to the reference time provided, or the entry preceding
	 * it if an entry with the provided time does not exist.
	 * <b>NOTE: If <code>refTime</code> is greater than the simulation time, 
	 * then the method will consider the current simulation time as <tt>refTime</tt>. 
	 * @param refTime the reference time for removing the entries. <br>
	 */
	public void removePastEntries(double refTime) {
		refTime = Math.min(refTime, currentTime());
		double timePrec = getPrecedingValue(refTime).getTime();
		Iterator<Double> it = avail.keySet().iterator();
		while(it.hasNext()) {
			double timeEntry = it.next();
			if(timeEntry >= timePrec){
				break;
			}
			it.remove();
		}
	}
	
	/**
	 * Returns a profile entry with the currently available PEs.
	 * @return a {@link ProfileEntry} with the start time equals to the current 
	 * time and the ranges available at the current time.
	 */
	public ProfileEntry checkImmediateAvailability() {
		double currentTime = currentTime();
		ProfileEntry entry = avail.getPrecValue(currentTime, true);
		return entry == null ? 
				new Profile.Entry(currentTime) : 
					entry.clone(currentTime);
	}
	
	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled. 
	 * @param reqPE the number of PEs.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * @return a {@link ProfileEntry} with the start time provided and the 
	 * ranges available at that time OR <tt>null</tt> if not enough PEs are found.
	 */
	public ProfileEntry checkAvailability(int reqPE, double startTime, 
			long duration) {
		
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		if (!it.hasNext()) {
			return null;
		}
		
		PERangeList intersec = it.next().getAvailRanges().clone();
        double finishTime = startTime + duration;
            
        // Scans the availability profile until the expected termination
        // of the job to check whether enough PEs will be available for it. 
        while(it.hasNext()) {
        	ProfileEntry entry = it.next();
        	if(entry.getTime() >= finishTime || intersec.getNumPE() < reqPE) {
        		break;
        	}
        	intersec = intersec.intersection(entry.getAvailRanges());
        }
        return (intersec.getNumPE() >= reqPE) ?
        		new Entry(startTime, intersec) : null;
	}
	
	/**
	 * Selects an entry able to provide enough PEs to handle a job. The method 
	 * iterates the profile until it finds enough PEs for the job, starting 
	 * from the current simulation time.
	 * @param reqPE the number of PEs
	 * @param readyTime entries prior to ready time will not be considered
	 * @param duration the duration in seconds to execute the job
	 * @return an {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public ProfileEntry findStartTime(int reqPE, double readyTime, long duration) {
		readyTime = Math.max(readyTime, currentTime());
		
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(readyTime);
        PERangeList intersect = null;
        double potStartTime = readyTime;
        double potFinishTime = -1;
        ProfileEntry anchor = null;
        
       	// scans the profile until an entry with enough PEs is found
        while(it.hasNext()) {
          	anchor = it.next();
           	if(anchor.getNumPE() < reqPE) { 	
           		continue;
           	}
           	
           	potStartTime = Math.max(readyTime, anchor.getTime());
			potFinishTime = potStartTime + duration;
			intersect = anchor.getAvailRanges();
			Iterator<ProfileEntry> ita = avail.itValuesAfter(potStartTime);
			
			// Now scan the profile from potStartTime onwards analysing the
			// intersection of the ranges available in the entries until the
			// job's expected completion time.
			while (ita.hasNext()) {
				ProfileEntry nextEntry = ita.next();
				if (nextEntry.getTime() >= potFinishTime) {
					break;
				} 
					
				PERangeList nextRanges = nextEntry.getAvailRanges();
				if (nextRanges.getNumPE() < reqPE) {
					intersect = null;
					break;
				}

				intersect = intersect.intersection(nextEntry.getAvailRanges());
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
        
        return new Entry(potStartTime, intersect.clone());
	}
	
	/**
	 * Selects an entry able to provide enough PEs to handle a job. The method 
	 * iterates the profile until it finds enough PEs for the job, starting 
	 * from the current simulation time.
	 * @param reqPE the number of PEs
	 * @param duration the duration in seconds to execute the job
	 * @return an {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public ProfileEntry findStartTime(int reqPE, long duration) {
		return findStartTime(reqPE, currentTime(), duration);		
	}
	
	/**
	 * Allocates a list of PE ranges to a job/reservation.
	 * @param selected the list of PE ranges selected
	 * @param startTime the start time of the job/reservation
	 * @param finishTime the finish time of the job/reservation
	 */
	public void allocatePERanges(PERangeList selected, 
			double startTime, double finishTime) {
		
		// If the time of the last entry is equals to finish time than another
		// entry is not required. In this case we just increase the number of
		// gridlets that rely on that entry to mark either its completion
		// time or start time. The same is valid to the anchor entry, that is
		// the entry that represents the job's start time.

		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		ProfileEntry last = it.next();
		ProfileEntry newAnchor = null;
        
        if(Double.compare(last.getTime(), startTime) == 0) {
        	last.increaseJob();
        }
        else {
        	newAnchor = last.clone(startTime);
        	last = newAnchor;
        }

        ProfileEntry nextEntry = null;
        while(it.hasNext()) {
       		nextEntry = it.next();
       		if(nextEntry.getTime() <= finishTime) {
       			last.getAvailRanges().remove(selected);
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
        	last.getAvailRanges().remove(selected);
        }
        
        if(newAnchor != null) {
        	add(newAnchor);
        }
	}
	
	/**
	 * Returns the time slots contained in this availability profile
	 * within a specified period of time. <br>
	 * <b>NOTE:</b> The time slots returned by this method do not overlap.
	 * That is, they are not the scheduling options of a given job. They are
	 * the windows of availability. Also, they are sorted by start time.
	 * For example: <br>
	 * <pre><br>
	 *   |------------------------------------- 
  	 *   |    Job 3     |     Time Slot 3     | 
  	 * P |------------------------------------- 
	 * E |    Job 2  |      Time Slot 2       | 
	 * s |------------------------------------- 
  	 *   |  Job 1 |  Time Slot 1  |   Job 4   | 
  	 *   +------------------------------------- 
	 *  Start             Time              Finish 
	 *  Time                                 Time 
	 * <br></pre>
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @return a collection with the time slots.
	 * @see TimeSlot
	 */
	public Collection<TimeSlot> getTimeSlots(double startTime, double finishTime) {
		ArrayList<Entry> subProfile = toArrayList(startTime, finishTime);
		return getTimeSlots(finishTime, subProfile);
	}
	
	/**
	 * Returns the scheduling options of this availability profile within the 
	 * specified period of time. 
	 * <b>NOTE:</b> The time slots returned by this method <b>OVERLAP</b> 
	 * because they are the scheduling options for jobs.
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @param duration the minimum duration of the free time slots. Free time 
	 * slots whose time frames are smaller than <code>duration</code> will be ignored.
	 * If you choose <code>1</code>, then all scheduling options will be returned.
	 * @param reqPEs the minimum number of PEs of the free time slots. Free 
	 * time slots whose numbers of PEs are smaller than <code>numPEs</code> will be 
	 * ignored. If you choose <code>1</code>, then all scheduling options will be returned.
	 * 
	 * @return a collection with the time scheduling options.
	 * @see TimeSlot
	 */
	public Collection<TimeSlot> getSchedulingOptions(double startTime, 
			double finishTime, int duration, int reqPEs) {
		ArrayList<TimeSlot> slots = new ArrayList<TimeSlot>();
		
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		ProfileEntry ent = null;
		ProfileEntry nxtEnt = null;
		PERangeList slRgs = null;
		PERangeList its = null;

		while(it.hasNext()) {
			ent = it.next();
			if(ent.getTime() >= finishTime) {
				break;
			}
			else if (ent.getNumPE() == 0) {
				continue;
			}

			slRgs = ent.getAvailRanges();
			double sStart = Math.max(ent.getTime(), startTime);

			while(slRgs != null && slRgs.getNumPE() > 0) {
				int initialPE = slRgs.getNumPE();
				Iterator<ProfileEntry> ita = avail.itValuesAfter(sStart);
				boolean changed = false;
				
				while(ita.hasNext() && !changed) {
					nxtEnt = ita.next();
					
					if(nxtEnt.getTime() >= finishTime) {
						break;
					}
					
					its = slRgs.intersection(nxtEnt.getAvailRanges());
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
	
	// ------------------ PROTECTED METHODS -----------------------
	
	/**
	 * Adds an entry to the availability profile.
	 * @param entry the entry to be removed.
	 * @return the entry replaced by the new entry or <tt>null</tt> if no
	 * entry was replaced.
	 */
	protected ProfileEntry add(ProfileEntry entry) {
		return avail.put(entry.getTime(), entry);
	}
	
	/**
	 * Returns the entry whose time is closest to the <tt>time</tt> given but
	 * smaller, or whose time is equals to <tt>time</tt>
	 * @param time the time to be used to search for the entry
	 * @return the entry whose time is closest to the <tt>time</tt> given but
	 * smaller, or whose time is equals to <tt>time</tt>; <tt>null</tt> if
	 * not found.
	 */
	protected ProfileEntry getPrecedingValue(double time) {
		return avail.getPrecValue(time, true);
	}
	
	/**
	 * A helper method which actually does the real job for 
	 * {@link Profile#getTimeSlots(double, double)}.
	 * @param finishTime the finish time of the period.
	 * @param subProfile the profile already cloned and cut from start time
	 * and finish time.
	 * @return a collection with the time slots. 
	 */
	protected Collection<TimeSlot> getTimeSlots(double finishTime, 
			ArrayList<Entry> subProfile) {
		ArrayList<TimeSlot> slots = new ArrayList<TimeSlot>();
		
		double slStart = 0;		// the start time of the slot
		double slEnd = 0;		// the end time of the slot
		int stIdx = 0;			// index in which a slot starts
		int endIdx = 0;			// index in which a slot finishes

		ProfileEntry ent = null;
		ProfileEntry nxtEnt = null;
        PERangeList slRgs = null;	// ranges of the slot
        PERangeList its = null;		// the intersection of ranges
        int size = subProfile.size();
        
        for(int i=0; i<size; i++) {
        	ent = subProfile.get(i);
        	
        	if(ent.getNumPE() == 0) {
        		continue;
        	}
        	
        	slStart = ent.getTime();
        	stIdx = endIdx = i;
       	
        	// check all possible time slots starting at slStart
        	while(ent.getNumPE() > 0) {
        		slRgs = its = ent.getAvailRanges();
        		slEnd = finishTime;
	        	for(int j=i+1; j<size; j++) {
	        		nxtEnt = subProfile.get(j);
	        		its = its.intersection(nxtEnt.getAvailRanges());
	        		
	        		if(its.getNumPE() == 0) {
	        			slEnd = nxtEnt.getTime();
	        			break;
	        		}
	        		
	        		slRgs = its;
	        		endIdx = j;
	        	}

	        	// clone ranges because they may be pointing to ranges that
	        	// will be deleted next
	        	TimeSlot slot = new TimeSlot(slStart, slEnd, slRgs.clone());
	        	slots.add(slot);
	        	
	        	for(int j=stIdx; j<=endIdx; j++) {
	        		nxtEnt = subProfile.get(j);
	        		nxtEnt.getAvailRanges().remove(slRgs);
	        	}
        	} 
        }
        
		return slots;
	}
	
	// ------------------ PRIVATE METHODS -----------------------
	
	/**
	 * Returns part of the availability profile.<br>
	 * <b>NOTE:</b> The returned entries are clones of the original ones.
	 * @param startTime the start time of the resulting part
	 * @param finishTime the finish time of the resulting part
	 * @return part of the availability profile. 
	 */    
	private ArrayList<Entry> toArrayList(double startTime, double finishTime) {
		ArrayList<Entry> subProfile = new ArrayList<Entry>();
		startTime = Math.max(startTime, currentTime());
		
		Iterator<ProfileEntry> it = avail.itValuesFromPrec(startTime);
		Entry fe = null;
		
		// get first entry or create one if the profile is empty
		if(it.hasNext()) {
			ProfileEntry ent = it.next();
			PERangeList list = ent.getAvailRanges() == null ? 
					null : ent.getAvailRanges().clone();
			double entTime = Math.max(startTime, ent.getTime()); 
			fe = new Entry(entTime, list);
		}
		else {
			fe = new Entry(startTime);
		}
		subProfile.add(fe);
		
		while(it.hasNext()) {
			ProfileEntry entry = it.next();
           	if(entry.getTime() > finishTime) {
           		break;
   			}
           	
			PERangeList list = entry.getAvailRanges() == null ? 
					null : entry.getAvailRanges().clone();
           	
			Entry newEntry = new Entry(entry.getTime(), list);
			subProfile.add(newEntry);
        }

		return subProfile;
    }
	
	/**
	 * This class is used to return an entry when the user calls one of
	 * the methods to query the availability of resources.
	 *  
	 * @author Marcos Dias de Assuncao
	 */
	protected class Entry extends ProfileEntry {
		private PERangeList ranges = null;
		
		/**
		 * Creates an entry with null ranges and the time given
		 * @param time the time for the entry
		 */
		protected Entry(double time) {
			super(time);
			ranges = new PERangeList();
		}

		/**
		 * Creates an entry with the list of ranges and the time given
		 * @param time the time for the entry
		 * @param list the list of ranges
		 */
		protected Entry(double time, PERangeList list) {
			super(time);
			ranges = list;
		}
		
		@Override
		public PERangeList getAvailRanges() {
			return ranges;
		}

		@Override
		public int getNumPE() {
			return ranges.getNumPE();
		}

		@Override
		public ProfileEntry clone(double time) {
			Entry clone = new Entry(time);
			clone.ranges = ranges.clone();
			return clone;
		}
		
		public String toString() {
			return "Profile Entry = {time=" + super.getTime() + 
				", ranges=" + (ranges == null ? "{[]}" : ranges) +  "}";
		}
	}
}