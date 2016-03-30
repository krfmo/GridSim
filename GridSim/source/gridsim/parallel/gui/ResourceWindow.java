/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.parallel.profile.PERange;
import gridsim.parallel.profile.PERangeList;
import gridsim.parallel.profile.ScheduleItem;
import gridsim.parallel.reservation.ReservationStatus;

/**
 * {@link ResourceWindow} class represents the window that shows the 
 * scheduling queue of a given resource allocation policy. This interface 
 * was initially created for PajFit (<b>http://pajfit.sourceforge.net/</b>).
 *
 * @author Marco A. S. Netto and Marcos Dias de Assuncao
 * 
 * @since 5.0
 */
public class ResourceWindow extends JFrame 
								implements AllocationListener,
								ActionListener {
	
	private static final long serialVersionUID = 4453814344309889376L;
    private int numPE;   // number of processing elements the Grid resource has
	
	// default control options included in the left side of the window
	private JSlider sliderX, sliderY;
	private JRadioButtonMenuItem btSecond, btMinute, btHour;
	private boolean drawID_ = true;
	private boolean autoScroll_ = true;
	private boolean animate_ = true;
	private boolean showPartition_ = false;
	
	private JButton btSetSdWindowSize;
	private JTextField fdSdWindowSize;
	private double slidingWindowSize = Double.MAX_VALUE;
	
	// the left panel itself, the scroller for the scheduling queue panel
	// and the panel where the jobs are drawn
	private JComponent pnLeft;
	private JScrollPane sclGraph;
	private GraphPanel pnGraph;
	private double currentTime;

	// the panel that shows the list of gridlets or advance reservations
	private ItemPanel pnItem;
	private JobTypePanel pnColor;
	
	// the jobs or advance reservations displayed by this window
	private ArrayList<ScheduleItem> scheduledItems = new ArrayList<ScheduleItem>();
	
	// the settings object
	private static GUISettings settings = GUISettings.getInstance();
	
	// time unit used to display information on the screen
	private int timeUnit = ScheduleItem.TIME_UNIT_SECOND;
	
	private static final int WINDOW_WIDTH = 900;
	private static final int WINDOW_HEIGHT = 350;
	private static final int SHIFT_X = 30;
	private static final int SHIFT_Y = 25;
	private static final int SHIFT_BOTTOM = 25;
	private static final float PROPORTION_LEFT_PANEL = 0.6f;
	private static final float PROPORTION_RIGHT_PANEL = 1f - PROPORTION_LEFT_PANEL;
	private static final int HEIGHT_COLOR_PANEL = 90;
	
	/**
	 * Creates the scheduling window.
	 * @param resource the characteristics of the grid resource
	 * @param windowId an id for the window 
	 * @param hPos Horizontal position of the window
	 */
	public ResourceWindow(GridResource resource, int windowId, int hPos) {			
		numPE = resource.getResourceCharacteristics().getNumPE();
		super.getContentPane().setLayout(null);
		super.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);

		// initialise the left and right panels
		initPanels();
		FrameResizer adapter = new FrameResizer();
        super.addComponentListener(adapter);
		
		super.setLocation(hPos, windowId * 200);
		super.setTitle("Resource Information Window - " + resource.get_name());
	}
	
	// -------------------------- PUBLIC METHODS -----------------------
	
	/**
	 * Handles allocation actions
	 * @param action an allocation action performed
	 * @return <code>true<code> if action was handled
	 */
	public boolean allocationActionPerformed(AllocationAction action) {
		ActionType type = action.getActionType();
		LinkedList<ScheduleItem> list = action.getScheduleItems();
		double previousTime = currentTime;
		currentTime = GridSim.clock();
		
		if (type == ActionType.ITEM_ARRIVED) {
				for(ScheduleItem item : list){
					scheduledItems.add(item);
					pnItem.insertNewItem(item);
				}
				updateResourceWindow();
		} 
		else if (type == ActionType.ITEM_STATUS_CHANGED) {
				pnItem.updateItem(list.getLast());
				updateResourceWindow();
		} 
		else if (type == ActionType.ITEM_SCHEDULED) {
				for(ScheduleItem item : list){
					long finishTime = (long)item.getActualFinishTime();
					settings.setTimeSpan(finishTime);
				}
				pnItem.updateItem(list.getLast());
				updateResourceWindow();
		} 
		else if (type == ActionType.ITEM_CANCELLED) {
				for(ScheduleItem item : list){
					scheduledItems.remove(item);
				}
				pnItem.updateItem(list.getLast());
				updateResourceWindow();
		} 
		else if (type == ActionType.SIMULATION_TIME_CHANGED) {
				if(currentTime > previousTime) {
					updateResourceWindow();
				}
		} 
		else {
			updateResourceWindow();
		}
		
		return true;
	}
	
	/**
	 * Handles the action events triggered by interface components
	 * @param e the event received
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btSecond && btSecond.isSelected()) {
			timeUnit = ScheduleItem.TIME_UNIT_SECOND;
		}
		else if (e.getSource() == btMinute && btMinute.isSelected()) {
			timeUnit = ScheduleItem.TIME_UNIT_MINUTE;
		}
		else if (e.getSource() == btHour && btHour.isSelected()) {
			timeUnit = ScheduleItem.TIME_UNIT_HOUR; 
		}
		else if(e.getSource() == btSetSdWindowSize) {
			double newSize = 0; boolean success = true;
			try {
				newSize = Double.parseDouble(fdSdWindowSize.getText());
				if(newSize >= 60)  {
					slidingWindowSize = newSize;
				} else {
					success = false;
				}
			}
			catch (NumberFormatException nfe) {
				success = false;
			}
			if(!success) {
				String message = "The value informed for the size of the " +
				"sliding window is invalid.\nThe " + 
				(Double.compare(slidingWindowSize, Double.MAX_VALUE) == 0 ? "default" : "current") +
				" value will be used instead.\n\n" +
				"Note: the minimum size is 60 seconds.";
			
			JOptionPane.showMessageDialog(this, message, 
					"Error Setting the Sliding Window Size", 
					JOptionPane.ERROR_MESSAGE);

			}
		}
		
		if(scheduledItems.size() > 0) {
			pnItem.updatePanel();
		}
		
		updateResourceWindow();
	}

	// -------------------------- PRIVATE METHODS -----------------------
	
	/**
	 * Initialises the panels. That is, the left side where the control buttons
	 * and the panel where the scheduling queue is shown and the right side
	 * where the information about the gridlets is displayed
	 */
	private void initPanels() {
		
		// calculates the size of the two panels 
		// to be added to the window
		int leftPanelWidth = (int)((super.getWidth()) * PROPORTION_LEFT_PANEL);
		int panelsHeight = (int)(((float)super.getHeight()) - 40);
		int gridletPanelWidth = (int)((super.getWidth()) * PROPORTION_RIGHT_PANEL) - 10;
		int leftPanelXPos = 0;
		int gridletPanelXPos = leftPanelXPos + leftPanelWidth;
		
		pnLeft = new JPanel();
		pnLeft.setOpaque(true);
		pnLeft.setLayout(new BorderLayout());
		pnLeft.setLocation(leftPanelXPos, 0);
		pnLeft.setSize(leftPanelWidth, panelsHeight);

		Border raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		JPanel instructionPanel = new JPanel();
		instructionPanel.setLayout(new BoxLayout(instructionPanel, BoxLayout.X_AXIS));
		instructionPanel.setBorder(raisedetched);
		
		JPanel sliderPanel = new JPanel(new GridLayout(1, 2));
		sliderPanel.setBorder(new TitledBorder("Scale X and Y Axes"));
		sliderX = new JSlider(10, 100, 10);
		sliderY = new JSlider(10, 100, 10);
		
		ChangeListener graphResizer = new ChangeListener() {
			public synchronized void stateChanged(ChangeEvent e) {
				pnGraph.repaint();
			}
		};
		
		sliderX.addChangeListener(graphResizer);
		sliderY.addChangeListener(graphResizer);
		sliderPanel.add(sliderX);
		sliderPanel.add(sliderY);
		
		JPanel pnWindowProp = new JPanel(new GridLayout(1, 2));
		pnWindowProp.setBorder(new TitledBorder("Sliding Window Size (Sec.):"));
		
		fdSdWindowSize = new JTextField(8);
		pnWindowProp.add(fdSdWindowSize);
		
		btSetSdWindowSize = new JButton("Change");
		btSetSdWindowSize.addActionListener(this);
		pnWindowProp.add(btSetSdWindowSize);
		
		instructionPanel.add(sliderPanel);
		instructionPanel.add(pnWindowProp);
		
		//Set up the drawing area.
		pnGraph = new GraphPanel();

		//Put the drawing area in a scroll pane.
		sclGraph = new JScrollPane(pnGraph);
		pnGraph.setBorder(new BevelBorder(BevelBorder.LOWERED));
		
		pnLeft.add(instructionPanel, BorderLayout.NORTH);
		pnLeft.add(sclGraph, BorderLayout.CENTER);
        
		Border paneBorder = BorderFactory.createEmptyBorder(10, 20, 10, 20);
		pnLeft.setBorder(paneBorder);
		
		pnItem = new ItemPanel();
		pnItem.setLocation(gridletPanelXPos, 0);
		pnItem.setSize(gridletPanelWidth, panelsHeight - HEIGHT_COLOR_PANEL - 10);
		
		pnColor = new JobTypePanel();
		pnColor.setLocation(gridletPanelXPos, pnItem.getHeight());
		pnColor.setSize(gridletPanelWidth, HEIGHT_COLOR_PANEL);
		
		this.getContentPane().add(pnLeft);
		this.getContentPane().add(pnItem);
		this.getContentPane().add(pnColor);
		
		createMenuBar();
		
		pnItem.setMinimumSize(new Dimension( (int)(WINDOW_WIDTH/2.7),
				(int)(super.getMaximumSize().height)));
	}
	
	/**
	 * Creates the menu bar of the main window
	 */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menuCommand = new JMenu("Options");
	
		JMenu mnGridlet = new JMenu("Gridlet");
		JCheckBoxMenuItem miShowGridID = new JCheckBoxMenuItem("Show ID");
		miShowGridID.setSelected(true);
		mnGridlet.add(miShowGridID);
		
		miShowGridID.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					drawID_ = false;
				}
				else if (e.getStateChange() == ItemEvent.SELECTED){
					drawID_ = true;
				}
				pnGraph.repaint();
			}
		});
		
		menuCommand.add(mnGridlet);
		
		JMenu mnTime = new JMenu("Time Unit");
		btSecond = new JRadioButtonMenuItem("Second");
		btSecond.setActionCommand("time_second");
		btSecond.setSelected(true);

		btMinute = new JRadioButtonMenuItem("Minute");
		btMinute.setActionCommand("time_minutes");
	    
		btHour = new JRadioButtonMenuItem("Hour");
		btHour.setActionCommand("time_hour");

		ButtonGroup timeButtonGroup = new ButtonGroup();
		timeButtonGroup.add(btSecond);
		timeButtonGroup.add(btMinute);
		timeButtonGroup.add(btHour);
		
		btSecond.addActionListener(this);
		btMinute.addActionListener(this);
		btHour.addActionListener(this);
		
		mnTime.add(btSecond);
		mnTime.add(btMinute);
		mnTime.add(btHour);
		
		menuCommand.add(mnTime);
		
		JMenu mnScroll = new JMenu("Scrolling");

		JCheckBoxMenuItem miAutoScroll = new JCheckBoxMenuItem("Auto Scroll to End of Queue");
		miAutoScroll.setSelected(true);
		mnScroll.add(miAutoScroll);
		
		miAutoScroll.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					autoScroll_ = false;
				}
				else if (e.getStateChange() == ItemEvent.SELECTED){
					autoScroll_ = true;
				}
				updateResourceWindow();
			}
		});
		
		menuCommand.add(mnScroll);
		
		JMenu mnAnimation = new JMenu("Animation");
		JCheckBoxMenuItem miAnimation = new JCheckBoxMenuItem("Animate this Window");
		miAnimation.setSelected(true);
		mnAnimation.add(miAnimation);
		
		miAnimation.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					animate_ = false;
				}
				else if (e.getStateChange() == ItemEvent.SELECTED){
					animate_ = true;
				}
				updateResourceWindow();
			}
		});
		
		menuCommand.add(mnAnimation);
		
		JMenu mnPartition = new JMenu("Partitions");
		JCheckBoxMenuItem miPartition = new JCheckBoxMenuItem("Show Partition Informations");
		miPartition.setSelected(false);
		mnPartition.add(miPartition);
		
		miPartition.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					showPartition_ = false;
				}
				else if (e.getStateChange() == ItemEvent.SELECTED){
					showPartition_ = true;
				}
				updateResourceWindow();
			}
		});
		
		menuCommand.add(mnPartition);
		
		menuBar.add(menuCommand);
		setJMenuBar(menuBar);
	}
	
	private void updateResourceWindow() {
		pnGraph.repaint();
		
		if(slidingWindowSize != Double.MAX_VALUE) {
			int max = sclGraph.getHorizontalScrollBar().getMaximum();
			if(autoScroll_) {
				Rectangle visRect = sclGraph.getVisibleRect();
				Rectangle rect = new Rectangle(max - visRect.width, 
						0, visRect.width, sclGraph.getHeight());  
				sclGraph.getHorizontalScrollBar().setValue(max - visRect.width);
				sclGraph.scrollRectToVisible(rect);
			}
		}
	}
	
	/*
	 * Converts the time to the time unit in use
	 * @param time in seconds
	 * @return the time in the unit in use
	 */
	private double convertTime(double time) {
		return time / timeUnit; 
	}
	
	// -------------------------- PRIVATE CLASSES -----------------------
	
	/**
	 * Class responsible for resizing the two main panels
	 * that compose the resource window interface
	 */
	class FrameResizer extends ComponentAdapter {
		
        public void componentResized(ComponentEvent evt) {
       		
    		// calculates the size of the two panels 
    		// to be added to the window
    		int leftPanelWidth = (int)((ResourceWindow.this.getWidth()) * PROPORTION_LEFT_PANEL);
    		int panelsHeight = (int)((ResourceWindow.this.getHeight()) - 40);
    		int gridletPanelWidth = (int)((ResourceWindow.this.getWidth()) * PROPORTION_RIGHT_PANEL) - 10;
    		int leftPanelXPos = 0;
    		int gridletPanelXPos = leftPanelXPos + leftPanelWidth;
    		
       		pnLeft.setLocation(leftPanelXPos, 0);
       		pnLeft.setSize(leftPanelWidth, panelsHeight);
       		pnLeft.updateUI();
        		
       		pnItem.setLocation(gridletPanelXPos, 0);
       		pnItem.setSize(gridletPanelWidth, panelsHeight - HEIGHT_COLOR_PANEL - 10);
       		pnItem.updateUI();
       		
    		pnColor.setLocation(gridletPanelXPos, pnItem.getHeight());
    		pnColor.setSize(gridletPanelWidth, HEIGHT_COLOR_PANEL);
    		pnColor.updateUI();
        }
    }
	
	/** 
	 * The panel inside the scroll pane where the jobs are shown.
	 */  
	class GraphPanel extends JPanel {
		
		private int panelHeight_;
		private int panelWidth_;
		private float scaleY_;
		private float scaleX_;
		
		private BasicStroke dashedStk = settings.getDashedStroke();
		private BasicStroke normalStk = settings.getNormalStroke();
		private Composite transpComp = settings.getTransparentComposite();
		
		private Color bgColor = settings.getGraphBGColor();
		private Color color = settings.getGraphAreaColor();
		private Color bdColor = settings.getGraphBDColor();
		private Color gridColor = settings.getTimeGridColor();
		private Color topTxtColor = settings.getLabelColor();
		private Color xTxtColor = settings.getXAxisTextColor();
		private Color ctLnColor = settings.getTimeLineColor();
		private Font grFont = settings.getGraphFont();
		
		private Color[] colorsQueued = settings.getJobQueuedColors();
		private Color[] colorsDone = settings.getJobDoneColors();
		private Color[] colorsInExec = settings.getJobInExecColors();
		private Color[] colorsARNonCommitted = settings.getARNonCommittedColors();
		private Color[] colorsARCommitted = settings.getARCommittedColors();
		private Color[] colorsARInProgress = settings.getARInProgressColors();
		private Color[] colorQueues = settings.getQueueColors();

		// a job to be highlited. That is, a gridlet or advance reservation 
		// selected by the user on the right panel
		ScheduleItem hlItem = null; 
		
		protected GraphPanel() {
			super.setBackground(bgColor);
		}

		protected synchronized void paintComponent(Graphics g2) {
			if(!animate_) {
				return;
			}
			
			super.paintComponent(g2);
			Graphics2D g2D = (Graphics2D)g2; 
			g2D.setFont(grFont);
			
			double timeSpan = settings.getTimeSpan();
                				         				
			panelHeight_ = pnLeft.getHeight() - 100 -  SHIFT_Y - SHIFT_BOTTOM;
			int minWidth = pnLeft.getWidth() - 50 - 2 * SHIFT_X;
			panelWidth_ = minWidth; 
			
			double sdWindowSize = ResourceWindow.this.slidingWindowSize; 
			if(Double.compare(sdWindowSize, Double.MAX_VALUE) != 0) {
				panelWidth_ = (int)(minWidth * (settings.getTimeSpan() / sdWindowSize)); 
			}
			
			panelWidth_ = (panelWidth_ < minWidth) ? minWidth : panelWidth_;  

			scaleY_ = panelHeight_ / (float) numPE;
			scaleX_ = panelWidth_ / (float) (timeSpan);

			scaleY_ *= sliderY.getValue() * (float) 0.1;
			scaleX_ *= sliderX.getValue() * (float) 0.1;

			super.setPreferredSize(new Dimension((int) (timeSpan * scaleX_) + 2 * SHIFT_X,
					(int) ((numPE) * scaleY_) + SHIFT_Y + SHIFT_BOTTOM));

			drawSchedulingQueue(timeSpan, g2D);
			drawGridsAndAxes(timeSpan, g2D);
			super.revalidate();
		}
			
		/**
		 * Draws the lines and time scale on the scheduling window
		 * @param timeSpan the time span of the simulation
		 * @param g2D the graphics 2D context
		 */
		private void drawGridsAndAxes(double timeSpan, Graphics2D g2D) {

			String text = null;
			FontMetrics metrics = g2D.getFontMetrics();
			g2D.setColor(gridColor);
			g2D.setStroke(dashedStk);
			
			Composite previousComposite = g2D.getComposite();
			g2D.setComposite(transpComp);
			
			int heightGph = (int)(numPE * scaleY_);
			int widthGph = (int)(timeSpan * scaleX_);
			int x = SHIFT_X, y = SHIFT_Y;
			
			for(int i=0; i<=widthGph; i+=50) {
				x = SHIFT_X + i;
				g2D.drawLine(x, SHIFT_Y, x, SHIFT_Y + heightGph);
			}
			
			g2D.setComposite(previousComposite);
			g2D.setStroke(normalStk);
			
			g2D.setColor(bdColor);
			g2D.drawRect(SHIFT_X, SHIFT_Y, widthGph, heightGph);
			
			for(int i=0; i <= widthGph; i+=50) {
				x = SHIFT_X + i;
				g2D.drawLine(x, SHIFT_Y + heightGph - 5, x, SHIFT_Y + heightGph + 3);
			}

			g2D.setColor(xTxtColor);
			y = SHIFT_Y + heightGph + 20;
			for(int i=0; i<=widthGph; i+=50) {
				text = "" + (int)convertTime((i/scaleX_));
				g2D.drawString(text, SHIFT_X + i - 
						SwingUtilities.computeStringWidth(metrics, text) / 2, y);
			}

			g2D.setColor(topTxtColor);
			text = "CT: "+ (int)(convertTime(currentTime));
			g2D.drawString(text, SHIFT_X + (int)(currentTime * scaleX_) - 
					SwingUtilities.computeStringWidth(metrics, text) / 2, 
					SHIFT_Y - 10);
		
			text = "Time Span: " + (int)(convertTime(timeSpan));
			y = SHIFT_Y  + SwingUtilities.computeStringWidth(metrics, text);
			x = widthGph + SHIFT_X + 15;
			rotateAndPaint(g2D, x, y, 1.571, text);
			
			x = SHIFT_X - 5;
			y = heightGph + SHIFT_Y - 10;
			rotateAndPaint(g2D, x, y, 1.571, " Processing Elements: " + numPE);
			
			g2D.setColor(ctLnColor);
			x = SHIFT_X + (int)(currentTime * scaleX_);
			g2D.drawLine(x, SHIFT_Y - 7, x, SHIFT_Y + heightGph + 10);
		}
		
		/*
		 * Rotate the graphics, print the string and rotate the graphics back
		 */
		private void rotateAndPaint(Graphics2D g2D, 
				int x, int y, double theta, String text) {
			g2D.rotate(-theta, x, y);
			g2D.drawString(text, x, y);
			g2D.rotate(theta, x, y);
		}
			
		/*
		 * Draws the boxes representing the gridlets or advance reservations 
		 */
		private void drawSchedulingQueue(double timeSpan, Graphics2D g2D) {
				
			Color boxColor = null;
			Color fontColor = null;
			
			g2D.setColor(color);
			int heightGph = (int)(numPE * scaleY_);
			int widthGph = (int) (timeSpan * scaleX_);
			g2D.fillRect(SHIFT_X, SHIFT_Y, widthGph, heightGph);
			
			int size = scheduledItems.size();
				
			for(int i=0; i<size; i++) {
				ScheduleItem item = (ScheduleItem)scheduledItems.get(i);
				if(item == null || item.getStartTime() < 0) {
					continue;
				}
				
               	int itemId = item.getID();
				if (item.getPERangeList() != null) {
					// the color of the font for normal gridlets is black
					fontColor = Color.BLACK;
					if(showPartition_) {
						boxColor = colorQueues[item.getPartitionID() % colorQueues.length];
					}
					else if(!item.isAdvanceReservation()) {
						// Gridlet is in execution
						if(item.getStatus() == Gridlet.INEXEC) {
							boxColor = colorsInExec[(itemId % colorsInExec.length)];
						}
						// Gridlet has finished
						else if(item.getStatus() == Gridlet.SUCCESS) {
							boxColor = colorsDone[(itemId % colorsDone.length)];
						}
						else {				
							boxColor = colorsQueued[(itemId % colorsQueued.length)];
						}
					}
					else {
						// the color of the font for advance reservations is white
						fontColor = Color.WHITE;
						
						if(item.getStatus() == ReservationStatus.IN_PROGRESS.intValue()) {
							boxColor = colorsARInProgress[(itemId % colorsARInProgress.length)];
						}
						else if (item.getStatus() == ReservationStatus.NOT_COMMITTED.intValue() || 
								item.getStatus() == ReservationStatus.UNKNOWN.intValue()) {
							boxColor = colorsARNonCommitted[(itemId % colorsARNonCommitted.length)];
						}
						else if (item.getStatus() == ReservationStatus.COMMITTED.intValue()) {
							boxColor = colorsARCommitted[(itemId % colorsARCommitted.length)];
						}
						else if (item.getStatus() == ReservationStatus.FINISHED.intValue()) {
							boxColor = colorsDone[(itemId % colorsDone.length)];
						}
						else {
							boxColor = colorsDone[(itemId % colorsDone.length)];
						}
					}
					drawItem((Graphics2D) g2D, item, boxColor, fontColor);						
				}
			}
			
			// if there is an item to be highlighted, then do it
			if(hlItem != null) {
				highlightItem((Graphics2D) g2D, hlItem);
			}
		}

	    /*
		 * Draws a gridlet or advance reservation in the scheduling window. 
		 * This method assumes that the gridlet has a range of PEs
		 */
		private void drawItem(Graphics2D g2D, ScheduleItem item, Color boxColor, 
				Color fontColor) {
			int y;
			int h = 0; //controls the height to draw the gridlet
			PERangeList gridletPERanges = item.getPERangeList();
			gridletPERanges.sortRanges();
				
			int firstX, firstY;
			int width, height;
			int textX, textY;
			int textHeight, textWidth;
				
			// gets the time duration of the gridlet
			double duration = item.getActualFinishTime() - item.getStartTime(); 
				
			width = (int) (duration * scaleX_);
			firstX = SHIFT_X + (int) (item.getStartTime() * scaleX_);
				
			String boxText;
			LineMetrics lineMetrics;
				
			Font font = g2D.getFont().deriveFont(10f);
		    g2D.setFont(font);
		    FontRenderContext frc = g2D.getFontRenderContext();
		        
		    // A gridlet can have the nodes 0-2, 5-7, etc. 
		    // So it must be painted in parts
		    for(PERange range : gridletPERanges){
	       	    y = range.getEnd();
	       	    h = range.getNumPE();
		        
				firstY = SHIFT_Y + (int) ((numPE - (y + 1)) * scaleY_);
				height = (int) ((h) * scaleY_);
	
				// if it is a gridlet that reserved resources, then make it
				// transparent to show the advance reservation as well
				boolean reservedGridlet = !item.isAdvanceReservation() && item.hasReserved();
				Composite previousComposite = null;
				if(reservedGridlet) {
					previousComposite = g2D.getComposite();
					g2D.setComposite(transpComp);
				}
	
				g2D.setColor(boxColor);
				if(!reservedGridlet)
					g2D.fillRect(firstX, firstY, width, height);
	
				g2D.setColor(Color.black);
				g2D.drawRect(firstX, firstY, width, height);
	
				//draw the label in the center of the box
				boxText = new Integer(item.getID()).toString();
		        textWidth = (int)font.getStringBounds(boxText, frc).getWidth();
		        lineMetrics = font.getLineMetrics(boxText, frc);
		        textHeight = (int)(lineMetrics.getAscent() + lineMetrics.getDescent());
	
		        textX = firstX + (width - textWidth)/2;
		        textY = (int)(firstY + (height + textHeight)/2 - lineMetrics.getDescent());
	
		        g2D.setColor(fontColor);
		        if(drawID_){
		        	g2D.drawString(boxText, textX, textY);
		        }
		        
				if(reservedGridlet) {
					g2D.setComposite(previousComposite);
				}
			}
		}
		
		/*
		 * Highlights a schedule item. This method basically draws the item
		 * in the resource window with red lines.
		 */
		private void highlightItem(Graphics2D g2D, ScheduleItem item) {

			int y;
			int h = 0; //controls the height to draw the gridlet
			PERangeList gridletPERanges = item.getPERangeList();
			
			if(gridletPERanges == null)
				return;
			
			gridletPERanges.sortRanges();
				
			int firstX, firstY;
			int width, height;
				
			// gets the time duration of the gridlet
			double duration = item.getActualFinishTime() - item.getStartTime(); 
				
			width = (int) (duration * scaleX_);
			firstX = SHIFT_X + (int) (item.getStartTime() * scaleX_);
				
		    // A gridlet can have the nodes 0-2, 5-7, etc. 
		    // So it must be painted in parts
		    for(PERange range : gridletPERanges){
	       	    y = range.getEnd();
	       	    h = range.getNumPE();
		        
				firstY = SHIFT_Y + (int) ((numPE - (y + 1)) * scaleY_);
				height = (int) ((h) * scaleY_);
	
				g2D.setColor(Color.RED);
				g2D.drawRect(firstX, firstY, width, height);
			}
		}
	}
	
	/**
	 * This class corresponds to the panel that contains information about the 
	 * {@link ScheduleItem}s (i.e. Gridlets and Reservations) received by a Grid 
	 * resource. This panel displays the list of items. If the user clicks on an
	 * item, additional information is shown. <br>
	 * This interface was initially created for another simulator called PajFit
	 * available at <b>http://pajfit.sourceforge.net/</b>.
	 *
	 * @author	Marco A. S. Netto (created this class)
	 * @author  Marcos Dias de Assuncao (modified this class to be used by GridSim
	 * 				and receive updates from the {@link ResourceWindow})
	 * 
	 * @since GridSim Turbo Alpha 0.1
	 * @see ResourceWindow
	 */

	class ItemPanel extends JPanel implements ListSelectionListener {
		
		private JList itemQueueJList_;
		private JTextArea itemInfoArea_;
		private ItemListModel itemModel_ = new ItemListModel();
		
		protected ItemPanel() {
	        
			// creates the list that contains the gridlets
			itemQueueJList_ = new JList();
			itemQueueJList_.setModel(itemModel_);
			itemQueueJList_.addListSelectionListener(this);
			
			JScrollPane scrollPaneJobs = new JScrollPane();
			scrollPaneJobs.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			
			scrollPaneJobs.setViewportView(itemQueueJList_);
			scrollPaneJobs.setBorder(new TitledBorder("List"));
			super.setLayout(new GridLayout(1, 2));
			
			itemQueueJList_.setFont(itemQueueJList_.getFont().deriveFont(9.5f));
			itemQueueJList_.setBackground(super.getBackground());
			scrollPaneJobs.setBackground(super.getBackground());
					
			// the list that contains the details of an item
			itemInfoArea_ = new JTextArea();
			itemInfoArea_.setFont(itemInfoArea_.getFont().deriveFont(9.5f));

			Border panelBorder = 
				new CompoundBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0),
				    BorderFactory.createTitledBorder(
				    BorderFactory.createEtchedBorder(EtchedBorder.RAISED), 
				    "Information About Gridlets and Reservations"));

			super.setBorder(panelBorder);
			
			itemInfoArea_.setBorder(new TitledBorder("Details"));
			itemInfoArea_.setBackground(super.getBackground());
			itemInfoArea_.setEditable(false);

			super.add(scrollPaneJobs);
			super.add(itemInfoArea_);
		}
		
		/**
		 * Handles events triggered by the change of the list of Gridlets
		 * @see ListSelectionListener#valueChanged(ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent event) {
			int selectedIndex = (int) itemQueueJList_.getSelectedIndex();
			ScheduleItem item = itemModel_.get(selectedIndex);

			if (item != null) {
				updateItemDetails(item);
				pnGraph.hlItem = item;
				pnGraph.repaint();
			}
		}
		
		/**
		 * Inserts a Gridlet to the JList and the vector of Gridlets
		 * @param insertItem the item to be inserted in the vector of
		 * schedule items and the JList
		 */
		public void insertNewItem(final ScheduleItem insertItem) {
			if(insertItem == null)
				return;
			
			itemModel_.addItem(insertItem);
			itemQueueJList_.setSelectedIndex(itemModel_.getSize() - 1);
		    updateItemDetails(insertItem);
		}
		
		/**
		 * Updates information in the list. This method checks whether
		 * the Item is already in the list or not. If it is, just update 
		 * the details window. Otherwise, inserts the item in
		 * the list and updates the details.
		 * @param item the item whose information has to be updated
		 */
		protected void updateItem(ScheduleItem item) {
			if(item == null)
				return;
			
			int position = itemModel_.getPosition(item.getID(), item.getSenderID(), 
					item.isAdvanceReservation());
			
			int selectedIndex = (int) itemQueueJList_.getSelectedIndex();

			if(selectedIndex == position) {
				updateItemDetails(item);
			}
		}
		
		/**
		 * Called when an update of the whole panel is needed
		 */
		protected void updatePanel() {
			int selectedIndex = (int) itemQueueJList_.getSelectedIndex();
			ScheduleItem item = itemModel_.get(selectedIndex);

			if (item != null)
				updateItemDetails(item);
		}
		
		/**
		 * Updates the details about the selected gridlet
		 * in the gridlet details panel
		 */
		private void updateItemDetails(ScheduleItem item){
			itemInfoArea_.setText(item.toString(timeUnit));
		}
		
		// the list model. This is a wrapper around the vector 
		// of elements to be shown in the list
		private class ItemListModel extends AbstractListModel {
			private Vector<ScheduleItem> items_ = new Vector<ScheduleItem>();
			
			public Object getElementAt(int index) {
				ScheduleItem item = items_.get(index);
				return item == null ? "" : itemSummary(item);
			}
			
			// Creates a small summary of an item
			private String itemSummary(ScheduleItem item) {
				return (item.isAdvanceReservation() ? "Res. " : "Grl. ") + "ID: " + item.getID() + 
					", User: " + item.getSenderID();
			}

			public int getSize() {
				return items_.size();
			}
			
			public ScheduleItem get(int index) {
				return items_.get(index);
			}
			
			/*
			 * Gets the position of an item in the list
			 * @param itemId the item id
			 * @param itemId the user id
			 * @param ar <tt>true</tt> if it is an advance reservation or 
			 * <tt>false</tt> otherwise
			 * @return the index of the element or <tt>-1</tt> if not found
			 */
			private int getPosition(int itemId, int userId, boolean ar) {
				int sizeVector = items_.size();
				ScheduleItem item;
				for (int i = 0; i < sizeVector; i++) {
					item = items_.get(i);
					if (item.getID() == itemId 
							&& item.getSenderID() == userId
							&& item.isAdvanceReservation() == ar)
						return i;
				}
				return -1;
			}
			
			/*
			 * Inserts an item to the model
			 * @param insertItem the item to be inserted in the model
			 */
			public void addItem(ScheduleItem insertItem) {
				int index = items_.size();
			    items_.add(index, insertItem);
			    
			    super.fireIntervalAdded(this, index, index);
			}
		} 
	} 
}

