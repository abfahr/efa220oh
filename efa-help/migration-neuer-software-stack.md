# Migration: efa in einen neuen Software-Stack

Stand: 2026-08-23  
Basis: `software-spezifikation.md` und Java-Code aus `efa220oh`

## 1. Zielbild

Die bestehende efa-Anwendung soll fachlich in einen neuen Software-Stack ueberfuehrt werden. Ziel ist keine 1:1-Portierung der Swing-/XML-Architektur, sondern eine fachlich kompatible Neuentwicklung mit sauberem Domänenmodell, zentraler Datenhaltung, API-Schicht und modernen Clients.

Der neue Stack sollte die fachlichen Kernprozesse erhalten:

- Bootshausbetrieb fuer Fahrtstart, Fahrtende, Korrektur und Nachtrag.
- Administration von Stammdaten und Fahrtenbuechern.
- Reservierungen inklusive automatischer Erinnerungen und optionalem Fahrtstart.
- Bootstatus und Bootsschadenverwaltung.
- Personen-/Mitgliederverwaltung.
- Statistiken, Exporte und verbandliche Meldungen.
- Benachrichtigungen und E-Mail-Versand.
- Import bestehender efa-Daten.

## 2. Empfohlene Zielarchitektur

Die neue Anwendung sollte als mehrschichtiges System aufgebaut werden:

| Schicht | Aufgabe |
| --- | --- |
| Web-/Tablet-Client Bootshaus | robuste, touchfreundliche Oberflaeche fuer wiederholte Bootshausbedienung |
| Admin-Webclient | Stammdaten, Konfiguration, Fahrtenbuecher, Auswertungen, Benutzer/Rechte |
| Backend/API | fachliche Use Cases, Validierung, Transaktionen, Berechtigungen |
| Job Worker | E-Mail, Erinnerungen, automatische Fahrtenden, Reservierungsjobs, Audit, Import/Export |
| Datenbank | zentrale relationale Datenhaltung mit Historisierung wo fachlich noetig |
| Importer | Migration aus bestehenden XML-Storage-Objekten |

Nicht mehr uebernehmen:

- globale Java-Singletons wie `Daten`.
- Swing-spezifische Dialoglogik.
- Dateibasierte Todo-Kommandos als primaere Integrationsschnittstelle.
- Remote-Efa-Protokoll als interne Architektur. Bei Bedarf nur als Legacy-Adapter erhalten.

## 3. Feature-Priorisierung

### 3.1 MVP: Muss migriert werden

Diese Funktionen bilden den produktiven Kern und sollten zuerst umgesetzt werden:

1. Projekt-/Clubverwaltung mit Bootshauskonfiguration.
2. Benutzer, Admins, Rollen und Rechte.
3. Personen und Mitgliedsstatus.
4. Bootsstammdaten inklusive Varianten und Nutzungseinschraenkungen.
5. Ziele, Gewaesser und Distanzen.
6. Fahrtenbuecher und Fahrteneintraege.
7. Bootshausoberflaeche fuer:
   - Fahrt starten.
   - Fahrt beenden.
   - Fahrt korrigieren.
   - Fahrt abbrechen.
   - Fahrt nachtragen.
8. Bootstatusliste:
   - verfuegbar.
   - auf dem Wasser.
   - nicht verfuegbar.
   - verborgen.
   - unbekanntes Boot.
9. Bootsreservierungen:
   - erstellen, aendern, loeschen.
   - aktuelle Reservierungen im Bootstatus anzeigen.
   - veraltete Reservierungen bereinigen.
10. Bootsschaeden:
   - erfassen.
   - Schweregrad.
   - als behoben markieren.
   - Statusauswirkung auf Bootsliste.
11. E-Mail-/Nachrichtensystem fuer Reservierungen und Admin-/Bootswart-Hinweise.
12. Datenimport aus bestehenden efa-XML-Dateien.

### 3.2 Phase 2: Wichtig, aber nach MVP

- Automatische Fahrtanlage aus Reservierungen.
- Automatisches Fahrtende bei gesetzter Endzeit.
- Automatischer Fahrtenbuchwechsel.
- Vereinsarbeit/Arbeitsstunden.
- Standardmannschaften.
- Gruppen und Gruppenberechtigungen.
- Statistikdefinitionen und Standardauswertungen.
- CSV-/PDF-/HTML-Export.
- Backup- und Wiederherstellungsfunktionen im neuen Format.
- Audit- und Reparaturroutinen fuer Datenkonsistenz.

