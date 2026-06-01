# am8xControl — Roadmap & Stato del progetto

Documento di pianificazione del modulo `am8xControl` per Niagara 4.15.
Aggiornato al 2026-05-19.

---

## Obiettivo del modulo

Importare la topologia di una centrale antincendio Notifier **AM-8x00** dal
file XML esportato dal tool di configurazione, esporla in Niagara come
device tree navigabile, e collegare automaticamente ogni sensore/modulo ai
relativi registri **Modbus TCP** della centrale per il polling live.

L'idea di fondo è risparmiare ore in commissioning: il file XML è
disponibile **prima** del collegamento fisico alla centrale, quindi tutta la
struttura (loop, posizioni, zone, sub-moduli M720) viene predisposta offline.
Quando la centrale è collegata, il polling parte automaticamente sui registri
calcolati dalle formule deterministiche.

---

## Stato attuale

### ✅ Completato

| Fase | Descrizione | File chiave |
|------|-------------|-------------|
| **1** | Parsing XML topologia AM-8200N, descrittori device e sub-moduli M720 | `Am8xXmlParser`, `Am8xDeviceDescriptor`, `Am8xSubModuleDescriptor` |
| **2** | NDriver Discovery View nativa: colonne `Panel`/`Loop`/`Pos`/`Type`/`Zone`, M720 come nodo espandibile, naming sanitizzato | `BAm8xDiscoveryEntry @NiagaraType`, `BAm8xDeviceManager`, `Am8xDeviceLearn` |
| **2.5** | Pulsante **Match** funzionante sui device già esistenti; matching ricorsivo dentro `BNDeviceFolder` | `Am8xDeviceLearn.isMatchable()`, `matchesInTree()` |
| **3** | Auto-creazione di `alarm` / `fault` / `statusLabel` come point figli di ogni `BAm8xDevice` con indirizzi Modbus calcolati da loop + posizione + sub-modulo | `BAm8xDevice.ensurePoints()`, `Am8xModbusAddressing` |

### 🟡 In corso — Phase 3.5 (Wizard popup + auto-rete Modbus)

| Sotto-fase | Stato |
|------------|-------|
| Popup wizard a 4 campi (`xmlFilePath`, `ipAddress`, `tcpPort`, `modbusDeviceName`) attivato sopra `Discover` | ✅ |
| `BModbusTcpNetwork` creata automaticamente come sibling sotto `/Drivers` | ✅ (visibile nel Nav tree) |
| `BModbusTcpDevice` mountato sotto la rete con IP/porta del wizard | ❌ **bloccato**: `slotPath=null` dopo `mtn.add()` |

**Blocco attuale** — durante `submitDiscoveryJob`, la `BModbusTcpNetwork`
recuperata o creata risulta non-mountata (`mtn.getSlotPath() == null`).
Aggiungere il `BModbusTcpDevice` a quella istanza non lo collega all'albero
persistente: la mutation rimane in memoria e il bog non la salva.

Status diagnostico osservato in `BAm8xNetwork.lastDiscoveryStatus`:
```
ADDED AM8200N_Panel under null (slotPath=null) | ord=null
```

Ipotesi sul blocco:
- `submitDiscoveryJob` gira in un contesto FOX asincrono che non permette
  mutazioni cross-network persistenti.
- `getParent()` su `BAm8xNetwork` durante quel contesto potrebbe restituire
  un wrapper che blocca il commit.
- L'engine thread di Niagara potrebbe richiedere `Sys.getRefTransaction()`
  per propagare l'add al bog.

### 🔮 Da fare

| Fase | Descrizione | Priorità |
|------|-------------|---------|
| **3.5 close** | Risolvere mount del `BModbusTcpDevice` — refactor: salvare i valori del wizard come property persistenti su `BAm8xNetwork` (`modbusIpAddress`, `modbusTcpPort`, `modbusDeviceName`) e creare il device in `started()` o in un'action esplicita `doCreateModbusDevice()` invece che dentro `submitDiscoveryJob` | 🔥 alta |
| **4 — Polling live** | Su Add dalla Discovery: creare i point Modbus **direttamente sotto il `BModbusTcpDevice`** invece che sotto `BAm8xDevice`, così entrano nel contesto di polling. Il `BAm8xDevice` può mantenere link/riferimenti ai point | alta |
| **5 — Lifecycle** | Sincronizzazione bidirezionale: rimuovere un `BAm8xDevice` rimuove anche i suoi point Modbus; rinominare l'IP aggiorna il device; gestione di rete Modbus mancante (graceful) | media |
| **6 — Test unitari** | JUnit per `Am8xXmlParser` (XML valido / con M720 / campi mancanti) e `Am8xModbusAddressing` (formule su valori noti). Tier 1: gira senza Niagara | media |
| **7 — Production hardening** | Validazione IP nel wizard, gestione conflitti slot name, riconnessione Modbus, correlazione alarm/fault sui template Niagara, palette icone | bassa |

---

## Architettura attuale

