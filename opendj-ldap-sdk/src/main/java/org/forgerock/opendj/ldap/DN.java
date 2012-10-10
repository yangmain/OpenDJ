/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 *      Portions copyright 2011-2012 ForgeRock AS.
 */

package org.forgerock.opendj.ldap;



import static org.forgerock.opendj.ldap.CoreMessages.ERR_DN_TYPE_NOT_FOUND;

import java.util.*;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldap.schema.UnknownSchemaElementException;

import com.forgerock.opendj.util.SubstringReader;
import com.forgerock.opendj.util.Validator;



/**
 * A distinguished name (DN) as defined in RFC 4512 section 2.3 is the
 * concatenation of its relative distinguished name (RDN) and its immediate
 * superior's DN. A DN unambiguously refers to an entry in the Directory.
 * <p>
 * The following are examples of string representations of DNs:
 *
 * <pre>
 * UID=nobody@example.com,DC=example,DC=com CN=John
 * Smith,OU=Sales,O=ACME Limited,L=Moab,ST=Utah,C=US
 * </pre>
 *
 * @see <a href="http://tools.ietf.org/html/rfc4512#section-2.3">RFC 4512 -
 *      Lightweight Directory Access Protocol (LDAP): Directory Information
 *      Models </a>
 */
public final class DN implements Iterable<RDN>, Comparable<DN>
{
  private static final DN ROOT_DN = new DN(null, null, "");

  // This is the size of the per-thread per-schema DN cache. We should
  // be conservative here in case there are many threads. We will only
  // cache parent DNs, so there's no need for it to be big.
  private static final int DN_CACHE_SIZE = 32;

  private static final ThreadLocal<WeakHashMap<Schema, Map<String, DN>>> CACHE =
    new ThreadLocal<WeakHashMap<Schema, Map<String, DN>>>()
  {

    /**
     * {@inheritDoc}
     */
    @Override
    protected WeakHashMap<Schema, Map<String, DN>> initialValue()
    {
      return new WeakHashMap<Schema, Map<String, DN>>();
    }

  };



  /**
   * Returns the Root DN. The Root DN does not contain and RDN components and is
   * superior to all other DNs.
   *
   * @return The Root DN.
   */
  public static DN rootDN()
  {
    return ROOT_DN;
  }



  /**
   * Parses the provided LDAP string representation of a DN using the default
   * schema.
   *
   * @param dn
   *          The LDAP string representation of a DN.
   * @return The parsed DN.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public static DN valueOf(final String dn)
  {
    return valueOf(dn, Schema.getDefaultSchema());
  }



  /**
   * Parses the provided LDAP string representation of a DN using the provided
   * schema.
   *
   * @param dn
   *          The LDAP string representation of a DN.
   * @param schema
   *          The schema to use when parsing the DN.
   * @return The parsed DN.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} or {@code schema} was {@code null}.
   */
  public static DN valueOf(final String dn, final Schema schema)
  {
    Validator.ensureNotNull(dn, schema);
    if (dn.length() == 0)
    {
      return ROOT_DN;
    }

    // First check if DN is already cached.
    final Map<String, DN> cache = getCache(schema);
    final DN cachedDN = cache.get(dn);
    if (cachedDN != null)
    {
      return cachedDN;
    }

    // Not in cache so decode.
    final SubstringReader reader = new SubstringReader(dn);
    return decode(dn, reader, schema, cache);
  }



