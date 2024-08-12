package com.isw2data.controller;


import com.isw2data.enumeration.CostSensitive;
import com.isw2data.enumeration.FeatureSelection;
import com.isw2data.model.ClassifierEvaluation;
import com.isw2data.enumeration.Sampling;
import com.isw2data.enumeration.ClassifierType;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.instance.Resample;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static java.lang.System.*;


public class WekaController {
    // List to store evaluation results
    private List<ClassifierEvaluation> evaluations = new ArrayList<>();
    private final String FilePath = "%s//%s//%s_%d_%s.arff";

    // Evaluate a project with multiple classifiers over several releases
    public void evaluateProject(String projectName, int numReleases) throws Exception {
        for (ClassifierType classifierType : ClassifierType.values()) {
            crossForwardEvaluations(projectName, classifierType.name(), numReleases);
        }
        printEvaluationsToCsv(projectName);
    }

    public void crossForwardEvaluations(String projectName, String classifierName, int numReleases) throws Exception {

        Classifier classifier = switch (classifierName) {

            case "NAIVE_BAYES" -> new NaiveBayes();

            case "IBK" -> new IBk();

            default -> new RandomForest();
        };

        out.println("Evaluation for "+classifierName);
        //nell'i-esima iterazione del walkForward, il training test contiene fino alla release i, il testing set è costituito dalla release i + 1
        for (int i = 2; i < numReleases; i++) {
            //simple dataset with no feature selection, no balancing
            String trainingSet= String.format(FilePath, projectName, "training","training",i,projectName);
            String testingSet = String.format(FilePath, projectName, "testing","testing",i,projectName);
            Instances training = ConverterUtils.DataSource.read(trainingSet);
            Instances testing = ConverterUtils.DataSource.read(testingSet);

            int numAttr = training.numAttributes();
            training.setClassIndex(numAttr - 1);
            testing.setClassIndex(numAttr - 1);

            classifier.buildClassifier(training);
            Evaluation eval = new Evaluation(testing);
            eval.evaluateModel(classifier, testing);
            ClassifierEvaluation evaluation = new ClassifierEvaluation(projectName, i, eval ,classifierName, FeatureSelection.NONE, Sampling.NONE, CostSensitive.NONE);
            evaluations.add(evaluation);


            // Esegui la selezione delle caratteristiche
            AttributeSelection featureSelection = new AttributeSelection();
            CfsSubsetEval cfsEvaluator = new CfsSubsetEval(); // Usando CfsSubsetEval
            GreedyStepwise search = new GreedyStepwise();

            search.setSearchBackwards(false);
            featureSelection.setEvaluator(cfsEvaluator);
            featureSelection.setSearch(search);
            // Applica la selezione delle caratteristiche al set di training
            featureSelection.SelectAttributes(training);
            Instances reducedTraining = featureSelection.reduceDimensionality(training);

            // Applica la selezione delle caratteristiche al set di testing
            Instances reducedTesting = featureSelection.reduceDimensionality(testing);

            // Costruisci e valuta il modello con selezione delle caratteristiche
            classifier.buildClassifier(reducedTraining);
            Evaluation evalWithFS = new Evaluation(reducedTraining);
            evalWithFS.evaluateModel(classifier, reducedTesting);

            // Salva i risultati della valutazione con feature selection specifica
            ClassifierEvaluation evaluationWithFS = new ClassifierEvaluation(projectName, i, evalWithFS, classifierName,
                    FeatureSelection.BESTFIRST, Sampling.NONE, CostSensitive.NONE);
            evaluations.add(evaluationWithFS);

            // Applicare oversampling senza selezione delle caratteristiche
            SMOTE smote = new SMOTE();
            // Applica SMOTE al set di training
            smote.setInputFormat(training);
            Instances smoteTraining = Filter.useFilter(training, smote);

            // Costruisci e valuta il modello con oversampling senza selezione delle caratteristiche
            classifier.buildClassifier(smoteTraining);
            Evaluation evalWithSMOTE = new Evaluation(smoteTraining);
            evalWithSMOTE.evaluateModel(classifier, testing);
            ClassifierEvaluation evaluationWithSMOTE = new ClassifierEvaluation(projectName, i, evalWithSMOTE, classifierName,
                    FeatureSelection.NONE, Sampling.SMOTE, CostSensitive.NONE);
            evaluations.add(evaluationWithSMOTE);

            // Applicare oversampling senza selezione delle caratteristiche
            Resample resample = new Resample();
            resample.setInputFormat(training);
            resample.setNoReplacement(false);
            resample.setSampleSizePercent(200);

            FilteredClassifier fcOver = new FilteredClassifier();
            fcOver.setClassifier(classifier);
            fcOver.setFilter(resample);
            fcOver.buildClassifier(training);

            Evaluation overSamplingEval = new Evaluation(testing);
            overSamplingEval.evaluateModel(fcOver, testing);

            ClassifierEvaluation evaluationWithOverSampling = new ClassifierEvaluation(projectName, i, overSamplingEval, classifierName, FeatureSelection.NONE, Sampling.OVERSAMPLING, CostSensitive.NONE);
            evaluations.add(evaluationWithOverSampling);

            // Applicare undersampling senza selezione delle caratteristiche
            SpreadSubsample spreadSubsample = new SpreadSubsample();
            spreadSubsample.setOptions(new String[]{"-M", "1.0"}); // Bilancia le classi

            FilteredClassifier fcUnder = new FilteredClassifier();
            fcUnder.setClassifier(classifier);
            fcUnder.setFilter(spreadSubsample);
            fcUnder.buildClassifier(training);

            Evaluation underSamplingEval = new Evaluation(testing);
            underSamplingEval.evaluateModel(fcUnder, testing);

            ClassifierEvaluation evaluationWithUnderSampling = new ClassifierEvaluation(projectName, i, underSamplingEval, classifierName, FeatureSelection.NONE, Sampling.UNDERSAMPLING, CostSensitive.NONE);
            evaluations.add(evaluationWithUnderSampling);

            CostSensitiveClassifier c1 = new CostSensitiveClassifier();
            c1.setClassifier(classifier);
            c1.setCostMatrix(createCostMatrix(1, 10)); // Creare una matrice dei costi, dove puoi specificare i costi per FP e FN
            c1.setMinimizeExpectedCost(true); // Minimizza il costo atteso
            c1.buildClassifier(training);

            Evaluation ec1 = new Evaluation(testing, c1.getCostMatrix());
            ec1.evaluateModel(c1, testing);

            ClassifierEvaluation evaluationWithCostSensitive = new ClassifierEvaluation(projectName, i, ec1, classifierName, FeatureSelection.NONE, Sampling.NONE, CostSensitive.COST_SENSITIVE_CLASSIFIER);
            evaluationWithCostSensitive.setCost(1,10);
            evaluations.add(evaluationWithCostSensitive);
        }
    }

