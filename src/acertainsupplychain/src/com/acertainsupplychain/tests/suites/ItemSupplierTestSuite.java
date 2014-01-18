package com.acertainsupplychain.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.acertainsupplychain.tests.ItemSupplierAdvanced;
import com.acertainsupplychain.tests.ItemSupplierSimple;

@RunWith(Suite.class)
@SuiteClasses({ ItemSupplierSimple.class, ItemSupplierAdvanced.class })
public class ItemSupplierTestSuite {

}
