import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class ConnectionManager implements Display{

	public static final int DEFAULT_PORT = 10000;
	
	private CopyOnWriteArrayList<Client> clients;
	private CopyOnWriteArrayList<Server> servers;
	private CopyOnWriteArrayList<PeerInfo> peers;
	private CopyOnWriteArrayList<Display> displays;
	private CopyOnWriteArrayList<ConnectionMonitor> monitors;
	private ServerSocket srvSocket;
	private RandomAccessFile rFile;
	private BitSet pieces;
	private FileInfo fInfo;
	
	public ConnectionManager(FileInfo fi)  throws IOException{
		this(fi, new BitSet());
	}
	
	public ConnectionManager(FileInfo fi, BitSet pieces) throws IOException{

		servers = new CopyOnWriteArrayList<Server>();
		clients = new CopyOnWriteArrayList<Client>();
		peers = new CopyOnWriteArrayList<PeerInfo>();
		displays = new CopyOnWriteArrayList<Display>();
		monitors = new CopyOnWriteArrayList<ConnectionMonitor>();
		
		fInfo = fi;
		rFile = new RandomAccessFile(fInfo.getFilePath(), "rw");
		rFile.setLength(fInfo.getFileSize());
		this.pieces = (BitSet)pieces.clone();

	}
	
	public void addMonitor(ConnectionMonitor m){
			monitors.add(m);
	}
	
	public void addDisplay(Display d){
			displays.add(d);
	}
	
	public void removeDisplay(Display d){
			displays.remove(d);
	}
	
	public void write(String message, int level){
		for(Display d: displays){
			d.write(message, level);
		}
	}
	

	public int startServer(){
		int port = -1;
		try {
			srvSocket = new ServerSocket();
			srvSocket.bind(null); //null choses random empheral port
			port = srvSocket.getLocalPort();	
		}catch (IOException ioe){
			write("Server Error: "+ioe.getMessage(),0);
			return port;
		}
	
			Thread t = new Thread(new Runnable(){
				public void run(){
					try { 
						while(true){
							Server s = new Server(ConnectionManager.this, srvSocket.accept());
							Thread t = new Thread(s);
							t.start();
							register(s);
						}
					} catch(IOException ioe){
						write("Server Error: "+ioe.getMessage(), 0);
					}
				}
			});
			t.setDaemon(true);
			t.start();
			write("Server Port: "+port, 0);
			write("Server File: "+fInfo, 0);
			return port;
	}
	
	public void addClient(PeerInfo p){
		try{
			if(!peers.contains(p)){
				peers.add(p);
				Client c = new Client(this, p);
				register(c);
				Thread t = new Thread(c);
				t.start();
			}
		} catch (IOException ioe){
			write("Error starting Client for :"+p,0);
		}
	}
	
	public void addClient(PeerInfo[] ps){
		for(PeerInfo pi : ps){
			addClient(pi);
		}
	}
		
	public void unregister(Server s){
		int n;
		servers.remove(s);
		s.close();
		n = servers.size();
		serversMonitor(n);
		write("Servers Stopped: "+s, 0);
		
	}
	
	private void register(Server s){
		int n;
		servers.add(s);
		n = servers.size();
		serversMonitor(n);
		write("Server Started: "+s, 0);
	}
	
	private void register(Client c){
		int n;
		clients.add(c);
		n = clients.size();
		clientsMonitor(n);
		write("Client Started: "+c, 0);
	}
	
	public void unregister(Client c){
		int n;
		clients.remove(c);
		peers.remove(c);
		n = clients.size();
		clientsMonitor(n);
		write("Client Stopped: "+c, 0);
	}
	
	private long getFileOffset(int pNo){
		//cast as long due to overflow bug in >2GB files
		return (long)pNo * (long)fInfo.getPieceSize();
	}
	
	private int duplicates = 0;
	public synchronized void writePiece(int pNo, byte[] data) throws IOException{
		if(!pieces.get(pNo)){
			rFile.seek(getFileOffset(pNo));
			rFile.write(data);
			pieces.set(pNo);
			sendPieceUpdate(pNo);
		} else {
			duplicates++;
			duplicatesMonitor(duplicates);
			//display.write("duplicate piece", 0);
		}
	}
	
	private void sendPieceUpdate(int pNo){
		PieceUpdateMessage update = new PieceUpdateMessage(pNo);
		for(Server s : servers){
			s.sendMessage(update);
		}
	}

	
	private int getPieceSize(int i){
		return i < fInfo.getNumberOfPieces() - 1 
					? fInfo.getPieceSize() 
					: (int)(fInfo.getFileSize() - ((fInfo.getNumberOfPieces() - 1)* fInfo.getPieceSize()));
	}
	
	public synchronized byte[] readPiece(int pNo) throws IOException{
		byte[] data = new byte[getPieceSize(pNo)];
		rFile.seek(getFileOffset(pNo));
		rFile.readFully(data);
		return data;
	}
	
	public BitSet getPieces(){
		return (BitSet)pieces.clone();
	}
	
	public FileInfo getFileInfo(){
		return fInfo;
	}
	

	private void piecesMonitor(int h, int t){
		for(ConnectionMonitor m: monitors){
			m.pieces(h, t);
		}
	}
	
	private void serversMonitor(int s){
		for(ConnectionMonitor m: monitors){
			m.servers(s);
		}
	}
	
	private void clientsMonitor(int c){
		for(ConnectionMonitor m: monitors){
				m.clients(c);
		}
	}
	
	private void duplicatesMonitor(int d){
		for(ConnectionMonitor m: monitors){
			m.duplicates(d);
		}
	}
	
	private int startIndex = 0;
	private boolean finished = false;
	
	public synchronized int nextPiece(BitSet peer){
		//clone the pieces we have
		BitSet clone = (BitSet)pieces.clone();
		int have = clone.cardinality();
		int total = fInfo.getNumberOfPieces();
		piecesMonitor(have, total);
		if (have >= total) {
			finished = true;
			//write("File Complete", 0);
			return -2;
		}
		//flip to determine pieces we need
		clone.flip(0, fInfo.getNumberOfPieces());
		
		//determine which pieces we need and our peer has
		clone.and(peer);
		
		int retIndex = clone.nextSetBit(startIndex);
		if (retIndex < 0) {
			retIndex = clone.nextSetBit(0);
		}
		//TODO (optional) some randomness
		startIndex = (retIndex + 1 + (int)(fInfo.getNumberOfPieces()*0.1))%fInfo.getNumberOfPieces();
		
		return retIndex;
		
	}
	

	//returns true if all pieces downloaded
	public synchronized  boolean isFinished(){
		return finished;
	}
	
	
}
