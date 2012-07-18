/**
 * @file Bmml2Dot.java
 * @author Reto Schneider, 2012
 * @brief A simple tool to generate a dot graph which shows the link depencies between mockups.
 * @link http://www.graphviz.org/
 *  
 * Example usage:
 *  java -jar bmml2dot.jar /path/to/bmmlfiles/*.bmml > Generated_Graph.dot
 *  dot -Tpng Generated_Graph.dot > Generated_Graph.png
 *  $YOURPICTUREVIEWER Generated_Graph.png
 */
package ch.reto_schneider.bmml2dot;

import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A simple class to generate a dot graph which shows the link depencies between
 * the given mockups.
 * 
 * @author reto
 * 
 */
public class Bmml2Dot {
	private static final String BMMLENCODING = "ISO-8859-1";
	static Pattern destinationFilenamePattern = Pattern
			.compile(".*&bm;(.+bmml)");
	static XPath xpath = XPathFactory.newInstance().newXPath();
	static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
			.newInstance();
	static String XPathQuery = "//control[controlProperties[href or hrefs and text]]";

	File bmmlFile = null;
	Vector<DotEdge> dotEdgeVector = null;
	XPathExpression controlsXpath = null;
	DocumentBuilder documentBuilder = null;

	/**
	 * Default constructor for Bmml2Dot.
	 * 
	 * @throws XPathExpressionException
	 * @throws ParserConfigurationException
	 */
	public Bmml2Dot() throws XPathExpressionException,
			ParserConfigurationException {
		dotEdgeVector = new Vector<DotEdge>();
		controlsXpath = xpath.compile(XPathQuery);
		documentBuilder = documentBuilderFactory.newDocumentBuilder();
	}

	/**
	 * Parse a given *.bmml file.
	 * 
	 * @param bmmlFilename
	 * @throws Exception
	 */
	public void parseFile(String bmmlFilename) throws Exception {
		bmmlFile = new File(bmmlFilename);
		Document document = (Document) documentBuilder.parse(bmmlFile);
		document.normalize();

		NodeList controlNodeList = (NodeList) controlsXpath.evaluate(document,
				XPathConstants.NODESET);

		for (int i = 0; i < controlNodeList.getLength(); i++) {
			Element textElement = null;
			Element controlPropertiesElement = null;
			Element controlElement = null;
			ArrayList<String> linkLabelArray = null;
			ArrayList<String> linkToArray = null;
			String textElementContent = null;

			controlElement = (Element) controlNodeList.item(i);
			controlPropertiesElement = extractSingleChildElement(
					controlElement, "controlProperties");

			textElement = extractSingleChildElement(controlPropertiesElement,
					"text");
			textElementContent = (textElement == null) ? ""
					: decodeText(textElement);
			linkLabelArray = splitText(
					controlElement.getAttribute("controlTypeID"),
					textElementContent);
			linkToArray = extractLinksTo(controlPropertiesElement, linkToArray);

			// if there are no links available, continue
			if (linkToArray == null) {
				continue;
			}
			assert (linkLabelArray.size() <= linkToArray.size());

			for (int j = 0; j < linkLabelArray.size(); j++) {
				String linkTo = linkToArray.get(j);
				String linkLabel = linkLabelArray.get(j);
				String linkFrom = bmmlFile.getName();
				Matcher m = destinationFilenamePattern.matcher(linkTo);

				if (m.find()) {
					if (m.groupCount() != 1) {
						continue;
					}
					DotEdge dotEdge = new DotEdge(m.group(1), linkFrom,
							linkLabel);
					dotEdgeVector.add(dotEdge);
				} else {
					System.err.println(linkFrom + ": '" + linkLabel
							+ "' has no matching link");
				}
			}
		}
	}

	/**
	 * Extract links (either of the subtag "href" or "hrefs", not both) in a
	 * given controlProperties element.
	 * 
	 * @param currentControlPropertiesElement
	 * @param linksTo
	 * @return
	 * @throws Exception
	 * @throws UnsupportedEncodingException
	 */
	private ArrayList<String> extractLinksTo(
			Element currentControlPropertiesElement, ArrayList<String> linksTo)
			throws Exception, UnsupportedEncodingException {
		Element currentHrefsNode;
		Element currentHrefNode;
		currentHrefNode = extractSingleChildElement(
				currentControlPropertiesElement, "href");
		currentHrefsNode = extractSingleChildElement(
				currentControlPropertiesElement, "hrefs");
		if (currentHrefsNode != null) {
			String currentHrefsNodeText = decodeText(currentHrefsNode);
			linksTo = new ArrayList<String>(Arrays.asList(currentHrefsNodeText
					.split(",", -1)));
		} else if (currentHrefNode != null) {
			String currentHrefNodeText = decodeText(currentHrefNode);
			linksTo = new ArrayList<String>(Arrays.asList(currentHrefNodeText
					.split(",", -1)));
		} else {
			return null;
		}
		return linksTo;
	}

	/**
	 * Split a given text into substrings. Splitting depends on the given
	 * controlTypeID.
	 * 
	 * @param controlTypeID
	 * @param elementText
	 * @return
	 */
	private ArrayList<String> splitText(String controlTypeID, String elementText) {
		ArrayList<String> texts;
		switch (controlTypeID) {
		case "com.balsamiq.mockups::TabBar":
			texts = new ArrayList<String>(Arrays.asList(elementText.split(",",
					-1)));
			break;
		case "com.balsamiq.mockups::Paragraph":
			texts = new ArrayList<>();
			texts.add(elementText);
			break;
		default:
			texts = new ArrayList<String>(
					Arrays.asList(elementText.split("\n")));
		}
		return texts;
	}

	/**
	 * Decode an urlencoded text.
	 * 
	 * @param element
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String decodeText(Element element)
			throws UnsupportedEncodingException {
		return URLDecoder.decode(element.getTextContent(), BMMLENCODING);
	}

	/**
	 * Extract a single child element with the given tagname. If more than one
	 * element available, pick the first one.
	 * 
	 * @param parentElement
	 * @param tagname
	 * @return
	 * @throws Exception
	 */
	private Element extractSingleChildElement(Element parentElement,
			String tagname) throws Exception {
		Node extractedNode = parentElement.getElementsByTagName(tagname)
				.item(0);
		if (extractedNode instanceof Element) {
			Element extractedElement = (Element) extractedNode;
			return extractedElement;
		} else if (extractedNode == null) {
			return null;
		}
		throw new Exception(extractedNode.getNodeName());
	}

	/**
	 * Write the current graph as dot file.
	 * 
	 * @param outstream
	 */
	public void writeGraph(PrintStream outstream) {
		outstream.println("digraph G {");
		for (DotEdge currentDotEdge : dotEdgeVector) {
			outstream.println("\t\"" + currentDotEdge.sourceFilename
					+ "\" -> \"" + currentDotEdge.destinationFilename
					+ "\"[label=\"" + currentDotEdge.linkLabel + "\"]");
		}
		outstream.println("}");
	}

	/**
	 * To be called with all the filenames of the *.bmml files to be searched.
	 * 
	 * @param bmmlFilenameArray
	 */
	public static void main(String[] bmmlFilenameArray) {
		Bmml2Dot graph = null;
		try {
			graph = new Bmml2Dot();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (String bmmlFilename : bmmlFilenameArray) {
			try {
				graph.parseFile(bmmlFilename);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		graph.writeGraph(System.out);
	}
}
