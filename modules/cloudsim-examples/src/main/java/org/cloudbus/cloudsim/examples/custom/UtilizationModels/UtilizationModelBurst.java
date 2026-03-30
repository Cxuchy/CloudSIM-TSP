package org.cloudbus.cloudsim.examples.custom.UtilizationModels;

import org.cloudbus.cloudsim.UtilizationModel;

public class UtilizationModelBurst implements UtilizationModel {
    private double chargeDeBase;
    private double chargeEnPic;
    private double intervallePic;
    private double dureePic;

    /**
     * @param base Charge CPU normale et basse (ex: 0.20)
     * @param pic La charge CPU pendant le pic (ex: 1.00 pour 100%)
     * @param intervalle Le temps écoulé entre le début de chaque pic
     * @param duree Combien de temps dure le pic d'activité
     */
    public UtilizationModelBurst(double base, double pic, double intervalle, double duree) {
        this.chargeDeBase = base;
        this.chargeEnPic = pic;
        this.intervallePic = intervalle;
        this.dureePic = duree;
    }

    @Override
    public double getUtilization(double time) {
        // L'opérateur modulo (%) permet de créer une boucle répétitive dans le temps
        if (time % intervallePic < dureePic) {
            return chargeEnPic; // Nous sommes dans la fenêtre de pic d'activité
        }
        return chargeDeBase; // Fonctionnement normal
    }
}
