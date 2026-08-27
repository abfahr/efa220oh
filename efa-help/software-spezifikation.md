# Software-Spezifikation: efa 2.2/2.3 OH-Fork

Stand: 2026-08-23  
Quelle: bestehender Java-Code im Repository `efa220oh`

## 1. Zweck und Kontext

`efa` ist ein elektronisches Fahrtenbuch fuer Ruder- und Kanubetrieb. Die Software verwaltet Vereinsprojekte mit Fahrtenbuechern, Personen, Booten, Zielen, Gewaessern, Reservierungen, Bootsschaeden, Statistiken, Benachrichtigungen und Vereinsarbeit.

Der vorliegende Fork enthaelt zusaetzliche Bootshaus-/OH-spezifische Funktionen, erkennbar unter anderem an Feldern wie `Bootshaus+`, `PopUp-Frage`, automatischen Reservierungsablaeufen, Link-/Todo-Dateien fuer Reservierungs- und Personenprofil-Aenderungen sowie spezifischen Erinnerungs- und Neustartmechanismen.

## 2. Systemueberblick

Das System ist eine Java-Desktop-Anwendung mit mehreren Startvarianten:

| Programm | Einstiegspunkt | Zweck |
| --- | --- | --- |
| efa-Bootshaus | `de.nmichael.efa.boathouse.Main` | Touch-/Bootshausbetrieb fuer Fahrtenstart, Fahrtende, Reservierungen, Statusanzeigen und eingeschraenkte Bedienung |
| efaBase | `de.nmichael.efa.base.Main` | Administrations- und Stammdatenoberflaeche |
| efaCLI | `de.nmichael.efa.cli.Main` | Kommandozeilenzugriff auf ein laufendes/remote Projekt |
| efaDRV | `de.nmichael.efa.drv.Main` | DRV-/Wettbewerbsbezogene Anwendung |
| efaEmil | `de.nmichael.efa.emil.Main` | Spezialprogramm fuer Import-/Meldeablaeufe |
| efaElwiz | `de.nmichael.efa.elwiz.Main` | Spezialprogramm fuer weitere externe/verbandliche Abarbeitung |

Das paketierte Haupt-JAR startet laut `efa-main/pom.xml` standardmaessig `de.nmichael.efa.boathouse.Main`.

## 3. Build und Laufzeit

Buildsystem:

- Maven-Multimodulprojekt mit Parent `efa-parent`.
- Module: `efa-dependencies`, `efa-help`, `efa-main`.
- Java-Version: 17 laut Parent-POM.
- Artefaktversion: `2.3.0-SNAPSHOT`.
- Release-ID aus POM: `${efa.version}_196`.

Wichtige Abhaengigkeiten:

- Jakarta Mail / Java Activation fuer E-Mail-Versand.
- Apache FOP und XMLGraphics fuer Druck-/PDF-nahe Ausgaben.
- Batik fuer SVG/XML-Grafikverarbeitung.
- ical4j fuer Kalender-/iCal-Funktionen.
- ZXing fuer QR-/Barcode-Funktionen.
- JavaHelp fuer integrierte Hilfe.
- Commons IO / Lang fuer Hilfsfunktionen.

Beispielstart aus README:

```sh
java -Xmx128m -XX:NewSize=32m -XX:MaxNewSize=32m -Duser.country=DE -Duser.language=de -jar efa-main-2.3.0-SNAPSHOT-jar-with-dependencies.jar
```

## 4. Architektur

### 4.1 Schichten

Die Anwendung ist grob in folgende Schichten gegliedert:

| Schicht | Pakete | Aufgabe |
| --- | --- | --- |
| Programmstart und globale Laufzeit | `de.nmichael.efa`, `base`, `boathouse`, `cli`, `drv`, `emil`, `elwiz` | Initialisierung, Argumente, Start der jeweiligen UI/CLI |
| GUI | `de.nmichael.efa.gui`, `gui.util`, `gui.dataedit`, `gui.statistics`, `gui.widgets` | Swing-Dialoge, Bootshausoberflaeche, Adminoberflaeche, Eingabekomponenten |
| Fachmodell | `de.nmichael.efa.data` | Projekt, Fahrtenbuch, Personen, Boote, Ziele, Reservierungen, Schaeden, Nachrichten |
| Persistenz | `de.nmichael.efa.data.storage` | Abstrakte Datenhaltung, XML-Dateien, Remote-Zugriff, Locks, Audit, Import/Export |
| Konfiguration | `de.nmichael.efa.core.config` | globale und projektspezifische Einstellungen, Admins, Typdefinitionen |
| Statistik/Wettbewerb | `de.nmichael.efa.statistics`, `data.efawett`, `gui.statistics` | Auswertungen, DRV-/Wettbewerbsmeldungen, CSV/HTML/PDF-nahe Ausgaben |
| Infrastruktur | `de.nmichael.efa.core`, `util`, `calendar` | Backup, Crontab, E-Mail, Internationalisierung, Logging, Updates |

### 4.2 Initialisierung

Alle Programme leiten von `Program` ab:

1. `Daten.iniBase(applId)` setzt globale Laufzeitparameter.
2. Kommandozeilenargumente werden in `Program.checkArgs()` und programmspezifischen Overrides verarbeitet.
3. `Daten.initialize()` initialisiert Konfiguration, Logging, I18n, Verzeichnisse, Locks, E-Mail-Thread und weitere globale Dienste.
4. Danach startet die konkrete UI oder CLI.

Unterstuetzte allgemeine Kommandozeilenoptionen:

- `-help`
- `-javaRestart`
- Entwickleroptionen: `-debug`, `-debugAll`, `-traceTopic`, `-traceLevel`, `-logToStdOut`, `-wws`, `-exc`, `-printargs`, `-emulateWin`, `-i18n...`

## 5. Persistenz und Datenzugriff

### 5.1 Storage-Abstraktion

Persistente Datenklassen erben von `StorageObject`. Jedes `StorageObject` kapselt ein `IDataAccess`:

- `TYPE_FILE_XML`: lokale XML-Dateien.
- `TYPE_EFA_REMOTE`: Remote-Zugriff ueber efa-Remote-Protokoll.
- `TYPE_DB_SQL`: im Code vorgesehen, aber nicht implementiert.

`DataRecord` ist die gemeinsame Basisklasse fuer Datensaetze. Felder, Typen, Schluessel und Indizes werden ueber `MetaData` registriert. Beim Oeffnen eines Storage-Objekts werden Datenfelder und Schluesselfelder aus der jeweiligen Record-Klasse aufgebaut.

### 5.2 Datentypen

Wichtige Datentypen:

- primitive/nahe primitive Typen: String, Integer, Long, Double, Boolean.
- fachliche Typen: Datum, Zeit, Distanz, Dezimalzahl, UUID, Passwort gehasht/verschluesselt.
- Listen: String-, Integer- und UUID-Listen.
- versionierte Datensaetze besitzen zusaetzlich Gueltigkeitsfelder `ValidFrom`, `InvalidFrom`, `Invisible`, `Deleted`.

### 5.3 Projektstruktur

Ein Projekt (`Project`) ist selbst ein Storage-Objekt und verwaltet eine Sammlung weiterer Storage-Objekte:

- `autoincrement`
- `sessiongroups`
- `persons`
- `clubwork`
- `status`
- `groups`
- `fahrtenabzeichen`
- `boats`
- `crews`
- `boatstatus`
- `boatreservations`
- `boatdamages`
- `destinations`
- `waters`
- `statistics`
- `messages`
- zusaetzlich beliebige Fahrtenbuecher (`Logbook`) und Vereinsarbeitsbuecher (`Clubwork`)

Lokale Projekte werden aus XML-Dateien geoeffnet. Remote-Projekte verwenden einen Remote-Client, der Datenoperationen an einen Remote-Server weiterleitet.

### 5.4 Remote-Protokoll

Der Remote-Zugriff basiert auf `RemoteEfaClient`, `RemoteEfaServer`, `RemoteEfaMessage` und `RemoteEfaParser`.

