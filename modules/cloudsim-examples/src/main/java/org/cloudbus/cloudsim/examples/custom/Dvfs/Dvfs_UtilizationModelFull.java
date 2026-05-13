package org.cloudbus.cloudsim.examples.custom.Dvfs;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;

import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * DVFS Test Class for Phase 3 (Weeks 7-8)
 * Compares Linear vs Cubic Power Models to simulate frequency scaling impact.
 */
public class Dvfs_UtilizationModelFull {

    public static void main(String[] args) {
        try {
            // Initialize CloudSim core [cite: 16, 17]
            CloudSim.init(1, Calendar.getInstance(), false);

            String vmAllocationPolicy = "dvfs";
            // 1. Create Hosts representing different DVFS Scaling states
            List<Host> hostList = new ArrayList<>();

            // Linear model: Simulates basic power scaling [cite: 38, 48]
            PowerHost linearHost = createPowerHost(1, new PowerModelLinear(250, 0.5));
            // Cubic model: Simulates realistic CMOS DVFS scaling where Power ∝ f³ [cite: 38, 48]
            PowerHost cubicHost = createPowerHost(2, new PowerModelCubic(250, 0.5));

            hostList.add(linearHost);
            hostList.add(cubicHost);

            // 2. Setup Datacenter and Broker
            Datacenter datacenter = createDatacenter("DVFS_Datacenter", hostList);
            DatacenterBroker broker = createBroker();

            // 3. Create Cloudlets to simulate different CPU loads [cite: 34, 51]
            List<Vm> vmlist = new ArrayList<>();
            List<Cloudlet> cloudletList = new ArrayList<>();

            // Creating a high-load scenario (100% CPU)
            for (int i = 0; i < 2; i++) {
                Vm vm = new Vm(i, broker.getId(), 1000, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerTimeShared());
                vmlist.add(vm);

                // UtilizationModelFull ensures 100% load to see max power impact
                Cloudlet cloudlet = new Cloudlet(i, 20000, 1, 300, 300, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
                cloudlet.setUserId(broker.getId());
                cloudlet.setVmId(i);
                cloudletList.add(cloudlet);
            }

            broker.submitGuestList(vmlist);
            broker.submitCloudletList(cloudletList);

            // 4. Run Simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // 5. Analysis of Energy-Performance Tradeoff [cite: 11, 53]
            System.out.println("\n========== DVFS ANALYSIS RESULTS ==========");
            analyzeHost(linearHost, "Linear Scaling");
            analyzeHost(cubicHost, "Cubic (DVFS) Scaling");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void analyzeHost(PowerHost host, String label) {
        // Retrieve energy consumption based on current CPU utilization [cite: 40, 52]
        double utilization = host.getUtilizationOfCpu();
        double power = host.getPowerModel().getPower(utilization);
        System.out.printf("Model: %s | CPU Utilization: %.2f%% | Current Power: %.2f Watts%n",
                label, utilization * 100, power);
    }

    private static PowerHost createPowerHost(int id, org.cloudbus.cloudsim.power.models.PowerModel pm) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000)));
        return new PowerHost(id, new RamProvisionerSimple(4096), new BwProvisionerSimple(10000),
                1000000, peList, new VmSchedulerTimeShared(peList), pm);
    }

    private static Datacenter createDatacenter(String name, List<Host> hostList) throws Exception {
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics("x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0);
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
    }

    private static DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("DVFS_Broker");
    }
}