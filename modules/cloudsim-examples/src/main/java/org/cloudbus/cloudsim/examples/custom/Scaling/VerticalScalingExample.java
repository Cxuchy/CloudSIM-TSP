package org.cloudbus.cloudsim.examples.custom.Scaling;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.*;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

/**
 * Vertical Scaling Simulation — CloudSim + XChart
 *
 * Flow:
 *   Phase 1 (cloudlets 0–5)  : Initial VM  — 500 MIPS, 1 vCPU,  512 MB RAM
 *   [VM DESTROY + RECREATE]
 *   Phase 2 (cloudlets 6–11) : Scaled  VM  — 2000 MIPS, 4 vCPUs, 2048 MB RAM
 *
 * Power model: linear between idle (100 W) and peak (250 W) for the initial VM,
 *              linear between idle (150 W) and peak (400 W) for the scaled VM.
 *   Power(W) = idleWatts + (peakWatts - idleWatts) * cpuUtilisation
 *   Energy(J) = Power(W) * execTime(s)
 *
 * XChart produces a 4-panel PNG saved to vertical_scaling_chart.png.
 */
public class VerticalScalingExample {

    // ── Simulation constants ──────────────────────────────────────────────────
    private static final int  TOTAL_CLOUDLETS  = 12;
    private static final int  CLOUDLETS_PHASE1 = 6;
    private static final int  VM_ID            = 0;

    // Initial VM spec
    private static final int INIT_MIPS  = 500;
    private static final int INIT_VCPUS = 1;
    private static final int INIT_RAM   = 512;    // MB

    // Scaled VM spec
    private static final int SCALED_MIPS  = 2000;
    private static final int SCALED_VCPUS = 4;
    private static final int SCALED_RAM   = 2048; // MB

    // Shared VM parameters
    private static final long   STORAGE = 10_000L;
    private static final long   BW      = 1_000L;
    private static final String VMM     = "Xen";

    // Cloudlet parameters
    private static final long CLOUDLET_LENGTH      = 40_000L; // MI
    private static final long CLOUDLET_FILE_SIZE   = 300L;
    private static final long CLOUDLET_OUTPUT_SIZE = 300L;


    private static final double INIT_IDLE_WATTS  = 100.0;
    private static final double INIT_PEAK_WATTS  = 250.0;
    // Scaled VM: quad-core, higher-frequency — larger power envelope
    private static final double SCALED_IDLE_WATTS = 150.0;
    private static final double SCALED_PEAK_WATTS = 400.0;

    // ── Shared state ──────────────────────────────────────────────────────────
    private static List<Cloudlet> phase1Results = new ArrayList<>();
    private static List<Cloudlet> phase2Results = new ArrayList<>();

