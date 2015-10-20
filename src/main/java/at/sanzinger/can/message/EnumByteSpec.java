package at.sanzinger.can.message;

import java.nio.ByteBuffer;

import at.sanzinger.can.modela.ValueEnum;

public class EnumByteSpec<T extends ValueEnum> extends VariableByteSpec {
	private T[] values;

	public EnumByteSpec(String name, ByteSpec predecessor, ByteSpec length, T[] values) {
		super(name, predecessor, length);
		this.values = values;
	}
	
	public T[] getValues() {
		return values;
	}

	public void setValue(ByteBuffer payload, T value) {
		setValue(payload, value.getValue());
	}
	
	@Override
	public String format(ByteBuffer payload) {
		return String.format("%s: %s", name, ValueEnum.getEnum(this, payload));
	}
}
