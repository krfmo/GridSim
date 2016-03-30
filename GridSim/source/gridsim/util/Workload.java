/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2004, The University of Melbourne, Australia
 */

package gridsim.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;

import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.IO_data;
import gridsim.ParameterException;
import gridsim.net.InfoPacket;
import gridsim.net.Link;
import gridsim.net.SimpleLink;

/**
 * The main purpose of this class is to create a realistic simulation
 * environment where your jobs or Gridlets are competing with others.
 * In other words, the grid resource might not be available at certain times.
 * In addition, the arrival time of jobs are also captured in the trace file.
 * <p>
 * This class is responsible for reading resource traces from a file and
 * sends Gridlets to only <tt>one</tt> destinated resource. <br>
 * <b>NOTE:</b>
 * <ul>
 *      <li> This class can only take <tt>one</tt> trace file of the following
 *           format: <i>ASCII text, zip, gz.</i>
 *      <li> This class can be classified as <b>one grid user entity</b>.
 *           Hence, you need to incorporate this entity into <tt>numUser</tt>
 *           during {@link gridsim.GridSim#init(int, Calendar, boolean)}
 *      <li> If you need to use multiple trace files to submit Gridlets to
 *           same or different resources, then you need to create multiple
 *           instances of this class <tt>each with a unique entity name</tt>.
 *      <li> If size of the trace file is huge or contains lots of traces
 *           please increase the JVM heap size accordingly by using
 *           <tt>java -Xmx</tt> option when running the simulation.
 *      <li> If you are running an experiment using the network extension,
 *           i.e. the gridsim.net package, then you need to use
 *           {@link #Workload(String, double, double, int, String, String, int)}
 *           instead.
 *      <li> The default job file size for sending to and receiving from
 *           a resource is {@link gridsim.net.Link#DEFAULT_MTU}.
 *           However, you can specify
 *           the file size by using {@link #setGridletFileSize(int)}.
 *      <li> A job run time is only for 1 PE <tt>not</tt> the total number of
 *           allocated PEs.
 *           Therefore, a Gridlet length is also calculated for 1 PE.<br>
 *           For example, job #1 in the trace has a run time of 100 seconds
 *           for 2 processors. This means each processor runs
 *           job #1 for 100 seconds, if the processors have the same
 *           specification.
 * </ul>
 * <p>
 * By default, this class follows the standard workload format as specified
 * in <a href="http://www.cs.huji.ac.il/labs/parallel/workload/">
 * http://www.cs.huji.ac.il/labs/parallel/workload/</a> <br>
 * However, you can use other format by calling the below methods before
 * running the simulation:
 * <ul>
 *      <li> {@link #setComment(String)}
 *      <li> {@link #setField(int, int, int, int, int)}
 * </ul>
 *
 * @see gridsim.GridSim#init(int, Calendar, boolean)
 * @author   Anthony Sulistio
 * @since    GridSim Toolkit 3.1
 * @invariant $none
 */
public class Workload extends GridSim
{
    private String fileName_;   // file name
    private String resName_;    // resource name
    private int resID_;         // resource ID
    private int rating_;        // a PE rating
    private int gridletID_;     // gridletID
    private int size_;          // job size for sending it through a network
    private ArrayList<Gridlet> list_;    // a list for getting all the Gridlets

    // constant
    private int JOB_NUM;        // job number
    private int SUBMIT_TIME;    // submit time of a Gridlet
    private int RUN_TIME;       // running time of a Gridlet
    private int NUM_PROC;       // number of processors needed for a Gridlet
    private int REQ_NUM_PROC;   // required number of processors
    private int REQ_RUN_TIME;   // required running time
    private int MAX_FIELD;      // max number of field in the trace file
    private String COMMENT;     // a string that denotes the start of a comment
    private static final int IRRELEVANT = -1;  // irrelevant number
    private static final int INTERVAL = 10;    // number of intervals
    private String[] fieldArray_;       // a temp array storing all the fields