### 3.3 Phase 3: Spezial- und Legacy-Funktionen

- DRV-Fahrtenabzeichen und Wanderruderstatistik.
- Kanu-eFB-Synchronisation.
- efaRemote-Kompatibilitaet.
- efaCLI-Kompatibilitaet.
- Import alter efa1-Daten.
- Online-Update-Mechanik.
- Dateibasierte Link-/Todo-Kompatibilitaet.

## 4. Fachliche Kernfeatures

### 4.1 Bootshausbetrieb

Der Bootshausclient muss fuer den Alltag optimiert sein: wenige Klicks, grosse Bedienelemente, klare Bootstatuslisten und tolerante Eingaben.

Pflichtfunktionen:

- Bootlisten nach Status anzeigen.
- Suche/Auswahl von Booten, Personen, Mannschaften und Zielen.
- Fahrt starten und offenen Bootstatus erzeugen.
- Fahrt beenden und Bootstatus freigeben.
- Fahrt korrigieren, abbrechen und nachtragen.
- Reservierungs- und Schadenhinweise direkt in der Bootsliste anzeigen.
- Adminmodus oder Adminfreigabe fuer geschuetzte Aktionen.
- Offline-/Netzwerkfehler freundlich behandeln, falls der Client als Tablet/Web-App eingesetzt wird.

Wichtige Regeln:

- Ein Boot kann nur eine offene Fahrt im aktiven Fahrtenbuch haben.
- Eine offene Fahrt muss mit `boat_status.current_trip_id` verknuepft sein.
- Beim Fahrtende wird der Fahrteneintrag geschlossen und der Bootstatus zurueckgesetzt.
- Nicht nutzbare Bootsschaeden koennen den Status `not_available` erzwingen.
- Reservierungen koennen Boote temporär als reserviert/nicht verfuegbar markieren.

### 4.2 Fahrtenbuch

Fahrteneintraege sind die wichtigste fachliche Entitaet.

Pflichtfelder:

- Fahrtenbuch.
- Eintragsnummer oder stabile externe Nummer.
- Datum.
- Boot oder freier Bootname.
- mindestens verantwortliche Person oder freier Personenname.
- Startzeit und optional Endzeit.
- Ziel oder freier Zielname.
- Distanz, falls Konfiguration Fahrten ohne Distanz nicht erlaubt.
- Offen-/geschlossen-Status.

Fachregeln:

- Eintragsnummer ist pro Fahrtenbuch eindeutig.
- Fahrtendatum darf nicht vor Startdatum liegen.
- Fahrtdatum muss im Gueltigkeitszeitraum des Fahrtenbuchs liegen, sofern dieser definiert ist.
- Offene Fahrt und Bootstatus muessen konsistent sein.
- Historische freie Namen muessen erhalten bleiben, auch wenn Stammdaten spaeter geaendert werden.

### 4.3 Stammdaten

Zu migrierende Stammdaten:

- Personen.
- Mitgliedsstatus.
- Boote.
- Bootstypen/Varianten.
- Ziele.
- Gewaesser.
- Gruppen.
- Standardmannschaften.
- Bootshaeuser.
- Admins/Benutzer.
- Konfigurationswerte.

Wichtig fuer den neuen Stack:

- Stammdaten erhalten technische UUIDs.
- Historische Fahrten speichern Snapshot-Namen zusaetzlich zu Referenzen.
- Versionierte Stammdaten aus efa sollten entweder als Historientabellen oder als `valid_from`/`valid_to` modelliert werden.

### 4.4 Reservierungen

Reservierungen muessen folgende Anwendungsfaelle abdecken:

- einmalige Reservierung.
- wiederkehrende Reservierung nach Wochentag.
- Reservierung fuer Mitglied oder freien Namen.
- Grund und Kontakt.
- Storno-Link oder Storno-Code.
- E-Mail-Bestaetigung.
- Erinnerung vor Startzeitpunkt.
- automatische Sichtbarkeit im Bootshausstatus.

Zu modernisieren:

