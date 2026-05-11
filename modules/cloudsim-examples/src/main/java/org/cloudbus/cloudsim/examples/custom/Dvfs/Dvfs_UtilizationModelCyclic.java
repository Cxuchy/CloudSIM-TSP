package org.cloudbus.cloudsim.examples.custom.Dvfs;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.custom.UtilizationModels.UtilizationModelBurst;
import org.cloudbus.cloudsim.examples.custom.UtilizationModels.UtilizationModelCyclic;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;

import java.util.Calendar;

public class Dvfs_UtilizationModelCyclic {

    public static void main(String[] args) {
        try {
            // Initialize CloudSim
            CloudSim.init(1, Calendar.getInstance(), false);

            // 1. Define the Power Models to compare
            // Max Power: 250W, Static/Idle Power: 125W (50%)
            PowerModelLinear pmLinear = new PowerModelLinear(250, 0.5);

            // 2. Initialize your custom Cyclic
            // min load: 10%, Peak load: 90%
            // Period of 100
            UtilizationModelCyclic burstModel = new UtilizationModelCyclic(0.1, 0.9, 100);

            // 3. Generate Timeline Data for Graphing
            System.out.println("========== DVFS CYCLIC SCENARIO TIMELINE ==========");
            System.out.println("Time(s), CPU_Load(%), Power(W)");

            // Simulate 250 seconds of time passing, sampling every 10 seconds
            for (int time = 0; time <= 250; time += 10) {
                // Get the CPU load at this specific second
                double currentLoad = burstModel.getUtilization(time);

                // Calculate power for both models at this load
                double linearPower = pmLinear.getPower(currentLoad);

                // Print as CSV format
                System.out.printf("%d, %.0f, %.2f%n",
                        time, (currentLoad * 100), linearPower);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}