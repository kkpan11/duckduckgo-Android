package com.duckduckgo.subscriptions.impl.ui

import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.subscriptions.impl.RealSubscriptionsManager.Companion.SUBSCRIPTION_NOT_FOUND_ERROR
import com.duckduckgo.subscriptions.impl.RealSubscriptionsManager.RecoverSubscriptionResult
import com.duckduckgo.subscriptions.impl.SubscriptionStatus.AUTO_RENEWABLE
import com.duckduckgo.subscriptions.impl.SubscriptionStatus.EXPIRED
import com.duckduckgo.subscriptions.impl.SubscriptionsChecker
import com.duckduckgo.subscriptions.impl.SubscriptionsManager
import com.duckduckgo.subscriptions.impl.pixels.SubscriptionPixelSender
import com.duckduckgo.subscriptions.impl.repository.Entitlement
import com.duckduckgo.subscriptions.impl.repository.Subscription
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.Error
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.RestoreFromEmail
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.SubscriptionNotFound
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.Success
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RestoreSubscriptionViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val subscriptionsManager: SubscriptionsManager = mock()
    private val pixelSender: SubscriptionPixelSender = mock()
    private val subscriptionsChecker: SubscriptionsChecker = mock()
    private lateinit var viewModel: RestoreSubscriptionViewModel

    @Before
    fun before() {
        viewModel = RestoreSubscriptionViewModel(
            subscriptionsManager = subscriptionsManager,
            dispatcherProvider = coroutineTestRule.testDispatcherProvider,
            pixelSender = pixelSender,
            subscriptionsChecker = subscriptionsChecker,
        )
    }

    @Test
    fun whenRestoreFromEmailThenSendCommand() = runTest {
        viewModel.commands().test {
            viewModel.restoreFromEmail()
            assertTrue(awaitItem() is RestoreFromEmail)
        }
    }

    @Test
    fun whenRestoreFromStoreIfFailureThenReturnError() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure("error"),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is Error)
        }
    }

    @Test
    fun whenRestoreFromStoreIfNoSubscriptionFoundThenReturnNotFound() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure(SUBSCRIPTION_NOT_FOUND_ERROR),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is SubscriptionNotFound)
        }
    }

    @Test
    fun whenRestoreFromStoreIfNotActiveThenReturnNotFound() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionNotActive()),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is SubscriptionNotFound)
        }
    }

    @Test
    fun whenRestoreFromStoreIfActiveThenReturnSuccess() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionActive()),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is Success)
        }
    }

    @Test
    fun whenRestoreFromStoreClickThenPixelIsSent() = runTest {
        viewModel.restoreFromStore()
        verify(pixelSender).reportActivateSubscriptionRestorePurchaseClick()
    }

    @Test
    fun whenRestoreFromEmailClickThenPixelIsSent() = runTest {
        viewModel.restoreFromEmail()
        verify(pixelSender).reportActivateSubscriptionEnterEmailClick()
    }

    @Test
    fun whenRestoreFromStoreSuccessThenPixelIsSent() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionActive()),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreSuccess()
    }

    @Test
    fun whenRestoreFromStoreFailsBecauseThereAreNoEntitlementsThenPixelIsSent() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionNotActive()),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreFailureSubscriptionNotFound()
    }

    @Test
    fun whenRestoreFromStoreFailsBecauseThereIsNoSubscriptionThenPixelIsSent() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure(SUBSCRIPTION_NOT_FOUND_ERROR),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreFailureSubscriptionNotFound()
    }

    @Test
    fun whenRestoreFromStoreFailsForOtherReasonThenPixelIsSent() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure("bad stuff happened"),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreFailureOther()
    }

    private fun subscriptionNotActive(): Subscription {
        return Subscription(
            productId = "productId",
            startedAt = 10000L,
            expiresOrRenewsAt = 10000L,
            status = EXPIRED,
            platform = "google",
            entitlements = emptyList(),
        )
    }

    private fun subscriptionActive(): Subscription {
        return Subscription(
            productId = "productId",
            startedAt = 10000L,
            expiresOrRenewsAt = 10000L,
            status = AUTO_RENEWABLE,
            platform = "google",
            entitlements = listOf(Entitlement("name", "product")),
        )
    }
}
