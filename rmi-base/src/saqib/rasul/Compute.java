package saqib.rasul;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Compute extends Remote {
    
    public static final String SERVICE_NAME = "ComputeEngine";
    
    <T> T executeTask(Task<T> t) throws RemoteException;
}
