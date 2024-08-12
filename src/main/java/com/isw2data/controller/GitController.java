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
    private List<Release> releases;  //le releases del repository ordinate per releaseDate crescente
    private List<TicketBug> tickets;    //i ticket su Jira associati al repository
    private static final String CLASS_PATH = ".java";
    private static final String TEST_DIR = "/test/";

    public GitController(Repository repo, List<Release> releases, List<TicketBug> tickets) {
        this.repo = repo;
        git = new Git(repo);
        this.releases = releases;
        this.tickets = tickets;
    }

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



    //recupera tutti i commit di tutti i branch della repository corrente
    public List<RevCommit> getAllCommits() throws IOException {

        Collection<Ref> allRefs = repo.getRefDatabase().getRefs();
        List<RevCommit> allCommits = new ArrayList<>();

        // a RevWalk allows to walk over commits based on some filtering that is defined
        try (RevWalk revWalk = new RevWalk(repo)) {
            for( Ref ref : allRefs ) {
                revWalk.markStart( revWalk.parseCommit( ref.getObjectId() ));
            }
            System.out.println("Walking all commits starting with " + allRefs.size() + " refs: " + allRefs);
            int count = 0;
            for( RevCommit commit : revWalk ) {
                allCommits.add(commit);
                count++;
            }
            System.out.println("Had " + count + " commits");
        }

        return allCommits;
    }

    // use the following instead to list commits on head branch
    private List<RevCommit> getCommitsFromMaster() throws IOException, GitAPIException {

        List<RevCommit> commitsFromHead = new ArrayList<>();
        ObjectId branchId = this.repo.resolve("HEAD");
        Iterable<RevCommit> commits = this.git.log().add(branchId).call();

        for (RevCommit commit : commits) {
            commitsFromHead.add(commit);
        }
        return commitsFromHead;
    }

    //assegna i commit alla release di appartenenza
    private void assignCommitsToReleases(List<RevCommit> commits) {

        for (RevCommit commit : commits) {
            LocalDateTime commitTime = Utils.convertTime(commit.getCommitTime());

            for (int i = 0; i<releases.size(); i++) {
                if (releases.get(i).getReleaseDate().isAfter(commitTime)) {
                    releases.get(i).addCommit(commit);
                    break;
                }
            }
        }
        System.out.println("Settati i commit");

        setLastCommitFromRelease();
        /*
        if (oldestCommit != null && newestCommit != null) {

            midpointDate = Utils.convertTime((oldestCommit.getCommitTime()+((newestCommit.getCommitTime() - oldestCommit.getCommitTime()) / 2)));
            System.out.println("Midpoint Date: " + midpointDate);
            System.out.println("Old: " + Utils.convertTime(oldestCommit.getCommitTime()));
            System.out.println("Last: " + Utils.convertTime(newestCommit.getCommitTime()));

            // Trova l'ultima release prima della data intermedia
            Release lastReleaseBeforeMidpoint = null;
            for (Release release : releases) {
                if (release.getReleaseDate().isBefore(midpointDate)) {
                    lastReleaseBeforeMidpoint = release;
                }
            }
            out.println("PRENDO LE PRIME: "+ lastReleaseBeforeMidpoint.getIndex());
        }
        */
        /*
        // Commit classification
        int sum = 0;
        for (int i = 0; i < releases.size(); i++) {
            List<RevCommit> commitss = releases.get(i).getAllCommits();
            int temp = (commitss != null) ? commitss.size() : 0;
            // Stampa l'indice della release e il numero di commit
            out.println(releases.get(i).getIndex() + " " + temp +" DATA: "+releases.get(i).getReleaseDate() );
            sum += temp;
        }
        out.println("Tot2:  " + sum);
        */
    }

    //essendo i commit ordinati per commitTime decrescente (dal più recente), l'ultimo commit di ogni Releasee è il primo della lista
    private void setLastCommitFromRelease() {
        for (Release release : releases) {
            List<RevCommit> commits = release.getAllCommits();
            if (commits != null) {
                release.setLastCommit(commits.get(0));
            }
        }
    }

    //verifica la presenza di release con zero commit associati, e, se presenti, elimina le release vuote e shifta gli indici delle release
    private void checkEmptyReleases() {
        Iterator<Release> i = releases.iterator();
        while (i.hasNext()) {
            Release currentRelease = i.next();
            if (currentRelease.getAllCommits() == null) {
                System.out.println("Rimossa la release " + currentRelease.getIndex()+" per assenza di commit");
                shiftReleaseIndexes(currentRelease.getIndex());
                i.remove();
            }
        }
    }

    private void shiftReleaseIndexes(int index) {
        for (Release release : releases) {
            if (release.getIndex() > index) release.setIndex(release.getIndex() - 1);
        }
    }

    //assegna a ogni Releasee le classi presenti in quella Releasee (con il corrispettivo stato di queste classi in quella Releasee)
    //in pratica recupera lo stato del repository in una certa Releasee
    private void assignClassesToReleases() throws IOException {
        for (Release release : releases) {
            RevCommit lastCommit = release.getLastCommit();
            if (lastCommit != null) {
                Map<String, Class> relClasses = getClasses(lastCommit, release);
                release.setAllClasses(relClasses);
            }
        }
        System.out.println("Settate le classi");

    }


    //recupera le classi appartenenti al repository al momento del commit indicato
    private Map<String, Class> getClasses(RevCommit commit, Release release) throws IOException {

        //recupera tutti i files presenti nel repository al momento del commit indicato
        RevTree tree = commit.getTree();
        Map<String, Class> allClasses = new HashMap<>();

        //il TreeWalk mi permette di navigare tra questi file
        TreeWalk treeWalk = new TreeWalk(this.repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while(treeWalk.next()) {
            //considera solo le classi java (esludendo le classi di test)
            if (treeWalk.getPathString().contains(CLASS_PATH) && !treeWalk.getPathString().contains(TEST_DIR)) {
                allClasses.put(treeWalk.getPathString(), new Class(treeWalk.getPathString(), new String(this.repo.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8), release));
            }
        }

        treeWalk.close();
        return allClasses;
    }

    private void calculateMetrics() throws GitAPIException, IOException {
        for (Release release : releases) {
            //calcola le metriche (LOC, churn...) per le classi in ogni release
            calculateLoc(release);
            calculateAllLocMetrics(release);
            calculatenAuth(release);
            personalMetrics(release);
        }

    }

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

    //recupera il numero di linee di codice di ogni classe in una certa release
    private void calculateLoc(Release release) {
        for (Class javaClass : release.getAllClasses().values()) {
            String[] lines = javaClass.getContent().split("\r\n|\r|\n");
            javaClass.setLoc(lines.length);
        }
    }

    private void calculateAllLocMetrics(Release release) throws GitAPIException, IOException {
        if (release.getAllCommits() == null) return;

        // Calcola le linee di codice modificate e aggiorna le metriche per ogni commit
        for (RevCommit commit : release.getAllCommits()) updateMetricsForCommit(commit, release);

        // Setta il valor medio di churn per ogni classe
        release.getAllClasses().values().forEach(this::setAverageChurn);
    }

    private void updateMetricsForCommit(RevCommit commit, Release release) throws GitAPIException, IOException {
        if (commit.getParentCount() == 0) return;

        String commitId = commit.getId().getName();
        String parentCommit = commitId + "^";

        List<DiffEntry> diffs = this.git.diff()
                .setOldTree(prepareTreeParser(this.repo, parentCommit))
                .setNewTree(prepareTreeParser(this.repo, commitId))
                .call();

        // Aggiorna le metriche per ogni diff
        for (DiffEntry diff : diffs) {
            updateMetricsForDiff(diff, commit, release);
        }
    }

    private void updateMetricsForDiff(DiffEntry diff, RevCommit commit, Release release) throws IOException {
        if (!diff.getNewPath().contains(CLASS_PATH) || diff.getNewPath().contains(TEST_DIR)) return;

        DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(this.repo);

        String modifiedClassPath = diff.getNewPath();
        Class modifiedClass = release.getAllClasses().get(modifiedClassPath);

        if (modifiedClass == null) {
            return;
        }

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

    private void setAverageChurn(Class javaClass) {
        List<Integer> churnArray = javaClass.getChurnArray();
        if (!churnArray.isEmpty()) {
            float averageChurn = (float) churnArray.stream().mapToInt(Integer::intValue).sum() / churnArray.size();

            javaClass.setAverageChurn(averageChurn);
        }
    }


    //ritorna la lista dei path delle classi modificate dal commit
    private List<String> getClassesFromCommit(RevCommit commit) throws GitAPIException, IOException {
        List<String> modifiedClassPaths = new ArrayList<>();  //path delle classi modificate dal commit
        if (commit.getParentCount() == 0) return modifiedClassPaths;

        String commitId = commit.getId().getName();
        String parentCommit = commitId + "^";

        final List<DiffEntry> diffs = this.git.diff()
                .setOldTree(prepareTreeParser(this.repo, parentCommit))
                .setNewTree(prepareTreeParser(this.repo, commitId))
                .call();

        //recupera la lista delle modifiche effettuate rispetto al commit precedente, ogni modifica è relativa ad un certo file
        for (DiffEntry diff : diffs) {
            if (diff.getNewPath().contains(CLASS_PATH) && !diff.getNewPath().contains(TEST_DIR)) {
                //recupera il path della classe modificata
                String modifiedClassPath = diff.getNewPath();
                modifiedClassPaths.add(modifiedClassPath);
            }
        }

        return modifiedClassPaths;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
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

    private void calculatenAuth(Release release) {
        if (release.getAllClasses() != null) {
            for (Class javaClass : release.getAllClasses().values()) {
                javaClass.setNumAuth(javaClass.getAuthors().size());
            }
        }
    }


    private void setFixCommits() {
        int count = 0;
        int nocount = 0;
        for (TicketBug ticket : tickets) {
            if (assignCommitToTicket(ticket)) {
                count++;
            }
            else{
                nocount++;
            }
        }
        System.out.println("Fix commit: " + count +"\t No fix commit: "+ nocount);
    }


    //assegna il fix commit che riporta l'id del ticket (qualora il commit non fosse presente il ticket viene scartato)
    private boolean assignCommitToTicket(TicketBug fixTicket) {
        boolean found = false;
        for (Release release : releases) {
            if (release.getAllCommits() != null) {
                for (RevCommit fixCommit : release.getAllCommits()) {
                    if (fixCommit.getFullMessage().contains(fixTicket.getKey() + "]") || fixCommit.getFullMessage().contains(fixTicket.getKey() + ":")) {
                        fixTicket.add_fix_commit(fixCommit);
                        found = true;
                    }
                }
            }
        }
        return found;
    }



    private void setBuggyClassesWalkForward(int index) throws GitAPIException, IOException {
        //index rappresenta l'indice della release su cui pongo il punto di osservazione (cioè rispetto a cui etichetto le classi)
        //considera solo il ticket con index della fix <= index Release attuale (cioè esistenti rispetto al punto di osservazione fissato)
        for (TicketBug ticket : tickets) {
            //scarta i ticket che hanno IV = FV, perché siamo interessati solo ai difetti post-release (IV<FV)
            if (ticket.getFixVersion().getIndex() > index || ticket.getInjectedVersion().getId() == ticket.getFixVersion().getId())
                continue;
            //recupera i fix commit associati al ticket
            for (RevCommit fixCommit : ticket.getCommits()) {
                //recupera il path delle classi modificate dal commit (ignorando le classi di test)
                List<String> classPaths = getClassesFromCommit(fixCommit);
                //recupera tutte le Release tra IV (inclusa) e FV(esclusa) indicate sul ticket
                //etichetta come buggy le classi modificate dal commit come in queste Release
                Release injectedRelease = ticket.getInjectedVersion();
                Release fixRelease = ticket.getFixVersion();
                setNumFixes(classPaths, fixRelease);
                if (!classPaths.isEmpty()) {
                    labelClasses(classPaths, injectedRelease, fixRelease);
                }
            }
        }
    }


    public void setBuggyClasses() throws GitAPIException, IOException {
        for (TicketBug ticket : tickets) {
            //scarta i ticket che hanno IV = FV, perchè siamo interessati solo ai difetti post-release (IV<FV)
            if (ticket.getInjectedVersion().getId() == ticket.getFixVersion().getId()) continue;
            //recupera i fix commit associati al ticket
            for (RevCommit fixCommit : ticket.getCommits())  {
                //recupera il path delle classi modificate dal commit (ignorando le classi di test)
                List<String> classPaths = getClassesFromCommit(fixCommit);
                //recupera tutte le Releasei tra IV (inclusa) e FV(esclusa) indicate sul ticket
                //etichetta come buggy le classi modificate dal commit come in queste Releasei
                Release injectedRelease = ticket.getInjectedVersion();
                Release fixRelease = ticket.getFixVersion();
                // setNumFixes(classPaths, fixRelease);
                if (!classPaths.isEmpty()) {
                    labelClasses(classPaths, injectedRelease, fixRelease);
                }
            }
        }
    }



    private void labelClasses(List<String>  classPaths, Release injectedRelease, Release fixRelease) {
        //itera fino all injected Release e si ferma
        for (Release Release : releases) {
            if (Release.getIndex() == fixRelease.getIndex()) break;
            if (Release.getIndex() >= injectedRelease.getIndex() && Release.getIndex() < fixRelease.getIndex()) {
                for (String modifiedClass : classPaths){
                    Class modifclass = Release.getAllClasses().get(modifiedClass);
                    if (modifclass!= null) {
                        //setta la classe come buggy in una certa Release
                        modifclass.setBuggy(true);
                    }
                }
            }
        }
    }

    //incrementa di 1 il numero di fix delle classi associate a un fix commit nella fix Release (conta solo i fix dei difetti post-release)
    private void setNumFixes(List<String> classPaths, Release fixRelease) {
        for (String classPath : classPaths) {
            Class modifiedClass = fixRelease.getAllClasses().get(classPath);
            if (modifiedClass != null) modifiedClass.addFix();
        }
    }

    //per ridurre lo snoring viene rimossa metà delle release (le più recenti)
    private void deleteLastReleases() {
        int numReleases = releases.size();
        releases.removeIf(currentRelease -> currentRelease.getIndex() > numReleases / 2);
        // if (releases.removeIf(currentRelease -> currentRelease.getIndex() == 1)) shiftReleaseIndexes(1);

        //out.println(releases.get(0).getReleaseDate());
        //for (Release diff : releases) {
            //out.println(Utils.convertTime(diff.getLastCommit().getCommitTime()));
        //}

        //out.println(Utils.convertTime(releases.get(releases.size()-1).getLastCommit().getCommitTime()));
        //out.println(releases.get(releases.size()).getLastCommit());

    }

    private void printCsvArffWalkForward(String projName) throws IOException, GitAPIException {
        FileWriter trainingWriterCsv = null;
        FileWriter trainingWriterArff = null;

        FileWriter testingWriterCsv = null;
        FileWriter testingWriterArff = null;

        setBuggyClassesWalkForward(1);

        //per ridurre lo snoring si considerano solo metà delle release
        //crea due csv per ogni release del progetto (a partire dalla seconda release), uno conterrà il set di dati per il training, l'altro per il setting, secondo un approccio walkForward
        for (int i = 2; i < releases.size() / 2; i++) { //l'indice i tiene traccia della creazione di training e testing set usati nell'i-esima iterazione del walkForward
            String trainingName = projName + "//training//" + "training_" + i + "_" + projName+ ".csv";
            String arffName = projName + "//training//" + "training_" + i + "_" + projName+ ".arff";

            setBuggyClassesWalkForward(i);
            printTrainingSet(trainingWriterCsv, trainingName, i);
            writeArffTraining(trainingWriterArff, arffName,i);
        }

        setBuggyClassesWalkForward(releases.size() - 1);
        //il testing set ad ogni iterazione è costruito usando tutte le informazioni disponibili (ponendo il punto di osservazione all'ultima release)
        for (int i = 2; i < releases.size() / 2; i++) {
            String testingName = projName + "//testing//" + "testing_" + i + "_" + projName+ ".csv";
            String arffName = projName + "//testing//" + "testing_" + i + "_" + projName+ ".arff";

            printTestingSet(testingWriterCsv, testingName, i);
            writeArffTesting(testingWriterArff  ,arffName,i);
        }
    }

    private void printTrainingSet(FileWriter trainingWriter, String trainingName, int i) throws IOException {
        try {
            trainingWriter = new FileWriter(trainingName);
            trainingWriter.append("LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,Churn,MAX_Churn,AVG_Churn,AVG_Holiday,Buggy");
            trainingWriter.append("\n");
            for (int j = 0; j < i; j++) {
                for (Class javaClass : releases.get(j).getAllClasses().values()) {
                    trainingWriter.append(String.valueOf(javaClass.getLoc()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getLocTouched()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getNr()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getNumFix()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getNumAuth()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getLocAdded()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getMaxLocAdded()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getChurn()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getMaxChurn()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getAverageChurn()));
                    trainingWriter.append(",");
                    trainingWriter.append(String.valueOf(javaClass.getAverageHolydays()));
                    trainingWriter.append(",");
                    if (javaClass.isBuggy()) trainingWriter.append("YES");
                    else trainingWriter.append("NO");
                    trainingWriter.append("\n");
                }
            }
        } finally {
            if (trainingWriter != null) trainingWriter.close();
        }
    }

    private void printTestingSet(FileWriter testingWriter, String testingName, int i) throws IOException {
        try {
            testingWriter = new FileWriter(testingName);
            testingWriter.append("LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,Churn,MAX_Churn,AVG_Churn,AVG_Holiday,Buggy");
            testingWriter.append("\n");
            for (Class javaClass : releases.get(i).getAllClasses().values()) {
                testingWriter.append(String.valueOf(javaClass.getLoc()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getLocTouched()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getNr()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getNumFix()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getNumAuth()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getLocAdded()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getMaxLocAdded()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getChurn()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getMaxChurn()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getAverageChurn()));
                testingWriter.append(",");
                testingWriter.append(String.valueOf(javaClass.getAverageHolydays()));
                testingWriter.append(",");
                if (javaClass.isBuggy()) testingWriter.append("YES");
                else testingWriter.append("NO");
                testingWriter.append("\n");
            }
            testingWriter.flush();
        } finally {
            if (testingWriter != null) testingWriter.close();
        }
    }


    public void printDataset(String projName) throws IOException, GitAPIException {
        FileWriter fileWriter = null;
        setBuggyClasses();
        //Name of CSV for output
        String outname = projName + "//dataset_" + projName + ".csv";
        try {
            fileWriter = new FileWriter(outname);
            fileWriter.append("Index,Class,LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,Churn,MAX_Churn,AVG_Churn,AVG_Holiday,Buggy");
            fileWriter.append("\n");
            for (Release release : releases) {
                if (release.getAllClasses() != null) {
                    for (Class javaClass : release.getAllClasses().values()) {
                        fileWriter.append(String.valueOf(javaClass.getVersion()));
                        fileWriter.append(",");
                        fileWriter.append(javaClass.getPath());
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getLoc()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getLocTouched()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getNr()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getNumFix()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getNumAuth()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getLocAdded()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getMaxLocAdded()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getChurn()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getMaxChurn()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getAverageChurn()));
                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(javaClass.getAverageHolydays()));
                        fileWriter.append(",");
                        if (javaClass.isBuggy()) fileWriter.append("YES");
                        else fileWriter.append("NO");
                        fileWriter.append("\n");
                    }
                }
            }
            fileWriter.flush();
        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }


    private void writeArffTesting(FileWriter fileWriter, String filename, int i) throws IOException{
        String[] parts = filename.split("\\.");

        //Name of CSV for output
        try {
            fileWriter = new FileWriter(filename);
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
                fileWriter.append(String.valueOf(javaClass.getLoc()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getLocTouched()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getNr()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getNumFix()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getNumAuth()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getLocAdded()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getMaxLocAdded()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getChurn()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getMaxChurn()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getAverageChurn()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(javaClass.getAverageHolydays()));
                fileWriter.append(",");
                if (javaClass.isBuggy()) fileWriter.append("YES");
                else fileWriter.append("NO");
                fileWriter.append("\n");

            }
            fileWriter.flush();
        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }

    private void writeArffTraining(FileWriter fileWriter, String filename, int i) throws IOException{
        String[] parts = filename.split("\\.");

        //Name of CSV for output
        try {
            fileWriter = new FileWriter(filename);

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
                    fileWriter.append(String.valueOf(javaClass.getLoc()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getLocTouched()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getNr()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getNumFix()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getNumAuth()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getLocAdded()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getMaxLocAdded()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getChurn()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getMaxChurn()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getAverageChurn()));
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(javaClass.getAverageHolydays()));
                    fileWriter.append(",");
                    if (javaClass.isBuggy()) fileWriter.append("YES");
                    else fileWriter.append("NO");
                    fileWriter.append("\n");
                }
            }
            fileWriter.flush();
        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }
}
