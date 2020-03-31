
public class PieceUpdateMessage implements Message{

	private final int piece;
	
	public PieceUpdateMessage(int piece){
		this.piece = piece;
	}
	
	public byte[] toBytes() {
		return new byte[] {
				5,
				(byte)(piece >>> 24),
				(byte)(piece >>> 16),
				(byte)(piece >>> 8),
				(byte)(piece >>> 0)
		};
	}
	
	public String toString(){
		return "<-5 "+piece;
	}

}
