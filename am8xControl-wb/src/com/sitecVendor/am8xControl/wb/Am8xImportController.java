package com.sitecVendor.am8xControl.wb;

import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryCandidate;
import com.sitecVendor.am8xControl.discovery.BAm8xPanelFolder;
import com.sitecVendor.am8xControl.service.BAm8xImportService;
import javax.baja.naming.BOrd;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.ui.BDialog;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.CommandEvent;
import javax.baja.workbench.mgr.MgrController;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * Controller per BAm8xImportManager.
 *
 * Workflow utente:
 *   - Discover  → popup parametri (file XML, IP, porta, deviceAddress start), poi discover
 *   - Clear All → svuota completamente il discovery report
 *   - Add       → marca tutti i device della centrale selezionata come da aggiungere
 *   - Add All   → marca tutti i device di tutte le centrali
 *   - Cancel    → demarca la centrale selezionata
 *   - Commit    → crea i Modbus device per i candidate marcati
 */
public class Am8xImportController extends MgrController {

    private static final String SHARED_ORD_PREFIX = "file:^shared/";
    private static final String STATION_USER_HOME_ORD = "station:|file:!";

    public Am8xImportController(BAm8xImportManager manager) {
        super(manager);
    }

    private BAm8xImportService getService() {
        return (BAm8xImportService) getManager().getCurrentValue();
    }

    private Am8xImportLearn getImportLearn() {
        return (Am8xImportLearn) getManager().getLearn();
    }

    private void refresh() {
        Am8xImportLearn learn = getImportLearn();
        learn.updateRoots(new Object[0]);
        learn.updateDiscoveryData();
        learn.updateTable();
        try {
            ((BAm8xImportManager) getManager()).forceDiscoverOnlyLayout();
        } catch (Exception ignore) {}
    }

    // ────────────────────────────────────────────────────────────────
    // Toolbar: ritorna i 6 comandi custom (Discover / Clear All / Add /
    // Add All / Cancel / Commit) — sostituisce i built-in.
    // ────────────────────────────────────────────────────────────────
    @Override
    protected IMgrCommand[] makeCommands() {
        return new IMgrCommand[]{
            new DiscoverCmd(),
            new ClearAllCmd(),
            new EditCandidateCmd(),
            new CancelCmd(),
            new CommitCmd()
        };
    }

    // ────────────────────────────────────────────────────────────────
    // Comandi
    // ────────────────────────────────────────────────────────────────

