package com.isw2data.controller;

import com.isw2data.utils.Utils;
import com.isw2data.utils.HolidayUtils;
import com.isw2data.model.TicketBug;
import com.isw2data.model.Class;
import com.isw2data.model.Release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import static java.lang.System.*;

public class GitController {
    private Repository repo;
    private Git git;
    private List<Release> releases;  // Le release del repository ordinate per data di rilascio crescente
    private List<TicketBug> tickets;  // I ticket su Jira associati al repository
    private static final String CLASS_PATH = ".java";
    private static final String TEST_DIR = "/test/";

    public GitController(Repository repo, List<Release> releases, List<TicketBug> tickets) {
        this.repo = repo;
        this.git = new Git(repo);
        this.releases = releases;
        this.tickets = tickets;
    }

    // Crea il dataset per un progetto specifico
    public void createDataset(String projectName) throws GitAPIException, IOException {
        List<RevCommit> commits = getCommitsFromMaster();

        assignCommitsToReleases(commits);
        checkEmptyReleases();
        assignClassesToReleases();
        calculateMetrics();
        setFixCommits();

        printCsvArffWalkForward(projectName);
        deleteLastReleases();
        printDataset(projectName);
    }

    // Recupera tutti i commit di tutti i branch del repository corrente
    public List<RevCommit> getAllCommits() throws IOException {
        Collection<Ref> allRefs = repo.getRefDatabase().getRefs();
        List<RevCommit> allCommits = new ArrayList<>();

        try (RevWalk revWalk = new RevWalk(repo)) {
            for (Ref ref : allRefs) {
                revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
            }
            out.println("Walking all commits starting with refs: " + allRefs);
            int count = 0;
            for (RevCommit commit : revWalk) {
                allCommits.add(commit);
                count++;
            }
            out.println("Had " + count + " commits");
        }

        return allCommits;
    }

    // Recupera i commit dal branch master
    private List<RevCommit> getCommitsFromMaster() throws IOException, GitAPIException {
        List<RevCommit> commitsFromHead = new ArrayList<>();
        ObjectId branchId = this.repo.resolve("HEAD");
        Iterable<RevCommit> commits = this.git.log().add(branchId).call();

        for (RevCommit commit : commits) {
            commitsFromHead.add(commit);
        }
        return commitsFromHead;
    }

    // Assegna i commit alle release di appartenenza
    private void assignCommitsToReleases(List<RevCommit> commits) {
        for (RevCommit commit : commits) {
            LocalDateTime commitTime = Utils.convertTime(commit.getCommitTime());

            for (int i = 0; i < releases.size(); i++) {
                if (releases.get(i).getReleaseDate().isAfter(commitTime)) {
                    releases.get(i).addCommit(commit);
                    break;
                }
            }
        }
        out.println("I commit sono stati assegnati alle release");

        setLastCommitFromRelease();
    }

    // Imposta l'ultimo commit di ogni release (l'ultimo commit è il primo nella lista ordinata per data)
    private void setLastCommitFromRelease() {
        for (Release release : releases) {
            List<RevCommit> commits = release.getAllCommits();
            if (commits != null && !commits.isEmpty()) {
                release.setLastCommit(commits.get(0));
            }
        }
    }

    // Verifica la presenza di release senza commit e le rimuove se vuote
    private void checkEmptyReleases() {
        Iterator<Release> iterator = releases.iterator();
        while (iterator.hasNext()) {
            Release currentRelease = iterator.next();
            if (currentRelease.getAllCommits() == null || currentRelease.getAllCommits().isEmpty()) {
                out.println("Rimossa la release " + currentRelease.getIndex() + " per assenza di commit");
                shiftReleaseIndexes(currentRelease.getIndex());
                iterator.remove();
            }
        }
    }

    // Sposta gli indici delle release successivi a quello rimosso
    private void shiftReleaseIndexes(int index) {
        for (Release release : releases) {
            if (release.getIndex() > index) release.setIndex(release.getIndex() - 1);
        }
    }

    // Assegna le classi presenti in ogni release
    private void assignClassesToReleases() throws IOException {
        for (Release release : releases) {
            RevCommit lastCommit = release.getLastCommit();
            if (lastCommit != null) {
                Map<String, Class> relClasses = getClasses(lastCommit, release);
                release.setAllClasses(relClasses);
            }
        }
        out.println("Le classi sono state assegnate alle release");
    }

