/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2004, The University of Melbourne, Australia
 */

package gridsim.util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import gridsim.GridSim;
import gridsim.ParameterException;

/**
 * Records any information that is needed by a GridSim entity.
 * It is the responsibility of each entity to record data. <br>
 * <tt>NOTE:</tt> before the simulation ends, call {@link #finalWrite()}
 * to finalize writing the data into a file and close it.
 * Forgotten to call this method will result in not writing
 * into a file at all.
 * <p>
 * Given the file name, this class will record information in
 * CSV (Comma delimited) format (*.csv) with the first column usually
 * represents the simulation time. The format of the next columns
 * afterward is the responsibility of the coder.
 *
 * @since GridSim Toolkit 3.1
 * @author Anthony Sulistio
 * @invariant $none
 */
public class SimReport
{
    private String name_;
    private StringBuffer buffer_;
    private static final int SIZE = 1000;
    private BufferedOutputStream outBuffer_;
    private String indent_;

    /** A space-delimited text file, i.e. each column is separated by a space */
    public static final int INDENT_SPACE = 0;
    
    /** A comma-delimited text file, i.e. each column is separated by a comma */
    public static final int INDENT_COMMA = 1;
    
    /** A tab-delimited text file, i.e. each column is separated by a tab */
    public static final int INDENT_TAB   = 2;


    /**
     * Creates a new report file. This constructor will automatically append
     * the file extension to the given parameter. Use {@link #finalWrite()}
     * before exiting to write the data into a file and close it.
     * Forgotten to call this method will result in not writing
     * into a file at all.<br>
     * NOTE: By default, each column inside this file is separated by a comma.
     * <br>
     * <b>WARNING:</b> existing file with the same name will be overwritten.
     * @param name  file/entity name
     * @throws ParameterException  This happens when name is invalid or null.
     * @pre name != null
     * @post $none
     */
    public SimReport(String name) throws ParameterException {
        this(name, INDENT_SPACE);
    }
    
    /**
     * Creates a new report file. This constructor will automatically append
     * the file extension to the given parameter. Use {@link #finalWrite()}
     * before exiting to write the data into a file and close it.
     * Forgotten to call this method will result in not writing
     * into a file at all.
     * <br>
     * <b>WARNING:</b> existing file with the same name will be overwritten.
     * @param name    file/entity name
     * @param indent  type of indentation (either a space, a comma or a tab)
     * @throws ParameterException  This happens when name is invalid or null.
     * @pre name != null
     * @post $none
     */
    public SimReport(String name, int indent) throws ParameterException
    {
        String msg = "SimReport(): Error - ";
        if (name == null || name.length() == 0)
        {
            msg += "invalid name.";
            System.out.println(msg);
            throw new ParameterException(msg);
        }

        name_ = name + ".csv";
        try
        {
            FileOutputStream out = new FileOutputStream(name_);
            outBuffer_ = new BufferedOutputStream(out);
        }
        catch(Exception e)
        {
            msg += "unable to create/overwrite " + name_;
            System.out.println(msg);
            throw new ParameterException(msg);
        }

        buffer_ = new StringBuffer(SIZE);
        
        if (indent == INDENT_SPACE) {
            indent_ = " ";
        }
        else if (indent == INDENT_COMMA) {
            indent_ = ", ";
        }
        else if (indent == INDENT_TAB) {
            indent_ = "\t";
        }
        else {
            indent_ = ", ";
        }        
    }

    /**
     * Write the given data into the file. <br>
     * For each row, the format would be: <tt>simulation time, num, desc</tt>.
     * The <tt>simulation time</tt> is automatically handle inside this method.
     * @param   num     integer number
     * @param   desc    the description of this number
     * @pre $none
     * @post $none
     */
    public void write(int num, String desc)
    {
        buffer_.append( GridSim.clock() );
        buffer_.append(indent_);
        buffer_.append(num);
        buffer_.append(indent_);
        buffer_.append(desc);
        buffer_.append("\n");
        writeToFile();
    }

    /**
     * Write the given data into the file. <br>
     * For each row, the format would be: <tt>simulation time, num, desc</tt>.
     * The <tt>simulation time</tt> is automatically handle inside this method.
     * @param   num     decimal number
     * @param   desc    the description of this number
     * @pre $none
     * @post $none
     */
    public void write(double num, String desc)
    {
        buffer_.append( GridSim.clock() );
        buffer_.append(indent_);
        buffer_.append(num);
        buffer_.append(indent_);
        buffer_.append(desc);
        buffer_.append("\n");
        writeToFile();
    }

    /**
     * Write the given data into the file. <br>
     * For each row, the format would be: <tt>simulation time, num, desc</tt>.
     * The <tt>simulation time</tt> is automatically handle inside this method.
     * @param   num     long number
     * @param   desc    the description of this number
     * @pre $none
     * @post $none
     */
    public void write(long num, String desc)
    {
        buffer_.append( GridSim.clock() );
        buffer_.append(indent_);
        buffer_.append(num);
        buffer_.append(indent_);
        buffer_.append(desc);
        buffer_.append("\n");
        writeToFile();
    }

    /**
     * Write the given data into the file. <br>
     * For each row, the format would be: <tt>simulation time, desc</tt>.
     * The <tt>simulation time</tt> is automatically handle inside this method.
     * @param   data    data to be recorded
     * @pre $none
     * @post $none
     */
    public void write(String data)
    {
        buffer_.append( GridSim.clock() );
        buffer_.append(indent_);
        buffer_.append(data);
        buffer_.append("\n");
        writeToFile();
    }

    /**
     * If the string buffer reaches max. limit, then write all into
     * a file.
     * @pre $none
     * @post $none
     */
    private void writeToFile()
    {
        try
        {
            int len = buffer_.length();
            if (len >= SIZE)
            {
                // write into a file
                outBuffer_.write( buffer_.toString().getBytes() );

                // delete all the existing strings
                buffer_.delete(0, len);
            }
        }
        catch(Exception e)
        {
            System.out.println(name_ + ".writeToFile(): Error - " +
                "unable to write into a file.");
        }

    }

    /**
     * Finalize the recording by writing all the previously given
     * information into a file. <br>
     * Forgotten to call this method will result in not writing
     * into a file at all.
     * @pre $none
     * @post $none
     */
    public void finalWrite()
    {
        try
        {
            outBuffer_.write( buffer_.toString().getBytes() );
            outBuffer_.close();
        }
        catch(Exception e)
        {
            System.out.println(name_ + ".finalWrite(): Error - " +
                "unable to write and to close a file sucessfully.");
        }
    }

} 

