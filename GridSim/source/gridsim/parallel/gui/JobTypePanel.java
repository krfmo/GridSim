/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

/**
 * Panel with a legend with information about the types of possible
 * statuses of jobs and advance reservations in GridSim.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public class JobTypePanel extends JPanel {
	private static final long serialVersionUID = 2840147863520739914L;
	private static GUISettings settings = GUISettings.getInstance();
	private Color[] cQueued = settings.getJobQueuedColors();
	private Color[] cDone = settings.getJobDoneColors();
	private Color[] cInExec = settings.getJobInExecColors();
	private Color[] cARNonComm = settings.getARNonCommittedColors();
	private Color[] cARComm = settings.getARCommittedColors();
	private Color[] cARExec = settings.getARInProgressColors();
	
	/**
	 * Default constructor.
	 */
	public JobTypePanel() {
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2D = (Graphics2D)g;
		g.setFont(settings.getGraphFont());
		
		int panelHeight = super.getHeight();
		int panelWidth = super.getWidth();

		int incY = (int)(((float)panelHeight - 10) / 3);
		int startY = 10;
		int startX = 15;
		int rectWidth = 5;
		int rectHeight = 15;
		
		drawRectColor(g2D, cQueued, rectWidth, rectHeight, startX, startY);
		g2D.setColor(Color.BLACK);
		g2D.drawString("Waiting jobs", startX + 10 + 
				(cQueued.length * rectWidth), startY + rectHeight - 3);
		
		startY += incY;
		drawRectColor(g2D, cDone, rectWidth, rectHeight, startX, startY);
		g2D.setColor(Color.BLACK);
		g2D.drawString("Jobs/ARs completed", startX + 10 + 
				(cDone.length * rectWidth), startY + rectHeight - 3);
		
		startY += incY;
		drawRectColor(g2D, cInExec, rectWidth, rectHeight, startX, startY);
		g2D.setColor(Color.BLACK);
		g2D.drawString("Jobs in execution", startX + 10 + 
				(cInExec.length * rectWidth), startY + rectHeight - 3);
		
		startY = 10;
		startX = 15 + (int)((float)panelWidth / 2);

		drawRectColor(g2D, cARNonComm, rectWidth, rectHeight, startX, startY);
		g2D.setColor(Color.BLACK);
		g2D.drawString("ARs not confirmed", startX + 10 + 
				(cARNonComm.length * rectWidth), startY + rectHeight - 3);
		
		startY += incY;
		drawRectColor(g2D, cARComm, rectWidth, rectHeight, startX, startY);
		g2D.setColor(Color.BLACK);
		g2D.drawString("ARs confirmed", startX + 10 + 
				(cARComm.length * rectWidth), startY + rectHeight - 3);
		
		startY += incY;
		drawRectColor(g2D, cARExec, rectWidth, rectHeight, startX, startY);
		g2D.setColor(Color.BLACK);
		g2D.drawString("ARs in progress", startX + 10 + 
				(cARExec.length * rectWidth), startY + rectHeight - 3);
	}
	
	/*
	 * Draws a small rectangle with the possible colours of the jobs
	 */
	private void drawRectColor(Graphics2D g2D, Color[] colors, 
			int rectWidth, int rectHeight, int startX, int startY) {
		
		for (Color color : colors) {
			g2D.setColor(color);
			g2D.fillRect(startX, startY, rectWidth, rectHeight);
			startX += rectWidth;
		}
	}
}
