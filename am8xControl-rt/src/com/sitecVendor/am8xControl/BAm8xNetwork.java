package com.sitecVendor.am8xControl;

import com.tridium.ndriver.BNDeviceFolder;
import com.tridium.ndriver.BNNetwork;
import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Network driver radice per la centrale AM-8200N.
 *
 * <p>Implementa {@link BINDiscoveryHost} seguendo il pattern NDriver ufficiale:
 * {@link #submitDiscoveryJob} crea un {@link BNDiscoveryJob} e chiama
 * {@code job.submit(null)} — l'infrastruttura NDriver gestisce il job service
 * internamente e restituisce un ORD valido al Workbench.
 * Il vero lavoro avviene in {@link #getDiscoveryObjects}, chiamato dal job sul
 * lato RT: parsifica l'XML e restituisce i {@link BAm8xDiscoveryEntry}.</p>
 */
@NiagaraType
@NoSlotomatic
public class BAm8xNetwork extends BNNetwork implements BINDiscoveryHost {

    private static final Logger LOG = Logger.getLogger(BAm8xNetwork.class.getName());
    private static final String DEFAULT_RESOURCE = "/resources/test.xml";

    ////////////////////////////////////////////////////////////////
    // Properties
    ////////////////////////////////////////////////////////////////

    /** Path filesystem assoluto al file XML. Se vuoto usa la risorsa di default. */
    public static final Property xmlFilePath = newProperty(Flags.SUMMARY, "", null);
    public String getXmlFilePath() { return getString(xmlFilePath); }
    public void setXmlFilePath(String v) { setString(xmlFilePath, v, null); }

    /** Label delle centrali scoperte nell'ultimo discovery. */
    public static final Property lastPanelLabel =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLastPanelLabel() { return getString(lastPanelLabel); }
    public void setLastPanelLabel(String v) { setString(lastPanelLabel, v, null); }

    ////////////////////////////////////////////////////////////////
    // BNNetwork abstract
    ////////////////////////////////////////////////////////////////

    @Override public String getNetworkName()       { return "AM-8200N"; }
    @Override public Type   getDeviceType()        { return BAm8xDevice.TYPE; }
    @Override public Type   getDeviceFolderType()  { return BNDeviceFolder.TYPE; }

    ////////////////////////////////////////////////////////////////
    // BINDiscoveryHost
    ////////////////////////////////////////////////////////////////

    @Override
    public BNDiscoveryPreferences getDiscoveryPreferences() {
        return new BAm8xDiscoveryPreferences();
    }

    /**
     * Crea un {@link BNDiscoveryJob} e lo sottomette all'infrastruttura NDriver.
     * Restituisce un ORD valido che il Workbench usa per monitorare il job.
     * Il parsing XML avviene in {@link #getDiscoveryObjects}, chiamato dal job.
     */
    @Override
    public BOrd submitDiscoveryJob(BNDiscoveryPreferences prefs) {
        try {
            BNDiscoveryJob job = new BNDiscoveryJob(this);
            job.setDiscoveryPreferences(prefs);
            BOrd ord = job.submit(null);
            LOG.info("[am8x] submitDiscoveryJob: job submitted, ord=" + ord);
            return ord;
        } catch (Exception e) {
            LOG.severe("[am8x] submitDiscoveryJob failed: " + e.getMessage());
            return BOrd.make("null:");
        }
    }

    /**
     * Parsifica l'XML e restituisce i device come array di {@link BAm8xDiscoveryEntry}.
     * Chiamato dal {@link BNDiscoveryJob} sul lato RT durante l'esecuzione del job.
     */
    @Override
    public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
        List<Am8xDeviceDescriptor> descriptors = loadDescriptors();
        updatePanelSummary(descriptors);
        List<BAm8xDiscoveryEntry> entries = new ArrayList<>();
        for (Am8xDeviceDescriptor d : descriptors) {
            entries.add(new BAm8xDiscoveryEntry(d));
        }
        LOG.info("[am8x] getDiscoveryObjects: returning " + entries.size() + " entries");
        return entries.toArray(new BAm8xDiscoveryEntry[0]);
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

    private void updatePanelSummary(List<Am8xDeviceDescriptor> descriptors) {
        List<String> seen = new ArrayList<>();
        for (Am8xDeviceDescriptor d : descriptors) {
            String lbl = d.getPanelLabel();
            if (!lbl.isEmpty() && !seen.contains(lbl)) seen.add(lbl);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seen.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(seen.get(i));
        }
        setLastPanelLabel(sb.toString());
    }

    private String safe(String s) { return s == null ? "" : s; }

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
