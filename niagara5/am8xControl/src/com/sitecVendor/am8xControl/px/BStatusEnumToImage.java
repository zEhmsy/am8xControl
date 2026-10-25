package com.sitecVendor.am8xControl.px;

import niagara.gx.BImage;
import niagara.status.BIStatus;
import niagara.status.BStatus;
import niagara.sys.BComponent;
import niagara.sys.BEnum;
import niagara.sys.BIEnum;
import niagara.sys.BObject;
import niagara.sys.BValue;
import niagara.sys.Context;
import niagara.sys.Property;
import niagara.sys.Sys;
import niagara.sys.Type;
import niagara.util.BConverter;

public class BStatusEnumToImage extends BConverter {

    public static final Property normalImage = newProperty(0, BImage.NULL, null);
    public BImage getNormalImage() { return (BImage) get(normalImage); }
    public void setNormalImage(BImage v) { set(normalImage, v, null); }

    public static final Property emptyImage = newProperty(0, BImage.NULL, null);
    public BImage getEmptyImage() { return (BImage) get(emptyImage); }
    public void setEmptyImage(BImage v) { set(emptyImage, v, null); }

    public static final Property excludedImage = newProperty(0, BImage.NULL, null);
    public BImage getExcludedImage() { return (BImage) get(excludedImage); }
    public void setExcludedImage(BImage v) { set(excludedImage, v, null); }

    public static final Property testImage = newProperty(0, BImage.NULL, null);
    public BImage getTestImage() { return (BImage) get(testImage); }
    public void setTestImage(BImage v) { set(testImage, v, null); }

    public static final Property activeImage = newProperty(0, BImage.NULL, null);
    public BImage getActiveImage() { return (BImage) get(activeImage); }
    public void setActiveImage(BImage v) { set(activeImage, v, null); }

    public static final Property faultImage = newProperty(0, BImage.NULL, null);
    public BImage getFaultImage() { return (BImage) get(faultImage); }
    public void setFaultImage(BImage v) { set(faultImage, v, null); }

    public static final Property preAlarmImage = newProperty(0, BImage.NULL, null);
    public BImage getPreAlarmImage() { return (BImage) get(preAlarmImage); }
    public void setPreAlarmImage(BImage v) { set(preAlarmImage, v, null); }

    public static final Property defaultImage = newProperty(0, BImage.NULL, null);
    public BImage getDefaultImage() { return (BImage) get(defaultImage); }
    public void setDefaultImage(BImage v) { set(defaultImage, v, null); }

    @Override
    public BObject convert(BObject from, BObject to, Context cx) {
        BImage fallback = imageOr(getDefaultImage(), to);
        BObject value = getOutOrSelf(from);

        if (value instanceof BIStatus) {
            BStatus status = ((BIStatus) value).getStatus();
            if (status == null || status.isNull() || status.isDown() || status.isFault() || status.isStale()) {
                return imageOr(getFaultImage(), fallback);
            }
        }

        if (value instanceof BIEnum) {
            BEnum enumValue = ((BIEnum) value).getEnum();
            if (enumValue != null) {
                switch (enumValue.getOrdinal()) {
                    case 0: return imageOr(getNormalImage(), fallback);
                    case 1: return imageOr(getEmptyImage(), fallback);
                    case 2: return imageOr(getExcludedImage(), fallback);
                    case 3: return imageOr(getTestImage(), fallback);
                    case 4: return imageOr(getActiveImage(), fallback);
                    case 5: return imageOr(getFaultImage(), fallback);
                    case 6: return imageOr(getPreAlarmImage(), fallback);
                    default: return fallback;
                }
            }
        }

        return fallback;
    }

    private static BObject getOutOrSelf(BObject value) {
        if (value instanceof BComponent) {
            BValue out = ((BComponent) value).get("out");
            if (out instanceof BObject) return (BObject) out;
        }
        return value;
    }

    private static BImage imageOr(BImage image, BObject fallback) {
        if (image != null && !image.isNull()) return image;
        if (fallback instanceof BImage) return (BImage) fallback;
        return BImage.NULL;
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BStatusEnumToImage.class);
}
