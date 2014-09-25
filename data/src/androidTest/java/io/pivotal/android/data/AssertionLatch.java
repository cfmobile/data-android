/*
 * Copyright (C) 2014 Pivotal Software, Inc. All rights reserved.
 */
package io.pivotal.android.data;

import junit.framework.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AssertionLatch extends CountDownLatch {

	public AssertionLatch(final int count) {
        super(count);
	}

	@Override
	public void countDown() {
		final long count = getCount();
		if (count == 0) {
			Assert.fail("This latch has already finished.");
		} else {
			super.countDown();
		}
	}

	public void assertComplete() {
		try {
			Assert.assertTrue(await(0, TimeUnit.SECONDS));
		} catch (final InterruptedException e) {
			Assert.fail();
		}
	}
}