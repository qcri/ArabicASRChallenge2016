package mgbmain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import mgbutils.ArabicUtils;
import mgbutils.MGBUtil;

/**
 * 
 * @author Sameer Khurana (skhurana@qf.org.qa)
 *
 */
public class ExtractLMText {

	public static void main(String[] args) {

		try {
			File[] xmlFiles = new File("/Users/alt-sameerk/Documents/aj_net/ARTICLES").listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return !pathname.isHidden();
				}
			});

			for (File fxmlFile : xmlFiles) {

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = null;
				try {
					doc = dBuilder.parse(fxmlFile);
				} catch (SAXParseException e) {
					System.out.println("not a good document cannot be parsed");
					continue;
				}
				doc.getDocumentElement().normalize();

				NodeList nList1 = doc.getElementsByTagName("doc");
				BufferedWriter bw = null;
				for (int j = 0; j < nList1.getLength(); j++) {
					Element e = (Element) nList1.item(j);
					NodeList nList = e.getElementsByTagName("field");
					for (int i = 0; i < nList.getLength(); i++) {
						bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/testLM.txt", true));
						Node nNode = nList.item(i);
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
							Element element = (Element) nNode;
							if (element.getAttribute("name").equals("Content")) {
								String content = MGBUtil.normalizeWord(element.getTextContent()
										.replaceAll("[،؟:/!,؛\"]", "").replaceAll("[^0-9٠-٩]\\.[^0-9٠-٩]", "")
										.replaceAll("[\\(\\)\\[\\]_]", "").replaceAll("\\s\\s^\n", " ")
										.replaceAll("[-\\.$]", "").replaceAll("[A-Za-z]", "")
										.replaceAll("(?m)^[\t]*\r?\n", ""));
								bw.write(ArabicUtils.utf82buck(content.trim()));
								bw.close();

							}
						}

					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
