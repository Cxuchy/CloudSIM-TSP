package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CC {

    public static DatacenterBroker broker;

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public static void main(String[] args) {

        Log.println("Starting CloudSimExample1...");

        List<Long> lengths = new ArrayList<>();
        List<Double> execTimes = new ArrayList<>();

        long[] cloudletLengths = {100000, 200000, 400000, 600000, 800000};

        try {

            for(long lengthValue : cloudletLengths){

                int num_user = 1;
                Calendar calendar = Calendar.getInstance();
                boolean trace_flag = false;

                CloudSim.init(num_user, calendar, trace_flag);

                Datacenter datacenter0 = createDatacenter("Datacenter_0");

                broker = new DatacenterBroker("Broker");
                int brokerId = broker.getId();

                vmlist = new ArrayList<>();

                int vmid = 0;
                int mips = 1000;
                long size = 10000;
                int ram = 512;
                long bw = 1000;
                int pesNumber = 1;
                String vmm = "Xen";

                Vm vm = new Vm(
                        vmid,
                        brokerId,
                        mips,
                        pesNumber,
                        ram,
                        bw,
                        size,
                        vmm,
                        new CloudletSchedulerTimeShared()
                );

                vmlist.add(vm);
                broker.submitGuestList(vmlist);

                cloudletList = new ArrayList<>();

                int id = 0;
                long fileSize = 300;
                long outputSize = 300;

                UtilizationModel utilizationModel = new UtilizationModelFull();

                Cloudlet cloudlet = new Cloudlet(
                        id,
                        lengthValue,
                        pesNumber,
                        fileSize,
                        outputSize,
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

                lengths.add(lengthValue);
                execTimes.add(time);

                Log.println("Cloudlet Length: " + lengthValue +
                        " Execution Time: " + time);
            }

            plotResults(lengths, execTimes);

            Log.println("CloudSimExample1 finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Error occurred");
        }
    }

    private static Datacenter createDatacenter(String name) {

        List<Host> hostList = new ArrayList<>();

        List<Pe> peList = new ArrayList<>();

        int mips = 1000;

        peList.add(new Pe(new PeProvisionerSimple(mips)));

        int ram = 2048;
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

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";

        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        arch,
                        os,
                        vmm,
                        hostList,
                        time_zone,
                        cost,
                        costPerMem,
                        costPerStorage,
                        costPerBw
                );

        Datacenter datacenter = null;

        try {

            datacenter = new Datacenter(
                    name,
                    characteristics,
                    new VmAllocationPolicySimple(hostList),
                    storageList,
                    0
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static void plotResults(List<Long> lengths, List<Double> times){

        XYSeries series = new XYSeries("Execution Time");

        for(int i = 0; i < lengths.size(); i++){
            series.add(lengths.get(i), times.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Cloudlet Length vs Execution Time",
                "Cloudlet Length (MI)",
                "Execution Time",
                dataset
        );

        ChartFrame frame = new ChartFrame("Simulation Plot", chart);
        frame.setSize(800,600);
        frame.setVisible(true);
    }

}