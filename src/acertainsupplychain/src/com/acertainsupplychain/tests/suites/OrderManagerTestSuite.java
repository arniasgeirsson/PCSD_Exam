package com.acertainsupplychain.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.acertainsupplychain.tests.OrderManagerAdvanced;
import com.acertainsupplychain.tests.OrderManagerSimple;

@RunWith(Suite.class)
@SuiteClasses({ OrderManagerSimple.class, OrderManagerAdvanced.class })
public class OrderManagerTestSuite {

}
