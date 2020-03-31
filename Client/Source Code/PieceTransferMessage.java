import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class PieceTransferMessage implements Message {

	private int pieceNo;
	private byte[] data;
	
	public PieceTransferMessage(int pieceNo, byte[] data){
		this.pieceNo = pieceNo;
		this.data = data;
	}
	
	public byte[] toBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
			
		try {
			out.writeByte(4);
			out.writeInt(pieceNo);
			out.writeInt(data.length);
			out.write(data);

		} catch (IOException ioe){
			//should never happen
		}
		
		return baos.toByteArray();
	}
	
	public String toString(){
		return "<- 4 "+pieceNo+" "+data.length+" [data]";
	}

}
