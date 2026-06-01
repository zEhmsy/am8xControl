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

## Roadmap e Prossime Fasi 🚀

| Task | Descrizione | Priorità | Stato |
|------|-------------|----------|-------|
| **Test Upload Base64** | Verificare sul campo l'upload del file XML direttamente dal Workbench PC verso il demone (tramite il path temporaneo `~niagara-user-home/am8x_uploads`). | Alta | Da validare |
| **Rilevamento Already Imported** | Sincronizzare il flag `alreadyImported` analizzando l'albero Modbus persistente all'apertura del Manager, in modo da evitare duplicati visivi in tabella. | Media | Da fare |
| **Clear Imported (Fase 5)** | Implementare l'azione `doClearImported()` nel Service per ripulire agevolmente o rimuovere in blocco l'albero dei dispositivi importati precedentemente dal database Niagara. | Bassa | Placeholder |
| **Hardening** | Gestire scenari dove più file XML vengono accodati o uniti. Migliorare il feedback UX durante l'upload XML in caso di file malformati. | Bassa | Da fare |