Unterstuetzte Operationen umfassen unter anderem:

- Login / Session-Verwaltung mit Benutzername, Passwort und Session-ID.
- Storage-Operationen: existiert, oeffnen, erstellen, Status pruefen.
- Datensatzoperationen: `Get`, `GetAllKeys`, `GetByFields`, `Add`, `Update`, `Delete`.
- Gueltigkeitsabfragen fuer versionierte Daten: `GetValidAny`, `GetValidAt`, `GetValidLatest`, `GetValidNearest`.
- Locks: globale und lokale Locks erwerben/freigeben.
- Statuswerte: SCN und Anzahl Datensaetze.
- Remote-Kommandos: efa beenden/neustarten, Online-Update.

## 6. Fachliches Datenmodell

Die folgende Tabelle fasst die persistenten Kernobjekte zusammen. Die Feldliste nennt die wichtigsten im Code definierten Felder, nicht jedes Hilfs-/GUI-Feld.

| Storage/Record | Zweck | Hauptfelder |
| --- | --- | --- |
| `Project` / `ProjectRecord` | Projekt-Metadaten, Club, Bootshaus, Fahrtenbuecher, Storage-Konfiguration, Verbandsdaten | `Type`, `ProjectName`, `ProjectID`, `Name`, `Description`, `StorageType`, `StorageLocation`, `RemoteProjectName`, `AdminName`, `AdminEmail`, Club-/Verbandsdaten, Kanu-eFB/DRV-Daten |
| `Logbook` / `LogbookRecord` | Fahrtenbuch-Eintraege | `EntryId`, `Date`, `EndDate`, `BoatId`, `BoatName`, Cox-/Crew-Felder, `BoatCaptain`, `Contact`, `StartTime`, `EndTime`, Ziel/Gewaesser, `Distance`, `Comments`, `SessionGroupId`, `Open` |
| `Persons` / `PersonRecord` | Personen-/Mitgliederstammdaten | `Id`, `FirstName`, `LastName`, `FirstLastName`, `StatusId`, `Email`, Erlaubnisse fuer E-Mail/Telefon/Kuerzel, `MembershipNo`, Statistik-Ausschluss, `InputShortcut` |
| `Boats` / `BoatRecord` | Bootsstammdaten | `Id`, `Name`, `NameAffix`, `Owner`, Varianten, Typ/Bauart/Plaetze/Riggerung/Steuerung, erlaubte Gruppen, Obmannpflicht, Hersteller, Modell, Kauf-/Verkaufsdaten, OH-Felder |
| `BoatStatus` / `BoatStatusRecord` | aktueller Bootshausstatus je Boot | `BoatId`, `BoatText`, `UnknownBoat`, `BaseStatus`, `CurrentStatus`, `ShowInList`, `OnlyInBoathouseId`, `Logbook`, `EntryNo`, `Comment` |
| `BoatReservations` / `BoatReservationRecord` | Bootsreservierungen | `BoatId`, `Reservation`, `Type`, Datum/Zeit von/bis, Wochentag, PersonId/-Name, `Reason`, `Contact`, `HashId`, Vorstandsbeschluss/OH-Felder |
| `BoatDamages` / `BoatDamageRecord` | Bootsschaeden und Reparaturen | `BoatId`, `Damage`, `Description`, `Severity`, `Fixed`, Melde-/Fixdatum, meldende/fixende Person, Kosten, Versicherung/Claim, Notizen |
| `Destinations` / `DestinationRecord` | Ziele/Routen | `Id`, `Name`, `Start`, `End`, `StartIsBoathouse`, `Roundtrip`, Zielgebiete, Schleusen, `Distance`, `OnlyInBoathouseId`, `WatersIdList` |
| `Waters` / `WatersRecord` | Gewaesserstammdaten | `Id`, `EfbId`, `Name`, `Details` |
| `Status` / `StatusRecord` | Personen-/Mitgliedsstatus | `Id`, `Name`, `Type`, `Membership` |
| `Groups` / `GroupRecord` | Gruppen und Berechtigungsgruppen | `Id`, `Name`, `Color`, `MemberIdList` |
| `Crews` / `CrewRecord` | Standardmannschaften | `Id`, `Name`, Cox-/Crew-IDs, `BoatCaptain` |
| `Clubwork` / `ClubworkRecord` | Vereinsarbeitsstunden | `Id`, Person, Name, Datum, Beschreibung, Stunden, Personenliste, Flag, Kuerzel |
| `SessionGroups` / `SessionGroupRecord` | Fahrt-/Terminserien | `Id`, `Logbook`, `Name`, `Route`, `Organizer`, Start-/Enddatum, aktive Tage |
| `Statistics` / `StatisticsRecord` | gespeicherte Auswertungsdefinitionen | ID, Name, Position, Zeitraum, Kategorie/Typ/Key, Filter fuer Status/Fahrtart/Boot/Person/Gruppe, Ausgabefelder, Aggregationen |
| `Messages` / `MessageRecord` | interne Benachrichtigungen und Mail-Warteschlange | `MessageId`, `Date`, `Time`, `To`, `From`, `Subject`, `Text`, `Read`, `ToBeMailed`, `ReplyTo` |
| `Fahrtenabzeichen` / `FahrtenabzeichenRecord` | Fahrtenabzeichen-/DRV-relevante Personenwerte | `PersonId`, `Abzeichen`, `AbzeichenAB`, `Kilometer`, `KilometerAB`, `Fahrtenheft` |
| `AutoIncrement` / `AutoIncrementRecord` | Sequenzen fuer laufende IDs | `Sequence`, `IntValue`, `LongValue` |

