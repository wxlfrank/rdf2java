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
import org.junit.Test;
import org.open.Configuration;
import org.open.rdfs.PackageEx;
import org.open.rdfs.RDFSFile;
import org.open.rdfs.RDFSEx;
import org.open.rdfs.java.JavaGenerateConfig;
import org.open.rdfs.java.JavaGenerator;

public class TestOWL {

	@Test
	public void test2() throws MalformedURLException {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		model.read(Paths.get("ontology/epos-dcat-ap_shapes.ttl").toUri().toURL().toString(), "TURTLE");
		Model base = model.getBaseModel();
		base.listSubjects().forEachRemaining(resource -> {
			if (resource.isAnon())
				System.out.println(resource.getId());
			else
				System.out.println(resource.getURI());
		});
	}

	@Test
	public void testOWL2Java() throws IOException {

		List<RDFSFile> files = new ArrayList<RDFSFile>();
		Files.list(Paths.get("ontology")).forEach(path -> {
			try {
				if (!path.toFile().isHidden())
					files.add(new RDFSFile(path.toUri().toURL().toString(), "TURTLE"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		});
		RDFSEx transform = new RDFSEx(files);
		RDFSFile main = new RDFSFile(Paths.get("epos-dcat-ap_shapes.ttl").toUri().toURL().toString(), "TURTLE");
		JavaGenerator generator = new JavaGenerator(new JavaGenerateConfig());
		generator.generate(Paths.get("src/main/java").toFile(), transform.translate(main));
	}

	@Test
	public void testPackageName() {
		Configuration config = new Configuration();
		String name = "http://xmlns.com/foaf/0.1/#";
		System.out.println(name + "->" + config.getPackageName(name));
		name = "http://www.w3.org/2000/01/rdf-schema#";
		System.out.println(name + "->" + config.getPackageName(name));
	}

	@Test
	public void testRange() {
		RDFSEx trans = new RDFSEx(null);
		Entry<String, Model> pack = trans.getConfig().readWriteModel("ontology/org.w3._2004_02_skos_core", "TURTLE");
		Resource resource = pack.getValue().getResource("http://www.w3.org/2004/02/skos/core#member");
		PackageEx ex = trans.createPackageEx(pack.getKey(), pack.getValue());
		ex.getRanges(resource);
	}

}