  /**
   * Compares the provided DN values to determine their relative order in a
   * sorted list.
   *
   * @param dn1
   *          The first DN to be compared. It must not be {@code null}.
   * @param dn2
   *          The second DN to be compared. It must not be {@code null}.
   * @return A negative integer if the first DN should come before the second DN
   *         in a sorted list, a positive integer if the first DN should come
   *         after the second DN in a sorted list, or zero if the two DN values
   *         can be considered equal.
   */
  private static int compareTo(final DN dn1, final DN dn2)
  {
    // Quickly check if we are comparing against root dse.
    if (dn1.isRootDN())
    {
      if (dn2.isRootDN())
      {
        // both are equal.
        return 0;
      }
      else
      {
        // dn1 comes before dn2.
        return -1;
      }
    }

    if (dn2.isRootDN())
    {
      // dn1 comes after dn2.
      return 1;
    }

    int dn1Size = dn1.size - 1;
    int dn2Size = dn2.size - 1;
    while (dn1Size >= 0 && dn2Size >= 0)
    {
      final DN dn1Parent = dn1.parent(dn1Size--);
      final DN dn2Parent = dn2.parent(dn2Size--);
      final int result = dn1Parent.rdn.compareTo(dn2Parent.rdn);
      if (result > 0)
      {
        return 1;
      }
      else if (result < 0)
      {
        return -1;
      }
    }

    // What do we have here?
    if (dn1Size > dn2Size)
    {
      return 1;
    }
    else if (dn1Size < dn2Size)
    {
      return -1;
    }

    return 0;
  }



  // Decodes a DN using the provided reader and schema.
  private static DN decode(final String dnString, final SubstringReader reader,
      final Schema schema, final Map<String, DN> cache)
  {
    reader.skipWhitespaces();
    if (reader.remaining() == 0)
    {
      return ROOT_DN;
    }

    RDN rdn;
    try
    {
      rdn = RDN.decode(null, reader, schema);
    }
    catch (final UnknownSchemaElementException e)
    {
      final LocalizableMessage message = ERR_DN_TYPE_NOT_FOUND.get(reader
          .getString(), e.getMessageObject());
      throw new LocalizedIllegalArgumentException(message);
    }

    DN parent;
    if (reader.remaining() > 0 && reader.read() == ',')
    {
      reader.mark();
      final String parentString = reader.read(reader.remaining());

      parent = cache.get(parentString);
      if (parent == null)
      {
        reader.reset();
        parent = decode(parentString, reader, schema, cache);

        // Only cache parent DNs since leaf DNs are likely to make the
        // cache to volatile.
        cache.put(parentString, parent);
      }
    }
    else
    {
      parent = ROOT_DN;
    }

    return new DN(parent, rdn, dnString);
  }



