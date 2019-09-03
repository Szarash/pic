package org.petri;

import java.io.File;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * Task to load Petri Net from file and create default layout and
 * visual style
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class LoadNetworkTask extends AbstractTask {

	private final CyNetworkManager netMgr;
	private final CyNetworkNaming namingUtil;
	@Tunable(description="Choose a file", params="input=true")
	public File inpFile; // Ask file for creating Petri Net
	private final CyEventHelper eventHelper;
	private final CyNetwork petriNet;
	private final PetriUtils petriUtils;
	
	/**
	 * Constructor
	 * @param netMgr CyNetworkManager
	 * @param namingUtil CyNetworkNaming
	 * @param eventHelper EventHelper
	 * @param petriNet Petri Net to be filled with data
	 * @param petriUtils Utilities for Petri Net
	 */
	public LoadNetworkTask(final CyNetworkManager netMgr, final CyNetworkNaming namingUtil,
			final CyEventHelper eventHelper, CyNetwork petriNet, PetriUtils petriUtils){
		this.netMgr = netMgr;
		this.namingUtil = namingUtil;
		this.eventHelper = eventHelper;
		this.petriNet = petriNet;
		this.petriUtils = petriUtils;
	}

	/**
	 * Fill Petri Net with Nodes and Edges from an input file.
	 * Depending on format of file, a corresponding reading function
	 * will be called from org.petri.FileUtils.
	 * Afterwards apply default visual style and layout for Petri Nets.
	 */
	public void run(TaskMonitor monitor) throws Exception {
		petriUtils.initializeColumns();
		String fileName = inpFile.getName(); 	// Get extension of input file
		String extension = "";
		int dot = fileName.lastIndexOf('.');
		if (dot > 0) {
		    extension = fileName.substring(dot+1);
		    FileUtils fileUtils = new FileUtils(inpFile);
		    fileUtils.choose(extension, petriNet);	// Choose reading method based on extension
		}
		else {
			throw new Exception("Could not find extension"); // No extension could be found (no "." in fileName)
		}
		petriNet.getRow(petriNet).set(CyNetwork.NAME,
			      namingUtil.getSuggestedNetworkTitle(fileName.substring(0, dot)));
		eventHelper.flushPayloadEvents();
		petriUtils.createVisualStyle();

		boolean destroyNetwork = false;
		if (destroyNetwork) {
			netMgr.destroyNetwork(petriNet);
		}
	}
}
