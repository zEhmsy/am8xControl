package com.sitecVendor.am8xControl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser standalone per file di topologia AM-8200N esportati in XML.
 * Struttura attesa:
 * &lt;TopologyImport&gt; &lt;PanelData Type="..."&gt; &lt;PanelLabel&gt;...&lt;/PanelLabel&gt;
 *   &lt;Device&gt;...&lt;/Device&gt; ... &lt;/PanelData&gt; &lt;/TopologyImport&gt;
 */
public final class Am8xXmlParser {

    private static final Logger LOG = Logger.getLogger(Am8xXmlParser.class.getName());

    private Am8xXmlParser() {}

    /**
     * Parse di un file XML su filesystem.
     *
     * @param file file XML di topologia
     * @return lista di descriptor, una entry per ogni &lt;Device&gt;
     * @throws Exception se la lettura o il parsing fallisce
     */
    public static List<Am8xDeviceDescriptor> parseFile(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return parseStream(in);
        }
    }

    /**
     * Parse di un payload XML in formato stringa.
     */
    public static List<Am8xDeviceDescriptor> parseString(String xml) throws Exception {
        if (xml == null) return new ArrayList<>();
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return parseStream(in);
        }
    }

    /**
     * Parse di un payload XML da uno stream.
     */
    public static List<Am8xDeviceDescriptor> parseStream(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        // hardening contro XXE
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(in));
        return extractDevices(doc);
    }

    private static List<Am8xDeviceDescriptor> extractDevices(Document doc) {
        List<Am8xDeviceDescriptor> result = new ArrayList<>();
        NodeList panels = doc.getElementsByTagName("PanelData");
        for (int i = 0; i < panels.getLength(); i++) {
            Element panel = (Element) panels.item(i);
            String panelType = panel.getAttribute("Type");
            String panelLabel = getChildText(panel, "PanelLabel");

            NodeList devices = panel.getElementsByTagName("Device");
            for (int j = 0; j < devices.getLength(); j++) {
                Element device = (Element) devices.item(j);
                try {
                    int loop = parseInt(getChildText(device, "LoopNumber"));
                    int pos = parseInt(getChildText(device, "PositionOnLoop"));
                    String type = getChildText(device, "Type");
                    String label = getChildText(device, "Label");
                    int zoneAddr = parseInt(getChildText(device, "ZoneAddress"));
                    String zoneLabel = getChildText(device, "ZoneLabel");

                    result.add(new Am8xDeviceDescriptor(
                            panelType, panelLabel, loop, pos, type, label, zoneAddr, zoneLabel));
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Skipped malformed <Device> entry", e);
                }
            }
        }
        return result;
    }

    private static String getChildText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() == 0) return "";
        Node first = children.item(0);
        if (first == null) return "";
        String txt = first.getTextContent();
        return txt == null ? "" : txt.trim();
    }

    private static int parseInt(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