  @SuppressWarnings("serial")
  private static Map<String, DN> getCache(final Schema schema)
  {
    final WeakHashMap<Schema, Map<String, DN>> threadLocalMap = CACHE.get();
    Map<String, DN> schemaLocalMap = threadLocalMap.get(schema);

    if (schemaLocalMap == null)
    {
      schemaLocalMap = new LinkedHashMap<String, DN>(DN_CACHE_SIZE, 0.75f, true)
      {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, DN> e)
        {
          return size() > DN_CACHE_SIZE;
        }
      };
      threadLocalMap.put(schema, schemaLocalMap);
    }
    return schemaLocalMap;
  }



  private final RDN rdn;

  private DN parent;

  private final int size;

  // We need to store the original string value if provided in order to
  // preserve the original whitespace.
  private String stringValue;



  // Private constructor.
  private DN(final DN parent, final RDN rdn, final String stringValue)
  {
    this(parent, rdn, stringValue, parent != null ? parent.size + 1 : 0);
  }



  // Private constructor.
  private DN(final DN parent, final RDN rdn, final String stringValue,
      final int size)
  {
    this.parent = parent;
    this.rdn = rdn;
    this.stringValue = stringValue;
    this.size = size;
  }



  /**
   * Returns a DN which is subordinate to this DN and having the additional RDN
   * components contained in the provided DN.
   *
   * @param dn
   *          The DN containing the RDN components to be added to this DN.
   * @return The subordinate DN.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public DN child(final DN dn)
  {
    Validator.ensureNotNull(dn);

    if (dn.isRootDN())
    {
      return this;
    }
    else if (isRootDN())
    {
      return dn;
    }
    else
    {
      final RDN[] rdns = new RDN[dn.size()];
      int i = rdns.length;
      for (DN next = dn; next.rdn != null; next = next.parent)
      {
        rdns[--i] = next.rdn;
      }
      DN newDN = this;
      for (i = 0; i < rdns.length; i++)
      {
        newDN = new DN(newDN, rdns[i], null);
      }
      return newDN;
    }
  }



  /**
   * Returns a DN which is an immediate child of this DN and having the
   * specified RDN.
   * <p>
   * <b>Note:</b> the child DN whose RDN is {@link RDN#maxValue()} compares
   * greater than all other possible child DNs, and may be used to construct
   * range queries against DN keyed sorted collections such as {@code SortedSet}
   * and {@code SortedMap}.
   *
   * @param rdn
   *          The RDN for the child DN.
   * @return The child DN.
   * @throws NullPointerException
   *           If {@code rdn} was {@code null}.
   * @see RDN#maxValue()
   */
  public DN child(final RDN rdn)
  {
    Validator.ensureNotNull(rdn);
    return new DN(this, rdn, null);
  }



  /**
   * Returns a DN which is subordinate to this DN and having the additional RDN
   * components contained in the provided DN decoded using the default schema.
   *
   * @param dn
   *          The DN containing the RDN components to be added to this DN.
   * @return The subordinate DN.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public DN child(final String dn)
  {
    Validator.ensureNotNull(dn);
    return child(valueOf(dn));
  }



  /**
   * {@inheritDoc}
   */
  public int compareTo(final DN dn)
  {
    return compareTo(this, dn);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    else if (obj instanceof DN)
    {
      DN other = (DN)obj;
      if(size == other.size())
      {
        if(size == 0)
        {
          return true;
        }

        if(rdn.equals(other.rdn))
        {
          return parent.equals(other.parent);
        }
      }
    }

    return false;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    if (size == 0)
    {
      return 0;
    }
    else
    {
      return 31 * parent.hashCode() + rdn.hashCode();
    }
  }



  /**
   * Returns {@code true} if this DN is an immediate child of the provided DN.
   *
   * @param dn
   *          The potential parent DN.
   * @return {@code true} if this DN is the immediate child of the provided DN,
   *         otherwise {@code false}.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isChildOf(final DN dn)
  {
    // If this is the Root DN then parent will be null but this is ok.
    return dn.equals(parent);
  }



  /**
   * Returns {@code true} if this DN is an immediate child of the provided DN
   * decoded using the default schema.
   *
   * @param dn
   *          The potential parent DN.
   * @return {@code true} if this DN is the immediate child of the provided DN,
   *         otherwise {@code false}.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isChildOf(final String dn)
  {
    // If this is the Root DN then parent will be null but this is ok.
    return isChildOf(valueOf(dn));
  }



  /**
   * Returns {@code true} if this DN matches the provided base DN and search
   * scope.
   *
   * @param dn
   *          The base DN.
   * @param scope
   *          The search scope.
   * @return {@code true} if this DN matches the provided base DN and search
   *         scope, otherwise {@code false}.
   * @throws NullPointerException
   *           If {@code dn} or {@code scope} was {@code null}.
   */
  public boolean isInScopeOf(DN dn, SearchScope scope)
  {
    if (scope == SearchScope.BASE_OBJECT)
    {
      // The base DN must equal this DN.
      return equals(dn);
    }
    else if (scope == SearchScope.SINGLE_LEVEL)
    {
      // The parent DN must equal the base DN.
      return isChildOf(dn);
    }
    else if (scope == SearchScope.SUBORDINATES)
    {
      // This DN must be a descendant of the provided base DN, but
      // not equal to it.
      return isSubordinateOrEqualTo(dn) && !equals(dn);
    }
    else if (scope == SearchScope.WHOLE_SUBTREE)
    {
      // This DN must be a descendant of the provided base DN.
      return isSubordinateOrEqualTo(dn);
    }
    else
    {
      // This is a scope that we don't recognize.
      return false;
    }
  }



  /**
   * Returns {@code true} if this DN matches the provided base DN and search
   * scope.
   *
   * @param dn
   *          The base DN.
   * @param scope
   *          The search scope.
   * @return {@code true} if this DN matches the provided base DN and search
   *         scope, otherwise {@code false}.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} or {@code scope} was {@code null}.
   */
  public boolean isInScopeOf(String dn, SearchScope scope)
  {
    return isInScopeOf(valueOf(dn), scope);
  }



  /**
   * Returns {@code true} if this DN is the immediate parent of the provided DN.
   *
   * @param dn
   *          The potential child DN.
   * @return {@code true} if this DN is the immediate parent of the provided DN,
   *         otherwise {@code false}.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isParentOf(final DN dn)
  {
    // If dn is the Root DN then parent will be null but this is ok.
    return equals(dn.parent);
  }



  /**
   * Returns {@code true} if this DN is the immediate parent of the provided DN.
   *
   * @param dn
   *          The potential child DN.
   * @return {@code true} if this DN is the immediate parent of the provided DN,
   *         otherwise {@code false}.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isParentOf(final String dn)
  {
    // If dn is the Root DN then parent will be null but this is ok.
    return isParentOf(valueOf(dn));
  }



  /**
   * Returns {@code true} if this DN is the Root DN.
   *
   * @return {@code true} if this DN is the Root DN, otherwise {@code false}.
   */
  public boolean isRootDN()
  {
    return size == 0;
  }



  /**
   * Returns {@code true} if this DN is subordinate to or equal to the provided
   * DN.
   *
   * @param dn
   *          The potential child DN.
   * @return {@code true} if this DN is subordinate to or equal to the provided
   *         DN, otherwise {@code false}.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isSubordinateOrEqualTo(final DN dn)
  {
    if (size < dn.size)
    {
      return false;
    }
    else if (size == dn.size)
    {
      return equals(dn);
    }
    else
    {
      // dn is a potential superior of this.
      return parent(size - dn.size).equals(dn);
    }
  }



  /**
   * Returns {@code true} if this DN is subordinate to or equal to the provided
   * DN.
   *
   * @param dn
   *          The potential child DN.
   * @return {@code true} if this DN is subordinate to or equal to the provided
   *         DN, otherwise {@code false}.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isSubordinateOrEqualTo(final String dn)
  {
    return isSubordinateOrEqualTo(valueOf(dn));
  }



  /**
   * Returns {@code true} if this DN is superior to or equal to the provided DN.
   *
   * @param dn
   *          The potential child DN.
   * @return {@code true} if this DN is superior to or equal to the provided DN,
   *         otherwise {@code false}.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isSuperiorOrEqualTo(final DN dn)
  {
    if (size > dn.size)
    {
      return false;
    }
    else if (size == dn.size)
    {
      return equals(dn);
    }
    else
    {
      // dn is a potential subordinate of this.
      return dn.parent(dn.size - size).equals(this);
    }
  }



  /**
   * Returns {@code true} if this DN is superior to or equal to the provided DN.
   *
   * @param dn
   *          The potential child DN.
   * @return {@code true} if this DN is superior to or equal to the provided DN,
   *         otherwise {@code false}.
   * @throws LocalizedIllegalArgumentException
   *           If {@code dn} is not a valid LDAP string representation of a DN.
   * @throws NullPointerException
   *           If {@code dn} was {@code null}.
   */
  public boolean isSuperiorOrEqualTo(final String dn)
  {
    return isSuperiorOrEqualTo(valueOf(dn));
  }



  /**
   * Returns an iterator of the RDNs contained in this DN. The RDNs will be
   * returned in the order starting with this DN's RDN, followed by the RDN of
   * the parent DN, and so on.
   * <p>
   * Attempts to remove RDNs using an iterator's {@code remove()} method are not
   * permitted and will result in an {@code UnsupportedOperationException} being
   * thrown.
   *
   * @return An iterator of the RDNs contained in this DN.
   */
  public Iterator<RDN> iterator()
  {
    return new Iterator<RDN>()
    {
      private DN dn = DN.this;



      public boolean hasNext()
      {
        return dn.rdn != null;
      }



      public RDN next()
      {
        if (dn.rdn == null)
        {
          throw new NoSuchElementException();
        }

        final RDN rdn = dn.rdn;
        dn = dn.parent;
        return rdn;
      }



      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }



  /**
   * Returns the DN whose content is the specified number of RDNs from this DN.
   * The following equivalences hold:
   *
   * <pre>
   * dn.localName(0).isRootDN();
   * dn.localName(1).equals(rootDN.child(dn.rdn()));
   * dn.localName(dn.size()).equals(dn);
   * </pre>
   *
   * @param index
   *          The number of RDNs to be included in the local name.
   * @return The DN whose content is the specified number of RDNs from this DN.
   * @throws IllegalArgumentException
   *           If {@code index} is less than zero.
   */
  public DN localName(final int index)
  {
    Validator.ensureTrue(index >= 0, "index less than zero");

    if (index == 0)
    {
      return ROOT_DN;
    }
    else if (index >= size)
    {
      return this;
    }
    else
    {
      final DN localName = new DN(null, rdn, null, index);
      DN nextLocalName = localName;
      DN lastDN = parent;
      for (int i = index - 1; i > 0; i--)
      {
        nextLocalName.parent = new DN(null, lastDN.rdn, null, i);
        nextLocalName = nextLocalName.parent;
        lastDN = lastDN.parent;
      }
      nextLocalName.parent = ROOT_DN;
      return localName;
    }
  }



  /**
   * Returns the DN which is the immediate parent of this DN, or {@code null} if
   * this DN is the Root DN.
   * <p>
   * This method is equivalent to:
   *
   * <pre>
   * parent(1);
   * </pre>
   *
   * @return The DN which is the immediate parent of this DN, or {@code null} if
   *         this DN is the Root DN.
   */
  public DN parent()
  {
    return parent;
  }



  /**
   * Returns the DN which is equal to this DN with the specified number of RDNs
   * removed. Note that if {@code index} is zero then this DN will be returned
   * (identity).
   *
   * @param index
   *          The number of RDNs to be removed.
   * @return The DN which is equal to this DN with the specified number of RDNs
   *         removed, or {@code null} if the parent of the Root DN is reached.
   * @throws IllegalArgumentException
   *           If {@code index} is less than zero.
   */
  public DN parent(final int index)
  {
    // We allow size + 1 so that we can return null as the parent of the
    // Root DN.
    Validator.ensureTrue(index >= 0, "index less than zero");

    DN parentDN = this;
    for (int i = 0; parentDN != null && i < index; i++)
    {
      parentDN = parentDN.parent;
    }
    return parentDN;
  }



  /**
   * Returns the RDN of this DN, or {@code null} if this DN is the Root DN.
   *
   * @return The RDN of this DN, or {@code null} if this DN is the Root DN.
   */
  public RDN rdn()
  {
    return rdn;
  }



  /**
   * Returns a copy of this DN whose parent DN, {@code fromDN}, has been renamed
   * to the new parent DN, {@code toDN}. If this DN is not subordinate or equal
   * to {@code fromDN} then this DN is returned (i.e. the DN is not renamed).
   *
   * @param fromDN
   *          The old parent DN.
   * @param toDN
   *          The new parent DN.
   * @return The renamed DN, or this DN if no renaming was performed.
   * @throws NullPointerException
   *           If {@code fromDN} or {@code toDN} was {@code null}.
   */
  public DN rename(final DN fromDN, final DN toDN)
  {
    Validator.ensureNotNull(fromDN, toDN);

    if (!isSubordinateOrEqualTo(fromDN))
    {
      return this;
    }
    else if (equals(fromDN))
    {
      return toDN;
    }
    else
    {
      return toDN.child(localName(size - fromDN.size));
    }
  }



  /**
   * Returns the number of RDN components in this DN.
   *
   * @return The number of RDN components in this DN.
   */
  public int size()
  {
    return size;
  }



  /**
   * Returns the RFC 4514 string representation of this DN.
   *
   * @return The RFC 4514 string representation of this DN.
   * @see <a href="http://tools.ietf.org/html/rfc4514">RFC 4514 - Lightweight
   *      Directory Access Protocol (LDAP): String Representation of
   *      Distinguished Names </a>
   */
  @Override
  public String toString()
  {
    // We don't care about potential race conditions here.
    if (stringValue == null)
    {
      final StringBuilder builder = new StringBuilder();
      rdn.toString(builder);
      if (!parent.isRootDN())
      {
        builder.append(',');
        builder.append(parent.toString());
      }
      stringValue = builder.toString();
    }
    return stringValue;
  }
}