- Der bisherige Hash-ID-Mechanismus sollte als `public_token` mit Ablauf-/Revocation-Option modelliert werden.
- Die bisherige Dateiaktion `DELETE`/`INSERT` sollte durch HTTP-API-Endpunkte ersetzt werden.
- Legacy-Dateien im `todo`-Ordner koennen optional von einem Import-/Compatibility-Worker weiter verarbeitet werden.

### 4.5 Bootsschaeden

Pflichtfunktionen:

- Schaden fuer Boot erfassen.
- Schweregrad erfassen.
- Nutzungseinschraenkung ableiten.
- Schaden als behoben markieren.
- Melde- und Reparaturinformationen speichern.
- Erinnerung fuer offene Schaeden erzeugen.

Statuswirkung:

- Schaden mit Schweregrad `not_useable` setzt Boot in der Bootshausliste auf nicht verfuegbar.
- Sobald alle nicht nutzbaren Schaeden behoben sind, darf der normale Bootstatus wieder greifen.

### 4.6 Nachrichten und E-Mail

Das neue System sollte interne Nachrichten und E-Mail-Versand trennen:

- `notifications`: fachliche Benachrichtigungen im System.
- `email_outbox`: Versandwarteschlange.
- `email_events`: Versandversuche und Fehler.

Migrationsrelevante Nachrichtentypen:

- Admin-Hinweis.
- Bootswart-Hinweis.
- Reservierungsbestaetigung.
- Reservierungserinnerung.
- Stornobestaetigung.
- Personenprofil-Bestaetigung.
- Warnungs-/Systembericht.
- Fahrtenbuchwechsel-Hinweis.

### 4.7 Statistiken und Exporte

Minimum:

- Fahrtenliste exportieren.
- Personen-/Bootsstatistik nach Zeitraum.
- Kilometer je Person, Boot, Gruppe und Zeitraum.
- CSV-Export.

Spaeter:

- gespeicherte Statistikdefinitionen migrieren.
- Matrixauswertungen.
- Wettbewerbs-/DRV-Auswertungen.
- PDF/HTML-Berichte.

### 4.8 Automatische Jobs

Der neue Stack sollte alle Hintergrundfunktionen als explizite Jobs modellieren:

| Job | Zweck | Frequenz |
| --- | --- | --- |
| `close-expired-trips` | offene Fahrten mit abgelaufener Endzeit beenden | jede Minute oder alle 5 Minuten |
| `refresh-boat-status` | Reservierungen/Schaeden in Statusprojektion einarbeiten | jede Minute |
| `reservation-reminders` | Reservierungserinnerungen erzeugen | alle 10 Minuten oder stuendlich |
| `purge-old-reservations` | veraltete Reservierungen bereinigen/archivieren | taeglich |
| `damage-reminders` | offene Bootsschaeden melden | taeglich |
| `logbook-switch` | automatischen Fahrtenbuchwechsel ausfuehren | taeglich/periodisch |
| `email-dispatch` | E-Mails senden | fortlaufend |
| `audit-consistency` | Datenkonsistenz pruefen | nach Import und nachts |
| `backup` | Datenbanksicherung/Export | konfigurierbar |

## 5. Ziel-Datenmodell

Das bestehende StorageObject/Record-Modell sollte in ein relationales Datenmodell ueberfuehrt werden. Die folgenden Tabellen bilden einen vorgeschlagenen Zielzustand.

### 5.1 Mandant und Projekt

#### `clubs`

Repraesentiert den Verein bzw. das Projekt.

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `name` | text | Vereins-/Projektname |
| `project_code` | text | ehemaliger Projektname/Dateiname |
| `address_street` | text | optional |
| `address_city` | text | optional |
| `admin_email` | text | optional |
| `global_association_name` | text | z. B. DRV/DKV |
| `global_association_member_no` | text | optional |
| `regional_association_name` | text | optional |
| `settings` | jsonb | selten genutzte Legacy-/Verbandswerte |
| `created_at`, `updated_at` | timestamp | Audit |

#### `boathouses`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK `clubs` |
| `name` | text | Bootshausname |
| `is_default` | boolean | Standard-Bootshaus |
| `legacy_boathouse_id` | integer | aus efa-Projekt |

### 5.2 Benutzer und Rechte

