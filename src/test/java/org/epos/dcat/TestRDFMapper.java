package org.epos.dcat;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.epos.rdf.RDFMapper;
import org.junit.Test;
import org.open.generate.Class;

public class TestRDFMapper {

//	@Test
//	public void test() {
//		RDFMapper mapper = new RDFMapper();
//		// mapper.handleField(new Class());
//		mapper.toRDF(new Class()).write(System.out);
//	}

	@Test
	public void testRDFModel() {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		model.setNsPrefix("dc", DC.NS);
		model.setNsPrefix("", RDFS.uri);
		Resource onto = model.createResource(RDFS.uri);
		onto.addProperty(RDF.type, OWL.Ontology).addLiteral(DC.title, "The RDF Schema vocabulary (RDFS)");
		Resource resource = createSubject(model, "Resource", "The class resource, everything.");
		createSubject(model, "Class", "The class of classes.", resource);
		model.write(System.out, "N3");
	}

	public Resource createSubject(Model model, String label, String comment) {
		String uri = model.getNsPrefixURI("");
		Resource resource = model.createResource(uri + label).addLiteral(RDFS.isDefinedBy, uri)
				.addProperty(RDF.type, RDFS.Class).addLiteral(RDFS.label, label);
		if (comment != null && !comment.isEmpty())
			resource.addLiteral(RDFS.comment, comment);
		return resource;
	}
	public Resource createSubject(Model model, String label, String comment, Resource parent) {
		Resource resource = createSubject(model, label, comment);
		if(parent != null)
			resource.addProperty(RDFS.subClassOf, parent);
		return resource;
	}

}
