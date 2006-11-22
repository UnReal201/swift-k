//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jun 20, 2005
 */
package org.globus.cog.karajan.scheduler;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.impl.common.StatusEvent;
import org.globus.cog.abstraction.impl.common.task.TaskSubmissionException;
import org.globus.cog.abstraction.interfaces.Service;
import org.globus.cog.abstraction.interfaces.Status;
import org.globus.cog.abstraction.interfaces.Task;
import org.globus.cog.karajan.util.BoundContact;
import org.globus.cog.karajan.util.Contact;
import org.globus.cog.karajan.util.ContactSet;
import org.globus.cog.karajan.util.TypeUtil;
import org.globus.cog.karajan.workflow.KarajanRuntimeException;

public class WeightedHostScoreScheduler extends LateBindingScheduler {

	private static final Logger logger = Logger.getLogger(WeightedHostScoreScheduler.class);

	public static final int POLICY_WEIGHTED_RANDOM = 0;
	public static final int POLICY_BEST_SCORE = 1;
	public static final String FACTOR_CONNECTION_REFUSED = "connectionRefusedFactor";
	public static final String FACTOR_CONNECTION_TIMEOUT = "connectionTimeoutFactor";
	public static final String FACTOR_SUBMISSION_TASK_LOAD = "jobSubmissionTaskLoadFactor";
	public static final String FACTOR_TRANSFER_TASK_LOAD = "transferTaskLoadFactor";
	public static final String FACTOR_FILEOP_TASK_LOAD = "fileOperationTaskLoadFactor";
	public static final String FACTOR_SUCCESS = "successFactor";
	public static final String FACTOR_FAILURE = "failureFactor";
	public static final String SCORE_HIGH_CAP = "scoreHighCap";
	public static final String SCORE_LOW_CAP = "scoreLowCap";
	public static final String NORMALIZATION_DELAY = "normalizationDelay";
	public static final String POLICY = "policy";

	private WeightedHostSet sorted;
	private int policy;
	private int delay;

	/*
	 * These field names must match the property names
	 */
	private double connectionRefusedFactor, connectionTimeoutFactor, jobSubmissionTaskLoadFactor,
			transferTaskLoadFactor, fileOperationTaskLoadFactor, successFactor, failureFactor,
			scoreHighCap, scoreLowCap;
	private int normalizationDelay;

	public WeightedHostScoreScheduler() {
		policy = POLICY_WEIGHTED_RANDOM;
		setDefaultFactors();
	}

	protected final void setDefaultFactors() {
		connectionRefusedFactor = 0.1;
		connectionTimeoutFactor = 0.05;
		jobSubmissionTaskLoadFactor = 0.9;
		transferTaskLoadFactor = 0.9;
		fileOperationTaskLoadFactor = 0.95;
		successFactor = 1.01;
		failureFactor = 0.9;
		scoreHighCap = 100;
		scoreLowCap = 0.01;
		normalizationDelay = 100;
	}

	public void setResources(ContactSet grid) {
		super.setResources(grid);
		sorted = new WeightedHostSet();
		Iterator i = grid.getContacts().iterator();
		while (i.hasNext()) {
			addToSorted(new WeightedHost((BoundContact) i.next()));
		}
	}

	protected void addToSorted(WeightedHost wh) {
		sorted.add(wh);
	}

	protected synchronized void multiplyScore(WeightedHost wh, double factor) {
		double score = wh.getScore();
		if (logger.isDebugEnabled()) {
			logger.debug("multiplyScore(" + wh + ", " + factor + ")");
		}
		double ns = checkCaps(score * factor);
		sorted.changeScore(wh, ns);
		if (logger.isDebugEnabled()) {
			logger.debug("Old score: " + score + ", new score: " + ns);
		}
	}

	protected double checkCaps(double score) {
		if (score > scoreHighCap) {
			return scoreHighCap;
		}
		else if (score < scoreLowCap) {
			return scoreLowCap;
		}
		else {
			return score;
		}
	}

	protected synchronized BoundContact getNextContact(TaskConstraints t)
			throws NoFreeResourceException {
		checkGlobalLoadConditions();
		BoundContact contact;

		WeightedHostSet s = sorted;

		s = constrain(s, getConstraintChecker(), t);

		double sum = s.getSum();
		if (policy == POLICY_WEIGHTED_RANDOM) {
			double rand = Math.random() * sum;
			if (logger.isDebugEnabled()) {
				logger.debug("Sorted: " + s);
				logger.debug("Rand: " + rand + ", sum: " + sum);
			}
			Iterator i = s.iterator();

			while (i.hasNext()) {
				WeightedHost wh = (WeightedHost) i.next();
				sum += wh.getScore();
				if (sum >= rand) {
					return wh.getHost();
				}
			}
			normalize();
			contact = s.last().getHost();
		}
		else if (policy == POLICY_BEST_SCORE) {
			normalize();
			contact = s.last().getHost();
		}
		else {
			throw new KarajanRuntimeException("Invalid policy number: " + policy);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Next contact: " + contact);
		}
		return contact;
	}

