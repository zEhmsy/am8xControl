package com.sitecVendor.am8xControl.tags;

import com.sitecVendor.am8xControl.semantics.Am8xIdentity;
import com.sitecVendor.am8xControl.semantics.Am8xIdentitySource;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.Id;
import javax.baja.tag.Tag;
import javax.baja.tagdictionary.BSmartTagDictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Tag dictionary am8x: tag impliciti (calcolati), mai scritti nel config.bog.
 *
 * PERCHÉ IMPLIED E NON STORED: i tag calcolati costano zero byte nel config.bog,
 * non vanno mai fuori sync con l'albero dopo un re-import, e — la ragione
 * decisiva — funzionano IMMEDIATAMENTE sugli alberi già importati da versioni
 * PRECEDENTI di questo modulo, già in campo su impianti dei clienti, senza
 * bisogno di re-importare nulla. Se questi tag venissero salvati come proprietà
 * ogni installazione esistente resterebbe indietro finché non re-importata.
 * Non "ottimizzare" questa scelta trasformandola in caching su proprietà.
 *
 * Estende BSmartTagDictionary (javax.baja.tagdictionary), non l'interfaccia
 * javax.baja.tag.SmartTagDictionary direttamente: l'interfaccia richiede anche
 * i metodi di relazione (getImpliedRelation/addAllImpliedRelations/
 * addImpliedRelations) che qui non servono; BSmartTagDictionary li implementa
 * già in modo innocuo (basati su una tagRules list che lasciamo vuota) e noi
 * sovrascriviamo solo i due metodi che riguardano i tag.
 *
 * NON auto-installata in /Services/TagDictionaryService: il modulo non deve mai
 * scrivere se stesso nei servizi di una station di allarme antincendio già
 * commissionata. L'operatore la trascina dalla palette.
 */
@NiagaraType
public class BAm8xTagDictionary extends BSmartTagDictionary {

    public static final String NS = "am8x";

    public static final Id PANEL_ID       = Id.newId(NS, "panel");
    public static final Id LOOP_ID        = Id.newId(NS, "loop");
    public static final Id POSITION_ID    = Id.newId(NS, "position");
    public static final Id CHANNEL_ID     = Id.newId(NS, "channel");
    public static final Id ZONE_ID        = Id.newId(NS, "zone");
    public static final Id DEVICE_TYPE_ID = Id.newId(NS, "deviceType");
    public static final Id KIND_ID        = Id.newId(NS, "kind");

    public BAm8xTagDictionary() {
        setNamespace(NS);
    }

    @Override
    public void addAllImpliedTags(Entity entity, Collection<Tag> tags) {
        tags.addAll(implied(entity));
    }

    @Override
    public Optional<Tag> getImpliedTag(Id id, Entity entity) {
        for (Tag t : implied(entity)) {
            if (t.getId().equals(id)) return Optional.of(t);
        }
        return Optional.empty();
    }

    /**
     * Non lancia mai: un'eccezione qui, chiamata per OGNI entity di OGNI query di
     * tag della station, romperebbe le query di tag su tutta la station.
     * Am8xIdentitySource.fromComponent applica già sia il bail-out rapido
     * (instanceof) sia il try/catch — qui ci limitiamo a consumarne il risultato.
     */
    private List<Tag> implied(Entity entity) {
        List<Tag> out = new ArrayList<>();
        if (!(entity instanceof BComponent)) return out;

        Optional<Am8xIdentity> maybe = Am8xIdentitySource.fromComponent((BComponent) entity);
        if (!maybe.isPresent()) return out;
        Am8xIdentity id = maybe.get();

        out.add(new Tag(PANEL_ID, BString.make(id.getPanel())));
        out.add(new Tag(LOOP_ID, BInteger.make(id.getLoop())));
        out.add(new Tag(POSITION_ID, BInteger.make(id.getPosition())));
        out.add(new Tag(KIND_ID, BString.make(id.kindTag())));
        if (id.getChannel() >= 0) {
            out.add(new Tag(CHANNEL_ID, BInteger.make(id.getChannel())));
        }
        if (!id.getZoneAddress().isEmpty()) {
            out.add(new Tag(ZONE_ID, BString.make(id.getZoneAddress())));
        }
        if (!id.getDeviceType().isEmpty()) {
            out.add(new Tag(DEVICE_TYPE_ID, BString.make(id.getDeviceType())));
        }
        return out;
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xTagDictionary.class);
}
