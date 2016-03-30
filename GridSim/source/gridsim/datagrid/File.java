/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */

package gridsim.datagrid;

import gridsim.ParameterException;

/**
 * A class for representing a physical file in a DataGrid environment
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */

public class File {

    private String name_;           // logical file name
    private FileAttribute attr_;    // a file attribute

    // a transaction time for adding / getting /deleting this file
    private double transTime_;

    /** Denotes that this file has not been registered to a Replica Catalogue */
    public static final int NOT_REGISTERED = -1;

    /** Denotes that the type of this file is unknown */
    public static final int TYPE_UNKOWN = 0;

    /** Denotes that the type of this file is a raw data */
    public static final int TYPE_RAW_DATA = 1;

    /** Denotes that the type of this file is a reconstructed data */
    public static final int TYPE_RECONSTRUCTED_DATA = 2;

    /** Denotes that the type of this file is a tag data */
    public static final int TYPE_TAG_DATA = 3;


    /**
     * Creates a new DataGrid file with a given size (in MBytes). <br>
     * NOTE: By default, a newly-created file is set to a <b>master</b> copy.
     * @param fileName  file name
     * @param fileSize  file size is in MBytes
     * @throws ParameterException This happens when one of the following
     * scenarios occur:
     *      <ul>
     *      <li> the file name is empty or <tt>null</tt>
     *      <li> the file size is zero or negative numbers
     *      </ul>
     */
    public File(String fileName, int fileSize) throws ParameterException
    {
        if (fileName == null || fileName.length() == 0) {
            throw new ParameterException("File(): Error - invalid file name.");
        }

        if (fileSize <= 0) {
            throw new ParameterException("File(): Error - size <= 0.");
        }

        name_ = fileName;
        attr_ = new FileAttribute(fileName, fileSize);
        transTime_ = 0;
    }

    /**
     * Copy constructor, i.e. cloning from a source file into this object,
     * but this object is set to a <b>replica</b>
     * @param file  the source of a File object to copy
     * @throws ParameterException This happens when the source file is
     *                            <tt>null</tt>
     */
    public File(File file) throws ParameterException
    {
        if (file == null) {
            throw new ParameterException("File(): Error - file is null.");
        }

        // copy the attributes into the file
        FileAttribute fileAttr = file.getFileAttribute();
        attr_.copyValue(fileAttr);
        fileAttr.setMasterCopy(false);   // set this file to replica
    }

    /**
     * Clone this file but the clone file is set to a <b>replica</b>
     * @return  a clone of this file (as a replica)
     *          or <tt>null</tt> if an error occurs
     */
    public File makeReplica() {
        return makeCopy();
    }

    /**
     * Clone this file and make the new file as a <b>master</b> copy as well
     * @return  a clone of this file (as a master copy)
     *          or <tt>null</tt> if an error occurs
     */
    public File makeMasterCopy()
    {
        File file = makeCopy();
        if (file != null) {
            file.setMasterCopy(true);
        }

        return file;
    }

    /**
     * Makes a copy of this file
     * @return  a clone of this file (as a replica)
     *          or <tt>null</tt> if an error occurs
     */
    private File makeCopy()
    {
        File file = null;
        try
        {
            file = new File(name_, attr_.getFileSize());
            FileAttribute fileAttr = file.getFileAttribute();
            attr_.copyValue(fileAttr);
            fileAttr.setMasterCopy(false);   // set this file to replica
        }
        catch (Exception e) {
            file = null;
        }

        return file;
    }

    /**
     * Gets an attribute of this file
     * @return a file attribute
     */
    public FileAttribute getFileAttribute() {
        return attr_;
    }

    /**
     * Gets the size of this object (in byte).<br>
     * NOTE: This object size is NOT the actual file size. Moreover,
     * this size is used for transferring this object over a network.
     * @return the object size (in byte)
     */
    public int getAttributeSize() {
        return attr_.getAttributeSize();
    }

    /**
     * Sets the resource ID that stores this file
     * @param resourceID    a resource ID
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setResourceID(int resourceID) {
        return attr_.setResourceID(resourceID);
    }

    /**
     * Gets the resource ID that stores this file
     * @return the resource ID
     */
    public int getResourceID() {
        return attr_.getResourceID();
    }

    /**
     * Returns the file name
     * @return the file name
     */
    public String getName() {
        return attr_.getName();
    }

    /**
     * Sets the file name
     * @param name  the file name
     */
    public void setName(String name) {
       attr_.setName(name);
    }

    /**
     * Sets the owner name of this file
     * @param name  the owner name
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setOwnerName(String name) {
        return attr_.setOwnerName(name);
    }

    /**
     * Gets the owner name of this file
     * @return the owner name or <tt>null</tt> if empty
     */
    public String getOwnerName() {
        return attr_.getOwnerName();
    }

