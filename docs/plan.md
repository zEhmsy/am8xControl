# am8xControl — Roadmap & Stato del Progetto

Documento di pianificazione del modulo `am8xControl` per Niagara 4.15.  
*Aggiornato a Giugno 2026.*

---

## Obiettivo del modulo

Importare la topologia di una centrale antincendio Notifier/Hochiki **AM-8200N** dal file XML esportato dal tool di configurazione, permettere la revisione dei punti e generare automaticamente l'albero **Modbus TCP** corrispondente in Niagara.

Questo modulo accelera enormemente il commissioning, consentendo l'integrazione offline basata sul file di configurazione della centrale. Una volta sul campo, l'integrazione Modbus è già mappata in base ad algoritmi deterministici e pronta per il polling in tempo reale.

---

## Architettura Corrente (ex V2)

La vecchia architettura basata sul driver framework (`BAm8xNetwork` / `BNDevice`) è stata abbandonata e unificata nel nuovo approccio basato sul servizio (`BAm8xImportService`).

```text
am8xControl/
├── am8xControl-rt/
│   └── src/com/sitecVendor/am8xControl/
│       ├── service/     BAm8xImportService (entry point, logic)
│       ├── discovery/   BAm8xDiscoveryReport, BAm8xPanelFolder, BAm8xDiscoveryCandidate
│       ├── parser/      Am8xXmlParser, Am8xDeviceDescriptor
│       ├── modbus/      ModbusTreeBuilder, ModbusPointFactory (Generazione albero Modbus)
│       └── model/       CandidateKey
└── am8xControl-wb/
    └── src/com/sitecVendor/am8xControl/wb/
        ├── BAm8xImportManager, Am8xImportController
        └── Am8xImportModel (BComponentTable model for the Discovery)
```

### Formule Indirizzi Modbus (Deterministiche)

Da `Am8xModbusAddressing.java` (basate sulle specifiche AM-8200):
- `sensorState (Analog) = Loop * 5000 + 2000 + Position`
- `sensorAnalog (Analog) = Loop * 5000 + 3000 + Position`
- `moduleState (Analog) = Loop * 5000 + ModulePos * 10 + subChannel`
- `moduleAnalog (Analog) = Loop * 5000 + ModulePos * 10 + subChannel + 1000`

---

## Stato Attuale: Fasi Completate ✅

| Fase | Descrizione | File Chiave |
|------|-------------|-------------|
| **1. Parsing XML** | Parsing accurato del file XML AM-8200N, estrazione gerarchica dei Loop, sensori e sottomoduli (es. M720). | `Am8xXmlParser` |
| **2. Import Service** | Componente Service (`BAm8xImportService`) per avviare il caricamento RPC o dal filesystem locale della Station. | `BAm8xImportService` |
| **3. Manager View** | Tabella (Database Pane) per visualizzare i dispositivi estratti (`BAm8xDiscoveryCandidate`), permettendo di filtrare, selezionare e modificare le etichette, le zone o i registri pre-calcolati. | `Am8xImportModel` |
| **4. Modbus Tree Generator** | Logica idempotente che, al "Commit", costruisce dinamicamente la rete `/Drivers/ModbusTcpNetwork`, crea device fisici e genera le cartelle `Loop_X` con dentro i point (`EnumWritable` per stato, `NumericWritable` per misura). Proxy Extensions configurati automaticamente. | `ModbusTreeBuilder`, `ModbusPointFactory` |
| **5. Device Virtuale "CENTRALE"** | Auto-generazione del nodo virtuale per esporre comandi globali di centrale (Es. Reset, Evacuazione) e stati riassuntivi. | `ModbusTreeBuilder` |

---

## Fasi Aggiuntive Completate (Upgrade Post-Teardown, Agosto 2026) ✅

Ciclo di lavoro su branch `feat/am8x-upgrades-2026-08` (13 task), che ha portato il servizio di
import da prototipo a componente robusto per l'uso in campo su impianti già esistenti.

