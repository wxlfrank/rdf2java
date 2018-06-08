package org.open.rdfs.structure;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.open.rdf.Configuration;
import org.open.rdfs.RDFSFile;

public class RDFSExConfig extends Configuration {

	/**
	 * Read a model corresponding to a url<br>
	 * If the url is not <a href="http://www.w3.org/2001/XMLSchema#">XMLSchema</a>
	 * and not <a href="http://schema.org/#">schema.org</a> (Temporary
	 * handling, @TODO handle these two cases in the future)<br>
	 * <ol>
	 * <li>read the model using the {@code url}</li>
	 * <li>find the real namespace of the model</li>
	 * <li>write the model in TURTLE format locally</li>
	 * </ol>
	 * 
	 * @param url
	 *            the url for the model to be read
	 * @param format
	 *            the format of the model to be read
	 * @return an {@link Entry} entry where the key is the real namespace of the
	 *         model and the value is the newly read model
	 */
	public RDFSFile readWriteModel(String url, String format) {
		try {
			RDFSFile rdfs = new RDFSFile(new URL(url), format);
			readWriteModel(rdfs);
			return rdfs;
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}

	private static Set<String> STATIC_URLS = new LinkedHashSet<String>();
	static {
		STATIC_URLS.addAll(Arrays.asList("http://www.w3.org/2001/XMLSchema#", 
//				"http://schema.org/#",
				"http://spdx.org/rdf/terms#"));
	}

	private static final RDFSUtilWithCache RDFSCACHE = RDFSUtilWithCache.INSTANCE;

	/**
	 * Read the corresponding model from web and save it locally if necessary
	 * 
	 * @param rdfs
	 */
	public Model readWriteModel(RDFSFile rdfs) {
		Model model = ModelFactory.createDefaultModel();
		rdfs.setModel(model);
		String url = rdfs.getLocalPath();
		if (!STATIC_URLS.contains(url)) {
			model = model.read(url, rdfs.getFormat());
			if(url.endsWith("org.schema")) {
				System.out.println("");
			}
			// base = (OntModel) base.union(model);
			RDFSCACHE.traverseModel(model);
			String ns = RDFSCACHE.getRealNamespace(model);
			if (url.equals(ns)) {
				String localPath = "ontology/" + getPackageName(ns);
				try {
					// save the model locally in turtle format
					model.write(new FileWriter(localPath), "TURTLE");
					rdfs.setLocalPath(Paths.get(localPath).toUri().toURL().toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			rdfs.setNamespace(ns);
		} else {
			rdfs.setNamespace(url);
			rdfs.setLocalPath(null);
		}
		return model;
	}
}