    /**
     * Gets the file size (in MBytes)
     * @return the file size (in MBytes)
     */
    public int getSize() {
        return attr_.getFileSize();
    }

    /**
     * Gets the file size (in bytes)
     * @return the file size (in bytes)
     */
    public int getSizeInByte() {
        return attr_.getFileSizeInByte();
    }

    /**
     * Sets the file size (in MBytes)
     * @param fileSize  the file size (in MBytes)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setFileSize(int fileSize) {
        return attr_.setFileSize(fileSize);
    }

    /**
     * Sets the last update time of this file (in seconds)<br>
     * NOTE: This time is relative to the start time. Preferably use
     *       {@link gridsim.GridSim#clock()} method.
     * @param time  the last update time (in seconds)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setUpdateTime(double time) {
        return attr_.setUpdateTime(time);
    }

    /**
     * Gets the last update time (in seconds)
     * @return the last update time (in seconds)
     */
    public double getLastUpdateTime() {
        return attr_.getLastUpdateTime();
    }

    /**
     * Sets the file registration ID (published by a Replica Catalogue entity)
     * @param id    registration ID
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setRegistrationID(int id) {
        return attr_.setRegistrationID(id);
    }

    /**
     * Gets the file registration ID
     * @return registration ID
     */
    public int getRegistrationID() {
        return attr_.getRegistrationID();
    }

    /**
     * Sets the file type (e.g. raw, tag, etc)
     * @param type  a file type
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setType(int type) {
        return attr_.setType(type);
    }

    /**
     * Gets this file type
     * @return file type
     */
    public int getType() {
        return attr_.getType();
    }

    /**
     * Sets the checksum of this file
     * @param checksum  the checksum of this file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setChecksum(int checksum) {
        return attr_.setChecksum(checksum);
    }

    /**
     * Gets the file checksum
     * @return file checksum
     */
    public int getChecksum() {
        return attr_.getChecksum();
    }

    /**
     * Sets the cost associated with this file
     * @param cost  cost of this file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setCost(double cost) {
        return attr_.setCost(cost);
    }

    /**
     * Gets the  cost associated with this file
     * @return the cost of this file
     */
    public double getCost() {
        return attr_.getCost();
    }

    /**
     * Gets the file creation time (in millisecond)
     * @return the file creation time (in millisecond)
     */
    public long getCreationTime() {
        return attr_.getCreationTime();
    }

    /**
     * Checks if this file already registered to a Replica Catalogue
     * @return <tt>true</tt> if it is registered, <tt>false</tt> otherwise
     */
    public boolean isRegistered() {
        return attr_.isRegistered();
    }

    /**
     * Marks this file as a master copy or replica
     * @param masterCopy    a flag denotes <tt>true</tt> for master copy or
     *                      <tt>false</tt> for a replica
     */
    public void setMasterCopy(boolean masterCopy) {
        attr_.setMasterCopy(masterCopy);
    }

    /**
     * Checks whether this file is a master copy or replica
     * @return <tt>true</tt> if it is a master copy or <tt>false</tt> otherwise
     */
    public boolean isMasterCopy() {
        return attr_.isMasterCopy();
    }

    /**
     * Marks this file as a read only or not
     * @param readOnly      a flag denotes <tt>true</tt> for read only or
     *                      <tt>false</tt> for re-writeable
     */
    public void setReadOnly(boolean readOnly) {
        attr_.setReadOnly(readOnly);
    }

    /**
     * Checks whether this file is a read only or not
     * @return <tt>true</tt> if it is a read only or <tt>false</tt> otherwise
     */
    public boolean isReadOnly() {
        return attr_.isReadOnly();
    }

    /**
     * Sets the current transaction time (in second) of this file.
     * This transaction time can be related to the operation of adding /
     * deleting / getting this file on a resource's storage.
     * @param time  the transaction time (in second)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @see gridsim.datagrid.storage.Storage#addFile(File)
     * @see gridsim.datagrid.storage.Storage#addFile(List)
     * @see gridsim.datagrid.storage.Storage#addReservedFile(File)
     * @see gridsim.datagrid.storage.Storage#deleteFile(File)
     * @see gridsim.datagrid.storage.Storage#deleteFile(String)
     * @see gridsim.datagrid.storage.Storage#deleteFile(String, File)
     * @see gridsim.datagrid.storage.Storage#getFile(String)
     * @see gridsim.datagrid.storage.Storage#renameFile(File, String)
     */
    public boolean setTransactionTime(double time)
    {
        if (time < 0) {
            return false;
        }

        transTime_ = time;
        return true;
    }

    /**
     * Gets the last transaction time of this file (in second).
     * @return the transaction time (in second)
     */
    public double getTransactionTime() {
        return transTime_;
    }

} 

