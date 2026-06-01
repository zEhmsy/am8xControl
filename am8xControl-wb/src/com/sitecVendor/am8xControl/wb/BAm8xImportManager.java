package com.sitecVendor.am8xControl.wb;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.workbench.mgr.BAbstractManager;
import javax.baja.workbench.mgr.MgrController;
import javax.baja.workbench.mgr.MgrLearn;
import javax.baja.workbench.mgr.MgrModel;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
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

    @Override
    public void updateContent() {
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

    void forceDiscoverOnlyLayout() {
        if (forcingDiscoverOnlyLayout) return;
        forcingDiscoverOnlyLayout = true;
        try {
            try {
                getController().learnMode.setSelected(true);
                getController().learnMode.setEnabled(false);
            } catch (Exception ignore) {}

            BWidget tablePane = widgetField("tablePane");
            if (tablePane != null) tablePane.setVisible(false);

            BWidget learnPane = widgetField("learnPane");
            if (learnPane != null) learnPane.setVisible(true);

            BSplitPane mainSplitter = splitField("mainSplitter");
            if (mainSplitter != null) {
                mainSplitter.setDividerPosition(100.0);
                mainSplitter.setDividerWidth(0.0);
                mainSplitter.setMoveableDivider(false);
                mainSplitter.relayout();
            }

            BSplitPane upperPaneSplitter = splitField("upperPaneSplitter");
            if (upperPaneSplitter != null) {
                upperPaneSplitter.setDividerPosition(100.0);
                upperPaneSplitter.setDividerWidth(0.0);
                upperPaneSplitter.setMoveableDivider(false);
                upperPaneSplitter.relayout();
            }

            if (learnPane != null) learnPane.relayout();
        } catch (Exception e) {
            LOG.fine("[Am8xImportManager] forceDiscoverOnlyLayout failed: " + e.getMessage());
        } finally {
            forcingDiscoverOnlyLayout = false;
        }
    }

    private BWidget widgetField(String fieldName) {
        Object value = managerField(fieldName);
        return value instanceof BWidget ? (BWidget) value : null;
    }

    private BSplitPane splitField(String fieldName) {
        Object value = managerField(fieldName);
        return value instanceof BSplitPane ? (BSplitPane) value : null;
    }

    private Object managerField(String fieldName) {
        try {
            Field f = BAbstractManager.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(this);
        } catch (Exception e) {
            LOG.fine("[Am8xImportManager] cannot read " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public BIcon getIcon() {
        return BIcon.make("module://am8xControl/img/am8xCentral.png");
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xImportManager.class);
}
