package com.sitecVendor.am8xControl.service;

import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryCandidate;
import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryReport;
import com.sitecVendor.am8xControl.discovery.BAm8xPanelFolder;
import com.sitecVendor.am8xControl.job.BAm8xCommitJob;
import com.sitecVendor.am8xControl.job.BAm8xDiscoverJob;
import com.sitecVendor.am8xControl.model.CandidateKey;
import com.sitecVendor.am8xControl.modbus.Am8xModbusAddressing;
import com.sitecVendor.am8xControl.modbus.Am8xAlarmAutomation;
import com.sitecVendor.am8xControl.modbus.ModbusPointFactory;
import com.sitecVendor.am8xControl.modbus.ModbusTreeBuilder;
import com.sitecVendor.am8xControl.modbus.BAm8xStatePoint;
import com.sitecVendor.am8xControl.modbus.BAm8xModuleStatePoint;
import com.sitecVendor.am8xControl.parser.Am8xDeviceDescriptor;
import com.sitecVendor.am8xControl.parser.Am8xSubModuleDescriptor;
import com.sitecVendor.am8xControl.parser.Am8xXmlParser;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusTcp.BModbusTcpNetwork;

import javax.baja.file.BIFile;
import javax.baja.job.BJob;
import javax.baja.job.BJobState;
import javax.baja.job.JobCancelException;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;
import javax.baja.util.Lexicon;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

/**
 * Servizio Niagara che importa la topologia AM-8200N da XML
 * e crea i point Modbus corrispondenti sotto un BModbusTcpDevice.
 *
 * Workflow:
 *   1. Imposta xmlFilePath, modbusIpAddress, modbusTcpPort, modbusDeviceSlot.
 *   2. Invoca discover() → popola il sotto-componente "discovery" con i candidate.
 *   3. Deseleziona eventuali device da escludere (o usa selectNone + selezione manuale).
 *   4. Invoca addSelected() → crea folder loop + point Modbus sotto /Drivers/...
 */
@NiagaraType
@NoSlotomatic
public final class BAm8xImportService extends BAbstractService {

    private static final Logger LOG = Logger.getLogger(BAm8xImportService.class.getName());
    private static final String DEFAULT_RESOURCE = "/resources/test.xml";
    private static final String DISCOVERY_SLOT   = "discovery";
    private static final String SHARED_ORD_PREFIX = "file:^shared/";
    private static final String STATION_ORD_PREFIX = "file:^";
    private static final String NIAGARA_USER_HOME_ORD_PREFIX = "file:!";
    private static final String LOCAL_ORD_PREFIX = "local:|";
    private static final String STATION_HOST_ORD_PREFIX = "station:|";
    private static final String SHARED_DIR_NAME = "shared";

    ////////////////////////////////////////////////////////////////
    // Properties — configurazione (editabili)
    ////////////////////////////////////////////////////////////////

    /**
     * Percorso file XML come BOrd: nel Property Sheet di WB il campo
     * mostra un pulsante "Edit…" che apre l'ORD chooser con accesso
     * al file system virtuale della station. Esempio: file:^shared/test.xml
     */
    public static final Property xmlFilePath =
            newProperty(Flags.SUMMARY, BOrd.DEFAULT, null);
    public BOrd getXmlFilePath() { return (BOrd) get(xmlFilePath); }
    public void setXmlFilePath(BOrd v) { set(xmlFilePath, v, null); }

    public static final Property modbusNetworkSlot =
            newProperty(Flags.SUMMARY, "ModbusTcpNetwork", null);
    public String getModbusNetworkSlot() { return getString(modbusNetworkSlot); }
    public void setModbusNetworkSlot(String v) { setString(modbusNetworkSlot, v, null); }

    public static final Property modbusIpAddress =
            newProperty(Flags.SUMMARY, "", null);
    public String getModbusIpAddress() { return getString(modbusIpAddress); }
    public void setModbusIpAddress(String v) { setString(modbusIpAddress, v, null); }

    public static final Property modbusTcpPort =
            newProperty(Flags.SUMMARY, 502, null);
    public int getModbusTcpPort() { return getInt(modbusTcpPort); }
    public void setModbusTcpPort(int v) { setInt(modbusTcpPort, v, null); }

    public static final Property modbusDeviceSlot =
            newProperty(Flags.SUMMARY, "AM8200N_Panel", null);
    public String getModbusDeviceSlot() { return getString(modbusDeviceSlot); }
    public void setModbusDeviceSlot(String v) { setString(modbusDeviceSlot, v, null); }

    public static final Property deviceAddressStart =
            newProperty(Flags.SUMMARY, 101, null);
    public int getDeviceAddressStart() { return getInt(deviceAddressStart); }
    public void setDeviceAddressStart(int v) { setInt(deviceAddressStart, v, null); }

    /**
     * Tipo di device parent da instanziare: ModbusTcp (default) o ModbusGateway.
     * Impostato dal popup Discover (BAm8xWizardInput) e consumato in doAddSelected.
     */
    public static final Property deviceType = newProperty(
            Flags.SUMMARY,
            BDynamicEnum.make(0, BAm8xWizardInput.DEVICE_TYPE_RANGE),
            BFacets.makeEnum(BAm8xWizardInput.DEVICE_TYPE_RANGE));
    public BDynamicEnum getDeviceType() { return (BDynamicEnum) get(deviceType); }
    public void setDeviceType(BDynamicEnum v) { set(deviceType, v, null); }
    public boolean isGatewayMode() { return getDeviceType().getOrdinal() == 1; }

