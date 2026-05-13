package org.cloudbus.cloudsim.examples.custom.Dvfs;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.custom.UtilizationModels.UtilizationModelBurst;
import org.cloudbus.cloudsim.examples.custom.UtilizationModels.UtilizationModelCyclic;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;

import java.util.Calendar;

public class Dvfs_UtilizationModelCyclic {




    public static void main(String[] args) {
        try {
            // Initialisation de CloudSim [cite: 16, 17]
            CloudSim.init(1, Calendar.getInstance(), false);

            // 1. Définition des modèles de puissance [cite: 38]
            // Max Power: 250W, Static/Idle Power: 125W (50%)
            PowerModelLinear pmLinear = new PowerModelLinear(250, 0.5);
            // Le modèle Cubique simule l'efficacité du DVFS (P proportionnel à f³)
            PowerModelCubic pmCubic = new PowerModelCubic(250, 0.5);

            // 2. Initialisation de votre modèle cyclique (Charge logicielle) [cite: 51]
            // min load: 10%, Peak load: 90%, Période: 100s
            UtilizationModelCyclic cyclicModel = new UtilizationModelCyclic(0.1, 0.9, 100);

            // 3. Génération des données pour l'analyse du compromis énergie/performance
            System.out.println("========== ANALYSE COMPARATIVE DVFS (CYCLIC) ==========");
            System.out.println("Time(s), CPU_Load(%), Linear_Power(W), DVFS_Cubic_Power(W), Economy(W)");

            // Simulation de 250 secondes avec un relevé toutes les 10 secondes [cite: 40]
            for (int time = 0; time <= 250; time += 10) {
                // Récupération de la charge à l'instant t
                double currentLoad = cyclicModel.getUtilization(time);

                // Calcul de la puissance pour les deux types de matériel [cite: 38, 40]
                double linearPower = pmLinear.getPower(currentLoad);
                double dvfsPower = pmCubic.getPower(currentLoad);
                double economy = linearPower - dvfsPower;

                // Affichage au format CSV pour vos graphiques Python/Excel [cite: 43, 44]
                System.out.printf("%d, %.0f, %.2f, %.2f, %.2f%n",
                        time, (currentLoad * 100), linearPower, dvfsPower, economy);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}