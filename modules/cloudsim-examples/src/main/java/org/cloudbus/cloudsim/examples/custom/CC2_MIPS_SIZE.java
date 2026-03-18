package org.cloudbus.cloudsim.examples.custom;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CC2_MIPS_SIZE {

    public static DatacenterBroker broker;
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public static void main(String[] args) {

        System.out.println("Starting VM Processing Power Experiment...");

        List<Integer> mipsValues = new ArrayList<>();
        List<Double> execTimes = new ArrayList<>();

        int[] mipsArray = {250, 500, 1000, 2000, 4000};

        try {

            for(int mips : mipsArray){

                int num_user = 1;
                Calendar calendar = Calendar.getInstance();
                boolean trace_flag = false;

                CloudSim.init(num_user, calendar, trace_flag);

                Datacenter datacenter0 = createDatacenter("Datacenter_0");

                broker = new DatacenterBroker("Broker");
                int brokerId = broker.getId();

                vmlist = new ArrayList<>();

                int vmid = 0;
                int pesNumber = 1;
                int ram = 512;
                long bw = 1000;
                long size = 10000;

                Vm vm = new Vm(
                        vmid,
                        brokerId,
                        mips,
                        pesNumber,
                        ram,
                        bw,
                        size,
                        "Xen",
                        new CloudletSchedulerTimeShared()
                );

                vmlist.add(vm);
                broker.submitGuestList(vmlist);

                cloudletList = new ArrayList<>();

                long length = 400000;

                UtilizationModel utilizationModel = new UtilizationModelFull();

                Cloudlet cloudlet = new Cloudlet(
                        0,
                        length,
                        pesNumber,
                        300,
                        300,
                        utilizationModel,
                        utilizationModel,
                        utilizationModel
                );

                cloudlet.setUserId(brokerId);
                cloudlet.setGuestId(vmid);

                cloudletList.add(cloudlet);
                broker.submitCloudletList(cloudletList);

                CloudSim.startSimulation();
                CloudSim.stopSimulation();

                List<Cloudlet> newList = broker.getCloudletReceivedList();

                double time = newList.get(0).getActualCPUTime();

                mipsValues.add(mips);
                execTimes.add(time);

                System.out.println("VM MIPS: " + mips + " Execution Time: " + time);
            }

            plotResults(mipsValues, execTimes);

            System.out.println("Simulation Finished");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) {

        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();

        int mips = 4000;

        peList.add(new Pe(new PeProvisionerSimple(mips)));

        int ram = 4096;
        long storage = 1000000;
        int bw = 10000;

        hostList.add(
                new Host(
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
                )
        );

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        "x86",
                        "Linux",
                        "Xen",
                        hostList,
                        10.0,
                        3.0,
                        0.05,
                        0.001,
                        0.0
                );

        Datacenter datacenter = null;

        try {

            datacenter = new Datacenter(
                    name,
                    characteristics,
                    new VmAllocationPolicySimple(hostList),
                    new LinkedList<>(),
                    0
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    public static void plotResults(List<Integer> mips, List<Double> time){

        XYSeries series = new XYSeries("Execution Time");

        for(int i=0;i<mips.size();i++){
            series.add(mips.get(i), time.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "VM Processing Power vs Execution Time",
                "VM MIPS",
                "Execution Time",
                dataset
        );

        ChartFrame frame = new ChartFrame("VM Power Plot", chart);
        frame.setSize(800,600);
        frame.setVisible(true);
    }
}