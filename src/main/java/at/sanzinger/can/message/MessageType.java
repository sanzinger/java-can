package at.sanzinger.can.message;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;

import at.sanzinger.can.Log;


public abstract class MessageType<T> {
	
	protected MessageType() {
	}
	
	protected static AnonymousConstant c(long i) {
		return new AnonymousConstant(i);
	}
	
	public abstract ByteSpec[] getByteSpec();
	
	public void setConstant(ByteBuffer buff) {
		for(ByteSpec s : getByteSpec()) {
			if(s instanceof ConstantByteSpec) {
				((ConstantByteSpec) s).emit(buff);
			}
		}
	}
	
	protected static ByteSpec[] extend(ByteSpec[] parent, ByteSpec ... specs) {
		ArrayList<ByteSpec> extended = new ArrayList<>(parent.length + specs.length);
		IdentityHashMap<ByteSpec, ByteSpec> constantOverwrite = new IdentityHashMap<>();
		for(ByteSpec s : specs) {
			if(s instanceof ConstantByteSpec) {
				VariableByteSpec constantOf = ((ConstantByteSpec)s).getConstantOf();
				if(constantOf != null) {					
					constantOverwrite.put(constantOf, s);
				}
			}
		}
		ByteSpec last = specs[specs.length-1];
		while(last != null) {
			ByteSpec candidate = constantOverwrite.containsKey(last) ? constantOverwrite.get(last) : last;
			extended.add(candidate);
			last = last.getPredecessor();
		}
		Collections.reverse(extended);
		return extended.toArray(new ByteSpec[0]);
	}
	
	public int getLength(ByteBuffer payload) {
		ByteSpec[] specs = getByteSpec();
		ByteSpec last = specs[specs.length-1];
		return last.getBegin(payload) + last.getLength(payload);
	}
	
	public String getName() {
		return getClass().getSimpleName();
	}
	
	public abstract T readMessage(InputStream is) throws IOException;
	
	protected void read(ByteBuffer buff, InputStream is, ByteSpec startAt) throws IOException {
		boolean started = false;
		for(ByteSpec s : getByteSpec()) {
			if(startAt == s || started) {
				started = true;
				int length = s.getLength(buff);
				int waitCount = 10;
				while(is.available() < length && waitCount > 0) {
					try {
						Thread.sleep(0, 500*1000);
					} catch (InterruptedException e) {
						// Ignore
					}
					waitCount --;
				}
				if(waitCount == 0) {
					throw new IOException("Read timed out");
				}
				byte[] bytes = new byte[length];
				int bytesRead = is.read(bytes);
				if(bytesRead != length) {
					Log.error("Not exptected");
				}
				buff.put(bytes);
			}
		}
	}
}