    /**
     * Create a new Workload object <b>without</b> using the network extension.
     * This means this entity directly sends Gridlets to a destinated resource
     * without going through a wired network. <br>
     * <tt>NOTE:</tt>
     * You can not use this constructor in an experiment that uses a wired
     * network topology.
     *
     * @param name      this entity name
     * @param fileName  the workload trace filename in one of the following
     *                  format: <i>ASCII text, zip, gz.</i>
     * @param resourceName  the resource name
     * @param rating    the resource's PE rating
     * @throws Exception  This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @throws ParameterException   This happens for the following conditions:
     *      <ul>
     *          <li>the entity name is null or empty
     *          <li>the workload trace file name is null or empty
     *          <li>the resource entity name is null or empty
     *          <li>the resource PE rating <= 0
     *      </ul>
     * @pre name != null
     * @pre fileName != null
     * @pre resourceName != null
     * @pre rating > 0
     * @post $none
     */
    public Workload(String name, String fileName, String resourceName,
                    int rating) throws ParameterException, Exception
    {
        super(name, GridSimTags.DEFAULT_BAUD_RATE);

        // check the input parameters first
        String msg = name + "(): Error - ";
        if (fileName == null || fileName.length() == 0) {
            throw new ParameterException(msg + "invalid trace file name.");
        }
        else if (resourceName == null || resourceName.length() == 0) {
            throw new ParameterException(msg + "invalid resource name.");
        }
        else if (rating <= 0) {
            throw new ParameterException(msg+"resource PE rating must be > 0.");
        }

        System.out.println(name + ": Creating a workload object ...");
        init(fileName, resourceName, rating);
    }

    /**
     * Create a new Workload object <b>with</b> the network extension.
     * This means this entity directly sends Gridlets to a destinated resource
     * through a link. The link is automatically created by this constructor.
     *
     * @param name      this entity name
     * @param baudRate  baud rate of this link (bits/s)
     * @param propDelay Propagation delay of the Link in milli seconds
     * @param MTU       Maximum Transmission Unit of the Link in bytes.
     *                  Packets which are larger than the MTU should be split
     *                  up into MTU size units.
     *                  For example, a 1024 byte packet trying to cross a 576
     *                  byte MTU link should get split into 2 packets of 576
     *                  bytes and 448 bytes.
     * @param fileName  the workload trace filename in one of the following
     *                  format: <i>ASCII text, zip, gz.</i>
     * @param resourceName  the resource name
     * @param rating    the resource's PE rating
     * @throws Exception  This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @throws ParameterException   This happens for the following conditions:
     *      <ul>
     *          <li>the entity name is null or empty
     *          <li> baudRate <= 0
     *          <li> propDelay <= 0
     *          <li> MTU <= 0
     *          <li>the workload trace file name is null or empty
     *          <li>the resource entity name is null or empty
     *          <li>the resource PE rating <= 0
     *      </ul>
     * @pre name != null
     * @pre baudRate > 0
     * @pre propDelay > 0
     * @pre MTU > 0
     * @pre fileName != null
     * @pre resourceName != null
     * @pre rating > 0
     * @post $none
     */
    public Workload(String name, double baudRate, double propDelay, int MTU,
                    String fileName, String resourceName, int rating)
                    throws ParameterException, Exception
    {
        super( name, new SimpleLink(name+"_link", baudRate, propDelay, MTU) );

        // check the input parameters first
        String msg = name + "(): Error - ";
        if (fileName == null || fileName.length() == 0) {
            throw new ParameterException(msg + "invalid trace file name.");
        }
        else if (resourceName == null || resourceName.length() == 0) {
            throw new ParameterException(msg + "invalid resource name.");
        }
        else if (rating <= 0) {
            throw new ParameterException(msg+"resource PE rating must be > 0.");
        }

        System.out.println(name + ": Creating a workload object ...");
        init(fileName, resourceName, rating);
    }

    /**
     * Create a new Workload object <b>with</b> the network extension.
     * This means this entity directly sends Gridlets to a destinated resource
     * through a link. The link is automatically created by this constructor.
     *
     * @param name      this entity name
     * @param link      the link that will be used to connect this Workload
     *                  to another entity or a Router.
     * @param fileName  the workload trace filename in one of the following
     *                  format: <i>ASCII text, zip, gz.</i>
     * @param resourceName  the resource name
     * @param rating    the resource's PE rating
     * @throws Exception  This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @throws ParameterException   This happens for the following conditions:
     *      <ul>
     *          <li>the entity name is null or empty
     *          <li>the link is empty
     *          <li>the workload trace file name is null or empty
     *          <li>the resource entity name is null or empty
     *          <li>the resource PE rating <= 0
     *      </ul>
     * @pre name != null
     * @pre link != null
     * @pre fileName != null
     * @pre resourceName != null
     * @pre rating > 0
     * @post $none
     */
    public Workload(String name, Link link, String fileName,
                    String resourceName, int rating)
                    throws ParameterException, Exception
    {
        super(name, link);

        // check the input parameters first
        String msg = name + "(): Error - ";
        if (fileName == null || fileName.length() == 0) {
            throw new ParameterException(msg + "invalid trace file name.");
        }
        else if (resourceName == null || resourceName.length() == 0) {
            throw new ParameterException(msg + "invalid resource name.");
        }
        else if (rating <= 0) {
            throw new ParameterException(msg+"resource PE rating must be > 0.");
        }

        System.out.println(name + ": Creating a workload object ...");
        init(fileName, resourceName, rating);
    }

