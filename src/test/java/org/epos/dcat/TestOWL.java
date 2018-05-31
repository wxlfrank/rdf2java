package org.epos.dcat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.open.rdf.Configuration;
import org.open.rdfs.RDFSFile;
import org.open.rdfs.RDFSUtil;
import org.open.rdfs.java.JavaGenerateConfig;
import org.open.rdfs.java.JavaGenerator;
import org.open.rdfs.structure.Binding;
import org.open.rdfs.structure.ClassEx;
import org.open.rdfs.structure.FieldEx;
import org.open.rdfs.structure.PackageEx;
import org.open.rdfs.structure.RDFS2Structure;
import org.open.rdfs.structure.RDFSUtilWithCache;
import org.open.rdfs.SHACL;

public class TestOWL {

	@Test
	public void testOWL2Java() throws IOException {

		List<RDFSFile> files = new ArrayList<RDFSFile>();
		Files.list(Paths.get("ontology")).forEach(path -> {
			if (!path.toFile().isHidden())
				files.add(new RDFSFile(path, "TURTLE"));
		});
		RDFS2Structure transform = new RDFS2Structure(files);
		RDFSFile main = new RDFSFile(Paths.get("epos-dcat-ap_shapes.ttl"), "TURTLE");
		Collection<Binding> result = transform.translate(main);
		checkResult(result);
		JavaGenerator generator = new JavaGenerator(new JavaGenerateConfig());
		generator.generate(Paths.get("src/main/java").toFile(), result);
	}

	private void checkResult(Collection<Binding> result) {

		String uri = "http://www.epos-eu.org/epos/dcat-ap#";
		PackageEx epos_ap = null;
		for (Binding bind : result) {
			Assert.assertTrue(bind instanceof PackageEx);
			if (((PackageEx) bind).getPackage().getName().equals(uri))
				epos_ap = (PackageEx) bind;
		}
		Assert.assertNotNull(epos_ap);
		Map<Object, Binding> child = epos_ap.getContents();
		String[] Classes = { "Equipment" };
		for (String cls : Classes) {
			Assert.assertNotNull(child.get(cls));
		}
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
		RDFS2Structure trans = new RDFS2Structure(null);
		RDFSFile pack = trans.getConfiguration().readWriteModel("ontology/org.w3._2004_02_skos_core", "TURTLE");
		Resource resource = pack.getModel().getResource("http://www.w3.org/2004/02/skos/core#member");
		PackageEx ex = trans.createPackageEx(pack);
		ex.getRanges(resource);
	}

	private static String URI_BASE = "http://www.epos-eu.org/epos/dcat-ap#";

	private static String toURI(String base, String e) {
		return base + e;
	}

	private static String toURI(String e) {
		return toURI(URI_BASE, e);
	}

	private static Resource getResource(Model model, String e) {
		return model.getResource(e);
	}

	private static boolean isResource(Resource resource, String name) {
		return resource != null && resource.getLocalName().equals(name);
	}

	@Test
	public void testConstraints() {
		Model model = getModel("epos-dcat-ap_shapes.ttl", "TURTLE");
		Assert.assertNotNull(model);
		Resource personShape = getResource(model, toURI("PersonShape"));
		Resource DateOrDateTimeDataType = getResource(model, toURI("DateOrDateTimeDataType"));
		Assert.assertNotNull(personShape);
		Assert.assertNotNull(DateOrDateTimeDataType);
		RDFS2Structure top = new RDFS2Structure();
		RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;
		cache.traverseModel(model);
		String namespace = cache.getRealNamespace(model);
		PackageEx packageEx = new PackageEx(top, namespace, model);
		packageEx.handleConstraints(DateOrDateTimeDataType);
		RDFSUtil.getProperties(personShape, SHACL.property).forEach(shape -> {
			RDFNode path = RDFSUtil.getProperty(shape, SHACL.path);
			if (path instanceof Resource) {
				Resource pathResource = (Resource) path;
				if (isResource(pathResource, "address")) {
					Integer max = RDFSUtil.getMaxCount(shape);
					Assert.assertTrue(max == 1);
					Set<ClassEx> result = packageEx.handleConstraints(shape);
					Assert.assertTrue(!result.isEmpty());
					Assert.assertTrue(result.size() == 2);
				}
				if (isResource(pathResource, "familyName")) {
					Integer max = RDFSUtil.getMaxCount(shape);
					Assert.assertTrue(max == Integer.MAX_VALUE);
					Set<ClassEx> result = packageEx.handleConstraints(shape);
					Assert.assertTrue(!result.isEmpty());
				}
				if (isResource(pathResource, "contactPoint")) {
					Set<ClassEx> result = packageEx.handleConstraints(shape);
					Assert.assertTrue(!result.isEmpty());
				}
				if (isResource(pathResource, "identifier")) {
					Set<ClassEx> result = packageEx.handleConstraints(shape);
					Assert.assertTrue(result.size() == 3);
				}
			}
		});

		Resource property = getResource(model, toURI("resource"));
		Assert.assertNotNull(property);

		Set<FieldEx> fields = packageEx.toFieldEx(property);
		Assert.assertFalse(fields.isEmpty());
	}

	private Model getModel(String url, String format) {
		Model model = ModelFactory.createDefaultModel();
		model = model.read(url, format);
		return model;
	}
	
}
