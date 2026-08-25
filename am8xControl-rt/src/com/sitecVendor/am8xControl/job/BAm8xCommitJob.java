package com.sitecVendor.am8xControl.job;

import com.sitecVendor.am8xControl.service.BAm8xImportService;

import javax.baja.job.BSimpleJob;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

/**
 * Commit come job: progress, cancel e stato finale gestiti dal framework.
 *
 * Il corpo vive nel service (usa diversi helper privati non spostabili senza
 * refactor fuori scope): vedi BAm8xImportService.runCommit(BJob).
 */
@NiagaraType
public class BAm8xCommitJob extends BSimpleJob {

    /** Richiesto dalla deserializzazione: Niagara usa il costruttore vuoto. */
    public BAm8xCommitJob() {}

    public BAm8xCommitJob(BAm8xImportService service) { this.service = service; }

    /** Transiente: dopo un restart il campo è null e va risolto di nuovo. */
    private transient BAm8xImportService service;

    private BAm8xImportService service() {
        if (service == null) {
            service = (BAm8xImportService) Sys.getService(BAm8xImportService.TYPE);
        }
        return service;
    }

    @Override
    public void run(Context cx) throws Exception { service().runCommit(this); }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xCommitJob.class);
}
