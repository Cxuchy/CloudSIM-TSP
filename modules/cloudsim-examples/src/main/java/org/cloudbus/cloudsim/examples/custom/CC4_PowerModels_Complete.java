package org.cloudbus.cloudsim.examples.custom;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.power.models.PowerModelSquare;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class CC4_PowerModels_Complete {

    public static void main(String[] args) {
        try {
            // 1. Initialisation de CloudSim
            CloudSim.init(1, Calendar.getInstance(), false);

            // 2. Création des Hôtes avec différents modèles (Linear, Square, Cubic)
            List<Host> hostList = new ArrayList<>();
            int ram = 4096; long storage = 1000000; int bw = 100000; int mips = 1000;

            // Paramètres : (Max Power, Static Power %)
            hostList.add(createPowerHost(1, ram, bw, storage, mips, new PowerModelLinear(100, 0.30)));
            hostList.add(createPowerHost(2, ram, bw, storage, mips, new PowerModelSquare(150, 0.30)));
            hostList.add(createPowerHost(3, ram, bw, storage, mips, new PowerModelCubic(200, 0.30)));

            // 3. Infrastructure (Datacenter & Broker)
            Datacenter datacenter = createDatacenter("Energy_Datacenter", hostList);
            DatacenterBroker broker = createBroker();

            // 4. Lancement de la Simulation (Court pour l'exemple)
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // --- ANALYSE DES DONNÉES ET GRAPHIQUES ---

            // Listes pour les graphiques
            List<Double> cpuLoads = new ArrayList<>();
            List<Double> timeMinutes = new ArrayList<>();

            List<Double> linearPower = new ArrayList<>();
            List<Double> squarePower = new ArrayList<>();
            List<Double> cubicPower = new ArrayList<>();

            List<Double> linearKWh = new ArrayList<>();
            List<Double> squareKWh = new ArrayList<>();
            List<Double> cubicKWh = new ArrayList<>();

            double cumEnergyLin = 0, cumEnergySq = 0, cumEnergyCub = 0;
            double sampleIntervalSec = 60; // 1 minute
            DecimalFormat df = new DecimalFormat("#.####");

            System.out.println("Génération des données d'analyse...");

            for (double load = 0.0; load <= 1.05; load += 0.05) {
                double currentLoad = Math.min(load, 1.0);

                // Calcul de la puissance instantanée (Watts)
                double p1 = ((PowerHost) hostList.get(0)).getPowerModel().getPower(currentLoad);
                double p2 = ((PowerHost) hostList.get(1)).getPowerModel().getPower(currentLoad);
                double p3 = ((PowerHost) hostList.get(2)).getPowerModel().getPower(currentLoad);

                cpuLoads.add(currentLoad * 100);
                linearPower.add(p1);
                squarePower.add(p2);
                cubicPower.add(p3);

                // Calcul de l'énergie cumulée (kWh) sur une heure simulée
                // Formule : (Puissance * Temps_en_secondes) / 3,600,000
                cumEnergyLin += (p1 * sampleIntervalSec) / 3600000.0;
                cumEnergySq  += (p2 * sampleIntervalSec) / 3600000.0;
                cumEnergyCub += (p3 * sampleIntervalSec) / 3600000.0;

                timeMinutes.add(load * 60); // On simule le passage du temps proportionnel à la charge
                linearKWh.add(cumEnergyLin);
                squareKWh.add(cumEnergySq);
                cubicKWh.add(cumEnergyCub);
            }

            // --- AFFICHAGE DES GRAPHIQUES ---

            // Graphique 1 : Modèles de Puissance (W vs %)
            XYChart powerChart = new XYChartBuilder().width(600).height(400).title("Modèles de Puissance").xAxisTitle("Charge CPU (%)").yAxisTitle("Puissance (Watts)").build();
            powerChart.addSeries("Linear", cpuLoads, linearPower).setMarker(SeriesMarkers.NONE);
            powerChart.addSeries("Square", cpuLoads, squarePower).setMarker(SeriesMarkers.NONE);
            powerChart.addSeries("Cubic", cpuLoads, cubicPower).setMarker(SeriesMarkers.NONE);
            new SwingWrapper<>(powerChart).displayChart();

            // Graphique 2 : Énergie Cumulée (kWh vs Temps)
            XYChart energyChart = new XYChartBuilder().width(600).height(400).title("Consommation Totale (kWh)").xAxisTitle("Temps (Minutes)").yAxisTitle("Énergie Cumulée (kWh)").build();
            energyChart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
            energyChart.addSeries("Host Linear", timeMinutes, linearKWh);
            energyChart.addSeries("Host Square", timeMinutes, squareKWh);
            energyChart.addSeries("Host Cubic", timeMinutes, cubicKWh);
            new SwingWrapper<>(energyChart).displayChart();

            System.out.println("\nSimulation terminée avec succès.");
            System.out.println("Énergie finale Linear : " + df.format(cumEnergyLin) + " kWh");
            System.out.println("Énergie finale Cubic  : " + df.format(cumEnergyCub) + " kWh");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- MÉTHODES UTILITAIRES ---

    private static PowerHost createPowerHost(int id, int ram, long bw, long storage, int mips, PowerModel powerModel) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(new PeProvisionerSimple(mips))); // Dual Core
        peList.add(new Pe(new PeProvisionerSimple(mips)));
        return new PowerHost(id, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList), powerModel);
    }

    private static Datacenter createDatacenter(String name, List<Host> hostList) throws Exception {
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics("x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0);
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
    }

    private static DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("Broker");
    }
}