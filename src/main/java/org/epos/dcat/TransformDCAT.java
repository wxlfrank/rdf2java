package org.epos.dcat;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.epos_ip.dcatflat.Person;

public class TransformDCAT {
	private static final String URI_DCAT_AP = "http://www.epos-eu.org/epos/dcat-ap";
	private Map<String, Model> models =  new HashMap<String, Model>();
	public void printResource(Resource resource) {
		if (resource.getURI() != null && resource.getLocalName().equals("Dataset")) {
			System.out.println(resource.getURI());
			resource.listProperties().forEachRemaining(statement -> {

				String name = statement.getPredicate().getLocalName();
				System.out.println(name);
				if (name.equals("subClassOf")) {
					Resource re = statement.getObject().asResource();
					System.out.println(re.getModel());
				}
			});
		}
	}

	public void transform(Person person) throws MalformedURLException {
		OntModel base = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
		OntDocumentManager dm = base.getDocumentManager();
		dm.addAltEntry(URI_DCAT_AP, Paths.get("epos-dcat-ap_shapes.ttl").toUri().toURL().toString());
		Model schema = base.read(URI_DCAT_AP, "TURTLE");
		// schema.listNameSpaces().forEachRemaining(name ->
		// System.out.println(name));
		schema.listSubjects().forEachRemaining(subject -> printResource(subject));
	}

}
