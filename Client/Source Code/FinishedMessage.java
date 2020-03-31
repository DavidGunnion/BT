
public class FinishedMessage implements Message{

	public byte[] toBytes(){
		return new byte[]{6};
	}
	
	public String toString(){
		return "<- 6 ";
	}
}
