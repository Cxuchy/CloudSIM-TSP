package org.cloudbus.cloudsim.examples.custom.Scaling;

import org.cloudbus.cloudsim.core.CloudSim;


import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.*;

import java.text.DecimalFormat;
import java.util.*;

/**
 * VerticalScalingExample — CloudSim 7G
 *
 * Simule le scaling vertical : augmentation des ressources CPU (MIPS)
 * et RAM d'une VM existante pour absorber une charge croissante,
 * sans créer de nouvelle VM.
 *
 * Trois scénarios sont comparés :
 *   - SMALL  : VM avec ressources minimales (1000 MIPS, 512 MB)
 *   - MEDIUM : VM après 1er scale-up      (2000 MIPS, 1024 MB)
 *   - LARGE  : VM après 2ème scale-up     (4000 MIPS, 2048 MB)
 *
 * Les mêmes cloudlets sont exécutés dans chaque scénario.
 * Le temps d'exécution diminue proportionnellement à l'augmentation
 * des MIPS — ce qui démontre l'effet du vertical scaling.
 */
public class VerticalScalingExample {

    // ──────────────────────────────────────────────
    //  Paramètres des cloudlets (identiques dans les 3 scénarios)
    // ──────────────────────────────────────────────
    private static final int  NUM_CLOUDLETS         = 6;
    private static final long CLOUDLET_LENGTH       = 40000; // MI
    private static final long CLOUDLET_FILESIZE     = 300;
    private static final long CLOUDLET_OUTPUTSIZE   = 300;
    private static final int  CLOUDLET_PES          = 1;

    // Ressources VM communes
    private static final long VM_BW   = 1000;
    private static final long VM_SIZE = 10000;

    // Ressources hôte (assez larges pour les 3 scénarios)
    private static final int  HOST_MIPS    = 20000;
    private static final int  HOST_PES     = 8;
    private static final int  HOST_RAM     = 16384; // 16 GB
    private static final long HOST_BW      = 10000;
    private static final long HOST_STORAGE = 1000000;

    // ──────────────────────────────────────────────
    //  Définition des 3 niveaux de scaling
    // ──────────────────────────────────────────────
    private enum ScaleLevel {
        SMALL  ("SMALL  (baseline)",  1000, 1, 512),
        MEDIUM ("MEDIUM (scale x2)",  2000, 2, 1024),
        LARGE  ("LARGE  (scale x4)",  4000, 4, 2048);

        final String label;
        final int    mips;
        final int    pes;
        final int    ram;

        ScaleLevel(String label, int mips, int pes, int ram) {
            this.label = label;
            this.mips  = mips;
            this.pes   = pes;
            this.ram   = ram;
        }
    }

    // Stockage des résultats pour le tableau final
    private static final Map<ScaleLevel, Double> makespanResults = new LinkedHashMap<>();
    private static final Map<ScaleLevel, Double> avgTimeResults  = new LinkedHashMap<>();

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   VERTICAL SCALING — CloudSim 7G");
        System.out.println("=================================================\n");

        for (ScaleLevel level : ScaleLevel.values()) {
            runScenario(level);
            System.out.println();
        }

