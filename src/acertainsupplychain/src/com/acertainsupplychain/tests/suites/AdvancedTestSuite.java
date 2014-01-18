package com.acertainsupplychain.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.acertainsupplychain.tests.ItemSupplierAdvanced;
import com.acertainsupplychain.tests.OrderManagerAdvanced;

@RunWith(Suite.class)
@SuiteClasses({ ItemSupplierAdvanced.class, OrderManagerAdvanced.class })
public class AdvancedTestSuite {

}
