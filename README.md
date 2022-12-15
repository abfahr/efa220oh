# Allgemein


# Dev-Umgebung
## Requirements
### Java JDK
- Installieren eines aktuellen Java JDKs z.B. OpenJDK 19
### Maven
- Installieren von Maven ab Version 3 (z.B. 3.8.6)
### Git
- Installieren eines lokalen GITs (z.B. 2.32.X)
### IDE
- Zur Entwicklung kann jede Java-fähige IDE eingesetzt werden. Die Entwicklung mit IntelliJ wird empfohlen.
## Lokale Installation EFA
1.  Clone von EFA auf lokale Maschine: 

         git clone https://github.com/abfahr/efa220oh.git
2. In das Checkout-Verzeichnis (standardmäßig) efa220oh/efa-parent wechseln und im Terminal / auf der Kommandozeile ausführen:
        
        mvn clean install
3. Wechseln in das Verzeichnis efa220oh/efa-main/target und starten der EFA-Applikation via:

        java -Xmx128m -XX:NewSize=32m -XX:MaxNewSize=32m -Duser.country=DE -Duser.language=de -jar efa-main-2.3.0-SNAPSHOT-jar-with-dependencies.jar
    

## Importieren in Intellij

## Debugging

## Testing

## Integration in Master

# PRD-Umgebung