    // =========================================================================
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║      Vertical Scaling Simulation (CloudSim)      ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");
        try {
            runSimulation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void runSimulation() throws Exception {

        int numUsers = 1;
        Calendar calendar = Calendar.getInstance();
        boolean traceFlag = false;

        // ── Phase 1 ───────────────────────────────────────────────────────────
        printPhaseHeader("PHASE 1 — Initial VM  (cloudlets 0–5)");

        CloudSim.init(numUsers, calendar, traceFlag);
        createDatacenter("Datacenter_Phase1");

        DatacenterBroker broker1   = new DatacenterBroker("Broker_Phase1");
        int              broker1Id = broker1.getId();

        Vm initialVm = buildVm(VM_ID, broker1Id, INIT_MIPS, INIT_VCPUS, INIT_RAM);
        printVmSpec("INITIAL VM (before vertical scaling)", initialVm);

        broker1.submitGuestList(Collections.singletonList(initialVm));
        broker1.submitCloudletList(createCloudlets(broker1Id, 0, CLOUDLETS_PHASE1));

        CloudSim.startSimulation();
        phase1Results = broker1.getCloudletReceivedList();
        CloudSim.stopSimulation();

        printCloudletResults("Phase 1 Results  —  Initial VM",
                phase1Results, INIT_MIPS, INIT_VCPUS,
                INIT_IDLE_WATTS, INIT_PEAK_WATTS);

        // ── VM destroy / recreate event ────────────────────────────────────
        System.out.println(">>> [EVENT] Destroying VM #" + VM_ID + " (initial spec)...");
        System.out.printf ("            MIPS=%.0f  vCPUs=%d  RAM=%d MB%n%n",
                initialVm.getMips(), initialVm.getNumberOfPes(), initialVm.getRam());

        // ── Phase 2 ───────────────────────────────────────────────────────────
        printPhaseHeader("PHASE 2 — Scaled VM   (cloudlets 6–11)");
        System.out.println(">>> [EVENT] Creating vertically scaled VM #" + VM_ID + "...");

        CloudSim.init(numUsers, calendar, traceFlag);
        createDatacenter("Datacenter_Phase2");

        DatacenterBroker broker2   = new DatacenterBroker("Broker_Phase2");
        int              broker2Id = broker2.getId();

        Vm scaledVm = buildVm(VM_ID, broker2Id, SCALED_MIPS, SCALED_VCPUS, SCALED_RAM);
        printVmSpec("SCALED VM (after vertical scaling)", scaledVm);
        printScalingDelta(initialVm, scaledVm);

        broker2.submitGuestList(Collections.singletonList(scaledVm));
        broker2.submitCloudletList(createCloudlets(broker2Id, CLOUDLETS_PHASE1, TOTAL_CLOUDLETS));

        CloudSim.startSimulation();
        phase2Results = broker2.getCloudletReceivedList();
        CloudSim.stopSimulation();

        printCloudletResults("Phase 2 Results  —  Scaled VM",
                phase2Results, SCALED_MIPS, SCALED_VCPUS,
                SCALED_IDLE_WATTS, SCALED_PEAK_WATTS);

        // ── Final summary + chart ─────────────────────────────────────────────
        printFinalSummary(initialVm, scaledVm, phase1Results, phase2Results);
    }

    private static double cpuUtilisation(int numVcpus) {
        return Math.min(1.0, (double) CLOUDLETS_PHASE1 / numVcpus);
    }

    private static double powerWatts(double util, double idleW, double peakW) {
        return idleW + util * (peakW - idleW);
    }

    private static double energyJoules(double execTimeSec,
                                       double util,
                                       double idleW, double peakW) {
        return powerWatts(util, idleW, peakW) * execTimeSec;
    }


    private static Vm buildVm(int vmId, int brokerId,
                              int mips, int vcpus, int ram) {
        return new Vm(vmId, brokerId, mips, vcpus, ram,
                BW, STORAGE, VMM, new CloudletSchedulerTimeShared());
    }


    private static Datacenter createDatacenter(String name) throws Exception {
        int numPes   = 16;
        int hostMips = 10_000;

        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < numPes; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(hostMips)));
        }

        Host host = new Host(0,
                new RamProvisionerSimple(16_384),
                new BwProvisionerSimple(10_000),
                1_000_000L, peList,
                new VmSchedulerTimeShared(peList));

        DatacenterCharacteristics chars = new DatacenterCharacteristics(
                "x86", "Linux", "Xen",
                Collections.singletonList(host),
                10.0, 3.0, 0.05, 0.001, 0.1);

        return new Datacenter(name, chars,
                new VmAllocationPolicySimple(Collections.singletonList(host)),
                new LinkedList<>(), 0);
    }


    private static List<Cloudlet> createCloudlets(int brokerId, int fromId, int toId) {
        List<Cloudlet> list = new ArrayList<>();
        UtilizationModel um = new UtilizationModelFull();
        for (int i = fromId; i < toId; i++) {
            Cloudlet c = new Cloudlet(i, CLOUDLET_LENGTH, 1,
                    CLOUDLET_FILE_SIZE, CLOUDLET_OUTPUT_SIZE, um, um, um);
            c.setUserId(brokerId);
            list.add(c);
        }
        return list;
    }


    private static void printPhaseHeader(String text) {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" " + text);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }

    private static void printVmSpec(String label, Vm vm) {
        System.out.printf("  %s%n", label);
        System.out.printf("    VM ID  : %d%n",      vm.getId());
        System.out.printf("    MIPS   : %.0f%n",    vm.getMips());
        System.out.printf("    vCPUs  : %d%n",      vm.getNumberOfPes());
        System.out.printf("    RAM    : %d MB%n%n",  vm.getRam());
    }

    private static void printScalingDelta(Vm before, Vm after) {
        System.out.println("  Resource changes applied by vertical scaling:");
        System.out.printf("    MIPS   : %4.0f → %4.0f  (×%.1f)%n",
                before.getMips(),        after.getMips(),
                after.getMips()        / before.getMips());
        System.out.printf("    vCPUs  : %4d → %4d  (×%.1f)%n",
                before.getNumberOfPes(), after.getNumberOfPes(),
                (double) after.getNumberOfPes() / before.getNumberOfPes());
        System.out.printf("    RAM    : %4d → %4d MB  (×%.1f)%n%n",
                before.getRam(), after.getRam(),
                (double) after.getRam() / before.getRam());
    }

    private static void printCloudletResults(String title, List<Cloudlet> list,
                                             int mips, int vcpus,
                                             double idleW, double peakW) {
        double util = cpuUtilisation(vcpus);
        double pw   = powerWatts(util, idleW, peakW);

        String header = String.format("│  %-75s│", title);
        System.out.println("┌" + "─".repeat(77) + "┐");
        System.out.println(header);
        System.out.println("├────────────┬──────────┬────────────┬────────────┬────────────┬────────────┤");
        System.out.printf("│ %-10s │ %-8s │ %-10s │ %-10s │ %-10s │ %-10s │%n",
                "Cloudlet", "Status", "Start (s)", "Finish (s)", "CPU Time(s)", "Power (W)");
        System.out.println("├────────────┼──────────┼────────────┼────────────┼────────────┼────────────┤");

        for (Cloudlet c : list) {
            if (c.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                System.out.printf("│ %-10d │ %-8s │ %-10.2f │ %-10.2f │ %-10.2f │ %-10.2f │%n",
                        c.getCloudletId(), "SUCCESS",
                        c.getExecStartTime(), c.getFinishTime(),
                        c.getActualCPUTime(), pw);
            }
        }
        System.out.println("└────────────┴──────────┴────────────┴────────────┴────────────┴────────────┘");
        System.out.printf("  VM utilisation: %.1f %%   Instantaneous power: %.2f W%n%n",
                util * 100, pw);
    }

    private static void printFinalSummary(Vm before, Vm after,
                                          List<Cloudlet> phase1,
                                          List<Cloudlet> phase2) {

        double avgTimeBefore = avgCpuTime(phase1);
        double avgTimeAfter  = avgCpuTime(phase2);

        double utilBefore = cpuUtilisation(before.getNumberOfPes());
        double utilAfter  = cpuUtilisation(after.getNumberOfPes());

        double pwBefore = powerWatts(utilBefore, INIT_IDLE_WATTS,   INIT_PEAK_WATTS);
        double pwAfter  = powerWatts(utilAfter,  SCALED_IDLE_WATTS, SCALED_PEAK_WATTS);

        double timeImprovement = ((avgTimeBefore - avgTimeAfter) / avgTimeBefore) * 100.0;
        double speedup         = avgTimeBefore / avgTimeAfter;

        System.out.println("\n╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println(  "║             VERTICAL SCALING — FINAL SUMMARY                        ║");
        System.out.println(  "╠══════════════════════════════════════════════════════════════════════╣");
        System.out.printf(   "║  %-34s  %8.0f  →  %8.0f MIPS   ║%n", "MIPS:",  before.getMips(),        after.getMips());
        System.out.printf(   "║  %-34s  %8d  →  %8d cores  ║%n",     "vCPUs:", before.getNumberOfPes(), after.getNumberOfPes());
        System.out.printf(   "║  %-34s  %8d  →  %8d MB     ║%n",     "RAM:",   before.getRam(),         after.getRam());
        System.out.println(  "╠══════════════════════════════════════════════════════════════════════╣");
        System.out.printf(   "║  %-34s  %8.2f  →  %8.2f s      ║%n", "Avg CPU time/cloudlet:", avgTimeBefore, avgTimeAfter);
        System.out.printf(   "║  %-34s  %8.2f  →  %8.2f W      ║%n", "Instantaneous power:", pwBefore, pwAfter);
        System.out.println(  "╠══════════════════════════════════════════════════════════════════════╣");
        System.out.printf(   "║  Performance improvement (time)      :  %7.1f %%                   ║%n", timeImprovement);
        System.out.printf(   "║  Speed-up factor                     :  %7.2f ×                   ║%n", speedup);
        System.out.println(  "╚══════════════════════════════════════════════════════════════════════╝");
    }





    private static double avgCpuTime(List<Cloudlet> list) {
        return list.stream()
                .filter(c -> c.getStatus() == Cloudlet.CloudletStatus.SUCCESS)
                .mapToDouble(Cloudlet::getActualCPUTime)
                .average().orElse(0);
    }
}