    ////////////////////////////////////////////////////////////////
    // Properties — stato runtime (readonly)
    ////////////////////////////////////////////////////////////////

    public static final Property lastImportStatus =
            newProperty(Flags.SUMMARY | Flags.READONLY, "idle", null);
    public String getLastImportStatus() { return getString(lastImportStatus); }
    public void setLastImportStatus(String v) { setString(lastImportStatus, v, null); }

    public static final Property lastError =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLastError() { return getString(lastError); }
    public void setLastError(String v) { setString(lastError, v, null); }

    public static final Property parsedCount =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getParsedCount() { return getInt(parsedCount); }
    public void setParsedCount(int v) { setInt(parsedCount, v, null); }

    public static final Property addedCount =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getAddedCount() { return getInt(addedCount); }
    public void setAddedCount(int v) { setInt(addedCount, v, null); }

    // Upload XML dal WB client: il WB scrive bytes (base64) + nome qui, poi invoca uploadXml.
    public static final Property pendingUploadName =
            newProperty(Flags.HIDDEN | Flags.TRANSIENT, "", null);
    public String getPendingUploadName() { return getString(pendingUploadName); }
    public void setPendingUploadName(String v) { setString(pendingUploadName, v, null); }

    public static final Property pendingUploadB64 =
            newProperty(Flags.HIDDEN | Flags.TRANSIENT, "", null);
    public String getPendingUploadB64() { return getString(pendingUploadB64); }
    public void setPendingUploadB64(String v) { setString(pendingUploadB64, v, null); }

    ////////////////////////////////////////////////////////////////
    // Fault reporting — configFail()/configOk() rendono l'esito dell'import
    // visibile nell'albero della station, non solo in lastError/JUL.
    ////////////////////////////////////////////////////////////////

    /**
     * Traccia se il fault corrente è nostro. configOk() va chiamato SOLO dopo un
     * nostro configFail(), altrimenti si cancella il fault di qualcun altro.
     * Transiente per natura: lo stato di fault di un BComponent è runtime, non
     * persistito, quindi dopo un riavvio si riparte puliti (scelta della spec).
     */
    private boolean inConfigFail = false;

    private void fail(String lexKey, String detail) {
        inConfigFail = true;
        String msg = Lexicon.make("am8xControl").getText(lexKey);
        setLastError(msg + ": " + detail);
        configFail(msg + ": " + detail);
    }

    private void clearFail() {
        if (inConfigFail) { inConfigFail = false; configOk(); }
    }

    ////////////////////////////////////////////////////////////////
    // Actions
    ////////////////////////////////////////////////////////////////

    /** Decodifica pendingUploadB64 e scrive in ^shared/pendingUploadName. */
    public static final Action uploadXml = newAction(Flags.OPERATOR | Flags.HIDDEN, null);
    public void uploadXml() { invoke(uploadXml, null, null); }
    public void doUploadXml() {
        String name = getPendingUploadName();
        String b64 = getPendingUploadB64();
        if (name == null || name.isEmpty() || b64 == null || b64.isEmpty()) {
            setLastError("uploadXml: nome o bytes mancanti");
            return;
        }
        try {
            name = safeUploadFileName(name);
            byte[] data = java.util.Base64.getDecoder().decode(b64);
            File sharedDir = getStationSharedDir();
            Files.createDirectories(sharedDir.toPath());
            File dest = new File(sharedDir, name);
            Files.write(dest.toPath(), data);
            LOG.info("[Am8xImportService] uploadXml OK: " + dest.getAbsolutePath() + " (" + data.length + " bytes)");
            setLastImportStatus("upload OK: " + name);
            // Auto-imposta xmlFilePath sul file appena caricato
            setXmlFilePath(BOrd.make(sharedFileOrd(name)));
        } catch (Exception e) {
            LOG.severe("[Am8xImportService] uploadXml failed: " + e.getMessage());
            fail("import.fail.upload", e.getMessage());
        } finally {
            // Pulisci i buffer per non occupare memoria con i bytes
            setPendingUploadB64("");
            setPendingUploadName("");
        }
    }

    public static final Action setupAndDiscover = newAction(Flags.HIDDEN, new BAm8xWizardInput(), null);
    public void setupAndDiscover(BAm8xWizardInput input) { invoke(setupAndDiscover, input, null); }
    public void doSetupAndDiscover(BAm8xWizardInput input) {
        if (input.getXmlFilePath() != null && !BOrd.DEFAULT.equals(input.getXmlFilePath())) {
            setXmlFilePath(input.getXmlFilePath());
        }
        if (input.getModbusNetworkSlot() != null && !input.getModbusNetworkSlot().isEmpty()) {
            setModbusNetworkSlot(input.getModbusNetworkSlot());
        }
        if (input.getDefaultIpAddress() != null && !input.getDefaultIpAddress().isEmpty()) {
            setModbusIpAddress(input.getDefaultIpAddress());
        }
        if (input.getDefaultPort() > 0) {
            setModbusTcpPort(input.getDefaultPort());
        }
        if (input.getDeviceAddressStart() > 0) {
            setDeviceAddressStart(input.getDeviceAddressStart());
        }
        if (input.getDeviceType() != null) {
            setDeviceType(input.getDeviceType());
        }
        doDiscover();
    }

