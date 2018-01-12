package org.epos.dcat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.epos.tranform.RDFFile;
import org.epos.tranform.RDFS2Java;
import org.junit.Test;
import org.open.generate.PackageEx;
import org.open.generate.RDFSUtils;

public class TestOWL {

	@Test
	public void test() throws IOException {

		List<RDFFile> files = new ArrayList<RDFFile>();
		Files.list(Paths.get("ontology")).forEach(path -> {
			try {
				if (!path.toFile().isHidden())
					files.add(new RDFFile(path.toUri().toURL().toString(), "TURTLE"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		});
		RDFS2Java transform = new RDFS2Java(files);
		RDFFile main = new RDFFile(Paths.get("epos-dcat-ap_shapes.ttl").toUri().toURL().toString(), "TURTLE");
		transform.translate(main);
	}

	@Test
	public void testPackageName() {
		String name = "http://xmlns.com/foaf/0.1/#";
		System.out.println(name + "->" + RDFSUtils.getPackageName(name));
		name = "http://www.w3.org/2000/01/rdf-schema#";
		System.out.println(name + "->" + RDFSUtils.getPackageName(name));
	}

	@Test
	public void test2() throws MalformedURLException {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		model.read(Paths.get("ontology/epos-dcat-ap_shapes.ttl").toUri().toURL().toString(), "TURTLE");
		Model base = model.getBaseModel();
		// Resource resource =
		// model.getResource("http://www.w3.org/2006/vcard/ns#anniversary");
		// System.out.println(resource);
		// Resource emptynode =
		// resource.getProperty(RDFS.range).getObject().asResource();
		// System.out.println(emptynode.getURI());
		// System.out.println(emptynode.getNameSpace());
		// System.out.println(emptynode.getLocalName());
		// System.out.println(emptynode.getId());
		base.listSubjects().forEachRemaining(resource -> {
			if (resource.isAnon())
				System.out.println(resource.getId());
			else
				System.out.println(resource.getURI());
		});
		// base.listStatements().forEachRemaining(stm ->
		// System.out.println(stm.getSubject().getURI()));
	}

	@Test
	public void testRange() {
		RDFS2Java trans = new RDFS2Java(null);
		Entry<String, Model> pack = RDFSUtils.readWriteModel("ontology/org.w3._2004_02_skos_core", "TURTLE");
		Resource resource = pack.getValue().getResource("http://www.w3.org/2004/02/skos/core#member");
		PackageEx ex = trans.getConfiguration().createPackageEx(pack.getKey(), pack.getValue());
		trans.getRange(resource, ex);
	}

}
