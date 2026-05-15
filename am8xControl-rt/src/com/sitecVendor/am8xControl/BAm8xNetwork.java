package com.sitecVendor.am8xControl;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Componente radice del modulo am8xControl.
 *
 * <p>Espone una proprieta' {@code xmlFilePath} per indicare il file di topologia
 * da importare e una action {@code discover} che esegue il parsing XML e
 * popola i {@link BAm8xDevice} figli con i dispositivi scoperti.</p>
 *
 * <p>Se {@code xmlFilePath} e' vuoto, viene caricata la copia di {@code test.xml}
 * inclusa nel JAR come risorsa di default.</p>
 */
@NiagaraType
@NoSlotomatic
public class BAm8xNetwork extends BComponent {

    private static final Logger LOG = Logger.getLogger(BAm8xNetwork.class.getName());

    private static final String DEFAULT_RESOURCE = "/resources/test.xml";

    ////////////////////////////////////////////////////////////////
    // Properties
    ////////////////////////////////////////////////////////////////

    /**
     * Path filesystem assoluto al file XML di topologia. Se vuoto, usa la
     * risorsa di default inclusa nel modulo.
     */
    public static final Property xmlFilePath = newProperty(Flags.SUMMARY, "", null);
    public String getXmlFilePath() { return getString(xmlFilePath); }
    public void setXmlFilePath(String v) { setString(xmlFilePath, v, null); }

    /**
     * Etichetta della centrale letta dall'ultimo discovery.
     */
    public static final Property lastPanelLabel =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLastPanelLabel() { return getString(lastPanelLabel); }
    public void setLastPanelLabel(String v) { setString(lastPanelLabel, v, null); }

    /**
     * Tipo della centrale letta dall'ultimo discovery.
     */
    public static final Property lastPanelType =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLastPanelType() { return getString(lastPanelType); }
    public void setLastPanelType(String v) { setString(lastPanelType, v, null); }

    /**
     * Numero di dispositivi importati nell'ultimo discovery.
     */
    public static final Property lastDiscoveryCount =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getLastDiscoveryCount() { return getInt(lastDiscoveryCount); }
    public void setLastDiscoveryCount(int v) { setInt(lastDiscoveryCount, v, null); }

    /**
     * Stato sintetico dell'ultimo discovery.
     */
    public static final Property lastDiscoveryStatus =
            newProperty(Flags.SUMMARY | Flags.READONLY, "idle", null);
    public String getLastDiscoveryStatus() { return getString(lastDiscoveryStatus); }
    public void setLastDiscoveryStatus(String v) { setString(lastDiscoveryStatus, v, null); }

    /**
     * Eventuale messaggio di errore dell'ultimo discovery.
     */
    public static final Property lastDiscoveryError =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLastDiscoveryError() { return getString(lastDiscoveryError); }
    public void setLastDiscoveryError(String v) { setString(lastDiscoveryError, v, null); }

    ////////////////////////////////////////////////////////////////
    // Actions
    ////////////////////////////////////////////////////////////////

    /**
     * Esegue il parsing del file XML configurato e popola i BAm8xDevice figli.
     */
    public static final Action discover = newAction(Flags.ASYNC, null);
    public void discover() { invoke(discover, null, null); }

    public void doDiscover() {
        try {
            List<Am8xDeviceDescriptor> descriptors = loadDescriptors();
            applyDescriptors(descriptors);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Discovery failed", e);
            setLastDiscoveryStatus("error");
            setLastDiscoveryError(e.getClass().getSimpleName() + ": " + e.getMessage());
            setLastDiscoveryCount(0);
        }
    }

    /**
     * Svuota la lista di device figli (rimuove tutti i BAm8xDevice).
     */
    public static final Action clearDiscovered = newAction(0, null);
    public void clearDiscovered() { invoke(clearDiscovered, null, null); }

