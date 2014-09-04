package worker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;




import java.util.HashMap;

import utility.Message;
/*
 * The workerNode is the slave of the process manager. It is a seperate running machine which is connected 
 * to the process manager with socket. When it is started, the port and IP address of the processmanager
 * is set.  Its responsibilities including:
 * 	Response to the CMD from the process manager.
 *  send message every 5 seconds to the process manager to inform its own status. 
 *  
 *  @Author Yifan Li
 *  @Author Jian Wang
 **/
import utility.Message.msgType;
import utility.ResponseType;
import processes.MigratableProcess;

public class WorkerNode {
// properties
	// socket related properties
	private String host;
	private int port;
	private Socket socket;
	
	// object I/O port
	private ObjectInputStream obis;
	private ObjectOutputStream obos;

	private boolean failure = false; // failure == ture means the process died!
	private int workerID;
	// process related properties
	private Thread t;
	private HashMap<Integer, MigratableProcess> currentMap;
	
// methods
 	//constructing method
	public WorkerNode(){
		this.host = null;
		this.port = 0;
		this.workerID = 0;
		this.currentMap = new HashMap<Integer, MigratableProcess>();
	}
	public WorkerNode(String host, int port){
		this.host=host;
		this.port = port;
		this.workerID = 0;
		this.currentMap = new HashMap<Integer, MigratableProcess>();
	}
	
	//command handling methods
	private void handle_kill(Message msg) {
		//response message prepared!
		Message response = new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.KILLRES);
		MigratableProcess mp = currentMap.remove(msg.getProcessId());
		mp.exit();
		response.setProcessId(mp.getProcessID());
		response.setResult(Message.msgResult.SUCCESS);
		
		//send the response back
		sendToManager(response);		
	}
	
	// receive the process from the process manager
	private void handle_migratetarget(Message msg) {
		//response prepared
		Message response = new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.MIGRATETARGETRES);
		response.setProcessId(msg.getProcessId());;
		response.setWorkerID(workerID);
		//continue the process here£¡
		MigratableProcess mp = msg.getProcessObject();
		System.out.println("object received, ready to go!");
		runProcess(mp);
		
		response.setResult(Message.msgResult.SUCCESS);
		
		//send the response back
		sendToManager(response);
	}
	
	// handle the migrate from command. The worker received should package the process and diliver it to the master
	private void handle_migratesource(Message msg) {
		// response prepared!
		Message response = new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.MIGARATESOURCERES);
		// package the process to be transfered!
		MigratableProcess mp = currentMap.get(msg.getProcessId());
		mp.suspend();
		currentMap.remove(mp.getProcessID());
		response.setTargetId(msg.getTargetId());
		response.setProcessObject(mp);
		response.setResult(Message.msgResult.SUCCESS);
		
		// send the response
		sendToManager(response);
	}
	// using reflection to construct the process and find the class by the name of it
	private void handle_start(Message msg) {
		// response message prepared!
		Message response=new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.STARTRES);
		response.setProcessId(msg.getProcessId());
		response.setProcessName(msg.getProcessName());
		//start the process
		Class processClass;
		try {
			processClass = Process.class.getClassLoader().loadClass(
					msg.getProcessName());
			Constructor constructor;
			constructor = processClass.getConstructor(String[].class);
			Object[] passed = { msg.getArgs() };
			MigratableProcess process = (MigratableProcess) constructor.newInstance(passed);
			process.setProcessID(msg.getProcessId()); // method needed to be created in MigratableProcess
			runProcess(process);
		}catch (ClassNotFoundException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Class not Found!");
		}catch (NoSuchMethodException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("No such method!");
		} catch (SecurityException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Security Exception!");
		} catch (InstantiationException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Instantiation Exception!");
		} catch (IllegalAccessException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Illegal Access !");
		} catch (IllegalArgumentException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Illegal Argument!");
		} catch (InvocationTargetException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Invocation Target Exception!");
		}
		// send the response back to the master
		response.setResult(Message.msgResult.SUCCESS);
		sendToManager(response);
	}
	
	 // handle the command assign id 
	private void handle_assignID(Message msg){
		this.workerID =msg.getWorkerID(); 
	}
	
	// hanld the command exit
	
	private void handle_exit(Message msg){
		System.exit(0);
	}
	
	// some auxiliary methods
	
		// create a thread to run the process
		private  void runProcess(MigratableProcess mp) {
			System.out.println("start process");
			t = new Thread(mp);
			t.start();
			currentMap.put(mp.getProcessID(), mp);
		}
		
		//send method writes object into output stream
		private void sendToManager(Message sc) {
			try {
				obos.writeObject(sc);
			} catch (IOException e) {
				System.err.println("fail to send manager");
			}
		}
		
		
	public static void main(String [] args){
		//start only when there are two arguments 
		if(args.length == 2){
			// establish the connection first.
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			WorkerNode worker = new WorkerNode(host, port);
			
			try{
				worker.socket = new Socket(host,port);
			}catch(IOException e){
				worker.failure = true;
				System.err.println("Socket creation failure!");
				System.exit(0);
			}
			System.out.println("Socket creation succeded!");
			
			//establish the object IO tunnel

			try {
				worker.obos = new ObjectOutputStream(
						worker.socket.getOutputStream());
				worker.obis = new ObjectInputStream(
						worker.socket.getInputStream());
			} catch (IOException e) {
				worker.failure = true;
				System.err.println("cannot create stream");
				e.printStackTrace();
			}
			
			//wait for the CMDs and deal with them
			while(!worker.failure){
				try {
					Message master_cmd = (Message) worker.obis.readObject();
					switch(master_cmd.getCommandId()){
						case ASSIGNID:// this command assignes the id to the process
							worker.handle_assignID(master_cmd);
							break;
						case START:// this command tries to start a process on this worker
							worker.handle_start(master_cmd);
							break;
						case MIGARATESOURCE: // this command tries to migrate a process from this worker
							worker.handle_migratesource(master_cmd);
							break;
						case MIGRATETARGET: // this command tries to migrate a process to this worker
							worker.handle_migratetarget(master_cmd);
							break;
						case KILL:	// this command tries to kill a process on this worker
							worker.handle_kill(master_cmd);
							break;
						case SHUTDOWN:// this command shutdown this worker
							worker.handle_exit(master_cmd);
							break;
						default:
							System.out.println("Wrong cmd:"+master_cmd.getCommandId());
							break;
					}
					
				} catch (ClassNotFoundException e) {
					worker.failure = true;
					System.out.println("Class not found!");
				} catch (IOException e) {
					worker.failure = true;
					System.out.println("Cannot read from the stream!");
				}
			}
			try {
				worker.obos.close();
				worker.obis.close();
				worker.socket.close();
				System.out.println("Process Worker closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("Please enter the host ip and port number");
		}
	}

}
