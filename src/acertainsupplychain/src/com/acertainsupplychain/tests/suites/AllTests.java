package com.acertainsupplychain.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.acertainsupplychain.tests.AtomicityTests;
import com.acertainsupplychain.tests.FailureHandlingTests;
import com.acertainsupplychain.tests.FileLoggerTests;
import com.acertainsupplychain.tests.ItemSupplierAdvanced;
import com.acertainsupplychain.tests.ItemSupplierSimple;
import com.acertainsupplychain.tests.ItemSupplierUtilityTests;
import com.acertainsupplychain.tests.OrderManagerAdvanced;
import com.acertainsupplychain.tests.OrderManagerSimple;
import com.acertainsupplychain.tests.TestUtilityTests;

@RunWith(Suite.class)
@SuiteClasses({ AtomicityTests.class, FailureHandlingTests.class,
		FileLoggerTests.class, ItemSupplierAdvanced.class,
		ItemSupplierSimple.class, ItemSupplierUtilityTests.class,
		OrderManagerAdvanced.class, OrderManagerSimple.class,
		TestUtilityTests.class })
public class AllTests {

}
