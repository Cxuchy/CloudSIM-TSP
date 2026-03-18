package org.cloudbus.cloudsim.examples.custom;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

// Importations JFreeChart pour les graphiques en bâtons
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class CC3_Scheduling_VM_SIZE {

    public static DatacenterBroker broker;
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public static void main(String[] args) {

        Log.println("Démarrage du test Semaine 4 : Variation des paramètres et Graphiques...");

        int[] vmCounts = {1, 2, 4};
        String[] policies = {"TimeShared", "SpaceShared"};

        int numCloudlets = 8;
        long cloudletLength = 400000;

        // Préparation des jeux de données pour les graphiques
        DefaultCategoryDataset makespanDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset avgTimeDataset = new DefaultCategoryDataset();

        System.out.println("\n=========================================================================================");
        System.out.printf("%-10s | %-15s | %-12s | %-15s | %-20s\n",
                "Nb VMs", "Politique", "Makespan (s)", "Temps Moyen (s)", "Utilisation CPU Est.");
        System.out.println("=========================================================================================");

        try {
            for (String policyName : policies) {
                for (int numVms : vmCounts) {

                    CloudSim.init(1, Calendar.getInstance(), false);

                    Datacenter datacenter0 = createDatacenter("Datacenter_0", 8);

                    broker = new DatacenterBroker("Broker");
                    int brokerId = broker.getId();

                    vmlist = new ArrayList<>();
                    int mips = 1000;
                    for (int i = 0; i < numVms; i++) {
                        CloudletScheduler scheduler = policyName.equals("TimeShared") ?
                                new CloudletSchedulerTimeShared() : new CloudletSchedulerSpaceShared();

                        Vm vm = new Vm(i, brokerId, mips, 1, 512, 1000, 10000, "Xen", scheduler);
                        vmlist.add(vm);
                    }
                    broker.submitGuestList(vmlist);

                    cloudletList = new ArrayList<>();
                    UtilizationModel utilizationModel = new UtilizationModelFull();

                    for (int id = 0; id < numCloudlets; id++) {
                        Cloudlet cloudlet = new Cloudlet(id, cloudletLength, 1, 300, 300,
                                utilizationModel, utilizationModel, utilizationModel);
                        cloudlet.setUserId(brokerId);
                        cloudletList.add(cloudlet);
                    }
                    broker.submitCloudletList(cloudletList);

                    CloudSim.startSimulation();
                    CloudSim.stopSimulation();

                    List<Cloudlet> newList = broker.getCloudletReceivedList();

                    double makespan = 0;
                    double totalCompletionTime = 0;

                    for (Cloudlet c : newList) {
                        makespan = Math.max(makespan, c.getFinishTime());
                        totalCompletionTime += (c.getFinishTime() - c.getExecStartTime());
                    }

                    double avgCompletionTime = totalCompletionTime / numCloudlets;
                    double totalWorkMI = numCloudlets * cloudletLength;
                    double totalCapacityMips = numVms * mips;
                    double cpuUtilizationEst = (totalWorkMI / (totalCapacityMips * makespan)) * 100;

                    System.out.printf("%-10d | %-15s | %-12.2f | %-15.2f | %-18.2f %%\n",
                            numVms, policyName, makespan, avgCompletionTime, cpuUtilizationEst);

                    // AJOUT DES RÉSULTATS DANS LES DATASETS POUR LES GRAPHIQUES
                    String category = numVms + " VM(s)";
                    makespanDataset.addValue(makespan, policyName, category);
                    avgTimeDataset.addValue(avgCompletionTime, policyName, category);
                }
            }
            System.out.println("=========================================================================================\n");

            // GÉNÉRATION DES GRAPHIQUES À LA FIN
            plotBarChart(makespanDataset, "Impact du Nombre de VMs sur le Makespan", "Nombre de VMs", "Makespan (secondes)");
            plotBarChart(avgTimeDataset, "Impact de la Politique sur le Temps de Complétion Moyen", "Nombre de VMs", "Temps Moyen (secondes)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name, int numPes) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;

        for (int i = 0; i < numPes; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(mips)));
        }

        hostList.add(new Host(
                0,
                new RamProvisionerSimple(4096),
                new BwProvisionerSimple(10000),
                1000000,
                peList,
                new VmSchedulerTimeShared(peList)
        ));

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0);

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // NOUVELLE MÉTHODE POUR CRÉER LES GRAPHIQUES EN BÂTONS
    private static void plotBarChart(DefaultCategoryDataset dataset, String title, String xAxisLabel, String yAxisLabel) {
        JFreeChart chart = ChartFactory.createBarChart(
                title,                  // Titre du graphique
                xAxisLabel,             // Étiquette de l'axe X
                yAxisLabel,             // Étiquette de l'axe Y
                dataset,                // Données
                PlotOrientation.VERTICAL,
                true,                   // Inclure la légende
                true,                   // Tooltips (bulles d'info)
                false                   // URLs
        );

        ChartFrame frame = new ChartFrame(title, chart);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }
}