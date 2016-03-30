/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class represents a list of {@link PERange}'s. This is used to represent 
 * the ranges of PEs used by a Gridlet and by the allocation policies
 * to create a profile of ranges available at particular simulation times.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see PERange
 */

public class PERangeList implements Cloneable, Iterable<PERange> {
	private int numPE = 0;
	private ArrayList<PERange> ranges = new ArrayList<PERange>(initialSize);
	private static final int initialSize = 15; // default size of a PERangeList
	private boolean sorted = true;		// true if the range is sorted
	private boolean merged = true; 		// true if the range is merged
	private boolean iterated = false;	// true if the range has been iterated
	
	/**
	 * Default constructor.
	 */
	public PERangeList() {};
	
	/**
	 * Creates a new <tt>PERangeList</tt> object.
	 * @param stPE the initial PE for this range.
	 * @param fnPE the final PE for this range.
	 */
	public PERangeList(int stPE, int fnPE) {
		numPE = fnPE - stPE + 1;
		ranges.add(new PERange(stPE, fnPE));
	}
	
	/**
	 * Returns the number of PEs in this list
	 * @return the number of PEs
	 */
	public int getNumPE() {
		if (iterated) {
			numPE = 0;
			for (PERange range: this.ranges) {
				numPE += range.getNumPE();
			}
			iterated = false;
		}	
		return numPE;
	}
	
	/**
	 * Merges PE ranges: e.g. [3-5],[5-8],[10-20] => [3-8],[10-20].
	 */
	public void mergePERanges() {
		if(!merged && ranges.size() > 1) {
			sortRanges(); 
			int i = 0;
			do {
				PERange cr = ranges.get(i);
				PERange next = ranges.get(i+1);
				if((next.getBegin() - cr.getEnd()) == 1) {			
					cr.setEnd(next.getEnd());
					ranges.remove(i + 1);
					continue;
				}
				i++;
			} while(i<ranges.size()-1);
			merged = true;
		}
	}
	
	/**
	 * Returns a clone of this list of ranges
	 * @return the cloned list
	 */
	public PERangeList clone() {
		PERangeList clone = new PERangeList();
		sortRanges();

		for(PERange range : ranges){
			clone.add(range.clone());
		}
			
		clone.sorted = true;
		clone.merged = this.merged;
		return clone;
	}
	
	/**
	 * Adds a new PE range to the PERangeList
	 * @param range the range to be added to the list
	 * @return <tt>true</tt> if the range has been added; 
	 * <tt>false</tt> otherwise.
	 */
	public boolean add(PERange range) {
		boolean success = ranges.add(range);
		if (success) {
			numPE += range.getNumPE();
		}
		
		sorted = merged = false;
		return success;
	}
	
	/**
	 * Adds a list of PE ranges to the PERangeList
	 * @param l the list of ranges to be added to the list
	 * @return <tt>true</tt> if the ranges have been added; 
	 * <tt>false</tt> otherwise.
	 */
	public boolean addAll(PERangeList l) {
		boolean success = ranges.addAll(l.ranges);
		if (success) {
			numPE += l.numPE;
		}
		sorted = merged = false;
		return success;
	}
	
	/**
	 * Removes all ranges from this list of ranges
	 */
	public void clear() {
		ranges.clear();
		sorted = merged = true;
		numPE = 0;
	}
	
	/**
	 * Sorts the ranges in this list of ranges
	 */
	public void sortRanges() {
		if(!sorted) {
			if(numPE > 0 && ranges.size() > 1) {
				// Avoid Java's sorting, which dumps the list into an array before sorting
				quickSort(ranges, 0, ranges.size() - 1);
			}
			sorted = true;
		}
	}
	
	/**
	 * Returns the number of PE ranges in this list.
	 * @return the number of PE ranges.
	 */
	public int size() {
		return ranges.size();
	}
	
	/**
	 * Returns the smallest PE number in this list.
	 * @return the smallest PE number of <tt>-1</tt> if not found.
	 */
	public int getLowestPE() {
		sortRanges();
		return (ranges.size() > 0) ? ranges.get(0).getBegin() : -1;
	}
	
	/**
	 * Returns the greatest PE number in this list.
	 * @return the greatest PE number of <tt>-1</tt> if not found.	 
	 */
	public int getHighestPE() {
		sortRanges();
		return (ranges.size() > 0) ? ranges.get(ranges.size() - 1).getEnd() : -1;
	}

	/**
     * Creates an String representation of this list
     * @return the string representation
     */
    public String toString() {
    	if(ranges.size() == 0) {
    		return "{[]}";
    	}

   		sortRanges();
    	StringBuilder stringBuilder = new StringBuilder();
    	stringBuilder.append("{");
    	int index = -1;
    	int last = ranges.size() - 1;
    	
    	for(PERange range : ranges) {
    		index++;
    		stringBuilder.append(range);
    		if(index < last) {
    			stringBuilder.append(",");
    		}
    	}
    	
    	stringBuilder.append("}");
    	return stringBuilder.toString();
    }
    
    /**
     * Returns an iterator for this list.
     * @return an iterator for the ranges in this list.
     */
    public Iterator<PERange> iterator() {
    	return new PrivateIterator();
    }
    
    /**
     * Internal Iterator.
     */
    private class PrivateIterator implements Iterator<PERange> {
    	Iterator<PERange> it = null;
    	PERange next = null;
    	PERange lastReturned = null;

