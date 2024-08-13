package com.isw2data;

import com.isw2data.controller.GitController;
import com.isw2data.controller.JiraController;
import com.isw2data.controller.WekaController;
import com.isw2data.model.TicketBug;
import com.isw2data.utils.Configuration;
import com.isw2data.model.Release;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Classe principale per l'esecuzione dell'applicazione.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Carica la configurazione a partire dagli argomenti passati
            Configuration config = new Configuration("/config%s.properties");
            config.loadConfiguration(args);

            String projectName = config.getProjectName();
            String repoPath = config.getRepoPath();

            // Costruisce il repository Git a partire dal percorso fornito
            Repository repo = new FileRepositoryBuilder()
                    .setGitDir(new File(repoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            // Crea un'istanza di JiraController per recuperare informazioni sui rilasci e sui ticket
            JiraController jiraInfo = new JiraController();

            // Ottiene informazioni sui rilasci e sui ticket da Jira
            List<Release> releases = jiraInfo.getReleaseInfo(projectName);
            List<TicketBug> tickets = jiraInfo.getFixTicket(projectName, releases);

            // Calcola la proporzione di cold start e assegna le versioni iniettate ai rilasci
            jiraInfo.calculateProportionColdStart();
            jiraInfo.assignInjectedInversion(releases);

            // Crea un'istanza di GitController e genera il dataset per il progetto
            GitController gitInfo = new GitController(repo, releases, tickets);
            gitInfo.createDataset(projectName);

            // Crea un'istanza di WekaController e valuta il progetto utilizzando Weka
            WekaController wekaController = new WekaController();
            wekaController.evaluateProject(projectName, releases.size());

            // Termina il programma con successo
            System.exit(0);

        } catch (IOException e) {
            // Gestisce eventuali errori di input/output e fornisce un messaggio di errore dettagliato
            System.err.println("Errore IO: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            // Gestisce eventuali altri tipi di eccezioni e fornisce un messaggio di errore dettagliato
            System.err.println("Errore: " + e.getMessage());
            System.exit(1);
        }
    }
}