    /**
     * Initialises all the attributes
     * @param   fileName    trace file name
     * @param   resourceName    resource entity name
     * @param   rating      resource PE rating
     * @pre $none
     * @post $none
     */
    private void init(String fileName, String resourceName, int rating)
    {
        fileName_ = fileName;
        resName_ = resourceName;
        resID_ = GridSim.getEntityId(resName_);
        rating_ = rating;
        gridletID_ = 1;   // starts at 1 to make it the same as in a trace file
        list_ = null;
        size_ = Link.DEFAULT_MTU;

        // if using Standard Workload Format -- don't forget to substract by 1
        // since an array starts at 0, but the field in a trace starts at 1
        JOB_NUM = 1 - 1;
        SUBMIT_TIME = 2 - 1;
        RUN_TIME = 4 - 1;
        NUM_PROC = 5 - 1;
        REQ_NUM_PROC = 8 - 1;
        REQ_RUN_TIME = 9 - 1;

        COMMENT = ";";      // semicolon means the start of a comment
        MAX_FIELD = 18;     // standard workload format has 18 fields
        fieldArray_ = null;
    }

    /**
     * Sets a Gridlet file size (in byte) for sending to/from a resource.
     * @param size  a Gridlet file size (in byte)
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre size > 0
     * @post $none
     */
    public boolean setGridletFileSize(int size)
    {
        if (size < 0) {
            return false;
        }

        size_ = size;
        return true;
    }

    /**
     * Identifies the start of a comment line. Hence, a line that starts
     * with a given comment will be ignored.
     * @param comment  a character that denotes the start of a comment,
     *                 e.g. ";" or "#"
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre comment != null
     * @post $none
     */
    public boolean setComment(String comment)
    {
        boolean success = false;
        if (comment != null && comment.length() > 0)
        {
            COMMENT = comment;
            success = true;
        }
        return success;
    }

    /**
     * Tells this class what to look in the trace file.
     * This method should be called before the start of the simulation.
     * <p>
     * By default, this class follows the standard workload format as specified
     * in <a href="http://www.cs.huji.ac.il/labs/parallel/workload/">
     * http://www.cs.huji.ac.il/labs/parallel/workload/</a> <br>
     * However, you can use other format by calling this method.
     * <p>
     * The parameters must be a positive integer number starting from 1.
     * A special case is where <tt>jobNum == -1</tt>, meaning the job or
     * gridlet ID starts at 1.
     *
     * @param maxField  max. number of field/column in one row
     * @param jobNum    field/column number for locating the job ID
     * @param submitTime   field/column number for locating the job submit time
     * @param runTime   field/column number for locating the job run time
     * @param numProc   field/column number for locating the number of PEs
     *                  required to run a job
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre maxField > 0
     * @pre submitTime > 0
     * @pre runTime > 0
     * @pre numProc > 0
     * @post $none
     */
    public boolean setField(int maxField, int jobNum, int submitTime,
                            int runTime, int numProc)
    {
        // need to substract by 1 since array starts at 0. Need to convert,
        // position in a field into the index of the array
        if (jobNum > 0) {
            JOB_NUM = jobNum - 1;
        }
        else if (jobNum == 0)
        {
            System.out.println(super.get_name() +
                    ".setField(): Invalid job number field.");
            return false;
        }
        else {
            JOB_NUM = -1;
        }

        // get the max. number of field
        if (maxField > 0) {
            MAX_FIELD = maxField;
        }
        else
        {
            System.out.println(super.get_name() +
                    ".setField(): Invalid max. number of field.");
            return false;
        }

        // get the submit time field
        if (submitTime > 0) {
            SUBMIT_TIME = submitTime - 1;
        }
        else
        {
            System.out.println(super.get_name() +
                    ".setField(): Invalid submit time field.");
            return false;
        }

        // get the run time field
        if (runTime > 0) {
            REQ_RUN_TIME = runTime - 1;
        }
        else
        {
            System.out.println(super.get_name() +
                    ".setField(): Invalid run time field.");
            return false;
        }

        // get the number of processors field
        if (numProc > 0) {
            REQ_NUM_PROC = numProc - 1;
        }
        else
        {
            System.out.println(super.get_name() +
                    ".setField(): Invalid number of processors field.");
            return false;
        }

        return true;
    }

