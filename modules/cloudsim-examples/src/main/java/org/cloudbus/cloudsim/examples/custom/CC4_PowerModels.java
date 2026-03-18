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

// Imports pour le graphique XChart
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

public class CC4_PowerModels {

    public static void main(String[] args) {
        try {
            // ... (Initialisation CloudSim identique) ...
            CloudSim.init(1, Calendar.getInstance(), false);

            List<Host> hostList = new ArrayList<Host>();
            int ram = 4096; long storage = 1000000; int bw = 100000; int mips = 1000;

            hostList.add(createPowerHost(1, ram, bw, storage, mips, new PowerModelLinear(100, 0.30)));
            hostList.add(createPowerHost(2, ram, bw, storage, mips, new PowerModelSquare(150, 0.30)));
            hostList.add(createPowerHost(3, ram, bw, storage, mips, new PowerModelCubic(200, 0.30)));

            Datacenter datacenter = createDatacenter("Energy_Datacenter", hostList);
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            List<Vm> vmlist = new ArrayList<Vm>();
            List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();

            for (int i = 0; i < 3; i++) {
                vmlist.add(new Vm(i, brokerId, mips, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerTimeShared()));
                UtilizationModel utilizationModel = new UtilizationModelStochastic();
                Cloudlet cloudlet = new Cloudlet(i, 40000, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudlet.setGuestId(i);
                cloudletList.add(cloudlet);
            }

            broker.submitGuestList(vmlist);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // --- PRÉPARATION DES DONNÉES POUR LE GRAPHIQUE ---
            System.out.println("\n--- DONNÉES POUR COURBES : CHARGE CPU vs PUISSANCE (W) ---");

            List<Double> cpuLoads = new ArrayList<>();
            List<Double> linearPower = new ArrayList<>();
            List<Double> squarePower = new ArrayList<>();
            List<Double> cubicPower = new ArrayList<>();

            DecimalFormat df = new DecimalFormat("#.##");

            for (double load = 0.0; load <= 1.0; load += 0.05) { // Pas de 5% pour une courbe plus lisse
                double currentLoad = Math.min(load, 1.0);
                double p1 = ((PowerHost) hostList.get(0)).getPowerModel().getPower(currentLoad);
                double p2 = ((PowerHost) hostList.get(1)).getPowerModel().getPower(load);
                double p3 = ((PowerHost) hostList.get(2)).getPowerModel().getPower(load);

                // Ajout aux listes pour le graphique
                cpuLoads.add(currentLoad * 100); // En pourcentage
                linearPower.add(p1);
                squarePower.add(p2);
                cubicPower.add(p3);

                System.out.println(df.format(currentLoad * 100) + "%, " + df.format(p1) + "W, " + df.format(p2) + "W, " + df.format(p3) + "W");
            }

            // --- CRÉATION ET AFFICHAGE DU GRAPHIQUE ---
            displayChart(cpuLoads, linearPower, squarePower, cubicPower);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode pour générer le graphique
    private static void displayChart(List<Double> xData, List<Double> linear, List<Double> square, List<Double> cubic) {
        // Création du graphique
        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title("Consommation Énergétique selon la Charge CPU")
                .xAxisTitle("Charge CPU (%)")
                .yAxisTitle("Puissance Consommée (Watts)")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setChartTitleVisible(true);
        chart.getStyler().setMarkerSize(8);

        chart.addSeries("Modèle Linéaire (100W Max)", xData, linear).setMarker(SeriesMarkers.CIRCLE);
        chart.addSeries("Modèle Quadratique (150W Max)", xData, square).setMarker(SeriesMarkers.SQUARE);
        chart.addSeries("Modèle Cubique (200W Max)", xData, cubic).setMarker(SeriesMarkers.DIAMOND);

        new SwingWrapper<>(chart).displayChart();
    }

    private static PowerHost createPowerHost(int id, int ram, long bw, long storage, int mips, PowerModel powerModel) {
        List<Pe> peList = new ArrayList<Pe>();
        peList.add(new Pe(new PeProvisionerSimple(mips)));
        peList.add(new Pe(new PeProvisionerSimple(mips)));
        return new PowerHost(id, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList), powerModel);
    }

    private static Datacenter createDatacenter(String name, List<Host> hostList) throws Exception {
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics("x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0);
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
    }

    private static DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("Broker");
    }
}