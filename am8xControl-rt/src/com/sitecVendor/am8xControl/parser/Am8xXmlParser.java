package com.sitecVendor.am8xControl.parser;

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
 * {@code <TopologyImport><PanelData Type="..."><PanelLabel>...</PanelLabel>
 *   <Device>...</Device></PanelData></TopologyImport>}
 *
 * Hardened contro XXE (doctype disabilitato, entità esterne off).
 */
public final class Am8xXmlParser {

    private static final Logger LOG = Logger.getLogger(Am8xXmlParser.class.getName());

    private Am8xXmlParser() {}

    public static List<Am8xDeviceDescriptor> parseFile(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return parseStream(in);
        }
    }

    public static List<Am8xDeviceDescriptor> parseString(String xml) throws Exception {
        if (xml == null) return new ArrayList<>();
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return parseStream(in);
        }
    }

    public static List<Am8xDeviceDescriptor> parseStream(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
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
            Element panel     = (Element) panels.item(i);
            String panelType  = panel.getAttribute("Type");
            String panelLabel = getChildText(panel, "PanelLabel");

            NodeList devices = panel.getElementsByTagName("Device");
            for (int j = 0; j < devices.getLength(); j++) {
                Element device = (Element) devices.item(j);
                try {
                    int    loop      = parseInt(getChildText(device, "LoopNumber"));
                    int    pos       = parseInt(getChildText(device, "PositionOnLoop"));
                    String type      = getChildText(device, "Type");
                    String label     = getChildText(device, "Label");
                    int    zoneAddr  = parseInt(getChildText(device, "ZoneAddress"));
                    String zoneLabel = getChildText(device, "ZoneLabel");
                    int    devTypeId = parseIntOrDefault(getChildText(device, "DeviceTypeID"), -1);

                    Am8xDeviceDescriptor desc = new Am8xDeviceDescriptor(
                            panelType, panelLabel, loop, pos, type, label, zoneAddr, zoneLabel, devTypeId);
                    parseSubModules(device, desc);
                    result.add(desc);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Skipped malformed <Device> entry", e);
                }
            }
        }
        return result;
    }

    private static void parseSubModules(Element device, Am8xDeviceDescriptor desc) {
        NodeList subModuleList = device.getElementsByTagName("SubModule");
        if (subModuleList.getLength() == 0) return;
        Element subModule = (Element) subModuleList.item(0);
        NodeList modules  = subModule.getElementsByTagName("Module");
        for (int i = 0; i < modules.getLength(); i++) {
            Element mod = (Element) modules.item(i);
            try {
                String type      = getChildText(mod, "Type");
                String label     = getChildText(mod, "Label");
                int    number    = parseInt(getChildText(mod, "Number"));
                int    zoneAddr  = parseInt(getChildText(mod, "ZoneAddress"));
                String zoneLabel = getChildText(mod, "ZoneLabel");
                desc.addSubModule(new Am8xSubModuleDescriptor(type, label, number, zoneAddr, zoneLabel));
            } catch (Exception e) {
                LOG.log(Level.FINE, "Skipped malformed <Module> entry", e);
            }
        }
    }

    /**
     * Ritorna il testo del primo figlio diretto di {@code parent} con tag
     * {@code tagName}. NON è ricorsivo (al contrario di
     * {@link Element#getElementsByTagName(String)}, che scenderebbe anche
     * dentro {@code <SubModule><Module>...}, leggendo il Type di un modulo
     * nidificato quando il Device padre ha il proprio {@code <Type>} vuoto).
     */
    private static String getChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && tagName.equals(n.getNodeName())) {
                String txt = n.getTextContent();
                return txt == null ? "" : txt.trim();
            }
        }
        return "";
    }

    private static int parseInt(String s) {
        return parseIntOrDefault(s, 0);
    }

    private static int parseIntOrDefault(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