    /**
     * Gets a list of completed Gridlets
     * @return a list of Gridlets
     * @pre $none
     * @post $none
     */
    public ArrayList<Gridlet> getGridletList() {
        return list_;
    }

    /**
     * Prints the Gridlet objects
     * @param history   <tt>true</tt> means printing each Gridlet's history,
     *                  <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public void printGridletList(boolean history)
    {
        String name = super.get_name();
        int size = list_.size();
        Gridlet gridlet = null;

        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT for " + name + " ==========");
        System.out.println("Gridlet_ID" + indent + "STATUS" + indent +
                "Resource_ID" + indent + "Cost");

        int i = 0;
        for (i = 0; i < size; i++)
        {
            gridlet = list_.get(i);
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            // get the status of a Gridlet
            System.out.print( gridlet.getGridletStatusString() );
            System.out.println( indent + indent + gridlet.getResourceID() +
                    indent + indent + gridlet.getProcessingCost() );
        }

        System.out.println();
        if (history)
        {
            // a loop to print each Gridlet's history
            System.out.println();
            for (i = 0; i < size; i++)
            {
                gridlet = list_.get(i);
                System.out.println( gridlet.getGridletHistory() );

                System.out.print("Gridlet #" + gridlet.getGridletID() );
                System.out.println(", length = " + gridlet.getGridletLength()
                        + ", finished so far = "
                        + gridlet.getGridletFinishedSoFar() );
                System.out.println("=========================================");
                System.out.println();
            }
        }
    }

    /**
     * Reads from a given file when the simulation starts running.
     * Then submits Gridlets to a resource and collects them before exiting.
     * To collect the completed Gridlets, use {@link #getGridletList()}
     * @pre $none
     * @post $none
     */
    public void body()
    {
        System.out.println();
        System.out.println(super.get_name() + ".body() :%%%% Start ...");

        // create a temp array
        fieldArray_ = new String[MAX_FIELD];

        // get the resource id
        if (resID_ < 0)
        {
            System.out.println(super.get_name() +
                    ".body(): Error - invalid resource name: " + resName_);
            return;
        }

        boolean success = false;

        // read the gz file
        if (fileName_.endsWith(".gz")) {
            success = readGZIPFile(fileName_);
        }
        // read the zip file
        else if (fileName_.endsWith(".zip")) {
            success = readZipFile(fileName_);
        }
        // read from uncompressed file as well
        else {
            success = readFile(fileName_);
        }

        // if all the gridlets have been submitted
        if (success == true) {
            collectGridlet();
        }
        else
        {
            System.out.println(super.get_name() +
                    ".body(): Error - unable to parse from a file.");
        }

        // shut down all the entities, including GridStatistics entity since
        // we used it to record certain events.
        shutdownGridStatisticsEntity();
        shutdownUserEntity();
        terminateIOEntities();

        System.out.println(super.get_name() + ".body() : %%%% Exit ...");
    }

    //////////////////////// PRIVATE METHODS ///////////////////////

    /**
     * Collects Gridlets sent and stores them into a list.
     * @pre $none
     * @post $none
     */
    private void collectGridlet()
    {
        System.out.println(super.get_name() + ": Collecting Gridlets ...");
        list_ = new ArrayList(gridletID_ + 1);

        Object data = null;
        Gridlet gl = null;

        int counter = 1;    // starts at 1, since gridletID_ starts at 1 too
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);     // get the next available event
            data = ev.get_data();       // get the event's data

            // handle ping request
            if (ev.get_tag() == GridSimTags.INFOPKT_SUBMIT)
            {
                processPingRequest(ev);
                continue;
            }

            // get the Gridlet data
            if (data != null && data instanceof Gridlet)
            {
                gl = (Gridlet) data;
                list_.add(gl);
                counter++;
            }

