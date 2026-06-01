package com.sitecVendor.am8xControl.modbus;

import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.ext.BLimitEnable;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.fault.BEnumFaultAlgorithm;
import javax.baja.alarm.ext.offnormal.BEnumChangeOfStateAlgorithm;
import javax.baja.alarm.ext.offnormal.BOutOfRangeAlgorithm;
import com.tridium.modbusTcp.BModbusTcpDevice;
import com.tridium.modbusTcp.BModbusTcpNetwork;
import javax.baja.control.BNumericPoint;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BValue;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.util.BFormat;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AlarmService automation for AM-8x imported enum state points.
 */
public final class Am8xAlarmAutomation {

    private static final Logger LOG = Logger.getLogger(Am8xAlarmAutomation.class.getName());
    private static final String SOURCE_NAME_FORMAT =
            "%parent.parent.parent.parent.displayName%%parent.displayName%%parent.deviceLabel%";
    private static final String VC_SOURCE_NAME_FORMAT =
            "%parent.parent.parent.parent.displayName%_%parent.displayName%";
    private static final String ESCLUSIONI_SUFFIX = "_ESCLUSIONI";
    private static final String ALLARMI_SUFFIX = "_ALLARMI";
    private static final String GUASTI_SUFFIX = "_GUASTI";
    private static final String ESCLUSIONI_EXT_SLOT = "alarmEsclusioni";
    private static final String ALLARMI_EXT_SLOT = "alarmAllarmi";
    private static final String GUASTI_EXT_SLOT = "alarmGuasti";
    private static final String VALORE_CAMERA_EXT_SLOT = "AlarmVC";
    private static final Pattern LAST_NUMBER = Pattern.compile("(\\d+)(?!.*\\d)");

    private Am8xAlarmAutomation() {}

    public static AlarmClassNames ensureAlarmClasses(String prefix) {
        AlarmClassNames names = new AlarmClassNames(
                prefix + ESCLUSIONI_SUFFIX,
                prefix + ALLARMI_SUFFIX,
                prefix + GUASTI_SUFFIX);

        BAlarmService alarmService = getAlarmService();
        if (alarmService == null) return names;

        ensureAlarmClass(alarmService, names.esclusioni);
        ensureAlarmClass(alarmService, names.allarmi);
        ensureAlarmClass(alarmService, names.guasti);
        return names;
    }

    public static String panelAlarmPrefix(String panelSlot, int fallbackIndex) {
        String source = panelSlot == null ? "" : panelSlot;
        Matcher matcher = LAST_NUMBER.matcher(source);
        if (matcher.find()) {
            try {
                return "C" + Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignore) {}
        }
        return "C" + fallbackIndex;
    }

    public static void ensureStatePointAlarmExts(BAm8xStatePoint point, AlarmClassNames classes) {
        if (point == null || classes == null) return;

        ensureEnumAlarmExt(point, ESCLUSIONI_EXT_SLOT, classes.esclusioni, 2, ModbusPointFactory.STATE_TAGS[2]);
        ensureEnumAlarmExt(point, ALLARMI_EXT_SLOT, classes.allarmi, 4, ModbusPointFactory.STATE_TAGS[4]);
        ensureEnumAlarmExt(point, GUASTI_EXT_SLOT, classes.guasti, 5, ModbusPointFactory.STATE_TAGS[5]);
    }

    public static void ensureValoreCameraAlarmExt(BNumericPoint point, AlarmClassNames classes) {
        if (point == null || classes == null) return;
        ensureOutOfRangeAlarmExt(point, VALORE_CAMERA_EXT_SLOT, classes.guasti);
    }

    public static int ensureExistingTreeAlarmExts(BModbusTcpNetwork network) {
        if (network == null) return 0;

        ensureAlarmClasses("Z");

        int panelIndex = 0;
        int updatedPoints = 0;
        for (Property p : network.getPropertiesArray()) {
            BValue child;
            try {
                child = network.get(p);
            } catch (Exception ignore) {
                continue;
            }

            String deviceSlot = p.getName();
            if (!(child instanceof BModbusTcpDevice)) continue;

            AlarmClassNames classes;
            if ("CENTRALE".equals(deviceSlot)) {
                classes = ensureAlarmClasses("Z");
            } else {
                panelIndex++;
                classes = ensureAlarmClasses(panelAlarmPrefix(deviceSlot, panelIndex));
            }
            BComponent pointsContainer = ModbusTreeBuilder.getPointsContainer((BModbusTcpDevice) child);
            updatedPoints += ensurePointAlarmExtsRecursive(pointsContainer, classes);
        }

        if (updatedPoints > 0) {
            LOG.info("[Am8xAlarmAutomation] refreshed alarm extensions on "
                    + updatedPoints + " existing points");
        }
        return updatedPoints;
    }

