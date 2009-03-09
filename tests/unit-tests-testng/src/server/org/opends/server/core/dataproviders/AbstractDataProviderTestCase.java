/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2008-2009 Sun Microsystems, Inc.
 */
package org.opends.server.core.dataproviders;



import static org.testng.Assert.*;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.opends.messages.Message;
import org.opends.server.core.operations.AddRequest;
import org.opends.server.core.operations.BindRequest;
import org.opends.server.core.operations.CompareRequest;
import org.opends.server.core.operations.Context;
import org.opends.server.core.operations.DeleteRequest;
import org.opends.server.core.operations.ExtendedRequest;
import org.opends.server.core.operations.ExtendedResponseHandler;
import org.opends.server.core.operations.ModifyDNRequest;
import org.opends.server.core.operations.ModifyRequest;
import org.opends.server.core.operations.ResponseHandler;
import org.opends.server.core.operations.SearchRequest;
import org.opends.server.core.operations.SearchResponseHandler;
import org.opends.server.types.CanceledOperationException;
import org.opends.server.types.DN;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchFilter;
import org.opends.server.types.SearchScope;
import org.testng.annotations.Test;



/**
 * This class tests {@link DataProviderEvent}.
 */
public final class AbstractDataProviderTestCase extends
    DataProviderTestCase
{

  /**
   * Mock data provider.
   */
  private final static class MyDataProvider extends
      AbstractDataProvider
  {

    public void finalizeDataProvider()
    {
      // Nothing to do.
    }



    public void startDataProvider()
    {
      // Nothing to do.
    }



    public void stopDataProvider()
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeAdd(Context context, AddRequest request,
        ResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeBind(Context context, BindRequest request,
        ResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeCompare(Context context, CompareRequest request,
        ResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeDelete(Context context, DeleteRequest request,
        ResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeExtended(Context context,
        ExtendedRequest request, ExtendedResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeModify(Context context, ModifyRequest request,
        ResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeModifyDN(Context context,
        ModifyDNRequest request, ResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public void executeSearch(Context context, SearchRequest request,
        SearchResponseHandler responseHandler)
        throws CanceledOperationException, DirectoryException
    {
      // Nothing to do.
    }



    /**
     * {@inheritDoc}
     */
    public Set<DN> getBaseDNs()
    {
      // No implementation required.
      return Collections.emptySet();
    }



    /**
     * {@inheritDoc}
     */
    public Entry getEntry(DN dn) throws DirectoryException
    {
      // No implementation required.
      throw new RuntimeException();
    }



    /**
     * {@inheritDoc}
     */
    public DataProviderStatus getStatus(DN baseDN)
        throws DirectoryException
    {
      // No implementation required.
      return DataProviderStatus.ENABLED;
    }



    /**
     * {@inheritDoc}
     */
    public Set<String> getSupportedControls(DN baseDN)
        throws DirectoryException
    {
      // No implementation required.
      return Collections.emptySet();
    }



    /**
     * {@inheritDoc}
     */
    public Set<String> getSupportedFeatures(DN baseDN)
        throws DirectoryException
    {
      // No implementation required.
      return Collections.emptySet();
    }



    /**
     * {@inheritDoc}
     */
    public void search(DN baseDN, SearchScope scope,
        SearchFilter filter, DataProviderSearchHandler handler)
        throws DirectoryException
    {
      // No implementation required.
    }

  };

  /**
   * Mock listener.
   */
  private static final class TestListener implements
      DataProviderEventListener
  {

    public DataProviderEvent event = null;



    /**
     * {@inheritDoc}
     */
    public void dataProviderEventOccurred(DataProviderEvent event)
    {
      this.event = event;
    }

  };



  /**
   * Tests notifyDataProviderStateChanged with no listeners.
   */
  @Test
  public void testNotifyDataProviderStateChangedWithNoListeners1()
  {
    AbstractDataProvider provider = new MyDataProvider();
    provider.notifyDataProviderEventOccurred(Message.EMPTY, EnumSet
        .of(DataProviderEvent.Type.ACCESS_MODE));
  }



  /**
   * Tests notifyDataProviderStateChanged with no listeners.
   */
  @Test
  public void testNotifyDataProviderStateChangedWithNoListeners2()
  {
    AbstractDataProvider provider = new MyDataProvider();
    DataProviderEvent event =
        new DataProviderEvent(Message.EMPTY, EnumSet
            .of(DataProviderEvent.Type.ACCESS_MODE));
    provider.notifyDataProviderEventOccurred(event);
  }



  /**
   * Tests listener registration / deregistration.
   */
  @Test
  public void testNotifyDataProviderStateChangedWithSingleListener1()
  {
    AbstractDataProvider provider = new MyDataProvider();
    TestListener listener = new TestListener();

    // Test notification occurs after registration.
    provider.registerEventListener(listener);
    provider.notifyDataProviderEventOccurred(Message.EMPTY, EnumSet
        .of(DataProviderEvent.Type.ACCESS_MODE));

    assertNotNull(listener.event);

    // Test notification event is correct.
    assertSame(listener.event.getReason(), Message.EMPTY);
    assertEquals(listener.event.getEventTypes(), EnumSet
        .of(DataProviderEvent.Type.ACCESS_MODE));

    // Test notification stops after deregistration.
    provider.deregisterEventListener(listener);
    listener.event = null;

    provider.notifyDataProviderEventOccurred(Message.EMPTY, EnumSet
        .of(DataProviderEvent.Type.ACCESS_MODE));

    assertNull(listener.event);
  }



  /**
   * Tests listener registration / deregistration.
   */
  @Test
  public void testNotifyDataProviderStateChangedWithSingleListener2()
  {
    AbstractDataProvider provider = new MyDataProvider();
    TestListener listener = new TestListener();
    DataProviderEvent event =
        new DataProviderEvent(Message.EMPTY, EnumSet
            .of(DataProviderEvent.Type.ACCESS_MODE));

    // Test notification occurs after registration.
    provider.registerEventListener(listener);
    provider.notifyDataProviderEventOccurred(event);

    assertNotNull(listener.event);

    // Test notification event is correct.
    assertSame(listener.event.getReason(), event.getReason());
    assertEquals(listener.event.getEventTypes(), event.getEventTypes());

    // Test notification stops after deregistration.
    provider.deregisterEventListener(listener);
    listener.event = null;

    provider.notifyDataProviderEventOccurred(event);

    assertNull(listener.event);
  }

}
