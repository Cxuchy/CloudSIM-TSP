package org.cloudbus.cloudsim.examples.power;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.*;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyMinimumUtilization;

import java.text.DecimalFormat;
import java.util.*;

public class MigrationExample {

    // SIMULATION

    private static final double SIMULATION_TIME = 3600 * 24;
    private static final double SCHEDULING_INTERVAL = 10;

    // seuil surcharge
    private static final double THRESHOLD = 0.60;

    // HOST CONFIGURATION

    // WEAK HOST
    private static final int WEAK_HOST_PES = 1;
    private static final int WEAK_HOST_PE_MIPS = 1000;
    private static final int WEAK_HOST_RAM = 4096;
    private static final long WEAK_HOST_BW = 100000;
    private static final long WEAK_HOST_STORAGE = 1000000;

    private static final double WEAK_HOST_MAX_POWER = 100;
    private static final double WEAK_HOST_STATIC_POWER_PERCENT = 0.7;

    // STRONG HOST
    private static final int STRONG_HOST_PES = 4;
    private static final int STRONG_HOST_PE_MIPS = 4000;
    private static final int STRONG_HOST_RAM = 16384;
    private static final long STRONG_HOST_BW = 1000000;
    private static final long STRONG_HOST_STORAGE = 10000000;

    private static final double STRONG_HOST_MAX_POWER = 250;
    private static final double STRONG_HOST_STATIC_POWER_PERCENT = 0.6;

    // VM CONFIGURATION

    private static final int VM_COUNT = 4;

    private static final int VM_MIPS = 800;
    private static final int VM_PES = 1;
    private static final int VM_RAM = 1024;
    private static final long VM_BW = 1000;
    private static final long VM_SIZE = 10000;

    // CLOUDLET CONFIGURATION

    private static final long CLOUDLET_LENGTH = 2500 * 24 * 60 * 60;

    private static final int CLOUDLET_PES = 1;
    private static final long CLOUDLET_FILE_SIZE = 300;
    private static final long CLOUDLET_OUTPUT_SIZE = 300;

    // MAIN

