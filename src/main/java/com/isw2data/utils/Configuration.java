package com.isw2data.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import static java.lang.System.*;

/**
 * Gestisce la configurazione del progetto, caricando le impostazioni da un file di configurazione.
 */
public class Configuration {

    private String projectName;         // Nome del progetto
    private String repoPath;            // Percorso del repository
    private String configFilePathTemplate; // Modello di percorso del file di configurazione

    /**
     * Costruttore della classe Configuration.
     *
     * @param configFilePathTemplate Modello di percorso del file di configurazione.
     */
    public Configuration(String configFilePathTemplate) {
        this.configFilePathTemplate = configFilePathTemplate;
    }

    /**
     * Carica la configurazione a partire dagli argomenti della riga di comando.
     * Se non ci sono argomenti, carica il file di configurazione di default.
     *
     * @param args Argomenti della riga di comando.
     * @throws IOException Se si verifica un errore durante il caricamento del file di configurazione.
     */
    public void loadConfiguration(String[] args) throws IOException {
        String configFilePath;

        // Determina il percorso del file di configurazione basato sugli argomenti
        if (args.length == 1) {
            String configOption = args[0];
            configFilePath = getConfigFilePath(configOption);
        } else {
            out.println("Avviando Bookkeeper di default");
            configFilePath = String.format(configFilePathTemplate, "BOOKKEEPER");
        }

        // Carica le proprietà dal file di configurazione
        try (InputStream is = Objects.requireNonNull(Configuration.class.getResource(configFilePath)).openStream()) {
            Properties properties = new Properties();
            properties.load(is);
            this.repoPath = properties.getProperty("repository");
            this.projectName = properties.getProperty("projectName");
        }
    }

    /**
     * Restituisce il percorso del file di configurazione basato sull'opzione fornita.
     *
     * @param configOption Opzione di configurazione passata come argomento.
     * @return Percorso del file di configurazione.
     */
    private String getConfigFilePath(String configOption) {
        String configName;
        if (configOption.equals("1")) {
            out.println("Avviando Syncope");
            configName = "SYNCOPE";
        } else {
            out.println("Avviando default Bookkeeper");
            configName = "BOOKKEEPER";
        }
        return String.format(configFilePathTemplate, configName);
    }

    /**
     * Restituisce il nome del progetto.
     *
     * @return Nome del progetto.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Restituisce il percorso del repository.
     *
     * @return Percorso del repository.
     */
    public String getRepoPath() {
        return repoPath;
    }
}