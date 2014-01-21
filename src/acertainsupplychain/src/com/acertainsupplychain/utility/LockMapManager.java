package com.acertainsupplychain.utility;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class is a lock manager that manages a tree of locks which are all
 * associated with a single object each of type E.
 * 
 * @author Arni
 * 
 * @param <E>
 *            the type of the object used as key into the map, and which are
 *            locked upon.
 */
public class LockMapManager<E extends Comparable<E>> {

	private final Map<E, ReadWriteLock> lockMap;
	private final ReadWriteLock mapLock;

	/**
	 * Initialize the LockMapManager instance.
	 */
	public LockMapManager() {
		lockMap = new HashMap<E, ReadWriteLock>();
		mapLock = new ReentrantReadWriteLock();
	}

	/**
	 * Adds a single object to the lock map if it does not already exist.
	 * 
	 * @param object
	 */
	public void addToLockMap(E object) {
		mapLock.writeLock().lock();
		if (!lockMap.containsKey(object)) {
			lockMap.put(object, new ReentrantReadWriteLock());
		}
		mapLock.writeLock().unlock();
	}

	/**
	 * Adds a list of objects to the lock map. All objects that are already in
	 * the lock map are not re-inserted.
	 * 
	 * @param objects
	 */
	public void addToLockMap(List<E> objects) {
		mapLock.writeLock().lock();
		for (E object : objects) {
			if (!lockMap.containsKey(object)) {
				lockMap.put(object, new ReentrantReadWriteLock());
			}
		}
		mapLock.writeLock().unlock();
	}

	/**
	 * Lock the write lock associated with the given object.
	 * 
	 * @param object
	 *            , the object to lock upon.
	 */
	public void acquireWriteLock(E object) {
		lockMap.get(object).writeLock().lock();
	}

	/**
	 * Unlock the write lock associated with the given object.
	 * 
	 * @param object
	 *            , the object to unlock upon.
	 */
	public void releaseWriteLock(E object) {
		lockMap.get(object).writeLock().unlock();
	}

	/**
	 * Lock the read lock associated with the given object.
	 * 
	 * @param object
	 *            , the object to lock upon.
	 */
	public void acquireReadLock(E object) {
		lockMap.get(object).readLock().lock();
	}

	/**
	 * Unlock the read lock associated with the given object.
	 * 
	 * @param object
	 *            , the object to unlock upon.
	 */
	public void releaseReadLock(E object) {
		lockMap.get(object).readLock().unlock();
	}

	/**
	 * Lock all the write locks associated with all the objects in the given
	 * list. The locks are locked in an ascending order.
	 * 
	 * @param objects
	 *            , the list of objects to lock.
	 */
	public void acquireWriteLocks(List<E> objects) {
		Collections.sort(objects);
		for (E object : objects) {
			lockMap.get(object).writeLock().lock();
		}
	}

	/**
	 * Unlock all the write locks associated with all the objects in the given
	 * list. The locks are unlocked in a descending order.
	 * 
	 * @param objects
	 *            , the list of objects to unlock.
	 */
	public void releaseWriteLocks(List<E> objects) {
		Collections.sort(objects);
		Collections.reverse(objects);
		for (E object : objects) {
			lockMap.get(object).writeLock().unlock();
		}
	}

	/**
	 * Lock all the read locks associated with all the objects in the given
	 * list. The locks are locked in an ascending order.
	 * 
	 * @param objects
	 *            , the list of objects to lock.
	 */
	public void acquireReadLocks(List<E> objects) {
		Collections.sort(objects);
		for (E object : objects) {
			lockMap.get(object).readLock().lock();
		}
	}

	/**
	 * Unlock all the read locks associated with all the objects in the given
	 * list. The locks are unlocked in a descending order.
	 * 
	 * @param objects
	 *            , the list of objects to unlock.
	 */
	public void releaseReadLocks(List<E> objects) {
		Collections.sort(objects);
		Collections.reverse(objects);
		for (E object : objects) {
			lockMap.get(object).readLock().unlock();
		}
	}
}
