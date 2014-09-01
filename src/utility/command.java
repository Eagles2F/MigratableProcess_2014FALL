package utility;
import java.io.Serializable;
import processes.MigratableProcess;
/**
* command class used to send between manager and worker
*/
public class command implements Serializable {
    private int commandId;
    private int processId;
    private String processName;
    private String[] args;
    private MigratableProcess processObject;
    private int sourceNode;
    private int targetNode;
    
    
}