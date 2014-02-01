package com.livegameengine.scxml.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.Send;

public class SendInternalEvent extends Send {
	private final String targetType_ = "http://www.w3.org/TR/scxml/#SCXMLEventProcessor";
	private final String target_ = "#_internal";

	
	
	@Override
	public void execute(final EventDispatcher evtDispatcher, ErrorReporter errRep,
			SCInstance scInstance, Log appLog, Collection derivedEvents)
			throws ModelException, SCXMLExpressionException {
		super.execute(new EventDispatcher() {
			@Override
			public void cancel(String sendId) {}

			@Override
			public void send(String sendId, String target, String targetType,
					String event, Map params, Object hints, long delay,
					Object content, List externalNodes) {
				
				String e = String.format("board.%s", event);
				
				evtDispatcher.send(e + ".send", target_, targetType_, 
						e, params, hints, delay, content, externalNodes);
				
			}
			
		}, errRep, scInstance, appLog, derivedEvents);
	}
}