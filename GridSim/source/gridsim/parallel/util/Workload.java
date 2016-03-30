/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2004, The University of Melbourne, Australia
 */

package gridsim.parallel.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;

import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.IO_data;
import gridsim.net.InfoPacket;
import gridsim.net.Link;
import gridsim.net.SimpleLink;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;

/**
 * The main purpose of this class is to create a realistic simulation
 * environment where your jobs are competing with others.
 * In other words, the grid resource might not be available at certain times.
 * In addition, the arrival time of jobs are also captured in the trace file.
 * <p>
 * This class dispatches jobs to a grid resource according to the workload 
 * model provided. That is, the workload model can generate jobs according to
 * various distributions, or read the job information from a log file.
 * <p>
 * <b>NOTE THAT:</b>
 * <ul>
 *      <li> This class can be classified as <b>one grid user entity</b>.
 *           Hence, you need to incorporate this entity into <tt>numUser</tt>
 *           during {@link gridsim.GridSim#init(int, Calendar, boolean)}
 *      <li> If you are running an experiment using the network extension,
 *           i.e. the gridsim.net package, then you need to use
 *           {@link #Workload(String, double, double, int, String, WorkloadModel)}
 *           instead.
 * </ul>
 *
 * @author Marcos Dias de Assuncao
 * @since  5.0
 */
public class Workload extends GridSim {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
    private int resID;         		// resource ID
    private ArrayList<Gridlet> completedJobs = null;   // list for collecting jobs
    private WorkloadModel model = null;
    private int numGenJobs = 0;

    /**
     * Create a new Workload object <b>without</b> using the network extension.
     * This means this entity directly sends jobs to a destination resource
     * without going through a wired network. <br>
     * <tt>NOTE:</tt>
     * You can not use this constructor in an experiment that uses a wired
     * network topology.
     *
     * @param name      this entity name
     * @param resourceName  the resource name
     * @param model the workload model to be used for generating the jobs
     * @throws Exception this happens when creating this entity before
     *                   initialising GridSim package or this entity name is
     *                   <code>null</code> or empty
     * @throws IllegalArgumentException this happens for the following conditions:
     *      <ul>
     *          <li>the entity name is null or empty
     *          <li>the resource entity name is null or empty
     *      </ul>
     * @pre name != null
     * @pre resourceName != null
     * @pre model != null
     * @post $none
     */
    public Workload(String name, String resourceName, 
    						WorkloadModel model) throws Exception {
        super(name, GridSimTags.DEFAULT_BAUD_RATE);
        if (resourceName == null || resourceName.length() == 0) {
            throw new IllegalArgumentException("Invalid resource name: " + resourceName);
        }

        this.resID = GridSim.getEntityId(resourceName);
        this.model = model;
    }

    /**
     * Create a new Workload object <b>with</b> the network extension.
     * This means this entity directly sends jobs to a destination resource
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
     * @param resourceName  the resource name
     * @param model the workload model to be used for generating the jobs
     * @throws Exception  this happens when creating this entity before
     *                   initialising GridSim package or this entity name is
     *                   <code>null</code> or empty
     * @throws IllegalArgumentException this happens for the following conditions:
     *      <ul>
     *          <li>the entity name is null or empty
     *          <li> baudRate <= 0
     *          <li> propDelay <= 0
     *          <li> MTU <= 0
     *          <li>the resource entity name is null or empty
     *      </ul>
     * @pre name != null
     * @pre baudRate > 0
     * @pre propDelay > 0
     * @pre MTU > 0
     * @pre resourceName != null
     * @pre model != null
     * @post $none
     */
    public Workload(String name, double baudRate, double propDelay, int MTU,
                    String resourceName, WorkloadModel model)
                    throws Exception {
        super( name, new SimpleLink(name+"_link", baudRate, propDelay, MTU) );

        if (resourceName == null || resourceName.length() == 0) {
            throw new IllegalArgumentException("Invalid resource name: " + resourceName);
        }
        
        this.resID = GridSim.getEntityId(resourceName);
        this.model = model;
    }