```
am8xControl/
├── am8xControl-rt/
│   └── src/com/sitecVendor/am8xControl/
│       ├── BAm8xNetwork.java            # BNNetwork + BINDiscoveryHost
│       │                                  modbusNetworkOrd, lastDiscoveryStatus, lastError
│       │                                  submitDiscoveryJob → ensureModbusDevice (BUG attuale)
│       ├── BAm8xDevice.java             # BNDevice — un sensore/modulo importato
│       │                                  ensurePoints() crea alarm/fault/statusLabel su started()
│       ├── BAm8xDeviceFolder.java       # Raggruppamento opzionale per centrale
│       ├── BAm8xDiscoveryEntry.java     # @NiagaraType — entry della Discovery View
│       │                                  encoding FIELD_SEP/RECORD_SEP per i sub-moduli M720
│       ├── BAm8xDiscoveryPreferences.java # Wizard popup (xmlFilePath, ip, port, deviceName)
│       ├── BAm8xLearnDevicesJob.java    # Job di discovery tipizzato
│       ├── Am8xDeviceDescriptor.java
│       ├── Am8xSubModuleDescriptor.java
│       ├── Am8xModbusAddressing.java    # Formule indirizzi: sensorAlarm/Fault, moduleAlarm/Fault
│       └── Am8xXmlParser.java
└── am8xControl-wb/
    └── src/com/sitecVendor/am8xControl/wb/
        ├── BAm8xDeviceManager.java
        ├── Am8xDeviceLearn.java         # Match button + recursive isExisting
        ├── Am8xDeviceController.java
        └── Am8xDeviceModel.java
```

### Formule indirizzi Modbus (deterministiche)

Da `Am8xModbusAddressing.java`, derivate dalla specifica AM-8x00:

```
sensorAlarm(loop, pos)               = loop*5000 + 2000 + pos
sensorFault(loop, pos)               = loop*5000 + 3000 + pos
moduleAlarm(loop, modulePos, ch)     = loop*5000 + modulePos*10 + ch
moduleFault(loop, modulePos, ch)     = loop*5000 + modulePos*10 + ch + 1000
```

Esempi (loop=1):
- Sensore L1 P1 → alarm=7001, fault=8001
- M720 a L1 P1, sub-modulo MON3 ch=1 → alarm=5011, fault=6011

---

## Strategie di debugging (lessons learned)

### `console.txt` non è real-time

I log JUL via `LOG.info()` finiscono nel `console.txt` della station **solo
quando la station viene fermata**. Per leggere log di test recenti:
```bash
sudo systemctl stop n4d.service
sudo grep "\[BAm8xNetwork\]" /home/niagara/Niagara4.15/TridiumEMEA/stations/Training/console.txt
sudo systemctl start n4d.service
```

### Pattern preferito: Status Property

Invece di affidarsi a `console.txt`, esponiamo lo stato del componente come
property visibile direttamente nel Property Sheet:

```java
public static final Property lastDiscoveryStatus =
        newProperty(Flags.SUMMARY | Flags.READONLY, "idle", null);
public static final Property lastError =
        newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
```

Questo pattern viene da `BStreamSource.streamState` / `lastError`.

### `config.bog` come ground truth post-mortem

Il `config.bog` è uno ZIP; dentro c'è `file.xml` con tutto lo stato
persistito. Quando una mutazione del tree sembra fallita ma non si capisce
perché, leggi il bog dopo lo stop della station:
```bash
cd /tmp && mkdir -p bog && cd bog
sudo cp /home/niagara/Niagara4.15/TridiumEMEA/stations/Training/config.bog .
unzip -q config.bog
grep -A 10 "Am8xNetwork\|ModbusTcpNetwork" file.xml
```

### Iterare `getPropertiesArray()`, non `getSlotsArray()`

`getSlotsArray()` include anche le `Action`, e iterarle con
`parent.get(slotName)` può causare `ClassCastException: NAction cannot be
cast to Property` quando il framework fa cast interno. Usa sempre
`getPropertiesArray()` quando cerchi componenti figli.

---

## Decisioni di design

- **Topologia gerarchica per centrale**: ogni `BAm8xDevice` ha
  `panelLabel`, `loopNumber`, `positionOnLoop` come metadati. Il matching
  via `isExisting()` confronta su loop + posizione + panel per gestire
  centrali multiple sulla stessa station.

- **Sub-moduli M720 come children separati**: i contenitori M720 appaiono
  come gruppo espandibile nella Discovery; i loro sub-moduli (MON3, IN3, …)
  vengono aggiunti come device individuali con `parentModulePos` settato.

- **Wizard via `BNDiscoveryPreferences`**: il popup è gestito dal framework
  NDriver automaticamente — basta aggiungere property `SUMMARY` alla classe
  prefs e overridare `getDoNotAskAgain() → false`.

- **Driver vs Service**: `BAm8xNetwork` è registrato come driver (sotto
  `/Drivers`), non come service. Ha senso per la struttura naturale di
  NDriver (Network → Device → Point).