#### `users`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `username` | text | eindeutig je Club |
| `password_hash` | text | niemals Legacy-Passwort im Klartext speichern |
| `display_name` | text | Admin-/Benutzername |
| `email` | text | optional |
| `active` | boolean | Login erlaubt |
| `created_at`, `updated_at` | timestamp | Audit |

#### `roles`, `user_roles`, `permissions`

Rollen sollten mindestens enthalten:

- `admin`.
- `boathouse_operator`.
- `boat_maintenance`.
- `statistics`.
- `readonly`.

Legacy-Adminrechte werden beim Import auf Rollen/Permissions gemappt.

### 5.3 Stammdaten

#### `person_statuses`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `name` | text | Statusname |
| `type` | text | Legacy-Typ |
| `is_membership` | boolean | Mitgliedschaft |

#### `persons`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `first_name` | text | |
| `last_name` | text | |
| `display_name` | text | entspricht `FirstLastName` |
| `status_id` | UUID | FK `person_statuses` |
| `email` | text | optional |
| `phone` | text | optional/aus OH-Feldern ableiten |
| `membership_no` | text | optional |
| `input_shortcut` | text | Kuerzel |
| `allow_email` | boolean | Legacy `erlaubtEmail` |
| `allow_phone` | boolean | Legacy `erlaubtTelefon` |
| `allow_shortcut` | boolean | Legacy `erlaubtKürzel` |
| `exclude_from_statistics` | boolean | |
| `name_spelling_changed` | boolean | |
| `legacy_id` | UUID/text | Importreferenz |
| `valid_from`, `valid_to` | timestamp | falls Versionierung uebernommen wird |
| `created_at`, `updated_at` | timestamp | |

#### `boats`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `name` | text | |
| `name_affix` | text | optional |
| `owner` | text | optional |
| `default_variant_no` | integer | |
| `last_variant_no` | integer | |
| `manufacturer` | text | optional |
| `model` | text | optional |
| `max_crew_weight` | integer | optional |
| `purchase_date` | date | optional |
| `purchase_price` | decimal | optional |
| `selling_date` | date | optional |
| `selling_price` | decimal | optional |
| `only_with_boat_captain` | boolean | |
| `popup_question` | text | OH-Feld |
| `contact_person` | text | OH-Feld |
| `legacy_id` | UUID/text | Importreferenz |
| `valid_from`, `valid_to` | timestamp | falls Versionierung |

#### `boat_variants`

Legacy-Bootvarianten liegen als parallele Listen im Boot. Im neuen Modell sollten sie normalisiert werden.

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `boat_id` | UUID | FK `boats` |
| `variant_no` | integer | Legacy `TypeVariant` |
| `description` | text | |
| `boat_type` | text | Einer, Zweier, ... |
| `seats` | text/integer | Anzahl Plaetze |
| `rigging` | text | Riemen/Skull etc. |
| `coxing` | text | mit/ohne Stm. |

#### `groups` und `group_members`

| Tabelle | Zweck |
| --- | --- |
| `groups` | Name, Farbe, Club |
| `group_members` | Gruppe-Person-Zuordnung |
| `boat_allowed_groups` | Boot-Gruppen-Zuordnung |

#### `crews` und `crew_members`

Standardmannschaften:

- `crews`: Name, Club, BoatCaptain-Position.
- `crew_members`: Crew, Person, Position.

### 5.4 Fahrtenbuch

#### `logbooks`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `name` | text | Legacy-Fahrtenbuchname |
| `description` | text | optional |
| `start_date` | date | optional |
| `end_date` | date | optional |
| `is_current_boathouse` | boolean | aus Projektzustand ableiten |
| `is_current_admin` | boolean | optional |
| `legacy_storage_name` | text | Importreferenz |

#### `trips`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `logbook_id` | UUID | FK |
| `entry_no` | text/integer | eindeutig je Fahrtenbuch |
| `date` | date | Startdatum |
| `end_date` | date | optional |
| `boat_id` | UUID | optional FK |
| `boat_variant_no` | integer | optional |
| `boat_name_snapshot` | text | wichtig fuer Historie |
| `cox_person_id` | UUID | optional |
| `cox_name_snapshot` | text | |
| `boat_captain_position` | integer | 0=Cox, 1..n=Crew |
| `contact` | text | |
| `start_time` | time | |
| `end_time` | time | optional |
| `destination_id` | UUID | optional |
| `destination_name_snapshot` | text | |
| `destination_variant_name` | text | optional |
| `distance` | decimal | in Basiseinheit speichern |
| `comments` | text | |
| `session_group_id` | UUID | optional |
| `is_open` | boolean | offene Fahrt |
| `created_at`, `updated_at` | timestamp | |

