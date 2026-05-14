package org.cloudbus.cloudsim.examples.custom.Scaling;

import org.cloudbus.cloudsim.core.CloudSim;


import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.*;

import java.text.DecimalFormat;
import java.util.*;

/**
 * HorizontalScalingExample — CloudSim 7G
 *
 * Simule le scaling horizontal : si le nombre de cloudlets dépasse
 * la capacité d'une seule VM, de nouvelles VMs sont créées dynamiquement
 * pour répartir la charge (load balancing).
 *
 * Deux scénarios sont comparés :
 *   - SANS scaling : tous les cloudlets sur 1 seule VM
 *   - AVEC scaling : cloudlets répartis sur N VMs créées dynamiquement
 */
public class HorizontalScalingExample {

    // ──────────────────────────────────────────────
    //  Paramètres de simulation
    // ──────────────────────────────────────────────
    private static final int    NUM_CLOUDLETS        = 12;   // charge totale
    private static final int    MAX_CLOUDLETS_PER_VM = 3;    // seuil par VM
    private static final double SCALE_THRESHOLD      = 0.80; // 80% CPU

    // Ressources VM
    private static final int VM_MIPS    = 1000;
    private static final int VM_PES     = 1;
    private static final int VM_RAM     = 512;   // MB
    private static final long VM_BW     = 1000;
    private static final long VM_SIZE   = 10000; // MB storage

    // Ressources hôte
    private static final int HOST_MIPS    = 10000;
    private static final int HOST_PES     = 8;
    private static final int HOST_RAM     = 8192;
    private static final long HOST_BW     = 10000;
    private static final long HOST_STORAGE = 1000000;

    // Cloudlet
    private static final long CLOUDLET_LENGTH    = 40000; // MI
    private static final long CLOUDLET_FILESIZE  = 300;
    private static final long CLOUDLET_OUTPUTSIZE = 300;
    private static final int  CLOUDLET_PES       = 1;

    private static List<Cloudlet> cloudletList;
    private static List<Vm>       vmList;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   HORIZONTAL SCALING — CloudSim 7G");
        System.out.println("=================================================\n");

