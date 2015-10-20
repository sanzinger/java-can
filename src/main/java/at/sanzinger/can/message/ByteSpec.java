package at.sanzinger.can.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteSpec {
	protected final String name;
	protected final ByteSpec predecessor;
	protected final ByteSpec length;
	private final ByteOrder order;
	
	public ByteSpec(String name, ByteSpec predecessor, ByteSpec length, ByteOrder order) {
		super();
		this.predecessor = predecessor;
		this.length = length;
		this.name = name;
		this.order = order;
	}

	public int getBegin(ByteBuffer payload) {
		if(predecessor == null) {
			return 0;
		} else {
			return predecessor.getBegin(payload) + predecessor.getLength(payload);
		}
	}
	
	public int getLength(ByteBuffer payload) {
		return (int)length.getValue(payload);
	}
	
	public int positionAfter(ByteBuffer payload) {
		return getBegin(payload) + getLength(payload);
	}
	
	protected void setValue(ByteBuffer payload, long i) {
		payload.order(order);
		int begin = getBegin(payload);
		switch(getLength(payload)) {
		case 1:
			assert (i&0xFF) == i;
			payload.put(begin, (byte)i);
			break;
		case 2:
			assert (i&0xFFFF) == i;
			payload.putShort(begin, (short)i);
			break;
		case 4:
			assert (i&0xFFFFFFFF) == i;
			payload.putInt(begin, (int)i);
			break;
		case 5:
			payload.putLong(begin, i);
			break;
		default:
			throw new IllegalArgumentException("Not implemented");
		}
	}
	
	protected void setBytes(ByteBuffer payload, byte[] bytes) {
		if(bytes.length == getLength(payload)) {
			int begin = getBegin(payload);
			for(int i = 0; i<bytes.length; i++) {
				payload.put(begin + i, bytes[i]);
			}
		} else {
			throw new IllegalArgumentException("Set byte buffer has not the correct length");
		}
	}
	
	protected byte[] getBytes(ByteBuffer payload) {
		byte[] byteBuffer = new byte[getLength(payload)];
		int begin = getBegin(payload);
		for(int i = 0; i < byteBuffer.length; i++) {
			byteBuffer[i] = payload.get(begin + i);
		}
		return byteBuffer;
	}
	
	protected long getValue(ByteBuffer payload) {
		payload.order(order);
		int begin = getBegin(payload);
		switch(getLength(payload)) {
		case 1:
			return payload.get(begin);
		case 2:
			return payload.getShort(begin);
		case 4:
			return payload.getInt(begin);
		case 8:
			return payload.getLong(begin);
		default:
			throw new IllegalArgumentException("Not implemented " + this);
		}
	}
	
	protected Object getObject(ByteBuffer payload) {
		int length = getLength(payload);
		assert length > 0;
		if(length == 1 || length == 2 || length == 4 || length == 8) {
			return getValue(payload);
		} else {
			return getBytes(payload);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String format(ByteBuffer payload) {
		return name;
	}
	
	public ByteSpec getPredecessor() {
		return predecessor;
	}
}
