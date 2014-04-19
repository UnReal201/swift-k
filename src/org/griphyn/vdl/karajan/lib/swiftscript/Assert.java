/*
 * Copyright 2012 University of Chicago
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.griphyn.vdl.karajan.lib.swiftscript;

// import org.apache.log4j.Logger;
import k.rt.Channel;
import k.rt.ExecutionException;
import k.rt.Stack;

import org.globus.cog.karajan.analyzer.ChannelRef;
import org.globus.cog.karajan.analyzer.Signature;
import org.griphyn.vdl.karajan.AssertFailedException;
import org.griphyn.vdl.karajan.lib.SwiftFunction;
import org.griphyn.vdl.mapping.AbstractDataNode;
import org.griphyn.vdl.mapping.DSHandle;
import org.griphyn.vdl.mapping.DependentException;
import org.griphyn.vdl.type.Types;

/**
    Throw AssertionException if input is false or 0. 
    Optional second argument is string message printed on failure. 
 */
public class Assert extends SwiftFunction {
    private ChannelRef<AbstractDataNode> c_vargs;
        
    @Override
    protected Signature getSignature() {
        return new Signature(params("..."));
    }

    @Override
    public Object function(Stack stack) {
        Channel<AbstractDataNode> fargs = c_vargs.get(stack);
        
        if (fargs.size() < 1) {
            throw new ExecutionException(this, "Missing condition");
        }
        if (fargs.size() > 2) {
            throw new ExecutionException(this, "Too many arguments");
        }
        
        AbstractDataNode hmessage = null;
        if (fargs.size() == 2) {
            hmessage = fargs.get(1);
        }
        
        String message;
        try {
            if (hmessage != null) {
                hmessage.waitFor(this);
                message = (String) hmessage.getValue();
            }
            else {
                message = "Assertion failed";
            }
            AbstractDataNode hvalue = fargs.get(0);
            hvalue.waitFor(this);
                     
            checkAssert(hvalue, message);
        }
        catch (DependentException e) {
            // cannot make assertion so ignore
        }
        
        return null;
    }

    private void checkAssert(DSHandle value, String message) {
        boolean success = true; 
        if (value.getType() == Types.BOOLEAN) { 
            if (! (Boolean) value.getValue())
                success = false;
        }
        else if (value.getType() == Types.INT) {
            double d = ((Integer) value.getValue()).intValue();
            if (d == 0)
                success = false;
        } 
        else {
            throw new ExecutionException(this, "First argument to assert() must be boolean or int!");
        }
        if (!success) {
            throw new AssertFailedException(this, message);
        }
    }
}
