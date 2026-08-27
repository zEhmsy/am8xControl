<h1 align="center">
  <img src="docs/am8xcontrol_logo.png" alt="am8xControl Logo" width="120" style="border-radius: 20px;">
  <br>
  am8xControl
  <br>
</h1>

<h4 align="center">Modulo Niagara 4.15 per l'importazione automatica di centrali antincendio Notifier AM-8200N.</h4>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-4.0.0-blue.svg?cacheSeconds=2592000" />
  <img alt="Niagara" src="https://img.shields.io/badge/Niagara-4.15-orange.svg" />
  <img alt="Java" src="https://img.shields.io/badge/Java-8-red.svg" />
  <img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-yellow.svg" />
  <a href="https://github.com/zEhmsy/am8xControl/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/zEhmsy/am8xControl.svg?style=social&label=Star" /></a>
  <a href="https://www.buymeacoffee.com/gturturro"><img alt="Buy Me A Coffee" src="https://img.shields.io/badge/Donate-Buy%20Me%20A%20Coffee-FFDD00.svg?logo=buymeacoffee&logoColor=black" /></a>
</p>

<p align="center">
  <a href="#-scopo">Scopo</a> •
  <a href="#-installazione">Installazione</a> •
  <a href="#-utilizzo">Utilizzo</a> •
  <a href="#-architettura-e-dettagli-tecnici">Dettagli Tecnici</a>
</p>

<p align="center">
  <img src="docs/am8xcontrol_banner.png" alt="am8xControl Banner" width="100%">
</p>

---

## 🎯 Scopo

`am8xControl` è un modulo progettato per ridurre drasticamente i tempi di commissioning su Niagara 4.
Importa la topologia di una o più centrali antincendio **AM-8200N** direttamente dal file XML generato dal tool di configurazione.

Consente all'operatore di rivedere e modificare *offline* i dispositivi scoperti tramite un pannello visivo, generando poi automaticamente l'intero albero **Modbus TCP** (con proxy extension e link pre-configurati) sotto `/Drivers/ModbusTcpNetwork` nella Station.

---

## ✨ Novità della 4.0

* **Discover e Commit sono job Niagara.** Barra di avanzamento con percentuale, annullamento che funziona davvero ed esito finale riportato dalla station. Prima la UI stimava a tempo quando l'operazione fosse finita, e con import lunghi mostrava una tabella vuota.
* **Gli errori si vedono nell'albero.** Un import fallito manda il servizio in *fault* con la causa leggibile accanto, e lo stato torna normale da solo al primo import riuscito. Prima l'errore viveva in una property da aprire a mano.
* **Banner di versione.** Il servizio espone `moduleVersion` con versione, commit e data di build: da una segnalazione dal campo si risale subito a cosa è installato.
* **Tag dictionary `am8x` e hierarchy pronte.** Ogni punto porta centrale, loop, posizione, zona e tipo dispositivo come **tag impliciti** — calcolati, non scritti nel `config.bog`, quindi senza occupare spazio né restare disallineati dopo una rinomina. Nella palette ci sono due hierarchy da trascinare in `HierarchyService` per la vista per centrale e per zona.
* **Nomi di slot più sicuri.** I nomi vengono ripuliti dai caratteri non validi e, se due dispositivi diversi reclamano lo stesso nome nella stessa centrale, il secondo viene numerato invece di sovrascrivere il primo in silenzio.
* **Display name a template.** Lo slot resta canonico e stabile (`L01S002`, necessario perché il re-import ritrovi il punto), ma nell'albero si legge anche l'etichetta del dispositivo. Configurabile con `displayNameFormat`, applicabile anche a un albero già importato.
* **40 test JUnit** sulla logica pura: indirizzamento Modbus, round-trip dei nomi di slot, formattazione dei display name, risoluzione dei path.

> **Aggiornamento da 3.x** — Il servizio è passato da `BComponent` a `BAbstractService`, cambio necessario per poter segnalare il fault. La migrazione è stata verificata caricando un `config.bog` scritto dalla 3.1.1: configurazione, albero Modbus e discovery report vengono conservati, e i nuovi slot compaiono con i valori di default. Anche il ritorno alla 3.1.1 funziona: gli slot che il codice vecchio non conosce vengono ignorati. Resta buona pratica **fare una copia del `config.bog` prima di aggiornare**.

---

## 🚀 Installazione

Se stai utilizzando una release ufficiale con i moduli già compilati e firmati, scarica il pacchetto `.zip` dall'ultima **[Release Ufficiale](https://github.com/zEhmsy/am8xControl/releases/latest)**. L'installazione richiede pochi passaggi:

1. Importa il certificato (es. `code.pem`) nel **User Trust Store** in Niagara Workbench.
2. Copia i file `.jar` all'interno della cartella `modules/` della tua installazione Niagara.
3. Riavvia la Station e il Workbench.

