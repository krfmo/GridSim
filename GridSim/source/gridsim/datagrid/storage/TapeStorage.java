/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */
 
package gridsim.datagrid.storage;

import gridsim.datagrid.File;
import gridsim.ParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * An implementation of a tape storage system. It simulates the behaviour of a
 * typical tape storage with the following assumptions:
 * <ol>
 *    <li> a constant operation for rewind, access and transfer time
 *    <li> for every operation, the tape needs to be rewinded to the front
 *    <li> a tape is supposed to be for backup purposes, once it is full,
 *         individual files can't be deleted unless the tape is cleared
 * </ol>
 * <br>
 * The default values for this storage are those of
 * a HP Ultrium tape with the following parameters:
 * <ul>
 *      <li> access time = 142 seconds from the beginning of a tape
 *      <li> rewind time = 284 seconds from the ending of a tape
 *      <li> max transfer rate = 8 MB/sec (raw data and uncompressed)
 * </ul>
 *
 * @author Uros Cibej and Anthony Sulistio
 * @since GridSim Toolkit 4.0
 * @see gridsim.datagrid.storage.Storage
 */

public class TapeStorage implements Storage {

    /** a list storing the names of all the files on the tape drive */
    private ArrayList nameList_;

    /** a list storing all the files stored on the tape */
    private LinkedList fileList_;

    /** the name of the tape drive*/
    private String name_;

    /** the current size of files on the tape in MB */
    private double currentSize_;

    /** the total capacity of the tape in MB*/
    private double capacity_;

    /** the total rewind time in seconds */
    private int rewindTime_;

    /** the total access time in seconds */
    private int accessTime_;

    /** the maximum transfer rate in MB/sec */
    private double maxTransferRate_;

    /**
     * Constructs a new tape storage with a given name and capacity.
     *
     * @param name the name of the new tape drive
     * @param capacity the capacity in MB
     * @throws ParameterException when the name and the capacity are not valid
     */
    public TapeStorage(String name, double capacity) throws ParameterException {
        if (name == null || name.length() == 0) {
            throw new ParameterException(
                    "TapeStorage(): Error - invalid tape drive name.");
        }
        if (capacity <= 0) {
            throw new ParameterException(
                    "TapeStorage(): Error - capacity <= 0.");
        }
        name_ = name;
        capacity_ = capacity;
        init();
    }

    /**
     * Constructs a new tape storage with a given capacity.
     * In this case the name of the storage is a default name.
     * @param capacity the capacity in MB
     * @throws ParameterException when the capacity is not valid
     */
    public TapeStorage(double capacity) throws ParameterException {
        if (capacity <= 0) {
            throw new ParameterException(
                    "TapeStorage(): Error - capacity <= 0.");
        }

        name_ = "TapeStorage";
        capacity_ = capacity;
        init();
    }

    /**
     * The initialization of the tape is done in this method. The most common
     * parameters, such as access time, rewind time and maximum transfer rate,
     * are set. The default values are set to simulate the HP Ultrium Tape.
     * Furthermore, the necessary lists are created.
     */
    private void init() {
        nameList_ = new ArrayList();
        fileList_ = new LinkedList();
        currentSize_ = 0;

        // NOTE: Default value is taken from HP Ultrium Tape
        accessTime_ = 142; // sec from beginning of tape
        rewindTime_ = 284; // sec from end of tape

        // MB/sec for raw data not compressed (15MB/s)
        maxTransferRate_ = 8;
    }

    /**
     * Gets the available space on this storage in MB.
     *
     * @return  the available space in MB
     */
    public double getAvailableSpace() {
        return capacity_ - currentSize_;
    }

    /**
     * Checks if the storage is full or not.
     *
     * @return  <tt>true</tt> if the storage is full, <tt>false</tt>
     *         otherwise
     */
    public boolean isFull() {
        if (currentSize_ == capacity_) {
            return true;
        }

        return false;
    }

    /**
     * Gets the number of files stored on this storage.
     *
     * @return the number of stored files
     */
    public int getNumStoredFile() {
        return fileList_.size();
    }

