package com.unhuman.dataBuilder.descriptor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractCohesiveDataDescriptorTest {
    FirstNameDescriptor firstNameDescriptor = new FirstNameDescriptor("last");
    LastNameDescriptor lastNameDescriptor = new LastNameDescriptor("first");
    EmailDescriptor emailDescriptor = new EmailDescriptor("email");

    @Before
    public void setup() {
        AbstractCohesiveDataDescriptor.reset();
    }

    @Test
    public void testFirstNames() {
        String one = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL);
        String two = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL);
        Assert.assertNotEquals(one, two);
    }

    @Test
    public void testLastNames() {
        String one = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL);
        String two = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL);
        Assert.assertNotEquals(one, two);
    }

    @Test
    public void testEmail() {
        String one = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL);
        String two = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL);
        Assert.assertNotEquals(one, two);
    }

    @Test
    public void testAllOrder1() {
        String first = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String last = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String email = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        Assert.assertTrue(email.startsWith(first.substring(0, 1)));
        Assert.assertTrue(email.substring(1).startsWith(last));
    }

    @Test
    public void testAllOrder2() {
        String email = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String first = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String last = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        Assert.assertTrue(email.startsWith(first.substring(0, 1)));
        Assert.assertTrue(email.substring(1).startsWith(last));
    }

    @Test
    public void testCheckFirst() {
        String first = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String last = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String email = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        Assert.assertTrue(email.startsWith(first.substring(0, 1)));
        Assert.assertTrue(email.substring(1).startsWith(last));

        String first2 = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");

        Assert.assertNotEquals(first, first2);
    }

    @Test
    public void testCheckLast() {
        String first = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String last = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String email = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        Assert.assertTrue(email.startsWith(first.substring(0, 1)));
        Assert.assertTrue(email.substring(1).startsWith(last));

        String last2 = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");

        Assert.assertNotEquals(last, last2);
    }

    @Test
    public void testCheckEmail() {
        String first = firstNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String last = lastNameDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        String email = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");
        Assert.assertTrue(email.startsWith(first.substring(0, 1)));
        Assert.assertTrue(email.substring(1).startsWith(last));

        String email2 = emailDescriptor.getNextValue(AbstractEntityTypeDescriptor.NullHandler.AS_NULL).replace("\"", "");

        Assert.assertNotEquals(email, email2);
    }

}