        printComparisonTable();
    }

    // ──────────────────────────────────────────────
    //  Lancement d'un scénario
    // ──────────────────────────────────────────────
    private static void runScenario(ScaleLevel level) {
        System.out.println(">>> Scénario : " + level.label);
        System.out.printf("    Ressources VM : %d MIPS | %d vCPU | %d MB RAM%n",
                level.mips, level.pes, level.ram);

        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            Datacenter datacenter = createDatacenter("DC_" + level.name());
            DatacenterBroker broker = new DatacenterBroker("Broker_" + level.name());
            int brokerId = broker.getId();

            // Créer la VM avec les ressources du niveau courant
            Vm vm = new Vm(0, brokerId,
                    level.mips, level.pes, level.ram,
                    VM_BW, VM_SIZE,
                    "Xen", new CloudletSchedulerTimeShared());

            List<Vm> vmList = new ArrayList<>();
            vmList.add(vm);
            broker.submitGuestList(vmList);

            // Créer les cloudlets
            List<Cloudlet> cloudletList = createCloudlets(brokerId);
            for (Cloudlet cl : cloudletList) {
                cl.setVmId(0);
            }
            broker.submitCloudletList(cloudletList);

            // Lancer la simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // Récupérer résultats
            List<Cloudlet> results = broker.getCloudletReceivedList();
            printResults(results, level);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ──────────────────────────────────────────────
    //  Affichage des résultats par scénario
    // ──────────────────────────────────────────────
    private static void printResults(List<Cloudlet> list, ScaleLevel level) {
        DecimalFormat df = new DecimalFormat("###.##");

        System.out.println("    ╔══════════╦════════╦══════════╦══════════╦══════════╗");
        System.out.println("    ║ Cloudlet ║ Status ║ Début(s) ║  Fin(s)  ║ Durée(s) ║");
        System.out.println("    ╠══════════╬════════╬══════════╬══════════╬══════════╣");

        double totalTime  = 0;
        double minStart   = Double.MAX_VALUE;
        double maxFinish  = 0;

        for (Cloudlet cl : list) {
            double start    = cl.getExecStartTime();
            double finish   = cl.getFinishTime();
            double duration = finish - start;
            totalTime += duration;
            if (start  < minStart)  minStart  = start;
            if (finish > maxFinish) maxFinish = finish;

            System.out.printf("    ║ %8d ║ %6s ║ %8s ║ %8s ║ %8s ║%n",
                    cl.getCloudletId(),
                    cl.getCloudletStatusString().substring(0, 6),
                    df.format(start),
                    df.format(finish),
                    df.format(duration));
        }

        System.out.println("    ╚══════════╩════════╩══════════╩══════════╩══════════╝");

        double makespan = maxFinish - minStart;
        double avgTime  = totalTime / list.size();

        makespanResults.put(level, makespan);
        avgTimeResults.put(level, avgTime);

        System.out.printf("    Makespan total       : %s s%n", df.format(makespan));
        System.out.printf("    Temps moyen/cloudlet : %s s%n", df.format(avgTime));
    }

    // ──────────────────────────────────────────────
    //  Tableau comparatif final
    // ──────────────────────────────────────────────
    private static void printComparisonTable() {
        DecimalFormat df = new DecimalFormat("###.##");

        System.out.println("=================================================");
        System.out.println("   TABLEAU COMPARATIF — EFFET DU VERTICAL SCALING");
        System.out.println("=================================================");
        System.out.printf("%-26s | %5s | %4s | %6s | %12s | %10s | %8s%n",
                "Scénario", "MIPS", "PES", "RAM MB",
                "Makespan(s)", "Moy/CL(s)", "Gain(%)");
        System.out.println("-".repeat(85));

        double baseMakespan = makespanResults.get(ScaleLevel.SMALL);
        double baseAvg      = avgTimeResults.get(ScaleLevel.SMALL);

        for (ScaleLevel level : ScaleLevel.values()) {
            double makespan = makespanResults.get(level);
            double avg      = avgTimeResults.get(level);
            double gainMakespan = 100.0 * (baseMakespan - makespan) / baseMakespan;
            double gainAvg      = 100.0 * (baseAvg - avg)           / baseAvg;

            System.out.printf("%-26s | %5d | %4d | %6d | %12s | %10s | %7s%%%n",
                    level.label, level.mips, level.pes, level.ram,
                    df.format(makespan),
                    df.format(avg),
                    level == ScaleLevel.SMALL ? "baseline" : "+" + df.format(gainMakespan));
        }

        System.out.println("\n  → Plus les MIPS augmentent, plus le makespan diminue.");
        System.out.println("  → Le vertical scaling est efficace pour des tâches CPU-bound.");
        System.out.println("=================================================");
    }

    // ──────────────────────────────────────────────
    //  Factories
    // ──────────────────────────────────────────────
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
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(HOST_MIPS)));
        }

        List<Host> hostList = new ArrayList<>();
        hostList.add(new Host(0,
                new RamProvisionerSimple(HOST_RAM),
                new BwProvisionerSimple(HOST_BW),
                HOST_STORAGE, peList,
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