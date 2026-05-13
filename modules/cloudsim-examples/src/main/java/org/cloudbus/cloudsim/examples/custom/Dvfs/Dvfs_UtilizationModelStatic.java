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

public class Dvfs_UtilizationModelStatic {

    public static void main(String[] args) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            String vmAllocationPolicy = "dvfs";
            List<Host> hostList = new ArrayList<>();
            // Host capacity: 1000 MIPS, 4096 RAM
            hostList.add(createPowerHost(0, 1000, 4096, new PowerModelLinear(250, 0.5)));
            hostList.add(createPowerHost(1, 1000, 4096, new PowerModelCubic(250, 0.5)));

            Datacenter datacenter = createDatacenter("DVFS_Datacenter", hostList);
            DatacenterBroker broker = createBroker();

            List<Vm> vmlist = new ArrayList<>();
            List<Cloudlet> cloudletList = new ArrayList<>();

            // THE TRICK:
            // VM RAM = 4096 (Forces 1 VM per Host)
            // VM MIPS = 500 (Forces exactly 50% CPU load since Host has 1000 MIPS)
            for (int i = 0; i < 2; i++) {
                Vm vm = new Vm(i, broker.getId(), 500, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerTimeShared());
                vmlist.add(vm);

                UtilizationModel full = new UtilizationModelFull();
                Cloudlet cloudlet = new Cloudlet(i, 20000, 1, 300, 300, full, full, full);
                cloudlet.setUserId(broker.getId());
                cloudlet.setVmId(i);
                cloudletList.add(cloudlet);
            }

            broker.submitGuestList(vmlist);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // 5. Results Analysis
            System.out.println("\n========== DVFS ANALYSIS RESULTS ==========");
            System.out.println("Condition: 1 VM per Host, exactly 50% CPU load.\n");

            // We pass the known 0.50 utilization to bypass the stopSimulation() clock reset
            analyzeHost((PowerHost) hostList.get(0), "Linear Scaling", 0.50);
            analyzeHost((PowerHost) hostList.get(1), "Cubic (DVFS) Scaling", 0.50);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void analyzeHost(PowerHost host, String label, double targetUtilization) {
        // Query the PowerModel directly to ensure accurate readings for your report
        double power = host.getPowerModel().getPower(targetUtilization);
        System.out.printf("Model: %s | CPU Utilization: %.2f%% | Power Consumed: %.2f Watts%n",
                label, targetUtilization * 100, power);
    }

    private static PowerHost createPowerHost(int id, int mips, int ram, org.cloudbus.cloudsim.power.models.PowerModel pm) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));
        return new PowerHost(id, new RamProvisionerSimple(ram), new BwProvisionerSimple(10000),
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