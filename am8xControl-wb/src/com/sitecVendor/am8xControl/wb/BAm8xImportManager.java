package com.sitecVendor.am8xControl.wb;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.workbench.mgr.BAbstractManager;
import javax.baja.workbench.mgr.MgrController;
import javax.baja.workbench.mgr.MgrLearn;
import javax.baja.workbench.mgr.MgrModel;
import javax.swing.SwingUtilities;
import java.util.logging.Logger;

@NiagaraType(
    agent = @AgentOn(types = "am8xControl:Am8xImportService")
)
public class BAm8xImportManager extends BAbstractManager {

    private static final Logger LOG = Logger.getLogger(BAm8xImportManager.class.getName());
    private boolean forcingDiscoverOnlyLayout;

    @Override
    protected MgrModel makeModel() {
        return new Am8xImportModel(this);
    }

    @Override
    protected MgrLearn makeLearn() {
        return new Am8xImportLearn(this);
    }

    @Override
    protected MgrController makeController() {
        return new Am8xImportController(this);
    }

    @Override
    public void doLoadValue(BObject subject, Context cx) {
        super.doLoadValue(subject, cx);
        SwingUtilities.invokeLater(() -> {
            try {
                getController().learnMode.setSelected(true);
                getController().doLearnMode(true);
                ((Am8xImportLearn) getLearn()).updateDiscoveryData();
                forceDiscoverOnlyLayout();
            } catch (Exception ignore) {}
        });
    }

    /**
     * Il guard va PRIMA di super.updateContent(), non solo dentro
     * forceDiscoverOnlyLayout(): il relayout() dello splitter rientra qui, e
     * super.updateContent() rimette il divisore a 50 (tableLearnSplit) a ogni
     * passaggio. Guardando solo il nostro metodo, il giro rientrante annullava
     * la correzione appena applicata e il pannello tornava a metà altezza.
     */
    @Override
    public void updateContent() {
        if (forcingDiscoverOnlyLayout) return;
        super.updateContent();
        forceDiscoverOnlyLayout();
    }

    @Override
    public void restoreState() {
        forceDiscoverOnlyLayout();
    }

    @Override
    public void saveState() {
        // Intenzionalmente vuoto: questa UI resta sempre in modalità Discover.
    }

    @Override
    public void doHandleDbSelection() {
        forceDiscoverOnlyLayout();
    }

    /**
     * Tiene la vista sul solo pannello Discover.
     *
     * Il pannello Database non esiste come widget — Am8xImportModel.makePane()
     * restituisce un BNullWidget — ma BAbstractManager.updateContent() lo mette
     * comunque come widget2 di mainSplitter e a OGNI passaggio rimette il
     * divisore a 50: senza questa correzione metà vista resterebbe vuota e
     * ridimensionabile.
     *
     * Lo splitter si ottiene risalendo dal pannello Learn con getParentWidget()
     * (public final su BWidget). La versione precedente leggeva i campi
     * package-private di BAbstractManager per riflessione: falliva in silenzio,
     * quasi certamente perché setAccessible(true) non è concesso ai moduli sotto
     * il SecurityManager del Workbench, e il pannello Database restava visibile.
     */
    void forceDiscoverOnlyLayout() {
        if (forcingDiscoverOnlyLayout) return;
        forcingDiscoverOnlyLayout = true;
        try {
            try {
                getController().learnMode.setSelected(true);
                getController().learnMode.setEnabled(false);
            } catch (Exception ignore) {}

            BWidget learnPane = learnPane();
            if (learnPane != null) learnPane.setVisible(true);

            BSplitPane splitter = mainSplitter();
            if (splitter != null) {
                splitter.setDividerPosition(100.0);
                splitter.setDividerWidth(0.0);
                splitter.setMoveableDivider(false);
                splitter.relayout();
            } else {
                LOG.info("[Am8xImportManager] mainSplitter non trovato: il pannello "
                        + "Discover resterà a metà altezza");
            }

            if (learnPane != null) learnPane.relayout();
        } catch (Exception e) {
            LOG.info("[Am8xImportManager] forceDiscoverOnlyLayout failed: " + e);
        } finally {
            forcingDiscoverOnlyLayout = false;
        }
    }

    /** Il pannello Learn, catturato da Am8xImportLearn.makePane(). */
    private BWidget learnPane() {
        MgrLearn learn = getLearn();
        return learn instanceof Am8xImportLearn ? ((Am8xImportLearn) learn).pane() : null;
    }

    /**
     * Lo splitter verticale che BAbstractManager.updateContent() mette al centro
     * del BEdgePane della vista, con il Learn come widget1.
     *
     * Due strade, entrambe API pubbliche. Dall'alto (getContent().getCenter())
     * è quella deterministica, ma vale solo dopo init(): setContent(content)
     * viene invocato DOPO updateContent(), quindi al primo giro getContent()
     * restituisce ancora il BNullWidget iniziale. Lì si risale dal pannello
     * Learn, che a quel punto è già widget1 dello splitter.
     */
    private BSplitPane mainSplitter() {
        BWidget content = getContent();
        if (content instanceof BEdgePane) {
            BWidget center = ((BEdgePane) content).getCenter();
            if (center instanceof BSplitPane) return (BSplitPane) center;
        }
        BWidget learnPane = learnPane();
        BWidget parent = learnPane == null ? null : learnPane.getParentWidget();
        return parent instanceof BSplitPane ? (BSplitPane) parent : null;
    }

    @Override
    public BIcon getIcon() {
        return BIcon.make("module://am8xControl/img/am8xCentral.png");
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xImportManager.class);
}
