package com.extractor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class XmlSqlExtractor {

    public static String extractSql(List<Path> xmlFiles, String mapperFqcn, String methodName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Important: Disable DTD validation for faster parsing and avoiding network calls for MyBatis DTDs
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (Path xmlFile : xmlFiles) {
                try {
                    Document doc = builder.parse(xmlFile.toFile());
                    doc.getDocumentElement().normalize();

                    Element root = doc.getDocumentElement();
                    if ("mapper".equals(root.getNodeName())) {
                        String namespace = root.getAttribute("namespace");
                        if (mapperFqcn.equals(namespace)) {
                            // Found the right mapper. Now look for the statement with the matching id.
                            NodeList childNodes = root.getChildNodes();
                            for (int i = 0; i < childNodes.getLength(); i++) {
                                Node node = childNodes.item(i);
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    Element element = (Element) node;
                                    String id = element.getAttribute("id");
                                    if (methodName.equals(id)) {
                                        return formatXmlNode(node);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore parse errors on individual files
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String formatXmlNode(Node node) {
        try {
            javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new javax.xml.transform.dom.DOMSource(node), new javax.xml.transform.stream.StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            return node.getTextContent().trim(); // Fallback
        }
    }
}
