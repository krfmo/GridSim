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
 * Creates a filter based on a file name
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public class FileNameFilter extends Filter {
    private String name_;   // file name

    /**
     * Creates a new filtering function based on a given filename
     * @param fileName  a file name
     */
    public FileNameFilter(String fileName) {
        name_ = fileName;
    }

    /**
     * For each file in the Replica Catalogue list,
     * check whether it contains
     * a FileAttribute object with a matching file name
     * @param attr  a FileAttribute object to compare to
     * @return <tt>true</tt> if matching, <tt>false</tt> otherwise
     */
    public boolean match(FileAttribute attr) {

        // check for errors first
        if (attr == null || name_ == null) {
            return false;
        }

        // check based on the file name
        if (attr.getName().startsWith(name_)) {
            return true;
        } else {
            return false;
        }
    }

} 