    /** Parsa l'XML e popola discovery/ con i BAm8xDiscoveryCandidate. */
    public static final Action discover = newAction(Flags.OPERATOR, null);
    public BOrd discover() { return (BOrd) invoke(discover, null, null); }
    public BOrd doDiscover() {
        clearFail();
        setLastError("");
        return new BAm8xDiscoverJob(this).submit(null);
    }

    /** Crea i point Modbus per i candidate con selected=true. Eseguito come BJob: vedi runCommit(BJob). */
    public static final Action addSelected = newAction(Flags.HIDDEN, null);
    public BOrd addSelected() { return (BOrd) invoke(addSelected, null, null); }
    public BOrd doAddSelected() {
        clearFail();
        return new BAm8xCommitJob(this).submit(null);
    }

    /**
     * Corpo di addSelected/commit, eseguito dentro BAm8xCommitJob.
     *
     * BSimpleJob chiama success() se questo metodo ritorna normalmente: la
     * cancellazione va quindi propagata come JobCancelException, mai con un
     * return silenzioso (vedi BAm8xDiscoverJob per lo stesso pattern).
     */
    public void runCommit(BJob job) throws Exception {
        // Dichiarati fuori dal try: il catch di JobCancelException deve poterli
        // leggere per registrare quanto era stato scritto prima della cancellazione.
        int created = 0;
        int skipped = 0;
        try {
            setLastError("");
            setLastImportStatus("addSelected: avvio…");

            BAm8xDiscoveryReport report = getDiscoveryReport();
            if (report == null) {
                fail("import.fail.noReport", "discovery report assente");
                setLastImportStatus("addSelected FAILED: eseguire prima discover()");
                throw new IllegalStateException("eseguire prima discover()");
            }

            // Navigate to /Drivers
            BComponent drivers = resolveDrivers();
            if (drivers == null) {
                fail("import.fail.noDrivers", "resolveDrivers() ha restituito null");
                setLastImportStatus("addSelected FAILED: /Drivers non trovato");
                LOG.warning("[Am8xImportService] addSelected failed: /Drivers non trovato");
                throw new IllegalStateException("/Drivers non trovato");
            }

            // Tipo device scelto nel popup Discover (ModbusTcp vs ModbusGateway)
            boolean gateway = isGatewayMode();
            LOG.info("[Am8xImportService] addSelected: deviceType ordinal="
                    + getDeviceType().getOrdinal() + " gateway=" + gateway);

            // Network slot: in gateway mode usa un nome dedicato (così il nodo è
            // chiaramente distinto da una rete TCP) a meno che l'utente non abbia
            // personalizzato modbusNetworkSlot oltre il default.
            String networkSlot = getModbusNetworkSlot();
            if (gateway && (networkSlot == null || networkSlot.isEmpty()
                    || "ModbusTcpNetwork".equals(networkSlot))) {
                networkSlot = "ModbusTcpGateway";
            }

            // Ensure Modbus network (devices are created per-panel below).
            // In gateway mode IP/port vivono sulla rete BModbusTcpGateway.
            BModbusTcpNetwork network;
            try {
                network = ModbusTreeBuilder.ensureNetwork(drivers, networkSlot, gateway,
                        getModbusIpAddress(), getModbusTcpPort() > 0 ? getModbusTcpPort() : 502);
                LOG.info("[Am8xImportService] addSelected: network slot=" + networkSlot
                        + " type=" + network.getType());
            } catch (Exception e) {
                setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
                setLastImportStatus("addSelected FAILED: " + e.getMessage());
                LOG.severe("[Am8xImportService] addSelected network failed: " + e.getMessage());
                throw e;
            }

            // ====== NUOVA LOGICA: CREAZIONE DEVICE STATICO "CENTRALE" ======
            Am8xAlarmAutomation.ensureAlarmClasses("Z");
            try {
                int port = getModbusTcpPort() > 0 ? getModbusTcpPort() : 502;
                BModbusClientDevice centraleDev = ModbusTreeBuilder.ensureDevice(
                        network, "CENTRALE", getModbusIpAddress(), port, 1, gateway);
                BComponent centralePoints = ModbusTreeBuilder.getPointsContainer(centraleDev);
                if (centralePoints != null) {
                    // Estrai tutte le zone dai candidati per la CENTRALE
                    java.util.Map<Integer, String> zones = new java.util.TreeMap<>();
                    for (BAm8xPanelFolder panel : report.getPanelFolders()) {
                        for (BAm8xDiscoveryCandidate c : panel.getCandidates()) {
                            int z = c.getZoneAddress();
                            if (z > 0) {
                                if (!zones.containsKey(z) || zones.get(z) == null || zones.get(z).isEmpty()) {
                                    zones.put(z, c.getZoneLabel());
                                }
                            }
                        }
                    }
                    ModbusPointFactory.populateCentralePoints(centralePoints, zones);
                }
            } catch (Exception e) {
                LOG.warning("[Am8xImportService] Errore creazione device CENTRALE: " + e.getMessage());
            }
            // ===============================================================

            // total = candidate selezionati, calcolato prima del ciclo: è il numero
            // che l'utente legge nella tabella, quindi la percentuale del job
            // corrisponde a qualcosa di riconoscibile.
            int total = 0;
            for (BAm8xPanelFolder panel : report.getPanelFolders()) {
                for (BAm8xDiscoveryCandidate c : panel.getCandidates()) {
                    if (c.getSelected()) total++;
                }
            }
            int done = 0;

            int panelsUsed = 0;
            int panelAlarmIndex = 0;

            for (BAm8xPanelFolder panel : report.getPanelFolders()) {
                // Trova lo slot name del panel dal suo Property nel report
                String panelSlot = panelSlotNameOf(report, panel);
                if (panelSlot == null) continue;
                panelAlarmIndex++;
                Am8xAlarmAutomation.AlarmClassNames panelAlarmClasses =
                        Am8xAlarmAutomation.ensureAlarmClasses(
                                Am8xAlarmAutomation.panelAlarmPrefix(panelSlot, panelAlarmIndex));

                // Crea/aggiorna BModbusTcpDevice con la config del panel
                String ip   = panel.getIpAddress();
                int    port = panel.getPort() > 0 ? panel.getPort() : 502;
                int    devAddr = panel.getDeviceAddress();

                BModbusClientDevice dev;
                try {
                    dev = ModbusTreeBuilder.ensureDevice(network, panelSlot, ip, port, devAddr, gateway);
                } catch (Exception e) {
                    LOG.warning("[Am8xImportService] ensureDevice for " + panelSlot
                            + " failed: " + e.getMessage());
                    continue;
                }

                BComponent pointsContainer = ModbusTreeBuilder.getPointsContainer(dev);
                if (pointsContainer == null) {
                    LOG.warning("[Am8xImportService] no 'points' slot on device " + panelSlot);
                    continue;
                }
                panelsUsed++;
                java.util.Set<String> parentModuleSlotsWithSubModules =
                        parentModuleSlotsWithSubModules(panel);

                for (BAm8xDiscoveryCandidate c : panel.getCandidates()) {
                    // Il check di cancellazione resta qui, prima dei filtri: la
                    // cancellazione deve restare reattiva anche durante una lunga
                    // sequenza di candidate deselezionati.
                    if (job.getJobState() == BJobState.canceling) throw new JobCancelException();

                    String loopSlot   = String.format("L%02d", c.getLoopNumber());
                    String deviceSlot = buildSlotName(c);     // es. "L01S001"

                    if (parentModuleSlotsWithSubModules.contains(deviceSlot)) {
                        removeLegacyParentModulePoint(pointsContainer, loopSlot, deviceSlot);
                        skipped++;
                        continue;
                    }

                    if (!c.getSelected()) { skipped++; continue; }

                    // total conta solo i candidate selezionati: done/progress
                    // avanzano solo qui, altrimenti supererebbero il 100% non
                    // appena il panel contiene candidate deselezionati.
                    done++;
                    job.progress(total == 0 ? 100 : done * 100 / total);

                    boolean sensorPoint = isSensorPointSlot(deviceSlot);

                    if (c.getAlreadyImported()) {
                        BAm8xStatePoint existingStatePt =
                                Am8xAlarmAutomation.findStatePoint(pointsContainer, loopSlot, deviceSlot);
                        if (existingStatePt != null) {
                            if (existingStatePt instanceof BAm8xModuleStatePoint) {
                                BComponent pointParent = existingStatePt.getParentComponent();
                                existingStatePt = ModbusPointFactory.migrateStatePointType(
                                        pointParent, existingStatePt);
                            }
                            if (existingStatePt != null) existingStatePt.ensureCommandConfigSlots();
                        }
                        Am8xAlarmAutomation.ensureStatePointAlarmExts(existingStatePt, panelAlarmClasses);
                        skipped++;
                        continue;
                    }

                    try {
                        BComponent loopFolder = ModbusTreeBuilder.ensureFolder(pointsContainer, loopSlot);

                        // Punto State custom (nome = deviceSlot, senza suffisso _State)
                        BAm8xStatePoint statePt = ModbusPointFactory.createStatePoint(
                                loopFolder, deviceSlot, c.getStateAddress(),
                                c.getDeviceType(), c.getDeviceLabel(),
                                c.getZoneAddress(), c.getZoneLabel());

                        if (statePt != null) {
                            statePt.ensureCommandConfigSlots();
                        }

                        javax.baja.control.BNumericPoint analogPt = null;
                        if (sensorPoint) {
                            String analogSlot = deviceSlot + "_Analog";
                            analogPt = ModbusPointFactory.createNumericPoint(
                                    loopFolder, analogSlot, c.getAnalogAddress());

                            Am8xAlarmAutomation.ensureValoreCameraAlarmExt(analogPt, panelAlarmClasses);

                            if (analogPt != null && statePt != null) {
                                ModbusPointFactory.createLink(analogPt, statePt, "valoreCamera");
                            }
                        }
                        Am8xAlarmAutomation.ensureStatePointAlarmExts(statePt, panelAlarmClasses);

                        c.setAlreadyImported(true);
                        if (statePt != null || analogPt != null) created++;
                        else                                     skipped++;
                    } catch (Exception e) {
                        LOG.warning("[Am8xImportService] addSelected candidate " + deviceSlot
                                + " failed: " + e.getMessage());
                    }
                }
            }

            setAddedCount(getAddedCount() + created);
            setLastImportStatus("addSelected OK — " + created + " creati, " + skipped
                    + " saltati, " + panelsUsed + " centrali");
            LOG.info("[Am8xImportService] addSelected: " + created + " created, "
                    + skipped + " skipped, " + panelsUsed + " panels");

        } catch (JobCancelException e) {
            // Cancellazione, non un fallimento: non toccare lastError (resta
            // esclusivo del path di fallimento genuino). lastImportStatus e
            // addedCount vanno comunque aggiornati con quanto scritto finora,
            // cosi' un nuovo commit puo' riprendere in modo idempotente.
            setAddedCount(getAddedCount() + created);
            setLastImportStatus("addSelected CANCELLED — " + created + " creati, " + skipped + " saltati");
            LOG.info("[Am8xImportService] addSelected cancelled: " + created + " created, "
                    + skipped + " skipped so far");
            throw e;
        } catch (Exception e) {
            fail("import.fail.job", e.getClass().getSimpleName() + ": " + e.getMessage());
            setLastImportStatus("commit FAILED: " + e.getMessage());
            LOG.severe("[Am8xImportService] commit failed: " + e.getMessage());
            throw e;
        }
    }

