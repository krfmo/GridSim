/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia
 */

package gridsim.datagrid;

import gridsim.GridSim;
import gridsim.ParameterException;

import java.util.Date;


/**
 * A class for storing related information regarding to a
 * {@link gridsim.datagrid.File} entity.
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */

public class FileAttribute {

    private String name_;           // logical file name
    private String ownerName_;      // owner name of this file
    private int id_;                // file ID given by a Replica Catalogue
    private int type_;              // file type, e.g. raw, reconstructed, etc
    private int size_;              // file size in byte
    private int checksum_;          // check sum
    private double lastUpdateTime_; // last updated time (sec) - relative
    private long creationTime_;     // creation time (ms) - abosulte/relative
    private double cost_;           // price of this file
    private boolean masterCopy_;    // false if it is a replica
    private boolean readOnly_;      // false if it can be rewritten
    private int resourceID_;        // resource ID storing this file


    /**
     * Allocates a new FileAttribute class.
     * @param fileName  file name
     * @param fileSize  size of this file (in bytes)
     * @throws ParameterException This happens when one of the following
     * scenarios occur:
     *      <ul>
     *      <li> the file name is empty or <tt>null</tt>
     *      <li> the file size is zero or negative numbers
     *      </ul>
     */
    public FileAttribute(String fileName, int fileSize)
                         throws ParameterException {
        // check for errors in the input
        if (fileName == null || fileName.length() == 0) {
            throw new ParameterException(
                        "FileAttribute(): Error - invalid file name.");
        }

        if (fileSize <= 0) {
            throw new ParameterException(
                        "FileAttribute(): Error - size <= 0.");
        }

        size_ = fileSize;
        name_ = fileName;

        // set the file creation time. This is absolute time
        Date date = GridSim.getSimulationCalendar().getTime();
        if (date != null) {
            creationTime_ = date.getTime();
        } else {
            creationTime_ = 0;
        }

        ownerName_ = null;
        id_ = File.NOT_REGISTERED;
        checksum_ = 0;
        type_ = File.TYPE_UNKOWN;
        lastUpdateTime_ = 0;
        cost_ = 0;
        resourceID_ = -1;
        masterCopy_ = true;
        readOnly_ = false;
    }

    /**
     * Copy the values of this object into another FileAttribute class
     * @param attr  a FileAttribute object (the destination)
     * @return <tt>true</tt> if the copy operation is successful,
     *         <tt>false</tt> otherwise
     */
    public boolean copyValue(FileAttribute attr) {
        if (attr == null) {
            return false;
        }

        attr.setFileSize(size_);
        attr.setResourceID(resourceID_);
        attr.setOwnerName(ownerName_);
        attr.setUpdateTime(lastUpdateTime_);
        attr.setRegistrationID(id_);
        attr.setType(type_);
        attr.setChecksum(checksum_);
        attr.setCost(cost_);
        attr.setMasterCopy(masterCopy_);
        attr.setReadOnly(readOnly_);
        attr.setName(name_);
        attr.setCreationTime(creationTime_);

        return true;
    }

    /**
     * Sets the file creation time (in millisecond)
     * @param creationTime  the file creation time (in millisecond)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setCreationTime(long creationTime) {
        if (creationTime <= 0) {
            return false;
        }

        creationTime_ = creationTime;
        return true;
    }

    /**
     * Gets the file creation time (in millisecond)
     * @return the file creation time (in millisecond)
     */
    public long getCreationTime() {
        return creationTime_;
    }

    /**
     * Sets the resource ID that stores this file
     * @param resourceID    a resource ID
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setResourceID(int resourceID) {
        if (resourceID == -1) {
            return false;
        }

        resourceID_ = resourceID;
        return true;
    }

    /**
     * Gets the resource ID that stores this file
     * @return the resource ID
     */
    public int getResourceID() {
        return resourceID_;
    }

    /**
     * Sets the owner name of this file
     * @param name  the owner name
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setOwnerName(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }

        ownerName_ = name;
        return true;
    }

    /**
     * Gets the owner name of this file
     * @return the owner name or <tt>null</tt> if empty
     */
    public String getOwnerName() {
        return ownerName_;
    }