| Fase | Descrizione | File Chiave |
|------|-------------|-------------|
| **6. Job asincrono e UI reattiva** | Discover e Commit sono diventati `BJob` con `progress` e `cancel`, eseguiti fuori dal thread UI; la view del Manager non fa più polling manuale. | `BAm8xImportService`, `BAm8xImportManager` |
| **7. Fault visibile e banner di versione** | Introdotti `configFail`/`configOk` (che richiedono `BAbstractService` invece di `BComponent implements BIService`): un XML corrotto porta il servizio in fault visibile (rosso) nell'albero, con messaggio leggibile. Aggiunto un banner di versione (con `moduleVersion` e commit git) sia in `console.txt` sia come property del servizio. | `BAm8xImportService` |
| **8. Identity, tag dictionary e hierarchy** | Nuovo `Am8xIdentity`; tag dictionary `am8x` che emette tag impliciti (es. `am8x:loop`, `am8x:zone`) senza bisogno di re-importare gli impianti esistenti; due hierarchy Niagara (panel-first e zone-first) disponibili in palette. | `Am8xIdentity`, `Am8xTagDictionary`, hierarchy palette entries |
| **9. Slot name sicuri, anti-collisione e display name a template** | `SlotPath.escape` per generare slot name sempre validi; rilevamento delle collisioni scoped per centrale (panel), non più per nome nudo, per evitare falsi positivi su impianti multi-centrale; display name generabili da template con un'azione (`applyDisplayNames`) che li retro-applica sull'albero esistente senza toccare gli slot name. | `SlotPath`, `Am8xModbusAddressing`/`ModbusTreeBuilder` (collision scoping), display-name template + action |

**Test automatici**: aggiunto un source set JUnit dedicato (23 test) che copre parsing, indirizzamento
Modbus, escaping degli slot name e scoping delle collisioni. Gira nel container Docker con:

```bash
./scripts/docker/nbuild.sh am8xControl :am8xControl-rt:test
```

Non copre BFormat/rendering dei display name (richiede il runtime Niagara) né il caricamento di un
config.bog reale — per questi la verifica finale è demandata al collaudo su VM (vedi sezione dedicata
più sotto).

---

## Checklist di Collaudo VM (post-teardown, Agosto 2026)

Da eseguire su una **VM di collaudo, mai su un impianto in esercizio**, dopo aver copiato i jar
buildati con:

```bash
cd /Users/giuseppe/Documents/StremCamera_Niagara
./scripts/docker/nbuild.sh am8xControl clean build -Pam8xGitCommit=$(git rev-parse --short HEAD)
```

La property `-Pam8xGitCommit=...` è obbligatoria: dentro il container Docker `git` non è disponibile,
quindi senza passarla esplicitamente il banner di versione e `moduleVersion` riportano `unknown`
invece del commit reale.

- [ ] **a) PRIORITÀ MASSIMA — caricare un `config.bog` esistente.** `BAm8xImportService` è passato
  da `BComponent implements BIService` a `BAbstractService` (inevitabile: `configFail`/`configOk`
  esistono solo lì). L'analisi statica indica che è sicuro — gli slot ereditati `status`/`faultCause`
  sono TRANSIENT e non vengono persistiti, `enabled` ha default `true` — ma va confermato caricando
  un `config.bog` vero di un impianto esistente: il servizio deve avviarsi con tutte le sue property
  intatte. Un fallimento qui significa il servizio che sparisce dall'albero o va in fault su un sito
  già collaudato.
- [ ] **b) Discover su XML grande.** Il progress avanza; premendo Cancel il job termina in
  `canceled`, **non** in `success` — questa è la regressione specifica da sorvegliare.
- [ ] **c) Discover su XML corrotto.** Il servizio diventa **rosso nell'albero**, con un messaggio
  di fault leggibile, senza che si apra automaticamente il property sheet.