👉 **Leggi la [Guida all'Installazione Completa](https://github.com/zEhmsy/am8xControl/wiki/Installazione-e-Utilizzo)** per le istruzioni dettagliate passo-passo.

---

⭐ **Ti piace questo progetto?** Lascia una **stella** a questa repository per supportare lo sviluppo!

---

## 💻 Utilizzo

1. **Aggiungi il Servizio:** Dalla palette `am8xControl`, trascina `Am8xImportService` nella cartella `/Services` della tua Station.
2. **Apri il Manager:** Fai doppio clic sul servizio per aprire la vista di importazione.
3. **Discover:** Clicca sul pulsante **Discover** nella barra in basso, carica il file XML della centrale, scegli il **Tipo Device** (`ModbusTcp` o `ModbusGateway`) e imposta IP e Porta Modbus.
4. **Revisione:** Controlla l'albero dei dispositivi trovati. Puoi rinominarli, spostarli di zona o deselezionare quelli non necessari.
5. **Commit:** Clicca su **Commit**. Il modulo genererà automaticamente l'intera rete Modbus (TCP o Gateway, secondo la scelta) con tutti i point, il device della CENTRALE (per i comandi generali) e i folder organizzati per Loop.

> **Tipo Device — `ModbusTcp` vs `ModbusGateway`**
> Nel popup di Discover scegli quale topologia di rete generare:
> * **ModbusTcp** (default) → `BModbusTcpNetwork` + `BModbusTcpDevice`. Ogni device porta il proprio IP/porta: usalo quando ciascuna centrale è raggiungibile su un endpoint TCP dedicato.
> * **ModbusGateway** → `BModbusTcpGateway` + `BModbusTcpGatewayDevice`. IP/porta vivono sulla rete e sono condivisi da tutti i device figli (indirizzati per `deviceAddress`): usalo per più centrali dietro un unico gateway Modbus.
>
> I point creati sotto `points/` sono identici nei due casi. La creazione è **IP-aware**: reti su IP diversi coesistono; se invece esiste già una rete sullo stesso IP ma di tipo diverso, l'import si ferma con un *Topology Conflict* anziché sovrascrivere.

---

## 🛠 Architettura e Dettagli Tecnici

Per facilitare la lettura, i dettagli tecnici approfonditi per sviluppatori sono stati organizzati in sezioni espandibili.

<details>
<summary><b>📂 Struttura del Modulo</b></summary>

```text
am8xControl/
├── am8xControl-rt/          ← codice che gira nella station (runtime)
│   ├── src/.../
│   │   ├── service/           BAm8xImportService, BAm8xWizardInput
│   │   ├── discovery/         BAm8xDiscoveryReport, BAm8xPanelFolder, BAm8xDiscoveryCandidate
│   │   ├── job/               BAm8xDiscoverJob, BAm8xCommitJob, BAm8xDisplayNameJob
│   │   ├── parser/            Am8xXmlParser, Am8xDeviceDescriptor, Am8xSubModuleDescriptor
│   │   ├── modbus/            ModbusTreeBuilder, ModbusPointFactory, Am8xModbusAddressing, BAm8xStatePoint
│   │   ├── semantics/         Am8xIdentity, Am8xSlotNames, Am8xDisplayNameFormatter, Am8xFilePaths
│   │   ├── tags/              BAm8xTagDictionary
│   │   └── model/             CandidateKey
│   └── srcJUnit/.../          test della logica pura (nessun runtime Niagara)
└── am8xControl-wb/          ← codice che gira nel Workbench (UI)
    └── src/.../wb/
        ├── BAm8xImportManager
        ├── Am8xImportController
        ├── Am8xImportLearn
        └── Am8xImportModel
```

</details>

<details>
<summary><b>🔄 Flusso Utente Interno (Workflow RPC)</b></summary>

```text
[WB] Apri BAm8xImportService
        ↓ BAm8xImportManager si monta automaticamente in learn mode
[WB] Pulsante "Discover"
        ↓ Popup: File XML (PC… | Station…), Tipo Device (ModbusTcp|ModbusGateway), IP Modbus, Porta, Device Address Start
        ↓ service.discover() invocato via RPC → restituisce l'ORD di un BAm8xDiscoverJob
[Station] BAm8xDiscoverJob (progress, cancel cooperativo, esito finale)
        ↓ Legge XML → Am8xXmlParser → lista Am8xDeviceDescriptor
        ↓ Popola discovery/ con BAm8xPanelFolder → BAm8xDiscoveryCandidate
[WB] La UI si aggancia al job: barra di avanzamento e Cancel, nessun polling
[WB] Am8xImportLearn mostra albero: centrale (folder) → device (leaf)
        ↓ Utente seleziona device → "Edit Device" per modificare Label/Zone/Indirizzi
        ↓ Utente usa "Cancel" per deselezionare tutta una centrale
[WB] Pulsante "Commit"
        ↓ Conferma se ci sono già-importati
        ↓ service.commit() → BAm8xCommitJob (progress, cancel, esito)
[Station] runCommit()
        ↓ ensureNetwork IP-aware: riusa la rete sullo stesso IP, o ne crea una nuova
        ↓   ModbusTcp     → BModbusTcpNetwork + BModbusTcpDevice (IP/porta per device)
        ↓   ModbusGateway → BModbusTcpGateway + BModbusTcpGatewayDevice (IP/porta sulla rete)
        ↓   IP uguale ma tipo diverso → IllegalStateException (Topology Conflict)
        ↓ Crea device CENTRALE con punti di controllo generali (Buzzer, Alarm, Zone…)
        ↓ Per ogni candidate selected → crea BAm8xStatePoint + BNumericPoint + Link
        ↓   nomi di slot escapati, collisioni nella stessa centrale numerate
        ↓   display name applicato da displayNameFormat
        ↓ In caso di errore → il servizio va in fault con la causa leggibile
```

</details>

<details>
<summary><b>⚙️ Logica Modbus e Point Factory</b></summary>

### Indirizzamento Modbus (`Am8xModbusAddressing`)
Due registri per ogni device:
* **Sensore:** Stato = `loop×5000 + 2000 + pos` | Analogico = `stato + 1000`
* **Modulo M720:** Stato = `loop×5000 + modulePos×10 + channel` | Analogico = `stato + 1000`

### Creazione Albero Modbus (`ModbusTreeBuilder`)
Il costruttore trova (o crea, garantendo l'idempotenza) la gerarchia:
`Drivers/{ModbusTcpNetwork|ModbusTcpGateway}/{Centrale_Name}/points/L{loop}/{device}`.

`ensureNetwork(parent, slot, gateway, ip, port)` è **IP-aware**: identifica la rete che già serve l'endpoint `(ip, port)` — per un gateway tramite il suo `ipAddress`, per una rete TCP semplice tramite l'IP di uno qualsiasi dei suoi device. Se la rete esiste con il tipo corretto la riusa; se esiste con il tipo opposto NON fa cast né sovrascrive, ma lancia `IllegalStateException` (Topology Conflict) che `doAddSelected` riporta all'operatore come stato/errore di import.

### Custom Enum Point (`BAm8xStatePoint`)
Un point speciale che estende `BEnumPoint` con un enum personalizzato (Normale, Guasto, Allarme, ecc.) e possiede property meta-dati come `valoreCamera`, linkati dinamicamente al corrispettivo Point analogico, oltre che Action dedicate per Esclusione/Inclusione dei sensori, gestite creando point preset *on-the-fly*.
</details>

<details>
<summary><b>📦 Dipendenze e Limitazioni Note</b></summary>

### Dipendenze modulo
* **Niagara 4.15** — `baja`, `bajaui`, `workbench`, `workbench-mgr`
* **modbusTcp** — `BModbusTcpNetwork`, `BModbusTcpDevice`, `BModbusTcpGateway`, `BModbusTcpGatewayDevice`
* **modbusCore** — `BModbusClientDevice`, `BModbusClientNumericProxyExt`, `BModbusClientEnumBitsProxyExt`, `BModbusClientPresetRegisters`, `BFlexAddress`, `BModbusClientPointFolder`
* **alarm** — alarm class e alarm extension generate per centrale
* **tagdictionary** — `BTagDictionary`, `SmartTagDictionary` per i tag impliciti `am8x`
* **hierarchy** — le hierarchy per centrale e per zona spedite nella palette

### Limitazioni Note Attuali
1. **Upload non testato in produzione** — il meccanismo RPC Base64 per il caricamento da PC è stato implementato ma non ancora validato end-to-end sul campo.
2. **`clearImported` non implementato** — l'azione di rimozione automatica dal DB Niagara dei point importati è un placeholder per sviluppi futuri.
3. **`alreadyImported` è memorizzato, non derivato** — il flag viene scritto al commit, ma non è ri-sincronizzato con l'albero Modbus reale: se un punto viene cancellato a mano dalla Station, il candidato continua a dichiararsi importato finché non si rilancia un Discover.
4. **Il pannello Database del Manager non è usato** — la vista di importazione lavora sul solo pannello Discover. Il Database non mostra l'albero Modbus generato.
5. **Migrazione verificata su un impianto di prova** — il test di aggiornamento da 3.1.1 è stato fatto su una station con due centrali e 24 dispositivi, senza personalizzazioni manuali. Su impianti grandi o con modifiche fatte a mano all'albero, conviene comunque partire da una copia del `config.bog`.

</details>

---

<p align="center">
  Realizzato con ❤️ per <b>Niagara 4.15</b>.
</p>
