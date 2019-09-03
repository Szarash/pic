package org.petri;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Task to export a Petri net to a new file.
 * @author M. Gehrmann, M. Kirchner
 */
public class ExportTask extends AbstractTask {
	
	private CyNetwork petriNet;
	private CyNode[] cyPlaceArray;
	private CyNode[] cyTransitionArray;
	@Tunable(description="Choose an export location", groups="Nodes")
	public File outputFile;
	
	/**
	 * Constructor
	 * @param petriNet Petri net to be exported
	 * @param cyPlaceArray Array of CyNodes containing all places of petriNet
	 * @param cyTransitionArray Array of CyNodes containing all transitions of petriNet
	 */
	public ExportTask(CyNetwork petriNet, CyNode[] cyPlaceArray, CyNode[] cyTransitionArray) {
		this.petriNet = petriNet;
		List <CyNode> cyList = Arrays.asList(cyPlaceArray);
		Collections.reverse(cyList);
		this.cyPlaceArray = (CyNode[]) cyList.toArray();
		cyList = Arrays.asList(cyTransitionArray);
		Collections.reverse(cyList);
		this.cyTransitionArray = (CyNode[]) cyList.toArray();
	}

	/**
	 * Export the Petri net to a file as chosen by the user
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("sbml");
		Element modelElement = doc.createElement("model");
		Element compartmentsElement = doc.createElement("listOfCompartments");
		Element speciesElement = doc.createElement("listOfSpecies");
		Element reactionsElement = doc.createElement("listOfReactions");
		doc.appendChild(rootElement);
		Attr xlmns = doc.createAttribute("xlmns");
		xlmns.setValue("http://www.sbml.org/sbml/level3/version1/core");
		Attr lvl = doc.createAttribute("level");
		lvl.setValue("3");
		Attr ver = doc.createAttribute("version");
		ver.setValue("1");
		rootElement.setAttributeNode(xlmns);
		rootElement.setAttributeNode(lvl);
		rootElement.setAttributeNode(ver);
		rootElement.appendChild(doc.createTextNode("\nCreated by PIC at " + LocalDateTime.now().toString() + "\n    "));
		Attr id = doc.createAttribute("id");
		id.setValue("PIC-Export");
		modelElement.setAttributeNode(id);
		rootElement.appendChild(modelElement);
		Element comp = doc.createElement("compartment");
		Attr id2 = doc.createAttribute("id");
		id2.setValue("default_compartment");
		Attr constant = doc.createAttribute("constant");
		constant.setValue("true");
		Attr spatDim = doc.createAttribute("spatialDimensions");
		spatDim.setValue("3");
		Attr size = doc.createAttribute("size");
		size.setValue("1");
		comp.setAttributeNode(id2);
		comp.setAttributeNode(constant);
		comp.setAttributeNode(spatDim);
		comp.setAttributeNode(size);
		modelElement.appendChild(compartmentsElement);
		compartmentsElement.appendChild(comp);
		List<Element> specieslist = new ArrayList<Element>();
		int curr = 0;
		for (CyNode place : cyPlaceArray){
			specieslist.add(doc.createElement("species"));
			specieslist.get(curr).setAttribute("constant", "false");
			specieslist.get(curr).setAttribute("hasOnlySubstanceUnits", "true");
			specieslist.get(curr).setAttribute("boundaryCondition", "false");
			specieslist.get(curr).setAttribute("compartment", "default_compartment");
			specieslist.get(curr).setAttribute("id", petriNet.getDefaultNodeTable().getRow(place.getSUID()).get("id", String.class));
			specieslist.get(curr).setAttribute("initialAmount", petriNet.getDefaultNodeTable().getRow(place.getSUID()).get("initial tokens", Integer.class).toString());
			specieslist.get(curr).setAttribute("name", petriNet.getDefaultNodeTable().getRow(place.getSUID()).get("name", String.class));
			speciesElement.appendChild(specieslist.get(curr));
			curr++;
		}
		modelElement.appendChild(speciesElement);
		List<Element> reactionlist = new ArrayList<Element>();
		List<Element> reactantlists = new ArrayList<Element>();
		List<Element> productlists = new ArrayList<Element>();
		List<Element> speciesrefs = new ArrayList<Element>();
		curr = 0;
		int curr2 = 0;
		int curr3 = 0;
		int curr4 = 0;
		for (CyNode trans : cyTransitionArray){
			reactionlist.add(doc.createElement("reaction"));
			reactionlist.get(curr).setAttribute("id", petriNet.getDefaultNodeTable().getRow(trans.getSUID()).get("id", String.class));
			reactionlist.get(curr).setAttribute("name",  petriNet.getDefaultNodeTable().getRow(trans.getSUID()).get("name", String.class));
			reactionlist.get(curr).setAttribute("reversible", "false");
			reactionlist.get(curr).setAttribute("fast",  "false");
			reactionlist.get(curr).setAttribute("compartment", "default_compartment");
			Iterable<CyEdge>incomingEdges = petriNet.getAdjacentEdgeIterable(trans, CyEdge.Type.INCOMING);
			if (incomingEdges.iterator().hasNext()){
				reactantlists.add(doc.createElement("listOfReactants"));				
				for (CyEdge incEdge : incomingEdges) {
					speciesrefs.add(doc.createElement("speciesReference"));
					speciesrefs.get(curr2).setAttribute("constant", "true");
					speciesrefs.get(curr2).setAttribute("stoichiometry", petriNet.getDefaultEdgeTable().getRow(incEdge.getSUID()).get("weight", Integer.class).toString());
					speciesrefs.get(curr2).setAttribute("species", petriNet.getDefaultNodeTable().getRow(incEdge.getSource().getSUID()).get("id", String.class));
					reactantlists.get(curr3).appendChild(speciesrefs.get(curr2));
					curr2++;
				}
				reactionlist.get(curr).appendChild(reactantlists.get(curr3));
				curr3++;
			}
			Iterable<CyEdge>outgoingEdges = petriNet.getAdjacentEdgeIterable(trans, CyEdge.Type.OUTGOING);
			if (outgoingEdges.iterator().hasNext()){
				productlists.add(doc.createElement("listOfProducts"));
				for (CyEdge outEdge : outgoingEdges){
					speciesrefs.add(doc.createElement("speciesReference"));
					speciesrefs.get(curr2).setAttribute("constant", "true");
					speciesrefs.get(curr2).setAttribute("stoichiometry",  petriNet.getDefaultEdgeTable().getRow(outEdge.getSUID()).get("weight", Integer.class).toString());
					speciesrefs.get(curr2).setAttribute("species", petriNet.getDefaultNodeTable().getRow(outEdge.getTarget().getSUID()).get("id", String.class));
					productlists.get(curr4).appendChild(speciesrefs.get(curr2));
					curr2++;
				}
				reactionlist.get(curr).appendChild(productlists.get(curr4));
				curr4++;
			}
			reactionsElement.appendChild(reactionlist.get(curr));
			curr++;
		}
		modelElement.appendChild(reactionsElement);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(outputFile);
		transformer.transform(source, result);
	}
}
