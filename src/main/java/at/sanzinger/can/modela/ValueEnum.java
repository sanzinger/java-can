package at.sanzinger.can.modela;

import java.nio.ByteBuffer;

import at.sanzinger.can.message.EnumByteSpec;

public interface ValueEnum {

	long getValue();
	

	public static <T extends ValueEnum> T getEnum(EnumByteSpec<T> spec, ByteBuffer buff) {
		long number = spec.getValue(buff);
		for(T i : spec.getValues()) {
			if(i.getValue() == number) {
				return i;
			}
		}
		throw new IllegalArgumentException(String.format("Unknown %s value 0x%x", spec.getName(), number));
	}
}
