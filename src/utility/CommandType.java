package utility;

/**
* This file define the command type used to send from manager to worker and 
* the response type used to send from worker to manager
* @Author Yifan Li
* @Author Jian Wang
*/

public enum CommandType {
    PULLINFO,
    START,
    MIGARATESOURCE,/*manager to source worker*/
    MIGRATETARGET, /*manager to target worker*/
    KILL,
    SHUTDOWN, 
    ASSIGNID,
    REMOVEPROC
}

