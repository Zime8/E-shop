# Per avviare da intellij inserire in VM Options "--add-modules javafx.controls,javafx.fxml --module-path <percorso>/javafx-sdk-XX/lib", altrimenti con maven "mvn clean javafx:run"
### Configurazione database

1. Copiare il file di esempio:

   src/main/resources/db.properties.example
   -> src/main/resources/db.properties

2. Inserire le proprie credenziali MySQL nel file `db.properties`.

3. Creare il database:
   CREATE DATABASE e_commerce_db;