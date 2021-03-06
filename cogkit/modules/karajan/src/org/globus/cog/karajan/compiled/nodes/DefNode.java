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

/*
 * Created on Jul 31, 2003
 */
package org.globus.cog.karajan.compiled.nodes;

import java.util.List;

import org.apache.log4j.Logger;
import org.globus.cog.karajan.analyzer.ArgRef;
import org.globus.cog.karajan.analyzer.ChannelRef;
import org.globus.cog.karajan.analyzer.CompilationException;
import org.globus.cog.karajan.analyzer.Param;
import org.globus.cog.karajan.analyzer.Scope;
import org.globus.cog.karajan.analyzer.Scope.JavaDef;
import org.globus.cog.karajan.analyzer.Signature;
import org.globus.cog.karajan.analyzer.Var;
import org.globus.cog.karajan.parser.WrapperNode;
import org.globus.cog.karajan.util.Pair;

public class DefNode extends InternalFunction  {
	private static final Logger logger = Logger.getLogger(DefNode.class);

	private ArgRef<String> classname;
	private ChannelRef<Object> cr_vargs;


	@Override
	protected Signature getSignature() {
		return new Signature(
				params("classname"),
				returns(channel("..."))
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Node compileBody(WrapperNode w, Scope argScope, Scope scope) throws CompilationException {
		Var clsv = argScope.lookupParam("classname");
		String cls = (String) clsv.getValue();
		if (cls != null) {
			try {
				Var.Channel crvargs = scope.lookupChannel("...");
				Class<?> clsi = Class.forName(cls);
				if (!Node.class.isAssignableFrom(clsi)) {
					throw new CompilationException(w, "Invalid definition. Class " + 
							clsi.getName() + " is not a subclass of " + Node.class.getName());
				}
				JavaDef def = new JavaDef((Class<? extends Node>) clsi);
				//def.newInstance();
				if (!crvargs.append(def)) {
					throw new CompilationException(w, "Invalid definition");
				}
			}
			catch (ClassNotFoundException e) {
				throw new CompilationException(w, "Invalid node definition class: " + cls, e);
			}
			catch (NoClassDefFoundError e) {
				throw new CompilationException(w, "Invalid node definition class: " + cls, e);
			}
		}
		else {
			throw new CompilationException(w, "Could not statically determine class name");
		}
		return null;
	}

	@Override
	protected void optimizePositional(WrapperNode w, Scope scope, List<Param> params, List<Pair<Param, String>> dynamicOptimized)
			throws CompilationException {
	}
}