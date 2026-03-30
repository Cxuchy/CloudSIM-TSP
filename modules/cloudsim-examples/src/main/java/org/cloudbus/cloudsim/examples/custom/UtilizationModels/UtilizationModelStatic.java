package org.cloudbus.cloudsim.examples.custom.UtilizationModels;

import org.cloudbus.cloudsim.UtilizationModel;

public class UtilizationModelStatic implements UtilizationModel {
    private double niveauUtilisation;

    /**
     * @param niveauUtilisation Le pourcentage constant de CPU utilisé (ex: 0.70 pour 70%)
     */
    public UtilizationModelStatic(double niveauUtilisation) {
        this.niveauUtilisation = niveauUtilisation;
    }

    @Override
    public double getUtilization(double time) {
        return niveauUtilisation; // Retourne toujours la même valeur, peu importe le temps
    }
}
