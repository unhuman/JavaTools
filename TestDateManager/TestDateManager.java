package com.unhuman.test;

import java.util.Date;

/**
 * TestDateManager class for spoofing time
 *
 * Usage:
 *    * Important: when getting system time, you should use new Date().getTime() rather than System.currentTimeMillis()
 *                 This is because we want the application to get time in a manner consistent with this manager
 *
 *    * Annotate your test class with:
 *      @PrepareForTest(fullyQualifiedNames="your.package.domain.*")
 *      which cannot collide with this package
 *
 *    * In your @Before setup method, add:
 *
 *         // every time we call new Date() inside a method of any class
 *         // declared in @PrepareForTest we will get the FakeData instance
 *         testDateManager = new TestDateManager();
 *
 *         // new Date() - we want to get a copy of the current testDateManager Date (since it can change)
 *         PowerMockito.whenNew(Date.class).withNoArguments().thenAnswer(answer ->
 *             TestDateManager.createDate(testDateManager.getTime())
 *         );
 *         // new Date(x) - respect the time specified
 *         PowerMockito.whenNew(Date.class).withArguments(anyLong()).thenAnswer(answer -> {
 *             long time = answer.getArgument(0);
 *             return TestDateManager.createDate(time);
 *         });
 */
public class TestDateManager extends Date {
    public TestDateManager() {
        long currentTime = new Date().getTime();
        setTime(currentTime);
    }

    /**
     * Advance the internal time
     * Use this instead of Thread.sleep() in tests
     * @param advanceMilliseconds
     */
    public void advanceTimeMilliseconds(long advanceMilliseconds) {
        long currentTime = getTime() + advanceMilliseconds;
        this.setTime(currentTime);
    }

    /**
     * Method to get a real Date object based on time specified
     * @param explicitTimeMilliseconds
     * @return
     */
    public static Date createDate(long explicitTimeMilliseconds) {
        return new Date(explicitTimeMilliseconds);
    }
}