            // if all the Gridlets have been collected
            if (counter == gridletID_) {
                break;
            }
        }
    }

    /**
     * Processes a ping request.
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processPingRequest(Sim_event ev)
    {
        InfoPacket pkt = (InfoPacket) ev.get_data();
        pkt.setTag(GridSimTags.INFOPKT_RETURN);
        pkt.setDestID( pkt.getSrcID() );

        // sends back to the sender
        super.send(super.output, GridSimTags.SCHEDULE_NOW,
                   GridSimTags.INFOPKT_RETURN,
                   new IO_data(pkt, pkt.getSize(), pkt.getSrcID()) );
    }

    /**
     * Breaks a line of string into many fields.
     * @param line  a line of string
     * @param lineNum   a line number
     * @pre line != null
     * @pre lineNum > 0
     * @post $none
     */
    private void parseValue(String line, int lineNum)
    {
        // skip a comment line
        if (line.startsWith(COMMENT)) {
            return;
        }

        String[] sp = line.split("\\s+");  // split the fields based on a space
        int i;              // a counter
        int len = 0;        // length of a string
        int index = 0;      // the index of an array

        // check for each field in the array
        for (i = 0; i < sp.length; i++)
        {
            len = sp[i].length();  // get the length of a string

            // if it is empty then ignore
            if (len == 0) {
                continue;
            }
            // if not, then put into the array
            else
            {
                fieldArray_[index] = sp[i];
                index++;
            }
        }

        if (index == MAX_FIELD) {
            extractField(fieldArray_, lineNum);
        }
    }

    /**
     * Extracts relevant information from a given array
     * @param array  an array of String
     * @param line   a line number
     * @pre array != null
     * @pre line > 0
     */
    private void extractField(String[] array, int line)
    {
        try
        {
            Integer obj = null;

            // get the job number
            int id = 0;
            if (JOB_NUM == IRRELEVANT) {
                id = gridletID_;
            }
            else
            {
                obj = new Integer( array[JOB_NUM].trim() );
                id = obj.intValue();
            }

            // get the submit time
            Long l = new Long( array[SUBMIT_TIME].trim() );
            long submitTime = l.intValue();

            // get the run time
            obj = new Integer( array[REQ_RUN_TIME].trim() );
            int runTime = obj.intValue();

            // if the required run time field is ignored, then use
            // the actual run time
            if (runTime == IRRELEVANT)
            {
                obj = new Integer( array[RUN_TIME].trim() );
                runTime = obj.intValue();
            }

            // according to the SWF manual, runtime of 0 is possible due
            // to rounding down. E.g. runtime is 0.4 seconds -> runtime = 0
            if (runTime == 0) {
                runTime = 1;    // change to 1 second
            }

            // get the number of allocated processors
            obj = new Integer( array[REQ_NUM_PROC].trim() );
            int numProc = obj.intValue();

            // if the required num of allocated processors field is ignored
            // or zero, then use the actual field
            if (numProc == IRRELEVANT || numProc == 0)
            {
                obj = new Integer( array[NUM_PROC].trim() );
                numProc = obj.intValue();
            }

            // finally, check if the num of PEs required is valid or not
            if (numProc <= 0)
            {
                System.out.println(super.get_name() + ": Warning - job #"
                        + id + " at line " + line + " requires " + numProc
                        + " CPU. Change to 1 CPU.");
                numProc = 1;
            }

            // submit a Gridlet
            submitGridlet(id, submitTime, runTime, numProc);
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                    ": Exception in reading file at line #" + line + 
                    ", exception: " + e.getMessage());
        }
    }

    /**
     * Creates a Gridlet with the given information, then submit it to a
     * resource
     * @param id  a Gridlet ID
     * @param submitTime  Gridlet's submit time
     * @param runTime     Gridlet's run time
     * @param numProc     number of processors
     * @pre id >= 0
     * @pre submitTime >= 0
     * @pre runTime >= 0
     * @pre numProc > 0
     * @post $none
     */
    private void submitGridlet(int id, long submitTime, int runTime, int numProc)
    {
        // create the gridlet
        int len = runTime * rating_;      // calculate a job length for each PE
        Gridlet gl = new Gridlet(id, len, size_, size_, GridSim.isTraceEnabled());
        gl.setUserID( super.get_id() );   // set the owner ID
        gl.setNumPE(numProc);             // set the requested num of proc

        // printing to inform user
        if (gridletID_ == 1 || gridletID_ % INTERVAL == 0)
        {
            System.out.println(super.get_name() + ": Submitting Gridlets to " +
                    resName_ + " ...");
        }

        // check the submit time
        if (submitTime < 0) {
            submitTime = 0;
        }

        gridletID_++;   // increment the counter

        // submit a gridlet to resource
        super.send(super.output, submitTime, GridSimTags.GRIDLET_SUBMIT,
                new IO_data(gl, gl.getGridletFileSize(), resID_) );
    }

    /**
     * Reads a text file one line at the time
     * @param fileName   a file name
     * @return <tt>true</tt> if reading a file is successful, <tt>false</tt>
     *         otherwise.
     * @pre fileName != null
     * @post $none
     */
    private boolean readFile(String fileName)
    {
        boolean success = false;
        BufferedReader reader = null;
        try
        {
            FileInputStream file = new FileInputStream(fileName);
            reader = new BufferedReader(new InputStreamReader(file));

            // read one line at the time
            int line = 1;
            while ( reader.ready() )
            {
                parseValue(reader.readLine(), line);
                line++;
            }

            reader.close();    // close the file
            success = true;
        }
        catch (FileNotFoundException f)
        {
            System.out.println(super.get_name() +
                    ": Error - the file was not found: " + f.getMessage());
        }
        catch (IOException e)
        {
            System.out.println(super.get_name() +
                    ": Error - an IOException occurred: " + e.getMessage());
        }
        finally
        {
            if (reader != null)
            {
                try {
                    reader.close();    // close the file
                }
                catch (IOException e)
                {
                    System.out.println(super.get_name() +
                        ": Error - an IOException occurred: " + e.getMessage());
                }
            }
        }

        return success;
    }

    /**
     * Reads a gzip file one line at the time
     * @param fileName   a gzip file name
     * @return <tt>true</tt> if reading a file is successful, <tt>false</tt>
     *         otherwise.
     * @pre fileName != null
     * @post $none
     */
    private boolean readGZIPFile(String fileName)
    {
        boolean success = false;
        BufferedReader reader = null;
        try
        {
            FileInputStream file = new FileInputStream(fileName);
            GZIPInputStream gz =  new GZIPInputStream(file);
            reader = new BufferedReader(new InputStreamReader(gz));

            // read one line at the time
            int line = 1;
            while ( reader.ready() )
            {
                parseValue(reader.readLine(), line);
                line++;
            }

            reader.close();   // close the file
            success = true;
        }
        catch (FileNotFoundException f)
        {
            System.out.println(super.get_name() +
                    ": Error - the file was not found: " + f.getMessage());
        }
        catch (IOException e)
        {
            System.out.println(super.get_name() +
                    ": Error - an IOException occurred: " + e.getMessage());
        }
        finally
        {
            if (reader != null)
            {
                try {
                    reader.close();    // close the file
                }
                catch (IOException e)
                {
                    System.out.println(super.get_name() +
                        ": Error - an IOException occurred: " + e.getMessage());
                }
            }
        }

        return success;
    }

    /**
     * Reads a Zip file. Iterating through each entry and reading it one line
     * at the time.
     * @param fileName   a zip file name
     * @return <tt>true</tt> if reading a file is successful, <tt>false</tt>
     *         otherwise.
     * @pre fileName != null
     * @post $none
     */
    private boolean readZipFile(String fileName)
    {
        boolean success = false;
        ZipFile zipFile = null;
        try
        {
            BufferedReader reader = null;

            // ZipFile offers an Enumeration of all the files in the Zip file
            zipFile = new ZipFile(fileName);
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            while (e.hasMoreElements()) {
                success = false;    // reset the value again
                ZipEntry zipEntry = e.nextElement();

                reader = new BufferedReader(
                		new InputStreamReader(zipFile.getInputStream(zipEntry)));

                // read one line at the time
                int line = 1;
                while ( reader.ready() )
                {
                    parseValue(reader.readLine(), line);
                    line++;
                }

                reader.close();   // close the file
                success = true;
            }
        }
        catch (IOException e)
        {
            System.out.println(super.get_name() +
                    ": Error - an IOException occurred: " + e.getMessage());
        }
        finally
        {
            if (zipFile != null)
            {
                try {
                    zipFile.close();    // close the file
                }
                catch (IOException e)
                {
                    System.out.println(super.get_name() +
                        ": Error - an IOException occurred: " + e.getMessage());
                }
            }
        }

        return success;
    }
} 

