
public class PeerInfo {
	private String host; 
	private int port;
	public PeerInfo(String host, int port){
		this.host = host;
		this.port = port;
	}
	
	public String getHost(){
		return host;
	}
	
	public int getport(){
		return port; 
	}
	
	public boolean equals(Object o){
		if (o == this) return true;
		
		if(!(o instanceof PeerInfo)) return false;
		
		PeerInfo p = (PeerInfo)o;
	
		return (p.port == port && p.getHost().equalsIgnoreCase(host));
		
	}
	
	public String toString(){
		return "PeerInfo "+host+" "+port;
	}
	
	
}
