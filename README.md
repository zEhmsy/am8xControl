<div align="center">
  <img src="logo.png" alt="am8xControl Logo" width="200"/>

  # am8xControl - Niagara Framework Module

  ![Java](https://img.shields.io/badge/Java-8%2B-ED8B00?logo=openjdk&logoColor=white)
  ![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?logo=gradle&logoColor=white)
  ![Niagara](https://img.shields.io/badge/Niagara-Framework-blue)
  ![License](https://img.shields.io/badge/License-Apache%202.0-blue)
</div>

**am8xControl** è un modulo custom per Niagara Framework progettato per importare la topologia e i punti di una centrale antincendio **Notifier Serie 8000** a partire dal suo file di configurazione XML.

## 📌 Funzionalità

Questo modulo non stabilisce una connessione attiva tramite protocolli come Modbus o BACnet. Funge invece da **base solida e scheletro strutturale** per la creazione di un driver completo o come utility di importazione offline. Le sue funzionalità principali includono:

- **Parsing XML:** Legge il file XML esportato dal tool di configurazione della centrale Notifier serie 8000.
- **Creazione Alberatura:** Costruisce automaticamente la struttura dei dispositivi all'interno della station Niagara.
- **Creazione Punti:** Genera dinamicamente i punti (`BAm8xDevice`) raggruppati in specifiche cartelle (`BAm8xDeviceFolder`) basate sull'etichetta della centrale (Panel Label).
- **Offline Importer:** Permette di predisporre la grafica, i link, le logiche di allarme e le estensioni Niagara in modalità offline, prima ancora di avere il collegamento fisico o il driver di comunicazione funzionante sul campo, risparmiando preziose ore di messa in servizio.

Per avviare l'importazione, è sufficiente configurare la proprietà `xmlFilePath` sul componente `Am8xNetwork` e richiamare l'azione `discover`. Se il percorso è lasciato vuoto, il modulo caricherà un file di test XML incluso di default come risorsa.

## 🚀 Per gli Sviluppatori (Community Niagara)

Questo progetto nasce con lo scopo di essere condiviso con la community di sviluppatori Niagara. Rappresenta un ottimo punto di partenza per chiunque desideri implementare un driver completo per le centrali Notifier o voglia imparare come strutturare l'importazione massiva di punti e dispositivi da file esterni.

Siete tutti invitati a contribuire per estendere le funzionalità, aggiungere il livello di comunicazione o migliorare il parser!

### 🛠 Come Contribuire (Branch e Pull Request)

Il flusso di lavoro per contribuire al progetto su GitHub è il seguente:

1. **Effettua il Fork del repository:** Clicca sul pulsante "Fork" in alto a destra nella pagina GitHub per creare una copia del progetto sul tuo account.
2. **Clona il tuo Fork in locale:**
   ```bash
   git clone https://github.com/TUO_UTENTE/am8xControl.git
   cd am8xControl
   ```
3. **Crea un nuovo Branch per la tua modifica:**
   Usa un nome descrittivo per il branch (es. `feature/modbus-integration`, `bugfix/xml-parsing`).
   ```bash
   git checkout -b feature/nome-della-tua-feature
   ```
4. **Apporta le tue modifiche e compila il progetto:**
   Assicurati che il codice venga compilato correttamente tramite Gradle.
   ```bash
   ./gradlew clean jar --parallel
   ```
5. **Fai il Commit e il Push:**
   Aggiungi i file modificati, scrivi un messaggio di commit chiaro e invia le modifiche al tuo repository GitHub.
   ```bash
   git add .
   git commit -m "Aggiunta la funzionalità X per il driver Notifier"
   git push origin feature/nome-della-tua-feature
   ```
6. **Apri una Pull Request (PR):**
   Vai sulla pagina GitHub del tuo fork. GitHub ti mostrerà un banner suggerendo di aprire una "Pull Request" verso il repository originale. Cliccalo, compila il form descrivendo accuratamente il problema risolto o la funzionalità aggiunta, e invia la richiesta.
   Un maintainer revisionerà il codice e lo integrerà nel branch principale!

## 📦 Struttura del Modulo
- `am8xControl-rt`: Modulo runtime che contiene la logica di parsing XML (`Am8xXmlParser`), i componenti Niagara (`BAm8xNetwork`, `BAm8xDeviceFolder`, `BAm8xDevice`) e la logica di base del driver.
- `am8xControl-wb`: Modulo workbench contenente l'interfaccia utente e i componenti specifici per il software N4 Workbench.
