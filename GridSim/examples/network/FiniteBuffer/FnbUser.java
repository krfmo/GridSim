package network.FiniteBuffer;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Authors: Agustin Caminero and Anthony Sulistio
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */


import gridsim.*;
import gridsim.net.Link;
import java.util.Random;
import java.util.ArrayList;
import java.io.*;
import eduni.simjava.Sim_system;
import eduni.simjava.Sim_event;
import gridsim.net.fnb.*;


/**
 * An example of how to use the new functionality on finite buffers.
 * The functionality of this class, implemented in the body() method, is as follows.
 * 1. create n number of gridlets and a new csv file for statistics purposes
 * 2. send a reminder to itself to send the gridlets
 * 3. after receving a reminder at a later time, proceeds to
 *    the processGridletSubmission() method, where
 *    - it submits all gridlets in the list to a particular resource
 *    - it sets the gridlet submission time and status
 *
 * 4. after submitting all gridlets, then wait for incoming events
 *    - if a new event is regarding to receiving a Gridlet
 *      + remove this gridlet from the submission list
 *      + sets the gridlet latency time and status
 *
 *    - if a new event is regarding to a gridlet being dropped in the network
 *      + remove this gridlet from the submission list
 *      + sets the gridlet latency time and status
 *      + marks this gridlet as being dropped by the router
 *
 * 5. after all gridlets have been received, then exit the body() method and
 *    prints the statistics into a csv file.
 *
 * @author Agustin Caminero and Anthony Sulistio
 */
public class FnbUser extends GridUser
{
    // list of submitted Gridlets (array of GridletSubmission)
    private ArrayList GridletSubmittedList_;

    // counts how many gridlets have already comeback after being successfully
    // executed
    private int receivedGridletCounter;
    private int ToS_;           // type of service for differentiated network QoS
    private int NUM_GRIDLETS;

    // for a user whose name is "User_0", its myID_ will be 0, and so on.
    // this ID is set at the beginning or when you create an object of this class
    private int myID_;

    // The sizes of gridlets
    private long gridletLength;     // denotes computational time
    private long gridletInput;      // denotes input file size for each gridlet
    private long gridletOutput;     // denotes output file size for each gridlet

    // we store the time when each gridlet is submitted
    private double gridletSubmissionTime[];

    // we store the most recent status of each gridlet during execution
    private String gridletStatus[];

    // we store the total execution time, i.e.
    // time of receiving - time of submission
    double gridletLatencyTime[];

    // a counter to keep the number of gridlets which are failed because of
    // dropped packets
    int droppedGridletsCounter;

    // a list of the failed gridlets (those which failed because of
    // dropped packets)
    boolean droppedGridletsArray[];

    double init_time; // the time when this user will start submitting gridlets
    int resID;        // the id of the resource to which this user will send his gl



    /**
     * Creates a FnbUser object
     * @param name  this entity name
     * @param link  a network link connecting this entity
     * @param myID  a user ID
     * @param glLength  length (MI) for the gridlets of this user
     * @param glIn      input file size for the gridlets of this user
     * @param glOut     output file size for the gridlets of this user
     * @param init_time the time when this user will start submitting gridlets
     * @param resID     resource ID for sending the user's Gridlets
     * @throws java.lang.Exception happens if either name or link is empty
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public FnbUser(String name, Link link, int myID, long glLength, long glIn,
                   long glOut, double init_time, int resID) throws Exception
    {
        super(name, link);

        this.GridletSubmittedList_ = new ArrayList();
        receivedGridletCounter = 0;
        droppedGridletsCounter = 0;
        ToS_ = 0;
        this.myID_ = myID;

        gridletLength = glLength;
        gridletInput = glIn;
        gridletOutput = glOut;

        this.init_time = init_time;
        this.resID = resID;
    }


    /**
     * Creates a FnbUser object
     * @param name  this entity name
     * @param link  a network link connecting this entity
     * @param regionalGIS   a regional GridInformationService (GIS) entity name
     * @param myID          a user ID
     * @throws java.lang.Exception happens if one of the inputs is empty
     * @pre name != null
     * @pre link != null
     * @pre regionalGIS != null
     * @post $none
     */
    public FnbUser(String name, Link link, String regionalGIS, int myID)
                   throws Exception
    {
        super(name, link, regionalGIS);

        this.GridletSubmittedList_ = new ArrayList();
        receivedGridletCounter = 0;
        droppedGridletsCounter = 0;
        ToS_ = 0;
        this.myID_ = myID;

        gridletLength = 0;
        gridletInput = 0;
        gridletOutput = 0;

        this.init_time = 0;
        this.resID = 0;
    }


