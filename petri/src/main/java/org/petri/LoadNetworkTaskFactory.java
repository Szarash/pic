package org.petri;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Factory for CreateNetworkTasks
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class LoadNetworkTaskFactory extends AbstractTaskFactory{
	private final CyNetworkManager netMgr;
	private final CyNetworkNaming namingUtil; 
	private final CyEventHelper eventHelper;
	private final CyNetwork petriNet;
	private final PetriUtils petriUtils;
	
	/**
	 * Constructor
	 * @param netMgr CyNetworkManager
	 * @param namingUtil CyNetworkNaming
	 * @param eventHelper EventHelper
	 * @param petriNet Petri net to be filled with data
	 * @param petriUtils Utilities for Petri net
	 */
	public LoadNetworkTaskFactory(final CyNetworkManager netMgr, final CyNetworkNaming namingUtil,
			final CyEventHelper eventHelper, final CyNetwork petriNet, final PetriUtils petriUtils){
		this.netMgr = netMgr;
		this.namingUtil = namingUtil;
		this.eventHelper = eventHelper;
		this.petriNet = petriNet;
		this.petriUtils = petriUtils;
	}
	

	/**
	 * 
	 */
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new LoadNetworkTask(netMgr, namingUtil, eventHelper, petriNet, petriUtils));
	}	
}
