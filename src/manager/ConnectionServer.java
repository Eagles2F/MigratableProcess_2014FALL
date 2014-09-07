package manager;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class ConnectionServer implements Runnable{
    private ProcessManager manager;
    private int portNum;
    private int workerCnt;
    private volatile boolean running;
    ServerSocket serverSocket;
    public ConnectionServer(int port,ProcessManager procManager){
       portNum = port;
       manager = procManager;
       workerCnt = 0;
       running = true;
       try {
        serverSocket = new ServerSocket(portNum);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.println("failed to create the socket server");
        System.exit(0);
    }
       System.out.println("create connctionServer");
    }
    @Override
    public void run(){
       
       
       try{
           
           
           System.out.println("waiting for worker join in");
           while(true){
               Socket workerSocket = serverSocket.accept();
               System.out.println("worker: "+workerSocket.getInetAddress()+":"+workerSocket.getPort()+" join in");
               manager.workersMap.put(workerCnt, workerSocket);
               
               ManagerServer procServer = new ManagerServer(manager,workerCnt,workerSocket);
               manager.processServerMap.put(workerCnt, procServer);
               new Thread(procServer).start();
               workerCnt++;
           } 
       }catch(IOException e){
           e.printStackTrace();
           System.out.println("socket server accept failed");
       }
       try {
        serverSocket.close();
       } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.println("socket Server failed to close");
    }
        
    }
    
    public void stop(){
        running = false;
    }
    
}
