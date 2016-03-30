/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */
 
package gridsim.datagrid;

import java.util.LinkedList;

/**
 * A DataGridlet is an extension of the {@link gridsim.Gridlet} class,
 * which requires one or more data files to run.
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public class DataGridlet extends gridsim.Gridlet {

    private LinkedList requiredFiles_ = null;   // list of required filenames


    /**
     * Creates a new DataGridlet object. The gridlet length, input and output
     * file sizes should be greater than or equal to 1.
     * By default, this constructor records the history of this object.
     *
     * @param gridletID            the unique ID of this Gridlet
     * @param gridletLength        the length or size (in MI) of this Gridlet
     *                             to be executed in a GridResource
     * @param gridletFileSize      the file size (in byte) of this Gridlet
     *                             <tt>BEFORE</tt> submitting to a GridResource
     * @param gridletOutputSize    the file size (in byte) of this Gridlet
     *                             <tt>AFTER</tt> finish executing by
     *                             a GridResource
     * @param list                 a list of required filenames to execute
     *                             this Gridlet
     */
    public DataGridlet(int gridletID, double gridletLength,
                      long gridletFileSize, long gridletOutputSize,
                      LinkedList list)
    {
        super(gridletID, gridletLength, gridletFileSize, gridletOutputSize);
        requiredFiles_ = list;
    }

    /**
     * Creates a new DataGridlet object. The gridlet length, input and output
     * file sizes should be greater than or equal to 1.
     *
     * @param gridletID            the unique ID of this Gridlet
     * @param gridletLength        the length or size (in MI) of this Gridlet
     *                             to be executed in a GridResource
     * @param gridletFileSize      the file size (in byte) of this Gridlet
     *                             <tt>BEFORE</tt> submitting to a GridResource
     * @param gridletOutputSize    the file size (in byte) of this Gridlet
     *                             <tt>AFTER</tt> finish executing by
     *                             a GridResource
     * @param record               record the history of this object or not
     * @param list                 a list of required files to execute
     *                             this Gridlet
     */
    public DataGridlet(int gridletID, double gridletLength,
                      long gridletFileSize, long gridletOutputSize,
                      boolean record, LinkedList list)
    {
        super(gridletID,gridletLength,gridletFileSize,gridletOutputSize,record);
        requiredFiles_ = list;
    }

    /**
     * Creates a new DataGridlet object. The gridlet length, input and output
     * file sizes should be greater than or equal to 1.
     *
     * @param gridletID            the unique ID of this Gridlet
     * @param gridletLength        the length or size (in MI) of this Gridlet
     *                             to be executed in a GridResource
     * @param gridletFileSize      the file size (in byte) of this Gridlet
     *                             <tt>BEFORE</tt> submitting to a GridResource
     * @param gridletOutputSize    the file size (in byte) of this Gridlet
     *                             <tt>AFTER</tt> finish executing by
     *                             a GridResource
     * @param record               record the history of this object or not
     */
    public DataGridlet(int gridletID, double gridletLength,
                      long gridletFileSize, long gridletOutputSize,
                      boolean record)
    {
        super(gridletID,gridletLength,gridletFileSize,gridletOutputSize,record);
        requiredFiles_ = null;
    }

    /**
     * Returns the list files that this gridlet needs for execution.
     *
     * @return A list of required files or <tt>null</tt> if empty
     */
    public LinkedList getRequiredFiles() {
        return requiredFiles_;
    }

    /**
     * Adds the required filename to the list
     * @param fileName  the required filename
     * @return <tt>true</tt> if succesful, <tt>false</tt> otherwise
     */
    public boolean addRequiredFile(String fileName) {

        // if the list is empty
        if (requiredFiles_ == null) {
            requiredFiles_ = new LinkedList();
        }

        // then check whether filename already exists or not
        boolean result = false;
        for (int i = 0; i < requiredFiles_.size(); i++) {
            String temp = (String) requiredFiles_.get(i);
            if (temp.equals(fileName)) {
                result = true;
                break;
            }
        }

        if (!result) {
            requiredFiles_.add(fileName);
        }

        return result;
    }

    /**
     * Deletes the given filename from the list
     * @param filename  the given filename to be deleted
     * @return <tt>true</tt> if succesful, <tt>false</tt> otherwise
     */
    public boolean deleteRequiredFile(String filename) {
        boolean result = false;
        if (requiredFiles_ == null) {
            return result;
        }

        for (int i = 0; i < requiredFiles_.size(); i++) {
            String temp = (String) requiredFiles_.get(i);

            if (temp.equals(filename)) {
                requiredFiles_.remove(i);
                result = true;

                break;
            }
        }

        return result;
    }

    /**
     * Checks whether this gridlet requires any files or not
     * @return <tt>true</tt> if required, <tt>false</tt> otherwise
     */
    public boolean requiresFiles() {

        boolean result = false;
        if (requiredFiles_ != null && requiredFiles_.size() > 0) {
            result = true;
        }

        return result;
    }

} 