#### `trip_crew_members`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `trip_id` | UUID | FK `trips` |
| `position` | integer | Crewplatz |
| `person_id` | UUID | optional |
| `name_snapshot` | text | historischer Name |

#### `session_groups`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `logbook_id` | UUID | optional |
| `name` | text | |
| `route` | text | |
| `organizer` | text | |
| `start_date`, `end_date` | date | |
| `active_days` | jsonb/int[] | Wochentage |

### 5.5 Bootshausstatus

#### `boat_statuses`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `boat_id` | UUID | PK/FK `boats` |
| `base_status` | enum/text | available, not_available, hidden |
| `current_status` | enum/text | available, on_water, not_available, hidden |
| `show_in_list` | enum/text | Anzeigeprojektion |
| `only_in_boathouse_id` | UUID | optional |
| `current_trip_id` | UUID | FK `trips`, wenn auf Wasser |
| `comment` | text | Reservierung/Schaden/Freitext |
| `updated_at` | timestamp | |

Empfehlung: `boat_statuses` als Projektion verwenden, aber alle statusrelevanten Entscheidungen aus `trips`, `reservations`, `boat_damages` und Stammdaten reproduzierbar halten.

### 5.6 Reservierungen

#### `boat_reservations`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `boat_id` | UUID | FK |
| `legacy_reservation_no` | integer | alte Reservierungsnummer |
| `type` | enum/text | one_time, weekly |
| `date_from`, `date_to` | date | |
| `day_of_week` | integer | fuer weekly |
| `time_from`, `time_to` | time | |
| `person_id` | UUID | optional |
| `person_name_snapshot` | text | |
| `reason` | text | |
| `contact` | text | |
| `public_token` | text | Ersatz fuer `HashId` |
| `is_invisible` | boolean | fuer ausgeblendete/verbrauchte Reservierungen |
| `board_approval` | text/boolean | OH-Feld |
| `last_reminder_sent_at` | timestamp | statt LastModified-Hack |
| `created_at`, `updated_at` | timestamp | |

Indizes:

- `(boat_id, date_from, date_to)`.
- `(public_token)`.
- `(person_id)`.
- `(type, day_of_week)`.

### 5.7 Bootsschaeden

#### `boat_damages`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `boat_id` | UUID | FK |
| `legacy_damage_no` | integer | |
| `description` | text | |
| `severity` | enum/text | info, warning, not_useable |
| `is_fixed` | boolean | |
| `reported_at` | timestamp | aus ReportDate/ReportTime |
| `reported_by_person_id` | UUID | optional |
| `reported_by_name_snapshot` | text | |
| `fixed_at` | timestamp | optional |
| `fixed_by_person_id` | UUID | optional |
| `fixed_by_name_snapshot` | text | |
| `repair_costs` | decimal | optional |
| `claim` | text | optional |
| `notes` | text | optional |
| `logbook_text` | text | optional |

### 5.8 Ziele und Gewaesser

#### `destinations`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `name` | text | |
| `start` | text | optional |
| `end` | text | optional |
| `start_is_boathouse` | boolean | |
| `roundtrip` | boolean | |
| `destination_areas` | text/jsonb | |
| `passed_locks` | integer/text | je nach Legacyinhalt |
| `distance` | decimal | |
| `only_in_boathouse_id` | UUID | optional |

#### `waters` und `destination_waters`

- `waters`: ID, EFB-ID, Name, Details.
- `destination_waters`: many-to-many zwischen Zielen und Gewaessern.

### 5.9 Vereinsarbeit

#### `clubwork_entries`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `person_id` | UUID | optional |
| `first_name`, `last_name`, `display_name` | text | Snapshot/Freitext |
| `date` | date | |
| `description` | text | |
| `hours` | decimal | |
| `person_list` | jsonb/text | Legacy |
| `flag` | text | |
| `input_shortcut` | text | |

### 5.10 Nachrichten und E-Mail

