import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class FileRequestMessage implements Message {

	private FileInfo fInfo;
	
	public FileRequestMessage(FileInfo fInfo){
		this.fInfo = fInfo; 
	}
	
	public byte[] toBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
			
		try {
			out.writeByte(1);
			out.writeUTF(fInfo.getFileName());
			out.writeLong(fInfo.getFileSize());
			out.writeInt(fInfo.getPieceSize());
		} catch (IOException ioe){
			//should never happen
		}
		
		return baos.toByteArray();
	}

	public String toString(){
		return "<- 1 "+fInfo.getFileName()+" "+fInfo.getFileSize()+" "+fInfo.getPieceSize();
	}
}
