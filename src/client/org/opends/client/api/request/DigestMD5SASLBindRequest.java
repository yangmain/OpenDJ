package org.opends.client.api.request;

import org.opends.server.types.ByteString;
import static org.opends.server.util.ServerConstants.SASL_MECHANISM_DIGEST_MD5;
import org.opends.server.util.Validator;
import org.opends.client.api.NameCallbackHandler;
import org.opends.client.api.PasswordCallbackHandler;
import org.opends.client.api.TextInputCallbackHandler;
import org.opends.common.api.DN;

import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.RealmCallback;

/**
 * Created by IntelliJ IDEA.
 * User: boli
 * Date: Jul 1, 2009
 * Time: 3:42:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class DigestMD5SASLBindRequest extends AbstractSASLBindRequest
{
  private String authenticationID;
  private ByteString password;
  private String realm;

  private NameCallbackHandler authIDHandler;
  private PasswordCallbackHandler passHandler;
  private TextInputCallbackHandler realmHandler;

  public DigestMD5SASLBindRequest(String authenticationID,
                                  ByteString password)
  {
    super(SASL_MECHANISM_DIGEST_MD5);
    Validator.ensureNotNull(authenticationID, password);
    this.authenticationID = authenticationID;
    this.password = password;
  }

  public DigestMD5SASLBindRequest(DN authenticationDN,
                              ByteString password)
  {
    super(SASL_MECHANISM_DIGEST_MD5);
    Validator.ensureNotNull(authenticationDN, password);
    this.authenticationID = "dn:" + authenticationDN.toString();
    this.password = password;
  }


  public DigestMD5SASLBindRequest(String authenticationID,
                              String authorizationID,
                              ByteString password)
  {
    super(SASL_MECHANISM_DIGEST_MD5, authorizationID);
    Validator.ensureNotNull(authenticationID, password);
    this.authenticationID = authenticationID;
    this.password = password;
  }

  public DigestMD5SASLBindRequest(DN authenticationDN,
                              DN authorizationDN,
                              ByteString password)
  {
    super(SASL_MECHANISM_DIGEST_MD5, authorizationDN);
    Validator.ensureNotNull(authenticationDN, password);
    this.authenticationID = "dn:" + authenticationDN.toString();
    this.password = password;
  }

  public DigestMD5SASLBindRequest(String authenticationID,
                              String authorizationID,
                              ByteString password,
                              String realm)
  {
    super(SASL_MECHANISM_DIGEST_MD5, authorizationID);
    Validator.ensureNotNull(authenticationID, password, realm);
    this.authenticationID = authenticationID;
    this.password = password;
    this.realm = realm;
  }

  public DigestMD5SASLBindRequest(DN authenticationDN,
                              DN authorizationDN,
                              ByteString password,
                              String realm)
  {
    super(SASL_MECHANISM_DIGEST_MD5, authorizationDN);
    Validator.ensureNotNull(authenticationDN, password, realm);
    this.authenticationID = "dn:" + authenticationDN.toString();
    this.password = password;
    this.realm = realm;
  }

  public String getAuthorizationID() {
    return authorizationID;
  }

  public String getAuthenticationID() {
    return authenticationID;
  }

  public ByteString getPassword() {
    return password;
  }

  public NameCallbackHandler getAuthIDHandler() {
    return authIDHandler;
  }

  public PasswordCallbackHandler getPassHandler() {
    return passHandler;
  }

  public DigestMD5SASLBindRequest setAuthenticationID(
      String authenticationID)
  {
    Validator.ensureNotNull(authenticationID);
    this.authenticationID = authenticationID;
    return this;
  }

  public DigestMD5SASLBindRequest setPassword(ByteString password)
  {
    Validator.ensureNotNull(password);
    this.password = password;
    return this;
  }

  public String getRealm() {
    return realm;
  }

  public DigestMD5SASLBindRequest setRealm(String realm) {
    this.realm = realm;
    return this;
  }

  public DigestMD5SASLBindRequest setAuthIDHandler(
      NameCallbackHandler authIDHandler)
  {
    this.authIDHandler = authIDHandler;
    return this;
  }

  public DigestMD5SASLBindRequest setPassHandler(
      PasswordCallbackHandler passHandler)
  {
    this.passHandler = passHandler;
    return this;
  }

  public TextInputCallbackHandler getRealmHandler() {
    return realmHandler;
  }

  public void setRealmHandler(TextInputCallbackHandler realmHandler) {
    this.realmHandler = realmHandler;
  }

  @Override
  protected void handle(NameCallback callback)
      throws UnsupportedCallbackException
  {
    if(authIDHandler == null)
    {
      callback.setName(authenticationID);
    }
    else
    {
      authIDHandler.handle(callback);
    }
  }

  @Override
  protected void handle(PasswordCallback callback)
      throws UnsupportedCallbackException
  {
    if(passHandler == null)
    {
      callback.setPassword(password.toString().toCharArray());
    }
    else
    {
      passHandler.handle(callback);
    }
  }

  @Override
  protected void handle(RealmCallback callback) 
      throws UnsupportedCallbackException {
    if(realmHandler == null)
    {
      callback.setText(realm);
    }
    else
    {
      realmHandler.handle(callback);
    }
  }

  public void toString(StringBuilder buffer) {
    buffer.append("DigestMD5SASLBindRequest(bindDN=");
    buffer.append(getBindDN());
    buffer.append(", authentication=SASL");
    buffer.append(", saslMechanism=");
    buffer.append(saslMechanism);
    buffer.append(", authenticationID=");
    buffer.append(authenticationID);
    buffer.append(", authorizationID=");
    buffer.append(authorizationID);
    buffer.append(", realm=");
    buffer.append(realm);
    buffer.append(", password=");
    buffer.append(password);
    buffer.append(", controls=");
    buffer.append(getControls());
    buffer.append(")");
  }
}