    // Recupera le classi del repository al momento del commit
    private Map<String, Class> getClasses(RevCommit commit, Release release) throws IOException {
        RevTree tree = commit.getTree();
        Map<String, Class> allClasses = new HashMap<>();

        try (TreeWalk treeWalk = new TreeWalk(this.repo)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                // Considera solo i file Java (escludendo i file di test)
                if (treeWalk.getPathString().contains(CLASS_PATH) && !treeWalk.getPathString().contains(TEST_DIR)) {
                    allClasses.put(treeWalk.getPathString(), new Class(
                            treeWalk.getPathString(),
                            new String(this.repo.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8),
                            release
                    ));
                }
            }
        }

        return allClasses;
    }

    // Calcola le metriche per ogni release
    private void calculateMetrics() throws GitAPIException, IOException {
        for (Release release : releases) {
            calculateLoc(release);
            calculateAllLocMetrics(release);
            calculatenAuth(release);
            personalMetrics(release);
        }
    }

    // Calcola il numero di giorni festivi tra due release
    public void personalMetrics(Release release) {
        if (release.getIndex() > 1) {
            LocalDateTime currentReleaseDate = release.getReleaseDate();
            LocalDateTime previousReleaseDate = releases.get(release.getIndex() - 2).getReleaseDate();

            Duration duration = Duration.between(previousReleaseDate, currentReleaseDate);
            long holidaysBetween = HolidayUtils.countHolidaysBetween(previousReleaseDate, currentReleaseDate, previousReleaseDate.getYear(), "US");
            if(holidaysBetween==0) holidaysBetween=1;
            float holiday = ((float) (duration.toDays())/(float) (holidaysBetween));
            for (Class javaClass : release.getAllClasses().values()) {
                javaClass.setHoliday(holiday);
            }

        }
    }

    // Calcola le linee di codice di ogni classe in una certa release
    private void calculateLoc(Release release) {
        for (Class javaClass : release.getAllClasses().values()) {
            String[] lines = javaClass.getContent().split("\r\n|\r|\n");
            javaClass.setLoc(lines.length);
        }
    }

    // Calcola le metriche relative alle linee di codice per ogni commit
    private void calculateAllLocMetrics(Release release) throws GitAPIException, IOException {
        if (release.getAllCommits() == null) return;

        for (RevCommit commit : release.getAllCommits()) {
            updateMetricsForCommit(commit, release);
        }

        // Imposta il valore medio di churn per ogni classe
        release.getAllClasses().values().forEach(this::setAverageChurn);
    }

    // Aggiorna le metriche di churn per ogni commit
    private void updateMetricsForCommit(RevCommit commit, Release release) throws GitAPIException, IOException {
        if (commit.getParentCount() == 0) return;

        String commitId = commit.getId().getName();
        String parentCommit = commitId + "^";

        List<DiffEntry> diffs = this.git.diff()
                .setOldTree(prepareTreeParser(this.repo, parentCommit))
                .setNewTree(prepareTreeParser(this.repo, commitId))
                .call();

        for (DiffEntry diff : diffs) {
            updateMetricsForDiff(diff, commit, release);
        }
    }

    // Aggiorna le metriche per ogni differenza (diff) di un commit
    private void updateMetricsForDiff(DiffEntry diff, RevCommit commit, Release release) throws IOException {
        if (!diff.getNewPath().contains(CLASS_PATH) || diff.getNewPath().contains(TEST_DIR)) return;

        DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(this.repo);

        String modifiedClassPath = diff.getNewPath();
        Class modifiedClass = release.getAllClasses().get(modifiedClassPath);

        if (modifiedClass == null) return;

        int addedLines = 0;
        int deletedLines = 0;
        for (Edit edit : formatter.toFileHeader(diff).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
            deletedLines += edit.getEndA() - edit.getBeginA();
        }

        int locTouched = addedLines + deletedLines;
        int churn = addedLines - deletedLines;

        modifiedClass.incrementLocTouched(locTouched);
        modifiedClass.incrementLocAdded(addedLines);
        modifiedClass.incrementLocChurn(churn);

        if (addedLines >= modifiedClass.getMaxLocAdded()) {
            modifiedClass.setMaxLocAdded(addedLines);
        }

        if (churn >= modifiedClass.getMaxChurn()) {
            modifiedClass.setMaxChurn(churn);
        }

        modifiedClass.addChurnArray(churn);
        modifiedClass.incrementNumRevisions();

        String author = commit.getAuthorIdent().getName();
        if (!modifiedClass.getAuthors().contains(author)) {
            modifiedClass.addAuthors(author);
        }
    }

    // Imposta il valore medio di churn per una classe
    private void setAverageChurn(Class javaClass) {
        List<Integer> churnArray = javaClass.getChurnArray();
        if (!churnArray.isEmpty()) {
            float averageChurn = (float) churnArray.stream().mapToInt(Integer::intValue).sum() / churnArray.size();
            javaClass.setAverageChurn(averageChurn);
        }
    }

    // Recupera i percorsi delle classi modificate da un commit
    private List<String> getClassesFromCommit(RevCommit commit) throws GitAPIException, IOException {
        List<String> modifiedClassPaths = new ArrayList<>();
        if (commit.getParentCount() == 0) return modifiedClassPaths;

        String commitId = commit.getId().getName();
        String parentCommit = commitId + "^";

        List<DiffEntry> diffs = this.git.diff()
                .setOldTree(prepareTreeParser(this.repo, parentCommit))
                .setNewTree(prepareTreeParser(this.repo, commitId))
                .call();

        for (DiffEntry diff : diffs) {
            if (diff.getNewPath().contains(CLASS_PATH) && !diff.getNewPath().contains(TEST_DIR)) {
                modifiedClassPaths.add(diff.getNewPath());
            }
        }

        return modifiedClassPaths;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();
            return treeParser;
        }
    }

    // Imposta il numero di autori per ogni classe
    private void calculatenAuth(Release release) {
        if (release.getAllClasses() != null) {
            for (Class javaClass : release.getAllClasses().values()) {
                javaClass.setNumAuth(javaClass.getAuthors().size());
            }
        }
    }

    // Assegna i commit di fix ai ticket
    private void setFixCommits() {
        int count = 0;
        int noCount = 0;
        for (TicketBug ticket : tickets) {
            if (assignCommitToTicket(ticket)) {
                count++;
            } else {
                noCount++;
            }
        }
        out.println("Fix commit: " + count + "\t No fix commit: " + noCount);
    }

    // Assegna il commit di fix ai ticket, basandosi sull'ID del ticket. Se il commit non viene trovato, il ticket viene scartato.
    private boolean assignCommitToTicket(TicketBug fixTicket) {
        boolean found = false;
        for (Release release : releases) {
            if (release.getAllCommits() != null) {
                for (RevCommit fixCommit : release.getAllCommits()) {
                    // Controlla se il messaggio del commit contiene l'ID del ticket
                    if (fixCommit.getFullMessage().contains(fixTicket.getKey() + "]") || fixCommit.getFullMessage().contains(fixTicket.getKey() + ":")) {
                        fixTicket.addFixCommit(fixCommit);
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    // Etichetta le classi come buggy in base ai commit di fix e alle release tra IV (inclusa) e FV (esclusa).
    private void setBuggyClassesWalkForward(int index) throws GitAPIException, IOException {
        for (TicketBug ticket : tickets) {
            // Scarta i ticket che hanno IV = FV, poiché siamo interessati solo ai difetti post-release (IV < FV)
            if (ticket.getFixVersion().getIndex() > index || ticket.getInjectedVersion().getId() == ticket.getFixVersion().getId()) {
                continue;
            }
            // Recupera i commit di fix associati al ticket
            for (RevCommit fixCommit : ticket.getCommits()) {
                // Recupera i path delle classi modificate dal commit (ignorando le classi di test)
                List<String> classPaths = getClassesFromCommit(fixCommit);
                // Recupera tutte le release tra IV (inclusa) e FV (esclusa) indicate sul ticket
                // Etichetta come buggy le classi modificate dal commit in queste release
                Release injectedRelease = ticket.getInjectedVersion();
                Release fixRelease = ticket.getFixVersion();
                setNumFixes(classPaths, fixRelease);
                if (!classPaths.isEmpty()) {
                    labelClasses(classPaths, injectedRelease, fixRelease);
                }
            }
        }
    }

    // Etichetta le classi come buggy in base ai commit di fix e alle release tra IV (inclusa) e FV (esclusa).
    public void setBuggyClasses() throws GitAPIException, IOException {
        for (TicketBug ticket : tickets) {
            // Scarta i ticket che hanno IV = FV, poiché siamo interessati solo ai difetti post-release (IV < FV)
            if (ticket.getInjectedVersion().getId() == ticket.getFixVersion().getId()) continue;
            // Recupera i commit di fix associati al ticket
            for (RevCommit fixCommit : ticket.getCommits()) {
                // Recupera i path delle classi modificate dal commit (ignorando le classi di test)
                List<String> classPaths = getClassesFromCommit(fixCommit);
                // Recupera tutte le release tra IV (inclusa) e FV (esclusa) indicate sul ticket
                // Etichetta come buggy le classi modificate dal commit in queste release
                Release injectedRelease = ticket.getInjectedVersion();
                Release fixRelease = ticket.getFixVersion();
                setNumFixes(classPaths, fixRelease);
                if (!classPaths.isEmpty()) {
                    labelClasses(classPaths, injectedRelease, fixRelease);
                }
            }
        }
    }

    // Etichetta le classi come buggy per ogni release tra l'injectedRelease e la fixRelease
    private void labelClasses(List<String> classPaths, Release injectedRelease, Release fixRelease) {
        // Itera fino alla fixRelease e si ferma
        for (Release release : releases) {
            if (release.getIndex() == fixRelease.getIndex()) break;
            if (release.getIndex() >= injectedRelease.getIndex() && release.getIndex() < fixRelease.getIndex()) {
                for (String modifiedClass : classPaths) {
                    Class modifClass = release.getAllClasses().get(modifiedClass);
                    if (modifClass != null) {
                        // Imposta la classe come buggy in una certa release
                        modifClass.setBuggy(true);
                    }
                }
            }
        }
    }

    // Incrementa di 1 il numero di fix delle classi associate a un commit di fix nella fixRelease
    private void setNumFixes(List<String> classPaths, Release fixRelease) {
        for (String classPath : classPaths) {
            Class modifiedClass = fixRelease.getAllClasses().get(classPath);
            if (modifiedClass != null) modifiedClass.addFix();
        }
    }

    // Rimuove metà delle release più recenti per ridurre lo snoring
    private void deleteLastReleases() {
        int numReleases = releases.size();
        releases.removeIf(currentRelease -> currentRelease.getIndex() > numReleases / 2);
    }

    // Genera i file CSV e ARFF per il training e il testing usando l'approccio walkForward
    private void printCsvArffWalkForward(String projName) throws IOException, GitAPIException {
        // Crea due file per ogni release del progetto (a partire dalla seconda release)
        // Uno per il training set, l'altro per il testing set, usando l'approccio walkForward
        for (int i = 2; i < releases.size() / 2; i++) { // L'indice i tiene traccia della creazione di training e testing set
            String trainingName = projName + "//training//" + "training_" + i + "_" + projName + ".csv";
            String arffName = projName + "//training//" + "training_" + i + "_" + projName + ".arff";

            setBuggyClassesWalkForward(i);
            printCsvTrainingSet(trainingName, i);
            writeArffTraining(arffName, i);
        }

        // Per il testing set, utilizza tutte le informazioni disponibili (ponendo il punto di osservazione all'ultima release)
        setBuggyClassesWalkForward(releases.size() - 1);
        for (int i = 2; i < releases.size() / 2; i++) {
            String testingName = projName + "//testing//" + "testing_" + i + "_" + projName + ".csv";
            String arffName = projName + "//testing//" + "testing_" + i + "_" + projName + ".arff";

            printCsvTestingSet(testingName, i);
            writeArffTesting(arffName, i);
        }
    }

    // Crea il file CSV per il training set
    private void printCsvTrainingSet(String trainingName, int i) throws IOException {
        try (FileWriter trainingWriter = new FileWriter(trainingName)) {
            trainingWriter.append("LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,Churn,MAX_Churn,AVG_Churn,AVG_Holiday,Buggy")
                    .append("\n");
            for (int j = 0; j < i; j++) {
                for (Class javaClass : releases.get(j).getAllClasses().values()) {
                    trainingWriter.append(String.valueOf(javaClass.getLoc())).append(",")
                            .append(String.valueOf(javaClass.getLocTouched())).append(",")
                            .append(String.valueOf(javaClass.getNr())).append(",")
                            .append(String.valueOf(javaClass.getNumFix())).append(",")
                            .append(String.valueOf(javaClass.getNumAuth())).append(",")
                            .append(String.valueOf(javaClass.getLocAdded())).append(",")
                            .append(String.valueOf(javaClass.getMaxLocAdded())).append(",")
                            .append(String.valueOf(javaClass.getChurn())).append(",")
                            .append(String.valueOf(javaClass.getMaxChurn())).append(",")
                            .append(String.valueOf(javaClass.getAverageChurn())).append(",")
                            .append(String.valueOf(javaClass.getAverageHolydays())).append(",")
                            .append(javaClass.isBuggy() ? "YES" : "NO")
                            .append("\n");
                }
            }
        }
    }

    // Crea il file CSV per il testing set
    private void printCsvTestingSet(String testingName, int i) throws IOException {
        try (FileWriter testingWriter = new FileWriter(testingName)) {
            testingWriter.append("LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,Churn,MAX_Churn,AVG_Churn,AVG_Holiday,Buggy")
                    .append("\n");
            for (Class javaClass : releases.get(i).getAllClasses().values()) {
                testingWriter.append(String.valueOf(javaClass.getLoc())).append(",")
                        .append(String.valueOf(javaClass.getLocTouched())).append(",")
                        .append(String.valueOf(javaClass.getNr())).append(",")
                        .append(String.valueOf(javaClass.getNumFix())).append(",")
                        .append(String.valueOf(javaClass.getNumAuth())).append(",")
                        .append(String.valueOf(javaClass.getLocAdded())).append(",")
                        .append(String.valueOf(javaClass.getMaxLocAdded())).append(",")
                        .append(String.valueOf(javaClass.getChurn())).append(",")
                        .append(String.valueOf(javaClass.getMaxChurn())).append(",")
                        .append(String.valueOf(javaClass.getAverageChurn())).append(",")
                        .append(String.valueOf(javaClass.getAverageHolydays())).append(",")
                        .append(javaClass.isBuggy() ? "YES" : "NO")
                        .append("\n");
            }
        }
    }

    // Crea il file ARFF per il testing set
    private void writeArffTesting(String filename, int i) throws IOException {
        String[] parts = filename.split("\\.");
        try (FileWriter fileWriter = new FileWriter(filename)) {
            fileWriter.append("@relation " + parts[0]).append("\n\n")
                    .append("@attribute LOC numeric").append("\n")
                    .append("@attribute LOC_touched numeric").append("\n")
                    .append("@attribute NR numeric").append("\n")
                    .append("@attribute NFix numeric").append("\n")
                    .append("@attribute NAuth numeric").append("\n")
                    .append("@attribute LOC_added numeric").append("\n")
                    .append("@attribute MAX_LOC_added numeric").append("\n")
                    .append("@attribute Churn numeric").append("\n")
                    .append("@attribute MAX_Churn numeric").append("\n")
                    .append("@attribute AVG_Churn numeric").append("\n")
                    .append("@attribute AVG_Holiday numeric").append("\n")
                    .append("@attribute Buggy {'YES', 'NO'}").append("\n\n")
                    .append("@data").append("\n");
            for (Class javaClass : releases.get(i).getAllClasses().values()) {
                fileWriter.append(String.valueOf(javaClass.getLoc())).append(",")
                        .append(String.valueOf(javaClass.getLocTouched())).append(",")
                        .append(String.valueOf(javaClass.getNr())).append(",")
                        .append(String.valueOf(javaClass.getNumFix())).append(",")
                        .append(String.valueOf(javaClass.getNumAuth())).append(",")
                        .append(String.valueOf(javaClass.getLocAdded())).append(",")
                        .append(String.valueOf(javaClass.getMaxLocAdded())).append(",")
                        .append(String.valueOf(javaClass.getChurn())).append(",")
                        .append(String.valueOf(javaClass.getMaxChurn())).append(",")
                        .append(String.valueOf(javaClass.getAverageChurn())).append(",")
                        .append(String.valueOf(javaClass.getAverageHolydays())).append(",")
                        .append(javaClass.isBuggy() ? "YES" : "NO")
                        .append("\n");
            }
        }
    }

    // Crea il file ARFF per il training set
    private void writeArffTraining(String filename, int i) throws IOException {
        String[] parts = filename.split("\\.");
        try (FileWriter fileWriter = new FileWriter(filename)) {
            fileWriter.append("@relation " + parts[0]).append("\n\n")
                    .append("@attribute LOC numeric").append("\n")
                    .append("@attribute LOC_touched numeric").append("\n")
                    .append("@attribute NR numeric").append("\n")
                    .append("@attribute NFix numeric").append("\n")
                    .append("@attribute NAuth numeric").append("\n")
                    .append("@attribute LOC_added numeric").append("\n")
                    .append("@attribute MAX_LOC_added numeric").append("\n")
                    .append("@attribute Churn numeric").append("\n")
                    .append("@attribute MAX_Churn numeric").append("\n")
                    .append("@attribute AVG_Churn numeric").append("\n")
                    .append("@attribute AVG_Holiday numeric").append("\n")
                    .append("@attribute Buggy {'YES', 'NO'}").append("\n\n")
                    .append("@data").append("\n");
            for (int j = 0; j < i; j++) {
                for (Class javaClass : releases.get(j).getAllClasses().values()) {
                    fileWriter.append(String.valueOf(javaClass.getLoc())).append(",")
                            .append(String.valueOf(javaClass.getLocTouched())).append(",")
                            .append(String.valueOf(javaClass.getNr())).append(",")
                            .append(String.valueOf(javaClass.getNumFix())).append(",")
                            .append(String.valueOf(javaClass.getNumAuth())).append(",")
                            .append(String.valueOf(javaClass.getLocAdded())).append(",")
                            .append(String.valueOf(javaClass.getMaxLocAdded())).append(",")
                            .append(String.valueOf(javaClass.getChurn())).append(",")
                            .append(String.valueOf(javaClass.getMaxChurn())).append(",")
                            .append(String.valueOf(javaClass.getAverageChurn())).append(",")
                            .append(String.valueOf(javaClass.getAverageHolydays())).append(",")
                            .append(javaClass.isBuggy() ? "YES" : "NO")
                            .append("\n");
                }
            }
        }
    }

    // Crea il file CSV per l'intero dataset
    public void printDataset(String projName) throws IOException, GitAPIException {
        setBuggyClasses();
        String outname = projName + "//dataset_" + projName + ".csv";
        try (FileWriter fileWriter = new FileWriter(outname)) {
            fileWriter.append("Index,Class,LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,Churn,MAX_Churn,AVG_Churn,AVG_Holiday,Buggy")
                    .append("\n");
            for (Release release : releases) {
                if (release.getAllClasses() != null) {
                    for (Class javaClass : release.getAllClasses().values()) {
                        fileWriter.append(String.valueOf(javaClass.getVersion())).append(",")
                                .append(javaClass.getPath()).append(",")
                                .append(String.valueOf(javaClass.getLoc())).append(",")
                                .append(String.valueOf(javaClass.getLocTouched())).append(",")
                                .append(String.valueOf(javaClass.getNr())).append(",")
                                .append(String.valueOf(javaClass.getNumFix())).append(",")
                                .append(String.valueOf(javaClass.getNumAuth())).append(",")
                                .append(String.valueOf(javaClass.getLocAdded())).append(",")
                                .append(String.valueOf(javaClass.getMaxLocAdded())).append(",")
                                .append(String.valueOf(javaClass.getChurn())).append(",")
                                .append(String.valueOf(javaClass.getMaxChurn())).append(",")
                                .append(String.valueOf(javaClass.getAverageChurn())).append(",")
                                .append(String.valueOf(javaClass.getAverageHolydays())).append(",")
                                .append(javaClass.isBuggy() ? "YES" : "NO")
                                .append("\n");
                    }
                }
            }
        }
    }
}