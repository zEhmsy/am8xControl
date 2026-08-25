package com.sitecVendor.am8xControl.job;

import com.sitecVendor.am8xControl.service.BAm8xImportService;

import javax.baja.job.BSimpleJob;
import javax.baja.job.JobCancelException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

import java.util.logging.Logger;

/**
 * Riapplica i display name a un albero già importato. Idempotente.
 *
 * BSimpleJob implementa già doRun/doCancel; qui si implementa solo run().
 * ATTENZIONE: JobThread chiama success() se run() ritorna normalmente, quindi
 * la cancellazione va propagata come JobCancelException — un semplice break
 * farebbe risultare "riuscito" un job annullato.
 */
@NiagaraType
public class BAm8xDisplayNameJob extends BSimpleJob {

    private static final Logger LOG = Logger.getLogger(BAm8xDisplayNameJob.class.getName());

    /** Richiesto dalla deserializzazione: Niagara usa il costruttore vuoto. */
    public BAm8xDisplayNameJob() {}

    public BAm8xDisplayNameJob(BAm8xImportService service) { this.service = service; }

    /** Transiente: dopo un restart il campo è null e va risolto di nuovo. */
    private transient BAm8xImportService service;

    private BAm8xImportService service() {
        if (service == null) {
            service = (BAm8xImportService) Sys.getService(BAm8xImportService.TYPE);
        }
        return service;
    }

    @Override
    public void run(Context cx) throws Exception {
        BAm8xImportService svc = service();
        try {
            svc.runApplyDisplayNames(this);
        } catch (JobCancelException e) {
            // Cancellazione, non un fallimento: non toccare lastError/lastImportStatus.
            throw e;
        } catch (Exception e) {
            svc.reportJobFailure("import.fail.job", e.getClass().getSimpleName() + ": " + e.getMessage());
            svc.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
            svc.setLastImportStatus("applyDisplayNames FAILED");
            LOG.severe("[Am8xDisplayNameJob] applyDisplayNames failed: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDisplayNameJob.class);
}
