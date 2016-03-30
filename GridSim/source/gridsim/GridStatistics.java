/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import eduni.simjava.Sim_entity;
import eduni.simjava.Sim_event;


/**
 * Records statistical data reported by other entities.
 * <p>
 * It stores data objects
 * along with its arrival time and the ID of the machine and the PE
 * (Processing Element) allocated to it. It acts as a placeholder for
 * maintaining the amount of resource share allocated at various times for
 * simulating time-shared scheduling using internal events.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class GridStatistics extends Sim_entity
{
    private boolean active_;
    private PrintWriter outFile_;
    private LinkedList statList_;
    private LinkedList statSortByCategoryData_;
    private String[] excludeFromFile_;
    private String[] excludeFromProcessing_;


    /**
     * Allocates a new GridStatistics object
     * @param name the entity name
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre name != null
     * @post $none
     */
    public GridStatistics(String name) throws Exception
    {
        super(name);
        active_ = false;
        outFile_ = null;
        statList_ = null;
        statSortByCategoryData_ = null;
        excludeFromFile_ = null;
        excludeFromProcessing_ = null;
    }

    /**
     * Allocates a new GridStatistics object with a set of parameters
     * @param name      the entity name
     * @param fileName  the file name to be written into
     * @param append    if it is true, then bytes will be written to the end
     *                  of the file rather than the beginning
     * @param excludeFromFile     List of names to be excluded from
     *                            statistics
     * @param excludeFromProcessing   List of names to be excluded from
     *                                writing into a file
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre name != null
     * @pre fileName != null
     * @post $none
     */
    public GridStatistics(String name, String fileName, boolean append,
            String[] excludeFromFile, String[] excludeFromProcessing)
                    throws Exception
    {
        super(name);
        active_ = true;

        try
        {
            FileWriter fw = new FileWriter(fileName, append);
            BufferedWriter bw = new BufferedWriter(fw);
            outFile_ = new PrintWriter(bw);
            statList_ = new LinkedList();
            excludeFromFile_ = excludeFromFile;
            excludeFromProcessing_ = excludeFromProcessing;
            statSortByCategoryData_ = null;
        }
        catch (IOException e)
        {
            System.out.println("GridStatistics() : Error - " +
                    "unable to open " + fileName);
        }
    }

    ////////////////////////// INTERNAL CLASS /////////////////////////

    /**
     * This class can be used to sort a LinkedList of Stat in ascending order
     * by the field categoryNameData
     * @invariant $none
     */
    private class OrderByCategoryNameData implements Comparator
    {

        /**
         * Compares two objects
         * @param a     the first Object to be compared
         * @param b     the second Object to be compared
         * @return the value 0 if both Objects are equal;
         *         a value less than 0 if the first Object is lexicographically
         *         less than the second Object;
         *         and a value greater than 0 if the first Object is
         *         lexicographically greater than the second Object.
         * @throws ClassCastException   <tt>a</tt> and <tt>b</tt> are expected
         *              to be of type <tt>Stat</tt>
         * @see gridsim.Stat
         * @pre a != null
         * @pre b != null
         * @post $none
         */
        public int compare(Object a, Object b) throws ClassCastException
        {
            String c1 = ((Stat) a).getCategory();
            String c2 = ((Stat) b).getCategory();

            // if both objects have the same category
            if ( c1.equals(c2) )
            {
                String n1 = ((Stat) a).getName();
                String n2 = ((Stat) b).getName();

                // if both objects have the same name
                if ( n1.equals(n2) )
                {

                    String d1 = ((Stat) a).getData();
                    String d2 = ((Stat) b).getData();

                    return d1.compareTo(d2);
                }
                else {
                    return n1.compareTo(n2);
                }
            }
            else {
                return c1.compareTo(c2);
            }
        }

    } // end internal class


    /**
     * This class can be used to sort a LinkedList of Stat in ascending order
     * by the field categoryFieldData
     * @invariant $none
     */
    private class OrderByCategoryData implements Comparator
    {
    	
        /**
         * Compares two objects
         * @param a     the first Object to be compared
         * @param b     the second Object to be compared
         * @return the value 0 if both Objects are equal;
         *         a value less than 0 if the first Object is lexicographically
         *         less than the second Object;
         *         and a value greater than 0 if the first Object is
         *         lexicographically greater than the second Object.
         * @throws ClassCastException   <tt>a</tt> and <tt>b</tt> are expected
         *              to be of type <tt>Stat</tt>
         * @see gridsim.Stat
         * @pre a != null
         * @pre b != null
         * @post $none
         */
        public int compare(Object a, Object b) throws ClassCastException
        {
            String c1 = ((Stat) a).getCategory();
            String c2 = ((Stat) b).getCategory();

            if ( c1.equals(c2) )
            {
                String d1 = ((Stat) a).getData();
                String d2 = ((Stat) b).getData();

                return d1.compareTo(d2);
            }
            else {
                return c1.compareTo(c2);
            }
        }

    } // end internal class


    /**
     * This class can be used to sort a LinkedList of Stat in ascending order
     * by the field categoryNameField
     * @invariant $none
     */
    private class OrderByCategoryName implements Comparator
    {

        /**
         * Compares two objects
         * @param a     the first Object to be compared
         * @param b     the second Object to be compared
         * @return the value 0 if both Objects are equal;
         *         a value less than 0 if the first Object is lexicographically
         *         less than the second Object;
         *         and a value greater than 0 if the first Object is
         *         lexicographically greater than the second Object.
         * @throws ClassCastException   <tt>a</tt> and <tt>b</tt> are expected
         *              to be of type <tt>Stat</tt>
         * @see gridsim.Stat
         * @pre a != null
         * @pre b != null
         * @post $none
         */
        public int compare(Object a, Object b) throws ClassCastException
        {
            String c1 = ((Stat) a).getCategory();
            String c2 = ((Stat) b).getCategory();

            if ( c1.equals(c2) )
            {

                String n1 = ((Stat) a).getName();
                String n2 = ((Stat) b).getName();

                return n1.compareTo(n2);
            }
            else {
                return c1.compareTo(c2);
            }
        }

    } // end internal class


    /**
     * This class can be used to sort a LinkedList of Stat in ascending order
     * by the field categoryField
     * @invariant $none
     */
    private class OrderByCategory implements Comparator
    {

        /**
         * Compares two objects
         * @param a     the first Object to be compared
         * @param b     the second Object to be compared
         * @return the value 0 if both Objects are equal;
         *         a value less than 0 if the first Object is lexicographically
         *         less than the second Object;
         *         and a value greater than 0 if the first Object is
         *         lexicographically greater than the second Object.
         * @throws ClassCastException   <tt>a</tt> and <tt>b</tt> are expected
         *              to be of type <tt>Stat</tt>
         * @see gridsim.Stat
         * @pre a != null
         * @pre b != null
         * @post $none
         */
        public int compare(Object a, Object b) throws ClassCastException
        {
            String c1 = ((Stat) a).getCategory();
            String c2 = ((Stat) b).getCategory();

            return c1.compareTo(c2);
        }

    } // end internal class


    /**
     * This class is used to store a number associated with its index
     * @invariant $none
     */
    private class Times
    {
        private int index_;
        private int val_;

        /**
         * Allocates a new Times object
         * @param index  a reference number
         * @param val    an integer value
         * @pre $none
         * @post $none
         */
        public Times(int index, int val)
        {
            this.index_ = index;
            this.val_ = val;
        }

        /**
         * Gets the index number
         * @return index number
         * @pre $none
         * @post $none
         */
        public int getIndex() {
            return index_;
        }

        /**
         * Gets the value
         * @return integer value
         * @pre $none
         * @post $none
         */
        public int getValue() {
            return val_;
        }

    } // end internal class


    /**
     * This class sorts the order of Times object by its index value
     * @see gridsim.GridStatistics.Times
     * @invariant $none
     */
    private class OrderByIndex implements Comparator
    {
        /**
         * Allocates a new OrderByIndex value
         * @pre $none
         * @post $none
         */
        public OrderByIndex() {
            super();
        }

        /**
         * Compares two objects based on their indices
         * @param a     the first Object to be compared
         * @param b     the second Object to be compared
         * @return the value 0 if both Objects are equal;
         *         a value less than 0 if the first Object is lexicographically
         *         less than the second Object;
         *         and a value greater than 0 if the first Object is
         *         lexicographically greater than the second Object.
         * @throws ClassCastException   <tt>a</tt> and <tt>b</tt> are expected
         *              to be of type <tt>Stat</tt>
         * @see gridsim.Stat
         * @pre a != null
         * @pre b != null
         * @post $none
         */
        public int compare(Object a, Object b) throws ClassCastException
        {
            Integer i1 = new Integer( ((Times) a).getIndex() );
            Integer i2 = new Integer( ((Times) b).getIndex() );

            return i1.compareTo(i2);
        }

    } // end internal class

    ////////////////////////// INTERNAL CLASS /////////////////////////


    /**
     * Accumulates objects based on a given category
     * @param category  user-defined name for data type
     * @return an Accumulator object contains double values of objects
     *         that match the given category
     * @see gridsim.Accumulator
     * @pre category != null
     * @post $result != null
     */
    public Accumulator accumulate(String category)
    {
        LinkedList statList = statSortByCategoryData_;
        Accumulator acc = new Accumulator();

        int pos = Collections.binarySearch( statList,
                        new Stat(0.0, category, "", ""),
                        new OrderByCategory() );

        if (pos < 0) {
            return acc;
        }

        Stat statObj = (Stat) statList.get(pos);
        acc.add( new Double(statObj.getData()).doubleValue() );

        int i = 0;
        for (i = pos+1; i < statList.size(); i++)
        {
            statObj = (Stat) statList.get(i);
            if ( statObj.getCategory().equals(category) ) {
                acc.add( new Double(statObj.getData()).doubleValue() );
            }
            else {
                break;
            }
        }

        for (i = pos-1; i >= 0; i--)
        {
            statObj = (Stat) statList.get(i);
            if ( statObj.getCategory().equals(category) ) {
                acc.add( new Double(statObj.getData()).doubleValue() );
            }
            else {
                break;
            }
        }

        return acc;
    }

    /**
     * Accumulates objects based on a given category
     * @param category  user-defined name for data type
     * @param counter   user-defined name for data type
     * @return an Accumulator object contains double values of objects
     *         that match the given category
     * @see gridsim.Accumulator
     * @pre category != null
     * @pre counter != null
     * @post $result != null
     */
    public Accumulator accumulate(String category, String counter)
    {
        LinkedList statList = statSortByCategoryData_;
        Accumulator acc = new Accumulator();
        LinkedList timeList = new LinkedList();

        int pos = Collections.binarySearch( statList,
                        new Stat(0.0, counter, "", ""),
                        new OrderByCategory() );

        if (pos < 0) {
            return acc;
        }

        Stat statObj = (Stat) statList.get(pos);
        int j = Integer.parseInt(
                    statObj.getName().substring(
                        statObj.getName().indexOf('_')+1 )
                );

        timeList.add(new Times(j, Integer.parseInt(statObj.getData())) );

        int i = 0;
        for (i = pos+1; i < statList.size(); i++)
        {
            statObj = (Stat) statList.get(i);
            if ( statObj.getCategory().equals(counter) )
            {
                j = Integer.parseInt(
                            statObj.getName().substring(
                                statObj.getName().indexOf('_')+1 )
                    );

                timeList.add(
                        new Times(j, Integer.parseInt( statObj.getData()) )
                        );
            }
            else {
                break;
            }
        }

        for (i = pos-1; i >= 0; i--)
        {
            statObj = (Stat) statList.get(i);
            if ( statObj.getCategory().equals(counter) )
            {
                j = Integer.parseInt(
                            statObj.getName().substring(
                                statObj.getName().indexOf('_')+1 )
                        );

                timeList.add(
                        new Times(j, Integer.parseInt( statObj.getData()) )
                        );
            }
            else {
                break;
            }
        }

        Collections.sort(timeList, new OrderByIndex());
        pos = Collections.binarySearch( statList,
                    new Stat(0.0, category, "", ""), new OrderByCategory() );

        if (pos < 0) {
            return acc;
        }

        statObj = (Stat) statList.get(pos);
        j = Integer.parseInt(
                    statObj.getName().substring(
                        statObj.getName().indexOf('_')+1 )
                );

        int k = Collections.binarySearch( timeList, new Times(j,0),
                    new OrderByIndex() );

        acc.add( new Double(statObj.getData()).doubleValue(),
                ((Times) timeList.get(k)).getValue() );

        for (i = pos+1; i < statList.size(); i++)
        {
            statObj = (Stat) statList.get(i);
            if ( statObj.getCategory().equals(category) )
            {
                j = Integer.parseInt(
                            statObj.getName().substring(
                                statObj.getName().indexOf('_')+1 )
                        );

                k = Collections.binarySearch(timeList, new Times(j,0),
                        new OrderByIndex());

                acc.add(new Double(statObj.getData()).doubleValue(),
                        ((Times) timeList.get(k)).getValue() );
            }
            else {
                break;
            }
        }

        for (i = pos-1; i >= 0; i--)
        {
            statObj = (Stat) statList.get(i);

            if (statObj.getCategory().equals(category))
            {
                j = Integer.parseInt(
                            statObj.getName().substring(
                                statObj.getName().indexOf('_')+1 )
                        );

                k = Collections.binarySearch( timeList, new Times(j,0),
                        new OrderByIndex() );

                acc.add( new Double(statObj.getData()).doubleValue(),
                        ((Times) timeList.get(k)).getValue() );
            }
            else {
                break;
            }
        }

        return acc;
    }

    /**
     * A method that gets one process event at one time until the end
     * of a simulation, then records its statistics.
     * <p>
     * The services available to other GridSim entities are:
     * <ul>
     *      <li> GridSimTags.RECORD_STATISTICS </li>
     *      <li> GridSimTags.RETURN_ACC_STATISTICS_BY_CATEGORY </li>
     * </ul>
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // Process Events until END_OF_SIMULATION is received
        Sim_event ev = new Sim_event();
        for (sim_get_next(ev); ev.get_tag() != GridSimTags.END_OF_SIMULATION;
                sim_get_next(ev) )
        {
            if ( !active_ ) {
                continue; // Skip processing of this event
            }

            switch ( ev.get_tag() )
            {
                case GridSimTags.RECORD_STATISTICS:
                    if (ev.get_data() != null) {
                        recordStat( (Stat) ev.get_data() );
                    }
                    break;

                case GridSimTags.RETURN_ACC_STATISTICS_BY_CATEGORY:
                    returnAccStatByCategory(ev);
                    break;

                default:
                    System.out.println("GridStatistics.body() : " +
                            "Unable to handle request from GridSimTags " +
                            "with constant number " + ev.get_tag() );
                    break;
            }
        }

        if (active_) {
            outFile_.close();
        }
    }

    /**
     * Records the given statistics into a file
     * @param stat a Stat object
     * @see gridsim.Stat
     * @pre stat != null
     * @post $none
     */
    public void recordStat(Stat stat)
    {
        boolean flag = true;
        int i = 0;
        int length = 0;

        if (excludeFromProcessing_ != null)
        {
            length = excludeFromProcessing_.length;
            for (i = 0; i < length; i++)
            {
                if ( (stat.getCategory()+".").startsWith(excludeFromProcessing_[i]+".") )
                {
                    flag = false;
                    break;
                }
            }
        }

        if (flag) {
            statList_.add(stat);
        }

        flag = true;
        if (excludeFromFile_ != null)
        {
            length = excludeFromFile_.length;
            for (i = 0; i < length; i++)
            {
                if ( (stat.getCategory()+".").startsWith(excludeFromFile_[i]+".") )
                {
                    flag = false;
                    break;
                }
            }
        }

        if (flag) {
            outFile_.println( stat.toString() );
        }
    }

    /**
     * Sends an Accumulator object based on category into an event scheduler.
     * @param ev    an object of Sim_event
     * @see eduni.simjava.Sim_event
     * @pre ev != null
     * @post $none
     */
    public void returnAccStatByCategory(Sim_event ev)
    {
        statSortByCategoryData_ = new LinkedList(statList_);
        Collections.sort( statSortByCategoryData_, new OrderByCategoryData() );

        String category = (String) ev.get_data();
        Accumulator acc = accumulate(category);

        super.sim_schedule(ev.get_src(), 0.0, ev.get_tag(), acc);
    }

} 

