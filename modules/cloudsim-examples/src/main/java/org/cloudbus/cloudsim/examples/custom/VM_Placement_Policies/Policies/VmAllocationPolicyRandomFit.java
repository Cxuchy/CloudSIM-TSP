package org.cloudbus.cloudsim.examples.custom.VM_Placement_Policies.Policies;

import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Random Fit VM allocation policy for CloudSim 7G.
 *
 * findHostForGuest() is the only abstract method to implement.
 * Random Fit rule: among all hosts that can fit the VM, pick one randomly.
 */
public class VmAllocationPolicyRandomFit extends VmAllocationPolicy {

    public VmAllocationPolicyRandomFit(List<? extends HostEntity> hostList) {
        super(hostList);
    }

    @Override
    public HostEntity findHostForGuest(GuestEntity guest) {
        List<HostEntity> suitableHosts = new ArrayList<>();

        for (HostEntity host : getHostList()) {
            if (host.isSuitableForGuest(guest)) {
                suitableHosts.add(host);
            }
        }

        if (suitableHosts.isEmpty()) {
            return null; // No suitable host found
        }

        // Pick a random host from the list of suitable hosts
        int randomIndex = (int) (Math.random() * suitableHosts.size());
        return suitableHosts.get(randomIndex);
    }
}