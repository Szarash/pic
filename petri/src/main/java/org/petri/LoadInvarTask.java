package org.petri;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JComboBox;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * Task to load invariants from a file
 * @author M. Gehrmann
 *
 */
public class LoadInvarTask extends AbstractTask {

	@Tunable(description="Choose a file", params="input=true")
	public File inpFile; // Ask file for loading invariants
	private JComboBox<String> invarHolder;

	/**
	 * Constructor
	 * @param invarHolder Container for invariants
	 */
	public LoadInvarTask(JComboBox<String> invarHolder) {
		this.invarHolder = invarHolder;
	}
	
	public void run(TaskMonitor taskMonitor) throws Exception {
	    FileUtils fileUtils = new FileUtils(inpFile);
	    String content = fileUtils.getContent();
	    // Retrieve invariants from file. Currently by names
		String splitString[] = content.split("\\r?\\n"); // Split on newlines
		ArrayList<String[]> invars = new ArrayList<String[]>(); // Holds all invariants
		ArrayList<String> names = new ArrayList<String>(); // Holds invariant currently being extracted
		for (String currLine : splitString) {
			currLine = currLine.trim();
			if (currLine.length() < 2 || currLine.equals("invariants :")) {
				continue;
			}
			else if (currLine.charAt(1) == '.') {
				// This is a new invariant
				if (!names.isEmpty()) {
					// Finalize old invariant
					invars.add(names.toArray(new String[names.size()]));
					// Reset names
					names.clear();
				}
			}
			String[] lineSplit = currLine.split("\\+");
			// Do something about lineSplit[0], since it either contains start of new invariant or is empty
			if (lineSplit[0].length() != 0) {
				// Contains start of new invariant
				int start = lineSplit[0].indexOf(':');
				names.add(lineSplit[0].substring(start+1).trim());
			}
			// Split only contains name of transition
			for (int i=1; i<lineSplit.length; i++) {
				names.add(lineSplit[i].trim());
			}
		}
		invars.add(names.toArray(new String[names.size()]));
		// invars now holds all invariants
		for (String[] invarList : invars) {
			String inv = "";
			for (String trans : invarList) {
				inv += trans + ", ";
			}
		invarHolder.addItem(inv.substring(0, inv.length()-2));
		}
	}
}
