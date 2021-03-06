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

//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jan 7, 2013
 */
package k.rt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Context {
    public static final String VAR_NAME = "#context";
    public static final String ATTR_CWD = "CWD";
    
	private List<String> arguments;
	private Map<String, Object> attributes;

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}
	
	public void setAttribute(String name, Object value) {
	    if (attributes == null) {
	        attributes = new HashMap<String, Object>();
	    }
	    attributes.put(name, value);
	}
	
	public Object getAttribute(String name) {
	    if (attributes == null) {
	        return null;
	    }
	    else {
	        return attributes.get(name);
	    }
	}
	
	public void setCWD(String cwd) {
	    setAttribute(ATTR_CWD, cwd);
	}
	
	public String getCWD() {
	    return (String) getAttribute(ATTR_CWD);
	}
}
