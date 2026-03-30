package org.cloudbus.cloudsim.examples.custom.UtilizationModels;

import org.cloudbus.cloudsim.UtilizationModel;

public class UtilizationModelCyclic implements UtilizationModel {
    private double minUtilisation;
    private double maxUtilisation;
    private double periode;

    /**
     * @param min Charge CPU minimale (ex: 0.10 pour 10% pendant la nuit)
     * @param max Charge CPU maximale (ex: 0.90 pour 90% pendant la journée)
     * @param periode Le temps nécessaire (en ticks de simulation) pour faire un cycle complet
     */
    public UtilizationModelCyclic(double min, double max, double periode) {
        this.minUtilisation = min;
        this.maxUtilisation = max;
        this.periode = periode;
    }

    @Override
    public double getUtilization(double time) {
        // Calcul d'une onde sinusoïdale ajustée entre les valeurs min et max
        double amplitude = (maxUtilisation - minUtilisation) / 2.0;
        double decalageVertical = minUtilisation + amplitude;

        return amplitude * Math.sin((2 * Math.PI / periode) * time) + decalageVertical;
    }
}