    /**
     * Sets the number of gridlets for submission.
     * Also, creates 2 arrays for storing submission and receiving time of
     * each Gridlet
     * @param gridlet_num   number of gridlets
     */
    public void setGridletNumber(int gridlet_num)
    {
        NUM_GRIDLETS = gridlet_num;
        gridletSubmissionTime = new double[NUM_GRIDLETS];
        gridletLatencyTime = new double[NUM_GRIDLETS];
        gridletStatus = new String[NUM_GRIDLETS];

        // initialise whether each Gridlet has been dropped in the network or not
        droppedGridletsArray = new boolean[NUM_GRIDLETS];
        for (int i = 0; i < NUM_GRIDLETS; i++)
        {
            droppedGridletsArray[i] = false; // hasn't been dropped in the network
            gridletStatus[i] = "Created";
        }
    }


    /**
     * Sets the Type of Service (ToS) that this packet receives in the network
     * @param ToS   Type of Service
     */
    public void setNetServiceLevel(int ToS) {
        this.ToS_ = ToS;
    }


    /**
    * Handles incoming requests to this entity.
    * @pre $none
    * @post $none
    */
    public void body()
    {
        initializeResultsFile();        // create new files for statistics
        createGridlet(NUM_GRIDLETS);    // create gridlets

        // send a reminder to itself at time init_time, with a message to
        // submit Gridlets.
        super.send(super.get_id(), GridSimTags.SCHEDULE_NOW + init_time * 10,
                   GridSimTags.GRIDLET_SUBMIT);

        /*****
        // Uncomment this if you want more info on the progress of the sims
        System.out.println(super.get_name() +
                ": initial SUBMIT_GRIDLET event will be at clock: " +
                init_time + ". Current clock: " + GridSim.clock());
        ******/


        // a loop that keeps waiting for incoming events
        while (Sim_system.running())
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev); // get the next event in the incoming queue