#### `notifications`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `recipient_type` | enum/text | admin, boat_maintenance, user |
| `recipient_user_id` | UUID | optional |
| `from_label` | text | |
| `subject` | text | |
| `body` | text | |
| `reply_to` | text | optional |
| `is_read` | boolean | |
| `created_at` | timestamp | |

#### `email_outbox`

| Feld | Typ | Hinweis |
| --- | --- | --- |
| `id` | UUID | Primary Key |
| `club_id` | UUID | FK |
| `to_email` | text | |
| `subject` | text | |
| `body` | text | |
| `status` | enum/text | pending, sent, failed |
| `attempts` | integer | |
| `last_error` | text | optional |
| `send_after` | timestamp | |
| `sent_at` | timestamp | optional |

### 5.11 Statistik und Wettbewerb

#### `statistics_definitions`

Die bestehende `StatisticsRecord`-Struktur ist gross und stark konfigurationsgetrieben. Fuer Migration empfiehlt sich:

- Kernfelder normal speichern: ID, Name, Kategorie, Typ, Zeitraum, Sichtbarkeit.
- Filter und Darstellungsoptionen als `jsonb` speichern.
- Spaeter neue native Statistikmodelle ableiten.

#### `competition_records`

Fahrtenabzeichen/DRV/Kanu-eFB sollten nicht ins MVP-Datenmodell gedrueckt werden. Fuer Phase 3:

- eigene Tabellen fuer Fahrtenabzeichen.
- gespeicherte Meldungen.
- Signaturen/Schluessel.
- Export-/Einreichstatus.

## 6. Datenmigration

### 6.1 Quellsystem

Das alte System speichert Daten als Storage-Objekte mit Record-Metadaten. Lokale Projekte liegen als XML-Dateien im efa-Datenverzeichnis. Ein Projekt besteht aus einer Projektdatei plus mehreren Objektdateien wie:

- Projektbeschreibung.
- Personen.
- Boote.
- Bootstatus.
- Reservierungen.
- Bootsschaeden.
- Ziele.
- Gewaesser.
- Nachrichten.
- Statistikdefinitionen.
- beliebige Fahrtenbuecher.
- Vereinsarbeitsbuecher.

### 6.2 Importstrategie

Empfohlen wird ein zweistufiger Import:

1. Raw Import:
   - alle XML-Storage-Objekte unverändert in Staging-Tabellen laden.
   - Originaldatei, StorageObjectType, StorageObjectName, Key, XML/JSON-Payload speichern.
   - Importfehler nicht sofort verwerfen, sondern protokollieren.
2. Normalisierung:
   - Staging-Daten in Zieltabellen transformieren.
   - Legacy-IDs und Legacy-Schluessel in Mappingtabellen speichern.
   - Referenzen nach Mapping aufloesen.
   - Konsistenzpruefungen ausfuehren.

### 6.3 Staging-Tabellen

#### `legacy_import_runs`

| Feld | Typ |
| --- | --- |
| `id` | UUID |
| `source_path` | text |
| `started_at`, `finished_at` | timestamp |
| `status` | text |
| `summary` | jsonb |

#### `legacy_records`

| Feld | Typ |
| --- | --- |
| `id` | UUID |
| `import_run_id` | UUID |
| `storage_object_type` | text |
| `storage_object_name` | text |
| `legacy_key` | text |
| `payload` | jsonb |
| `raw_xml` | text |
| `import_status` | text |
| `error_message` | text |

#### `legacy_id_map`

| Feld | Typ |
| --- | --- |
| `id` | UUID |
| `import_run_id` | UUID |
| `storage_object_type` | text |
| `storage_object_name` | text |
| `legacy_key` | text |
| `legacy_id` | text |
| `target_table` | text |
| `target_id` | UUID |

### 6.4 Reihenfolge der Normalisierung

1. Club/Projekt.
2. Bootshaeuser.
3. Mitgliedsstatus.
4. Personen.
5. Gruppen und Gruppenmitglieder.
6. Boote und Bootvarianten.
7. Ziele und Gewaesser.
8. Fahrtenbuecher.
9. Sessiongroups.
10. Fahrten und Crewmitglieder.
11. Bootstatus.
12. Reservierungen.
13. Bootsschaeden.
14. Nachrichten und E-Mail-Outbox.
15. Vereinsarbeit.
16. Statistikdefinitionen.
17. Fahrtenabzeichen/DRV/Kanu-eFB.

