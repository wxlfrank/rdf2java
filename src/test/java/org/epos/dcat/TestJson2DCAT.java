package org.epos.dcat;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;

import org.epos_ip.dcatflat.Baseline;
import org.epos_ip.dcatflat.XMLReader;
import org.junit.Test;

public class TestJson2DCAT {
	@Test
	public void test() throws FileNotFoundException, JAXBException {

		XMLReader reader = new XMLReader();
		Baseline baseline = reader.readXML("EPOS-DCAT-AP_example.xml");
		final TransformDCAT dcat = new TransformDCAT();
		baseline.getPersons().forEach(person -> {
			try {
				dcat.transform(person);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});;
	}
	@Test
	public void testClass(){
	}
}
