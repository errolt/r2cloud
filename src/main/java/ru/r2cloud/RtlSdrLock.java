package ru.r2cloud;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

public class RtlSdrLock {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RtlSdrLock.class);

	private final Map<Class<?>, Integer> priorities = new HashMap<Class<?>, Integer>();
	private final Map<Class<?>, RtlSdrListener> listeners = new HashMap<Class<?>, RtlSdrListener>();
	private RtlSdrListener lockedBy = null;

	public void register(Class<?> clazz, int priority) {
		priorities.put(clazz, priority);
	}

	public synchronized boolean tryLock(RtlSdrListener listener) {
		listeners.put(listener.getClass(), listener);
		if (lockedBy == null) {
			lockedBy = listener;
			return true;
		}
		Integer currentPriority = priorities.get(lockedBy.getClass());
		Integer newPriority = priorities.get(listener.getClass());
		if (newPriority > currentPriority) {
			lockedBy.suspend();
			lockedBy = listener;
			return true;
		} else {
			return false;
		}
	}

	public synchronized void release(RtlSdrListener listener) {
		if (lockedBy != listener) {
			return;
		}
		listeners.remove(listener.getClass());
		lockedBy = null;
		int maxPriority = 0;
		RtlSdrListener listenerToResume = null;
		for (RtlSdrListener cur : listeners.values()) {
			Integer curPriority = priorities.get(cur.getClass());
			if (curPriority > maxPriority) {
				maxPriority = curPriority;
				listenerToResume = cur;
			}
		}
		if (listenerToResume != null) {
			try {
				listenerToResume.resume();
			} catch (Exception e) {
				LOG.error("unable to notify resume", e);
			}
		}
	}

}