package org.cloudbus.cloudsim.examples.custom.Dvfs;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;


import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.examples.custom.UtilizationModels.UtilizationModelBurst;

import java.util.Calendar;

public class Dvfs_UtilizationModelBurst {

    public static void main(String[] args) {
        try {
            // Initialize CloudSim
            CloudSim.init(1, Calendar.getInstance(), false);

            // 1. Define the Power Models to compare
            // Max Power: 250W, Static/Idle Power: 125W (50%)
            PowerModelLinear pmLinear = new PowerModelLinear(250, 0.5);

            // 2. Initialize your custom Burst Model
            // Base load: 20%, Peak load: 100%
            // Interval: Every 100 seconds, Peak Duration: 30 seconds
            UtilizationModelBurst burstModel = new UtilizationModelBurst(0.20, 1.00, 100, 30);

            // 3. Generate Timeline Data for Graphing
            System.out.println("========== DVFS BURST SCENARIO TIMELINE ==========");
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