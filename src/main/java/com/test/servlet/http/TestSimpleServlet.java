package com.test.servlet.http;

import com.gifisan.nio.common.Logger;
import com.gifisan.nio.common.LoggerFactory;
import com.gifisan.nio.component.protocol.http11.future.HTTPReadFuture;
import com.gifisan.nio.extend.http11.HttpSession;
import com.gifisan.nio.extend.service.HTTPFutureAcceptorService;

public class TestSimpleServlet extends HTTPFutureAcceptorService {
	
	private Logger	logger	= LoggerFactory.getLogger(TestSimpleServlet.class);

	protected void doAccept(HttpSession session, HTTPReadFuture future) throws Exception {
		System.out.println();
		logger.info(future.getHost());
		logger.info(future.getRequestURI());
		logger.info(future.getParamString());
		System.out.println();
		String res = "yes server already accept your message :) " + future.getParamString();

		future.write(res);
		
		session.flush(future);
	}
}