    /** Alias per addSelected() usato dalla UI */
    public static final Action commit = newAction(Flags.OPERATOR, null);
    public BOrd commit() { return (BOrd) invoke(commit, null, null); }
    public BOrd doCommit() { return doAddSelected(); }

    /** Rimuove tutti i candidate e formatta i counter */
    public static final Action clearAll = newAction(Flags.OPERATOR, null);
    public void clearAll() { invoke(clearAll, null, null); }
    public void doClearAll() {
        try {
            BValue v = get(DISCOVERY_SLOT);
            if (v instanceof BComponent) {
                remove(DISCOVERY_SLOT);
            }
        } catch (Exception ignore) {}
        setParsedCount(0);
        setAddedCount(0);
        setLastImportStatus("clear completato");
    }

    /** Trova lo slot name di un panel folder nel report iterando le sue properties. */
    private static String panelSlotNameOf(BAm8xDiscoveryReport report, BAm8xPanelFolder panel) {
        for (Property p : report.getPropertiesArray()) {
            try {
                if (report.get(p) == panel) return p.getName();
            } catch (Exception ignore) {}
        }
        return null;
    }

    private static void ensureDynamicProperty(BComponent parent, String name, BValue value) {
        try {
            if (parent.get(name) == null) {
                parent.add(name, value, Flags.SUMMARY | Flags.READONLY);
            }
        } catch (Exception ignore) {}
    }

