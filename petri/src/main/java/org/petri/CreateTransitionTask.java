package org.petri;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * Used for creating transition within already existing Petri Nets
 * @author M. Gehrmann, M.Kirchner
 *
 */
public class CreateTransitionTask extends AbstractTask {
	
	private CyNetwork petriNet;
	private CyNetworkViewManager cnvm;
	@Tunable(description="Name of new Transition")
	public String name;

	/**
	 * Constructor
	 * @param petriNet Petri Net within which transition is to be created
	 * @param cnvm Used to update view of newly created transition
	 */
	public CreateTransitionTask(CyNetwork petriNet, CyNetworkViewManager cnvm) {
		this.petriNet = petriNet;
		this.cnvm = cnvm;
	}

	/**
	 * Check whether all input values are correct. If so, create a new transition using them.
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {
		if (name.equals("")) { // empty input value
			JFrame f = new JFrame("Error during transition creation");
			String msg = "Missing input values";
			JOptionPane.showMessageDialog(f, msg);
			return;
		}
		int length = 0;
		for (CyNode n : petriNet.getNodeList()) {	// Get length of array for internal id
			String ntype = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class));
			if (ntype.equals("Transition")) {
				length++;				
			}
		}
		// Create new node and fill in its attributes
		CyNode transition = petriNet.addNode();
		petriNet.getDefaultNodeTable().getRow(transition.getSUID()).set("internal id", "t"+Integer.toString(length));
		petriNet.getDefaultNodeTable().getRow(transition.getSUID()).set("type", "Transition");
		petriNet.getDefaultNodeTable().getRow(transition.getSUID()).set("name", name);
		petriNet.getDefaultNodeTable().getRow(transition.getSUID()).set("id", name);
		petriNet.getDefaultNodeTable().getRow(transition.getSUID()).set("fired", 0);
		// Update view of newly created node
		CyNetworkView [] cnvs = new CyNetworkView[1];
		cnvm.getNetworkViews(petriNet).toArray(cnvs);
		CyNetworkView cnv = cnvs[0];
		cnv.updateView();
		View <CyNode> view = cnv.getNodeView(transition);
		view.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
		view.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 35.0);
	}
}
