package com.isw2data.model;

import com.isw2data.enumeration.CostSensitive;
import com.isw2data.enumeration.FeatureSelection;
import com.isw2data.enumeration.Sampling;
import weka.classifiers.Evaluation;

public class ClassifierEvaluation {
    private String projName;
    private int numTrainingReleases;
    private String classifier;
    private int tp;
    private int fp;
    private int tn;
    private int fn;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;
    private double f1;
    private double cost;
    private FeatureSelection featureSelection;
    private Sampling sampling;
    private CostSensitive costSensitive;


    public ClassifierEvaluation(String project, int numRelease, Evaluation evaluation, String classifier, FeatureSelection featureSelection, Sampling sampling, CostSensitive costSensitive, double costFP, double costFN) {
        this.projName = project;
        this.numTrainingReleases = numRelease;
        this.classifier = classifier;
        this.featureSelection = featureSelection;
        this.sampling = sampling;
        this.costSensitive = costSensitive;
        this.precision = evaluation.precision(0);
        this.recall = evaluation.recall(0);
        this.auc = evaluation.areaUnderROC(0);
        this.kappa = evaluation.kappa();
        this.tp = (int) evaluation.numTruePositives(0);
        this.fp = (int) evaluation.numFalsePositives(0);
        this.tn = (int) evaluation.numTrueNegatives(0);
        this.fn = (int) evaluation.numFalseNegatives(0);
        this.cost = fp*costFP + fn*costFN;
        this.f1 = evaluation.fMeasure(0);

    }

    public String getProjName() {
        return projName;
    }

    public int getNumTrainingReleases() {
        return numTrainingReleases;
    }

    public String getClassifier() {
        return classifier;
    }

    public FeatureSelection getFeatureSelection() {
        return featureSelection;
    }

    public Sampling getSampling() {
        return sampling;
    }

    public CostSensitive getCostSensitive() { return costSensitive; }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getAuc() {
        return auc;
    }

    public double getKappa() {
        return kappa;
    }

    public int getTp() {
        return tp;
    }

    public int getFp() {
        return fp;
    }

    public int getTn() {
        return tn;
    }

    public int getFn() { return fn; }

    public double getCost() { return cost; }

    public double getF1() {
        return f1;
    }
}