    private CostMatrix createCostMatrix(double weightFalsePositive, double
            weightFalseNegative) {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, weightFalsePositive);
        costMatrix.setCell(0, 1, weightFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }

    public static void printProbabilities(String projectName, int numReleases) throws Exception {
        Classifier classifier = new RandomForest();
        String trainingSet= String.format(FilePath, projectName, "training","training",(numReleases-1),projectName);
        String testingSet = String.format(FilePath, projectName, "testing","testing",(numReleases-1),projectName);
        Instances training = ConverterUtils.DataSource.read(trainingSet);
        Instances testing = ConverterUtils.DataSource.read(testingSet);

        int numAttr = training.numAttributes();
        training.setClassIndex(numAttr - 1);
        testing.setClassIndex(numAttr - 1);

        int numtesting = testing.numInstances();
        out.println("There are " + numtesting + " test instances");

        classifier.buildClassifier(training);

        // Loop over each test instance.
        for (int i = 0; i < numtesting; i++) {
            // Get the true class label from the instance's own classIndex.
            //ritorna il valore che l'istanza i-esima ha nel test set
            String trueClassLabel =
                    testing.instance(i).toString(testing.classIndex());

            // Make the prediction here.
            double predictionIndex =
                    classifier.classifyInstance(testing.instance(i));

            // Get the predicted class label from the predictionIndex.
            String predictedClassLabel =
                    testing.classAttribute().value((int) predictionIndex);

            // Get the prediction probability distribution.
            //ritorna la probabilità che l'instanza i-esima appartenga a ciascuna delle classi possibili (in questo caso 2)
            double[] predictionDistribution =
                    classifier.distributionForInstance(testing.instance(i));

            // Print out the true label, predicted label, and the distribution.
            out.printf("%5d: true=%-10s, predicted=%-10s, distribution=",
                    i, trueClassLabel, predictedClassLabel);

            // Loop over all the prediction labels in the distribution.
            //in questo caso abbiamo due possibili classi quindi due probabilità
            for (int predictionDistributionIndex = 0;
                 predictionDistributionIndex < predictionDistribution.length;
                 predictionDistributionIndex++) {
                // Get this distribution index's class label.
                String predictionDistributionIndexAsClassLabel =
                        testing.classAttribute().value(
                                predictionDistributionIndex);

                // Get the probability.
                double predictionProbability =
                        predictionDistribution[predictionDistributionIndex];

                out.printf(" [%10s : %6.3f] ",
                        predictionDistributionIndexAsClassLabel,
                        predictionProbability);
            }
        }
    }

    public void printEvaluationsToCsv(String projName) throws IOException {
        FileWriter fileWriter = null;
        //Name of CSV for output
        String outname = projName + "//evaluations"+projName+".csv";
        try {
            fileWriter = new FileWriter(outname);
            fileWriter.append("Dataset, #TrainingReleases, Classifier, FeatureSelection, Balancing, CostSensitive, TruePositive, FalsePositive, TrueNegative, FalseNegative, Cost, Precision, Recall, AUC, Kappa, FMeasure");
            fileWriter.append("\n");
            for (ClassifierEvaluation evaluation : evaluations) {
                fileWriter.append(evaluation.getProjName());
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getNumTrainingReleases()));
                fileWriter.append(",");
                fileWriter.append(evaluation.getClassifier());
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getFeatureSelection()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getSampling()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getCostSensitive()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getTp()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getFp()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getTn()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getFn()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getCost()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getPrecision()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getRecall()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getAuc()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getKappa()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getF1()));
                fileWriter.append("\n");
            }
        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }

}
