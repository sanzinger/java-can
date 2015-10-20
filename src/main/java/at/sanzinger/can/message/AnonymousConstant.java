package at.sanzinger.can.message;

import java.nio.ByteBuffer;

public class AnonymousConstant extends ByteSpec {
	private final long constant;
	public AnonymousConstant(long constant) {
		super(null, null, null, null);
		this.constant = constant;
	}
	
	@Override
	protected long getValue(ByteBuffer payload) {
		return constant;
	}
}
