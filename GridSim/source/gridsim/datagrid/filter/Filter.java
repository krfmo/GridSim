/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */
 
package gridsim.datagrid.filter;

import gridsim.datagrid.FileAttribute;

/**
 * An abstract class for filtering a file from the Replica Catalogue list
 * based on its attributes,
 * such as file name, size, etc. All you need to do is the following:
 * <ol>
 *      <li> creates a child class that extends this class
 *      <li> the child class' constructor should contain a list of
 *           attributes to compare
 *      <li> implement the {@link #match(FileAttribute)} method for
 *           comparison
 * </ol>
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 * @see     gridsim.datagrid.filter.FileNameFilter
 */
public abstract class Filter {

    /**
     * For each file in the Replica Catalogue list,
     * check whether it contains
     * a FileAttribute object with the given matching attribute(s)
     * @param attr  a FileAttribute object to compare to
     * @return <tt>true</tt> if matching, <tt>false</tt> otherwise
     */
    public abstract boolean match(FileAttribute attr);

} 