    private class DiscoverCmd extends MgrCommand {
        DiscoverCmd() { super(getManager(), "Discover"); setFlags(BARS); }
        @Override
        public CommandArtifact doInvoke(CommandEvent ev) throws Exception {
            if (!showDiscoverDialog()) return null;
            getService().discover();
            // discover() è ASYNC, fa polling refresh ogni 400ms per ~4s
            new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    try { Thread.sleep(400); } catch (InterruptedException ignore) {}
                    SwingUtilities.invokeLater(() -> refresh());
                }
            }, "Am8x-DiscoverPoll").start();
            return null;
        }
    }

    private class ClearAllCmd extends MgrCommand {
        ClearAllCmd() { super(getManager(), "Clear All"); setFlags(BARS); }
        @Override
        public CommandArtifact doInvoke(CommandEvent ev) throws Exception {
            getService().clearAll();
            refresh();
            return null;
        }
    }

    private class CancelCmd extends MgrCommand {
        CancelCmd() { super(getManager(), "Cancel"); setFlags(BARS); }
        @Override
        public CommandArtifact doInvoke(CommandEvent ev) throws Exception {
            BAm8xPanelFolder panel = getSelectedPanel();
            if (panel == null) {
                BDialog.error(getManager(), "Cancel", "Seleziona prima una centrale.", (Throwable) null);
                return null;
            }
            markPanel(panel, false);
            refresh();
            return null;
        }
    }

    private class EditCandidateCmd extends MgrCommand {
        EditCandidateCmd() { super(getManager(), "Edit Device"); setFlags(BARS); }
        @Override
        public CommandArtifact doInvoke(CommandEvent ev) throws Exception {
            Object[] sel = getLearnTable().getSelectedObjects();
            if (sel == null || sel.length != 1 || !(sel[0] instanceof BAm8xDiscoveryCandidate)) {
                BDialog.warning(getManager(), "Edit", "Seleziona un singolo device dalla lista.", (Throwable) null);
                return null;
            }
            BAm8xDiscoveryCandidate c = (BAm8xDiscoveryCandidate) sel[0];

            JTextField txtLabel = new JTextField(c.getDeviceLabel(), 20);
            JTextField txtZone = new JTextField(c.getZoneLabel(), 20);
            JTextField txtState = new JTextField(String.valueOf(c.getStateAddress()), 10);
            JTextField txtAnalog = new JTextField(String.valueOf(c.getAnalogAddress()), 10);

            JPanel p = new JPanel(new GridLayout(0, 2, 5, 5));
            p.add(new JLabel("Device Label:")); p.add(txtLabel);
            p.add(new JLabel("Zone Label:")); p.add(txtZone);
            p.add(new JLabel("State Address:")); p.add(txtState);
            p.add(new JLabel("Analog Address:")); p.add(txtAnalog);

            int r = JOptionPane.showConfirmDialog(null, p, "Edit Device " + c.getCandidateSlotName(), JOptionPane.OK_CANCEL_OPTION);
            if (r == JOptionPane.OK_OPTION) {
                // Update local copy
                c.setDeviceLabel(txtLabel.getText());
                c.setZoneLabel(txtZone.getText());
                try { c.setStateAddress(Integer.parseInt(txtState.getText())); } catch (Exception ignore) {}
                try { c.setAnalogAddress(Integer.parseInt(txtAnalog.getText())); } catch (Exception ignore) {}
                
                // Flush changes to station component
                try {
                    javax.baja.sys.BComponent stC = (javax.baja.sys.BComponent) c.getNavOrd().resolve(javax.baja.sys.Sys.getStation(), null).get();
                    stC.set("deviceLabel", javax.baja.sys.BString.make(txtLabel.getText()), null);
                    stC.set("zoneLabel", javax.baja.sys.BString.make(txtZone.getText()), null);
                    try { stC.set("stateAddress", javax.baja.sys.BInteger.make(Integer.parseInt(txtState.getText())), null); } catch (Exception ignore) {}
                    try { stC.set("analogAddress", javax.baja.sys.BInteger.make(Integer.parseInt(txtAnalog.getText())), null); } catch (Exception ignore) {}
                } catch (Exception ex) {
                    getManager().saveValue(); // Fallback to standard save if resolve fails
                }
                
                // Refresh leggero: ripopola senza resettare roots per preservare la selezione
                try {
                    Am8xImportLearn learn = (Am8xImportLearn) getManager().getLearn();
                    learn.updateDiscoveryData();
                    learn.updateTable();
                } catch (Exception ignore) {}
            }
            return null;
        }
    }

    private class CommitCmd extends MgrCommand {
        CommitCmd() { super(getManager(), "Commit"); setFlags(BARS); }
        @Override
        public CommandArtifact doInvoke(CommandEvent ev) throws Exception {
            // Conta selected e già-importati
            int totalSel = 0, alreadyImp = 0;
            BValue rv = getService().get("discovery");
            if (rv instanceof com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryReport) {
                for (BAm8xDiscoveryCandidate c :
                        ((com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryReport) rv).getCandidates()) {
                    if (c.getSelected()) {
                        totalSel++;
                        if (c.getAlreadyImported()) alreadyImp++;
                    }
                }
            }
            if (totalSel == 0) {
                BDialog.error(getManager(), "Commit",
                    "Nessuna centrale selezionata. Usa Cancel per deselezionare, ma almeno una deve restare attiva.",
                    (Throwable) null);
                return null;
            }
            // Se ci sono già-importati, chiedi conferma sovrascrittura
            if (alreadyImp > 0) {
                int r = BDialog.confirm(getManager(), "Conferma Commit",
                    "Stai per creare/aggiornare " + totalSel + " device.\n"
                    + alreadyImp + " di questi sono GIÀ presenti nel tree Modbus.\n\n"
                    + "I device esistenti verranno sovrascritti (eventuali modifiche manuali andranno perse).\n\n"
                    + "Continuare?",
                    BDialog.YES_NO);
                if (r != BDialog.YES) return null;
            }
            getService().commit(); // Esegue l'azione sulla station (RPC)
            refresh();
            
            String status = "Operazione completata";
            try {
                javax.baja.sys.BComponent stSvc = (javax.baja.sys.BComponent) getService().getNavOrd().resolve(javax.baja.sys.Sys.getStation(), null).get();
                status = ((javax.baja.sys.BString) stSvc.get("lastImportStatus")).getString();
            } catch (Exception ignore) {
                status = getService().getLastImportStatus();
            }

            BDialog.info(getManager(), "Commit Completato",
                "I dispositivi Modbus sono stati processati.\n" +
                "Dettaglio: " + status);
            return null;
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    /** Marca tutti i candidate della centrale come selected o no. */
    private static void markPanel(BAm8xPanelFolder panel, boolean selected) {
        for (BAm8xDiscoveryCandidate c : panel.getCandidates()) {
            c.setSelected(selected);
        }
    }

    /**
     * Recupera la centrale selezionata nella tabella di discovery.
     * Se l'utente seleziona un singolo device, ritorna la sua centrale padre.
     * Niagara può ritornare wrapper diversi a seconda della versione/contesto,
     * quindi gestiamo BComplex/BComponent generici.
     */
    private BAm8xPanelFolder getSelectedPanel() {
        try {
            Object[] sel = getLearnTable().getSelectedObjects();
            if (sel == null || sel.length == 0) return null;
            for (Object o : sel) {
                BAm8xPanelFolder pf = resolveToPanel(o);
                if (pf != null) return pf;
            }
        } catch (Exception ignore) {}
        return null;
    }

    /** Risolve un oggetto qualsiasi del Learn tree al panel folder corrispondente. */
    private static BAm8xPanelFolder resolveToPanel(Object o) {
        if (o == null) return null;
        if (o instanceof BAm8xPanelFolder) return (BAm8xPanelFolder) o;
        if (o instanceof BAm8xDiscoveryCandidate) {
            javax.baja.sys.BComplex parent = ((BAm8xDiscoveryCandidate) o).getParent();
            if (parent instanceof BAm8xPanelFolder) return (BAm8xPanelFolder) parent;
        }
        return null;
    }

    // ────────────────────────────────────────────────────────────────
    // Popup Discover: file XML + IP + porta + deviceAddress start
    // ────────────────────────────────────────────────────────────────

    private boolean showDiscoverDialog() {
        BAm8xImportService svc = getService();
        if (svc == null) return false;

        // Pre-fill con i valori correnti del service, o suggerisce un default station-side
        String curXml = sharedFileOrd("TEST.xml");
        try {
            BOrd o = svc.getXmlFilePath();
            if (o != null && !BOrd.DEFAULT.equals(o)) curXml = o.toString();
        } catch (Exception ignore) {}

        JTextField xmlField = new JTextField(curXml, 30);
        JTextField ipField = new JTextField(svc.getModbusIpAddress(), 15);
        JTextField portField = new JTextField(String.valueOf(svc.getModbusTcpPort()), 6);
        JTextField devAddrField = new JTextField(String.valueOf(svc.getDeviceAddressStart()), 6);

        // "PC…" — JFileChooser locale, carica e carica sulla station shared automaticamente
        JButton localBtn = new JButton("PC…");
        localBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("XML files", "xml"));
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                java.io.File localFile = fc.getSelectedFile();
                try {
                    // RPC/Fox: legge bytes dal client e demanda la scrittura al service sulla station.
                    byte[] data = java.nio.file.Files.readAllBytes(localFile.toPath());
                    String b64 = java.util.Base64.getEncoder().encodeToString(data);
                    String stationFileName = safeStationFileName(localFile.getName());
                    String stationOrd = sharedFileOrd(stationFileName);
                    BAm8xImportService uploadSvc = getService();
                    uploadSvc.setPendingUploadName(stationFileName);
                    uploadSvc.setPendingUploadB64(b64);
                    uploadSvc.uploadXml();  // invoca action station-side
                    xmlField.setText(stationOrd);
                    JOptionPane.showMessageDialog(null,
                        "File caricato sulla station:\n" + stationOrd
                        + "\n(" + data.length + " bytes)",
                        "Upload OK", JOptionPane.INFORMATION_MESSAGE);
                } catch (Throwable t) {
                    BDialog.error(getManager(), "Upload",
                        "Errore upload: " + t.getClass().getSimpleName() + ": " + t.getMessage(), (Throwable) null);
                }
            }
        });

        // "Station…" — BFileChooser Niagara per navigare file già sulla station
        JButton stationBtn = new JButton("Station…");
        stationBtn.addActionListener(e -> {
            try {
                javax.baja.ui.file.BFileChooser fc =
                    javax.baja.ui.file.BFileChooser.makeOpen(getManager());
                try { fc.setCurrentDirectory(BOrd.make(STATION_USER_HOME_ORD)); } catch (Exception ignore) {}
                javax.baja.naming.BOrd selOrd = fc.show();
                if (selOrd != null && !BOrd.DEFAULT.equals(selOrd)) {
                    xmlField.setText(toStationFileOrd(selOrd.toString()));
                }
            } catch (Throwable t) {
                BDialog.error(getManager(), "Browse Station",
                    "Impossibile aprire file chooser: " + t.getMessage(), (Throwable) null);
            }
        });

        JPanel btnPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
        btnPanel.add(localBtn);
        btnPanel.add(stationBtn);

        JPanel filePanel = new JPanel(new BorderLayout(4, 0));
        filePanel.add(xmlField, BorderLayout.CENTER);
        filePanel.add(btnPanel, BorderLayout.EAST);

        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.add(new JLabel(
            "<html><i><b>PC…</b>: seleziona dal tuo PC, viene copiato in station/shared automaticamente. "
            + "<b>Station…</b>: naviga lo user home Niagara della station.</i></html>"),
            BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("File XML:"));            panel.add(filePanel);
        panel.add(new JLabel(""));                     panel.add(helpPanel);
        panel.add(new JLabel("Modbus IP:"));           panel.add(ipField);
        panel.add(new JLabel("Porta Modbus:"));        panel.add(portField);
        panel.add(new JLabel("Device Address Start:")); panel.add(devAddrField);

        int result = JOptionPane.showConfirmDialog(
            null, panel, "Discover — Setup parametri",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return false;

        try {
            String xmlText = toStationFileOrd(xmlField.getText());
            if (!xmlText.isEmpty()) svc.setXmlFilePath(BOrd.make(xmlText));
            svc.setModbusIpAddress(ipField.getText().trim());
            svc.setModbusTcpPort(Integer.parseInt(portField.getText().trim()));
            svc.setDeviceAddressStart(Integer.parseInt(devAddrField.getText().trim()));
            return true;
        } catch (Exception ex) {
            BDialog.error(getManager(), "Discover",
                "Parametri non validi: " + ex.getMessage(), ex);
            return false;
        }
    }

    private static String toStationFileOrd(String text) {
        String ord = text == null ? "" : text.trim();
        if (ord.isEmpty()) return ord;
        int idx = ord.indexOf("file:");
        if (idx >= 0) return ord.substring(idx).replace('\\', '/');
        if (safeStationFileName(ord).equals(ord)) return sharedFileOrd(ord);
        throw new IllegalArgumentException(
            "Usa un ORD Niagara (es. file:^shared/configurazione.xml) oppure il pulsante PC...");
    }

    private static String safeStationFileName(String name) {
        String safe = name == null ? "" : name.trim().replace('\\', '/');
        int slash = safe.lastIndexOf('/');
        if (slash >= 0) safe = safe.substring(slash + 1);
        if (safe.isEmpty() || ".".equals(safe) || "..".equals(safe)
                || safe.indexOf('/') >= 0 || safe.indexOf('\\') >= 0 || safe.indexOf(':') >= 0) {
            throw new IllegalArgumentException("nome file XML non valido: " + name);
        }
        return safe;
    }

    private static String sharedFileOrd(String fileName) {
        return SHARED_ORD_PREFIX + fileName;
    }
}
