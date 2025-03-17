package com.example;

import org.testng.Assert;
import org.testng.annotations.*;

public class ExampleTest {

    @BeforeClass
    public void setUpClass() {
        System.out.println("Setting up test class");
    }

    @BeforeMethod
    public void setUpMethod() {
        System.out.println("Setting up test method");
    }

    @Test(groups = "smoke")
    public void testAddition() {
        int a = 5;
        int b = 3;
        int expected = 8;
        int actual = a + b;
        Assert.assertEquals(actual, expected, "Addition test failed");
    }

    @Test(groups = "regression")
    public void testString() {
        String str = "Hello TestNG";
        Assert.assertTrue(str.contains("TestNG"), "String should contain 'TestNG'");
    }

    @Test(groups = "regression")
    @Parameters({ "browser" })
    public void testWithParameter(@Optional("chrome") String browser) {
        System.out.println("Running test with browser: " + browser);
        Assert.assertNotNull(browser, "Browser parameter should not be null");
    }

    @AfterMethod
    public void tearDownMethod() {
        System.out.println("Cleaning up after test method");
    }

    @AfterClass
    public void tearDownClass() {
        System.out.println("Cleaning up test class");
    }
}