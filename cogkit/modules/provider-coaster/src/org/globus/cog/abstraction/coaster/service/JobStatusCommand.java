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
 * Created on Feb 12, 2008
 */
package org.globus.cog.abstraction.coaster.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.impl.common.execution.JobException;
import org.globus.cog.abstraction.interfaces.Status;
import org.globus.cog.coaster.ProtocolException;
import org.globus.cog.coaster.RemoteException;
import org.globus.cog.coaster.commands.Command;

public class JobStatusCommand extends Command {
    public static final Logger logger = Logger.getLogger(JobStatusCommand.class);
    
    public static final String NAME = "JOBSTATUS";

    private String taskId;
    private Status status;
    private String out, err;

    public JobStatusCommand(String taskId, Status status) {
        super(NAME);
        this.taskId = taskId;
        this.status = status;
    }
    
    public JobStatusCommand(String taskId, Status status, String out, String err) {
        this(taskId, status);
        this.out = out;
        this.err = err;
    }


    public void send() throws ProtocolException {
        try {
            serialize();
        }
        catch (Exception e) {
            throw new ProtocolException(
                    "Could not serialize status", e);
        }
        super.send();
    }

    protected void serialize() throws IOException {
        addOutData(taskId); //0
        addOutData(status.getStatusCode()); //1
        if (status.getException() instanceof JobException) {
            addOutData(((JobException) status.getException()).getExitCode());
        }
        else {
            addOutData(0);
        }
        StringBuffer sb = new StringBuffer();
        if (status.getMessage() != null) {
        	sb.append(status.getMessage());
        }
        addOutData(sb.toString()); //3
        addOutData(status.getTime().getTime()); //4
        if (out != null && err != null) {
            addOutData(out);
            addOutData(err);
            addException(status.getException());
        }
        else if (out != null || err != null) {
            logger.warn("Only one of job STDOUT or STDERR is non-null");
        }
        else {
            addException(status.getException());
        }
    }

    private void addException(Exception e) {
        if (e == null) {
            return;
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(e);
            addOutData(os.toByteArray());
        }
        catch (Exception ex) {
            logger.info("Could not serialize job status exception", ex);
        }
        
    }
}