    	PrivateIterator() {
   			it = ranges.iterator();
   			if(it.hasNext()) {
   				next = it.next();
   			}
    	}
    	
        public boolean hasNext() {
        	return (next != null);
        }

        public PERange next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
        	iterated = true;
        	sorted = merged = false;
            lastReturned = next;
			next = it.hasNext() ? it.next() : null;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            lastReturned = null;
        	it.remove();
        }
    }

    /**
     * Identifies the intersections between lists of ranges
     * @param listb the second list
     * @return a list containing the intersection between the lists
     */
    public PERangeList intersection(PERangeList listb) {
    	PERangeList rIts = new PERangeList();
    	if(getNumPE() == 0 || listb.getNumPE() == 0) {
    		return rIts;
    	}
    	
   		sortRanges();
   		listb.sortRanges();
    	
    	for(PERange rq : this.ranges) {
    		for(PERange ru : listb.ranges) {
    			// rq's end is already smaller than the start of the following ranges
    			if(rq.getEnd() < ru.getBegin()) {
    				break;
    			}
    			
    			// No intersection has started yet because ru's end is still 
    			// smaller than the start of rq, which means ru is still smaller
    			if(ru.getEnd() < rq.getBegin()) {
    				continue;
    			}
    			
    			rIts.add(rq.intersection(ru));
    		}
    	}

    	return rIts;
    }
    
	/**
	 * Removes the ranges provided from this list.
	 * @param list the ranges to be removed from this list.
	 */
    public void remove(PERangeList list) {
    	mergePERanges();
    	list.mergePERanges();
   	
    	int i = 0;
		PERangeList diffRange = null;
    	while(i < ranges.size()) {
    		PERange rq = ranges.get(i);

    		for(PERange ru : list.ranges) {
	    		// if the end of the range of this queue is smaller
	    		// than the start of the range ru in list, then it means
	    		// that all following ranges in list are beyond rq
	        	if(rq.getEnd() < ru.getBegin()) {
	        		break;
	        	}

	        	// if the end of ru in the list is smaller than the start
	        	// of rq, then it means that ru is below rq, so just continue
	        	if(ru.getEnd() < rq.getBegin()) {
	        		continue;
	        	}
	    				
    			diffRange = rq.difference(ru);
    			if(diffRange == null) {
    				ranges.remove(i);
    				numPE -= rq.getNumPE();
    				i--;
    				break;
    			}
    			else {
    				numPE -= (rq.getNumPE() - diffRange.getNumPE());
    				PERange fr = diffRange.ranges.get(0);
    				rq.setBegin(fr.getBegin());
    				rq.setEnd(fr.getEnd());
    				
    				if(diffRange.size() > 1) {
    					ranges.add(i+1,diffRange.ranges.get(1));
        				i--;
        				break;
    				}
    			}
    		}
    		i++;
    	}
    	
    	sorted = merged = false;
       	mergePERanges();
    }
    
    public boolean equals(PERangeList other) {
    	if(this == other) {
    		return true;
    	} else if(getNumPE() != other.getNumPE()) {
    		return false;
    	}
    	
    	PERangeList list = intersection(other);
    	if(list.getNumPE() == getNumPE() && list.getNumPE() == other.getNumPE()) {
    		return true;
    	}
    	
    	return true;
    }
    
    /**
     * Selects a range to be used by a Gridlet.
     * @param reqPE the number of PEs required.
     * @return the range to be allocated or <tt>null</tt> if no
     * range suitable is found.
     */
    public PERangeList selectPEs(int reqPE){
    	if(getNumPE() < reqPE) {
    		return null;
    	}
    	
   		mergePERanges();
    	PERangeList selected = new PERangeList();
    	
    	for(PERange range : ranges) {
	    	if(range.getNumPE() >= reqPE){
	    		int begin = range.getBegin();
	    		int end = begin + reqPE - 1;
	    		selected.add(new PERange(begin, end));
	    		break;
	    	}
	    	else{
	    		selected.add(range.clone());
	    		reqPE -= range.getNumPE();
	    	}
	    }
	    	
	    selected.sorted = true;
    	return selected;
    }

    /*
     * Quicksorts an array
     */
	private static void quickSort(ArrayList<PERange> array, int start, int end) {
		int i = start; 	// index of left-to-right scan
		int k = end; 	// index of right-to-left scan

		if (end - start >= 1) {
			PERange pivot = array.get(start); 	
			while (k > i) {
				while (array.get(i).getBegin() <= pivot.getBegin()
						&& i <= end && k > i) {
					i++; 
				}
				
				while (array.get(k).getBegin() > pivot.getBegin()
						&& k >= start && k >= i) {
					k--; 
				}
				
				if (k > i) {
					swap(array, i, k); 
				}
			}
			swap(array, start, k); 	
			quickSort(array, start, k - 1); 	// quicksort the left partition
			quickSort(array, k + 1, end); 		// quicksort the right partition
		} 
		// if there is only one element in the partition, do not do any sorting
		else {
			// the array is sorted, so exit
			return; 
		}
	}

	/*
	 * Swap indices in the array
	 */
	private static void swap(ArrayList<PERange> array, int index1, int index2) {
		PERange temp = array.get(index1);    	
		array.set(index1, array.get(index2));  	
		array.set(index2, temp);           		
	}
}