    private BComponent resolveDrivers() {
        String[] ords = {
                "station:|slot:/Drivers",
                "local:|station:|slot:/Drivers",
                "slot:/Drivers"
        };
        for (String ord : ords) {
            try {
                BValue v = (BValue) BOrd.make(ord).resolve(this, null).get();
                if (v instanceof BComponent) return (BComponent) v;
            } catch (Exception e) {
                LOG.fine("[Am8xImportService] resolveDrivers ORD '" + ord
                        + "' failed: " + e.getMessage());
            }
        }

        try {
            BComponent root = this;
            while (root.getParentComponent() != null) {
                root = root.getParentComponent();
            }
            BValue v = root.get("Drivers");
            if (v instanceof BComponent) return (BComponent) v;
        } catch (Exception e) {
            LOG.fine("[Am8xImportService] resolveDrivers parent-walk failed: " + e.getMessage());
        }
        LOG.warning("[Am8xImportService] resolveDrivers failed: /Drivers not found from "
                + safeOrd());
        return null;
    }

    private void refreshAlarmAutomationForExistingTree() {
        try {
            BModbusTcpNetwork network = findExistingNetwork();
            if (network == null) return;

            int migrated = normalizeExistingStatePointTypes(network);
            int updated = Am8xAlarmAutomation.ensureExistingTreeAlarmExts(network);
            int commandPoints = ensureCommandConfigSlots(network);
            if (migrated > 0) {
                setLastImportStatus("service started: tipi punti OK — "
                        + migrated + " moduli aggiornati");
            } else if (updated > 0) {
                setLastImportStatus("service started: alarm automation OK — "
                        + updated + " punti aggiornati");
            } else if (commandPoints > 0) {
                setLastImportStatus("service started: Inhibit CMD OK — "
                        + commandPoints + " punti verificati");
            }
        } catch (Exception e) {
            LOG.warning("[Am8xImportService] refreshAlarmAutomationForExistingTree failed: "
                    + e.getMessage());
        }
    }

    private BModbusTcpNetwork findExistingNetwork() {
        BComponent drivers = resolveDrivers();
        if (drivers == null) return null;

        try {
            BValue v = drivers.get(getModbusNetworkSlot());
            if (v instanceof BModbusTcpNetwork) return (BModbusTcpNetwork) v;
        } catch (Exception ignore) {}

        for (Property p : drivers.getPropertiesArray()) {
            try {
                BValue v = drivers.get(p);
                if (v instanceof BModbusTcpNetwork) return (BModbusTcpNetwork) v;
            } catch (Exception ignore) {}
        }
        return null;
    }

    private static String buildSlotName(BAm8xDiscoveryCandidate c) {
        String name = c.getCandidateSlotName();
        if (name != null && !name.isEmpty()) return name;
        // Fallback for candidates discovered before candidateSlotName was added
        String type = c.getDeviceType();
        boolean isModule = type != null && type.startsWith("M");
        return isModule
            ? String.format("L%02dM%03d", c.getLoopNumber(), c.getPositionOnLoop())
            : String.format("L%02dS%03d", c.getLoopNumber(), c.getPositionOnLoop());
    }

    private static boolean isSensorPointSlot(String slotName) {
        return slotName != null && slotName.matches("L\\d+S\\d+");
    }

