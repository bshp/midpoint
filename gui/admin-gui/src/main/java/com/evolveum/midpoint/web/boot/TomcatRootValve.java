/*
 * Copyright (c) 2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.boot;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class TomcatRootValve extends ValveBase {

	private static final Trace LOGGER = TraceManager.getTrace(TomcatRootValve.class);
	
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		
		Context context = request.getContext();
		if (context instanceof RootRootContext) {
			LOGGER.trace("Redirecting {} to real application root", request.getContextPath());
			response.sendRedirect("/midpoint");
			return;
		}
		
		getNext().invoke(request, response);
	}

}
