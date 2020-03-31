import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*; 

public class FileOfferMessage implements Message {
	private final int totalPieces;
	private final int havePieces;
	private final BitSet pieces;
	
	public FileOfferMessage(int totalPieces, int havePieces, BitSet pieces){
		this.totalPieces = totalPieces;
		this.havePieces = havePieces;
		this.pieces = pieces;
	}
	
	public byte[] toBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
			
		try {
			out.write(2);
			out.writeInt(totalPieces);
			out.writeInt(havePieces);

			if (havePieces > 0 && havePieces != totalPieces){
				int arraySize = (int)Math.ceil(((double)totalPieces)/8);
				byte[] bits = new byte[arraySize];
				//System.out.println(pieces.length);
				for (int i=0; i < pieces.length(); i++) {
		            if (pieces.get(i)) {
		            	//System.out.println("i="+i+" "+(i/8));
		                bits[i/8] |= ( 0x80 >>> (i%8) );
		            }
		        }
				out.write(bits);
			}
		} catch (IOException ioe){
			//should never happen
		}
		
		return baos.toByteArray();
	}
	
	public String toString(){
		return "<- 2 "+totalPieces+" "+havePieces+" "+(havePieces > 0 && havePieces != totalPieces);
	}

}