    private static java.util.Set<String> parentModuleSlotsWithSubModules(BAm8xPanelFolder panel) {
        java.util.Set<String> out = new java.util.HashSet<>();
        if (panel == null) return out;

        for (BAm8xDiscoveryCandidate c : panel.getCandidates()) {
            String slot = buildSlotName(c);
            int sep = slot == null ? -1 : slot.indexOf('_');
            if (sep > 0 && ModbusPointFactory.isModuleStateSlot(slot)) {
                out.add(slot.substring(0, sep));
            }
        }
        return out;
    }

    private static void removeLegacyParentModulePoint(
            BComponent pointsContainer, String loopSlot, String deviceSlot) {
        try {
            BValue loopValue = pointsContainer == null ? null : pointsContainer.get(loopSlot);
            if (!(loopValue instanceof BComponent)) return;

            BComponent loopFolder = (BComponent) loopValue;
            BValue existing = loopFolder.get(deviceSlot);
            if (existing instanceof BAm8xStatePoint) {
                loopFolder.remove(deviceSlot);
                LOG.info("[Am8xImportService] removed parent module point with submodules: "
                        + loopSlot + "/" + deviceSlot);
            }
        } catch (Exception e) {
            LOG.warning("[Am8xImportService] remove parent module point " + loopSlot
                    + "/" + deviceSlot + " failed: " + e.getMessage());
        }
    }

    private static int normalizeExistingStatePointTypes(BComponent parent) {
        if (parent == null) return 0;

        int count = 0;
        for (Property p : parent.getPropertiesArray()) {
            try {
                BValue child = parent.get(p);
                if (child instanceof BAm8xStatePoint) {
                    String slotName = p.getName();
                    if (child instanceof BAm8xModuleStatePoint) {
                        BAm8xStatePoint migrated =
                                ModbusPointFactory.migrateStatePointType(parent, (BAm8xStatePoint) child);
                        if (migrated != null && !(migrated instanceof BAm8xModuleStatePoint)) count++;
                    } else {
                        ((BAm8xStatePoint) child).ensureCommandConfigSlots();
                    }
                    continue;
                }

                if (child instanceof BComponent) {
                    count += normalizeExistingStatePointTypes((BComponent) child);
                }
            } catch (Exception ignore) {}
        }
        return count;
    }

    private static int ensureCommandConfigSlots(BComponent parent) {
        if (parent == null) return 0;

        int count = 0;
        if (parent instanceof BAm8xStatePoint) {
            BAm8xStatePoint point = (BAm8xStatePoint) parent;
            point.ensureCommandConfigSlots();
            return 1;
        }

        for (Property p : parent.getPropertiesArray()) {
            try {
                BValue child = parent.get(p);
                if (child instanceof BComponent) {
                    count += ensureCommandConfigSlots((BComponent) child);
                }
            } catch (Exception ignore) {}
        }
        return count;
    }

    /** Rimuove i point creati da questo servizio. Fase 5. */
    public static final Action clearImported = newAction(Flags.HIDDEN, null);
    public void clearImported() { invoke(clearImported, null, null); }
    public void doClearImported() {
        setLastImportStatus("clearImported: non ancora implementato (Fase 5)");
    }

    /** Seleziona tutti i candidate (selected=true). */
    public static final Action selectAll = newAction(Flags.HIDDEN, null);
    public void selectAll() { invoke(selectAll, null, null); }
    public void doSelectAll() {
        BAm8xDiscoveryReport report = getDiscoveryReport();
        if (report == null) { setLastImportStatus("selectAll: nessun discovery eseguito"); return; }
        for (BAm8xDiscoveryCandidate c : report.getCandidates()) c.setSelected(true);
        report.refreshSelectedCount();
        setLastImportStatus("selectAll: " + report.getTotalCandidates() + " selezionati");
    }

    /** Deseleziona tutti i candidate (selected=false). */
    public static final Action selectNone = newAction(Flags.HIDDEN, null);
    public void selectNone() { invoke(selectNone, null, null); }
    public void doSelectNone() {
        BAm8xDiscoveryReport report = getDiscoveryReport();
        if (report == null) { setLastImportStatus("selectNone: nessun discovery eseguito"); return; }
        for (BAm8xDiscoveryCandidate c : report.getCandidates()) c.setSelected(false);
        report.refreshSelectedCount();
        setLastImportStatus("selectNone: 0 selezionati");
    }

    ////////////////////////////////////////////////////////////////
    // BIService lifecycle
    ////////////////////////////////////////////////////////////////

    @Override
    public void serviceStarted() throws Exception {
        LOG.info("[Am8xImportService] serviceStarted — " + safeOrd());
        setLastImportStatus("service started");
        refreshAlarmAutomationForExistingTree();
    }

    @Override
    public void serviceStopped() throws Exception {
        LOG.info("[Am8xImportService] serviceStopped — " + safeOrd());
    }

    @Override
    public Type[] getServiceTypes() { return new Type[]{ TYPE }; }

    ////////////////////////////////////////////////////////////////
    // Internals — discovery
    ////////////////////////////////////////////////////////////////