    /**
     * Gets the size of this object (in byte).<br>
     * NOTE: This object size is NOT the actual file size. Moreover,
     * this size is used for transferring this object over a network.
     * @return the object size (in byte)
     */
    public int getAttributeSize() {
        int length = DataGridTags.PKT_SIZE;
        if (ownerName_ != null) {
            length += ownerName_.length();
        }

        if (name_ != null) {
            length += name_.length();
        }

        return length;
    }

    /**
     * Sets the file size (in MBytes)
     * @param fileSize  the file size (in MBytes)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setFileSize(int fileSize) {
        if (fileSize < 0) {
            return false;
        }

        size_ = fileSize;
        return true;
    }

    /**
     * Gets the file size (in MBytes)
     * @return the file size (in MBytes)
     */
    public int getFileSize() {
        return size_;
    }

    /**
     * Gets the file size (in bytes)
     * @return the file size (in bytes)
     */
    public int getFileSizeInByte() {
        return size_ * 1000000;   // 1e6
        //return size_ * 1048576;   // 1e6 - more accurate
    }

    /**
     * Sets the last update time of this file (in seconds)<br>
     * NOTE: This time is relative to the start time. Preferably use
     *       {@link gridsim.GridSim#clock()} method.
     * @param time  the last update time (in seconds)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setUpdateTime(double time) {
        if (time <= 0 || time < lastUpdateTime_) {
            return false;
        }

        lastUpdateTime_ = time;
        return true;
    }

    /**
     * Gets the last update time (in seconds)
     * @return the last update time (in seconds)
     */
    public double getLastUpdateTime() {
        return lastUpdateTime_;
    }

    /**
     * Sets the file registration ID (published by a Replica Catalogue entity)
     * @param id    registration ID
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setRegistrationID(int id) {
        if (id < 0) {
            return false;
        }

        id_ = id;
        return true;
    }

    /**
     * Gets the file registration ID
     * @return registration ID
     */
    public int getRegistrationID() {
        return id_;
    }

    /**
     * Sets the file type (e.g. raw, tag, etc)
     * @param type  a file type
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setType(int type) {
        if (type < 0) {
            return false;
        }

        type_ = type;
        return true;
    }

    /**
     * Gets this file type
     * @return file type
     */
    public int getType() {
        return type_;
    }

    /**
     * Sets the checksum of this file
     * @param checksum  the checksum of this file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setChecksum(int checksum) {
        if (checksum < 0) {
            return false;
        }

        checksum_ = checksum;
        return true;
    }

    /**
     * Gets the file checksum
     * @return file checksum
     */
    public int getChecksum() {
        return checksum_;
    }

    /**
     * Sets the cost associated with this file
     * @param cost  cost of this file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setCost(double cost) {
        if (cost < 0) {
            return false;
        }

        cost_ = cost;
        return true;
    }

    /**
     * Gets the  cost associated with this file
     * @return the cost of this file
     */
    public double getCost() {
        return cost_;
    }

    /**
     * Checks if this file already registered to a Replica Catalogue
     * @return <tt>true</tt> if it is registered, <tt>false</tt> otherwise
     */
    public boolean isRegistered() {
        boolean result = true;
        if (id_ == File.NOT_REGISTERED) {
            result = false;
        }

        return result;
    }

    /**
     * Marks this file as a master copy or replica
     * @param masterCopy    a flag denotes <tt>true</tt> for master copy or
     *                      <tt>false</tt> for a replica
     */
    public void setMasterCopy(boolean masterCopy) {
        masterCopy_ = masterCopy;
    }

    /**
     * Checks whether this file is a master copy or replica
     * @return <tt>true</tt> if it is a master copy or <tt>false</tt> otherwise
     */
    public boolean isMasterCopy() {
        return masterCopy_;
    }

    /**
     * Marks this file as a read only or not
     * @param readOnly      a flag denotes <tt>true</tt> for read only or
     *                      <tt>false</tt> for re-writeable
     */
    public void setReadOnly(boolean readOnly) {
        readOnly_ = readOnly;
    }

    /**
     * Checks whether this file is a read only or not
     * @return <tt>true</tt> if it is a read only or <tt>false</tt> otherwise
     */
    public boolean isReadOnly() {
        return readOnly_;
    }

    /**
     * Sets the file name
     * @param name  the file name
     */
    public void setName(String name) {
        this.name_ = name;
    }

    /**
     * Returns the file name
     * @return the file name
     */
    public String getName() {
        return name_;
    }

} 