## 7. Benutzerrollen und Betriebsmodi

Das System unterscheidet mehrere Applikations-IDs und Modi:

- `APPL_EFABASE`: Admin-/Basisanwendung.
- `APPL_EFABH`: Bootshausanwendung.
- `APPL_CLI`: Kommandozeile.
- `APPL_DRV`, `APPL_EMIL`, `APPL_ELWIZ`: Spezialprogramme.
- `APPL_MODE_NORMAL`: normaler Bootshausbetrieb.
- `APPL_MODE_ADMIN`: Adminmodus.

Administratoren werden ueber `AdminRecord`/`Admins` verwaltet. Rechte steuern unter anderem:

- Projekt- und Stammdatenverwaltung.
- Remote-Zugriff.
- Update-/Restart-Rechte.
- Zugriff auf Adminfunktionen im Bootshaus.

Im Bootshausmodus sind bestimmte Aktionen eingeschraenkt oder nur nach Admin-Freigabe moeglich.

## 8. Kernfunktionen

### 8.1 Projektverwaltung

Das System muss Projekte erstellen, oeffnen, schliessen, loeschen und konvertieren koennen. Beim Oeffnen eines Projekts werden:

1. Projektmetadaten geladen.
2. Interne Konvertierungen/Aufraeumarbeiten ausgefuehrt.
3. Alle benoetigten Storage-Objekte geoeffnet.
4. Bei lokalen Projekten optional ein Audit gestartet.
5. Bei Remote-Projekten ein Remote-Login vorbereitet und ein erster Zugriff ausgefuehrt.

### 8.2 Stammdatenverwaltung

efaBase muss folgende Stammdaten verwalten:

- Personen und Mitgliedsstatus.
- Boote inklusive Varianten, Bootstypen und Nutzungsbeschraenkungen.
- Ziele und Gewaesser.
- Gruppen und Standardmannschaften.
- Fahrtenbuecher und Vereinsarbeitsbuecher.
- Statistiken und Auswertungsdefinitionen.
- Admins und Konfiguration.

Validierungen erfolgen in den jeweiligen `preModifyRecordCallback()`-Methoden, z. B. Pflichtfelder, Eindeutigkeit, Referenzschutz und fachliche Konsistenz.

### 8.3 Fahrtenbuch

Ein Fahrtenbucheintrag beschreibt eine Fahrt mit:

- laufender Eintragsnummer.
- Startdatum und optional Enddatum.
- Boot per ID/Variante oder freier Bootname.
- Obmann/Cox und Mannschaft.
- Start-/Endzeit.
- Ziel, Zielvariante und Zusatzgewaesser.
- Distanz.
- Bemerkungen.
- Sessiongruppe/Fahrtgruppe.
- Offen-/geschlossen-Status.

Fachregeln:

- Eintragsnummern muessen eindeutig sein.
- Start-/Enddaten muessen im Fahrtenbuchzeitraum liegen.
- Bei gesetztem Enddatum darf das Enddatum nicht vor dem Startdatum liegen.
- Offene Fahrten werden im Bootshausstatus referenziert.

### 8.4 Bootshausbetrieb

`EfaBoathouseFrame` bildet die Hauptoberflaeche fuer den laufenden Bootshausbetrieb. Die Anwendung muss:

- Bootlisten nach Status anzeigen.
- Fahrten starten, korrigieren, beenden, nachtragen und abbrechen.
- Personen, Boot, Ziel, Zeit und Distanz erfassen.
- offene Fahrten und Bootstatus synchronisieren.
- Popups, Warnungen und Widgets anzeigen.
- Adminmodus temporaer aktivieren.

Der Bootshaus-Hintergrundthread `EfaBoathouseBackgroundTask` fuehrt periodisch aus:

- Projekt-/Remote-Status pruefen.
- Fahrten mit abgelaufener Endzeit automatisch beenden.
- GUI bei Konfigurationsaenderungen aktualisieren.
- Bootstatus und Reservierungen pruefen.
- veraltete Reservierungen entfernen.
- bei Reservierungen optional automatisch Fahrten starten.
- Nachrichtenstatus anzeigen.
- Link-/Todo-Dateien fuer Reservierungen/Personenprofile verarbeiten.
- automatische Beendigung/Neustart/Sperre pruefen.
- automatischen Fahrtenbuchwechsel ausfuehren.
- Fensterfokus/Always-on-top/Memory pruefen.
- Warnungen und offene Bootsschaeden melden.

### 8.5 Reservierungen

Bootsreservierungen unterstuetzen:

- einmalige und wiederkehrende Reservierungen.
- Zeitraum mit Datum und Uhrzeit.
- Person per ID oder freier Name.
- Grund, Kontakt, Hash-ID.
- automatische E-Mail-Bestaetigungen und Erinnerungen.
- Loeschung ueber Hash-/Efa-ID.
- optionale automatische Fahrtanlage aus Reservierungen.

OH-spezifische Link-/Dateiablaeufe:

- Eingangsordner: `${efaUserDirectory}/todo`.
- Dateien mit Endung `.txt` oder `.json` werden als Key-Value-Zeilen `key=value` gelesen.
- Aktionen: `DELETE`, `INSERT`, `SUBSCRIBE`, `UNSUBSCRIBE`, `CHANGE_NAME`, `SETMAIL`, `SETPHONENR`, `SETKUERZEL` bzw. im Code auch `SETKÜRZEL`.
- Nach Verarbeitung wird die Datei in ein Backupverzeichnis verschoben.

### 8.6 Bootsschaeden

Bootsschaeden muessen:

- Boot, Beschreibung, Schweregrad und Status erfassen.
- meldende und reparierende Personen speichern.
- Melde- und Reparaturzeitpunkt speichern.
- Kosten, Claims und Notizen fuehren.
- Bootstatus beeinflussen, wenn ein Schaden die Nutzung verhindert.
- regelmaessige Erinnerungen an Bootswart/Admin erzeugen, wenn Schaeden offen und alt genug sind.

### 8.7 Nachrichten und E-Mail

Interne Nachrichten werden in `Messages` gespeichert und koennen fuer Admin oder Bootswart markiert werden. Nachrichten koennen als E-Mail in die Versandwarteschlange gehen.

Der E-Mail-Versand wird ueber `EmailSenderThread` ausgefuehrt, wenn:

