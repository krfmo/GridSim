/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

/**
 * Contains various static command tags that indicate a type of action that
 * needs to be undertaken by GridSim entities when they receive or send events.
 * <br>
 * <b>NOTE:</b> To avoid conflicts with other tags, GridSim reserves negative
 * numbers, 0 - 299, and 9600.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class GridSimTags
{
    // starting constant value for grid-related tags
    private static final int BASE = 0;

    // starting constant value for network-related tags
    private static final int NETBASE = 100;

    // starting constant value for AR-related tags (also negative numbers)
    private static final int ARBASE = 200;

    //////////////////////////////////////////////////////////////////////

    /** Denotes boolean <tt>true</tt> in <tt>int</tt> value */
    public static final int TRUE = 1;

    /** Denotes boolean <tt>false</tt> in <tt>int</tt> value */
    public static final int FALSE = 0;

    /** Denotes the default baud rate for some GridSim entities */
    public static final int DEFAULT_BAUD_RATE = 9600;

    /** Schedules an entity without any delay */
    public static final double SCHEDULE_NOW = 0.0;

    /** Denotes the end of simulation */
    public static final int END_OF_SIMULATION = -1;

    //////////////////////////////////////////////////////////////////////

    /**
     * Denotes insignificant simulation entity or time. This tag will not be
     * used for identification purposes.
     */
    public static final int INSIGNIFICANT = BASE + 0;

    /** Sends an Experiment object between UserEntity and Broker entity */
    public static final int EXPERIMENT = BASE + 1;

    /**
     * Denotes a grid resource to be registered. This tag is normally used
     * between GridInformationService and GridResouce entity.
     */
    public static final int REGISTER_RESOURCE = BASE + 2;

    /**
     * Denotes a grid resource, that can support advance reservation, to be
     * registered. This tag is normally used between
     * GridInformationService and GridResouce entity.
     */
    public static final int REGISTER_RESOURCE_AR = BASE + 3;

    /**
     * Denotes a list of all resources, including the ones that can support
     * advance reservation. This tag is normally used between
     * GridInformationService and GridSim entity.
     */
    public static final int RESOURCE_LIST = BASE + 4;

    /**
     * Denotes a list of resources that only support
     * advance reservation. This tag is normally used between
     * GridInformationService and GridSim entity.
     */
    public static final int RESOURCE_AR_LIST = BASE + 5;

    /**
     * Denotes grid resource characteristics information. This tag is normally
     * used between GridSim and GridResource entity.
     */
    public static final int RESOURCE_CHARACTERISTICS = BASE + 6;

    /**
     * Denotes grid resource allocation policy. This tag is normally
     * used between GridSim and GridResource entity.
     */
    public static final int RESOURCE_DYNAMICS = BASE + 7;

    /**
     * Denotes a request to get the total number of Processing Elements (PEs)
     * of a resource. This tag is normally used between GridSim and GridResource
     * entity.
     */
    public static final int RESOURCE_NUM_PE = BASE + 8;

    /**
     * Denotes a request to get the total number of free Processing Elements
     * (PEs) of a resource. This tag is normally used between GridSim and
     * GridResource entity.
     */
    public static final int RESOURCE_NUM_FREE_PE = BASE + 9;

    /**
     * Denotes a request to record events for statistical purposes. This tag is
     * normally used between GridSim and GridStatistics entity.
     */
    public static final int RECORD_STATISTICS = BASE + 10;

    /** Denotes a request to get a statistical list. */
    public static final int RETURN_STAT_LIST = BASE + 11;

    /**
     * Denotes a request to send an Accumulator object based on category into
     * an event scheduler. This tag is normally used between ReportWriter and
     * GridStatistics entity.
     */
    public static final int RETURN_ACC_STATISTICS_BY_CATEGORY = BASE + 12;

    /** Denotes a request to register a GridResource entity to a regional
     * GridInformationService (GIS) entity
     */
    public static final int REGISTER_REGIONAL_GIS = BASE + 13;

    /** Denotes a request to get a list of other regional GIS entities
     * from the system GIS entity
     */
    public static final int REQUEST_REGIONAL_GIS = BASE + 14;

    /////////////////////////////////////////////////////////////

    /** Signal used by Entities to connect to Routers using a link. */
    public static final int REGISTER_LINK = NETBASE + 0;

    /** This is used by a router when it wants to connect with another router.*/
    public static final int REGISTER_ROUTER = NETBASE + 1;

    /** All NetPackets are routed through the network with this tag. */
    public static final int PKT_FORWARD = NETBASE + 2;

    /** This is used by Routers to send route advertisements. */
    public static final int ROUTER_AD = NETBASE + 3;

    /** This tag is used by Output class to time its packet sendings. */
    public static final int SEND_PACKET = NETBASE + 4;

    /** This tag is used by an entity to send ping requests */
    public static final int INFOPKT_SUBMIT = NETBASE + 5;

    /** This tag is used to return the ping request back to sender */
    public static final int INFOPKT_RETURN = NETBASE + 6;

    /**
     * This tag is used to identify a packet as a junk packet used for
     * background traffic
     */
    public static final int JUNK_PKT = NETBASE + 7;

    /** Denotes that this packet is empty. This tag
     * is used internally by {@link gridsim.net.Input} and
     * {@link gridsim.net.Output} entities.
     */
    public static final int EMPTY_PKT = NETBASE + 8;

    /** Denotes that this packet will be sent to a packet scheduler by a router
     * for enqueing.
     * This tag is used by a router to an active packet scheduler, such as
     * the {@link gridsim.net.RateControlledScheduler} entity.
     */
    public static final int SCHEDULER_ENQUE = NETBASE + 9;

    /** Denotes that this packet will be sent by a packet scheduler to a router
     * for dequeing.
     * This tag is used by an active packet scheduler, such as
     * the {@link gridsim.net.RateControlledScheduler} entity to a router.
     */
    public static final int SCHEDULER_DEQUE = NETBASE + 10;

    public static final int NET_PACKET_LEVEL = NETBASE + 11;

    public static final int NET_FLOW_LEVEL   = NETBASE + 12;
    
    public static final int NET_BUFFER_PACKET_LEVEL = NETBASE + 13;

    // Denotes flow en-route to destination (used manually by entities)
    public static final int FLOW_SUBMIT = NETBASE + 14;

    // Internal message to check forecast of flow duration
    public static final int FLOW_HOLD = NETBASE + 15;

    // Message to update forecast of flow duration
    public static final int FLOW_UPDATE = NETBASE + 16;

    // Denotes flow ack en-route to destination (used manually by entities)
    public static final int FLOW_ACK = NETBASE + 17;
    

    ///////////////////////////////////////////////////////////////
    // For the gridsim.net.fnb package

    /** This is to simulate the finite buffers. 
     * This constant is to tell an entity
     * that a packet has been dropped. */
    public static final int FNB_PACKET_DROPPED = NETBASE + 31;

    /** This is to simulate the finite buffers. 
     * This constant is used when an Output
     * port tells a user that a gridlet has failed because its (Gridlet) packets
     *  has been dropped. */
    public static final int FNB_GRIDLET_FAILED_BECAUSE_PACKET_DROPPED = NETBASE + 32;

    /** This is to simulate the finite buffers. A FIFO dropping algorithm. */
    public static final int FNB_FIFO = NETBASE + 33;

    /** This is to simulate the finite buffers. A RED (Random Early Detection) 
      dropping algorithm. */
    public static final int FNB_RED = NETBASE + 34;

    /** This is to simulate the finite buffers. 
     * The event used to capture the number of dropped packets. 
     * This event is sent form a SCFScheduler to itself every T time. */
    public static final int FNB_COUNT_DROPPED_PKTS = NETBASE + 35;

    /** This is to simulate the finite buffers. 
     * Adaptative RED (Random Early Detection) dropping algorithm. */
    public static final int FNB_ARED = NETBASE + 36;

    /** This is to update the parameters of ARED. */
    public static final int FNB_UPDATE_ARED_PARAMETERS = NETBASE + 37;

    /** This is to simulate the finite buffers. 
     * This constant is used when an Output port tells a user that a file 
     * has failed because its (File) packets has been dropped. */
    public static final int FNB_FILE_FAILED_BECAUSE_PACKET_DROPPED = NETBASE + 38;

    /** This is to simulate the finite buffers. 
     *  This constant is used when an Output port tells a user that a file 
     * and a gridlet have failed because their (File and Gridlet) packets
     *  has been dropped. */
    public static final int FNB_FILE_GRIDLET_FAILED_BECAUSE_PACKET_DROPPED = NETBASE + 39;

    /** This is to identify when a packet contains a file. */
    public static final int FNB_PKT_CONTAINS_FILE = NETBASE + 40;

    /** This is to update the parameters of ARED. 
     * The time period between updates (0.5 seconds). */
    public static final double FNB_UPDATE_ARED_PARAMETERS_PERIOD = 0.5;

    /**
     * When a router drops a packet, it has to tell to a user.
     * But the router does not notify immediately (when the dropping ocurs), 
     * but with a given delay (this delay). */
    public static final double FNB_DROPPING_DELAY = 0.01; // 10 milliseconds


    /////////////////////////////////////////////////////////////
    // I intentionally put a gap to incorporate future tags
    // so I don't have to change the numbers!

    /**
     * Denotes the return of a Gridlet back to sender. This tag is
     * normally used by GridResource entity.
     */
    public static final int GRIDLET_RETURN = BASE + 20;

    /**
     * Denotes the submission of a Gridlet. This tag is normally
     * used between GridSim User and GridResource entity.
     */
    public static final int GRIDLET_SUBMIT = BASE + 21;

    /**
     * Denotes the submission of a Gridlet with an acknowledgement.
     * This tag is normally used between GridSim User and GridResource entity.
     */
    public static final int GRIDLET_SUBMIT_ACK = BASE + 22;

    /** Cancels a Gridlet submitted in the GridResource entity. */
    public static final int GRIDLET_CANCEL = BASE + 23;

    /** Denotes the status of a Gridlet. */
    public static final int GRIDLET_STATUS = BASE + 24;

    /** Pauses a Gridlet submitted in the GridResource entity. */
    public static final int GRIDLET_PAUSE = BASE + 25;

    /** Pauses a Gridlet submitted in the GridResource entity with an
     * acknowledgement.
     */
    public static final int GRIDLET_PAUSE_ACK = BASE + 26;

    /** Resumes a Gridlet submitted in the GridResource entity. */
    public static final int GRIDLET_RESUME = BASE + 27;

    /** Resumes a Gridlet submitted in the GridResource entity with an
     * acknowledgement.
     */
    public static final int GRIDLET_RESUME_ACK = BASE + 28;

    /** Moves a Gridlet to another GridResource entity. */
    public static final int GRIDLET_MOVE = BASE + 29;

    /** Moves a Gridlet to another GridResource entity with an acknowledgement.
     */
    public static final int GRIDLET_MOVE_ACK = BASE + 30;


    /////////////////////////////////////////////////////////////
    // NOTE: below are new tags needed for the resource failure functionality
    // added by Agustin Caminero, Universidad de Castilla La Mancha (UCLM),
    // Spain. Nov 2006.

    /**
     * Denotes a request to get the total number of machines of a resource.
     * This tag is normally used between GridSim and GridResource entity.
     */
    public static final int RESOURCE_NUM_MACHINES = BASE + 31;

    /**
     * This tag is used to simulate a resource failure.
     * A RegionalGIS object sends an event to itself by using this tag,
     * so it can send a failure message to resources in the future.
     * This tag is also used when a failed resource receives a ping request.
     * In this case, the resource will use this tag in the ping response.
     */
    public static final int GRIDRESOURCE_FAILURE = BASE + 32;

    /**
     * This tag is used to tell a resource, which is currently failed or down,
     * to come back to life.
     */
    public static final int GRIDRESOURCE_RECOVERY = BASE + 33;

    /**
     * This tag is used by a user to poll a resource to find out whether
     * it is out of order or not.
     */
    public static final int GRIDRESOURCE_FAILURE_INFO = BASE + 34;

    /** This tag is used by a user to poll a resource. */
    public static final int GRIDRESOURCE_POLLING = BASE + 35;

    /** The polling interval for users. */
    public static final int POLLING_TIME_USER = BASE + 36;

    /** The polling interval for the GIS entities. */
    public static final int POLLING_TIME_GIS = BASE + 37;


    ///////////////////////////////////////////////////////////////

    // The below tags are sent by AdvanceReservation to ARGridResource class

    /**
     * Commits a reservation <b>without</b> any Gridlets attached.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_COMMIT_ONLY = ARBASE + 1;

    /**
     * Commits a reservation <b>with</b> one or more Gridlets attached.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_COMMIT_WITH_GRIDLET = ARBASE + 2;

    /**
     * Requests for a new <b>advanced</b> reservation.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_CREATE = ARBASE + 3;

    /**
     * Requests for a new <b>immediate</b> reservation.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_CREATE_IMMEDIATE = ARBASE + 4;

    /**
     * Cancels an existing reservation.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_CANCEL = ARBASE + 5;

    /**
     * Requests a list of busy time of a resource.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_LIST_BUSY_TIME = ARBASE + 6;

    /**
     * Requests a list of free or empty time of a resource.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_LIST_FREE_TIME = ARBASE + 7;

    /**
     * Queries the current status of a reservation.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_QUERY = ARBASE + 8;

    /**
     * Modifies an existing reservation.
     * This tag is sent by AdvanceReservation to ARGridResource class.
     */
    public static final int SEND_AR_MODIFY = ARBASE + 9;

    ///////////////////////////////////////////////////////////////

    // Below denotes the status of a reservation during its lifetime.
    // This answer is given in reply to
    // AdvanceReservation.queryReservation() method.

    /** The reservation has not yet begun, i.e. the current simulation time is
     * before the start time.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_NOT_STARTED = ARBASE + 10;

    /**
     * The reservation has been accepted by a resource, but not yet been
     * committed by a user.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_NOT_COMMITTED = ARBASE + 11;

    /**
     * The reservation has been canceled by a user during execution or active
     * session.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_TERMINATED = ARBASE + 12;

    /** The reservation has begun and is currently being executed by a
     * destinated GridResource entity.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_ACTIVE = ARBASE + 13;

    /** The reservation has been completed.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_COMPLETED = ARBASE + 14;

    /** The reservation has been canceled before activation.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_CANCELED = ARBASE + 15;

    /**
     * The reservation has passed its expiry time before being committed.
     * Hence, a resource will not execute this reservation.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_EXPIRED = ARBASE + 16;

    /**
     * The reservation booking ID is invalid.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_ERROR_INVALID_BOOKING_ID = ARBASE + 17;

    /**
     * The reservation booking ID does not exist.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_RESERVATION_DOESNT_EXIST = ARBASE + 18;

    /**
     * Unknown error happens.
     * This tag is sent by a resource or allocation policy to AdvanceReservation
     * class.
     */
    public static final int AR_STATUS_ERROR = ARBASE + 19;

    ///////////////////////////////////////////////////////////////

    // The below tags are used in reply to
    // AdvanceReservation.cancelReservation() method.
    // These tags are sent by a resource or allocation policy.

    /**
     * Cancellation of a reservation fails.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CANCEL_FAIL = ARBASE + 20;

    /**
     * Cancellation of a reservation fails due to invalid booking ID.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CANCEL_FAIL_INVALID_BOOKING_ID = ARBASE + 21;

    /**
     * Cancellation of a reservation fails due to finished Gridlets.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CANCEL_FAIL_GRIDLET_FINISHED = ARBASE + 22;

    /**
     * Cancellation of a reservation is successful. It could also means
     * a reservation has expired or completed.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CANCEL_SUCCESS = ARBASE + 23;

    /**
     * Cancellation of a reservation fails since a resource can not
     * support Advance Reservation functionalities.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CANCEL_ERROR_RESOURCE_CANT_SUPPORT = ARBASE + 24;

    /**
     * Cancellation of a reservation fails due to unknown error.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CANCEL_ERROR = ARBASE + 25;

    ///////////////////////////////////////////////////////////////

    // The below tags are used in reply to
    // AdvanceReservation.commitReservation() method.
    // These tags are sent by a resource or allocation policy.

    /**
     * Committing a reservation is successful.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_COMMIT_SUCCESS = ARBASE + 30;

    /**
     * Committing a reservation is failed.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_COMMIT_FAIL = ARBASE + 31;

    /**
     * Committing a reservation is failed due to expiry.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_COMMIT_FAIL_EXPIRED = ARBASE + 32;

    /**
     * Committing a reservation is failed due to invalid booking ID.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_COMMIT_FAIL_INVALID_BOOKING_ID = ARBASE + 33;

    /**
     * Committing a reservation is failed due to a resource does not support
     * Advance Reservation functionalities.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_COMMIT_ERROR_RESOURCE_CANT_SUPPORT = ARBASE + 34;

    /**
     * Committing a reservation is failed due to unknown error.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_COMMIT_ERROR = ARBASE + 35;

    ///////////////////////////////////////////////////////////////

    // The below tags are used in reply to
    // AdvanceReservation.modifyReservation() method.
    // These tags are sent by a resource or allocation policy.

    /**
     * Modification of a reservation fails due to invalid booking ID.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_FAIL_INVALID_BOOKING_ID = ARBASE + 40;

    /**
     * Modification of a reservation fails since it is in active state.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_FAIL_RESERVATION_ACTIVE = ARBASE + 41;

    /**
     * Modification of a reservation fails due to invalid start time.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_FAIL_INVALID_START_TIME = ARBASE + 42;

    /**
     * Modification of a reservation fails due to invalid end time.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_FAIL_INVALID_END_TIME = ARBASE + 43;

    /**
     * Modification of a reservation fails due to invalid number of PEs
     * requested.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_FAIL_INVALID_NUM_PE = ARBASE + 44;

    /**
     * Modification of a reservation fails due to unknown error.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_ERROR = ARBASE + 45;

    /**
     * Modification of a reservation is successfull.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_SUCCESS = ARBASE + 46;

    /**
     * Modification of a reservation fails due to a resource that can not
     * support Advance Reservation functionalities.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_MODIFY_FAIL_RESOURCE_CANT_SUPPORT = ARBASE + 47;

    ///////////////////////////////////////////////////////////////

    /** Denotes a return tag from GridResource to User entity
     * for sending the result of committing a reservation back.
     */
    public static final int RETURN_AR_COMMIT = ARBASE + 50;

    /** Denotes a return tag from GridResource to User entity
     * for sending the result of query free or busy time of a reservation
     */
    public static final int RETURN_AR_QUERY_TIME = ARBASE + 51;

    /** Denotes a return tag from GridResource to User entity
     * for sending the result of a reservation status
     */
    public static final int RETURN_AR_QUERY_STATUS = ARBASE + 52;

    /** Denotes a return tag from GridResource to User entity
     * for sending the result of cancelling a reservation
     */
    public static final int RETURN_AR_CANCEL = ARBASE + 53;

    /** Denotes a return tag from GridResource to User entity
     * for sending the result of requesting or creating a new reservation
     */
    public static final int RETURN_AR_CREATE = ARBASE + 54;

    /** Denotes a return tag from GridResource to User entity
     * for sending the result of modifying a reservation
     */
    public static final int RETURN_AR_MODIFY = ARBASE + 55;

    ///////////////////////////////////////////////////////////////

    // The below tags are used in reply to
    // AdvanceReservation.createReservation() method.
    // These tags are sent by a resource or allocation policy.
    // These tags have negative numbers as not to confuse with a generated
    // reservation ID done by a resource.

    /**
     * New request of a reservation fails due to invalid start time.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_ERROR_INVALID_START_TIME = -1;

    /**
     * New request of a reservation fails due to invalid end time.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_ERROR_INVALID_END_TIME = -2;

    /**
     * New request of a reservation fails due to invalid duration time.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_ERROR_INVALID_DURATION_TIME = -3;

    /**
     * New request of a reservation fails due to invalid number of PEs
     * requested.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_ERROR_INVALID_NUM_PE = -4;

    /**
     * New request of a reservation fails due to invalid resource ID.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_ERROR_INVALID_RESOURCE_ID = -5;

    /**
     * New request of a reservation fails due to invalid resource name.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_ERROR_INVALID_RESOURCE_NAME = -6;

    /**
     * New request of a reservation fails due to unknown error.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_ERROR = -7;

    /**
     * New request of a reservation fails due to a resource that can not
     * support Advance Reservation functionalities.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_CANT_SUPPORT = -8;

    /**
     * New request of a reservation fails due to not enough PEs.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_NOT_ENOUGH_PE = -9;

    /**
     * New request of a reservation fails since trying to request more than
     * the max. number allowed by a resource.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    // TODO: not sure at the moment. Probably for future tag
    //public static final int AR_CREATE_FAIL_OVERLIMIT_MAX_PE = -10;

    /**
     * New request of a reservation fails due to a resource full in 1 second.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_1_SEC = -11;

    /**
     * New request of a reservation fails due to a resource full in 5 seconds.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_5_SECS = -12;

    /**
     * New request of a reservation fails due to a resource full in 10 seconds.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_10_SECS = -13;

    /**
     * New request of a reservation fails due to a resource full in 15 seconds.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_15_SECS = -14;

    /**
     * New request of a reservation fails due to a resource full in 30 seconds.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_30_SECS = -15;

    /**
     * New request of a reservation fails due to a resource full in 45 seconds.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_45_SECS = -16;

    /**
     * New request of a reservation fails due to a resource full in 1 minute.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_1_MIN = -17;

    /**
     * New request of a reservation fails due to a resource full in 5 minutes.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_5_MINS = -18;

    /**
     * New request of a reservation fails due to a resource full in 10 minutes.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_10_MINS = -19;

    /**
     * New request of a reservation fails due to a resource full in 15 minutes.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_15_MINS = -20;

    /**
     * New request of a reservation fails due to a resource full in 30 minutes.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_30_MINS = -21;

    /**
     * New request of a reservation fails due to a resource full in 45 minutes.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_45_MINS = -22;

    /**
     * New request of a reservation fails due to a resource full in 1 hour.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_1_HOUR = -23;

    /**
     * New request of a reservation fails due to a resource full in 5 hours.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_5_HOURS = -24;

    /**
     * New request of a reservation fails due to a resource full in 10 hours.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_10_HOURS = -25;

    /**
     * New request of a reservation fails due to a resource full in 15 hours.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_15_HOURS = -26;

    /**
     * New request of a reservation fails due to a resource full in 30 hours.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_30_HOURS = -27;

    /**
     * New request of a reservation fails due to a resource full in 45 hours onwards.
     * This tag is sent by a resource or allocation policy to
     * AdvanceReservation entity.
     */
    public static final int AR_CREATE_FAIL_RESOURCE_FULL_IN_45_HOURS = -28;

    ///////////////////////////////////////////////////////////////

    /** Private Constructor */
    private GridSimTags() {
        throw new UnsupportedOperationException("GridSim Tags cannot be instantiated");
    }

} 