            // exit the loop if we get a signal of end of simulation
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                System.out.println("============== " + super.get_name() +
                        ". Ending simulation...");
                break;
            }

            // for other events or activities
            switch (ev.get_tag())
            {
                // submit a gridlet
                case GridSimTags.GRIDLET_SUBMIT:    //SUBMIT_GRIDLET:
                    /***
                    // Uncomment this if you want more info on the progress
                    System.out.println(super.get_name() +
                        ": received an SUBMIT_GRIDLET event. Clock: " + GridSim.clock());
                    ****/
                    processGridletSubmission(ev); // process the received event
                break;

                // Receive a gridlet back from the resource
                case GridSimTags.GRIDLET_RETURN:
                    /*****
                    // Uncomment this if you want more info on the progress
                    System.out.println(super.get_name() +
                        ": received an GRIDLET_RETURN event. Clock: " + GridSim.clock());
                    ****/
                    processGridletReturn(ev);
                break;

                // A gridlet has failed because a packet was dropped
                case GridSimTags.FNB_GRIDLET_FAILED_BECAUSE_PACKET_DROPPED:
                    processGridletPacketDropping(ev);
                break;

                default:
                    System.out.println(super.get_name() +
                            ": Received an unknown event: " + ev.get_tag());
                break;

            } // switch (ev.get_tag())

            ev = null;

        }//  while (Sim_system.running())

        // remove I/O entities created during construction of this entity
        super.terminateIOEntities();

        // print the statistics regarding to this experiment
        printStatistics();

    }// body()


    /**
     * This functions process the fail of a gridlet. The failure has happened
     * because a packet was dropped.
     * @param ev    an incoming event
     */
    private void processGridletPacketDropping(Sim_event ev)
    {
        FnbMessageDropGridlet msgDropGl = (FnbMessageDropGridlet) ev.get_data();
        int glID = msgDropGl.getEntityID();
        double clock = GridSim.clock();

        if (droppedGridletsArray[glID] == false)
        {
            System.out.println("<<< " + super.get_name() +
                ": Receiving GRIDLET_FAILED_BECAUSE_PACKET_DROPPED at time = " +
                clock + ". Gridlet # " + glID);

            // Commented. Write to a single file instead of a different one
            //write(super.get_name() + ", " + glID + ", GRIDLET_DROPPED, " + clock + "\n",
            //      super.get_name() + "_DroppedGridlets.csv");

            // find out the total execution time, from the submission time
            gridletLatencyTime[glID] = GridSim.clock() - gridletSubmissionTime[glID];

            // get the latest status of a gridlet
            gridletStatus[glID] = "Dropped by router";

            // set the status of this gridlet ID
            droppedGridletsArray[glID] = true;  // we have a dropped Gridlet
            droppedGridletsCounter++;           // increment the counter

            // remove this gridlet from the submission list
            // we don't re-send this gridlet
            removeGridletFromGridletSubmittedList(glID);

            // In this case, the user has finished his work, as all the gridlets
            // are back or failed. So, finish the simulation.
            if (receivedGridletCounter + droppedGridletsCounter == NUM_GRIDLETS)
            {
                super.finishSimulation();
                //System.out.println("**** " + super.get_name() + ": FINISHED!!");
            }

            System.out.println("<<< " + super.get_name() +
                    ": Finished processing GRIDLET_FAILED_BECAUSE_PACKET_DROPPED\nfor Gridlet # " +
                    glID + ". Received: " + receivedGridletCounter +
                    ". Dropped: " + droppedGridletsCounter + 
                    ". NUM_GRIDLETS: " + NUM_GRIDLETS);

        } //if (droppedGridletsArray[glID] == false)

        ev = null;
    }


    /**
     * This function remove a gridlet from the submission list.
     * @param glID the id of the gridlet to be removed
     */
    private void removeGridletFromGridletSubmittedList(int glID)
    {
        int i = 0;
        while (i < GridletSubmittedList_.size())
        {
            GridletSubmission obj = (GridletSubmission) GridletSubmittedList_.get(i);
            int id = obj.getGridletID();

            // if found the maching Gridlet
            if (glID == id)
            {
                GridletSubmittedList_.remove(i);
                break;
            }

            i++;
        }
    }


    /**
     * This method submits all the gridlets that have not been submitted before
     * @param ev an incoming event
     */
    private void processGridletSubmission(Sim_event ev)
    {
        int i = 0;
        GridletSubmission gridletSub;
        Gridlet gl;

        while (i < GridletSubmittedList_.size())
        {
            gridletSub = (GridletSubmission) GridletSubmittedList_.get(i);

            // Submit the gridlets have not been submitted before
            if (gridletSub.getSubmitted() == false)
            {
                // we have to resubmit this gridlet
                gl = gridletSub.getGridlet();

                System.out.println(">>> " + super.get_name() +
                    ". Sending Gridlet #" + i + " to " +
                    GridSim.getEntityName(resID) + " at clock: " + GridSim.clock());

                // set the submission time
                gridletSubmissionTime[gl.getGridletID()] = GridSim.clock();
                gridletStatus[gl.getGridletID()] = "Submitted";

                // send the gridlet to a particular resource
                super.gridletSubmit(gl, resID, 0, false, ToS_);

                gridletSub.setSubmitted(true); // set this gridlet as submitted

                // Write into a file
                write(super.get_name(), "Sending", gl.getGridletID(),
                      GridSim.getEntityName(resID), gl.getGridletStatusString(),
                      GridSim.clock());

            } // if (gridletSub.getSubmitted() == false)

            i++;

        } // while (i < GridletSubmittedList_.size())

    }// processGridletSubmission


    /**
     * This functions process the return of a gridlet. We pay attention to
     * the status of the gridlet and then decide what we have to do the next
     * @param ev an incoming event
     */
    public void processGridletReturn(Sim_event ev)
    {
        Object obj = (Object) ev.get_data();
        Gridlet gl = null;

        int glID;

        if (obj instanceof Gridlet)
        {
            gl = (Gridlet) obj;

            glID = gl.getGridletID();
            System.out.println("<<< " + super.get_name() +
                ": Receiving Gridlet #" + glID + " with status " +
                gl.getGridletStatusString() + " at time = " + GridSim.clock() +
                " from " + GridSim.getEntityName(gl.getResourceID()) );

            // Write into a file
            write(super.get_name(), "Receiving", glID,
                  GridSim.getEntityName(gl.getResourceID()),
                  gl.getGridletStatusString(), GridSim.clock());

            receivedGridletCounter++;

            // find out the total execution time, from the submission time
            gridletLatencyTime[glID] = GridSim.clock() -
                        gridletSubmissionTime[glID];

            // get the latest status of a gridlet
            gridletStatus[glID] = gl.getGridletStatusString();

            // remove the gridlet
            removeGridletFromGridletSubmittedList(glID);
            gl = null;

            // We have received all the gridlets. So, finish the simulation.
            if ((receivedGridletCounter == NUM_GRIDLETS) ||
               (receivedGridletCounter + droppedGridletsCounter == NUM_GRIDLETS))
            {
                super.finishSimulation();
                //System.out.println("**** " + super.get_name() + ": FINISHED!!");
            }


            System.out.println("<<< " + super.get_name() +
                    ": Finished processing the return of Gridlet # " +
                    glID + "\nReceived: " + receivedGridletCounter +
                    ". Dropped: " + droppedGridletsCounter + 
                    ". NUM_GRIDLETS: " + NUM_GRIDLETS);


        } // if (obj instanceof Gridlet)
    }


    private void printStatistics()
    {
        String name = super.get_name();
        String filename = name + ".csv";
        StringBuffer str = new StringBuffer(1000);  // store 1000 chars

        // print the heading name first
        str.append("\n\ngridletID, status, time, latency time (time - submit time)\n");
        for (int i = 0; i < gridletStatus.length; i++)
        {
            double time = gridletLatencyTime[i] + gridletSubmissionTime[i];
            str.append(i); // gridlet ID
            str.append(", ");
            str.append(gridletStatus[i]);
            str.append(", "); // gridlet status
            str.append(time);
            str.append(", ");
            str.append(gridletLatencyTime[i]);
            str.append("\n");
        }

        this.write(str.toString(), filename);
    }


    /**
     * Prints the Gridlet objects
     * @param list the list of gridlets
     * @param name the name of the user
     * @param detail if we want the gridlet's history or not
     * @param gridletLatencyTime array containing the latencies of gridlets.
     * Latencies are from the moment when the gridlet is sent, till
     * the moment they are back at the user. They take into account the last submission of a gridlet
     * (when the gridlet is successfully run)
     */
    private void printGridletList(GridletList list, String name,
                                  boolean detail, double gridletLatencyTime[])
    {
        int size = list.size();
        Gridlet gridlet = null;

        String indent = "    ";
        System.out.println();
        System.out.println("============= OUTPUT for " + name + " ==========");
        System.out.println("Gridlet ID" + indent + "STATUS" + indent +
                "Resource ID" + indent + "Cost" + indent +
                "CPU Time"+ indent + "Latency");

        // a loop to print the overall result
        int i = 0;
        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);

            System.out.print(indent + gridlet.getGridletID() + indent + indent);
            System.out.print(gridlet.getGridletStatusString());
            System.out.println(indent + indent + gridlet.getResourceID() +
                indent + indent + gridlet.getProcessingCost() +
                indent + indent + gridlet.getActualCPUTime()+ indent + indent +
                gridletLatencyTime[gridlet.getGridletID()]);

            writeFin(name, gridlet.getGridletID(),
                GridSim.getEntityName(gridlet.getResourceID()),
                gridlet.getProcessingCost(), gridlet.getActualCPUTime(),
                GridSim.clock());
        }


        if (detail == true)
        {
            // a loop to print each Gridlet's history
            for (i = 0; i < size; i++)
            {
                gridlet = (Gridlet) list.get(i);
                System.out.println(gridlet.getGridletHistory());

                System.out.print("Gridlet #" + gridlet.getGridletID());
                System.out.println(", length = " + gridlet.getGridletLength()
                    + ", finished so far = " + gridlet.getGridletFinishedSoFar());
                System.out.println("======================================\n");
            }
        }

        System.out.println("=================================================");
    }


    /**
     * This method will show you how to create Gridlets
     * @param userID        owner ID of a Gridlet
     * @param numGridlet    number of Gridlet to be created
     */
    private void createGridlet(int userID, int numGridlet)
    {
        for (int i = 0; i < numGridlet; i++)
        {
            // Creates a Gridlet
            Gridlet gl = new Gridlet(i, gridletLength, gridletInput, gridletOutput);
            gl.setUserID(userID);

            GridletSubmission gst = new GridletSubmission(gl, false);

            // add this gridlet into a list
            this.GridletSubmittedList_.add(gst);
        }
    }


    /**
     * Tells you the possition of this gridlet in the GridletSubmittedList_
     * @param gl the gridlet
     * @return the position of this gridlet in the list of submitted gridlets
     */
    private int findGridletInGridletSubmittedList(Gridlet gl)
    {
        Gridlet g = null;
        GridletSubmission gst = null;
        for (int i = 0; i< GridletSubmittedList_.size(); i++)
        {
            gst = (GridletSubmission)GridletSubmittedList_.get(i);
            if ( gst.getGridletID() == gl.getGridletID() )
            {
                return i;
            }
        }

        return -1;
    }


    /**
     * This method will show you how to create Gridlets
     * @param numGridlet    number of Gridlet to be created
     */
    private void createGridlet(int numGridlet)
    {
        long data;
        int MI = 0;
        double percent = 0.30;  // 30 %
        Random r = new Random();

        // running approx. 20 mins on CERN node of 70k MIPS +/- 30%
        double min_size = gridletLength - (gridletLength * percent);
        double max_size = gridletLength + (gridletLength * percent);
        int range = 1 + (int) (max_size - min_size);  // round up

        // for input and output file transfer
        double data_min = gridletInput - (gridletInput * percent);
        double data_max = gridletInput + (gridletInput * percent);
        int data_range = 1 + (int) (data_max - data_min);  // round up

        for (int i = 0; i < numGridlet; i++)
        {
            MI = (int) min_size + r.nextInt(range);
            data = (long) data_min + r.nextInt(data_range);

            // Creates a Gridlet
            Gridlet gl = new Gridlet(i, MI, data, data);
            gl.setUserID( super.get_id() );
            gl.setNetServiceLevel(ToS_);

            GridletSubmission gst = new GridletSubmission(gl, false);

            // add this gridlet into a list
            this.GridletSubmittedList_.add(gst);
        }
    }


    /**
     * Write some data into the final results file.
     * @param user user name
     * @param glID gridlet id
     * @param resName Name of the resource
     * @param cost the processing cost of the gridlet
     * @param cpu the cpu time
     * @param clock Current time
     */
    private void writeFin(String user, int glID, String resName,
                          double cost, double cpu, double clock)
    {
        // Write into a results file
        FileWriter fwriter = null;
        try
        {
            fwriter = new FileWriter(user + "_Fin.csv", true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while opening file " + user + "_Fin.csv");
        }

        try
        {
            fwriter.write(user + ", "+ glID + ", " +
                resName + ", " + cost + ", "+ cpu + ", " + + clock + "\n");
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while writing on file " + user + "_Fin");
        }

        try
        {
            fwriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while closing file " + user + "_Fin");
        }

    }

    /**
     * Write some data into a results file.
     * @param user user name
     * @param event Values: "Sending" or "Receive" a gridlet
     * @param glID gridlet id
     * @param resName Name of the resource
     * @param status Status of the gridlet
     * @param clock Current time
     */
    private void write(String user, String event, int glID, String resName,
                       String status, double clock)
    {
        FileWriter fwriter = null;
        try
        {
            fwriter = new FileWriter(super.get_name() + ".csv", true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                "Unwanted errors while opening file " + super.get_name());
        }

        try
        {
            fwriter.write(user + ", " + event + ", " + glID + ", " +
                    resName + ", " + status + ", " + clock + "\n");
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
            "Unwanted errors while writing on file " + super.get_name());
        }

        try
        {
            fwriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
            "Unwanted errors while closing file " + super.get_name());
        }

    }


    /**
     * Initialize the results files (put headers over each column)
     */
    private void initializeResultsFile()
    {
        // Initialize the results file
        FileWriter fwriter = null;
        FileWriter fwriterFin = null;
        FileWriter fwriterDropped = null;

        try
        {
            // overwrite existing file
            fwriter = new FileWriter(super.get_name() + ".csv");
            //fwriterFin = new FileWriter(super.get_name() + "_Fin.csv");
            //fwriterDropped = new FileWriter(super.get_name() + "_DroppedGridlets.csv");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while opening file "
                + super.get_name() + " or " + super.get_name() + "_Fin.csv");
        }

        try
        {
            fwriter.write("User Name, Event, GridletID, Resource, GridletStatus, Time\n");
            //fwriterFin.write( "User, GridletID, Resource, Cost, CPU time, Latency\n");
            //fwriterDropped.write( "User, GridletID, Status, Time\n");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while writing on file "
                + super.get_name() + " or " + super.get_name() + "_Fin.csv or "
                + super.get_name() + "_DroppedGridlets.csv");
        }

        try
        {
            fwriter.close();
            //fwriterFin.close();
            //fwriterDropped.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while closing file "
                + super.get_name() + " or " + super.get_name() + "_Fin.csv or "
                + super.get_name() + "_DroppedGridlets.csv");
        }
    }


    /**
     * Prints out the given message into stdout.
     * In addition, writes it into a file.
     * @param msg   a message
     * @param file  file where we want to write
     */
    private void write(String msg, String file)
    {
        FileWriter fwriter = null;
        try
        {
            fwriter = new FileWriter(file, true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while opening file " + file);
        }

        try
        {
            fwriter.write(msg);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while writing on file " + file);
        }

        try
        {
            fwriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while closing file " + file);
        }
    }

} // end class

