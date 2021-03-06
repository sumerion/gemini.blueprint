/******************************************************************************
 * Copyright (c) 2006, 2010 VMware Inc., Oracle Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at 
 * http://www.eclipse.org/legal/epl-v10.html and the Apache License v2.0
 * is available at http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses. 
 * 
 * Contributors:
 *   VMware Inc.
 *   Oracle Inc.
 *****************************************************************************/

package org.eclipse.gemini.blueprint.iandt.dependencyWithDelay;

import java.io.FilePermission;
import java.security.Permission;
import java.util.List;

import org.eclipse.gemini.blueprint.iandt.BaseIntegrationTest;
import org.junit.Test;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.eclipse.gemini.blueprint.util.OsgiStringUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * @author Hal Hildebrand Date: Aug 27, 2007 Time: 9:36:23 AM
 */

public class FactoryDependencyTest extends BaseIntegrationTest {

	private static final String DEPENDENT_CLASS_NAME = "org.eclipse.gemini.blueprint.iandt.dependencies.Dependent";

	private static final String DELAY_PROP = "org.eclipse.gemini.blueprint.iandt.dependencies.factory.delay";

	protected String getManifestLocation() {
		return null;
	}

	// dependency bundle - depends on service2, service3 and, through a nested
	// reference, to service1
	// simple.service2 - publishes service2
	// simple.service3 - publishes service3
	// simple - publishes service1
	@Test
	public void testDependencies() throws Exception {
		System.setProperty(DELAY_PROP, "10000");

		final Bundle dependencyTestBundle = installTestBundle("dependencies");
		final Bundle simpleService2Bundle = installTestBundle("simple.service2");
		final Bundle simpleService3Bundle = installTestBundle("simple.service3");
		final Bundle factoryServiceBundle = installTestBundle("dependendencies.factory");

		assertNotNull("Cannot find the factory service bundle", factoryServiceBundle);
		assertNotNull("Cannot find the simple service 2 bundle", simpleService2Bundle);
		assertNotNull("Cannot find the simple service 3 bundle", simpleService3Bundle);
		assertNotNull("dependencyTest can't be resolved", dependencyTestBundle);

		assertNotSame("simple service bundle is in the activated state!", Bundle.ACTIVE, factoryServiceBundle.getState());
		assertNotSame("simple service 2 bundle is in the activated state!", Bundle.ACTIVE, simpleService2Bundle.getState());
		assertNotSame("simple service 3 bundle is in the activated state!", Bundle.ACTIVE, simpleService3Bundle.getState());

		startDependencyAsynch(dependencyTestBundle);
		Thread.sleep(2000); // Yield to give bundle time to get into waiting
		// state.
		ServiceReference dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		startDependency(simpleService3Bundle);

		dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		startDependency(simpleService2Bundle);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		startDependency(factoryServiceBundle);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		waitOnContextCreation("org.eclipse.gemini.blueprint.iandt.dependencies");

		dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		assertNotNull("Service has not been started!", dependentRef);

		Object dependent = bundleContext.getService(dependentRef);

		assertNotNull("Service is not available!", dependent);

	}

	private void startDependency(Bundle bundle) throws BundleException, InterruptedException {
		bundle.start();
		waitOnContextCreation(bundle.getSymbolicName());
		System.out.println("started bundle [" + OsgiStringUtils.nullSafeSymbolicName(bundle) + "]");
	}

	private void startDependencyAsynch(final Bundle bundle) {
		System.out.println("starting dependency test bundle");
		Runnable runnable = new Runnable() {

			public void run() {
				try {
					bundle.start();
					System.out.println("started dependency test bundle");
				}
				catch (BundleException ex) {
					System.err.println("can't start bundle " + ex);
				}
			}
		};
		Thread thread = new Thread(runnable);
		thread.setDaemon(false);
		thread.setName("dependency test bundle");
		thread.start();
	}

	protected boolean shouldWaitForSpringBundlesContextCreation() {
		return true;
	}

	protected long getDefaultWaitTime() {
		return 60L;
	}

	protected List<Permission> getTestPermissions() {
		final List<Permission> list = super.getTestPermissions();
		list.add(new FilePermission("<<ALL FILES>>", "read"));
		list.add(new AdminPermission("*", AdminPermission.LIFECYCLE));
		list.add(new AdminPermission("*", AdminPermission.EXECUTE));
		list.add(new AdminPermission("*", AdminPermission.RESOLVE));
		list.add(new AdminPermission("*", AdminPermission.METADATA));
		return list;
	}
}