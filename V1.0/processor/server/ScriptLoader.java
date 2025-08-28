package processor.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.Settings;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import traffic.light.TrafficLightTiming;
import traffic.routing.Routing;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * This class loads simulation setups from a file.
 */
public class ScriptLoader {
	private final List<Experiment> experiments = new ArrayList<>();

	boolean isEmpty() {
		return experiments.isEmpty();
	}

	/*
	 * Load setups from a file and save the setups to a list.
	 */
	public boolean loadScriptFile(String inputSimulationScript) {
		experiments.clear();
		if(inputSimulationScript.endsWith(".xml")){
			return loadFromXMLFile(inputSimulationScript);
		}else{
			return loadFromTextFile(inputSimulationScript);
		}
	}

	public boolean loadFromTextFile(String inputSimulationScript){
		File file = new File(inputSimulationScript);
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			ArrayList<String> oneSimSetup = new ArrayList<>();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals("###")) {
					if (oneSimSetup.size() > 0) {
						Experiment experiment = convertToExperiment(oneSimSetup);
						experiment.experimentId = "Experiment " + experiments.size();
						experiments.add(experiment);
						oneSimSetup = new ArrayList<>();
					}
				} else {
					oneSimSetup.add(line);
				}
			}
			return true;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	public boolean loadFromXMLFile(String fileName){
		File file = new File(fileName);
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			Element root= doc.getDocumentElement();
			root.normalize();
			Map<Element, Experiment> experiments = new HashMap<>();
			experiments.put(root, updateExperimentFromElement(new Experiment(), root));
			find(experiments, root);
			this.experiments.addAll(experiments.values());
			return true;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void find(Map<Element,Experiment> experiments, Element element){
		if(element.hasChildNodes()) {
			Experiment experiment = experiments.remove(element);
			NodeList nodes = element.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				if(nodes.item(i) instanceof  Element) {
					Element child = (Element) nodes.item(i);
					Experiment childExp = updateExperimentFromElement(experiment.clone(), child);
					experiments.put(child, childExp);
					find(experiments, child);
				}
			}
		}
	}

	public Experiment updateExperimentFromElement(Experiment experiment, Element element){
		NamedNodeMap map = element.getAttributes();
		for (int j = 0; j < map.getLength(); j++) {
			Attr attr = (Attr) map.item(j);
			experiment.setValues(attr.getName(), attr.getValue());
		}
		return experiment;
	}

	public List<Experiment> getExperiments() {
		return experiments;
	}

	public Experiment getFirstExperiment() {
		return experiments.get(0);
	}

	public Experiment convertToExperiment(ArrayList<String> newSettings) {
		Experiment experiment = new Experiment();

		for (final String string : newSettings) {
			if (string.length() >= 2 && string.substring(0, 2).equals("//"))
				continue;
			String[] fields = string.split(" ");
			if (fields.length < 2)
				continue;
			experiment.setValues(fields[0], fields[1]);
		}
		return experiment;
	}
}
