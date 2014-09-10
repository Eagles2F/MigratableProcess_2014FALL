package processes;
import java.io.PrintStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Thread;
import java.lang.InterruptedException;

import utility.*;
import utility.ProcessInfo.Status;

public class GrepProcess extends MigratableProcess
{
	private TransactionalFileInputStream  inFile;
	private TransactionalFileOutputStream outFile;
	private String query;
	
	private volatile boolean suspending;

	public GrepProcess(String args[]) throws Exception
	{
		if (args.length != 3) {
			System.out.println("usage: GrepProcess <queryString> <inputFile> <outputFile>");
			throw new Exception("Invalid Arguments");
		}
		
		query = args[0];
		inFile = new TransactionalFileInputStream(args[1]);
		outFile = new TransactionalFileOutputStream(args[2]);
		this.complete =false;
	}

	public void run()
	{
		PrintStream out = new PrintStream(outFile);
		DataInputStream in = new DataInputStream(inFile);
		
		try {
		
			while (!suspending) {
				String line = in.readLine();

				if (line == null) break;
				
				if (line.contains(query)) {
					out.println(line);
				}
				
				// Make grep take longer so that we don't require extremely large files for interesting results
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore it
				}
			}
		//}
		} catch (EOFException e) {
			//End of File
		    System.out.println("GrepProcess: end of file");
		} catch (IOException e) {
			System.out.println ("GrepProcess: Error: " + e);
		}

		if(suspending == true){
    		System.out.println("set suspending false");
    		suspending = false;
		}else{
		    complete = true;
		    System.out.println("GrepProcess complete");
		}
	}

	public void suspend()
	{
		suspending = true;
		while(suspending);
            
	}

	
    

}