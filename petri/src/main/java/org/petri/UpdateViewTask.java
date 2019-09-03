package org.petri;

import java.awt.Color;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

/**
 * Task for updating view of Petri Net after changes to its components
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class UpdateViewTask extends AbstractTask {
	
	private CyNetwork petriNet;
	private CyNetworkView cnv;
	private CyNetworkViewManager cnvm;
	
	/**
	 * Constructor
	 * @param petriNet Petri net for which view should be updated
	 * @param cnvm CyNetworkViewManager
	 */
	public UpdateViewTask(CyNetwork petriNet, CyNetworkViewManager cnvm) {
		this.petriNet = petriNet;
		this.cnvm = cnvm;
	}

	/**
	 * Set color of transitions that fired to green,
	 * color of transitions that didn't fire to white.
	 * Update labels of places to reflect new amount of
	 * tokens after firing. 
	 */
	public void run(TaskMonitor taskMonitor) {
		CyNetworkView [] cnvs = new CyNetworkView[1];
		cnvm.getNetworkViews(petriNet).toArray(cnvs);
		cnv = cnvs[0];
		for (View<CyNode> v : cnv.getNodeViews()) {		// Separate views in places and transitions
			String ntype = petriNet.getDefaultNodeTable().getRow(v.getModel().getSUID()).get("type", String.class);
			if (ntype.equals("Transition")) {
				if (petriNet.getDefaultNodeTable().getRow(v.getModel().getSUID()).get("fired", Integer.class) == 1) {
					v.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.GREEN);	// Green if fired
				}
				else {
					v.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);	// White if not fired
				}
			}
			else {
				v.setLockedValue(BasicVisualLexicon.NODE_LABEL,	// New token amounts
					petriNet.getDefaultNodeTable().getRow(v.getModel().getSUID()).get("name", String.class)+"\n"
					+ Integer.toString(petriNet.getDefaultNodeTable().getRow(v.getModel().getSUID()).get("tokens", Integer.class)));
			}
		}
		cnv.updateView();
	}
}
