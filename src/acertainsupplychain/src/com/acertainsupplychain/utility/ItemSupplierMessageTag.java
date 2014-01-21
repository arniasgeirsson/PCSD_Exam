package com.acertainsupplychain.utility;

/**
 * A set of HTTP message tags used in the HTTP communication to and from an
 * ItemSupplier and OrderManager.
 * 
 * @author Arni
 * 
 */
public enum ItemSupplierMessageTag {
	EXECUTESTEP, GETORDERS, CLEAR, GETSUPID, REGISTERWORKFLOW,
	GETWORKFLOWSTATUS, INIT_ORDERMANAGER, JOBGETSUPID, JOBGETWORKFLOW, 
	JOBSETSTATUS, WAITFORJOBS, INIT_ITEMSUPPLIER, INIT_ORDERMANAGER_PROXY, 
	ORDERMANAGER_STOP;
}