    public BAm8xDiscoveryReport ensureDiscoveryReport() {
        // Try to reuse existing child
        try {
            BValue v = get(DISCOVERY_SLOT);
            if (v instanceof BAm8xDiscoveryReport) {
                // Forza HIDDEN sullo slot esistente (per nasconderlo dalla Database view del Manager)
                try {
                    Property p = (Property) getSlot(DISCOVERY_SLOT);
                    if (p != null) setFlags(p, Flags.HIDDEN);
                } catch (Exception ignore) {}
                return (BAm8xDiscoveryReport) v;
            }
            // Wrong type: remove and recreate
            for (Property p : getPropertiesArray()) {
                if (DISCOVERY_SLOT.equals(p.getName())) { remove(p); break; }
            }
        } catch (Exception ignore) {}
        BAm8xDiscoveryReport report = new BAm8xDiscoveryReport();
        try { add(DISCOVERY_SLOT, report, Flags.HIDDEN); } catch (Exception e) {
            LOG.warning("[Am8xImportService] cannot add discovery child: " + e.getMessage());
        }
        return report;
    }

    private BAm8xDiscoveryReport getDiscoveryReport() {
        try {
            BValue v = get(DISCOVERY_SLOT);
            return v instanceof BAm8xDiscoveryReport ? (BAm8xDiscoveryReport) v : null;
        } catch (Exception ignore) { return null; }
    }

