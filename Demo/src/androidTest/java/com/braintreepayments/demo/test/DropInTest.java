package com.braintreepayments.demo.test;

import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAction.setText;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextContaining;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

@RunWith(AndroidJUnit4.class)
public class DropInTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Drop-In")).waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void tokenizesACard() {
        onDevice(withText("Credit or Debit Card")).perform(click());
        onDevice(withContentDescription("Card Number")).perform(setText("4111111111111111"));
        onDevice(withText("12")).perform(click());
        onDevice(withText("2019")).perform(click());
        onDevice().pressBack();
        onDevice(withContentDescription("CVV")).perform(setText("123"));
        onDevice(withContentDescription("Postal Code")).perform(setText("12345"));
        onDevice(withTextContaining("Add Card")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 11")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @RequiresDevice
    @Test(timeout = 60000)
    public void tokenizesAndroidPay() {
        onDevice(withText("Android Pay")).perform(click());
        onDevice(withText("CONTINUE")).perform(click());

        getNonceDetails().check(text(containsString("Underlying Card Last Two")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 60000)
    public void exitsAfterCancelingAddingAPaymentMethod() {
        onDevice(withText("PayPal")).perform(click());
        onDevice(withContentDescription("Proceed with Sandbox Purchase")).waitForExists();
        onDevice().pressBack();
        onDevice(withContentDescription("Pay with PayPal")).waitForExists();

        onDevice().pressBack();

        onDevice(withText("Drop-In")).check(text(equalToIgnoringCase("Drop-In")));
    }
}
