import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable {
	
	private FileInfo fInfo;
	private Socket socket;
	private BitSet peerPieces; 
	private ConnectionManager manager;
	private DataInputStream in;
	private DataOutputStream out;
	private String ID;
	
	public Client(ConnectionManager cm, PeerInfo pi) throws IOException{
		fInfo = cm.getFileInfo();
		manager = cm;
		socket = new Socket(pi.getHost(), pi.getport());
		ID = "Client "+socket.getInetAddress().getHostAddress()+":"+socket.getPort()+" ";
		manager.write("Client connected to "+ID, 0);
		peerPieces = new BitSet(fInfo.getNumberOfPieces());
		in = new DataInputStream(new BufferedInputStream( socket.getInputStream() ));
		out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream() ));
	}
	
	public void run() {
		int pieces = 0;
		int pr = 0;
		
		try {
			//manager.write("Client Started", 0);

			sendMessage(new FileRequestMessage(manager.getFileInfo()));

			readPieceInfo();

			for(int p = manager.nextPiece(peerPieces); p >= -1; p = manager.nextPiece(peerPieces)){
				//System.out.println("in loop "+p);
				//System.out.println("pr "+pr);
				if (p != -1 && pr < 1) {
					sendMessage(new PieceRequestMessage(p));
					pr++;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException ie){
					System.err.println("Thread interrupted");
				}
				if(in.available() > 0){
					int type = in.readUnsignedByte();
					//System.out.println(type+" klj");
					switch (type){
						case 4:
							pr--;
							pieces++;
							readPiece();
							break;
						case 5:
							pieceUpdate();
							break;
							
					}
				}

			} 
			sendMessage(new FinishedMessage());
		
		} catch (IOException e){
			manager.write("Caught Exception: "+ID+e.getMessage(), 0);
		} finally {
			try {
				manager.unregister(this);
				manager.write(ID+"Pieces: "+pieces, 0);
				in.close();
				out.close();
				socket.close();
			} catch(IOException ioe){
				//ignore
			}
		}
		
	}
	
	private void pieceUpdate() throws IOException{
		int pNo = in.readInt();
		peerPieces.set(pNo);
		manager.write(ID+"->5 "+pNo, 0);
		//System.exit(43);
	}
	
	private void readPieceInfo() throws IOException{
		int type = in.readByte();
		int totalPieces = in.readInt();
		int peerHas = in.readInt();
		if(totalPieces == peerHas){
			peerPieces.flip(0, fInfo.getNumberOfPieces());
		} else if(peerHas == 0) {
			//do nothing
		} else {
			int arraySize = (int)Math.ceil(((double)manager.getFileInfo().getNumberOfPieces())/8);
			byte[] pieces = new byte[arraySize];
			in.readFully(pieces);
			for (int i=0; i < 8*pieces.length; i++) {
	            if ( ( pieces[i/8] & (0x80 >>> (i%8)) ) > 0 ) {
	                peerPieces.set(i);
	            }
	        }
		}

		manager.write(ID+"-> "+type+" "+totalPieces+" "+peerHas+" "+(peerHas > 0 && peerHas != totalPieces), 0);
		
	}
	
	public void sendMessage(Message msg) throws IOException{
		manager.write(ID+msg, 0);
		synchronized (out) {
			out.write(msg.toBytes());
			out.flush();
		}

	}
	
	
	private void readPiece() throws IOException{
		int type = 4;//in.readByte();
		int pNo = in.readInt();
		int size = in.readInt();
		byte[] data = new byte[size];
		in.readFully(data);
		manager.write(ID+"->"+type+" "+pNo+" "+size+" "+"[DATA]", 0);
		manager.writePiece(pNo, data);
	}
	
	public String toString(){
		return ID;
	}
	
}
