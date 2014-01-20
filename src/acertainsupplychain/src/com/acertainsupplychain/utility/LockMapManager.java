package com.acertainsupplychain.utility;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockMapManager<E extends Comparable<E>> {

	private final Map<E, ReadWriteLock> lockMap;
	private final ReadWriteLock mapLock;

	public LockMapManager() {
		lockMap = new HashMap<E, ReadWriteLock>();
		mapLock = new ReentrantReadWriteLock();
	}

	public void addToLockMap(E object) {
		mapLock.writeLock().lock();
		if (!lockMap.containsKey(object)) {
			lockMap.put(object, new ReentrantReadWriteLock());
		}
		mapLock.writeLock().unlock();
	}

	public void addToLockMap(List<E> objects) {
		mapLock.writeLock().lock();
		for (E object : objects) {
			if (!lockMap.containsKey(object)) {
				lockMap.put(object, new ReentrantReadWriteLock());
			}
		}
		mapLock.writeLock().unlock();
	}

	public void acquireWriteLock(E object) {
		lockMap.get(object).writeLock().lock();
	}

	public void releaseWriteLock(E object) {
		lockMap.get(object).writeLock().unlock();
	}

	public void acquireReadLock(E object) {
		lockMap.get(object).readLock().lock();
	}

	public void releaseReadLock(E object) {
		lockMap.get(object).readLock().unlock();
	}

	public void acquireWriteLocks(List<E> objects) {
		Collections.sort(objects);
		for (E object : objects) {
			lockMap.get(object).writeLock().lock();
		}
	}

	public void releaseWriteLocks(List<E> objects) {
		Collections.sort(objects);
		Collections.reverse(objects);
		for (E object : objects) {
			lockMap.get(object).writeLock().unlock();
		}
	}

	public void acquireReadLocks(List<E> objects) {
		Collections.sort(objects);
		for (E object : objects) {
			lockMap.get(object).readLock().lock();
		}
	}

	public void releaseReadLocks(List<E> objects) {
		Collections.sort(objects);
		Collections.reverse(objects);
		for (E object : objects) {
			lockMap.get(object).readLock().unlock();
		}
	}
}
