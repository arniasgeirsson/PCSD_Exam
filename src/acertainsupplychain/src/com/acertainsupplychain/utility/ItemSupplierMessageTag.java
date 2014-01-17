/**
 * 
 */
package com.acertainsupplychain.utility;

/**
 * TODO BookStoreMessageTag implements the messages supported in the bookstore
 * 
 */
public enum ItemSupplierMessageTag {
	EXECUTESTEP, GETORDERS, CLEAR, GETSUPID, REGISTERWORKFLOW,
	GETWORKFLOWSTATUS, INIT_ORDERMANAGER, JOBGETSUPID, JOBGETWORKFLOW,
	JOBSETSTATUS, WAITFORJOBS, INIT_ITEMSUPPLIER, INIT_ORDERMANAGER_PROXY,
	ORDERMANAGER_STOP;
}
