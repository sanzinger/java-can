package at.sanzinger.can;

public class DataMessage {
	private final byte[] data;
	private final int id;
	
	public DataMessage(byte[] data, int id) {
		this.data = data;
		this.id = id;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(byte b : data) {
			sb.append(String.format("0x%02x, ", b));
		}
		return String.format("ID: 0x%x Data: {%s}", id, sb.subSequence(0, sb.length()-2));
	}
}
