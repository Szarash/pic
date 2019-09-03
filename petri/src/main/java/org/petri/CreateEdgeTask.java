package org.petri;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * Used for creating edges within already existing Petri Nets
 * @author M. Gehrmann, M.Kirchner
 *
 */
public class CreateEdgeTask extends AbstractTask {
	
	private CyNetwork petriNet;
	private CyNetworkViewManager cnvm;
	@Tunable(description="Internal ID of Source Node", groups="Nodes")
	public String sourceID;
	@Tunable(description="Internal ID of Target Node", groups="Nodes")
	public String targetID;
	@Tunable(description="Weight of Edge", groups="Weight")
	public String weight;
	
	/**
	 * Constructor
	 * @param petriNet Petri Net within which edge is to be created
	 * @param cnvm Used to update view of newly created edge
	 */
	public CreateEdgeTask(CyNetwork petriNet, CyNetworkViewManager cnvm) {
		this.petriNet = petriNet;
		this.cnvm = cnvm;
	}

	/**
	 * Check whether all input values are correct. If so, create a new edge using them.
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {
		if (sourceID.equals("") || targetID.equals("") || weight.equals("")) { // Check for empty values
			JFrame f = new JFrame("Error during edge creation");
			String msg = "Missing input values";
			JOptionPane.showMessageDialog(f, msg);
			return;
		} 
		// Check for non-int weights
		boolean invalidWeight = PetriUtils.not_int(weight);
		if (invalidWeight || Integer.parseInt(weight) < 1) { // non-int weights or weight < 1
			JFrame f = new JFrame("Error during edge creation");
			String msg = "Invalid weight";
			JOptionPane.showMessageDialog(f, msg);
			return;
		}
		CyNode source = null; // initialize source and target nodes for edge
		CyNode target = null;
		for (CyNode n : petriNet.getNodeList()) { // search for source and target node by internal id
			if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("internal id", String.class).equals(sourceID)) {
				source = n;
			}
			if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("internal id", String.class).equals(targetID)) {
				target = n;
			}
		}
		if (source == null || target == null) { // one or both of the nodes have not been found
			JFrame f = new JFrame("Error during edge creation");
			String msg = "Source or Target not found";
			JOptionPane.showMessageDialog(f, msg);
			return;
		}
		if (petriNet.getDefaultNodeTable().getRow(source.getSUID()).get("type", String.class).equals( // nodes have same type
				petriNet.getDefaultNodeTable().getRow(target.getSUID()).get("type", String.class))) {
			JFrame f = new JFrame("Error during edge creation");
			String msg = "Source and Target have same type";
			JOptionPane.showMessageDialog(f, msg);
			return;
		}
		// Create new edge and fill ins its attributes
		CyEdge edge = petriNet.addEdge(source, target, true);
		petriNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("internal id", "e"+Integer.toString(petriNet.getEdgeCount()-1));
		petriNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("weight", Integer.parseInt(weight));
		String sourcename = petriNet.getDefaultNodeTable().getRow(source.getSUID()).get("name", String.class);
		String targetname = petriNet.getDefaultNodeTable().getRow(target.getSUID()).get("name", String.class);
		petriNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("name", sourcename+"->"+targetname);
		// Update view of newly created edge
		CyNetworkView [] cnvs = new CyNetworkView[1];
		cnvm.getNetworkViews(petriNet).toArray(cnvs);
		CyNetworkView cnv = cnvs[0];
		cnv.updateView();
		View<CyEdge> view = cnv.getEdgeView(edge);
		view.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);
	}
}