    public void doClearDiscovered() {
        for (Slot slot : getSlotsArray()) {
            if (!(slot instanceof Property)) continue;
            Property p = (Property) slot;
            try {
                Object v = get(p);
                if ((v instanceof BAm8xDevice || v instanceof BAm8xDeviceFolder) && isDynamic(p)) {
                    remove(p);
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Failed to remove slot " + p.getName(), e);
            }
        }
        setLastDiscoveryCount(0);
        setLastDiscoveryStatus("cleared");
        setLastDiscoveryError("");
    }

    ////////////////////////////////////////////////////////////////
    // Internals
    ////////////////////////////////////////////////////////////////

    private List<Am8xDeviceDescriptor> loadDescriptors() throws Exception {
        String path = safe(getXmlFilePath());
        if (!path.isEmpty()) {
            File f = new File(path);
            if (!f.exists() || !f.canRead()) {
                throw new IllegalStateException("XML file not readable: " + path);
            }
            return Am8xXmlParser.parseFile(f);
        }
        try (InputStream in = BAm8xNetwork.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Default resource not found: " + DEFAULT_RESOURCE
                                + ". Set xmlFilePath explicitly.");
            }
            return Am8xXmlParser.parseStream(in);
        }
    }

    private void applyDescriptors(List<Am8xDeviceDescriptor> descriptors) {
        // rimuove folder e device dinamici esistenti
        for (Slot slot : getSlotsArray()) {
            if (!(slot instanceof Property)) continue;
            Property p = (Property) slot;
            try {
                Object v = get(p);
                if ((v instanceof BAm8xDevice || v instanceof BAm8xDeviceFolder) && isDynamic(p)) {
                    remove(p);
                }
            } catch (Exception ignore) { }
        }

        // raggruppa i descriptor per centrale (panelLabel)
        Map<String, List<Am8xDeviceDescriptor>> byPanel = new LinkedHashMap<>();
        Map<String, String> panelTypeMap = new LinkedHashMap<>();
        for (Am8xDeviceDescriptor d : descriptors) {
            String key = d.getPanelLabel().isEmpty() ? "PANEL" : d.getPanelLabel();
            byPanel.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
            panelTypeMap.putIfAbsent(key, d.getPanelType());
        }

        int totalCount = 0;
        List<String> panelNames = new ArrayList<>();
        List<String> panelTypes = new ArrayList<>();

        for (Map.Entry<String, List<Am8xDeviceDescriptor>> entry : byPanel.entrySet()) {
            String pLabel = entry.getKey();
            String pType = panelTypeMap.getOrDefault(pLabel, "");
            List<Am8xDeviceDescriptor> group = entry.getValue();

            BAm8xDeviceFolder folder = new BAm8xDeviceFolder();
            folder.setPanelInfo(pLabel, pType);

            String folderSlot = sanitizeSlotName(pLabel);
            folderSlot = uniqueSlotName(folderSlot);
            try {
                add(folderSlot, folder);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to add folder for panel " + pLabel, e);
                continue;
            }

            int folderCount = 0;
            for (Am8xDeviceDescriptor d : group) {
                try {
                    BAm8xDevice device = new BAm8xDevice();
                    device.applyDescriptor(d);
                    String devSlot = sanitizeSlotName(d.getSlotName());
                    devSlot = uniqueSlotInParent(folder, devSlot);
                    folder.add(devSlot, device);
                    folderCount++;
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to add device " + d.getSlotName(), e);
                }
            }

            totalCount += folderCount;
            panelNames.add(pLabel);
            panelTypes.add(pType);
        }

        setLastPanelLabel(join(panelNames, "; "));
        setLastPanelType(join(panelTypes, "; "));
        setLastDiscoveryCount(totalCount);
        setLastDiscoveryStatus(totalCount > 0 ? "ok" : "empty");
        setLastDiscoveryError("");
    }

    private boolean isDynamic(Property p) {
        try { return p.isDynamic(); } catch (Throwable t) { return false; }
    }

    private String sanitizeSlotName(String raw) {
        if (raw == null || raw.isEmpty()) return "dev";
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') sb.append(c);
            else sb.append('_');
        }
        String s = sb.toString();
        if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0))) {
            s = "d" + s;
        }
        return s;
    }

    private String uniqueSlotName(String base) {
        if (getSlot(base) == null) return base;
        int i = 2;
        while (getSlot(base + "_" + i) != null) i++;
        return base + "_" + i;
    }

    private String uniqueSlotInParent(BComponent parent, String base) {
        if (parent.getSlot(base) == null) return base;
        int i = 2;
        while (parent.getSlot(base + "_" + i) != null) i++;
        return base + "_" + i;
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    ////////////////////////////////////////////////////////////////
    // Boilerplate
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xNetwork.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON = BIcon.make("module://am8xControl/img/am8xNetwork.png");
}
