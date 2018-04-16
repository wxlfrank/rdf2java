package org.open.rdfs;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;

/**
 * RDFSFile abstract a RDFS/OWL model and additional information, such as the
 * corresponding local file, the format of the model, the namespace of the
 * model, which are used during translation
 * 
 * @author wxlfrank
 *
 */
public class RDFSFile {
	public String localPath;
	public String format;
	private String namespace;
	private Model model;

	public RDFSFile(Path localPath, String format) {
		try {
			this.localPath = localPath.toUri().toURL().toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		this.format = format;
	}

	public RDFSFile(URL url, String format) {
		this.localPath = url.toString();
		this.format = format;
	}

	public String getLocalPath() {
		return localPath;
	}

	public String getFormat() {
		return format;
	}

	public void setNamespace(String ns) {
		this.namespace = ns;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return model;
	}
}
