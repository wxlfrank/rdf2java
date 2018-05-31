package org.epos.dcat;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.open.rdf.RDFMapper;
import org.schema.ContactPoint;
import org.w3._2001_XMLSchema.Date;
import org.w3.ns_dcat.Dataset;

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

	public static XMLGregorianCalendar toXMLGregorianCalendar(java.util.Date date) {
		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTime(date);
		XMLGregorianCalendar xmlCalendar = null;
		try {
			xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
		} catch (DatatypeConfigurationException ex) {
		}
		return xmlCalendar;
	}

	@Test
	public void test() {
		RDFMapper mapper = new RDFMapper();
		Dataset dataset = new Dataset();
		dataset.setIdentifier(new org.w3._2001_XMLSchema.String("identifier"));
		try {
			dataset.setModified(new Date(DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()))));
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ContactPoint cp = new ContactPoint();
		cp.setTelephone(new org.w3._2001_XMLSchema.String("1234567"));
		dataset.getContactPoint().add(cp);
		StringWriter result = new StringWriter();
		mapper.write(dataset).write(result, "TURTLE");
		String write = result.getBuffer().toString();
		System.out.println(write);
		Model model = ModelFactory.createDefaultModel();
		model.read(new StringReader(result.getBuffer().toString()), null, "TURTLE");
		Object javaObject = mapper.read(model);
		result = new StringWriter();
		mapper.write(javaObject).write(result, "TURTLE");
		String read = result.getBuffer().toString();
		Assert.assertTrue(write.equals(read));
	}

	@Test
	public void testExamples() {
		try {
			final RDFMapper mapper = new RDFMapper();
			Files.list(Paths.get("EPOS-DCAT-AP/examples")).forEach(path -> {
				if (!path.toFile().isHidden() && !path.toFile().isDirectory()) {
					System.out.println("------------------------------------------------");
					System.out.println(path);
					Model model = ModelFactory.createDefaultModel();
					try {
						model.read(path.toUri().toURL().toString(), null, "TURTLE");
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					StringWriter result = new StringWriter();
					model.write(result, "TURTLE");
//					String orginal = result.toString();
//					System.out.println(orginal);
					Object javaObject = mapper.read(model);
					Assert.assertNotNull(javaObject);
					result = new StringWriter();
					mapper.write(javaObject).write(result, "TURTLE");
//					Assert.assertTrue(result.toString().equals(orginal));
//					System.out.println(result.toString());
					
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testRDFModel() {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		model.read("epos-dcat-ap_shapes.ttl", "TURTLE");
		model.setNsPrefix("dc", DC.NS);
		model.setNsPrefix("", RDFS.uri);
		Resource onto = model.createResource(RDFS.uri);
		onto.addProperty(RDF.type, OWL.Ontology).addLiteral(DC.title, "The RDF Schema vocabulary (RDFS)");
		Resource resource = createSubject(model, "Resource", "The class resource, everything.");
		createSubject(model, "Class", "The class of classes.", resource);
		model.write(System.out, "N3");
	}

}