    /**
     * Makes a reservation of the space on the storage to store a file.
     *
     * @param fileSize the size to be reserved in MB
     *
     * @return <tt>true</tt> if reservation succeeded, <tt>false</tt>
     *         otherwise
     */
    public boolean reserveSpace(int fileSize) {
        if (fileSize <= 0) {
            return false;
        }

        if (currentSize_ + fileSize >= capacity_) {
            return false;
        }

        currentSize_ += fileSize;
        return true;
    }

    /**
     * Adds a file for which the space has already been reserved.
     * The time taken (in seconds) for adding the file can also be
     * found using {@link gridsim.datagrid.File#getTransactionTime()}.
     * @param file the file to be added
     *
     * @return the time (in seconds) required to add the file
     */
    public double addReservedFile(File file) {
        if (file == null) {
            return 0;
        }

        currentSize_ -= file.getSize();
        double result = addFile(file);

        // if add file fails, then set the current size back to its old value
        if (result == 0.0) {
            currentSize_ += file.getSize();
        }

        return result;
    }

    /**
     * Checks whether there is enough space on the storage for a certain file.
     *
     * @param fileSize
     *            a FileAttribute object to compare to
     * @return <tt>true</tt> if enough space available, <tt>false</tt>
     *         otherwise
     */
    public boolean hasPotentialAvailableSpace(int fileSize) {
        if (fileSize <= 0) {
            return false;
        }

        // check if enough space left
        if (getAvailableSpace() > fileSize) {
            return true;
        }

        Iterator it = fileList_.iterator();
        File file = null;
        int deletedFileSize = 0;

        // if not enough space, then if want to clear some files
        boolean result = false;
        while (it.hasNext()) {
            file = (File) it.next();
            if (!file.isReadOnly()) {
                deletedFileSize += file.getSize();
            }

            if (deletedFileSize > fileSize) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Gets the total capacity of the storage in MB.
     *
     * @return  the capacity of the storage in MB
     */
    public double getCapacity() {
        return capacity_;
    }

    /**
     * Gets the current size of the stored files in MB.
     *
     * @return  the current size of the stored files in MB
     */
    public double getCurrentSize() {
        return currentSize_;
    }

    /**
     * Gets the name of the storage.
     *
     * @return  the name of this storage
     */
    public String getName() {
        return name_;
    }

    /**
     * Gets the total access time of this tape drive in seconds.
     * @return the total access time in seconds
     */
    public int getTotalAccessTime() {
        return accessTime_;
    }

    /**
     * Sets the total access time for this tape in seconds.
     *
     * @param time  the total access time in seconds
     * @return <tt>true</tt> if the setting succeeds, <tt>false</tt>
     *         otherwise
     */
    public boolean setTotalAccessTime(int time) {
        if (time <= 0) {
            return false;
        }

        accessTime_ = time;
        return true;
    }

    /**
     * Gets the maximum transfer rate of the storage in MB/sec.
     *
     * @return  the maximum transfer rate in MB/sec
     */
    public double getMaxTransferRate() {
        return maxTransferRate_;
    }

    /**
     * Sets the maximum transfer rate of this storage system in MB/sec.
     *
     * @param rate the maximum transfer rate in MB/sec
     * @return  <tt>true</tt> if the setting succeeded, <tt>false</tt>
     *         otherwise
     */
    public boolean setMaxTransferRate(int rate)
    {
        if (rate <= 0) {
            return false;
        }

        maxTransferRate_ = rate;
        return true;
    }

    /**
     * Sets the total rewind time of the tape. The total rewind time is the time
     * needed to rewind the tape from the end to the beginning.
     *
     * @param time  the total rewind time in seconds
     * @return <tt>true</tt> if the setting succeeded, <tt>false</tt> otherwise
     */
    public boolean setTotalRewindTime(int time)
    {
        if (time <= 0) {
            return false;
        }

        rewindTime_ = time;
        return true;
    }

    /**
     * Gets the total rewind time of the tape in seconds.
     *
     * @return  the total rewind time in seconds
     */
    public int getTotalRewindTime() {
        return rewindTime_;
    }

    /**
     * Gets the file with the specified name.
     * The time taken (in seconds) for getting the file can also be
     * found using {@link gridsim.datagrid.File#getTransactionTime()}.
     * @param fileName the name of the needed file
     * @return the file with the specified filename
     */
    public File getFile(String fileName) {
        File obj = null;

        // check first whether file name is valid or not
        if (fileName == null || fileName.length() == 0) {
            System.out.println(name_ + ".getFile(): Warning - invalid "
                    + "file name.");
            return obj;
        }

        Iterator it = fileList_.iterator();
        int size = 0;
        int index = 0;
        boolean found = false;

        // iterate linearly in a tape to find the file
        while (it.hasNext()) {
            obj = (File) it.next();
            size += obj.getSize();

            if (obj.getName().equals(fileName)) {
                found = true;
                break;
            }

            index++;
        }
        // if the file is found, then determine the time taken to get it
        if (found == true) {
            obj = (File) fileList_.get(index);
            double rewindTime = getRewindTime(size);
            double accessTime = getAccessTime(size - obj.getSize());
            double transferTime = getTransferTime(obj.getSize());

            // total time for this operation
            obj.setTransactionTime(rewindTime + accessTime + transferTime);
        }

        return obj;
    }

    /**
     * Gets the list of file names located on this storage.
     *
     * @return a LinkedList of file names
     */
    public List getFileNameList() {
        return nameList_;
    }

    /**
     * Checks if the file is valid or not. This method checks whether the given
     * file or the file name of the file is valid. The method name parameter is
     * used for debugging purposes, to output in which method an error has
     * occured.
     *
     * @param file  the file to be checked for validity
     * @param methodName   the name of the method in which we check for
     *                     validity of the file
     * @return <tt>true</tt> if the file is valid, <tt>false</tt>
     *         otherwise
     */
    private boolean isFileValid(File file, String methodName) {
        // check if the file is invalid or not
        if (file == null) {
            System.out.println(name_ + "." + methodName
                    + ": Warning - the given file is null.");
            return false;
        }

        String fileName = file.getName();
        if (fileName == null || fileName.length() == 0) {
            System.out.println(name_ + "." + methodName
                    + ": Warning - invalid file name.");
            return false;
        }

        return true;
    }

    /**
     * Adds a file to the storage. First the method checks if there is enough
     * space on the storage, then it checks if the file with the same name is
     * already taken to avoid duplicate filenames. <br>
     * The time taken (in seconds) for adding the file can also be
     * found using {@link gridsim.datagrid.File#getTransactionTime()}.
     *
     * @param file  the file to be added
     * @return the time taken (in seconds) for adding the specified file
     */
    public double addFile(File file) {
        double result = 0.0;

        // check if the file is valid or not
        if (isFileValid(file, "addFile()") == false) {
            return result;
        }

        // check the capacity
        if (file.getSize() + currentSize_ >= capacity_) {
            System.out.println(name_ + ".addFile(): Warning - not enough space"
                    + " to store " + file.getName());
            return result;
        }

        // check if the same file name is alredy taken
        if (!contains(file.getName())) {
            double accessTime = getAccessTime(currentSize_);
            double transferTime = getTransferTime(file.getSize());

            nameList_.add(file.getName());
            fileList_.add(file); // add the file into the tape
            currentSize_ += file.getSize(); // increment the current tape size

            // rewind time is calculated after the file has been written into
            // the tape. Hence, currentSize = currentSize + file size
            double rewindTime = getRewindTime(currentSize_);
            result = accessTime + transferTime + rewindTime; // total time
        }

        file.setTransactionTime(result);
        return result;
    }

    /**
     * Adds a set of files to the storage.
     * Run through the list of files and save all of them.
     * The time taken (in seconds) for adding each file can also be
     * found using {@link gridsim.datagrid.File#getTransactionTime()}.
     * @param list the files to be added
     * @return the time taken (in seconds) for adding the specified files
     */
    public double addFile(List list) {
        double result = 0.0;
        if (list == null || list.size() == 0) {
            System.out.println(name_ + ".addFile(): Warning - list is empty.");
            return result;
        }

        Iterator it = list.iterator();
        File file = null;

        // add each file in the list into the tape
        while (it.hasNext()) {
            file = (File) it.next();
            result += addFile(file);
        }

        return result;
    }

    /**
     * Removes a file from the storage -- <b>NOT SUPPORTED</b>.<br>
     * NOTE: a tape is supposed to be for backup purposes, once it is full,
     *       individual files can't be deleted unless the tape is cleared.
     * @param fileName the name of the file to be removed
     * @return <tt>null</tt>
     */
    public File deleteFile(String fileName) {
        System.out.println(name_ + ".deleteFile(): Not supported.");
        return null;
    }

    /**
     * Removes a file from the storage -- <b>NOT SUPPORTED</b>.<br>
     * NOTE: a tape is supposed to be for backup purposes, once it is full,
     *       individual files can't be deleted unless the tape is cleared.
     * @param fileName the name of the file to be removed
     * @param file the file which is removed from the storage is returned
     *        through this parameter
     * @return the time taken (in seconds) for deleting the specified file
     */
    public double deleteFile(String fileName, File file) {
        return deleteFile(file);
    }

    /**
     * Removes a file from the storage -- <b>NOT SUPPORTED</b>.<br>
     * NOTE: a tape is supposed to be for backup purposes, once it is full,
     *       individual files can't be deleted unless the tape is cleared.
     * @param file the file which is removed from the storage is returned
     *        through this parameter
     * @return the time taken (in seconds) for deleting the specified file
     */
    public double deleteFile(File file) {
        System.out.println(name_ + ".deleteFile(): Not supported.");
        return 0.0;
    }

    // NOTE: I assume a tape has an indexing system, rather than search through
    // the whole tape.
    /**
     * Checks whether a certain file is on the storage or not.
     *
     * @param fileName the name of the file we are looking for
     * @return <tt>true</tt> if the file is in the storage, <tt>false</tt>
     *         otherwise
     */
    public boolean contains(String fileName) {
        boolean result = false;
        if (fileName == null || fileName.length() == 0) {
            System.out.println(name_
                    + ".contains(): Warning - invalid file name");
            return result;
        }

        // check each file in the list
        Iterator it = nameList_.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.equals(fileName)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Checks whether a certain file is on the storage or not.
     *
     * @param file the file we are looking for
     * @return <tt>true</tt> if the file is in the storage, <tt>false</tt>
     *         otherwise
     */
    public boolean contains(File file) {
        boolean result = false;
        if (!isFileValid(file, "contains()")) {
            return result;
        }

        result = contains(file.getName());
        return result;
    }

    /**
     * Gets the access time of this tape in seconds.
     * fileSize should be from the file starting point in the tape.
     *
     * @return  the access time in seconds.
     */
    private double getAccessTime(double fileSize) {
        double result = 0.0;
        if (fileSize > 0 && capacity_ != 0) {
            result = ((double) fileSize * accessTime_) / capacity_;
        }

        return result;
    }

    /**
     * Gets the rewind time in seconds.
     * fileSize should be from the file ending point in the tape.
     *
     * @return  the rewind time in seconds
     */
    private double getRewindTime(double fileSize) {
        double result = 0.0;
        if (fileSize > 0 && capacity_ != 0) {
            result = ((double) fileSize * rewindTime_) / capacity_;
        }

        return result;
    }

    /**
     * Gets the transfer time in seconds.
     * fileSize should be from the file ending point in the tape
     *
     * @return  the transfer time in seconds
     */
    private double getTransferTime(int fileSize) {
        double result = 0.0;
        if (fileSize > 0 && capacity_ != 0) {
            result = ((double) fileSize * maxTransferRate_) / capacity_;
        }

        return result;
    }

    /**
     * Renames a file on the storage.
     * The time taken (in seconds) for renaming the file can also be
     * found using {@link gridsim.datagrid.File#getTransactionTime()}.
     * @param file the file we would like to rename
     * @param newName the new name of the file
     *
     * @return <tt>true</tt> if the renaming succeeded, <tt>false</tt>
     *         otherwise
     */
    public boolean renameFile(File file, String newName) {
        // check whether the new filename is conflict with existing ones
        // or not
        boolean result = false;
        if (contains(newName)) {
            return result;
        }

        // replace the file name in the file (physical) list
        File obj = getFile(file.getName());
        if (obj != null) {
            obj.setName(newName);
        } else {
            return result;
        }

        // replace the file name in the name list
        Iterator it = nameList_.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.equals(file.getName())) {
                nameList_.remove(name);
                nameList_.add(newName);
                result = true;
                break;
            }
        }

        return result;
    }

} 

