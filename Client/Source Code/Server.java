import java.io.*;
import java.net.*;

public class Server implements Runnable{
	
	private Socket socket;
	private ConnectionManager manager;
	private DataInputStream in;
	private DataOutputStream out;
	private String ID;
	private boolean twosent;
	
	public Server(ConnectionManager cm, Socket s) throws IOException{
		ID = "Server "+s.getInetAddress().getHostAddress()+":"+s.getPort()+" ";
		manager = cm;
		manager.write("Connected to "+ID, 0);
		socket = s;
		in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		twosent = false;
	}
	
	public void run(){
		try {
			socket.setSoTimeout(3 * 60 * 1000);
			readFileRequest();

			sendMessage(new FileOfferMessage(
					manager.getFileInfo().getNumberOfPieces(),
					manager.getPieces().cardinality(),
					manager.getPieces())
			);
			twosent = true;
			
			boolean finished = false;
			while(!finished){
				//System.err.println("closed="+socket.isClosed());
				
				/*try {
					Thread.sleep(10);
				} catch (InterruptedException ie){
					System.err.println("Thread interrupted");
				}*/
				int type = in.readByte();
				switch(type){
					case 3:
						int pieceNo = in.readInt();
						manager.write(ID+"-> "+type+" "+pieceNo,0);
						sendMessage(new PieceTransferMessage(pieceNo, manager.readPiece(pieceNo)));
						break;
					case 6:
						finished = true;
						manager.write(ID+"-> "+type+" ",0);
						break;
				}

				

			}
		

		} catch (SocketTimeoutException ste){
			manager.write(ID+"TIMED OUT", 0);
		}
		catch(IOException e){
			manager.write("Caught Exception: "+ID+e.getMessage()+" "+e.getClass(), 0);
			//System.err.println(ID+e.getMessage()+" "+e.getClass());
			//e.printStackTrace();
		} finally {
			manager.unregister(this);

		}
	}
	
	public void close(){
		try {
			in.close();
			out.close();
			socket.close();
		} catch(IOException ioe){
			//ignore
		}
	}
	
	private void readFileRequest() throws IOException{
		int type = in.readByte();
		String filename = in.readUTF();
		long fileSize = in.readLong();
		int pieceSize = in.readInt();
		manager.write(ID+"-> "+type+" "+filename+" "+fileSize+" "+pieceSize, 0);
	}
	
	public void sendMessage(Message msg){
		if((msg instanceof PieceUpdateMessage) && (twosent == false)) {
			//System.err.println("5 Ignored");
			manager.write(ID+"IGNORED "+msg, 0);
			return;
		}

		synchronized (out) {
			try {
				out.write(msg.toBytes());
				out.flush();
				manager.write(ID+msg, 0);
			} catch(IOException ie){
				manager.write(ID+"ERROR"+msg, 0);
				manager.unregister(this);
			}
		}
	}
	
	public String toString(){
		return ID;
	}

}
