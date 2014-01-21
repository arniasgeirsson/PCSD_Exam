package com.acertainsupplychain.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.acertainsupplychain.tests.AtomicityTests;
import com.acertainsupplychain.tests.FailureHandlingTests;
import com.acertainsupplychain.tests.ItemSupplierAdvanced;
import com.acertainsupplychain.tests.ItemSupplierSimple;
import com.acertainsupplychain.tests.OrderManagerAdvanced;
import com.acertainsupplychain.tests.OrderManagerSimple;

@RunWith(Suite.class)
@SuiteClasses({ AtomicityTests.class, FailureHandlingTests.class,
		ItemSupplierAdvanced.class, ItemSupplierSimple.class,
		OrderManagerAdvanced.class, OrderManagerSimple.class })
public class AllTests {

}
