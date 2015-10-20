package at.sanzinger.can.modela;

import java.nio.ByteBuffer;

import at.sanzinger.can.message.ByteSpec;
import at.sanzinger.can.message.VariableByteSpec;

public class ModelAMessage<T extends ModelAMessage<?>> {
	private final ModelAMessageType<T> type;
	protected final ByteBuffer payload;
	
	public ModelAMessage(ModelAMessageType<T> type, ByteBuffer payload) {
		super();
		this.type = type;
		this.payload = payload;
	}
	
	public ModelAMessageType<T> getType() {
		return type;
	}
	
	protected byte calculateChecksum() {
		byte sum = 0;
		VariableByteSpec checksum = type.getChecksum();
		int len = checksum.getBegin(payload);
		for(int i = ModelAMessageType.MESSAGE_TYPE.getBegin(payload); i < len; i++) {
			sum += payload.get(i);
		}
		return sum;
	}
	
	public void emitChecksum() {
		type.getChecksum().setValue(payload, calculateChecksum());
	}
	
	public final byte[] getBody() {
		type.setConstant(payload);
		emitChecksum();
		int length = type.getLength(payload);
		byte[] result = new byte[length];
		int oldPos = payload.position();
		payload.get(result, 0, length);
		payload.position(oldPos);
		return result;
	}

	public boolean verify() {
		return calculateChecksum() == getChecksum();
	}
	
	public byte getChecksum() {
		return (byte)type.getChecksum().getValue(payload);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type.getName());
		sb.append("[");
		for(ByteSpec s : type.getByteSpec()) {
			sb.append(s.format(payload));
			sb.append(", ");
		}
		return sb.substring(0, sb.length()-2) + "]";
	}
}