### 6.5 Wichtige Mappingregeln

- UUIDs aus Legacy-Daten nach Moeglichkeit beibehalten, wenn sie technisch gueltig und eindeutig sind.
- Laufende Nummern wie Fahrtenbuch-`EntryId`, Reservierungsnummer und Schadennummer als Legacy-/fachliche Nummer erhalten.
- Freitextnamen aus Fahrten, Reservierungen und Schaeden als Snapshot speichern.
- Listenfelder aus Legacy-Datensaetzen normalisieren, sofern sie Relationen darstellen.
- Unbekannte oder nicht aufloesbare Referenzen nicht verlieren: als Snapshot/Freitext importieren und im Importbericht markieren.
- Versionierte Stammdaten mit `ValidFrom`/`InvalidFrom` entweder in Historientabellen oder in `valid_from`/`valid_to` uebernehmen.
- `Invisible`/`Deleted` nicht ignorieren: als Archiv-/Sichtbarkeitsstatus modellieren.

### 6.6 Konsistenzpruefungen nach Import

Der neue Importer sollte mindestens pruefen:

- jedes Boot hat genau einen Bootstatus.
- jeder Bootstatus mit offener Fahrt referenziert eine existierende offene Fahrt.
- jede offene Fahrt hat, falls ein Boot gesetzt ist, einen passenden Bootstatus.
- Reservierungen referenzieren existierende Boote oder werden als fehlerhaft markiert.
- Bootsschaeden referenzieren existierende Boote oder werden als fehlerhaft markiert.
- Personenreferenzen in Fahrten, Reservierungen, Gruppen und Vereinsarbeit sind aufloesbar oder haben Snapshots.
- Fahrtenbuch-Eintragsnummern sind pro Fahrtenbuch eindeutig.
- Fahrtendatum liegt nicht vor Startdatum.
- Distanzen sind parsbar und in eine einheitliche Basiseinheit konvertiert.

## 7. API-Schnittstellen im neuen Stack

Die Backend-API sollte fachliche Use Cases abbilden, nicht nur CRUD.

### 7.1 Bootshaus-API

- `GET /boathouse/status` - Bootlisten mit Status, Reservierungen, Schaeden.
- `POST /trips/start` - Fahrt starten.
- `POST /trips/{id}/finish` - Fahrt beenden.
- `POST /trips/{id}/abort` - Fahrt abbrechen.
- `PATCH /trips/{id}` - Fahrt korrigieren.
- `POST /trips/late-entry` - Fahrt nachtragen.
- `GET /search/persons`
- `GET /search/boats`
- `GET /search/destinations`

### 7.2 Admin-API

- CRUD fuer Personen, Boote, Ziele, Gewaesser, Gruppen, Status, Fahrtenbuecher.
- Reservierungsverwaltung.
- Bootsschadenverwaltung.
- Benutzer/Rollen/Rechte.
- Konfiguration.
- Statistikdefinitionen.
- Import-/Exportjobs.

### 7.3 Public/Token-API

Fuer Ersatz der Linkdateien:

- `GET /public/reservations/{token}` - Reservierung anzeigen/stornierbar pruefen.
- `POST /public/reservations/{token}/cancel` - Reservierung stornieren.
- `POST /public/profile/{token}/email` - E-Mail aendern.
- `POST /public/profile/{token}/phone` - Telefon aendern.
- `POST /public/profile/{token}/shortcut` - Kuerzel aendern.
- `POST /public/newsletter/{token}/subscribe`
- `POST /public/newsletter/{token}/unsubscribe`

Tokens muessen zeitlich begrenzt, widerrufbar und auditierbar sein.

## 8. Konfiguration im neuen System

Konfiguration sollte in drei Ebenen getrennt werden:

| Ebene | Beispiele |
| --- | --- |
| System | SMTP, Storage, Logging, Backup, Hostnames |
| Club/Projekt | Bootshausregeln, Standard-Fahrtenbuch, Statistikregeln, Verband |
| Benutzer/Client | Anzeige, Sprache, Bootshaus-Clientoptionen |

Empfohlene Tabellen:

- `system_settings`.
- `club_settings`.
- `client_settings`.