    /**
     * Create a new Workload object <b>with</b> the network extension.
     * This means this entity directly sends jobs to a destination resource
     * through a link. The link is automatically created by this constructor.
     *
     * @param name      this entity name
     * @param link      the link that will be used to connect this Workload
     *                  to another entity or a Router.
     * @param resourceName  the resource name
     * @param model the workload model to be used for generating the jobs
     * @throws Exception this happens when creating this entity before
     *                   initialising GridSim package or this entity name is
     *                   <code>null</code> or empty
     * @throws IllegalArgumentException this happens for the following conditions:
     *      <ul>
     *          <li>the entity name is null or empty
     *          <li>the link is empty
     *          <li>the resource entity name is null or empty
     *      </ul>
     * @pre name != null
     * @pre link != null
     * @pre resourceName != null
     * @pre model != null
     * @post $none
     */
    public Workload(String name, Link link, 
                    String resourceName, WorkloadModel model)
                    throws Exception {
        super(name, link);

        if (resourceName == null || resourceName.length() == 0) {
            throw new IllegalArgumentException("Invalid resource name: " + resourceName);
        }

        this.resID = GridSim.getEntityId(resourceName);
        this.model = model;
    }

    /**
     * Generates jobs according to provided model when the simulation starts.
     * Then submits jobs to a resource and collects them before exiting.
     * To collect the completed Gridlets, use {@link #getGridletList()}
     */
    public void body() {
    	logger.info(super.get_name() + " is starting...");

        // get the resource id
        if (resID < 0) {
        	logger.severe("Invalid resource: " + GridSim.getEntityName(resID));
            return;
        }
        
        boolean success = submitGridlets();
        
        if(success) {
        	logger.info(super.get_name() +  " is collecting jobs...");
        	
            Sim_event ev = new Sim_event();
        	while (Sim_system.running()) {
                super.sim_get_next(ev);

                // if the simulation finishes then exit the loop
                if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                    break;
                }

                // process the received event
                processEvent(ev);
                
                // if all the Gridlets have been collected
                if (completedJobs.size() == numGenJobs) {
                    break;
                }
            }
        }
        else {
        	logger.severe(super.get_name() + " was unable to submit the jobs.");
        }

        // shut down all the entities, including GridStatistics entity since
        // we used it to record certain events.
        shutdownGridStatisticsEntity();
        shutdownUserEntity();
        terminateIOEntities();

        logger.info(super.get_name() + " is exiting...");
    }

    /**
     * Submits the jobs generated by the model to the resource 
     * @return <code>true</code> if successful; <code>false</code> otherwise.
     */
    private boolean submitGridlets() {
    	logger.info(super.get_name() +  " is submitting jobs to " + 
    			GridSim.getEntityName(resID) + " ...");
    	
    	List<WorkloadJob> jobs = model.generateWorkload();
    	if(jobs == null) {
    		return false;
    	}
    	
    	for(WorkloadJob job : jobs) {
            Gridlet gl = job.getGridlet();
            gl.setUserID( super.get_id() );   // set the owner ID
            long submitTime = job.getSubmissionTime();
            super.send(resID, submitTime, GridSimTags.GRIDLET_SUBMIT, gl);
    	}
    	
    	numGenJobs = jobs.size();
    	this.completedJobs = new ArrayList<Gridlet>(numGenJobs);
    	return true;
    }

    /**
     * Processes events sent to this entity.
     * @param ev the event received.
     */
    private void processEvent(Sim_event ev) {
        Object data = null;
        data = ev.get_data();       // get the event's data

        if (data != null && data instanceof Gridlet) {
        	Gridlet gl = (Gridlet)data;
        	completedJobs.add(gl);
        }
        else {
            // handle ping request
            if (ev.get_tag() == GridSimTags.INFOPKT_SUBMIT) {
                processPingRequest(ev);
            }
        }
    }
    
    /**
     * Processes a ping request.
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processPingRequest(Sim_event ev) {
        InfoPacket pkt = (InfoPacket) ev.get_data();
        pkt.setTag(GridSimTags.INFOPKT_RETURN);
        pkt.setDestID( pkt.getSrcID() );

        // sends back to the sender
        super.send(super.output, GridSimTags.SCHEDULE_NOW,
                   GridSimTags.INFOPKT_RETURN,
                   new IO_data(pkt, pkt.getSize(), pkt.getSrcID()) );
    }
    
    /**
     * Gets a list of completed jobs
     * @return a list of jobs
     * @throws IllegalStateException if this method is invoked before the 
     * simulation completes
     */
    public ArrayList<Gridlet> getGridletList() {
    	if(completedJobs == null || Sim_system.running()) {
    		throw new IllegalStateException("Impossible to return job list " +
    				"before the simulation completes");
    	}
    	
        return completedJobs;
    }
} 