- [ ] **d) Commit annullato a metà, poi rilanciato.** L'albero si completa senza duplicati e il
  dialog di cancel dichiara quanti point erano già stati creati al momento dell'annullamento.
  Il commit **non fa rollback per scelta**: dopo un annullamento l'albero resta parziale finché non
  si rilancia il commit.
- [ ] **e) Banner di versione.** Verificare in `console.txt` e nella property `moduleVersion` che
  entrambi riportino una versione e un commit git reali (non `unknown`) — ricordarsi di aver
  buildato con `-Pam8xGitCommit=$(git rev-parse --short HEAD)` come sopra.
- [ ] **f) Tag impliciti su impianto già esistente.** Trascinare `Am8xTagDictionary` dalla palette
  in `Services/TagDictionaryService`, poi lanciare una query tipo `am8x:loop` **su un albero
  importato con una versione PRECEDENTE del modulo, senza re-importarlo**. È la prova che i tag
  impliciti funzionano anche sugli impianti esistenti.
- [ ] **g) Hierarchy in palette.** Trascinare entrambe le hierarchy in `Services/HierarchyService`
  e verificare che si popolino (non vuote, non in fault). Una hierarchy vuota è il modo di fallire
  noto quando lo scope è sbagliato.
- [ ] **h) Point senza indirizzo di zona.** Non emettono il tag `am8x:zone` e quindi mancano da
  ENTRAMBE le hierarchy. Verificare quanto sono comuni sui dati reali e decidere se serve un livello
  catch-all — deliberatamente lasciato indeciso in attesa di dati veri.
- [ ] **i) Visibilità della job bar.** `BAm8xImportManager.forceDiscoverOnlyLayout()` ricompone il
  pane via reflection su campi privati di `BAbstractManager`, quindi la barra di progresso potrebbe
  non comparire. Se manca, il fix noto è una riga: aggiungere `getLearn().getJobBar()` al pane.
- [ ] **j) Import multi-centrale.** Importare un impianto con almeno DUE centrali che hanno
  dispositivi alla stessa coppia loop/posizione, e verificare che gli slot restino canonici
  (`L01S002` sotto ciascuna centrale) e NON suffissati (`L01S0022`). Verifica lo scoping per-panel
  del rilevamento delle collisioni.
- [ ] **k) `applyDisplayNames` su un albero esistente.** Lanciare l'azione e verificare che i
  display name compaiano e che gli slot name siano verificabilmente invariati.
- [ ] **l) Carattere `%` in una label di zona.** Una label di zona contenente un carattere `%` deve
  rendersi letterale nel display name, senza essere interpretata come espressione BFormat. Non è
  stato possibile coprirlo con un unit test (BFormat richiede il runtime Niagara), quindi la station
  è l'unico posto dove si può verificare.

Registrare qui sotto, dopo l'esecuzione, la data del collaudo, la VM usata e l'esito di ciascun
punto.

---

## Roadmap e Prossime Fasi 🚀

| Task | Descrizione | Priorità | Stato |
|------|-------------|----------|-------|
| **Test Upload Base64** | Verificare sul campo l'upload del file XML direttamente dal Workbench PC verso il demone (tramite il path temporaneo `~niagara-user-home/am8x_uploads`). | Alta | Da validare |
| **Rilevamento Already Imported** | Sincronizzare il flag `alreadyImported` analizzando l'albero Modbus persistente all'apertura del Manager, in modo da evitare duplicati visivi in tabella. | Media | Da fare |
| **Clear Imported (Fase 5)** | Implementare l'azione `doClearImported()` nel Service per ripulire agevolmente o rimuovere in blocco l'albero dei dispositivi importati precedentemente dal database Niagara. | Bassa | Placeholder |
| **Hardening** | Gestire scenari dove più file XML vengono accodati o uniti. Migliorare il feedback UX durante l'upload XML in caso di file malformati. | Bassa | Da fare |
| **Collaudo VM post-teardown** | Eseguire la checklist di collaudo sopra su una VM di test, con particolare attenzione al caricamento di un `config.bog` esistente (punto a). | Alta | Da eseguire |
