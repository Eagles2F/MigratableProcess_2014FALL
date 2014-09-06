package utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.RandomAccessFile;

import java.io.File;
/*the TransctionalFileInputStream class will remember the state of the file and use
 * RandmAcessFile to seek to the offset and continue the reading. It's useful when migrate the process*/
public class TransactionalFileInputStream extends InputStream implements
Serializable{
    private File sourceFile;
    private int offSet;
    boolean migrateState;
    private transient RandomAccessFile fileHdl;
    public TransactionalFileInputStream(String string) {
        offSet =0;
        migrateState = false;
        sourceFile = new File(string);
        
    }

    @Override
    /*when migrate, need to reopen the file and seek to the offset position*/
    public int read() throws IOException {
        int result;
        if(migrateState || (fileHdl == null)){
           fileHdl = new RandomAccessFile(sourceFile,"r"); 
           fileHdl.seek(offSet);
           migrateState = false;
        }
           result = fileHdl.read();
           offSet++;
           return result;
        
    }
    @Override
    public void close() throws IOException {
        fileHdl.close();
    }
    
    public void setMigrateState(boolean state){
        migrateState = state;
    }
    
}