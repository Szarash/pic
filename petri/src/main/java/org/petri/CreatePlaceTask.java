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
 * Used for creating places within already existing Petri Nets
 * @author M. Gehrmann, M.Kirchner
 *
 */
public class CreatePlaceTask extends AbstractTask {
	
	private CyNetwork petriNet;
	private CyNetworkViewManager cnvm;
	@Tunable(description="Name of new Place", groups="Name")
	public String name;
	@Tunable(description="Initial amount of Tokens", groups="Tokens")
	public String tokens;

	/**
	 * Constructor
	 * @param petriNet Petri Net within which edge is to be created
	 * @param cnvm Used to update view of newly created edge
	 */
	public CreatePlaceTask(CyNetwork petriNet, CyNetworkViewManager cnvm) {
		this.petriNet = petriNet;
		this.cnvm = cnvm;
	}

	/**
	 * Check whether all input values are correct. If so, create a new place using them.
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {
		if (name.equals("") || tokens.equals("")) { // Empty input values
			JFrame f = new JFrame("Error during place creation");
			String msg = "Missing input values";
			JOptionPane.showMessageDialog(f, msg);
			return;
		}
		boolean invalidTokens = PetriUtils.not_int(tokens); // Check for non-int token values
		if (invalidTokens || Integer.parseInt(tokens) < 0) { // non-int or negative token values
			JFrame f = new JFrame("Error during place creation");
			String msg = "Invalid token amount";
			JOptionPane.showMessageDialog(f, msg);
			return;
		}
		int length = 0;
		int currMax = 0;
		for (CyNode n : petriNet.getNodeList()) {	// Get length of array for internal id
			String ntype = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class));
			if (ntype.equals("Place")) {
				length++;
				String intID = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("internal id", String.class)); 
				String intIDprocess = intID.substring(1);
				int intIDint = Integer.parseInt(intIDprocess);
				if (intIDint > currMax) {
					currMax = intIDint;
				}
			}
		}
		// Create the node and fill in its attributes
		CyNode place = petriNet.addNode();
		petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("internal id", "p"+Integer.toString(currMax + 1));
		petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("type", "Place");
		petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("name", name);
		petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("id", name);
		petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("initial tokens", Integer.parseInt(tokens));
		petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("tokens", Integer.parseInt(tokens));
		// Update view of newly created node
		CyNetworkView [] cnvs = new CyNetworkView[1];
		cnvm.getNetworkViews(petriNet).toArray(cnvs);
		CyNetworkView cnv = cnvs[0];
		cnv.updateView();
		View <CyNode> view = cnv.getNodeView(place);
		view.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
		view.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 35.0);
		view.setLockedValue(BasicVisualLexicon.NODE_LABEL,
				petriNet.getDefaultNodeTable().getRow(view.getModel().getSUID()).get("name", String.class)+"\n"
				+ Integer.toString(petriNet.getDefaultNodeTable().getRow(view.getModel().getSUID()).get("tokens", Integer.class)));
	}
}
