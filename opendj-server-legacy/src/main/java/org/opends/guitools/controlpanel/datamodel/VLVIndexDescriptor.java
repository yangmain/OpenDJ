/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2008-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2015 ForgeRock AS
 */

package org.opends.guitools.controlpanel.datamodel;

import java.util.Collections;
import java.util.List;

import org.opends.server.admin.std.meta.LocalDBVLVIndexCfgDefn.Scope;
import org.opends.server.types.DN;

/**
 * The class used to describe the VLV index configuration.
 */
public class VLVIndexDescriptor extends AbstractIndexDescriptor
{
  private final DN baseDN;
  private final Scope scope;
  private final String filter;
  private List<VLVSortOrder> sortOrder = Collections.emptyList();
  private final int maxBlockSize;
  private int hashCode;

  /**
   * Constructor for the VLVIndexDescriptor.
   *
   * @param name
   *          the name of the index.
   * @param backend
   *          the backend where the index is defined.
   * @param baseDN
   *          the baseDN of the search indexed by the VLV index.
   * @param scope
   *          the scope of the search indexed by the VLV index.
   * @param filter
   *          the filter or the search indexed by the VLV index.
   * @param sortOrder
   *          the sort order list of the VLV index.
   * @param maxBlockSize
   *          the maximum block size of the VLV index.
   */
  public VLVIndexDescriptor(String name, BackendDescriptor backend, DN baseDN, Scope scope, String filter,
      List<VLVSortOrder> sortOrder, int maxBlockSize)
  {
    super(name, backend);
    this.baseDN = baseDN;
    this.scope = scope;
    this.filter = filter;
    this.sortOrder = Collections.unmodifiableList(sortOrder);
    this.maxBlockSize = maxBlockSize;

    recalculateHashCode();
  }

  @Override
  public int compareTo(AbstractIndexDescriptor o)
  {
    return getName().toLowerCase().compareTo(o.getName().toLowerCase());
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * Returns the baseDN of the search indexed by the VLV index.
   *
   * @return the baseDN of the search indexed by the VLV index.
   */
  public DN getBaseDN()
  {
    return baseDN;
  }

  /**
   * Returns the filter of the search indexed by the VLV index.
   *
   * @return the filter of the search indexed by the VLV index.
   */
  public String getFilter()
  {
    return filter;
  }

  /**
   * Returns the scope of the search indexed by the VLV index.
   *
   * @return the scope of the search indexed by the VLV index.
   */
  public Scope getScope()
  {
    return scope;
  }

  /**
   * Returns the sort order list of the VLV index.
   *
   * @return the sort order list of the VLV index.
   */
  public List<VLVSortOrder> getSortOrder()
  {
    return sortOrder;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
    {
      return true;
    }
    if (!(o instanceof VLVIndexDescriptor))
    {
      return false;
    }

    final VLVIndexDescriptor index = (VLVIndexDescriptor) o;
    return index.getName().equalsIgnoreCase(getName())
        && index.getBaseDN().equals(getBaseDN())
        && index.getFilter().equals(getFilter())
        && index.getScope() == getScope()
        && index.getSortOrder().equals(getSortOrder())
        && backendIdEqual(index);
  }

  private boolean backendIdEqual(VLVIndexDescriptor index)
  {
    return getBackend() != null
        && index.getBackend() != null
        // Only compare the backend IDs.  In this context is better to
        // do this since the backend object contains some state (like
        // number entries) that can change.
        && getBackend().getBackendID().equals(index.getBackend().getBackendID());
  }

  @Override
  protected void recalculateHashCode()
  {
    final StringBuilder sb = new StringBuilder();
    for (final VLVSortOrder s : sortOrder)
    {
      sb.append(s.getAttributeName()).append(s.isAscending()).append(",");
    }
    if (getBackend() != null)
    {
      sb.append(getBackend().getBackendID());
    }
    hashCode = (getName()+baseDN+scope+filter+sb+maxBlockSize).hashCode();
  }

  /**
   * Returns the maximum block size of the VLV index.
   *
   * @return the maximum block size of the VLV index.
   */
  public int getMaxBlockSize()
  {
    return maxBlockSize;
  }

}