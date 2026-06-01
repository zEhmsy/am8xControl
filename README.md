# am8xControl — Modulo Niagara: Recap Completo

## Scopo

`am8xControl` è un modulo Niagara 4.15 che importa la topologia di una o più centrali antincendio AM-8200N (Hochiki) da file XML, permette all'operatore di rivedere e modificare i device scoperti, poi crea i corrispondenti punti Modbus TCP sotto `/Drivers/ModbusTcpNetwork` nella station.

---

## 🚀 Installazione (Release Pre-compilata)

Se stai scaricando una release ufficiale `.zip` con i moduli già compilati e firmati, fai riferimento alla **[Guida all'Installazione](docs/installazione.md)** per le istruzioni su come aggiungere il certificato self-signed al *User Trust Store* di Niagara e installare i moduli.

---

## Struttura del modulo

```
am8xControl/
├── am8xControl-rt/          ← codice che gira nella station (runtime)
│   └── src/.../
│       ├── service/           BAm8xImportService, BAm8xWizardInput
│       ├── discovery/         BAm8xDiscoveryReport, BAm8xPanelFolder, BAm8xDiscoveryCandidate
│       ├── parser/            Am8xXmlParser, Am8xDeviceDescriptor, Am8xSubModuleDescriptor
│       ├── modbus/            ModbusTreeBuilder, ModbusPointFactory, Am8xModbusAddressing, BAm8xStatePoint
│       └── model/             CandidateKey
└── am8xControl-wb/          ← codice che gira nel Workbench (UI)
    └── src/.../wb/
        ├── BAm8xImportManager
        ├── Am8xImportController
        ├── Am8xImportLearn
        └── Am8xImportModel
```

---

## Flusso utente (workflow)

```
[WB] Apri BAm8xImportService
        ↓ BAm8xImportManager si monta automaticamente in learn mode
[WB] Pulsante "Discover"
        ↓ Popup: File XML (PC… | Station…), IP Modbus, Porta, Device Address Start
        ↓ service.discover() invocato via RPC
[Station] BAm8xImportService.doDiscover()
        ↓ Legge XML → Am8xXmlParser → lista Am8xDeviceDescriptor
        ↓ Popola discovery/ con BAm8xPanelFolder → BAm8xDiscoveryCandidate
[WB] Am8xImportLearn mostra albero: centrale (folder) → device (leaf)
        ↓ Utente seleziona device → "Edit Device" per modificare Label/Zone/Indirizzi
        ↓ Utente usa "Cancel" per deselezionare tutta una centrale
[WB] Pulsante "Commit"
        ↓ Conferma se ci sono già-importati
        ↓ service.commit() → doAddSelected()
[Station] doAddSelected()
        ↓ Crea/aggiorna BModbusTcpNetwork + BModbusTcpDevice per ogni centrale
        ↓ Crea device CENTRALE con punti di controllo generali (Buzzer, Alarm, Zone…)
        ↓ Per ogni candidate selected → crea BAm8xStatePoint + BNumericPoint + Link
```

---

## Componenti Runtime

### `BAm8xImportService` (BComponent, BIService)

Cuore del modulo. Vive nella station sotto `/Services/Am8xImportService`.

**Proprietà configurazione (editabili):**

| Property | Default | Descrizione |
|---|---|---|
| `xmlFilePath` | BOrd.DEFAULT | ORD del file XML da importare (es. `file:^shared/test.xml`) |
| `modbusNetworkSlot` | `"ModbusTcpNetwork"` | Nome slot della rete Modbus sotto `/Drivers` |
| `modbusIpAddress` | `""` | IP Modbus TCP della centrale |
| `modbusTcpPort` | `502` | Porta TCP Modbus |
| `modbusDeviceSlot` | `"AM8200N_Panel"` | Nome slot del device Modbus (legacy, ora usato il panel label) |
| `deviceAddressStart` | `101` | Primo slave address Modbus assegnato alle centrali |

**Proprietà stato (readonly):**

| Property | Descrizione |
|---|---|
| `lastImportStatus` | Ultima operazione eseguita |
| `lastError` | Ultimo errore occorso |
| `parsedCount` | Numero candidate trovati nell'ultimo discover |
| `addedCount` | Totale cumulativo di point Modbus creati |

**Proprietà upload (HIDDEN | TRANSIENT, buffer temporaneo RPC):**

| Property | Descrizione |
|---|---|
| `pendingUploadName` | Nome file da uploadare (es. `"test.xml"`) |
| `pendingUploadB64` | Contenuto file in Base64 |

**Actions:**

| Action | Flags | Descrizione |
|---|---|---|
| `discover` | OPERATOR, ASYNC | Parsa XML e popola `discovery/` |
| `commit` | OPERATOR | Alias di `addSelected`: crea punti Modbus |
| `addSelected` | HIDDEN | Implementazione effettiva del commit |
| `clearAll` | OPERATOR | Svuota discovery/ e resetta contatori |
| `selectAll` | HIDDEN | Imposta `selected=true` su tutti i candidate |
| `selectNone` | HIDDEN | Imposta `selected=false` su tutti i candidate |
| `uploadXml` | OPERATOR, HIDDEN | Decodifica Base64 e scrive su `^shared/{nome}` via FS diretto |
| `setupAndDiscover` | HIDDEN | Input wizard: setta config + chiama discover |
| `clearImported` | HIDDEN | Placeholder (non implementato, fase 5) |

**Logica `doDiscover()`:**
- Chiama `loadDescriptors()` per ottenere i device dall'XML
- Preserva i panel folder esistenti (non li ricrea) — mantiene IP/porta/deviceAddress configurati manualmente
- Svuota solo i candidate (`clearAllCandidates()`) e li ricrea
- Assegna `deviceAddress` incrementale solo ai nuovi panel
- Imposta sempre IP e porta dal service su tutti i panel (anche quelli esistenti)

**Logica `doAddSelected()`:**
1. Trova `/Drivers` via `station:|slot:/Drivers`
2. `ensureNetwork(drivers, modbusNetworkSlot)` → trova o crea `BModbusTcpNetwork`
3. Crea device speciale `CENTRALE` con punti di controllo generali (Buzzer, Alarm, Fault, Zone, comandi)
4. Per ogni `BAm8xPanelFolder`: crea `BModbusTcpDevice` con IP/porta/slave dal panel folder
5. Per ogni `BAm8xDiscoveryCandidate` selected e non già importato:
   - Crea folder loop `L{nn}` sotto `device/points/`
   - Crea `BAm8xStatePoint` (stato enumerato) + `BNumericPoint` (valore analogico)
   - Crea link `Analog.out → StatePoint.valoreCamera`
   - Imposta `alreadyImported=true`

**Risoluzione file XML (`loadDescriptors()`):**

Il metodo prova in sequenza:
1. Varianti dell'ORD: originale, senza prefisso `local:|`/`station:|`, con `local:|`, con `file:!stations/Training/...`
2. Fallback filesystem diretto (solo per ORD con `^` o `~`):
   - `^` → `/home/niagara/Niagara4.15/TridiumEMEA/stations/Training/{rel}`
   - `~` → `/home/niagara/Niagara4.15/TridiumEMEA/{rel}` e poi anche nella station
3. Se tutto fallisce → usa risorsa embedded `/resources/test.xml`

**Upload file da WB (`doUploadXml()`):**
- Riceve nome+Base64 dai property transient
- Scrive direttamente su filesystem: `/home/niagara/Niagara4.15/TridiumEMEA/stations/Training/shared/{nome}`
- Auto-imposta `xmlFilePath` sul file appena caricato (`file:^shared/{nome}`)
- Svuota i buffer dopo l'operazione

> **Limitazione nota:** il path di destinazione è hardcoded sulla station `Training`. Non è portabile tra station diverse.

---

### `BAm8xDiscoveryReport` (BComponent)

Contenitore persistente, figlio di `BAm8xImportService` con slot name `"discovery"` (HIDDEN).

Struttura gerarchica in memoria:

```
discovery (BAm8xDiscoveryReport)
  ├── CENTRALE_1  (BAm8xPanelFolder)
  │     ├── L01S001  (BAm8xDiscoveryCandidate)
  │     ├── L01S002  (BAm8xDiscoveryCandidate)
  │     └── L01M003_1  (BAm8xDiscoveryCandidate)
  └── CENTRALE_2  (BAm8xPanelFolder)
        └── L01S001  (BAm8xDiscoveryCandidate)
```

**Proprietà:** `lastRunTimestamp`, `totalCandidates`, `selectedCount`

**Metodi chiave:**
- `ensurePanelFolder(slot)` — trova o crea un `BAm8xPanelFolder` figlio
- `clearAllCandidates()` — svuota i candidate preservando la config dei panel folder
- `removePanelFolders()` — rimozione totale (usata da `clearAll`)
- `getCandidates()` — flat list di tutti i `BAm8xDiscoveryCandidate`
- `refreshSelectedCount()` — ricalcola `selectedCount`

---

### `BAm8xPanelFolder` (BComponent)

Un folder per ciascuna centrale AM-8200N, raggruppato per `PanelLabel` dall'XML.

**Proprietà:**

| Property | Default | Descrizione |
|---|---|---|
| `ipAddress` | `""` | IP del Modbus TCP server per questa centrale |
| `port` | `502` | Porta TCP (copiata dal service ad ogni discover) |
| `deviceAddress` | `0` | Slave Modbus (unit ID), assegnato automaticamente solo alla creazione |

Il `deviceAddress` parte da `deviceAddressStart` (default 101) e si incrementa per ogni nuova centrale.

---

### `BAm8xDiscoveryCandidate` (BComponent)

Rappresenta un singolo device (sensore o sub-modulo M720) letto dall'XML.

**Proprietà:**

| Property | RO/RW | Descrizione |
|---|---|---|
| `panelLabel` | RO | Label della centrale di appartenenza |
| `loopNumber` | RO | Numero loop (1-based) |
| `positionOnLoop` | RO | Posizione sul loop (1-based) |
| `deviceType` | RW | Codice tipo device (es. `"S4"`, `"M720"`) |
| `deviceLabel` | RW | Label testuale del device |
| `zoneAddress` | RW | Indirizzo zona numerica |
| `zoneLabel` | RW | Label zona testuale |
| `stateAddress` | RW | Indirizzo Modbus registro stato (Qualificatore) |
| `analogAddress` | RW | Indirizzo Modbus registro analogico (Misura) |
| `candidateSlotName` | RO | Slot name univoco: `"L01S002"`, `"L01M003_1"` |
| `panelSlotName` | RO | Slot name della centrale padre |
| `selected` | RW | Se `true` verrà incluso nel commit (default `true`) |
| `alreadyImported` | RO | `true` dopo il commit |

**Action:** `toggleSelected` — inverte il flag `selected`

---

### `CandidateKey` (POJO)

Identificatore fisico stabile di un device, basato su `(panelLabel, loop, pos, parentModulePos, channel)`.

**Slot name convention:**

| Tipo | Formato | Esempio |
|---|---|---|
| Loop folder | `L{nn}` | `L01` |
| Sensore | `L{nn}S{pos:03d}` | `L01S002` |
| Modulo | `L{nn}M{pos:03d}` | `L01M003` |
| Sub-canale M720 | `L{nn}M{mPos:03d}_{ch}` | `L01M003_1` |

Il separatore `_` è usato invece di `.` perché il framework Niagara non garantisce slot name con punto in tutte le versioni.

---

### `Am8xXmlParser`

Parser DOM standalone per file XML esportati da software AM-8200N.

**Struttura XML attesa:**
```xml
<TopologyImport>
  <PanelData Type="AM8200N">
    <PanelLabel>CENTRALE 1</PanelLabel>
    <Device>
      <LoopNumber>1</LoopNumber>
      <PositionOnLoop>1</PositionOnLoop>
      <Type>S4</Type>
      <Label>Rivelatore corridoio</Label>
      <ZoneAddress>5</ZoneAddress>
      <ZoneLabel>Piano 1</ZoneLabel>
      <SubModule>
        <Module><Type>M720</Type><Label>...</Label><Number>1</Number>...</Module>
      </SubModule>
    </Device>
  </PanelData>
</TopologyImport>
```

Hardened contro XXE: doctype disabilitato, entità esterne disabilitate.

---

### Modbus — indirizzi (`Am8xModbusAddressing`)

Due registri per ogni device:

| Tipo | Formula Stato | Formula Analogico |
|---|---|---|
| Sensore | `loop×5000 + 2000 + pos` | `stato + 1000` |
| Modulo | `loop×5000 + modulePos×10 + channel` | `stato + 1000` |

Esempi (loop=1):
- Sensore L1 P1 → stato=7001, analogo=8001
- M720 L1 P1 ch1 → stato=5011, analogo=6011

---

### `BAm8xStatePoint` (BEnumPoint)

Point custom per lo stato AM-8x00. Estende `BEnumPoint` con:
- **Enum 7 stati:** Normale, Non Programmato, Escluso, Test, Allarme, Guasto, PreAllarme
- **Proxy:** `BModbusClientEnumBitsProxyExt` (registro intero letto come ordinale enum)
- **Slot metadata:** `deviceType`, `deviceLabel`, `zoneAddress`, `zoneLabel`
- **Slot `valoreCamera`:** riceve il link dall'analogo (`BNumericPoint.out`)
- **Actions:**
  - `esclusione` → scrive `1` sul registro Modbus via `BModbusClientPresetRegisters` interno
  - `inclusione` → scrive `2` sul registro Modbus

Il `BModbusClientPresetRegisters` viene creato on-the-fly alla prima invocazione di un'action, con lo stesso indirizzo del proxy ext del punto.

---

### `ModbusTreeBuilder`

Classe utilitaria per navigare e creare la struttura ad albero Modbus sotto `/Drivers`, con logica idempotente (find-or-create):

- `ensureNetwork(parent, slot)` — trova o crea `BModbusTcpNetwork`
- `ensureDevice(network, slot, ip, port, devAddr)` — trova o crea `BModbusTcpDevice`, aggiorna sempre IP/porta/devAddr
- `getPointsContainer(device)` — ritorna il child `points` del device (dove devono vivere i point Modbus)
- `ensureFolder(parent, slot)` — crea `BModbusClientPointFolder` per loop/device folder

> I point **devono** essere sotto `device/points/` (non direttamente sul device) per essere inclusi nel ciclo di polling Modbus.

---

### `ModbusPointFactory`

Crea i singoli point Modbus:

- `createStatePoint(parent, slot, addr, ...)` → `BAm8xStatePoint` con `BModbusClientEnumBitsProxyExt`
- `createNumericPoint(parent, slot, addr)` → `BNumericPoint` con `BModbusClientNumericProxyExt`
- `createBooleanPoint(parent, slot, addr)` → `BBooleanPoint` con `BModbusClientBooleanProxyExt`
- `createNumericWritable(parent, slot, addr)` → `BNumericWritable` con `BModbusClientNumericProxyExt`
- `createLink(srcPoint, tgtPoint, tgtSlot)` → `BLink` da `Analog.out` a `StatePoint.valoreCamera`
- `populateCentralePoints(container, zones)` → crea i punti generali del device CENTRALE

**Device CENTRALE** (creato automaticamente a ogni commit):
- Coil read: `Buzzer` (321), `Alarm` (322), `Fault` (324), `Exclusion` (326), `Horn_Silenced` (327)
- Holding write: `Buzzer_Off` (80), `Reset` (81), `Toggle_Mute_Horn` (82)
- Per ogni zona: `Z{nnn}_{label}` a indirizzo `100 + zoneAddr`

---

## Componenti Workbench (UI)

### `BAm8xImportManager` (BAbstractManager)

Agente Niagara WB montato automaticamente quando si apre un `BAm8xImportService`.

- Instanzia `Am8xImportModel` (DB pane), `Am8xImportLearn` (Learn pane), `Am8xImportController`
- `doLoadValue()`: all'apertura forza la modalità Learn e chiama `updateDiscoveryData()` per popolare l'albero

### `Am8xImportController` (MgrController)

Gestisce la toolbar del Manager. Comandi:

| Comando | Descrizione |
|---|---|
| **Discover** | Mostra popup di setup, poi chiama `service.discover()` con polling refresh ogni 400ms per ~4s |
| **Clear All** | Chiama `service.clearAll()`, aggiorna la vista |
| **Edit Device** | Popup modifica per il device selezionato nel Learn pane (Label, Zone, State Addr, Analog Addr); persiste sulla station via resolve del navOrd |
| **Cancel** | Deseleziona tutta la centrale a cui appartiene il device selezionato |
| **Commit** | Chiede conferma se ci sono già-importati, poi chiama `service.commit()` e mostra il risultato |

**Popup Discover** include:
- Campo ORD del file XML con due pulsanti:
  - **PC…** — `JFileChooser` locale: legge bytes, Base64-encode, scrive su `pendingUploadName`/`pendingUploadB64`, invoca `uploadXml()` sulla station, aggiorna il campo con `file:^shared/{nome}`
  - **Station…** — `BFileChooser` Niagara: naviga i file già presenti sulla station a partire da `station:|file:^shared/`
- Campi: IP Modbus, Porta Modbus, Device Address Start

### `Am8xImportLearn` (MgrLearn)

Albero gerarchico del pannello Learn:
- **Gruppi (righe espandibili):** `BAm8xPanelFolder` — mostra il nome e un riepilogo dei device selezionati (`✓ tutti (N)` / `M / N` / `—`)
- **Foglie:** `BAm8xDiscoveryCandidate` — mostra Slot, Added, Type, Label, Loop, Pos, Zone, State Addr, Analog Addr, Imported

Colonne editabili (inline): Type, Label, Zone, State Addr, Analog Addr.

`updateDiscoveryData()` — ripopola `updateRoots()` dai `BAm8xPanelFolder` presenti nel report.

### `Am8xImportModel` (MgrModel)

Pannello Database (tabella piatta). Configurato per scansionare `BAm8xDiscoveryCandidate` figli di `BAm8xDiscoveryReport` (depth=2). Presente nel codice ma attualmente non mostra dati a causa di limitazioni nell'integrazione `BComponentTable.load()` lato WB — la UI si basa principalmente sul Learn pane.

---

## Albero Modbus risultante

Dopo il commit, la struttura in `/Drivers` diventa:

```
Drivers/
└── ModbusTcpNetwork (BModbusTcpNetwork)
      ├── CENTRALE  (BModbusTcpDevice — IP centrale, port 502, devAddr 1)
      │     └── points/
      │           ├── Buzzer  (BBooleanPoint @ 321)
      │           ├── Alarm   (BBooleanPoint @ 322)
      │           ├── Z001_Piano_1  (BNumericWritable @ 101)
      │           └── ...
      ├── CEENTRALE_1  (BModbusTcpDevice — IP/port/devAddr dal panel folder)
      │     └── points/
      │           └── L01/  (BModbusClientPointFolder)
      │                 ├── L01S001  (BAm8xStatePoint @ 7001)
      │                 │     ├── L01S001_Analog  (BNumericPoint @ 8001)
      │                 │     └── Link: Analog.out → StatePoint.valoreCamera
      │                 └── L01M003_1  (BAm8xStatePoint @ 5031)
      │                       └── ...
      └── CEENTRALE_2  (BModbusTcpDevice)
            └── ...
```

---

## Dipendenze modulo

- **Niagara 4.15** — `baja`, `bajaui`, `workbench`, `workbench-mgr`
- **modbusTcp** — `BModbusTcpNetwork`, `BModbusTcpDevice`
- **modbusCore** — `BModbusClientNumericProxyExt`, `BModbusClientEnumBitsProxyExt`, `BModbusClientPresetRegisters`, `BFlexAddress`, `BModbusClientPointFolder`

---

## Limitazioni note

1. **Upload non testato in produzione** — il meccanismo RPC Base64 per il caricamento da PC è stato implementato ma non ancora validato end-to-end.
2. **`clearImported` non implementato** — l'azione di rimozione dei point già creati è un placeholder (fase 5).
3. **`alreadyImported` non sincronizzato** — il flag viene impostato dal servizio ma non viene riletto dal Modbus tree reale a ogni apertura del manager.
