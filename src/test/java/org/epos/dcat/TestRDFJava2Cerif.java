package org.epos.dcat;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.epos.dcat.cerif.RDF2Cerif;
import org.junit.Test;
import org.open.rdf.RDFMapper;

public class TestRDFJava2Cerif {

	@Test
	public void test() {
		try {
			Iterator<Path> iterator = Files.list(Paths.get("EPOS-DCAT-AP/examples")).iterator();
			while (iterator.hasNext()) {
				Path path = iterator.next();
				if (!path.toFile().isHidden()) {
					System.out.println(path.toString());
					handlePath(path);
				}
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void handlePath(Path path) {
		Model model = ModelFactory.createDefaultModel();
		try {
			model.read(path.toUri().toURL().toString(), null, "TURTLE");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		final RDFMapper mapper = new RDFMapper();
		StringWriter result = new StringWriter();
		model.write(result, "TURTLE");
		Object javaObject = mapper.read(model);
		RDF2Cerif rdf2cerif = new RDF2Cerif();
		if(javaObject instanceof Collection<?>) {
			for(Object obj : (Collection<?>)javaObject) {
				rdf2cerif.tocerifObj(obj);
			}
		}else
			rdf2cerif.tocerifObj(javaObject);
	}

}
