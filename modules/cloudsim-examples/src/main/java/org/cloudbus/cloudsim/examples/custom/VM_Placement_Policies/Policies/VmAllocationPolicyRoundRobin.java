package org.cloudbus.cloudsim.examples.custom.VM_Placement_Policies.Policies;

import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Round Robin VM allocation policy for CloudSim 7G.
 *
 * findHostForGuest() distributes VMs cyclically across all hosts :
 *   VM#0 → Host#0
 *   VM#1 → Host#1
 *   VM#2 → Host#2
 *   VM#3 → Host#0  (cycle restarts)
 *   ...
 *
 * If the selected host cannot fit the VM (not enough resources),
 * the algorithm keeps cycling until it finds a suitable one.
 * Returns null if no host can accommodate the VM.
 */
public class VmAllocationPolicyRoundRobin extends VmAllocationPolicy {

    /** Index of the next host to try — persists across calls */
    private int lastIndex = 0;

    public VmAllocationPolicyRoundRobin(List<? extends HostEntity> hostList) {
        super(hostList);
    }

    @Override
    public HostEntity findHostForGuest(GuestEntity guest) {
        List<? extends HostEntity> hosts = getHostList();
        int numHosts = hosts.size();

        // Try every host starting from lastIndex, cycling around
        for (int i = 0; i < numHosts; i++) {
            int index = (lastIndex + i) % numHosts;
            HostEntity host = hosts.get(index);

            if (host.isSuitableForGuest(guest)) {
                // Advance the pointer for the NEXT VM
                lastIndex = (index + 1) % numHosts;
                return host;
            }
        }

        // No suitable host found
        return null;
    }
}