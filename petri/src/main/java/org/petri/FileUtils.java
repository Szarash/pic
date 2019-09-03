package org.petri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Used for loading Petri nets  and invariants from different input formats.
 * Supported formats for Petri nets:
 * 	- sbml
 *  - kgml
 * 	- pnt
 * 	- apnn
 *  - metatool.dat
 *  - reactionlist
 *  
 * For formats where weight of arcs is not given, a default value
 * of 1 is assumed. Similarly, if only an ID is given, but no name,
 * the ID will also double as a nodes name. Furthermore, if no
 * amount of tokens is specified, it will be defaulted to 0.
 * All of these values can then be manually edited in Cytoscape.
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class FileUtils {
	File inpFile;

	/**
	 * Constructor
	 * @param inpFile file to read from
	 */
	public FileUtils(File inpFile) {
		this.inpFile = inpFile;
	}

	/**
	 * Loads Petri net from SBML format.
	 * Place attributes constant and boundaryCondition are ignored.
	 * Transition attribute reversible is ignored, stoichiometry expected to be int
	 * @param petriNet Network to be used to represent Petri Net
	 * @param doc XML-Document used to extract information
	 * @throws Exception Errors during loading or incorrect format
	 */
	public void readSBML(CyNetwork petriNet, Document doc) throws Exception {
		// Generating Nodes for Places
		NodeList listOfPlaces = doc.getElementsByTagName("species");
		CyNode [] cyPlaceArray = new CyNode[listOfPlaces.getLength()];
		for (int i = 0; i<listOfPlaces.getLength(); i++) {
			cyPlaceArray[i] = petriNet.addNode();
			Element element = (Element) listOfPlaces.item(i);
			if (element.hasAttribute("initialAmount")) {
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("tokens", Integer.parseInt(element.getAttribute("initialAmount")));
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("initial tokens", Integer.parseInt(element.getAttribute("initialAmount")));
			}
			else if (element.hasAttribute("initialConcentration")) {
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("tokens", Integer.parseInt(element.getAttribute("initialConcentration")));
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("initial tokens", Integer.parseInt(element.getAttribute("initialConcentration")));
			}
			else {
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("tokens", 0);
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("initial tokens", 0);
			}
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("internal id", "p"+Integer.toString(i));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("id", element.getAttribute("id"));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("tokens", Integer.parseInt(element.getAttribute("initialAmount")));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("initial tokens", Integer.parseInt(element.getAttribute("initialAmount")));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("type", "Place");
			if (element.getAttribute("name") != "") { // if name exists, set it, else set name = id
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("name", element.getAttribute("name"));
			}
			else {
				petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("name", element.getAttribute("id"));
			}
		}
		// Generating nodes for transitions
		NodeList listOfTransitions = doc.getElementsByTagName("reaction");
		CyNode [] cyTransitionArray = new CyNode[listOfTransitions.getLength()];
		CyEdge [] cyEdgeArray = new CyEdge[listOfPlaces.getLength()*listOfTransitions.getLength()];
		int numOfEdges = 0;
		for (int i = 0; i<listOfTransitions.getLength(); i++) {
			cyTransitionArray[i] = petriNet.addNode();
			Element element = (Element) listOfTransitions.item(i);
			String id =  element.getAttribute("id");
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("internal id", "t"+Integer.toString(i));
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("id", id);
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("type", "Transition");
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("fired", 0);
			if (element.getAttribute("name") != "") {
				petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("name", element.getAttribute("name"));
			}
			else {
				petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("name", id);
			}
			NodeList children = element.getChildNodes();
			// Create edges for the current transition
			for (int index = 0; index<children.getLength(); index++) {
				String nname = children.item(index).getNodeName();
				if (nname.equals("listOfReactants")) { // Outgoing edges
					NodeList reactants = children.item(index).getChildNodes();
					for (int reactIndex = 0; reactIndex<reactants.getLength(); reactIndex++) {
						if (reactants.item(reactIndex).getNodeType() == Node.ELEMENT_NODE) {
							Element reactant = (Element) reactants.item(reactIndex);
							for (int placeIndex = 0; placeIndex<listOfPlaces.getLength(); placeIndex++) {
								if (petriNet.getDefaultNodeTable().getRow(cyPlaceArray[placeIndex].getSUID()).get("id", String.class).equals(reactant.getAttribute("species"))) {
									cyEdgeArray[numOfEdges] = petriNet.addEdge(cyPlaceArray[placeIndex], cyTransitionArray[i], true);
									petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("weight", Integer.parseInt(reactant.getAttribute("stoichiometry")));
									petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("name", reactant.getAttribute("species")+"->"+id);
									petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("internal id", "e"+Integer.toString(numOfEdges));
									numOfEdges++;
									break;
								}
							}
						}
					}
				}
				else if (nname.equals("listOfProducts")) { // Incoming edges
					NodeList products = children.item(index).getChildNodes();
					for (int prodIndex = 0; prodIndex<products.getLength(); prodIndex++) {
						if (products.item(prodIndex).getNodeType() == Node.ELEMENT_NODE) {
							Element product = (Element) products.item(prodIndex);
							for (int placeIndex = 0; placeIndex<listOfPlaces.getLength(); placeIndex++) {
								if (petriNet.getDefaultNodeTable().getRow(cyPlaceArray[placeIndex].getSUID()).get("id", String.class).equals(product.getAttribute("species"))) {
									cyEdgeArray[numOfEdges] = petriNet.addEdge(cyTransitionArray[i], cyPlaceArray[placeIndex], true);
									petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("weight", Integer.parseInt(product.getAttribute("stoichiometry")));
									petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("name", id+"->"+product.getAttribute("species"));
									petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("internal id", "e"+Integer.toString(numOfEdges));
									numOfEdges++;
									break;
								}
							}	
						}
					}
				}
			}
		}
	}

	/**
	 * Loads Petri net from KGML format.
	 * @param petriNet Network to be used to represent Petri Net
	 * @param doc XML-Document used to extract information
	 * @throws Exception Errors during loading or incorrect format
	 */
	public void readKGML(CyNetwork petriNet, Document doc) throws Exception {
		// Create nodes for places
		NodeList listOfPlaces = doc.getElementsByTagName("entry");
		CyNode[] cyPlaceArray = new CyNode[listOfPlaces.getLength()];
		for (int i=0; i<listOfPlaces.getLength(); i++) {
			cyPlaceArray[i] = petriNet.addNode();
			Element place = (Element) listOfPlaces.item(i);
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("internal id", "p"+Integer.toString(i));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("id", "e"+ place.getAttribute("id"));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("name", place.getAttribute("name"));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("type", "Place");
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("initial tokens", 0);
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i].getSUID()).set("tokens", 0);
		}
		// Create nodes for transitions
		NodeList listOfTransitions = doc.getElementsByTagName("reaction");
		CyNode[] cyTransitionArray = new CyNode[listOfTransitions.getLength()];
		int numOfEdges = 0;
		for (int i=0; i<listOfTransitions.getLength(); i++) {
			Element trans = (Element) listOfTransitions.item(i);
			cyTransitionArray[i] = petriNet.addNode();
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("internal id", "t"+Integer.toString(i));
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("id", "r" + trans.getAttribute("id"));
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("name", trans.getAttribute("name"));
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("type", "Transition");
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("fired", 0);
			// Create edges for current transition
			NodeList reactands = trans.getElementsByTagName("substrate"); // Incoming edges
			for (int index=0; index<reactands.getLength(); index++) {
				Element react = (Element) reactands.item(index);
				String id = "e" + react.getAttribute("id");
				CyNode source = null;
				for (CyNode n : cyPlaceArray) {
					if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("id", String.class).equals(id)) {
						source = n;
					}
				}
				CyEdge e = petriNet.addEdge(source, cyTransitionArray[i], true);
				petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("weight", 1);
				petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("name", react.getAttribute("name") + "->"
						+ trans.getAttribute("name"));
				petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("internal id", "e"+Integer.toString(numOfEdges));
				numOfEdges++;
			}
			NodeList products = trans.getElementsByTagName("product"); // Outgoing edges
			for (int index=0; index<products.getLength(); index++) {
				Element product = (Element) products.item(index);
				String id = "e" + product.getAttribute("id");
				CyNode target = null;
				for (CyNode n: cyPlaceArray) {
					if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("id", String.class).equals(id)) {
						target = n;
					}
				}
				CyEdge e = petriNet.addEdge(cyTransitionArray[i], target, true);
				petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("weight", 1);
				petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("name", trans.getAttribute("name") + "->"
						+ product.getAttribute("name"));
				petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("internal id", "e"+Integer.toString(numOfEdges));
				numOfEdges++;
			}
		}
		// Remove duplicate and/or nodes without edges
		Set<CyNode> toRemove = new HashSet<CyNode>();
		for (CyNode n : petriNet.getNodeList()) {
			if (toRemove.contains(n)) {
				continue;
			}
			if (petriNet.getAdjacentEdgeList(n, CyEdge.Type.ANY).isEmpty()) {
				toRemove.add(n);
				continue;
			}
			for (CyNode m : petriNet.getNodeList()) {
				if (n != m && petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class).equals(
						petriNet.getDefaultNodeTable().getRow(m.getSUID()).get("name", String.class))) {
						toRemove.add(m);
				}
			}
		}
		petriNet.removeNodes(toRemove);
	}
	
	/**
	 * Loads Petri net from PNT format.
	 * @param petriNet Network to be used to represent Petri Net
	 * @param content Content of input file
	 * @throws Exception Errors during loading or incorrect format
	 */
	public void readPNT(CyNetwork petriNet, String content) throws Exception {
		ArrayList<CyEdge> edges = new ArrayList<CyEdge>();
		String splitString[] = content.split("@");
		// Creating new places
		String placesSplit[] = splitString[1].split("\\r?\\n");
		CyNode [] cyPlaceArray = new CyNode[placesSplit.length - 1];
		for (int i = 2; i<placesSplit.length; i++){
			cyPlaceArray[i - 2] = petriNet.addNode();
			String placeSplit[] = placesSplit[i].split("\\s+");
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i - 2].getSUID()).set("internal id", "p" + Integer.toString(i - 2));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i - 2].getSUID()).set("name", placeSplit[2]);
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i - 2].getSUID()).set("tokens", 0);
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i - 2].getSUID()).set("initial tokens", 0);
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i - 2].getSUID()).set("type", "Place");
		}
		// Creating new transitions
		String transitionsSplit[] = splitString[2].split("\\r?\\n");
		CyNode [] cyTransitionArray = new CyNode[transitionsSplit.length - 1];
		for (int i = 2; i<transitionsSplit.length; i++){
			cyTransitionArray[i - 2] = petriNet.addNode();
			String transitionSplit[] = transitionsSplit[i].split("\\s+");
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i - 2].getSUID()).set("internal id","t" + Integer.toString(i - 2));
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i - 2].getSUID()).set("name", transitionSplit[2]);
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i - 2].getSUID()).set("type", "Transition");
			petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i - 2].getSUID()).set("fired", 0);
		}
		// Creating new edges
		String edgesSplit[] = splitString[0].split("\\r?\\n");
		for (int i = 1; i<edgesSplit.length; i++){
			String placeEdgesSplit[] = edgesSplit[i].split(",");
			String incomingEdges[] = placeEdgesSplit[0].split("\\s+");
			String outgoingEdges[] = placeEdgesSplit[1].split("\\s+");
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i - 1].getSUID()).set("initial tokens", Integer.parseInt(incomingEdges[2]));
			petriNet.getDefaultNodeTable().getRow(cyPlaceArray[i - 1].getSUID()).set("tokens", Integer.parseInt(incomingEdges[2]));
			for (int x = 3; x < incomingEdges.length; x++){
				if (incomingEdges[x].contains(":")){
					CyEdge e = petriNet.addEdge(cyTransitionArray[Integer.parseInt(incomingEdges[x].substring(0, incomingEdges[x].length() - 1))], cyPlaceArray[i - 1], true);
					petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("weight", Integer.parseInt(incomingEdges[x + 1]));
					edges.add(e);
					x++;
				}
				else{
					edges.add(petriNet.addEdge(cyTransitionArray[Integer.parseInt(incomingEdges[x])], cyPlaceArray[i - 1], true));
				}
			}
			for (int x = 1; x < outgoingEdges.length; x++){
				if(outgoingEdges[x].contains(":")){
					CyEdge e = petriNet.addEdge(cyPlaceArray[i - 1] , cyTransitionArray[Integer.parseInt(outgoingEdges[x].substring(0, outgoingEdges[x].length() - 1))], true);
					petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("weight", Integer.parseInt(outgoingEdges[x + 1]));
					edges.add(e);
					x++;
				}
				else{
					edges.add(petriNet.addEdge(cyPlaceArray[i - 1], cyTransitionArray[Integer.parseInt(outgoingEdges[x])], true));
				}
			}
		}
		int numOfEdges = 0;
		for (CyEdge e : edges){
			petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("name", petriNet.getDefaultNodeTable().getRow(e.getSource().getSUID()).get("name", String.class)+"->"+petriNet.getDefaultNodeTable().getRow(e.getTarget().getSUID()).get("name", String.class));
			petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("internal id", "e"+Integer.toString(numOfEdges));
			if (petriNet.getDefaultEdgeTable().getRow(e.getSUID()).get("weight", Integer.class) == null){
				petriNet.getDefaultEdgeTable().getRow(e.getSUID()).set("weight", 1);
			}
			numOfEdges++;
		}
	}

	/**
	 * Loads Petri Net from APNN format.
	 * @param petriNet Network to be used to represent Petri Net
	 * @param content Content of input file
	 * @throws Exception Errors during loading or incorrect format
	 */
	public void readAPNN(CyNetwork petriNet, String content) throws Exception {
		String splitString[] = content.split("\\r?\\n");
		ArrayList<CyNode> places = new ArrayList<CyNode>();
		ArrayList<CyNode> transitions = new ArrayList<CyNode>();
		String sep = "\\";
		int p = 0, t = 0, e = 0;	// number of places, transitions, edges
		for (String currLine : splitString) {
			String lineSplit[] = currLine.split(Pattern.quote(sep));
			if (lineSplit.length == 1) {
				continue;
			}
			if (lineSplit[1].contains("place")) { // Create a new place
				CyNode place = petriNet.addNode();
				places.add(place);
				petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("type", "Place");
				petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("internal id", "p"+Integer.toString(p));
				p++;
				for (String split : lineSplit) {
					if (split.length() == 0) {
						continue;
					}
					int pos1 = split.indexOf("{");
					int pos2 = split.indexOf("}");
					// Substring until first curly bracket contains attribute name
					// Substring between curly brackets contains attribute value
					if (split.substring(0, pos1).equals("place")) {
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("id", split.substring(pos1+1, pos2));
					}
					else if (split.substring(0, pos1).equals("name")) {
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("name", split.substring(pos1+1, pos2));
					}
					else if (split.substring(0, pos1).equals("init")) {
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("initial tokens",
								Integer.parseInt(split.substring(pos1+1, pos2)));
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("tokens", 
								Integer.parseInt(split.substring(pos1+1, pos2)));
					}
				}
			}
			else if (lineSplit[1].contains("transition")) { // Create a new transition
				CyNode trans = petriNet.addNode();
				transitions.add(trans);
				petriNet.getDefaultNodeTable().getRow(trans.getSUID()).set("type", "Transition");
				petriNet.getDefaultNodeTable().getRow(trans.getSUID()).set("fired", 0);
				petriNet.getDefaultNodeTable().getRow(trans.getSUID()).set("internal id", "t"+Integer.toString(t));
				t++;
				for (String split : lineSplit) {
					if (split.length() == 0) {
						continue;
					}
					// Substring until first curly bracket contains attribute name
					// Substring between curly brackets contains attribute value
					int pos1 = split.indexOf("{");
					int pos2 = split.indexOf("}");
					if (split.substring(0, pos1).equals("transition")) {
						petriNet.getDefaultNodeTable().getRow(trans.getSUID()).set("id", split.substring(pos1+1, pos2));
					}
					else if (split.substring(0, pos1).equals("name")) {
						petriNet.getDefaultNodeTable().getRow(trans.getSUID()).set("name", split.substring(pos1+1, pos2));
					}
				}
			}
			else if (lineSplit[1].contains("arc")) { // Create a new edge
				CyNode source = null;
				int pos1 = lineSplit[2].indexOf("{");
				int pos2 = lineSplit[2].indexOf("}");
				String idS = lineSplit[2].substring(pos1+1, pos2);
				for (CyNode n : petriNet.getNodeList()) {
					if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("id", String.class).equals(idS)) {
						source = n;
						break;
					}
				}
				CyNode target = null;
				pos1 = lineSplit[3].indexOf("{");
				pos2 = lineSplit[3].indexOf("}");
				String idT = lineSplit[3].substring(pos1+1, pos2);
				for (CyNode n : petriNet.getNodeList()) {
					if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("id", String.class).equals(idT)) {
						target = n;
						break;
					}
				}
				CyEdge arc = petriNet.addEdge(source, target, true);
				pos1 = lineSplit[1].indexOf("{");
				pos2 = lineSplit[1].indexOf("}");
				petriNet.getDefaultEdgeTable().getRow(arc.getSUID()).set("name", lineSplit[1].substring(pos1+1, pos2));
				petriNet.getDefaultEdgeTable().getRow(arc.getSUID()).set("internal id", "e"+Integer.toString(e));
				e++;
				for (int i = 4; i < lineSplit.length; i++) {
					if (lineSplit[i].contains("weight")){
						pos1 = lineSplit[i].indexOf("{");
						pos2 = lineSplit[i].indexOf("}");
						petriNet.getDefaultEdgeTable().getRow(arc.getSUID()).set("weight",
								Integer.parseInt(lineSplit[i].substring(pos1+1, pos2)));
					}
				}
			}
		}
	}

	/**
	 * Loads Petri Net from metatool.dat format.
	 * @param petriNet Network to be used to represent Petri Net
	 * @param content Content of input file
	 * @throws Exception Errors during loading or incorrect format
	 */
	public void readDAT(CyNetwork petriNet, String content) throws Exception {
		String splitString[] = content.split("\\r?\\n");
		ArrayList<String> transitions = new ArrayList<String>();
		ArrayList<String> places = new ArrayList<String>();
		for (int i = 0; i<splitString.length; i++){
			Integer x = i + 1;
			if (splitString[i].equals("-ENZIRREV")){ // transitions
				transitions.addAll(Arrays.asList(splitString[x].split("\\s+")));
				CyNode [] cyTransitionArray = new CyNode[transitions.size()];
				for (int y = 0; y < transitions.size(); y++){
					cyTransitionArray[y] = petriNet.addNode();
					petriNet.getDefaultNodeTable().getRow(cyTransitionArray[y].getSUID()).set("id", transitions.get(y));
					petriNet.getDefaultNodeTable().getRow(cyTransitionArray[y].getSUID()).set("name", transitions.get(y));
					petriNet.getDefaultNodeTable().getRow(cyTransitionArray[y].getSUID()).set("type", "Transition");
					petriNet.getDefaultNodeTable().getRow(cyTransitionArray[y].getSUID()).set("fired", 0);
					petriNet.getDefaultNodeTable().getRow(cyTransitionArray[y].getSUID()).set("internal id", "t"+Integer.toString(y));
				}
			}
			else if (splitString[i].equals("-METINT")){ // places
				places.addAll(Arrays.asList(splitString[x].split("\\s+")));
				CyNode [] cyPlaceArray = new CyNode[places.size()];
				for (int y = 0; y < places.size(); y ++){
					cyPlaceArray[y] = petriNet.addNode();
					petriNet.getDefaultNodeTable().getRow(cyPlaceArray[y].getSUID()).set("id", places.get(y));
					petriNet.getDefaultNodeTable().getRow(cyPlaceArray[y].getSUID()).set("name", places.get(y));
					petriNet.getDefaultNodeTable().getRow(cyPlaceArray[y].getSUID()).set("tokens", 0);
					petriNet.getDefaultNodeTable().getRow(cyPlaceArray[y].getSUID()).set("initial tokens", 0);
					petriNet.getDefaultNodeTable().getRow(cyPlaceArray[y].getSUID()).set("type", "Place");
					petriNet.getDefaultNodeTable().getRow(cyPlaceArray[y].getSUID()).set("internal id", "p"+Integer.toString(y));

				}
			}
			else if (splitString[i].equals("-CAT")){ // edges
				Integer numOfEdges = 0;
				CyEdge [] cyEdgeArray = new CyEdge[transitions.size()*places.size()];
				for (int y = x; y < splitString.length; y++){
					String lineSplit[] = splitString[y].split("\\s+");
					String currentTransition = lineSplit[0];
					CyNode trans = null;
					for (CyNode t : petriNet.getNodeList()) {
						if (petriNet.getDefaultNodeTable().getRow(t.getSUID()).get("id", String.class).equals(currentTransition)) {
							trans = t;
							break;
						}
					}
					String currentPosition =  "left"; // name of transition
					for (int z = 0; z < lineSplit.length; z++){
						// new incoming edge (reactant)
						if (currentPosition.equals("middle") && !lineSplit[z].equals(":") && !lineSplit[z].equals("=")
								&& !lineSplit[z].equals(".") && !lineSplit[z].equals("+")){	
							CyNode place = null;
							for (CyNode p : petriNet.getNodeList()) {
								if (petriNet.getDefaultNodeTable().getRow(p.getSUID()).get("id", String.class).equals(lineSplit[z])) {
									place = p;
									break;
								}
							}
							cyEdgeArray[numOfEdges] = petriNet.addEdge(place, trans, true);
							petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("weight", 0);
							petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("name", lineSplit[z]+"->"+currentTransition);
							petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("internal id", "e"+Integer.toString(numOfEdges));
							numOfEdges++;
						}
						// new outgoing edge (product)
						else if (currentPosition.equals("right") && !lineSplit[z].equals(":") && !lineSplit[z].equals("=")
								&& !lineSplit[z].equals(".") && !lineSplit[z].equals("+")){
							CyNode place = null;
							for (CyNode p : petriNet.getNodeList()) {
								if (petriNet.getDefaultNodeTable().getRow(p.getSUID()).get("id", String.class).equals(lineSplit[z])) {
									place = p;
									break;
								}
							}
							cyEdgeArray[numOfEdges] = petriNet.addEdge(trans, place, true);
							petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("weight", 0);
							petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("name", currentTransition+"->"+lineSplit[z]);
							petriNet.getDefaultEdgeTable().getRow(cyEdgeArray[numOfEdges].getSUID()).set("internal id", "e"+Integer.toString(numOfEdges));
							numOfEdges++;
						}
						if (lineSplit[z].equals(":")){ // entering reactants
							currentPosition = "middle";
						}
						else if (lineSplit[z].equals("=")){ // entering products
							currentPosition = "right";
						}
					}
				}
			}
		}
	}

	/**
	 * Loads Petri Net from Reactionlist, assumed extension .txt.
	 * @param petriNet Network to be used to represent Petri Net
	 * @param content Content of input file
	 * @throws Exception Errors during loading or incorrect format
	 */
	public void readRL(CyNetwork petriNet, String content) throws Exception {
		String splitString[] = content.split("\\r?\\n");
		ArrayList<CyNode> transitions = new ArrayList<CyNode>();
		ArrayList<CyEdge> edges = new ArrayList<CyEdge>();
		int p = 0;
		for (String currLine : splitString) {
			String lineSplit[] = currLine.split("\\s+");
			String name = lineSplit[0].substring(0, lineSplit[0].length()-2);
			String dir = "in";
			transitions.add(petriNet.addNode());
			petriNet.getDefaultNodeTable().getRow(transitions.get(transitions.size()-1).getSUID()).set("id", name);
			petriNet.getDefaultNodeTable().getRow(transitions.get(transitions.size()-1).getSUID()).set("name", name);
			petriNet.getDefaultNodeTable().getRow(transitions.get(transitions.size()-1).getSUID()).set("type", "Transition");
			petriNet.getDefaultNodeTable().getRow(transitions.get(transitions.size()-1).getSUID()).set("fired", 0);
			petriNet.getDefaultNodeTable().getRow(transitions.get(transitions.size()-1).getSUID()).set("internal id", "t"+Integer.toString(transitions.size()-1));

			for (int i=1; i<lineSplit.length; i++ ) {
				if (lineSplit[i].equals("->")) {
					dir = "out";
					continue;
				}
				else if (lineSplit[i].equals("+")) {
					continue;
				}
				else {
					CyNode place = null;
					for (CyNode n : petriNet.getNodeList()) {
						if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("id", String.class).equals(lineSplit[i])) {
							place = n;
							break;
						}
					}
					if (place == null) {
						place = petriNet.addNode();
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("id", lineSplit[i]);
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("name", lineSplit[i]);
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("initial tokens", 0);
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("tokens", 0);
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("type", "Place");
						petriNet.getDefaultNodeTable().getRow(place.getSUID()).set("internal id", "p"+Integer.toString(p));
						p++;
					}
					if (dir.equals("in")) {
						edges.add(petriNet.addEdge(place, transitions.get(transitions.size()-1), true));
						petriNet.getDefaultEdgeTable().getRow(edges.get(edges.size()-1).getSUID()).set("weight", 1);
						petriNet.getDefaultEdgeTable().getRow(edges.get(edges.size()-1).getSUID()).set("name", lineSplit[i]+"->"+lineSplit[0]);
						petriNet.getDefaultEdgeTable().getRow(edges.get(edges.size()-1).getSUID()).set("internal id", "e"+Integer.toString(edges.size()-1));
					}
					else if (dir.equals("out")) {
						edges.add(petriNet.addEdge(transitions.get(transitions.size()-1), place, true));
						petriNet.getDefaultEdgeTable().getRow(edges.get(edges.size()-1).getSUID()).set("weight", 1);
						petriNet.getDefaultEdgeTable().getRow(edges.get(edges.size()-1).getSUID()).set("name", lineSplit[0]+"->"+lineSplit[i]);
						petriNet.getDefaultEdgeTable().getRow(edges.get(edges.size()-1).getSUID()).set("internal id", "e"+Integer.toString(edges.size()-1));
					}
				}
			}
		}
	}

	/**
	 * Extracts content of input file for non-XML files
	 * @return content of input file
	 * @throws IOException
	 */
	public String getContent() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inpFile)));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			
			while (line != null){
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			String everything = sb.toString();
			return everything;
		}
		finally {
			br.close();
		}
	}
	
	/**
	 * Decides which function to call based on file extension
	 * @param ext extension of the file
	 * @param petriNet Network to be used to represent Petri Net
	 * @throws Exception Errors during loading or incorrect format
	 */
	public void choose(String ext, CyNetwork petriNet) throws Exception {
		if (ext.equals("xml")) { // SBML or KGML format
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(inpFile);
			NodeList sbml = doc.getElementsByTagName("sbml");
			if (sbml.getLength() == 0) { // If not SBML format, this will be zero
				readKGML(petriNet, doc);
			}
			else {
				readSBML(petriNet, doc);
			}
		}
		else {
			String content = getContent();
			if (ext.equals("pnt")) {
				readPNT(petriNet, content);
			}
			else if (ext.equals("apnn")) {
				readAPNN(petriNet, content);
			}
			else if (ext.equals("dat")) {
				readDAT(petriNet, content);
			}
			else if (ext.equals("txt")) {
				readRL(petriNet, content);
			}
			else {
				throw new Exception("Wrong extension!");
			}
		}
	}

	public static void loadInvars(String sutoringu) {
		// Read invars here, one Jörg gives us data anyway
	}
}
