package com.gifisan.nio.extend;

import com.gifisan.nio.component.future.nio.NIOReadFuture;

public class UpdateSessionIDOnReadFuture implements OnReadFuture{

	public void onResponse(FixedSession session, NIOReadFuture future) {
		session.getSession().setSessionID(Integer.valueOf(future.getText()));
	}
	
}