package com.acertainsupplychain.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.acertainsupplychain.tests.ItemSupplierSimple;
import com.acertainsupplychain.tests.OrderManagerSimple;

@RunWith(Suite.class)
@SuiteClasses({ ItemSupplierSimple.class, OrderManagerSimple.class })
public class SimpleTestSuite {

}
