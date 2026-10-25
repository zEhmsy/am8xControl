# am8xControl 4.0.0 per Niagara 5

Porting dei sorgenti Niagara 4 della revisione `4b3944a`, riuniti in un unico modulo Niagara 5. Il progetto principale N4 resta nelle cartelle `am8xControl-rt` e `am8xControl-wb` alla root del repository.

## Contenuto

- `am8xControl/src`: runtime, job, semantica, tag dictionary e vista Workbench, con namespace N5.
- `am8xControl/module-include.xml`: registro dei componenti e dell’agent Workbench.
- `am8xControl/module.palette`: servizio, dictionary e hierarchy.
- `KEYVAULT_DOCS.md`: guida installazione e utilizzo per il catalogo.
- `docs/panoramica.pdf`: brochure commerciale indipendente dal distributore.
- `PORTING-2026-10-25.md`: origine del porting, adattamenti e verifiche.

## Compilazione

Servono SDK Niagara 5.0, JDK e plugin Gradle della distribuzione Niagara e un proprio profilo di firma. Configurare gli home Niagara per il proprio ambiente: SDK e chiavi private non sono inclusi nel repository.

Nel workspace Docker di sviluppo il progetto viene compilato senza VM con `scripts/docker/nbuild.sh am8xControl/niagara5 build -Pam8xGitCommit=4b3944a`. Il comando usa l’immagine interna Niagara 5.0.0.12 e copia il JAR firmato in `dist/`; lo script e l’immagine contenente l’SDK non fanno parte di questo repository pubblico.

Il bundle Niagara 5 contiene il JAR unico `am8xControl.jar`, i certificati pubblici e `LICENSE.txt` Apache 2.0. Non serve un file `.license` di attivazione del modulo.

## Stato delle verifiche

Compilazione, firma, registro dei tipi e banner 4.0.0 verificati. La logica N4 di origine ha 40 test JUnit superati. Il porting conserva gli adattamenti N5 per gli ORD `file:^` e per i file `BIFile`; resta necessario il collaudo su una station Niagara 5 reale prima dell’impiego in impianto.
