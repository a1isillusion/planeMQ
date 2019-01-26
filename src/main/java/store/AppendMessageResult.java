package store;

public class AppendMessageResult {
public static String STATUS_SUCCESS="STATUS_SUCCESS";
public static String STATUS_FAIL="STATUS_FAIL";
public String status=STATUS_FAIL;
public long fileOffset;
public int offset;
public int size;
public int postion;
public long getFileOffset() {
	return fileOffset;
}
public void setFileOffset(long fileOffset) {
	this.fileOffset = fileOffset;
}
public String getStatus() {
	return status;
}
public void setStatus(String status) {
	this.status = status;
}
public int getOffset() {
	return offset;
}
public void setOffset(int offset) {
	this.offset = offset;
}
public int getSize() {
	return size;
}
public void setSize(int size) {
	this.size = size;
}
public int getPostion() {
	return postion;
}
public void setPostion(int postion) {
	this.postion = postion;
}

}
