import java.io.File;

public class FileInfo {
	
	private static final int KB = 1024;
	private static final int MB = 1024 * 1024;
	private static final int GB =  1024 * 1024 * 1024;
	
	private String fileName;
	private long fileSize;
	private int pieceSize;
	private int noPieces;
	private String path;
	

	public FileInfo(String name){
		File f = new File(name);
		fileName = f.getName();
		path = f.getPath();
		fileSize = f.length();
		pieceSize = calcPieceSize();
		noPieces = (int)(( (fileSize % pieceSize) == 0 ) ? fileSize / pieceSize : (fileSize / pieceSize) + 1);
		
	}
	
	public FileInfo(String name, long fs, int ps){
		File f = new File(name);
		fileName = f.getName();
		path = f.getPath();
		fileSize = fs;
		pieceSize = (int)(fileSize > ps ? ps : fileSize);
		noPieces = (int)(( (fileSize % pieceSize) == 0 ) ? fileSize / pieceSize : (fileSize / pieceSize) + 1);
	}
	
	private int calcPieceSize(){
		/*if (fileSize > (2L*GB)){
			return 2*MB;
		} else if(fileSize > (1*GB)){
			return 1*MB;
		} else if(fileSize > (500*MB)){
			return 512*KB;
		} else */
		if(fileSize > (256*MB)){
			return 64*KB;
		} else if(fileSize > (128*MB)){
			return 32*KB;
		} else if(fileSize > (64*MB)){
			return 16*KB;
		} else {
			return (int)(fileSize > 8*KB ? 8*KB : fileSize);
		}
		
	}
		
	public String getFileName(){
		return fileName;
	}
	
	public String getFilePath(){
		return path;	
	}
		
	public long getFileSize(){
		return fileSize;
	}
		
	public int getPieceSize(){
		return pieceSize;
	}
	
	public int getNumberOfPieces(){
		return noPieces;
	}
	
	public String toString(){
		return "FileInfo["+fileName+", "+fileSize+", "+pieceSize+", "+noPieces+"]";
	}
}