    public static BAm8xStatePoint findStatePoint(BComponent pointsContainer, String loopSlot, String pointSlot) {
        if (pointsContainer == null || loopSlot == null || pointSlot == null) return null;
        try {
            BValue loop = pointsContainer.get(loopSlot);
            if (!(loop instanceof BComponent)) return null;
            BValue point = ((BComponent) loop).get(pointSlot);
            return point instanceof BAm8xStatePoint ? (BAm8xStatePoint) point : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static BAlarmService getAlarmService() {
        try {
            return (BAlarmService) Sys.getService(BAlarmService.TYPE);
        } catch (ServiceNotFoundException e) {
            LOG.warning("[Am8xAlarmAutomation] AlarmService not found: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LOG.warning("[Am8xAlarmAutomation] cannot resolve AlarmService: " + e.getMessage());
            return null;
        }
    }

    private static BAlarmClass ensureAlarmClass(BAlarmService alarmService, String name) {
        try {
            BAlarmClass existing = alarmService.lookupAlarmClass(name);
            if (existing != null && name.equals(existing.getName())) return existing;
        } catch (Exception ignore) {}

        try {
            BValue existing = alarmService.get(name);
            if (existing instanceof BAlarmClass) return (BAlarmClass) existing;
        } catch (Exception ignore) {}

        BAlarmClass alarmClass = new BAlarmClass();
        try {
            alarmService.add(name, alarmClass, Flags.SUMMARY);
            BValue mounted = alarmService.get(name);
            if (mounted instanceof BAlarmClass) {
                LOG.info("[Am8xAlarmAutomation] created alarm class " + name);
                return (BAlarmClass) mounted;
            }
        } catch (Exception e) {
            LOG.warning("[Am8xAlarmAutomation] cannot create alarm class " + name + ": " + e.getMessage());
        }
        return alarmClass;
    }

    private static int ensurePointAlarmExtsRecursive(BComponent parent, AlarmClassNames classes) {
        if (parent == null) return 0;

        int count = 0;
        if (parent instanceof BAm8xStatePoint) {
            ensureStatePointAlarmExts((BAm8xStatePoint) parent, classes);
            return 1;
        }
        if (parent instanceof BNumericPoint && isValoreCameraPoint(parent)) {
            ensureValoreCameraAlarmExt((BNumericPoint) parent, classes);
            return 1;
        }

        for (Property p : parent.getPropertiesArray()) {
            try {
                BValue child = parent.get(p);
                if (child instanceof BComponent) {
                    count += ensurePointAlarmExtsRecursive((BComponent) child, classes);
                }
            } catch (Exception ignore) {}
        }
        return count;
    }

    private static void ensureEnumAlarmExt(BAm8xStatePoint point, String slotName,
                                           String alarmClass, int ordinal, String tag) {
        BAlarmSourceExt ext = getOrCreateAlarmExt(point, slotName);
        if (ext == null) return;

        try {
            BEnumChangeOfStateAlgorithm offnormal = new BEnumChangeOfStateAlgorithm();
            offnormal.setAlarmValues(BEnumRange.make(new int[] { ordinal }, new String[] { tag }));
            ext.setOffnormalAlgorithm(offnormal);

            BEnumFaultAlgorithm fault = new BEnumFaultAlgorithm();
            fault.setValidValues(BEnumRange.make(ModbusPointFactory.STATE_TAGS));
            ext.setFaultAlgorithm(fault);

            ext.setAlarmClass(alarmClass);
            ext.setSourceName(BFormat.make(SOURCE_NAME_FORMAT));
        } catch (Exception e) {
            LOG.warning("[Am8xAlarmAutomation] cannot configure " + slotName + " on "
                    + safePath(point) + ": " + e.getMessage());
        }
    }

    private static void ensureOutOfRangeAlarmExt(BNumericPoint point, String slotName,
                                                 String alarmClass) {
        BAlarmSourceExt ext = getOrCreateAlarmExt(point, slotName);
        if (ext == null) return;

        try {
            BOutOfRangeAlgorithm offnormal = new BOutOfRangeAlgorithm();
            offnormal.setHighLimit(75.0);
            offnormal.setDeadband(5.0);

            BLimitEnable limitEnable = new BLimitEnable();
            limitEnable.setHighLimitEnable(true);
            limitEnable.setLowLimitEnable(false);
            offnormal.setLimitEnable(limitEnable);

            ext.setOffnormalAlgorithm(offnormal);
            ext.setAlarmClass(alarmClass);
            ext.setSourceName(BFormat.make(VC_SOURCE_NAME_FORMAT));
            ext.setToOffnormalText(BFormat.make("Alto Valore Camera"));
            ext.setToNormalText(BFormat.make("Valore Camera Normale"));
        } catch (Exception e) {
            LOG.warning("[Am8xAlarmAutomation] cannot configure " + slotName + " on "
                    + safePath(point) + ": " + e.getMessage());
        }
    }

    private static boolean isValoreCameraPoint(BComponent point) {
        try {
            String name = point.getName();
            return name != null && name.endsWith("_Analog");
        } catch (Exception ignore) {
            return false;
        }
    }

    private static BAlarmSourceExt getOrCreateAlarmExt(BComponent point, String slotName) {
        try {
            BValue existing = point.get(slotName);
            if (existing instanceof BAlarmSourceExt) return (BAlarmSourceExt) existing;
            if (existing != null) {
                LOG.warning("[Am8xAlarmAutomation] slot " + slotName + " on "
                        + safePath(point) + " is not an alarm extension");
                return null;
            }
        } catch (Exception ignore) {}

        BAlarmSourceExt ext = new BAlarmSourceExt();
        try {
            point.add(slotName, ext, Flags.SUMMARY);
            BValue mounted = point.get(slotName);
            if (mounted instanceof BAlarmSourceExt) return (BAlarmSourceExt) mounted;
        } catch (Exception e) {
            LOG.warning("[Am8xAlarmAutomation] cannot add " + slotName + " to "
                    + safePath(point) + ": " + e.getMessage());
        }
        return ext;
    }

    private static String safePath(BComponent component) {
        try {
            return component.getSlotPath().toString();
        } catch (Exception e) {
            return component.getClass().getName();
        }
    }

    public static final class AlarmClassNames {
        public final String esclusioni;
        public final String allarmi;
        public final String guasti;

        private AlarmClassNames(String esclusioni, String allarmi, String guasti) {
            this.esclusioni = esclusioni;
            this.allarmi = allarmi;
            this.guasti = guasti;
        }
    }
}
