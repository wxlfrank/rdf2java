package org.epos.dcat;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;
import org.open.rdf.RDFMapper;
import org.open.rdfs.Class;
import org.w3._2002_07_owl.Thing;

import com.xmlns.foaf_0_1.Document;

public class TestRDFMapper {

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
		if (parent != null)
			resource.addProperty(RDFS.subClassOf, parent);
		return resource;
	}

	@Test
	public void test() {
		RDFMapper mapper = new RDFMapper();
		// mapper.handleField(new Class());
		Document document = new Document();
		document.setPrimaryTopic(new Thing());
		StringWriter result = new StringWriter();
		mapper.write(document).write(result);
		System.out.println(result.getBuffer().toString());
		Model model = ModelFactory.createDefaultModel();
		model.read(new StringReader(result.getBuffer().toString()), null);
		Object read = mapper.read(model);
		mapper.write(read).write(System.out);
	}

	@Test
	public void testFindRDFClass() {
		RDFMapper mapper = new RDFMapper();
		Model rdf_model = ModelFactory.createDefaultModel();
		Resource resource = mapper.findRDFClass(rdf_model, Class.class);
		System.out.println(resource.getURI());
		// mapper.handleField(new Class());
		// mapper.toRDF(new Class(new Package("temp"), "class")).write(System.out);
	}

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

}
