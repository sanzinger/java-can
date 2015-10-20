package at.sanzinger.can.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class VariableByteSpec extends ByteSpec {
	
	public VariableByteSpec(String name, ByteSpec predecessor, ByteSpec length) {
		super(name, predecessor, length, ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void setValue(ByteBuffer payload, long i) {
		super.setValue(payload, i);
	}
	
	@Override
	public long getValue(ByteBuffer payload) {
		return super.getValue(payload);
	}
	
	@Override
	public byte[] getBytes(ByteBuffer payload) {
		return super.getBytes(payload);
	}
	
	@Override
	public void setBytes(ByteBuffer payload, byte[] bytes) {
		super.setBytes(payload, bytes);
	}
	
	@Override
	public String toString() {
		return String.format("VariableByteSpec %s", name);
	}

	@Override
	public String format(ByteBuffer payload) {
		Object obj = getObject(payload);
		if(obj instanceof Number) {
			return String.format("%s: 0x%x", name, obj);
		} else if(obj instanceof byte[]) {
			return String.format("%s: %s", name, Arrays.toString((byte[])obj));
		} else {
			throw new IllegalArgumentException("Unimplemented");
		}
	}
}