- das Programm nicht im DRV-Spezialmodus laeuft,
- eine Touch-Datei `undMitEmailVersand.touch.txt` im Benutzerverzeichnis vorhanden ist,
- E-Mail-Konfiguration gesetzt ist.

Mail-Funktionen werden unter anderem genutzt fuer:

- Reservierungsbestaetigungen.
- Reservierungserinnerungen.
- Personenprofil-Bestaetigungen.
- Warnungsberichte.
- Bootsschaden-Erinnerungen.

### 8.8 Statistiken und Wettbewerb

Statistiken werden als `StatisticsRecord` gespeichert und von `StatisticTask` verarbeitet. Unterstuetzt werden:

- Listen, Matrizen und weitere Auswertungskategorien.
- Filter nach Zeitraum, Status, Fahrtart, Bootstyp, Boot, Person, Gruppe.
- Aggregationen und konfigurierbare Ausgabefelder.
- Export nach CSV und weitere druck-/dateibasierte Ausgaben.

Wettbewerbsfunktionen in `data.efawett` und `gui.statistics` behandeln:

- DRV-Fahrtenabzeichen.
- DRV-Wanderruderstatistik.
- Meldedateien, Signaturen und Plausibilitaetspruefungen.
- Online-/Browsergestuetztes Einsenden von Meldedateien.
- Kanu-eFB-Konfigurationsdaten im Projekt.

### 8.9 Backup, Audit und automatische Jobs

Das System enthaelt:

- Backup-Funktionen fuer Projekt- und Datendateien.
- Audit fuer lokale Projekte nach dem Oeffnen.
- Crontab-Thread fuer automatische Ablaeufe.
- automatische Neustarts durch Zeitplan oder Markerdateien.
- Online-Update-Pruefung und Update-Start.
- Speicherueberwachung und kontrolliertes Beenden bei Speichermangel.

Markerdateien fuer Neustart/Aktionen werden an mehreren Orten gesucht, unter anderem im efa-Benutzerverzeichnis, Home-Verzeichnis, Programmverzeichnis und speziellen Downloadpfaden.

## 9. Konfiguration

Die globale Konfiguration (`EfaConfig`) ist selbst ein Storage-Objekt mit kategorisierten Parametern:

- intern/common.
- Eingabe.
- efaBase.
- Bootshaus.
- Erscheinungsbild und Buttons.
- Backup.
- externe Programme.
- Drucken.
- Starten/Beenden.
- Berechtigungen.
- Sperren.
- Benachrichtigungen.
- Typbezeichnungen.
- Synchronisation.
- Kanu-eFB.
- Sprache und Region.
- Widgets.
- Datenzugriff lokal/remote.
- automatische Ablaeufe.

Wichtige Konfigurationswirkungen:

- zuletzt geoeffnete Projekte/Fahrtenbuecher.
- Touchscreen- und Eingabeverhalten.
- Namensformat und Autokorrektur.
- Bootshauslisten, Schaltflaechen, Farben, Fonts.
- automatische Fahrtenbuchwechsel.
- automatische Fahrtanlage aus Reservierung.
- automatische Beendigung offener Fahrten nach Endzeit.
- E-Mail, Benachrichtigungen und Markierung gelesener Nachrichten.
- Remote-Server-Port und Datenzugriff.

## 10. Schnittstellen

### 10.1 Benutzeroberflaechen

Die UI ist Swing-basiert. Wichtige Dialog-/Frame-Klassen:

- `EfaBoathouseFrame`: Bootshaus-Hauptoberflaeche.
- `EfaBaseFrame`: Admin-/Basisdialog und Fahrtenbearbeitung.
- `EfaConfigDialog`: Konfiguration.
- `AdminDialog`: Adminzugang.
- `BrowserDialog`: interner Browser/HTML-Anzeige.
- `Statistics...Dialog`: Statistik- und Wettbewerbsauswertungen.
- `BaseDialog`, `BaseFrame`, `BaseTabbedDialog`: UI-Basisklassen.

### 10.2 CLI

`efaCLI` kann sich mit einem Projekt verbinden:

```text
[username[:password]@][host[:port]][/project] [-cmd command] [-v]
```

Die CLI nutzt Menueklassen wie `MenuStatistics`, `MenuBackup` und weitere Menues, um Kommandos interaktiv oder per `-cmd` auszufuehren.

### 10.3 Dateien

Relevante Dateitypen und Verzeichnisse:

- lokale XML-Datenobjekte im efa-Datenverzeichnis.
- Konfigurationsdateien unter dem efa-Benutzer-/Config-Verzeichnis.
- Backup-Verzeichnis fuer Sicherungen und verarbeitete Linkdateien.
- Logdatei `efa.log`.
- Markerdateien fuer E-Mail-Versand, Neustart und Update.
- Import-/Exportdateien fuer CSV, HTML, Wettbewerbe und alte efa1-Daten.

### 10.4 Netzwerk

Netzwerkzugriffe:

- efaRemote-Client/-Server ueber HTTP/URL-Verbindungen und XML-Nachrichten.
- Online-Update-Informationen.
- EfaOnline/Kanu-eFB/DRV-/Wettbewerbsnahe Online-Funktionen.
- SMTP fuer E-Mail.

## 11. Nichtfunktionale Anforderungen aus dem Code

### 11.1 Robustheit

- Hintergrundthreads fangen Exceptions weitgehend ab und protokollieren sie.
- Datenzugriffe koennen lokale und Remote-Projekte unterscheiden.
- Locking-Mechanismen schuetzen kritische Speicheroperationen.
- Audit und Konvertierungen reparieren bekannte Dateninkonsistenzen.
- Speicherueberwachung kann kontrolliertes Beenden ausloesen.

### 11.2 Mehrsprachigkeit

Texte laufen ueber `International`. Deutsch ist erkennbar primaere Zielsprache; viele Funktionen verwenden `International.getString()` / `getMessage()`. Entwickleroptionen koennen fehlende I18n-Schluessel markieren oder loggen.

### 11.3 Sicherheit

- Admin-Logins und Adminrechte steuern sensible Funktionen.
- Remote-Zugriff verwendet Benutzername/Passwort und Session-ID.
- Passworttypen fuer Datenfelder existieren gehasht und verschluesselt.
- Remote-Kommandos wie Update/Exit pruefen Berechtigungen.
- Keystore-Dateien werden fuer Signaturen/DRV-/Programmsicherheit verwendet.

### 11.4 Performance

- XML-Datenzugriffe arbeiten mit Schluesseln, Indizes, SCN und Caches.
- Remote-Bootshaus prueft SCN periodisch und aktualisiert Listen nur bei Bedarf.
- Hintergrundaufgaben sind intervallbasiert, typischerweise im Minutenbereich.
- Nachrichtenpruefung beschraenkt sich auf die letzten 50 Nachrichten.

## 12. Zentrale Workflows

### 12.1 Projekt oeffnen

1. Benutzer oder Startparameter waehlt Projekt.
2. `Project.openProject()` oeffnet Projekt-Metadaten.
3. Interne Konvertierungen laufen.
4. Projekt wird global in `Daten.project` gesetzt.
5. Alle benoetigten Storage-Objekte werden geoeffnet.
6. Lokaler Audit oder Remote-Login wird vorbereitet.
7. UI aktualisiert Projekt-/Fahrtenbuchstatus.

### 12.2 Fahrt im Bootshaus starten

1. Benutzer waehlt Boot und Aktion.
2. Eingabedialog erfasst Person/Mannschaft, Zeiten, Ziel, Distanz.
3. `LogbookRecord` wird erzeugt und validiert.
4. Fahrtenbucheintrag wird gespeichert.
5. `BoatStatusRecord` wird auf `ONTHEWATER` gesetzt und mit Fahrtenbuch/EntryNo verknuepft.
6. Bootsliste wird aktualisiert.

### 12.3 Fahrt beenden