Komplexe seltene Einstellungen koennen als JSON gespeichert werden, aber Kernregeln sollten eigene Spalten oder klar versionierte Settings besitzen.

## 9. Migrationsrisiken

| Risiko | Auswirkung | Gegenmassnahme |
| --- | --- | --- |
| Legacy-Daten enthalten inkonsistente Referenzen | Import bricht ab oder verliert Daten | Staging, Importbericht, Snapshot-Felder, Reparaturlauf |
| Versionierte Stammdaten werden falsch vereinfacht | historische Statistiken/Fahrten werden verfälscht | Historisierung oder Snapshot-Felder konsequent verwenden |
| Bootstatus wird als Quelle statt Projektion behandelt | offene Fahrten und Listen laufen auseinander | Transaktionale Use Cases und Auditjobs |
| Reservierungslogik wird unterschätzt | falsche Verfuegbarkeit im Bootshaus | Reservierungsengine separat testen |
| E-Mail-Semantik steckt in Record-Methoden | Benachrichtigungen fehlen nach Migration | Notification-Service mit expliziten Templates |
| Datei-/Linkintegration wird ersatzlos entfernt | bestehende externe Prozesse brechen | Public API plus optionaler Legacy-File-Worker |
| Statistikmodell ist zu gross fuer MVP | Projekt verzögert sich | einfache Kernstatistiken zuerst, Legacydefinitionen spaeter |

## 10. Empfohlener Umsetzungsplan

### Phase 0: Analyse und Import-Prototyp

- reale Produktionsdaten sammeln.
- XML-Parser fuer Storage-Objekte bauen.
- Staging-Schema definieren.
- Importbericht erzeugen.
- Datenvolumen und Inkonsistenzen messen.

### Phase 1: Produktiver Kern

- Datenbankmodell fuer Club, Personen, Boote, Ziele, Fahrtenbuecher, Fahrten, Bootstatus.
- Backend-Use-Cases fuer Fahrtstart/-ende.
- Bootshaus-Webclient.
- Admin-Grundfunktionen.
- Import aus Legacy-Daten fuer MVP-Tabellen.

### Phase 2: Betriebsfunktionen

- Reservierungen.
- Bootsschaeden.
- E-Mail/Notifications.
- automatische Jobs.
- Backup/Restore.
- Audit.

### Phase 3: Auswertung und externe Schnittstellen

- Statistiken.
- CSV/PDF/HTML-Export.
- DRV/Kanu-eFB.
- Public Token API.
- Legacy-Dateiworker.
- ggf. efaRemote/CLI-Kompatibilitaet.

### Phase 4: Parallelbetrieb und Cutover

- Legacy-Daten importieren.
- Testbetrieb gegen kopierte Produktivdaten.
- Differenzberichte zwischen alter und neuer Auswertung.
- Bootshaus-Pilot.
- finaler Freeze des alten Systems.
- finaler Import.
- Umschaltung.
- Legacy-System read-only archivieren.

## 11. Abnahmekriterien

MVP ist fachlich abnahmefaehig, wenn:

- bestehende Personen, Boote, Ziele und aktive Fahrtenbuecher importiert werden.
- Bootshausnutzer Fahrten starten und beenden koennen.
- offene Fahrten nach Migration korrekt als Bootstatus erscheinen.
- historische Fahrten lesbar und auswertbar bleiben.
- Reservierungen und Bootsschaeden mindestens angezeigt und administrativ bearbeitet werden koennen.
- Admins Stammdaten pflegen koennen.
- E-Mail-Versand fuer zentrale Reservierungs- und Adminereignisse funktioniert.
- Importbericht keine ungeklärten kritischen Referenzfehler enthaelt.

## 12. Offene Entscheidungen

- Welcher neue Stack wird gewaehlt?
- Soll der Bootshausclient offlinefaehig sein?
- Wird Mehrmandantenbetrieb benoetigt oder reicht ein Club pro Installation?
- Werden Legacy-Remote-Clients weiter unterstuetzt?
- Wie lange muessen alte Statistikdefinitionen exakt kompatibel bleiben?
- Sollen DRV/Kanu-eFB-Funktionen direkt migriert oder neu bewertet werden?
- Soll die neue Datenbank PostgreSQL, SQLite oder ein anderer Speicher sein?
- Wird die Anwendung lokal im Bootshaus betrieben oder zentral gehostet?

