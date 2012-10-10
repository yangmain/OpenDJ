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
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package org.forgerock.opendj.ldap.requests;



import java.util.List;

import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.ControlDecoder;



/**
 * The SASL authentication method of the Bind operation allows clients to
 * authenticate using one of the SASL authentication methods defined in RFC
 * 4513.
 * <p>
 * <TODO>finish doc.
 */
public interface SASLBindRequest extends BindRequest
{
  /**
   * {@inheritDoc}
   */
  SASLBindRequest addControl(Control control);



  /**
   * {@inheritDoc}
   */
  BindClient createBindClient(String serverName) throws ErrorResultException;



  /**
   * Returns the authentication mechanism identifier for this SASL bind request
   * as defined by the LDAP protocol, which is always {@code 0xA3}.
   *
   * @return The authentication mechanism identifier.
   */
  byte getAuthenticationType();



  /**
   * {@inheritDoc}
   */
  <C extends Control> C getControl(ControlDecoder<C> decoder,
      DecodeOptions options) throws DecodeException;



  /**
   * {@inheritDoc}
   */
  List<Control> getControls();



  /**
   * Returns the name of the Directory object that the client wishes to bind as,
   * which is always the empty string for SASL authentication.
   *
   * @return The name of the Directory object that the client wishes to bind as.
   */
  String getName();



  /**
   * Returns the SASL mechanism for this SASL bind request.
   *
   * @return The SASL mechanism for this bind request.
   */
  String getSASLMechanism();

}
