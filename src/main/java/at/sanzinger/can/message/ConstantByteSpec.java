package at.sanzinger.can.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConstantByteSpec extends ByteSpec {

	private final long constant;
	// Reference to the variable byte spec
	private final VariableByteSpec constantOf;
	
	public ConstantByteSpec(String name, ByteSpec predecessor, ByteSpec length, long constant) {
		super(name, predecessor, length, ByteOrder.LITTLE_ENDIAN);
		this.constant = constant;
		this.constantOf = null;
	}
	
	public ConstantByteSpec(VariableByteSpec constantOf, long constant) {
		super(constantOf.name, constantOf.predecessor, constantOf.length, ByteOrder.LITTLE_ENDIAN);
		this.constant = constant;
		this.constantOf = constantOf;
	}	
	
	public void emit(ByteBuffer buff) {
		super.setValue(buff, constant);
	}
	
	public VariableByteSpec getConstantOf() {
		return constantOf;
	}
	
	@Override
	public String toString() {
		return String.format("ConstantByteSpec %s=0x%x", name, constant);
	}
	
	@Override
	public String format(ByteBuffer payload) {
		return String.format("%s: 0x%x", name, getValue(payload));
	}
}
