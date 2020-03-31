public class PieceRequestMessage implements Message {

	private final int piece;
	
	public PieceRequestMessage(int piece){
		this.piece = piece;
	}
	
	public byte[] toBytes() {
		return new byte[] {
				3,
				(byte)(piece >>> 24),
				(byte)(piece >>> 16),
				(byte)(piece >>> 8),
				(byte)(piece >>> 0)
		};
	}
	
	public String toString(){
		return "<- 3 "+piece;
	}

}