	protected WeightedHostSet constrain(WeightedHostSet s, ResourceConstraintChecker rcc,
			TaskConstraints tc) {
		if (rcc == null) {
			return s;
		}
		else {
			WeightedHostSet ns = new WeightedHostSet();
			Iterator i = s.iterator();
			while (i.hasNext()) {
				WeightedHost wh = (WeightedHost) i.next();
				if (rcc.checkConstraints(wh.getHost(), tc)) {
					ns.add(wh);
				}
			}
			return ns;
		}
	}

	protected void normalize() {
		delay++;
		if (delay > normalizationDelay) {
			if (logger.isDebugEnabled()) {
				logger.debug("Normalizing...");
				logger.debug("Before normalization: " + sorted);
			}
			delay = 0;
			sorted.normalize(1);
			if (logger.isDebugEnabled()) {
				logger.debug("After normalization: " + sorted);
			}
		}
	}

	private static String[] propertyNames;
	private static final String[] myPropertyNames = new String[] { POLICY,
			FACTOR_CONNECTION_REFUSED, FACTOR_CONNECTION_TIMEOUT, FACTOR_SUBMISSION_TASK_LOAD,
			FACTOR_TRANSFER_TASK_LOAD, FACTOR_FILEOP_TASK_LOAD, FACTOR_FAILURE, FACTOR_SUCCESS,
			SCORE_HIGH_CAP, SCORE_LOW_CAP, NORMALIZATION_DELAY };
	private static Set propertyNamesSet;

	static {
		propertyNamesSet = new HashSet();
		for (int i = 0; i < myPropertyNames.length; i++) {
			propertyNamesSet.add(myPropertyNames[i]);
		}
	}

	public synchronized String[] getPropertyNames() {
		if (propertyNames == null) {
			propertyNames = AbstractScheduler.combineNames(super.getPropertyNames(),
					myPropertyNames);
		}
		return propertyNames;
	}

	public final void setProperty(String name, Object value) {
		if (propertyNamesSet.contains(name)) {
			if (POLICY.equals(name)) {
				if (value instanceof String) {
					value = ((String) value).toLowerCase();
				}
				if ("random".equals(value)) {
					policy = POLICY_WEIGHTED_RANDOM;
				}
				else if ("best".equals("value")) {
					policy = POLICY_BEST_SCORE;
				}
				else {
					throw new KarajanRuntimeException("Unknown policy type: " + value);
				}
			}
			else {
				double val = TypeUtil.toDouble(value);
				try {
					Field f = this.getClass().getField(name);
					if (f.getClass().equals(int.class)) {
						f.setInt(this, (int) val);
					}
					else {
						f.setDouble(this, val);
					}
				}
				catch (Exception e) {
					throw new KarajanRuntimeException("Failed to set property '" + name + "'", e);
				}
			}
		}
		else {
			super.setProperty(name, value);
		}
	}

	public void submitBoundToServices(Task t, Contact[] contacts, Service[] services)
			throws TaskSubmissionException {
		factorSubmission(t, contacts, 1);
		super.submitBoundToServices(t, contacts, services);
	}

	public void statusChanged(StatusEvent e) {
		try {
			Task t = (Task) e.getSource();
			int code = e.getStatus().getStatusCode();
			Contact[] contacts = getContacts(t);
			if (code == Status.SUBMITTED) {
				// this isn't reliable
				// factorSubmission(t, contacts, 1);
			}
			else if (code == Status.COMPLETED) {
				factorSubmission(t, contacts, -1);
				factorMultiple(contacts, successFactor);
			}
			else if (code == Status.FAILED) {
				factorMultiple(contacts, failureFactor);
				Exception ex = e.getStatus().getException();
				if (ex != null) {
					String exs = ex.toString();
					if (exs.indexOf("Connection refused") >= 0
							|| exs.indexOf("connection refused") >= 0) {
						factorMultiple(contacts, connectionRefusedFactor);
					}
					else if (exs.indexOf("timeout") >= 0) {
						factorMultiple(contacts, connectionTimeoutFactor);
					}
				}
			}
		}
		catch (Exception ex) {
			logger.warn("Scheduler threw exception while processing task status change", ex);
		}
		finally {
			super.statusChanged(e);
		}
	}

	private void factorSubmission(Task t, Contact[] contacts, int exp) {
		// I wonder where the line between abstraction and obfuscation is...
		if (t.getType() == Task.JOB_SUBMISSION) {
			factorMultiple(contacts, spow(jobSubmissionTaskLoadFactor, exp));
		}
		else if (t.getType() == Task.FILE_TRANSFER) {
			factorMultiple(contacts, spow(transferTaskLoadFactor, exp));
		}
		else if (t.getType() == Task.FILE_OPERATION) {
			factorMultiple(contacts, spow(fileOperationTaskLoadFactor, exp));
		}
	}

	private double spow(double x, int exp) {
		if (exp == 1) {
			return x;
		}
		else if (exp == -1) {
			return 1 / x;
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	private void factorMultiple(Contact[] contacts, double factor) {
		for (int i = 0; i < contacts.length; i++) {
			WeightedHost wh = sorted.findHost((BoundContact) contacts[i]);
			multiplyScore(wh, factor);
		}
	}

}
