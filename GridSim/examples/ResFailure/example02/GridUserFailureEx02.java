package ResFailure.example02;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 *               An example of how to use the failure functionality.
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: UCLM (Spain)
 * Created on: August 2007
 */

import gridsim.*;
import gridsim.index.AbstractGIS;
import gridsim.net.Link;
import java.util.Random;
import java.util.ArrayList;
import java.io.FileWriter;
import gridsim.resFailure.RegionalGISWithFailure;
import eduni.simjava.Sim_system;
import eduni.simjava.Sim_event;


/**
 * Creates a Grid User that also considers what happens if a resource fails.
 * @author       Agustin Caminero and Anthony Sulistio
 * @since        GridSim Toolkit 4.1
 */
public class GridUserFailureEx02 extends GridUser
{
    // the base for our constants (chosen at random)
    private static final int BASE = 440000;

    /** This constant is to tell the user when he should submit a gridlet */
    private static final int SUBMIT_GRIDLET = BASE + 1;

    private ArrayList GridletSubmittedList_;   // list of submitted Gridlets
    private GridletList GridletReceiveList_;   // list of received Gridlets

    private int NUM_GRIDLETS;
    private double pollingTime_;

    // The sizes of gridlets
    private int gridletLength;
    private int gridletInput;
    private int gridletOutput;

    // we keep here the time when each gridlet is submitted
    private double gridletSubmissionTime [];
    private double gridletLatencyTime [];

    // a flag that denotes whether to trace GridSim events or not.
    private boolean trace_flag;


    /**
     * Creates a GridUserFailure object
     * @param name      this entity name
     * @param link      a network link connecting this entity
     * @param pollTime  the time between polls
     * @param glLength  length (MI) for the gridlets of this user
     * @param glIn      input file size for the gridlets of this user
     * @param glOut     output file size for the gridlets of this user
     * @param trace_flag  a flag that denotes whether to trace this user events
     *                    or not.
     * @throws java.lang.Exception happens if either name or link is empty
     */
    public GridUserFailureEx02(String name, Link link, double pollTime,
                        int glLength, int glIn, int glOut, boolean trace_flag)
                        throws Exception
    {
        super(name, link);

        this.GridletSubmittedList_ = new ArrayList();
        this.GridletReceiveList_ = new GridletList();
        pollingTime_ = pollTime;

        gridletLength = glLength;
        gridletInput = glIn;
        gridletOutput = glOut;
        this.trace_flag = trace_flag;
    }

    /**
     * Sets the number of gridlets that this user has to submit.
     * Also, create the submission and reception  times arrays.
     * @param gridlet_num the number of gridlets
     */
    public void setGridletNumber(int gridlet_num)
    {
        NUM_GRIDLETS = gridlet_num;

        gridletSubmissionTime = new double[NUM_GRIDLETS];
        gridletLatencyTime = new double[NUM_GRIDLETS];
    }

    /**
     * Handles incoming requests to this entity.
     * This method specifies the behaviours and/or scenarios of a user.
     */
    public void body()
    {
        initializeResultsFile();
        createGridlet(super.get_id(), NUM_GRIDLETS);

        // schedule the initial sending of gridlets.
        // The sending will start in a ramdom time within 5 min
        Random random = new Random();
        int init_time = random.nextInt(5*60);

        // sends a reminder to itself
        super.send(super.get_id(), init_time, SUBMIT_GRIDLET);
        System.out.println(super.get_name() +
               ": initial SUBMIT_GRIDLET event will be at clock: " +
               init_time + ". Current clock: " + GridSim.clock());


        ////////////////////////////////////////////////////////////
        // Now, we have the framework of the entity:
        while (Sim_system.running())
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev); // get the next event in the queue

