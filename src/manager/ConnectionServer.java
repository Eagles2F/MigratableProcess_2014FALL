package manager;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class ConnectionServer implements Runnable{
    private ProcessManager manager;
    int portNum;
    int workerCnt;
    public ConnectionServer(int port,ProcessManager procManager){
       portNum = port;
       manager = procManager;
       workerCnt = 0;
       System.out.println("create connctionServer");
    }
    @Override
    public void run(){
       ServerSocket serverSocket;
       
       try{
           
           serverSocket = new ServerSocket(portNum);
           System.out.println("waiting for worker join in");
           while(true){
               Socket workerSocket = serverSocket.accept();
               System.out.println("worker: "+workerSocket.getInetAddress()+":"+workerSocket.getPort()+" join in");
               manager.workersMap.put(workerCnt, workerSocket);
               
               ManagerServer procServer = new ManagerServer(manager,workerCnt,workerSocket);
               new Thread(procServer).start();
               workerCnt++;
           } 
       }catch(IOException e){
           e.printStackTrace();
       }
        
    }
    
}