    public static void main(String[] args) {

        try {

            Log.println("START SIMULATION\n");

            CloudSim.init(1, Calendar.getInstance(), false);

            // CREATE HOSTS

            List<PowerHost> hostList = createHosts();

            // CREATE DATACENTER

            PowerDatacenter datacenter =
                    createDatacenter(hostList);

            // BROKER

            DatacenterBroker broker =
                    new DatacenterBroker("Broker");

            int brokerId = broker.getId();

            // CREATE VMS

            List<Vm> vmList =
                    createVMs(brokerId);

            // CREATE CLOUDLETS

            List<Cloudlet> cloudlets =
                    createCloudlets(brokerId);

            broker.submitGuestList(vmList);

            broker.submitCloudletList(cloudlets);

            // BIND CLOUDLETS -> VMS

            for (int i = 0; i < cloudlets.size(); i++) {

                broker.bindCloudletToVm(i, i);
            }

            // FORCE INITIAL VM PLACEMENT

            PowerHost weakHost = hostList.get(0);

            PowerHost strongHost = hostList.get(1);

            // host faible surcharge
            weakHost.guestCreate(vmList.get(0));
            weakHost.guestCreate(vmList.get(1));

            // host puissant
            strongHost.guestCreate(vmList.get(2));
            strongHost.guestCreate(vmList.get(3));

            Log.println("\nINITIAL VM PLACEMENT");

            Log.println("VM #0 -> Host #0");
            Log.println("VM #1 -> Host #0");

            Log.println("VM #2 -> Host #1");
            Log.println("VM #3 -> Host #1");

            Log.println();

            // START SIMULATION

            CloudSim.terminateSimulation(
                    SIMULATION_TIME
            );

            double clock =
                    CloudSim.startSimulation();

            CloudSim.stopSimulation();

            // RESULTS

            printResults(datacenter, clock);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // CREATE HOSTS

    private static List<PowerHost> createHosts() {

        List<PowerHost> list = new ArrayList<>();

        // WEAK HOST

        List<Pe> weakPeList = new ArrayList<>();

        for (int i = 0; i < WEAK_HOST_PES; i++) {

            weakPeList.add(
                    new Pe(
                            i,
                            new PeProvisionerSimple(
                                    WEAK_HOST_PE_MIPS
                            )
                    )
            );
        }

        PowerHost weakHost =
                new PowerHost(
                        0,

                        new RamProvisionerSimple(
                                WEAK_HOST_RAM
                        ),

                        new BwProvisionerSimple(
                                WEAK_HOST_BW
                        ),

                        WEAK_HOST_STORAGE,

                        weakPeList,

                        new VmSchedulerTimeShared(
                                weakPeList
                        ),

                        new PowerModelLinear(
                                WEAK_HOST_MAX_POWER,
                                WEAK_HOST_STATIC_POWER_PERCENT
                        )
                );

        list.add(weakHost);

        // STRONG HOST

        List<Pe> strongPeList = new ArrayList<>();

        for (int i = 0; i < STRONG_HOST_PES; i++) {

            strongPeList.add(
                    new Pe(
                            i,
                            new PeProvisionerSimple(
                                    STRONG_HOST_PE_MIPS
                            )
                    )
            );
        }

        PowerHost strongHost =
                new PowerHost(
                        1,

                        new RamProvisionerSimple(
                                STRONG_HOST_RAM
                        ),

                        new BwProvisionerSimple(
                                STRONG_HOST_BW
                        ),

                        STRONG_HOST_STORAGE,

                        strongPeList,

                        new VmSchedulerTimeShared(
                                strongPeList
                        ),

                        new PowerModelLinear(
                                STRONG_HOST_MAX_POWER,
                                STRONG_HOST_STATIC_POWER_PERCENT
                        )
                );

        list.add(strongHost);

        Log.println("HOSTS");

        for (PowerHost h : list) {

            Log.println(
                    "Host #"
                            + h.getId()
                            + " totalMips="
                            + h.getTotalMips()
            );
        }

        Log.println();

        return list;
    }

    // CREATE DATACENTER

    private static PowerDatacenter createDatacenter(
            List<PowerHost> hosts
    ) throws Exception {

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        "x86",
                        "Linux",
                        "Xen",
                        hosts,
                        10.0,
                        3.0,
                        0.05,
                        0.001,
                        0.0
                );

        PowerVmAllocationPolicyMigrationStaticThreshold policy =
                new PowerVmAllocationPolicyMigrationStaticThreshold(
                        hosts,

                        new SelectionPolicyMinimumUtilization(),

                        THRESHOLD
                );

        PowerDatacenter dc =
                new PowerDatacenter(
                        "Datacenter",

                        characteristics,

                        policy,

                        new LinkedList<>(),

                        SCHEDULING_INTERVAL
                );

        dc.setDisableMigrations(false);

        Log.println("DATACENTER");

        Log.println("Threshold = " + THRESHOLD);

        Log.println("Migrations enabled");

        Log.println();

        return dc;
    }

    // CREATE VMS

    private static List<Vm> createVMs(
            int brokerId
    ) {

        List<Vm> list = new ArrayList<>();

        for (int i = 0; i < VM_COUNT; i++) {

            PowerVm vm =
                    new PowerVm(
                            i,

                            brokerId,

                            VM_MIPS,

                            VM_PES,

                            VM_RAM,

                            VM_BW,

                            VM_SIZE,

                            1,

                            "Xen",

                            new CloudletSchedulerDynamicWorkload(
                                    VM_MIPS,
                                    VM_PES
                            ),

                            SCHEDULING_INTERVAL
                    );

            list.add(vm);

            Log.println(
                    "VM #"
                            + i
                            + " created"
            );
        }

        Log.println();

        return list;
    }

    // CREATE CLOUDLETS

    private static List<Cloudlet> createCloudlets(
            int brokerId
    ) {

        List<Cloudlet> list = new ArrayList<>();

        for (int i = 0; i < VM_COUNT; i++) {

            Cloudlet cl =
                    new Cloudlet(
                            i,

                            CLOUDLET_LENGTH,

                            CLOUDLET_PES,

                            CLOUDLET_FILE_SIZE,

                            CLOUDLET_OUTPUT_SIZE,

                            new UtilizationModelFull(),

                            new UtilizationModelNull(),

                            new UtilizationModelNull()
                    );

            cl.setUserId(brokerId);

            cl.setGuestId(i);

            list.add(cl);
        }

        Log.println("CLOUDLETS");

        Log.println("Cloudlets CPU utilization = 100%");

        Log.println();

        return list;
    }

    // RESULTS

    private static void printResults(
            PowerDatacenter dc,
            double clock
    ) {

        DecimalFormat df =
                new DecimalFormat("###.##");

        Log.println("\n------------------RESULTS------------------");

        Log.println(
                "Simulation time = "
                        + df.format(clock)
                        + " sec"
        );

        Log.println(
                "Migrations = "
                        + dc.getMigrationCount()
        );

        Log.println(
                "Energy = "
                        + df.format(
                        dc.getPower() / 3600
                )
                        + " Wh"
        );
    }
}