1. Benutzer waehlt offene Fahrt/Boot.
2. Endezeit, Distanz und Bemerkungen werden erfasst oder automatisch gesetzt.
3. `LogbookRecord.Open` wird auf geschlossen gesetzt.
4. Bootstatus wird wieder auf Basisstatus/verfuegbar gesetzt.
5. Ereignis wird geloggt und Listen werden aktualisiert.

### 12.4 Automatischer Start aus Reservierung

1. Hintergrundtask findet aktuelle Reservierung fuer ein nicht auf Wasser befindliches Boot.
2. Wenn Konfiguration `AutomaticStartLogbookFromReservation` aktiv ist, wird ein neuer Fahrtenbucheintrag erstellt.
3. Person, Kontakt, Datum, Zeit, Boot und Grund werden aus der Reservierung uebernommen.
4. Ziel wird anhand des Reservierungsgrunds gesucht; sonst wird der Grund als Zielname gespeichert.
5. Reservierung wird unsichtbar gesetzt.
6. Bootstatus wird auf `ONTHEWATER` aktualisiert.

### 12.5 Automatischer Fahrtenbuchwechsel

1. Projekt enthaelt Datum und Namen fuer neues Fahrtenbuch.
2. Hintergrundtask prueft, ob das Datum erreicht ist.
3. Offene Fahrten werden bei lokalem Projekt abgebrochen.
4. Neues Fahrtenbuch wird geoeffnet.
5. Adminnachricht wird erstellt.
6. Umschaltstatus wird im Projekt gespeichert.

### 12.6 Reservierungs-Linkdatei verarbeiten

1. Hintergrundtask liest die aelteste `.txt`/`.json`-Datei im Todo-Ordner.
2. Datei wird als Key-Value-Map interpretiert.
3. Aktion wird validiert und auf Reservierung oder Person angewendet.
4. Bestaetigungs-/Fehlermeldung wird geloggt und ggf. per E-Mail versendet.
5. Datei wird ins Backup-Verzeichnis verschoben.

## 13. Bekannte Implementierungsgrenzen

- SQL-Datenzugriff ist im Interface vorgesehen, aber nicht implementiert.
- Viele Funktionen sind stark global ueber `Daten` gekoppelt.
- Swing-UI, Fachlogik und Datenzugriff sind teilweise eng verzahnt.
- Einige OH-spezifische Ablaufe verwenden Dateikonventionen und Stringaktionen statt strukturierter APIs.
- JSON-Dateien im Todo-Ordner werden laut Endung akzeptiert, aber der aktuelle Parser liest Key-Value-Zeilen `key=value`, kein vollwertiges JSON.
- Fehlerbehandlung protokolliert haeufig und setzt fort; fuer API-artige Aufrufer gibt es nicht immer strukturierte Fehlerobjekte.

## 14. Quellreferenzen

Wichtige Codebereiche fuer diese Spezifikation:

- `efa-parent/pom.xml`
- `efa-main/pom.xml`
- `README.md`
- `efa-main/src/main/java/de/nmichael/efa/Program.java`
- `efa-main/src/main/java/de/nmichael/efa/Daten.java`
- `efa-main/src/main/java/de/nmichael/efa/boathouse/Main.java`
- `efa-main/src/main/java/de/nmichael/efa/base/Main.java`
- `efa-main/src/main/java/de/nmichael/efa/cli/Main.java`
- `efa-main/src/main/java/de/nmichael/efa/gui/EfaBoathouseFrame.java`
- `efa-main/src/main/java/de/nmichael/efa/gui/EfaBaseFrame.java`
- `efa-main/src/main/java/de/nmichael/efa/gui/util/EfaBoathouseBackgroundTask.java`
- `efa-main/src/main/java/de/nmichael/efa/data/Project.java`
- `efa-main/src/main/java/de/nmichael/efa/data/*.java`
- `efa-main/src/main/java/de/nmichael/efa/data/storage/*.java`
- `efa-main/src/main/java/de/nmichael/efa/core/config/EfaConfig.java`
- `efa-main/src/main/java/de/nmichael/efa/statistics/*.java`
- `efa-main/src/main/java/de/nmichael/efa/data/efawett/*.java`

