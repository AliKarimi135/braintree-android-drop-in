package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;

import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.internal.AnalyticsDatabase;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.cardform.R;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static com.braintreepayments.api.test.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.api.test.WaitForActivityHelper.waitForActivityToFinish;
import static com.braintreepayments.api.utils.PaymentFormHelpers.clickPayPalButton;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInCardForm;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class AnalyticsTest extends BraintreePaymentActivityTestRunner {

    private BraintreeFragment mFragment;
    private BraintreePaymentActivity mActivity;

    @After
    public void cleanup() {
        clearAllEvents(getTargetContext());
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKInitialized() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        verifyAnalyticsEvent(mActivity, "dropin.appeared");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddCardStarted() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        waitForView(withId(R.id.bt_card_form_card_number)).perform(click());

        verifyAnalyticsEvent(mActivity, "dropin.card.form.focused");
    }

    @Test(timeout = 30000)
    public void addsEventOnLocalCardValidationSuccess() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());

        verifyAnalyticsEvent(mActivity, "dropin.card.form.submitted.succeeded");
    }

    @Test(timeout = 30000)
    public void addsEventOnLocalCardValidationFailure() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());

        verifyAnalyticsEvent(mActivity, "dropin.card.form.submitted.failed");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddCardSucceeded() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent(mActivity, "card.nonce-received");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddCardFailed() {
        setupActivity(new TestClientTokenBuilder()
                .withCvvVerification()
                .withAnalytics()
                .build());
        waitForAddPaymentFormHeader();

        onView(withHint(R.string.bt_form_hint_card_number)).perform(typeText(VISA));
        onView(withHint(R.string.bt_form_hint_expiration)).perform(typeText("0619"), closeSoftKeyboard());
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());

        verifyAnalyticsEvent(mActivity, "add-card.failed");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddPayPalStarted() {
        String clientToken = new TestClientTokenBuilder()
                .withPayPal()
                .withAnalytics()
                .build();

        String config = new TestConfigurationBuilder()
                .paypalEnabled(true)
                .withAnalytics()
                .build();

        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.MOCK_CONFIGURATION, config);

        mActivity = getActivity(intent);
        mFragment = mActivity.mBraintreeFragment;
        waitForAddPaymentFormHeader();

        clickPayPalButton();

        verifyAnalyticsEvent(mActivity, "paypal.future-payments.selected");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddPayPalSucceeded() throws InterruptedException {
        Looper.prepare();

        String clientToken = new TestClientTokenBuilder()
                .withPayPal()
                .withAnalytics()
                .build();

        String config = new TestConfigurationBuilder()
                .paypalEnabled(true)
                .withAnalytics()
                .build();

        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.MOCK_CONFIGURATION, config);

        mActivity = getActivity(intent);
        mActivity.mBraintreeFragment = spy(mActivity.mBraintreeFragment);
        mFragment = mActivity.mBraintreeFragment;
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PayPalAccountNonce payPalAccountNonce = new PayPalAccountNonce();
                        mFragment.postCallback(payPalAccountNonce);
                        countDownLatch.countDown();
                    }
                }, 500);
                return null;
            }
        }).when(mFragment).startActivity(any(Intent.class));

        PayPal.authorizeAccount(mFragment);

        countDownLatch.await();
        verifyAnalyticsEvent(mActivity, "add-paypal.success");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithSuccess() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(
                click());
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent(mActivity, "sdk.exit.success");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithUserCanceled() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent(mActivity, "sdk.exit.user-canceled");
    }

    @Test(timeout = 30000)
    public void doesNotCrashWhenUserExitsRightAfterDropInIsLaunched() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_inflated_loading_view));
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);
        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithDeveloperError() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        mFragment.postCallback(new AuthenticationException(""));
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent(mActivity, "sdk.exit.developer-error");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithServerError() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        mFragment.postCallback(new ServerException(""));
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent(mActivity, "sdk.exit.server-error");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithServerUnavailableError() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        mFragment.postCallback(new DownForMaintenanceException(""));
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent(mActivity, "sdk.exit.server-unavailable");
    }

    /* helpers */
    private void setupActivity(String clientToken) {
        mActivity = getActivity(clientToken);
        mFragment = mActivity.mBraintreeFragment;
    }

    private static void clearAllEvents(Context context) {
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context.getApplicationContext());
        database.getWritableDatabase().delete("analytics", null, null);
        database.close();
    }

    private static boolean verifyAnalyticsEvent(Context context, String eventFragment) {
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context.getApplicationContext());
        Cursor c = database.getReadableDatabase().query("analytics", new String[]{"event"}, "event like ?",
                new String[]{eventFragment}, null, null, null);
        return c.getCount() == 1;
    }
}
