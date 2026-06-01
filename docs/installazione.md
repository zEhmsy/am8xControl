# Guida all'Installazione (Release Pre-compilata)

Se stai utilizzando una release ufficiale di `am8xControl` (con i moduli `.jar` già pre-compilati e firmati con un certificato self-signed), segui attentamente questa guida per installare e autorizzare correttamente il modulo nel tuo ambiente Niagara Workbench.

---

## 1. Importazione del Certificato nel User Trust Store

Poiché i moduli sono firmati con un certificato *self-signed* (autofirmato), Niagara bloccherà l'esecuzione dei moduli a meno che tu non importi il certificato di firma all'interno del tuo **User Trust Store**.

1. Avvia il tuo **Niagara Workbench**.
2. Apri la connessione alla **Platform** (es. _localhost_ Platform).
3. Vai in **Certificate Management** (Gestione Certificati).
4. Seleziona la scheda **User Trust Store**.
5. Clicca sul pulsante **Import** in basso.
6. Seleziona il file del certificato (solitamente allegato alla release, es. `code.pem` o `cert.pem`).
7. Conferma l'importazione. Ora Niagara si fiderà dei moduli firmati con questo certificato.

---

## 2. Installazione dei Moduli (.jar)

Una volta importato il certificato, puoi procedere all'installazione dei moduli fisici:

1. Estrai il file `.zip` della release. Troverai due file JAR:
   - `am8xControl-rt.jar`
   - `am8xControl-wb.jar`
2. Chiudi il Niagara Workbench (e assicurati che il demone Niagara sia fermo o riavvialo dopo il prossimo passaggio).
3. Copia entrambi i file `.jar` all'interno della cartella `modules` della tua installazione Niagara.
   - *Percorso tipico su Windows:* `C:\Niagara\Niagara-4.15.x.x\modules\`
4. (Opzionale, per installazione su JACE/Edge) Se devi usare il modulo su un dispositivo target:
   - Usa il **Software Manager** dalla Platform del dispositivo remoto.
   - Seleziona i moduli `am8xControl-rt` (ed eventualmente `-wb` se serve interfaccia locale, anche se solitamente basta `-rt` sul controller) e procedi con il Commit.

---

## 3. Riavvio e Verifica

1. Avvia nuovamente il **Niagara Workbench** (ed eventuali Station interessate).
2. Apri la **Palette Sidebar** (`Ctrl+O` o `Window > Sidebars > Palette`).
3. Cerca la palette `am8xControl`.
   - Se la palette si apre correttamente e vedi i componenti all'interno, l'installazione e la validazione della firma sono andate a buon fine.
   - Se ricevi un errore di firma o "Untrusted certificate", torna al Punto 1 e assicurati che il certificato sia correttamente presente nel *User Trust Store*.

---

## 4. Guida all'Utilizzo (Workflow)

Una volta installato il modulo, ecco come importare la topologia della centrale antincendio all'interno della tua station:

1. **Aggiunta del Servizio:**
   - Apri la tua **Station**.
   - Dalla palette `am8xControl`, trascina il servizio `Am8xImportService` sotto la cartella `/Services` (o un'altra posizione a tua scelta) della tua Station.

2. **Avvio del Manager:**
   - Fai doppio clic sul servizio `Am8xImportService` appena inserito per aprire la relativa vista **Manager** (si aprirà automaticamente in modalità *Learn*).

3. **Discovery e Importazione XML:**
   - Clicca sul pulsante **Discover** in basso nella toolbar.
   - Apparirà un popup di configurazione. Clicca su **PC...** per caricare il file XML generato dal tool di configurazione della centrale AM-8200N.
   - Nello stesso popup, imposta l'**IP Modbus** della centrale, la **Porta** (di default 502) e il **Device Address Start** (indirizzo slave Modbus di partenza, default 101).
   - Clicca **OK**. Il file verrà caricato sulla station e il sistema leggerà automaticamente tutta la topologia (centrali, loop, sensori e sottomoduli M720).

4. **Revisione dei Dispositivi:**
   - Nel pannello superiore (Learn pane) apparirà un albero con le centrali trovate e, sotto ognuna, i relativi dispositivi ordinati per Loop e Posizione.
   - Puoi **selezionare/deselezionare** singoli dispositivi tramite l'apposita casella per decidere cosa importare nel database Niagara.
   - Cliccando su **Edit Device** (oppure facendo doppio clic sulla cella della tabella), puoi rinominare l'etichetta del dispositivo, assegnarlo a una zona diversa o modificare manualmente gli indirizzi dei registri di Stato e Analogico pre-calcolati.

5. **Commit nel Database Modbus:**
   - Una volta verificata la lista, clicca su **Commit** nella toolbar.
   - Il modulo eseguirà automaticamente le seguenti operazioni:
     - Creerà (o userà) una rete `ModbusTcpNetwork` sotto `/Drivers`.
     - Creerà un device speciale chiamato **CENTRALE** contenente i comandi generali (Silenzia, Reset, ecc.) e gli allarmi di tutte le zone.
     - Creerà un **ModbusTcpDevice** specifico per ogni centrale fisica importata.
     - Genererà l'albero completo per ogni Loop, inserendo i **Point Modbus** con gli indirizzi calcolati automaticamente per lo stato (Enum) e la misurazione (Numeric), collegandoli logicamente.

A questo punto la rete Modbus sarà configurata in modo esatto e basterà avviarla per avviare il polling dalla rete antincendio!