            switch (ev.get_tag())
            {
                // submit a gridlet
                case SUBMIT_GRIDLET:
                    processGridletSubmission(ev); // process the received event
                    break;

                // Receive a gridlet back
                case GridSimTags.GRIDLET_RETURN:
                    processGridletReturn(ev);
                    break;

                case GridSimTags.END_OF_SIMULATION:
                    System.out.println("\n============== " + super.get_name() +
                                       ". Ending simulation...");
                    break;

                default:
                    System.out.println(super.get_name() +
                                       ": Received an event: " + ev.get_tag());
                    break;

            } // switch

        } //  while

        // wait for few seconds before printing the output
        super.sim_pause( super.get_id()*2 );

        // remove I/O entities created during construction of this entity
        super.terminateIOEntities();

        // prints the completed gridlets
        printGridletList(GridletReceiveList_, super.get_name(), false,
                         gridletLatencyTime);
    } // body()

    /////////////////////////////////////////////////////////////////////////

    /**
     * This functions process the submission  of a gridlet. We have to get the
     * list of available resources from the RegGIS, choose one of them, and
     * submit the gridlet.
     * @param ev an incoming event
     */
    private void processGridletSubmission(Sim_event ev)
    {
        if (trace_flag)
        {
            System.out.println(super.get_name() +
                ": received an SUBMIT_GRIDLET event. Clock: " + GridSim.clock());
        }

        /***********
        We have to submit:
             - the gridlet whose id comes with the event
             - all the gridlets with the "gridletSub.getSubmitted() == false"

        So, set the gridletSub.getSubmitted() to false for the gridlet whose
        id comes with the event
        ***********/

        int i = 0;
        GridletSubmission gridletSub;
        int resourceID[];
        Random random = new Random(5);   // a random generator with a random seed
        int index;
        Gridlet gl;
        Integer obj;
        int glID;

        // This is because the initial GRIDLET_SUBMIT event, at the beginning
        // of sims, does not have any gridlet id. We have to submit
        // all the gridlets.
        if (ev.get_data() instanceof Integer)
        {
            obj = (Integer) ev.get_data();
            glID = obj.intValue(); // get the gridlet id.
        }
        else {
            glID = 99; // a value at random, not used at all in this case
        }

        while (i < GridletSubmittedList_.size())
        {
            gridletSub = (GridletSubmission)GridletSubmittedList_.get(i);

            if ( (gridletSub.getGridlet()).getGridletID() == glID )
            {
                // set this gridlet whose id comes with the event as not submitted,
                // so that it is submitted as soon as possible.
                ((GridletSubmission) GridletSubmittedList_.get(i)).setSubmitted(false);
            }

            // Submit the gridlets with the "gridletSub.getSubmitted() == false"
            if ( (gridletSub.getSubmitted() == false))
            {
                // we have to resubmit this gridlet
                gl = ((GridletSubmission) GridletSubmittedList_.get(i)).getGridlet();
                resourceID = getResList(); // Get list of resources from GIS

                // If we have resources in the list
                if ((resourceID != null) && (resourceID.length != 0))
                {
                    index = random.nextInt(resourceID.length);

                    // make sure the gridlet will be executed from the begining
                    resetGridlet(gl);

                    // submits this gridlet to a resource
                    super.gridletSubmit(gl, resourceID[index]);
                    gridletSubmissionTime[gl.getGridletID()] = GridSim.clock();

                    // set this gridlet as submitted
                    ((GridletSubmission) GridletSubmittedList_.get(i)).setSubmitted(true);

                    if (trace_flag)
                    {
                        System.out.println(super.get_name() +
                               ": Sending Gridlet #" + i + " to " +
                               GridSim.getEntityName(resourceID[index]) +
                               " at clock: " + GridSim.clock());

                        // Write into a results file
                        write(super.get_name(), "Sending", gl.getGridletID(),
                              GridSim.getEntityName(resourceID[index]),
                              gl.getGridletStatusString(), GridSim.clock());
                    }

                }
                // No resources available at this moment, so schedule an event
                // in the future. The event wil be in 15 min (900 sec), as
                // resource failures may last several hours.
                // This event includes the gridletID, so that the user will
                // try to submit only this gridlet
                else
                {
                    super.send(super.get_id(), GridSimTags.SCHEDULE_NOW + 900,
                               SUBMIT_GRIDLET, new Integer(gl.getGridletID()) );
                }

            }// if (gridletSub.getSubmitted() == false)

            i++;
        } // while (i < GridletSubmittedList_.size())

    } // processGridletSubmission

    /**
     * This functions process the return of a gridlet.
     * We pay attention to the status of the gridlet
     * and then decide what we have to do the next
     * @param ev an incoming event
     */
    private void processGridletReturn(Sim_event ev)
    {
        if (trace_flag)
        {
            System.out.println(super.get_name() +
                ": received an GRIDLET_RETURN event. Clock: " + GridSim.clock());
        }

        Object obj = (Object) ev.get_data();
        Gridlet gl = null;
        Random random = new Random(5);   // a random generator with a random seed

        if (obj instanceof Gridlet)
        {
            gl = (Gridlet) obj;
            gridletLatencyTime[gl.getGridletID()] = GridSim.clock();

            // Write into a results file
            if (trace_flag)
            {
                write(super.get_name(), "Receiving", gl.getGridletID(),
                      GridSim.getEntityName(gl.getResourceID()),
                      gl.getGridletStatusString(), GridSim.clock());
            }

            ///////////////////////// Gridlet Success
            if (gl.getGridletStatusString().compareTo("Success") == 0)
            {
                System.out.println(super.get_name() + ": Receiving Gridlet #" +
                       gl.getGridletID() + " with status Success at time = " +
                       GridSim.clock() + " from resource " +
                       GridSim.getEntityName(gl.getResourceID()));

                this.GridletReceiveList_.add(gl); // add into the received list
                gridletLatencyTime[gl.getGridletID()] =
                        gridletLatencyTime[gl.getGridletID()] -
                        gridletSubmissionTime[gl.getGridletID()];

                // We have received all the gridlets. So, finish the simulation.
                if (GridletReceiveList_.size() == GridletSubmittedList_.size())
                {
                    super.finishSimulation();
                }

            } // if (gl.getGridletStatusString() == "Success")

            //////////////////////// Gridlet Failed
            else if (gl.getGridletStatusString().compareTo("Failed") == 0)
            {
                System.out.println(super.get_name() + ": Receiving Gridlet #" +
                       gl.getGridletID() + " with status Failed at time = " +
                       GridSim.clock() + " from resource " +
                       GridSim.getEntityName(gl.getResourceID()));

                // Send the gridlet as soon as we have resources available.
                // This gridlet will be resend as soon as possible,
                // in the first loop.
                int pos = findGridletInGridletSubmittedList(gl);
                if (pos == -1) {
                    System.out.println(super.get_name() +
                        ". Gridlet not found in GridletSubmittedList.");
                }
                else
                {
                    // set this gridlet as submitted, because otherwise
                    // this gridlet may be submitted several times.
                    // A gridlet will only be submitted when the event carrying
                    // its id reaches the user
                    ((GridletSubmission) GridletSubmittedList_.get(pos)).setSubmitted(true);

                    // Now, schedule an event to itself to submit the gridlet
                    // The gridlet will be sent as soon as possible
                    Integer glID_Int = new Integer(gl.getGridletID());

                    // This event includes the gridletID, so that the user
                    // will try to submit only this gridlet
                    super.send(super.get_id(), GridSimTags.SCHEDULE_NOW,
                               SUBMIT_GRIDLET, glID_Int);
                }
            } // if (gl.getGridletStatusString() == "Failed")

            ////////////////////////////// Gridlet Failed_resource
            else if (gl.getGridletStatusString().compareTo("Failed_resource_unavailable") == 0)
            {
                int pos = findGridletInGridletSubmittedList(gl);
                if (pos == -1) {
                    System.out.println(super.get_name() +
                        ". Gridlet not found in GridletSubmittedList.");
                }
                else
                {
                    // Now, set its submission time for a random time between
                    // 1 and the polling time
                    double resubmissionTime = random.nextDouble() * pollingTime_;

                    // this is to prevent the gridlet from being submitted
                    // before its resubmission time.
                    // This is different from the FAILED case, because
                    // in that case, the gridlet should be resubmited as soon
                    // as possible. As oppossed to that, this gridlet should
                    // not be resubmited before its resubmission time.
                    ((GridletSubmission) GridletSubmittedList_.get(pos)).setSubmitted(true);


                    System.out.println(super.get_name() + ": Receiving Gridlet #" +
                           gl.getGridletID() +
                           " with status Failed_resource_unavailable at time = " +
                           GridSim.clock() + " from resource " +
                           GridSim.getEntityName(gl.getResourceID()) +
                           "(resID: " + gl.getResourceID() +
                           "). Resubmission time will be: " +
                           resubmissionTime + GridSim.clock());

                    // Now, we have to inform the GIS  about this failure, so it
                    // can keep the list of resources up-to-date.
                    informGIS(gl.getResourceID());

                    // Now, schedule an event to itself to submit the gridlet
                    Integer glID_Int = new Integer(gl.getGridletID());

                    // This event includes the gridletID, so that the user
                    // will try to submit only this gridlet
                    super.send(super.get_id(), resubmissionTime,
                               SUBMIT_GRIDLET, glID_Int);
                }
            } // else if
            else
            {
                System.out.println(super.get_name() + ": Receiving Gridlet #" +
                       gl.getGridletID() + " with status " +
                       gl.getGridletStatusString() + " at time = " +
                       GridSim.clock() + " from resource " +
                       GridSim.getEntityName(gl.getResourceID()) +
                       " resID: " + gl.getResourceID());
            }

        } // if (obj instanceof Gridlet)
    }

    /**
     * Prints the Gridlet objects
     * @param list the list of gridlets
     * @param name the name of the user
     * @param detail if we want the gridlet's history or not
     * @param gridletLatencyTime array containing the latencies of gridlets.
     * Latencies are from the moment when the gridlet is sent, till
     * the moment they are back at the user.
     * They take into account the last submission of a gridlet
     * (when the gridlet is successfully run)
     */
    private void printGridletList(GridletList list, String name,
                            boolean detail, double gridletLatencyTime[])
    {
        int size = list.size();
        Gridlet gridlet = null;

        String indent = "    ";
        StringBuffer buffer = new StringBuffer(1000);
        buffer.append("\n\n============== OUTPUT for " + name + " ===========");
        buffer.append("\nGridlet ID" + indent + "STATUS" + indent +
                       "Resource ID" + indent + indent + "Cost" + indent +
                       indent + "CPU Time" + indent + indent + "Latency");

        // a loop to print the overall result
        int i = 0;
        boolean header = true;

        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);

            buffer.append("\n");
            buffer.append(indent + gridlet.getGridletID() + indent + indent);
            buffer.append( gridlet.getGridletStatusString() );
            buffer.append(indent + indent + gridlet.getResourceID() +
                          indent + gridlet.getProcessingCost() +
                          indent + gridlet.getActualCPUTime() +
                          indent + gridletLatencyTime[gridlet.getGridletID()]);

            if (i != 0) {
                header = false;
            }

            writeFin(name, gridlet.getGridletID(),
                     GridSim.getEntityName(gridlet.getResourceID()),
                     gridlet.getProcessingCost(), gridlet.getActualCPUTime(),
                     GridSim.clock(), header);
        }

        if (detail == true)
        {
            // a loop to print each Gridlet's history
            for (i = 0; i < size; i++)
            {
                gridlet = (Gridlet) list.get(i);

                buffer.append( gridlet.getGridletHistory() );
                buffer.append("Gridlet #" + gridlet.getGridletID());
                buffer.append(", length = " + gridlet.getGridletLength()
                               + ", finished so far = " +
                               gridlet.getGridletFinishedSoFar());
                buffer.append("===========================================");
            }
        }

        buffer.append("\n====================================================");
        System.out.println( buffer.toString() );

    }

    /**
     * This method will show you on how to create Gridlets
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

            // Originally, gridlets are created to be submitted
            // as soon as possible (the 0.0 param)
            GridletSubmission gst = new GridletSubmission(gl, false);

            // add this gridlet into a list
            this.GridletSubmittedList_.add(gst);
        }
    }

    /**
     * Gets a list of received Gridlets
     * @return a list of received/completed Gridlets
     */
    public GridletList getGridletList() {
        return GridletReceiveList_;
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
            g = gst.getGridlet();

            if ( g.getGridletID() == gl.getGridletID() )
                return i;
        }

        return -1;
    }

    /** This function resets the gridlet into its original values of
     * length and gridletFinishedSoFar
     * @param gl the gridlet to be resetted
     */
    private void resetGridlet (Gridlet gl)
    {
        gl.setGridletLength(gridletLength);
        gl.setGridletFinishedSoFar(0);

    }

    /**
    * Write some data into the final results file.
    * @param user       user name
    * @param glID       gridlet id
    * @param resName    Name of the resource
    * @param cost       the processing cost of the gridlet
    * @param cpu        the cpu time
    * @param clock      Current time
    * @param header     write a row header or not
    * */
   private void writeFin(String user, int glID, String resName,
                                double cost, double cpu, double clock,
                                boolean header)
   {
       if (trace_flag == false) {
            return;
       }

       // Write into a results file
       FileWriter fwriter = null;
       try
       {
           fwriter = new FileWriter(user, true);
       } catch (Exception ex)
       {
           ex.printStackTrace();
           System.out.println(
                   "Unwanted errors while opening file " + user);
       }

       try
       {
           if (header == true) {
               fwriter.write(
                  "\n\nGridletID \t Resource \t Cost \t CPU time \t Latency\n");
           }

           fwriter.write(glID + "\t" + resName + "\t" + cost + "\t"+ cpu +
                         "\t" + + clock + "\n");
       } catch (Exception ex)
       {
           ex.printStackTrace();
           System.out.println(
                   "Unwanted errors while writing on file " + user);
       }

       try
       {
           fwriter.close();
       } catch (Exception ex)
       {
           ex.printStackTrace();
           System.out.println(
                   "Unwanted errors while closing file " + user);
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
        if (trace_flag == false) {
            return;
        }

        // Write into a results file
        FileWriter fwriter = null;
        try
        {
            fwriter = new FileWriter(super.get_name(), true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while opening file " + super.get_name());
        }

        try
        {
            fwriter.write(event + "\t\t" + glID + "\t" + resName + "\t" + status +
                          "\t\t" + clock + "\n");
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
     * This function informs the GIS of this user about the failure of a resource
     * @param resID the id of the resource which has failed
     */
    private void informGIS(int resID)
    {
        Integer resID_Int =  new Integer(resID);

        super.send(super.output, 0.0, AbstractGIS.NOTIFY_GIS_RESOURCE_FAILURE,
            new IO_data(resID_Int, Link.DEFAULT_MTU, super.getRegionalGISId()) );
    }

    /**
     * Initialize the results files (put headers over each column)
     * */
    private void initializeResultsFile()
    {
        if (trace_flag == false) {
            return;
        }

        // Initialize the results file
        FileWriter fwriter = null;
        try
        {
            fwriter = new FileWriter(super.get_name(), true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while opening file " + super.get_name() +
                    " or " + super.get_name()+"_Fin");
        }

        try
        {
            fwriter.write(
                "Event \t GridletID \t Resource \t GridletStatus \t\t Clock\n");
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while writing on file " + super.get_name()+
                    " or " + super.get_name()+"_Fin");
        }

        try
        {
            fwriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while closing file " + super.get_name()+
                    " or " + super.get_name()+"_Fin");
        }
    }

   /**
    * This function retrieves a list of available resources from the GIS.
    * @return an array containing the ids of the resources
    */
   private int[] getResList()
   {
       Object[] resList = super.getLocalResourceList();
       int resourceID[] = null;

       // if we have any resource
       if ((resList != null) && (resList.length != 0))
       {
           resourceID = new int[resList.length];
           for (int x = 0; x < resList.length; x++)
           {
               // Resource list contains list of resource IDs
               resourceID[x] = ((Integer) resList[x]).intValue();

               if (trace_flag == true)
               {
                   System.out.println(super.get_name() +
                            ": resource[" + x + "] = " + resourceID[x]);
               }
           }

        }

       return resourceID;
   }

} // end class
