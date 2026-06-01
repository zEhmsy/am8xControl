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

## 4. Primo Utilizzo

1. Apri la tua **Station**.
2. Trascina il servizio `Am8xImportService` dalla palette sotto la cartella `/Services` (o in una cartella a tua scelta) della tua Station.
3. Doppio clic sul servizio per aprire il **Manager** e avviare il processo di configurazione e discovery tramite file XML.