    public void addCandidate(BAm8xPanelFolder parent, CandidateKey key,
                              Am8xDeviceDescriptor d, Am8xSubModuleDescriptor sub) {
        BAm8xDiscoveryCandidate c = new BAm8xDiscoveryCandidate();
        c.setPanelLabel(d.getPanelLabel());
        c.setLoopNumber(key.getLoop());
        c.setPositionOnLoop(key.getPos());
        c.setZoneAddress(sub != null ? sub.getZoneAddress() : d.getZoneAddress());
        c.setZoneLabel(sub != null ? sub.getZoneLabel() : d.getZoneLabel());

        if (sub != null) {
            c.setDeviceType(sub.getType());
            c.setDeviceLabel(sub.getLabel());
            c.setStateAddress(Am8xModbusAddressing.moduleState(
                    key.getLoop(), key.getParentModulePos(), key.getChannel()));
            c.setAnalogAddress(Am8xModbusAddressing.moduleAnalog(
                    key.getLoop(), key.getParentModulePos(), key.getChannel()));
        } else {
            c.setDeviceType(d.getDeviceType());
            c.setDeviceLabel(d.getLabel());
            if (key.getKind() == CandidateKey.Kind.MODULE) {
                c.setStateAddress(Am8xModbusAddressing.moduleState(key.getLoop(), key.getPos(), 0));
                c.setAnalogAddress(Am8xModbusAddressing.moduleAnalog(key.getLoop(), key.getPos(), 0));
            } else {
                c.setStateAddress(Am8xModbusAddressing.sensorState(key.getLoop(), key.getPos()));
                c.setAnalogAddress(Am8xModbusAddressing.sensorAnalog(key.getLoop(), key.getPos()));
            }
        }

        c.setCandidateSlotName(key.toSlotName());
        c.setPanelSlotName(key.toPanelSlotName());
        try {
            parent.add(key.toSlotName(), c, Flags.SUMMARY);
        } catch (Exception e) {
            LOG.warning("[Am8xImportService] cannot add candidate " + key.toSlotName()
                    + ": " + e.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////
    // Internals — XML loading
    ////////////////////////////////////////////////////////////////

    public List<Am8xDeviceDescriptor> loadDescriptors() throws Exception {
        BOrd ord = getXmlFilePath();
        if (ord != null && !BOrd.DEFAULT.equals(ord)) {
            String ordStr = ord.toString();
            if (ordStr != null && !ordStr.isEmpty() && !"null:".equals(ordStr)) {
                // Tenta più strategie per risolvere il path file
                Exception lastEx = null;
                String[] candidates = buildOrdCandidates(ordStr);
                for (String cand : candidates) {
                    try {
                        LOG.info("[Am8xImportService] tentativo resolve ORD: " + cand);
                        BOrd candOrd = BOrd.make(cand);
                        BValue resolved = (BValue) candOrd.resolve(this, null).get();
                        if (resolved instanceof BIFile) {
                            try (InputStream in = ((BIFile) resolved).getInputStream()) {
                                LOG.info("[Am8xImportService] file letto con ord: " + cand);
                                return Am8xXmlParser.parseStream(in);
                            }
                        }
                    } catch (Exception e) {
                        lastEx = e;
                        LOG.info("[Am8xImportService] ORD '" + cand + "' fallito: "
                                + e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                }
                // Fallback finale: solo path station-side ricavati dalle API Niagara.
                if (ordStr.contains("^") || ordStr.contains("~") || ordStr.contains("file:!")) {
                    try {
                        for (File f : buildFsFileCandidates(ordStr)) {
                            try {
                                if (f.exists() && f.canRead()) {
                                    LOG.info("[Am8xImportService] file letto via FS diretto: " + f.getAbsolutePath());
                                    try (InputStream in = new FileInputStream(f)) {
                                        return Am8xXmlParser.parseStream(in);
                                    }
                                } else {
                                    LOG.info("[Am8xImportService] FS path non trovato: " + f.getAbsolutePath());
                                }
                            } catch (Exception e) { lastEx = e; }
                        }
                    } catch (Exception e) {
                        lastEx = e;
                    }
                }
                String failDetail = lastEx != null
                        ? lastEx.getClass().getSimpleName() + " " + lastEx.getMessage()
                        : "tutti i tentativi falliti";
                fail("import.fail.xml", failDetail);
                throw new IllegalStateException("Impossibile leggere XML da '" + ordStr + "': "
                        + failDetail, lastEx);
            }
        }
        try (InputStream in = BAm8xImportService.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null)
                throw new IllegalStateException("Risorsa default non trovata: " + DEFAULT_RESOURCE);
            return Am8xXmlParser.parseStream(in);
        } catch (Exception e) {
            fail("import.fail.xml", e.getMessage());
            throw e;
        }
    }

    /**
     * Espande gli ORD supportati verso File station-side usando solo API Niagara.
     * Il caso primario e supportato dal wizard e': file:^shared/nome.xml.
     */
    private static File[] buildFsFileCandidates(String ordStr) {
        java.util.List<File> paths = new java.util.ArrayList<>();
        String clean = stripHostOrdPrefix(ordStr).replace('\\', '/');
        if (clean.startsWith(SHARED_ORD_PREFIX)) {
            addSharedFileCandidate(paths, clean.substring(SHARED_ORD_PREFIX.length()));
        } else if (clean.startsWith(NIAGARA_USER_HOME_ORD_PREFIX)) {
            addNiagaraUserHomeFileCandidate(paths, clean.substring(NIAGARA_USER_HOME_ORD_PREFIX.length()));
        } else if (clean.startsWith(STATION_ORD_PREFIX)) {
            String rel = clean.substring(STATION_ORD_PREFIX.length());
            if (isSimpleFileName(rel)) {
                addSharedFileCandidate(paths, rel);
            }
        }
        return paths.toArray(new File[0]);
    }

    /** Genera varianti dell'ORD da provare in sequenza. */
    private static String[] buildOrdCandidates(String original) {
        java.util.List<String> out = new java.util.ArrayList<>();
        addOrdCandidate(out, original);
        // Se ha local:| o station:| prefisso, prova anche senza
        if (original.startsWith(LOCAL_ORD_PREFIX)) {
            addOrdCandidate(out, original.substring(LOCAL_ORD_PREFIX.length()));
        }
        if (original.startsWith(STATION_HOST_ORD_PREFIX)) {
            addOrdCandidate(out, original.substring(STATION_HOST_ORD_PREFIX.length()));
        }
        // Se NON ha prefisso ma ha file:, prova anche il contesto station esplicito.
        if (original.startsWith("file:") && !original.contains("|")) {
            addOrdCandidate(out, STATION_HOST_ORD_PREFIX + original);
            addOrdCandidate(out, LOCAL_ORD_PREFIX + original);
        }
        if (isSimpleFileName(original)) {
            addOrdCandidate(out, sharedFileOrd(original));
        }
        return out.toArray(new String[0]);
    }

    private static void addSharedFileCandidate(java.util.List<File> out, String relativePath) {
        String safeRel = safeSharedRelativePath(relativePath);
        if (safeRel.isEmpty()) return;
        File sharedDir = getStationSharedDir();
        out.add(new File(sharedDir, safeRel.replace('/', File.separatorChar)));
    }

    private static void addNiagaraUserHomeFileCandidate(java.util.List<File> out, String relativePath) {
        String safeRel = safeSharedRelativePath(relativePath);
        if (safeRel.isEmpty()) return;
        out.add(new File(Sys.getNiagaraUserHome(), safeRel.replace('/', File.separatorChar)));
    }

    private static File getStationSharedDir() {
        return new File(Sys.getStationHome(), SHARED_DIR_NAME);
    }

    private static String stripHostOrdPrefix(String ordStr) {
        if (ordStr.startsWith(LOCAL_ORD_PREFIX)) {
            return ordStr.substring(LOCAL_ORD_PREFIX.length());
        }
        if (ordStr.startsWith(STATION_HOST_ORD_PREFIX)) {
            return ordStr.substring(STATION_HOST_ORD_PREFIX.length());
        }
        return ordStr;
    }

    private static String safeUploadFileName(String name) {
        String safe = name == null ? "" : name.trim().replace('\\', '/');
        int slash = safe.lastIndexOf('/');
        if (slash >= 0) safe = safe.substring(slash + 1);
        if (!isSimpleFileName(safe)) {
            throw new IllegalArgumentException("nome file XML non valido: " + name);
        }
        return safe;
    }

    private static String safeSharedRelativePath(String relativePath) {
        String safe = relativePath == null ? "" : relativePath.trim().replace('\\', '/');
        while (safe.startsWith("/")) safe = safe.substring(1);
        if (safe.indexOf(':') >= 0 || safe.equals(".") || safe.equals("..") || safe.startsWith("../")
                || safe.contains("/../") || safe.endsWith("/..")) {
            throw new IllegalArgumentException("percorso shared non valido: " + relativePath);
        }
        return safe;
    }

    private static boolean isSimpleFileName(String name) {
        return name != null
                && !name.isEmpty()
                && !".".equals(name)
                && !"..".equals(name)
                && name.indexOf('/') < 0
                && name.indexOf('\\') < 0
                && name.indexOf(':') < 0;
    }

    private static String sharedFileOrd(String fileName) {
        return SHARED_ORD_PREFIX + fileName;
    }

    private static void addOrdCandidate(java.util.List<String> out, String ord) {
        if (ord != null && !ord.isEmpty() && !out.contains(ord)) out.add(ord);
    }

    private String safeOrd() {
        try { return getNavOrd().toString(); } catch (Exception e) { return "<unmounted>"; }
    }

    ////////////////////////////////////////////////////////////////
    // Boilerplate
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xImportService.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON =
            BIcon.make("module://am8xControl/img/am8xNetwork.png");
}
