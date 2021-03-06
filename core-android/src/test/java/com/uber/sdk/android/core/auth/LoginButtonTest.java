/*
 * Copyright (c) 2016 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.sdk.android.core.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.google.common.collect.Sets;
import com.uber.sdk.android.core.RobolectricTestBase;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.client.SessionConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.res.Attribute;
import org.robolectric.shadows.CoreShadowsAdapter;
import org.robolectric.shadows.RoboAttributeSet;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class LoginButtonTest extends RobolectricTestBase {

    private static final String UBER_PACKAGE_NAME = "com.uber.sdk.android.core";
    private static final HashSet<Scope> SCOPES = Sets.newHashSet(Scope.HISTORY, Scope.REQUEST_RECEIPT);
    private static final int REQUEST_CODE = 11133;
    private Activity activity;

    @Mock
    LoginManager loginManager;

    @Mock
    LoginCallback loginCallback;

    @Mock
    AccessTokenManager accessTokenManager;

    private LoginButton loginButton;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void testButtonClickWithAllParametersSetFromJava_shouldTriggerLogin() {
        loginButton = new TestLoginButton(activity, loginManager);
        loginButton.setCallback(loginCallback);
        loginButton.setScopes(SCOPES);
        loginButton.setRequestCode(REQUEST_CODE);

        loginButton.callOnClick();

        verify(loginManager).login(eq(activity));
    }

    @Test
    public void testButtonClickWithoutRequestCode_shouldUseDefaultCode() {
        loginButton = new TestLoginButton(activity, loginManager);
        loginButton.setCallback(loginCallback);
        loginButton.setScopes(SCOPES);

        loginButton.callOnClick();

        verify(loginManager).login(eq(activity));
    }

    @Test
    public void testButtonClickWithScopesFromXml_shouldUseParseScopes() {
        AttributeSet attributeSet = makeAttributeSet(
                makeAttribute(UBER_PACKAGE_NAME + ":attr/ub__scopes", "history|request_receipt")
        );

        loginButton = new TestLoginButton(activity, attributeSet, loginManager);
        loginButton.setCallback(loginCallback);

        loginButton.callOnClick();

        verify(loginManager).login(eq(activity));
    }

    @Test
    public void testButtonClickWithScopesRequestCodeFromXml_shouldUseParseAll() {
        AttributeSet attributeSet = makeAttributeSet(
                makeAttribute(UBER_PACKAGE_NAME + ":attr/ub__scopes", "history|request_receipt"),
                makeAttribute(UBER_PACKAGE_NAME + ":attr/ub__request_code", REQUEST_CODE)
        );

        loginButton = new TestLoginButton(activity, attributeSet, loginManager);
        loginButton.setCallback(loginCallback);

        loginButton.callOnClick();

        verify(loginManager).login(eq(activity));
    }

    @Test
    public void testButtonClickWithoutLoginManager_shouldCreateNew() {
        AttributeSet attributeSet = makeAttributeSet(
                makeAttribute(UBER_PACKAGE_NAME + ":attr/ub__scopes", "history|request_receipt"),
                makeAttribute(UBER_PACKAGE_NAME + ":attr/ub__request_code", REQUEST_CODE)
        );

        loginButton = new LoginButton(activity, attributeSet);
        loginButton.setSessionConfiguration(new SessionConfiguration.Builder().setClientId("clientId").build());
        loginButton.setCallback(loginCallback);
        loginButton.setScopes(SCOPES);
        loginButton.setAccessTokenManager(accessTokenManager);
        loginButton.callOnClick();

        assertThat(loginButton.getLoginManager()).isNotNull();
        assertThat(loginButton.getLoginManager().getAccessTokenManager())
                .isEqualTo(accessTokenManager);
    }

    @Test
    public void testOnActivityResult_shouldCascadeLoginManager() {
        loginButton = new TestLoginButton(activity, loginManager);
        loginButton.setCallback(loginCallback);

        final Intent intent = mock(Intent.class);

        loginButton.onActivityResult(LoginManager.REQUEST_CODE_LOGIN_DEFAULT, 1, intent);

        verify(loginManager).onActivityResult(eq(activity), eq(LoginManager.REQUEST_CODE_LOGIN_DEFAULT), eq(1),
                eq(intent));
    }

    @Test
    public void testOnActivityResultDifferentResultCode_shouldNotCallLoginManager() {
        loginButton = new TestLoginButton(activity, loginManager);
        loginButton.setCallback(loginCallback);

        final Intent intent = mock(Intent.class);

        loginButton.onActivityResult(REQUEST_CODE, 1, intent);

        verify(loginManager, never()).onActivityResult(eq(activity), anyInt(), anyInt(), any(Intent.class));

    }

    private static class TestLoginButton extends LoginButton {
        private LoginManager manager;

        public TestLoginButton(Context context, LoginManager loginManager) {
            super(context);
            this.manager = loginManager;
        }

        public TestLoginButton(Context context, AttributeSet attrs, LoginManager manager) {
            super(context, attrs);
            this.manager = manager;
        }

        @NonNull
        @Override
        protected LoginManager getOrCreateLoginManager() {
            return manager;
        }
    }

    private static AttributeSet makeAttributeSet(Attribute... attributes) {
        return new RoboAttributeSet(Arrays.asList(attributes), new CoreShadowsAdapter().getResourceLoader());
    }

    private static Attribute makeAttribute(String fullyQualifiedAttributeName, Object value) {
        return new Attribute(fullyQualifiedAttributeName, String.valueOf(value), UBER_PACKAGE_NAME);
    }
}