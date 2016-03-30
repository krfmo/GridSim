/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import eduni.simjava.Sim_entity;
import eduni.simjava.Sim_system;
import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.ResourceCharacteristics;
import gridsim.parallel.ParallelResource;

/**
 * {@link ParallelVisualizer} is the class that represents the 
 * main window used by the visualisation tool. From this window it is 
 * possible to start the simulation, run it step by step or in slow motion.
 * <p>
 * <b>NOTE:</b> This visualisation tool should be used for debugging 
 * purposes only. It is useful if you want to evaluate a new allocation
 * policy for example. A real experiment is not meant to have any 
 * visualisation features. This interface was initially created for 
 * another simulator called PajFit (<b>http://pajfit.sourceforge.net/</b>).
 *  
 * @author Marco A. S. Netto and Marcos Dias de Assuncao 
 * 
 * @since 5.0
 * 
 * @see gridsim.GridSim#startGridSimulation(boolean)
 */
public class ParallelVisualizer extends AbstractVisualizer 
			implements ActionListener, ListSelectionListener {

	private static final long serialVersionUID = 2059324063853260682L;
	private static final int WINDOW_WIDTH = 450;
	private static final int WINDOW_HEIGHT = 350;
	
	private JButton btStep, btRun, btSlowMotion, btChangePause;
	private JTextField tfRunUntil;
	private JLabel status;
	
	private JList jlResource;
	private JTextArea jlResourceInfo;
	
	// the thread responsible for starting the simulation. To prevent 
	// buttons from remaining blocked during the whole simulation
	private Thread simThread;
	private Runnable executeAtEnd = null;
	
	// indicates whether it is the first time the user clicks a button 
	private boolean firstClick = true;  
	
	// a table containing references to the resource windows.
	private HashMap<String, ResourceWindow> resourceWindows = 
		new HashMap<String,ResourceWindow>();
	
	private ArrayList<ParallelResource> resources = 
		new ArrayList<ParallelResource>();
	
	// a list of all the allocation listeners in the system
	private static LinkedHashMap<Integer, AllocationListener> listeners = 
							new LinkedHashMap<Integer, AllocationListener>();
	
	/**
	 * Creates the main window of the visualiser.
	 */
	public ParallelVisualizer() {

    	// Populates the list of resources
    	Collection<Sim_entity> entities = Sim_system.getEntityList();
    	for(Sim_entity entity : entities) {
    		if(entity instanceof ParallelResource) {
    			resources.add((ParallelResource)entity);
    		}
    	}

		initResourceWindows();
		
		super.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		super.setTitle("GridSim " + GridSim.GRIDSIM_VERSION_STRING + " Visualizer");

		JPanel mainPanel = new JPanel(new GridLayout(0, 1));
		JPanel simulationPanel = new JPanel(new GridLayout(0, 3));

		Border simulationBorder = BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Simulation");
		simulationPanel.setBorder(simulationBorder);
		
		JPanel executionPanel = new JPanel(new GridLayout(3,1));
		executionPanel.setBorder(new TitledBorder("Execution"));
		
		JPanel pausePanel = new JPanel(new GridLayout(3,1));
		pausePanel.setBorder(new TitledBorder("Pause Condition"));
					
		btStep = new JButton("Step by Step");
		btRun = new JButton("Run");
		btSlowMotion = new JButton("Slow Motion");
		
		btStep.addActionListener(this);
		btRun.addActionListener(this);
		btSlowMotion.addActionListener(this);
		
		JLabel lbRunUntil = new JLabel(" Pause at (Seconds):");
		lbRunUntil.setAlignmentY(JTextField.CENTER_ALIGNMENT);
		
		tfRunUntil = new JTextField(10);
		btChangePause = new JButton("Change");
		executionPanel.add(btStep);

		pausePanel.add(lbRunUntil);
		pausePanel.add(tfRunUntil);
		pausePanel.add(btChangePause);
		
		btChangePause.addActionListener(this);
		executionPanel.add(btSlowMotion);
		executionPanel.add(btRun);
		
		ArrayList<String> resourceNames = new ArrayList<String>();
		for(GridResource resource : resources) {
			resourceNames.add(resource.getName());
		}
		
        jlResource = new JList();
        jlResource.setListData(resourceNames.toArray());
        jlResource.setBackground(this.getBackground());
        jlResource.addListSelectionListener(this);
        
        JPanel resourcePanel = new JPanel(new GridLayout(0, 1));
        resourcePanel.setBorder(new TitledBorder("Resources"));
        JScrollPane scrollResourcePanel = new JScrollPane(jlResource);
        resourcePanel.add(scrollResourcePanel);
      
        simulationPanel.add(executionPanel);
        simulationPanel.add(pausePanel);
        simulationPanel.add(resourcePanel);    
        
        jlResourceInfo = new JTextArea();
        jlResourceInfo.setBackground(this.getBackground());
		jlResourceInfo.setEditable(false);
        
        JPanel resourceInfoPanel = new JPanel(new GridLayout(0, 1));
        resourceInfoPanel.setBorder(new TitledBorder("Resource Details"));
        resourceInfoPanel.add(jlResourceInfo);

        Border resourceInfoBorder = BorderFactory.createTitledBorder(
        		BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Resource Details");
        resourceInfoPanel.setBorder(resourceInfoBorder);

        mainPanel.add(simulationPanel);
        mainPanel.add(resourceInfoPanel);
        
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        status = new JLabel("Current simulation time is " + GridSim.clock() + " seconds.");
        statusPanel.add(status);
        
        createMenuBar();
        
        super.setLocation(0, 0);
		super.getContentPane().add(mainPanel, BorderLayout.CENTER);
		super.getContentPane().add(statusPanel, BorderLayout.SOUTH);
		super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Creates the thread responsible for starting the simulation
		simThread = new Thread("Simulation") {
			public void run() {
				// here in fact, start the simulation
				GridSim.startGridSimulation();
				
				if(executeAtEnd != null) {
					executeAtEnd.run();
				}
			}
		};
		
		super.setVisible(true);
	}

	/**
	 * Invokes the runnable provided at the end of the simulation.
	 * @param runnable runnable to be executed at the end of simulation
	 */
	public void invokeAtEndOfSimulation(Runnable runnable) {
		executeAtEnd = runnable;
	}
	
    /**
     * Notifies a listener about the action performed
     * @param action the action performed
     * @see ActionType
     */
    public void notifyListeners(AllocationAction action) {
    	AllocationListener listener = listeners.get(action.getSubject());
    	if(listener != null) {
    		listener.allocationActionPerformed(action);
    		informListenersAboutTime();
    	}
    }

	/**
	 * Handles events triggered by the list of resource
	 */
	public void valueChanged(ListSelectionEvent ev) {
		String selectedResource = (String) jlResource.getSelectedValue();
		updateResourceDetails(selectedResource);
	}
	
	/**
	 * Handles the events generated by this frame
	 * @param e the action event 
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(e.getSource() == btChangePause) {
			long newPause = -1; 
			boolean success = true;
			try {
				newPause = Long.parseLong(tfRunUntil.getText());
				if(newPause >= GridSim.clock()) {
					GridSim.pauseSimulation(newPause);
				} else {
					success = false;
				}
			}
			catch (NumberFormatException nfe) {
				success = false;
			}
			if(!success) {
				String message = "The value informed to pause the simulation " +
				"is invalid.\nThe current simulation time is " + GridSim.clock() + ".";
			
			JOptionPane.showMessageDialog(this, message, 
					"Error While Setting the Time", 
					JOptionPane.ERROR_MESSAGE);

			}
		}
		else if(e.getSource() == btStep){
        	enableStepByStepMode();
        	disableSlowMotionMode();
        	
        	checkFirstClick();
       		GridSim.resumeSimulation();
    	}
    	else if(e.getSource() == btRun){
    		disableStepByStepMode();
    		disableSlowMotionMode();
        	
        	if(!checkFirstClick()) {
    			GridSim.resumeSimulation();
    		}
    	}
    	else if(e.getSource() == btSlowMotion){
    		disableStepByStepMode();
        	enableSlowMotionMode();
    		
        	if(!checkFirstClick()) {
    			GridSim.resumeSimulation();
    		}
    	}
    	else if(cmd.equals("Exit") ){
        	System.exit(0);
        }
        else {
    		String resourceName = cmd;
    		ResourceWindow window = resourceWindows.get(resourceName);
    		window.setVisible(true);
        }
	}
	
	// ------------------------- PRIVATE METHODS -------------------------

	/*
	 * Informs all the listeners about the change in the 
	 * simulation time
	 */
	private void informListenersAboutTime() {
		settings.setTimeSpan((long)GridSim.clock());
		Iterator<AllocationListener> iterListener = listeners.values().iterator();
		while(iterListener.hasNext()) {
			AllocationListener listener = iterListener.next();
			AllocationAction action = new AllocationAction(ActionType.SIMULATION_TIME_CHANGED);
			listener.allocationActionPerformed(action);
		}
		status.setText("Current simulation time is " + GridSim.clock() + " seconds.");
	}
	
	/*
	 * This method initialises the resource windows
	 */
	private void initResourceWindows() {
   		int windowId = 0;
   		for(ParallelResource resource : resources){
   			ResourceWindow window = new ResourceWindow(resource, windowId, WINDOW_WIDTH);
   			resourceWindows.put(resource.getName(), window);
	   			
   			// registers the window as a listener of the allocation policy.
   			listeners.put(resource.getAllocationPolicy().get_id(), window);
   			windowId++;
   		}
	}

	/*
	 * Creates the menu bar of the main window
	 */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menuCommand = new JMenu("Start");
		JMenuItem item = null;

		for(ParallelResource rs : resources){
			item = new JMenuItem(rs.getName());
			item.addActionListener(this);
			menuCommand.add(item);
		}
									
		menuCommand.addSeparator();
		item = new JMenuItem("Exit");
		item.addActionListener(this);
		menuCommand.add(item);
		menuBar.add(menuCommand);
		setJMenuBar(menuBar);
	}
	
	/*
	 * This method checks whether this is the first time a button is clicked.
	 * @return true if it was the first click.
	 */
	private boolean checkFirstClick() {
		if(firstClick) {
    		firstClick = false;
    		simThread.start();
    		return true;
    	}
		return false;
	}
	
	/*
	 * Updates the resource details
	 */
	private void updateResourceDetails(String resourceName) {
		GridResource resource = null;
		for (GridResource tmpRes : resources) {
			if(tmpRes.getName().equals(resourceName)) {
				resource = tmpRes;
				break;
			}
		}

		if(resource == null) {
			return;
		}
		
		ResourceCharacteristics charact = resource.getResourceCharacteristics();
		String descr = "Resource ID: " + charact.getResourceID() + "\n" +
			"Number of PEs: " + charact.getNumPE() + "\n" +
			"Allocation Policy: " + resource.getAllocationPolicy() + "\n" +
			"Time Zone: " + charact.getResourceTimeZone() + "\n" +
			"Rating per PE: " + charact.getMIPSRatingOfOnePE() + " MIPS";
		jlResourceInfo.setText(descr);
	}
}
