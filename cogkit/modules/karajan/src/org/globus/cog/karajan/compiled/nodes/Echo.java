/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 * Code from Java CoG Kit Project (see notice below) with modifications.
 *
 * Copyright 2005-2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ----------------------------------------------------------------------
// This code is developed as part of the Java CoG Kit project
// The terms of the license can be found at http://www.cogkit.org/license
// This message may not be removed or altered.
// ----------------------------------------------------------------------

package org.globus.cog.karajan.compiled.nodes;

import k.rt.ExecutionException;
import k.rt.Stack;
import k.thr.LWThread;

import org.globus.cog.karajan.analyzer.ArgRef;
import org.globus.cog.karajan.analyzer.ChannelRef;
import org.globus.cog.karajan.analyzer.CompilationException;
import org.globus.cog.karajan.analyzer.Scope;
import org.globus.cog.karajan.analyzer.Signature;
import org.globus.cog.karajan.parser.WrapperNode;
import org.globus.cog.karajan.util.TypeUtil;

public class Echo extends InternalFunction {
	
	private ArgRef<Boolean> nl;
	private ChannelRef<Object> c_vargs;
	
	@Override
	protected Signature getSignature() {
		return new Signature(
				params(optional("nl", Boolean.TRUE), "...")
		);
	}

	public void runBody(LWThread thr) {
		Stack stack = thr.getStack();
		k.rt.Channel<Object> c = c_vargs.get(stack);
		for (Object o : c) {
			waitFor(o);
		}
		for (Object o : c) {
			System.out.print(TypeUtil.toString(o));
		}
		if (nl.getValue(stack)) {
			System.out.println();
		}
	}	
}