package org.cloudbus.cloudsim.examples.custom.VM_Placement_Policies;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.examples.custom.VM_Placement_Policies.Policies.VmAllocationPolicyBestFit;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;

public class BestFit {

    private static final int  NUM_HOSTS    = 8;
    private static final int  HOST_MIPS    = 3000;
    private static final int  HOST_RAM     = 16384;
    private static final long HOST_BW      = 10000;
    private static final long HOST_STORAGE = 1_000_000;

    private static final int[][] VM_PROFILES = {
            {1400, 2048, 1000},
            { 400,  512,  200},
            { 900, 1024,  500},
            {1400, 2048, 1000},
            { 400,  512,  200},
            { 900, 1024,  500},
            {1400, 2048, 1000},
            { 400,  512,  200},
            { 900, 1024,  500},
            {1400, 2048, 1000},
            { 400,  512,  200},
            { 900, 1024,  500},
            { 400,  512,  200},
            { 900, 1024,  500},
            { 400,  512,  200},
            { 900, 1024,  500},
    };

    // Snapshot captured during allocation (before simulation tears down VMs)
    private static final Map<Integer, Integer> vmToHost = new LinkedHashMap<>();
    private static final Map<Integer, Integer> vmMips   = new LinkedHashMap<>();

    public static void main(String[] args) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            List<PowerHost> hostList = createHosts();

            String vmAllocationPolicy = "dvfs";

            // Use BestFit policy directly — it implements findHostForGuest()
            // Subclass only to intercept the snapshot; no anonymous class needed
            VmAllocationPolicyBestFit policy = new VmAllocationPolicyBestFit(hostList) {
                @Override
                public boolean allocateHostForGuest(GuestEntity guest) {
                    boolean ok = super.allocateHostForGuest(guest);
                    if (ok) {
                        HostEntity h = getHost(guest);
                        vmToHost.put(guest.getId(), h.getId());
                        vmMips.put(guest.getId(), (int)((Vm) guest).getMips());
                    }
                    return ok;
                }
            };

            PowerDatacenter datacenter = createDatacenter("DC_BestFit", hostList, policy);
            DatacenterBroker broker    = new DatacenterBroker("Broker_BestFit");

            List<Vm>       vmList       = createVms(broker.getId());
            List<Cloudlet> cloudletList = createCloudlets(broker.getId(), vmList);

            broker.submitGuestList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            printResults(datacenter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Infrastructure ───────────────────────────────────────────────────────

    private static List<PowerHost> createHosts() {
        List<PowerHost> list = new ArrayList<>();
        for (int i = 0; i < NUM_HOSTS; i++) {
            List<Pe> peList = Collections.singletonList(
                    new Pe(0, new PeProvisionerSimple(HOST_MIPS)));
            list.add(new PowerHost(i,
                    new RamProvisionerSimple(HOST_RAM),
                    new BwProvisionerSimple(HOST_BW),
                    HOST_STORAGE, peList,
                    new VmSchedulerTimeShared(peList),
                    new PowerModelLinear(200, 0.4)));
        }
        return list;
    }

    private static List<Vm> createVms(int brokerId) {
        List<Vm> list = new ArrayList<>();
        for (int i = 0; i < VM_PROFILES.length; i++) {
            list.add(new Vm(i, brokerId,
                    VM_PROFILES[i][0], 1,
                    VM_PROFILES[i][1], VM_PROFILES[i][2],
                    10000, "Xen", new CloudletSchedulerTimeShared()));
        }
        Collections.shuffle(list);
        return list;
    }

    private static List<Cloudlet> createCloudlets(int brokerId, List<Vm> vmList) {
        List<Cloudlet> list = new ArrayList<>();
        UtilizationModel full = new UtilizationModelFull();
        for (int i = 0; i < vmList.size(); i++) {
            Cloudlet cl = new Cloudlet(i, 40_000, 1, 300, 300, full, full, full);
            cl.setUserId(brokerId);
            cl.setVmId(vmList.get(i).getId());
            list.add(cl);
        }
        return list;
    }

    // ── Results ──────────────────────────────────────────────────────────────

    private static void printResults(PowerDatacenter datacenter) {
        // Rebuild per-host view from snapshot
        Map<Integer, List<Integer>> hostVms = new LinkedHashMap<>();
        for (int i = 0; i < NUM_HOSTS; i++) hostVms.put(i, new ArrayList<>());
        for (Map.Entry<Integer, Integer> e : vmToHost.entrySet())
            hostVms.get(e.getValue()).add(vmMips.get(e.getKey()));

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║       RÉSULTATS — POLITIQUE BEST FIT                ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.printf("%-8s %-6s %-32s %-15s%n",
                "Host", "VMs", "MIPS alloués [total/cap]", "Remplissage");
        System.out.println("─".repeat(67));

        List<String> hostNames = new ArrayList<>();
        List<Number> vmCounts  = new ArrayList<>();
        int activeHosts = 0;

        for (int i = 0; i < NUM_HOSTS; i++) {
            List<Integer> vms   = hostVms.get(i);
            int numVms          = vms.size();
            int sumMips         = vms.stream().mapToInt(Integer::intValue).sum();
            double fillPct      = sumMips * 100.0 / HOST_MIPS;

            StringBuilder detail = new StringBuilder();
            for (int m : vms) { if (detail.length() > 0) detail.append("+"); detail.append(m); }
            if (detail.length() == 0) detail.append("—");
            else detail.append(" [").append(sumMips).append("/").append(HOST_MIPS).append("]");

            hostNames.add("H" + i);
            vmCounts.add(numVms);
            if (numVms > 0) activeHosts++;

            System.out.printf("Host %-3d  %-6d %-32s %.1f%%%n", i, numVms, detail, fillPct);
        }

        int totalMips = vmMips.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("─".repeat(67));
        System.out.printf("VMs placées          : %d / %d%n", vmToHost.size(), VM_PROFILES.length);
        System.out.printf("Hôtes actifs         : %d / %d%n", activeHosts, NUM_HOSTS);
        System.out.printf("MIPS alloués         : %d / %d%n", totalMips, NUM_HOSTS * HOST_MIPS);
        System.out.printf("Puissance datacenter : %.2f W%n", datacenter.getPower());

        displayChart(hostNames, vmCounts);
    }

    private static void displayChart(List<String> xData, List<Number> yData) {
        CategoryChart chart = new CategoryChartBuilder()
                .width(900).height(500)
                .title("BestFit — VMs par hôte  (capacité: 3000 MIPS/hôte)")
                .xAxisTitle("Hôte").yAxisTitle("Nombre de VMs").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setChartTitleVisible(true);
        chart.addSeries("VMs allouées", xData, yData);
        new SwingWrapper<>(chart).displayChart();
    }

    private static PowerDatacenter createDatacenter(
            String name, List<PowerHost> hostList, VmAllocationPolicy policy) throws Exception {
        DatacenterCharacteristics chars = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0);
        return new PowerDatacenter(name, chars, policy, new LinkedList<>(), 0.1);
    }
}