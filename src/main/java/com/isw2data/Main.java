package com.isw2data;

import com.isw2data.controller.GitController;
import com.isw2data.controller.JiraController;
import com.isw2data.controller.WekaController;
import com.isw2data.model.TicketBug;
import com.isw2data.model.Release;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        String projectName;
        String repoPath;
        String configFilePath;
        Properties p = new Properties();

        if (args.length == 1) {
            String configOption = args[0];
            switch (configOption) {
                case "1":
                    System.out.println("Avviando Syncope");
                    configFilePath = "/configSYNCOPE.properties";
                    break;
                default:
                    System.out.println("Avviando default Bookkeeper");
                    configFilePath = "/configBOOKKEEPER.properties";
                    break;
            }
        } else {
            System.out.println("Avviando Bookkeeper");
            configFilePath = "/configBOOKKEEPER.properties";
        }

        try (InputStream is = (Objects.requireNonNull(Main.class.getResource(configFilePath))).openStream()) {
            p.load(is);
            repoPath = p.getProperty("repository");
            projectName = p.getProperty("projectName");
        }

        Repository repo = new FileRepositoryBuilder().setGitDir(new File(repoPath)).readEnvironment().findGitDir().build();
        JiraController jiraInfo = new JiraController();

        List<Release> releases = jiraInfo.getReleaseInfo(projectName);
        List<TicketBug> tickets = jiraInfo.getFixTicket(projectName, releases);

        jiraInfo.calculateProportionColdStart();
        jiraInfo.assignInjectedInversion(releases);

        GitController gitInfo= new GitController(repo, releases,tickets);
        gitInfo.createDataset(projectName);

        WekaController wekaController = new WekaController();
        wekaController.evaluateProject(projectName, releases.size());

        System.exit(0);
    }
}