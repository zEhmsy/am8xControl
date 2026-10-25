# am8xControl — installazione e configurazione

am8xControl importa la configurazione XML delle centrali Notifier AM-8xxx e genera l’albero dei punti Modbus in Niagara. Il modulo è distribuito con licenza Apache 2.0: non richiede un file `.license`, un Host-ID o una richiesta di attivazione SITEC. Restano necessari i moduli e le abilitazioni Modbus previsti dalla propria installazione Niagara.

## Download e sorgenti

Seleziona il ramo Niagara 4 o Niagara 5 nella card del catalogo e scarica il bundle corrispondente. Il pacchetto contiene i JAR firmati, il certificato pubblico SITEC, le istruzioni di installazione e il testo completo della licenza in `LICENSE.txt`.

- Sorgenti: [github.com/zEhmsy/am8xControl](https://github.com/zEhmsy/am8xControl)
- Guida del progetto: [Installazione e utilizzo](https://github.com/zEhmsy/am8xControl/wiki/Installazione-e-Utilizzo)

## Installazione

1. Controlla lo SHA-256 del bundle e scegli il ramo compatibile con la tua installazione.
2. Importa `sitec-codesign.pem` nel User Trust Store di Workbench e nella Platform della JACE/Supervisor. Il file `.cer` contiene lo stesso certificato in formato DER.
3. Su Niagara 4 installa entrambi i JAR `am8xControl-rt.jar` e `am8xControl-wb.jar` nei rispettivi ambienti runtime e Workbench. Su Niagara 5 installa il JAR unico `am8xControl.jar`.
4. Usa Platform > Software Manager per installare il modulo sul target. Riavvia gli ambienti interessati e verifica che la firma risulti attendibile.
5. Verifica la disponibilità delle dipendenze Niagara, fra cui Modbus Core/TCP, alarm, control, kitControl, tagdictionary e hierarchy.

## Primo import

1. Apri la palette `am8xControl` e trascina `Am8xImportService` in `/Services` della station.
2. Apri la vista del servizio con un doppio clic.
3. Avvia **Discover**, seleziona il file XML della centrale, scegli il tipo device `ModbusTcp` o `ModbusGateway` e imposta indirizzo IP e porta.
4. Rivedi i dispositivi trovati: verifica etichette, zone, selezione dei punti e configurazione della rete.
5. Avvia **Commit**. La station crea o aggiorna rete, device, cartelle e punti Modbus. Controlla il job e i suoi eventuali messaggi di errore.

Su Niagara 5 l’ORD `file:^nome.xml` indica già un file nella shared della station: non aggiungere una seconda cartella `shared/`. Per upload ed import utilizza la vista del servizio e i percorsi proposti dal wizard.

## Novità della versione 4.0

- Discover e Commit utilizzano job Niagara con avanzamento, annullamento ed esito finale.
- Gli errori di import sono visibili nello stato del servizio; un import successivo riuscito ripristina lo stato normale.
- `moduleVersion` permette di identificare versione, revisione sorgente e build installata.
- Il tag dictionary `am8x` espone centrale, loop, posizione, zona e tipo dispositivo come tag impliciti. La palette include hierarchy per centrale e per zona.
- La generazione dei nomi di slot gestisce caratteri non validi e collisioni. Gli slot canonici già importati restano stabili durante il re-import.
- `displayNameFormat` controlla il nome visualizzato e può essere applicato anche a un albero già importato.

## Aggiornamento di una station esistente

Esegui prima un backup della station e del relativo `config.bog`. Aggiorna tutti i JAR dello stesso ramo e controlla `moduleVersion` nel servizio. Il passaggio dal componente base `BComponent` a `BAbstractService` è progettato per mantenere il tipo serializzato `am8xControl:Am8xImportService` e gli slot esistenti. Verifica comunque import, re-import e comunicazione su una station di prova prima di intervenire sull’impianto.

## Diagnosi

Se la palette non si apre o il modulo non viene caricato, verifica firma, certificato, ramo Niagara e dipendenze. Se un job fallisce, leggi lo stato del servizio e il relativo log; controlla file XML, permessi, IP, porta, tipo di device e configurazione Modbus. Prima di modificare registri, associazioni o comandi verifica la documentazione della centrale e l’effetto sul proprio impianto.

La disponibilità del modulo non sostituisce il collaudo dell’integrazione con la centrale reale.
