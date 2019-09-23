
package saqib.rasul.client;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import saqib.rasul.Compute;
import saqib.rasul.RmiStarter;

/**
 * get the RMI Compute service and send a task to compute PI to N digits
 * 
 * @author srasul
 * 
 */
public class StartComputeTaskPI
    extends RmiStarter {

    public StartComputeTaskPI() {
        super(PI.class);
    }

    @Override
    public void doCustomRmiHandling() {
        try {
            Registry registry = LocateRegistry.getRegistry();
            Compute compute = (Compute)registry.lookup(Compute.SERVICE_NAME);
            PI task = new PI(50);
            BigDecimal pi = compute.executeTask(task);
            System.out.println("computed pi: " + pi);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        new StartComputeTaskPI();
    }
}
