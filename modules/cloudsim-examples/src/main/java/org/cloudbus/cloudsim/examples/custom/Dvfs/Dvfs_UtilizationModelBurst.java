package org.cloudbus.cloudsim.examples.custom.Dvfs;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;

import java.util.ArrayList;
import java.util.Calendar;

import org.cloudbus.cloudsim.examples.custom.UtilizationModels.UtilizationModelBurst;



public class Dvfs_UtilizationModelBurst {

    public static void main(String[] args) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            // Power models : Linear (sans DVFS) vs Cubic (avec DVFS)
            PowerModelLinear pmLinear = new PowerModelLinear(250, 0.5);
            PowerModelCubic  pmCubic  = new PowerModelCubic(250, 0.5);

            // Charge burst : base 20%, pic 100%, toutes les 100s, durée pic 30s
            UtilizationModelBurst burstModel = new UtilizationModelBurst(0.20, 1.00, 100, 30);

            System.out.println("========== ANALYSE COMPARATIVE DVFS (BURST) ==========");
            System.out.println("Time(s), CPU_Load(%), Linear_Power(W), DVFS_Cubic_Power(W), Economy(W)");

            for (int time = 0; time <= 250; time += 10) {
                double currentLoad = burstModel.getUtilization(time);
                double linearPower = pmLinear.getPower(currentLoad);
                double dvfsPower   = pmCubic.getPower(currentLoad);
                double economy     = linearPower - dvfsPower;

                System.out.printf("%d, %.0f, %.2f, %.2f, %.2f%n",
                        time, (currentLoad * 100), linearPower, dvfsPower, economy);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}