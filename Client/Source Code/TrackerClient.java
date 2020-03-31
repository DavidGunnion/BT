import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;

public class TrackerClient {
    private String host;
    private int port;

    private Socket theSocket;
    private PrintWriter out;
    private BufferedReader in;
    
    public TrackerClient (String h, int p) throws IOException
    {
        this.host = h; 
        this.port = p;

        theSocket = new Socket(host, port);
      	out = new PrintWriter(theSocket.getOutputStream(), true);
       	in = new BufferedReader(new InputStreamReader(theSocket.getInputStream()));

    }
    
    public int getPort(){
    	return theSocket.getLocalPort();
    }

    public void registerComplete(FileInfo f, int port){
        String request = "registerComplete|" + f.getFileName() + "|" + f.getFileSize() + "|" + f.getPieceSize() + "|" + port;
        System.out.println(process(request));
        //System.out.println("File seed registered: " + f + " at port: " + p);
    }

    public void registerPartial(FileInfo f, int port){
        String request = "registerPartial|" + f.getFileName() + "|" + f.getFileSize() + "|" + f.getPieceSize() + "|" + port;
        System.out.println(process(request));
    }

    public String[] getList(){
    	String response = process("getList");

    	return (response.length() == 0) ?  new String[0] : filterGetList(response);
 
    }
    
    private String[] filterGetList(String response){
    	TreeSet<String> ts = new TreeSet<String>();
    	for(String s : response.split("\\|")){
        	ts.add(s);
    	}
    	return ts.toArray(new String[0]);
    }
    
    public FileInfo getFileInfo (String filename){
        String response = process("getFileInfo|" + filename);
        if(response == "") return null;
        
        String[] reply = response.split("\\|");
        
        return new FileInfo(reply[0], Long.parseLong(reply[1]), Integer.parseInt(reply[2]));
    }

    public synchronized PeerInfo[] getPeers(FileInfo f){
        String request = "getPeers|" + f.getFileName() + "|" + f.getFileSize() + "|" + f.getPieceSize();
        String response = process (request);
       
        String[] reply = (response.length() == 0) ? new String[0] : response.split("\\|");
        
        PeerInfo[] peerInfo = new PeerInfo[reply.length];
        //System.out.println("rere"+reply.length);
        for(int i = 0; i < peerInfo.length; i++){
            String[] data = reply[i].split(":");
            peerInfo[i] = new PeerInfo(data[0], Integer.parseInt(data[1]));
        }
        
        return peerInfo;
    }

    public void unregister(int p){
       System.out.println(process("unregister"));
       try {
       in.close();
       out.close();
       theSocket.close();
       } catch (IOException ioe){
    	   //ignore
       }
    }

    private String process(String request){
    	try {
    		out.println(request);
    		return in.readLine();
    	} catch(IOException ioe){
    		System.err.println(ioe.getMessage());
    		System.exit(-1);
    		return"";
    	}
    }
	
}