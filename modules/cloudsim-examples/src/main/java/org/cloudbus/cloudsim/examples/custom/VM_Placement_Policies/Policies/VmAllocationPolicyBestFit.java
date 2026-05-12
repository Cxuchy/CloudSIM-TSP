package org.cloudbus.cloudsim.examples.custom.VM_Placement_Policies.Policies;

import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Best Fit VM allocation policy for CloudSim 7G.
 *
 * findHostForGuest() is the only abstract method to implement.
 * Best Fit rule: among all hosts that can fit the VM,
 * pick the one with the LEAST remaining MIPS (tightest fit).
 */
public class VmAllocationPolicyBestFit extends VmAllocationPolicy {

    public VmAllocationPolicyBestFit(List<? extends HostEntity> hostList) {
        super(hostList);
    }

    @Override
    public HostEntity findHostForGuest(GuestEntity guest) {
        HostEntity bestHost     = null;
        double     minRemaining = Double.MAX_VALUE;

        for (HostEntity host : getHostList()) {
            // Skip hosts that cannot accommodate this guest
            if (!host.isSuitableForGuest(guest)) continue;

            double remaining = host.getTotalMips();

            // Best Fit: choose the host with the smallest remaining capacity
            // that is still large enough to host the VM
            if (remaining < minRemaining) {
                minRemaining = remaining;
                bestHost     = host;
            }
        }

        return bestHost; // null = no suitable host found (CloudSim handles this)
    }
}