        runScenario(false); // Sans scaling
        System.out.println("\n-------------------------------------------------\n");
        runScenario(true);  // Avec scaling
    }

    private static void runScenario(boolean scalingEnabled) {
        System.out.println(">>> Scénario : " +
                (scalingEnabled ? "AVEC Horizontal Scaling" : "SANS Horizontal Scaling (baseline)"));

        try {
            // 1. Initialiser CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUsers, calendar, false);

            // 2. Créer le datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // 3. Créer le broker
            DatacenterBroker broker = new DatacenterBroker("Broker");
            int brokerId = broker.getId();

            // 4. Créer les cloudlets
            cloudletList = createCloudlets(brokerId);

            // 5. Créer les VMs (avec ou sans scaling)
            vmList = new ArrayList<>();

            if (!scalingEnabled) {
                // BASELINE : 1 seule VM pour tous les cloudlets
                Vm vm = createVm(0, brokerId);
                vmList.add(vm);
                System.out.println("    VMs créées initialement : 1");

                broker.submitGuestList(vmList);

                // Tous les cloudlets sur la même VM
                for (Cloudlet cl : cloudletList) {
                    cl.setVmId(0);
                }

            } else {
                // HORIZONTAL SCALING : calculer le nombre de VMs nécessaires
                int vmsNeeded = (int) Math.ceil(
                        (double) NUM_CLOUDLETS / MAX_CLOUDLETS_PER_VM);

                System.out.println("    Cloudlets totaux      : " + NUM_CLOUDLETS);
                System.out.println("    Max cloudlets / VM    : " + MAX_CLOUDLETS_PER_VM);
                System.out.println("    VMs nécessaires       : " + vmsNeeded);

                for (int i = 0; i < vmsNeeded; i++) {
                    Vm vm = createVm(i, brokerId);
                    vmList.add(vm);
                    if (i == 0) {
                        System.out.println("    VM #0 créée (initiale)");
                    } else {
                        System.out.println("    [HorizontalScaling] → Création VM #" + i +
                                " (charge > " + (int)(SCALE_THRESHOLD * 100) + "% détectée)");
                    }
                }

                broker.submitGuestList(vmList);

                // Distribuer les cloudlets en round-robin sur les VMs
                for (int i = 0; i < cloudletList.size(); i++) {
                    int vmId = i % vmList.size();
                    cloudletList.get(i).setVmId(vmId);
                }
            }

            broker.submitCloudletList(cloudletList);

            // 6. Lancer la simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // 7. Récupérer et afficher les résultats
            List<Cloudlet> results = broker.getCloudletReceivedList();
            printResults(results, scalingEnabled);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ──────────────────────────────────────────────
    //  Affichage des résultats
    // ──────────────────────────────────────────────
    private static void printResults(List<Cloudlet> list, boolean scaling) {
        DecimalFormat df = new DecimalFormat("###.##");
        System.out.println("\n    ╔══════════╦════════╦═══════╦══════════╦══════════╗");
        System.out.println("    ║ Cloudlet ║ Status ║ VM ID ║ Début(s) ║  Fin(s)  ║");
        System.out.println("    ╠══════════╬════════╬═══════╬══════════╬══════════╣");

        double totalTime = 0;
        double minStart = Double.MAX_VALUE;
        double maxFinish = 0;

        for (Cloudlet cl : list) {
            String status = cl.getCloudletStatusString();
            double start  = cl.getExecStartTime();
            double finish = cl.getFinishTime();
            totalTime += (finish - start);
            if (start < minStart)   minStart  = start;
            if (finish > maxFinish) maxFinish = finish;

            System.out.printf("    ║ %8d ║ %6s ║ %5d ║ %8s ║ %8s ║%n",
                    cl.getCloudletId(),
                    status.substring(0, Math.min(status.length(), 6)),
                    cl.getVmId(),
                    df.format(start),
                    df.format(finish));
        }

        System.out.println("    ╚══════════╩════════╩═══════╩══════════╩══════════╝");

        double avgTime = totalTime / list.size();
        System.out.printf("%n    Nb VMs utilisées        : %d%n", vmList.size());
        System.out.printf("    Temps moyen/cloudlet    : %s s%n", df.format(avgTime));
        System.out.printf("    Makespan total          : %s s%n", df.format(maxFinish - minStart));
        System.out.printf("    Résultat                : %s%n",
                scaling ? "Charge répartie sur " + vmList.size() + " VMs ✓"
                        : "Charge concentrée sur 1 VM (bottleneck)");
    }

    // ──────────────────────────────────────────────
    //  Factories
    // ──────────────────────────────────────────────
    private static Vm createVm(int id, int brokerId) {
        return new Vm(id, brokerId, VM_MIPS, VM_PES, VM_RAM, VM_BW, VM_SIZE,
                "Xen", new CloudletSchedulerTimeShared());
    }

    private static List<Cloudlet> createCloudlets(int brokerId) {
        List<Cloudlet> list = new ArrayList<>();
        UtilizationModel um = new UtilizationModelFull();
        for (int i = 0; i < NUM_CLOUDLETS; i++) {
            Cloudlet cl = new Cloudlet(i, CLOUDLET_LENGTH, CLOUDLET_PES,
                    CLOUDLET_FILESIZE, CLOUDLET_OUTPUTSIZE, um, um, um);
            cl.setUserId(brokerId);
            list.add(cl);
        }
        return list;
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();

        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(HOST_MIPS)));
        }

        hostList.add(new Host(0,
                new RamProvisionerSimple(HOST_RAM),
                new BwProvisionerSimple(HOST_BW),
                HOST_STORAGE,
                peList,
                new VmSchedulerTimeShared(peList)));

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen",
                hostList, 10.0,
                3.0, 0.05, 0.001, 0.0);

        return new Datacenter(name, characteristics,
                new VmAllocationPolicySimple(hostList),
                new LinkedList<>(), 0);
    }
}