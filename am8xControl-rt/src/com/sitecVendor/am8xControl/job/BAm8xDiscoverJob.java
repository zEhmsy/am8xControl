package com.sitecVendor.am8xControl.job;

import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryReport;
import com.sitecVendor.am8xControl.discovery.BAm8xPanelFolder;
import com.sitecVendor.am8xControl.model.CandidateKey;
import com.sitecVendor.am8xControl.parser.Am8xDeviceDescriptor;
import com.sitecVendor.am8xControl.parser.Am8xSubModuleDescriptor;
import com.sitecVendor.am8xControl.service.BAm8xImportService;

import javax.baja.job.BJobState;
import javax.baja.job.BSimpleJob;
import javax.baja.job.JobCancelException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Discover come job: progress, cancel e stato finale gestiti dal framework.
 *
 * BSimpleJob implementa già doRun/doCancel; qui si implementa solo run().
 * ATTENZIONE: JobThread chiama success() se run() ritorna normalmente, quindi
 * la cancellazione va propagata come JobCancelException — un semplice break
 * farebbe risultare "riuscito" un job annullato.
 */
@NiagaraType
public class BAm8xDiscoverJob extends BSimpleJob {

    private static final Logger LOG = Logger.getLogger(BAm8xDiscoverJob.class.getName());

    /** Richiesto dalla deserializzazione: Niagara usa il costruttore vuoto. */
    public BAm8xDiscoverJob() {}

    public BAm8xDiscoverJob(BAm8xImportService service) { this.service = service; }

    /** Transiente: dopo un restart il campo è null e va risolto di nuovo. */
    private transient BAm8xImportService service;

    private BAm8xImportService service() {
        if (service == null) {
            service = (BAm8xImportService) Sys.getService(BAm8xImportService.TYPE);
        }
        return service;
    }

    private void checkCanceled() throws JobCancelException {
        if (getJobState() == BJobState.canceling) throw new JobCancelException();
    }

    @Override
    public void run(Context cx) throws Exception {
        BAm8xImportService svc = service();
        try {
            svc.setLastImportStatus("discover: parsing XML…");

            List<Am8xDeviceDescriptor> descriptors = svc.loadDescriptors();
            BAm8xDiscoveryReport report = svc.ensureDiscoveryReport();

            // Prima riga del run, non prima di lanciarlo: il job dev'essere
            // idempotente anche se rilanciato sullo stesso report.
            report.clearAllCandidates();

            int maxDeviceAddr = svc.getDeviceAddressStart() - 1;
            for (BAm8xPanelFolder pf : report.getPanelFolders()) {
                if (pf.getDeviceAddress() > maxDeviceAddr) maxDeviceAddr = pf.getDeviceAddress();
            }

            int total = descriptors.size();
            int done = 0;
            int count = 0;

            for (Am8xDeviceDescriptor d : descriptors) {
                checkCanceled();

                CandidateKey key = CandidateKey.forDevice(d);
                String panelSlot = key.toPanelSlotName();

                boolean isNew = report.getPanelFolder(panelSlot) == null;
                BAm8xPanelFolder panelFolder = report.ensurePanelFolder(panelSlot);

                panelFolder.setIpAddress(svc.getModbusIpAddress());
                panelFolder.setPort(svc.getModbusTcpPort() > 0 ? svc.getModbusTcpPort() : 502);
                if (isNew && panelFolder.getDeviceAddress() <= 0) {
                    panelFolder.setDeviceAddress(++maxDeviceAddr);
                }

                if (!d.hasSubModules()) {
                    svc.addCandidate(panelFolder, key, d, null);
                    count++;
                }
                for (Am8xSubModuleDescriptor sub : d.getSubModules()) {
                    CandidateKey subKey = CandidateKey.forSubModule(d, sub);
                    svc.addCandidate(panelFolder, subKey, d, sub);
                    count++;
                }

                // progress a ogni descriptor, anche quando non produce candidate
                done++;
                progress(total == 0 ? 100 : done * 100 / total);
            }

            svc.setParsedCount(count);
            report.setTotalCandidates(count);
            report.refreshSelectedCount();
            report.setLastRunTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            svc.setLastImportStatus("discover OK — " + descriptors.size() + " device, " + count + " candidate");
            LOG.info("[Am8xDiscoverJob] discover OK: " + count + " candidates");

        } catch (JobCancelException e) {
            // Cancellazione, non un fallimento: non toccare lastError/lastImportStatus.
            throw e;
        } catch (Exception e) {
            svc.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
            svc.setLastImportStatus("discover FAILED");
            LOG.severe("[Am8xDiscoverJob] discover failed: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDiscoverJob.class);
}
