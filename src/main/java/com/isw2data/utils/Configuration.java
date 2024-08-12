package com.isw2data.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import static java.lang.System.*;

public class Configuration {

    private String projectName;
    private String repoPath;
    private String configFilePathTemplate;

    public Configuration(String configFilePathTemplate) {
        this.configFilePathTemplate = configFilePathTemplate;
    }

    public void loadConfiguration(String[] args) throws IOException {
        String configFilePath;

        if (args.length == 1) {
            String configOption = args[0];
            configFilePath = getConfigFilePath(configOption);
        } else {
            out.println("Avviando Bookkeeper di default");
            configFilePath = String.format(configFilePathTemplate, "BOOKKEEPER");
        }

        try (InputStream is = Objects.requireNonNull(Configuration.class.getResource(configFilePath)).openStream()) {
            Properties p = new Properties();
            p.load(is);
            this.repoPath = p.getProperty("repository");
            this.projectName = p.getProperty("projectName");
        }
    }

    private String getConfigFilePath(String configOption) {
        String configName;
        if (configOption.equals("1")) {
            out.println("Avviando Syncope");
            configName = "SYNCOPE";
        }
        else {
            out.println("Avviando default Bookkeeper");
            configName = "BOOKKEEPER";
        }
        return String.format(configFilePathTemplate, configName);
    }

    public String getProjectName() {
        return projectName;
    }

    public String getRepoPath() {
        return repoPath;
    }
}
