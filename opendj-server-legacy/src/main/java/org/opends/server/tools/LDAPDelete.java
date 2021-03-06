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
 *      Copyright 2006-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2012-2015 ForgeRock AS.
 */
package org.opends.server.tools;

import static com.forgerock.opendj.cli.ArgumentConstants.*;
import static com.forgerock.opendj.cli.Utils.*;

import static org.opends.messages.ToolMessages.*;
import static org.opends.server.protocols.ldap.LDAPResultCode.*;
import static org.opends.server.util.args.LDAPConnectionArgumentParser.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DecodeException;
import org.opends.server.controls.SubtreeDeleteControl;
import org.opends.server.core.DirectoryServer.DirectoryServerVersionHandler;
import org.opends.server.protocols.ldap.DeleteRequestProtocolOp;
import org.opends.server.protocols.ldap.DeleteResponseProtocolOp;
import org.opends.server.protocols.ldap.LDAPMessage;
import org.opends.server.protocols.ldap.ProtocolOp;
import org.opends.server.types.Control;
import org.opends.server.types.LDAPException;
import org.opends.server.types.NullOutputStream;
import org.opends.server.util.EmbeddedUtils;

import com.forgerock.opendj.cli.ArgumentException;
import com.forgerock.opendj.cli.ArgumentParser;
import com.forgerock.opendj.cli.BooleanArgument;
import com.forgerock.opendj.cli.CliConstants;
import com.forgerock.opendj.cli.CommonArguments;
import com.forgerock.opendj.cli.FileBasedArgument;
import com.forgerock.opendj.cli.IntegerArgument;
import com.forgerock.opendj.cli.StringArgument;

/**
 * This class provides a tool that can be used to issue delete requests to the
 * Directory Server.
 */
public class LDAPDelete
{
  private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

  /** The fully-qualified name of this class. */
  private static final String CLASS_NAME = "org.opends.server.tools.LDAPDelete";


  /** The message ID counter to use for requests. */
  private final AtomicInteger nextMessageID;

  /** The print stream to use for standard error. */
  private final PrintStream err;
  /** The print stream to use for standard output. */
  private final PrintStream out;



  /**
   * Constructor for the LDAPDelete object.
   *
   * @param  nextMessageID  The next message ID to use for requests.
   * @param  out            The print stream to use for standard output.
   * @param  err            The print stream to use for standard error.
   */
  public LDAPDelete(AtomicInteger nextMessageID, PrintStream out,
                    PrintStream err)
  {
    this.nextMessageID = nextMessageID;
    this.out           = out;
    this.err           = err;
  }

  /**
   * Execute the delete request on the specified list of DNs.
   *
   * @param connection        The connection to use to execute the request.
   * @param lines             The list of DNs to delete.
   * @param deleteOptions     The constraints to use for this request.
   *
   * @throws  IOException  If a problem occurs while attempting to communicate
   *                       with the Directory Server.
   *
   * @throws  LDAPException  If the Directory Server returns an error response.
   */
  public void readAndExecute(LDAPConnection connection,
                             ArrayList<String> lines,
                             LDAPDeleteOptions deleteOptions)
    throws IOException, LDAPException
  {
    for(String line : lines)
    {
      executeDelete(connection, line, deleteOptions);
    }
  }

  /**
   * Read the specified DNs from the given reader
   * (file or stdin) and execute the given delete request.
   *
   * @param connection        The connection to use to execute the request.
   * @param reader            The reader to read the list of DNs from.
   * @param deleteOptions     The constraints to use for this request.
   *
   * @throws  IOException  If a problem occurs while attempting to communicate
   *                       with the Directory Server.
   *
   * @throws  LDAPException  If the Directory Server returns an error response.
   */
  public void readAndExecute(LDAPConnection connection, Reader reader,
                             LDAPDeleteOptions deleteOptions)
         throws IOException, LDAPException
  {
    BufferedReader in = new BufferedReader(reader);
    String line = null;

    while ((line = in.readLine()) != null)
    {
      executeDelete(connection, line, deleteOptions);
    }
    in.close();
  }


  /**
   * Execute the delete request for the specified DN.
   *
   * @param connection        The connection to use to execute the request.
   * @param line           The DN to delete.
   * @param deleteOptions  The list of constraints for this request.
   *
   * @throws  IOException  If a problem occurs while attempting to communicate
   *                       with the Directory Server.
   *
   * @throws  LDAPException  If the Directory Server returns an error response.
   */
  private void executeDelete(LDAPConnection connection, String line,
                             LDAPDeleteOptions deleteOptions)
          throws IOException, LDAPException
  {
    ArrayList<Control> controls = deleteOptions.getControls();
    ProtocolOp protocolOp = null;
    ByteString asn1OctetStr = ByteString.valueOfUtf8(line);

    protocolOp = new DeleteRequestProtocolOp(asn1OctetStr);

    out.println(INFO_PROCESSING_OPERATION.get("DELETE", asn1OctetStr));
    if(!deleteOptions.showOperations())
    {
      LDAPMessage message = new LDAPMessage(nextMessageID.getAndIncrement(),
                                            protocolOp, controls);
      LDAPMessage responseMessage = null;
      try
      {
        connection.getLDAPWriter().writeMessage(message);
        responseMessage = connection.getLDAPReader().readMessage();
      } catch(DecodeException ae)
      {
        logger.traceException(ae);
        if (!deleteOptions.continueOnError())
        {
          String msg = LDAPToolUtils.getMessageForConnectionException(ae);
          throw new IOException(msg, ae);
        }
        else
        {
          printWrappedText(err, INFO_OPERATION_FAILED.get("DELETE"));
          printWrappedText(err, ae.getMessage());
          return;
        }
      }

      DeleteResponseProtocolOp op =
           responseMessage.getDeleteResponseProtocolOp();
      int resultCode = op.getResultCode();
      LocalizableMessage errorMessage = op.getErrorMessage();
      if(resultCode != SUCCESS && resultCode != REFERRAL &&
         !deleteOptions.continueOnError())
      {
        LocalizableMessage msg = INFO_OPERATION_FAILED.get("DELETE");
        throw new LDAPException(resultCode, errorMessage, msg,
                                op.getMatchedDN(), null);
      } else
      {
        if(resultCode != SUCCESS && resultCode != REFERRAL)
        {
          LocalizableMessage msg = INFO_OPERATION_FAILED.get("DELETE");
          LDAPToolUtils.printErrorMessage(err, msg, resultCode, errorMessage,
                                          op.getMatchedDN());
        } else
        {
          LocalizableMessage msg = INFO_OPERATION_SUCCESSFUL.get("DELETE", line);
          out.println(msg);
        }
      }
    }
  }

  /**
   * The main method for LDAPDelete tool.
   *
   * @param  args  The command-line arguments provided to this program.
   */

  public static void main(String[] args)
  {
    int retCode = mainDelete(args, true, System.out, System.err);

    if(retCode != 0)
    {
      System.exit(filterExitCode(retCode));
    }
  }

  /**
   * Parses the provided command-line arguments and uses that information to
   * run the ldapdelete tool.
   *
   * @param  args  The command-line arguments provided to this program.
   *
   * @return The error code.
   */

  public static int mainDelete(String[] args)
  {
    return mainDelete(args, true, System.out, System.err);
  }

  /**
   * Parses the provided command-line arguments and uses that information to
   * run the ldapdelete tool.
   *
   * @param  args              The command-line arguments provided to this
   *                           program.
   * @param  initializeServer  Indicates whether to initialize the server.
   * @param  outStream         The output stream to use for standard output, or
   *                           <CODE>null</CODE> if standard output is not
   *                           needed.
   * @param  errStream         The output stream to use for standard error, or
   *                           <CODE>null</CODE> if standard error is not
   *                           needed.
   *
   * @return The error code.
   */

  public static int mainDelete(String[] args, boolean initializeServer,
                               OutputStream outStream, OutputStream errStream)
  {
    PrintStream out = NullOutputStream.wrapOrNullStream(outStream);
    PrintStream err = NullOutputStream.wrapOrNullStream(errStream);

    LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
    LDAPDeleteOptions deleteOptions = new LDAPDeleteOptions();
    LDAPConnection connection = null;

    BooleanArgument   continueOnError        = null;
    BooleanArgument   deleteSubtree          = null;
    BooleanArgument   noop                   = null;
    BooleanArgument   saslExternal           = null;
    BooleanArgument   showUsage              = null;
    BooleanArgument   startTLS               = null;
    BooleanArgument   trustAll               = null;
    BooleanArgument   useSSL                 = null;
    BooleanArgument   verbose                = null;
    FileBasedArgument bindPasswordFile       = null;
    FileBasedArgument keyStorePasswordFile   = null;
    FileBasedArgument trustStorePasswordFile = null;
    IntegerArgument   port                   = null;
    IntegerArgument   version                = null;
    StringArgument    bindDN                 = null;
    StringArgument    bindPassword           = null;
    StringArgument    certNickname           = null;
    StringArgument    controlStr             = null;
    StringArgument    encodingStr            = null;
    StringArgument    filename               = null;
    StringArgument    hostName               = null;
    StringArgument    keyStorePath           = null;
    StringArgument    keyStorePassword       = null;
    StringArgument    saslOptions            = null;
    StringArgument    trustStorePath         = null;
    StringArgument    trustStorePassword     = null;
    IntegerArgument   connectTimeout         = null;
    StringArgument    propertiesFileArgument = null;
    BooleanArgument   noPropertiesFileArgument = null;

    Reader rdr = null;
    ArrayList<String> dnStrings = new ArrayList<>();

    // Create the command-line argument parser for use with this program.
    LocalizableMessage toolDescription = INFO_LDAPDELETE_TOOL_DESCRIPTION.get();
    ArgumentParser argParser = new ArgumentParser(CLASS_NAME, toolDescription,
                                                  false, true, 0, 1, "\"DN\"");
    argParser.setShortToolDescription(REF_SHORT_DESC_LDAPDELETE.get());
    argParser.setVersionHandler(new DirectoryServerVersionHandler());
    try
    {
      propertiesFileArgument = new StringArgument("propertiesFilePath",
          null, OPTION_LONG_PROP_FILE_PATH,
          false, false, true, INFO_PROP_FILE_PATH_PLACEHOLDER.get(), null, null,
          INFO_DESCRIPTION_PROP_FILE_PATH.get());
      argParser.addArgument(propertiesFileArgument);
      argParser.setFilePropertiesArgument(propertiesFileArgument);

      noPropertiesFileArgument = new BooleanArgument(
          "noPropertiesFileArgument", null, OPTION_LONG_NO_PROP_FILE,
          INFO_DESCRIPTION_NO_PROP_FILE.get());
      argParser.addArgument(noPropertiesFileArgument);
      argParser.setNoPropertiesFileArgument(noPropertiesFileArgument);

      hostName = new StringArgument("host", OPTION_SHORT_HOST,
                                    OPTION_LONG_HOST, false, false, true,
                                    INFO_HOST_PLACEHOLDER.get(), "localhost",
                                    null,
                                    INFO_DESCRIPTION_HOST.get());
      hostName.setPropertyName(OPTION_LONG_HOST);
      argParser.addArgument(hostName);

      port = new IntegerArgument("port", OPTION_SHORT_PORT,
                                 OPTION_LONG_PORT, false, false, true,
                                 INFO_PORT_PLACEHOLDER.get(), 389, null,
                                 true, 1, true, 65535,
                                 INFO_DESCRIPTION_PORT.get());
      port.setPropertyName(OPTION_LONG_PORT);
      argParser.addArgument(port);

      useSSL = new BooleanArgument("useSSL", OPTION_SHORT_USE_SSL,
                                   OPTION_LONG_USE_SSL,
                                   INFO_DESCRIPTION_USE_SSL.get());
      useSSL.setPropertyName(OPTION_LONG_USE_SSL);
      argParser.addArgument(useSSL);

      startTLS = new BooleanArgument("startTLS", OPTION_SHORT_START_TLS,
                                     OPTION_LONG_START_TLS,
                                     INFO_DESCRIPTION_START_TLS.get());
      startTLS.setPropertyName(OPTION_LONG_START_TLS);
      argParser.addArgument(startTLS);

      bindDN = new StringArgument("bindDN", OPTION_SHORT_BINDDN,
                                  OPTION_LONG_BINDDN, false, false, true,
                                  INFO_BINDDN_PLACEHOLDER.get(), null, null,
                                  INFO_DESCRIPTION_BINDDN.get());
      bindDN.setPropertyName(OPTION_LONG_BINDDN);
      argParser.addArgument(bindDN);

      bindPassword = new StringArgument("bindPassword", OPTION_SHORT_BINDPWD,
                                        OPTION_LONG_BINDPWD,
                                        false, false, true,
                                        INFO_BINDPWD_PLACEHOLDER.get(),
                                        null, null,
                                        INFO_DESCRIPTION_BINDPASSWORD.get());
      bindPassword.setPropertyName(OPTION_LONG_BINDPWD);
      argParser.addArgument(bindPassword);

      bindPasswordFile =
           new FileBasedArgument("bindPasswordFile", OPTION_SHORT_BINDPWD_FILE,
                                 OPTION_LONG_BINDPWD_FILE,
                                 false, false,
                                 INFO_BINDPWD_FILE_PLACEHOLDER.get(), null,
                                 null, INFO_DESCRIPTION_BINDPASSWORDFILE.get());
      bindPasswordFile.setPropertyName(OPTION_LONG_BINDPWD_FILE);
      argParser.addArgument(bindPasswordFile);

      filename = new StringArgument("filename", OPTION_SHORT_FILENAME,
                                    OPTION_LONG_FILENAME, false, false,
                                    true, INFO_FILE_PLACEHOLDER.get(), null,
                                    null,
                                    INFO_DELETE_DESCRIPTION_FILENAME.get());
      filename.setPropertyName(OPTION_LONG_FILENAME);
      argParser.addArgument(filename);

      saslExternal =
              new BooleanArgument("useSASLExternal", 'r',
                                  "useSASLExternal",
                                  INFO_DESCRIPTION_USE_SASL_EXTERNAL.get());
      saslExternal.setPropertyName("useSASLExternal");
      argParser.addArgument(saslExternal);

      saslOptions = new StringArgument(
              "saslOption", OPTION_SHORT_SASLOPTION,
              OPTION_LONG_SASLOPTION,
              false, true, true,
              INFO_SASL_OPTION_PLACEHOLDER.get(), null,
              null, INFO_DESCRIPTION_SASL_PROPERTIES.get());
      saslOptions.setPropertyName(OPTION_LONG_SASLOPTION);
      argParser.addArgument(saslOptions);

      trustAll = CommonArguments.getTrustAll();
      argParser.addArgument(trustAll);

      keyStorePath = new StringArgument("keyStorePath",
                                        OPTION_SHORT_KEYSTOREPATH,
                                        OPTION_LONG_KEYSTOREPATH,
                                        false, false, true,
                                        INFO_KEYSTOREPATH_PLACEHOLDER.get(),
                                        null, null,
                                        INFO_DESCRIPTION_KEYSTOREPATH.get());
      keyStorePath.setPropertyName(OPTION_LONG_KEYSTOREPATH);
      argParser.addArgument(keyStorePath);

      keyStorePassword =
              new StringArgument("keyStorePassword",
                                 OPTION_SHORT_KEYSTORE_PWD,
                                 OPTION_LONG_KEYSTORE_PWD,
                                 false, false,
                                 true, INFO_KEYSTORE_PWD_PLACEHOLDER.get(),
                                 null, null,
                                 INFO_DESCRIPTION_KEYSTOREPASSWORD.get());
      keyStorePassword.setPropertyName(OPTION_LONG_KEYSTORE_PWD);
      argParser.addArgument(keyStorePassword);

      keyStorePasswordFile =
           new FileBasedArgument("keyStorePasswordFile",
                                 OPTION_SHORT_KEYSTORE_PWD_FILE,
                                 OPTION_LONG_KEYSTORE_PWD_FILE,
                                 false, false,
                                 INFO_KEYSTORE_PWD_FILE_PLACEHOLDER.get(),
                                 null, null,
                                 INFO_DESCRIPTION_KEYSTOREPASSWORD_FILE.get());
      keyStorePasswordFile.setPropertyName(OPTION_LONG_KEYSTORE_PWD_FILE);
      argParser.addArgument(keyStorePasswordFile);

      certNickname = new StringArgument(
              "certnickname", 'N', "certNickname",
              false, false, true, INFO_NICKNAME_PLACEHOLDER.get(), null,
              null, INFO_DESCRIPTION_CERT_NICKNAME.get());
      certNickname.setPropertyName("certNickname");
      argParser.addArgument(certNickname);

      trustStorePath = new StringArgument(
              "trustStorePath",
              OPTION_SHORT_TRUSTSTOREPATH,
              OPTION_LONG_TRUSTSTOREPATH,
              false, false, true,
              INFO_TRUSTSTOREPATH_PLACEHOLDER.get(),
              null, null,
              INFO_DESCRIPTION_TRUSTSTOREPATH.get());
      trustStorePath.setPropertyName(OPTION_LONG_TRUSTSTOREPATH);
      argParser.addArgument(trustStorePath);

      trustStorePassword =
           new StringArgument("trustStorePassword", null,
                              OPTION_LONG_TRUSTSTORE_PWD,
                              false, false, true,
                              INFO_TRUSTSTORE_PWD_PLACEHOLDER.get(), null,
                              null, INFO_DESCRIPTION_TRUSTSTOREPASSWORD.get());
      trustStorePassword.setPropertyName(OPTION_LONG_TRUSTSTORE_PWD);
      argParser.addArgument(trustStorePassword);

      trustStorePasswordFile =
           new FileBasedArgument(
                   "trustStorePasswordFile",
                   OPTION_SHORT_TRUSTSTORE_PWD_FILE,
                   OPTION_LONG_TRUSTSTORE_PWD_FILE, false, false,
                   INFO_TRUSTSTORE_PWD_FILE_PLACEHOLDER.get(), null, null,
                   INFO_DESCRIPTION_TRUSTSTOREPASSWORD_FILE.get());
      trustStorePasswordFile.setPropertyName(OPTION_LONG_TRUSTSTORE_PWD_FILE);
      argParser.addArgument(trustStorePasswordFile);

      deleteSubtree =
           new BooleanArgument("deleteSubtree", 'x', "deleteSubtree",
                               INFO_DELETE_DESCRIPTION_DELETE_SUBTREE.get());
      deleteSubtree.setPropertyName("deleteSubtree");
      argParser.addArgument(deleteSubtree);

      controlStr =
           new StringArgument("control", 'J', "control", false, true, true,
                    INFO_LDAP_CONTROL_PLACEHOLDER.get(),
                    null, null, INFO_DESCRIPTION_CONTROLS.get());
      controlStr.setPropertyName("control");
      argParser.addArgument(controlStr);

      version = new IntegerArgument("version", OPTION_SHORT_PROTOCOL_VERSION,
                                    OPTION_LONG_PROTOCOL_VERSION, false, false,
                                    true,
                                    INFO_PROTOCOL_VERSION_PLACEHOLDER.get(), 3,
                                    null, INFO_DESCRIPTION_VERSION.get());
      version.setPropertyName(OPTION_LONG_PROTOCOL_VERSION);
      argParser.addArgument(version);

      int defaultTimeout = CliConstants.DEFAULT_LDAP_CONNECT_TIMEOUT;
      connectTimeout = new IntegerArgument(OPTION_LONG_CONNECT_TIMEOUT,
          null, OPTION_LONG_CONNECT_TIMEOUT,
          false, false, true, INFO_TIMEOUT_PLACEHOLDER.get(),
          defaultTimeout, null,
          true, 0, false, Integer.MAX_VALUE,
          INFO_DESCRIPTION_CONNECTION_TIMEOUT.get());
      connectTimeout.setPropertyName(OPTION_LONG_CONNECT_TIMEOUT);
      argParser.addArgument(connectTimeout);

      encodingStr = new StringArgument("encoding", 'i',
                                       OPTION_LONG_ENCODING, false,
                                       false, true,
                                       INFO_ENCODING_PLACEHOLDER.get(), null,
                                       null,
                                       INFO_DESCRIPTION_ENCODING.get());
      encodingStr.setPropertyName(OPTION_LONG_ENCODING);
      argParser.addArgument(encodingStr);

      continueOnError =
           new BooleanArgument("continueOnError", 'c', "continueOnError",
                               INFO_DESCRIPTION_CONTINUE_ON_ERROR.get());
      continueOnError.setPropertyName("continueOnError");
      argParser.addArgument(continueOnError);

      noop = new BooleanArgument("no-op", OPTION_SHORT_DRYRUN,
          OPTION_LONG_DRYRUN, INFO_DESCRIPTION_NOOP.get());
      noop.setPropertyName(OPTION_LONG_DRYRUN);
      argParser.addArgument(noop);

      verbose = CommonArguments.getVerbose();
      argParser.addArgument(verbose);

      showUsage = CommonArguments.getShowUsage();
      argParser.addArgument(showUsage);
      argParser.setUsageArgument(showUsage, out);
    } catch (ArgumentException ae)
    {
      printWrappedText(err, ERR_CANNOT_INITIALIZE_ARGS.get(ae.getMessage()));
      return CLIENT_SIDE_PARAM_ERROR;
    }

    // Parse the command-line arguments provided to this program.
    try
    {
      argParser.parseArguments(args);
    }
    catch (ArgumentException ae)
    {
      argParser.displayMessageAndUsageReference(err, ERR_ERROR_PARSING_ARGS.get(ae.getMessage()));
      return CLIENT_SIDE_PARAM_ERROR;
    }

    // If we should just display usage or version information,
    // then it has already been done so just exit.
    if (argParser.usageOrVersionDisplayed())
    {
      return 0;
    }

    if(bindPassword.isPresent() && bindPasswordFile.isPresent())
    {
      printWrappedText(
          err, ERR_TOOL_CONFLICTING_ARGS.get(bindPassword.getLongIdentifier(), bindPasswordFile.getLongIdentifier()));
      return CLIENT_SIDE_PARAM_ERROR;
    }


    String hostNameValue = hostName.getValue();
    int portNumber = 389;
    try
    {
      portNumber = port.getIntValue();
    } catch(ArgumentException ae)
    {
      logger.traceException(ae);
      argParser.displayMessageAndUsageReference(err, ae.getMessageObject());
      return CLIENT_SIDE_PARAM_ERROR;
    }

    try
    {
      int versionNumber = version.getIntValue();
      if(versionNumber != 2 && versionNumber != 3)
      {
        printWrappedText(err, ERR_DESCRIPTION_INVALID_VERSION.get(versionNumber));
        return CLIENT_SIDE_PARAM_ERROR;
      }
      connectionOptions.setVersionNumber(versionNumber);
    } catch(ArgumentException ae)
    {
      logger.traceException(ae);
      argParser.displayMessageAndUsageReference(err, ae.getMessageObject());
      return CLIENT_SIDE_PARAM_ERROR;
    }

    String bindDNValue = bindDN.getValue();
    String fileNameValue = filename.getValue();
    String bindPasswordValue;
    try
    {
      bindPasswordValue = getPasswordValue(
          bindPassword, bindPasswordFile, bindDNValue, out, err);
    }
    catch (Exception ex)
    {
      logger.traceException(ex);
      printWrappedText(err, ex.getMessage());
      return CLIENT_SIDE_PARAM_ERROR;
    }

    String keyStorePathValue = keyStorePath.getValue();
    String trustStorePathValue = trustStorePath.getValue();

    String keyStorePasswordValue = null;
    if (keyStorePassword.isPresent())
    {
      keyStorePasswordValue = keyStorePassword.getValue();
    }
    else if (keyStorePasswordFile.isPresent())
    {
      keyStorePasswordValue = keyStorePasswordFile.getValue();
    }

    String trustStorePasswordValue = null;
    if (trustStorePassword.isPresent())
    {
      trustStorePasswordValue = trustStorePassword.getValue();
    }
    else if (trustStorePasswordFile.isPresent())
    {
      trustStorePasswordValue = trustStorePasswordFile.getValue();
    }

    deleteOptions.setShowOperations(noop.isPresent());
    deleteOptions.setVerbose(verbose.isPresent());
    deleteOptions.setContinueOnError(continueOnError.isPresent());
    deleteOptions.setEncoding(encodingStr.getValue());
    deleteOptions.setDeleteSubtree(deleteSubtree.isPresent());

    if(controlStr.isPresent())
    {
      for (String ctrlString : controlStr.getValues())
      {
        Control ctrl = LDAPToolUtils.getControl(ctrlString, err);
        if(ctrl == null)
        {
          printWrappedText(err, ERR_TOOL_INVALID_CONTROL_STRING.get(ctrlString));
          return CLIENT_SIDE_PARAM_ERROR;
        }
        deleteOptions.getControls().add(ctrl);
      }
    }

    if(deleteOptions.getDeleteSubtree())
    {
      Control control = new SubtreeDeleteControl(false);
      deleteOptions.getControls().add(control);
    }

    ArrayList<String> trailingArgs = argParser.getTrailingArguments();
    dnStrings.addAll(trailingArgs);

    // Set the connection options.
    // Parse the SASL properties.
    connectionOptions.setSASLExternal(saslExternal.isPresent());
    if(saslOptions.isPresent())
    {
      for (String saslOption : saslOptions.getValues())
      {
        boolean val = saslOption.startsWith("mech=")
            ? connectionOptions.setSASLMechanism(saslOption)
            : connectionOptions.addSASLProperty(saslOption);
        if (!val)
        {
          return CLIENT_SIDE_PARAM_ERROR;
        }
      }
    }
    connectionOptions.setUseSSL(useSSL.isPresent());
    connectionOptions.setStartTLS(startTLS.isPresent());

    if(connectionOptions.useSASLExternal())
    {
      if(!connectionOptions.useSSL() && !connectionOptions.useStartTLS())
      {
        printWrappedText(err, ERR_TOOL_SASLEXTERNAL_NEEDS_SSL_OR_TLS.get());
        return CLIENT_SIDE_PARAM_ERROR;
      }
      if(keyStorePathValue == null)
      {
        printWrappedText(err, ERR_TOOL_SASLEXTERNAL_NEEDS_KEYSTORE.get());
        return CLIENT_SIDE_PARAM_ERROR;
      }
    }

    LDAPDelete ldapDelete = null;
    try
    {
      if (initializeServer)
      {
        // Bootstrap and initialize directory data structures.
        EmbeddedUtils.initializeForClientUse();
      }

      // Connect to the specified host with the supplied userDN and password.
      SSLConnectionFactory sslConnectionFactory = null;
      if(connectionOptions.useSSL() || connectionOptions.useStartTLS())
      {
        String clientAlias;
        if (certNickname.isPresent())
        {
          clientAlias = certNickname.getValue();
        }
        else
        {
          clientAlias = null;
        }

        sslConnectionFactory = new SSLConnectionFactory();
        sslConnectionFactory.init(trustAll.isPresent(), keyStorePathValue,
                                  keyStorePasswordValue, clientAlias,
                                  trustStorePathValue, trustStorePasswordValue);
        connectionOptions.setSSLConnectionFactory(sslConnectionFactory);
      }

      AtomicInteger nextMessageID = new AtomicInteger(1);
      connection = new LDAPConnection(hostNameValue, portNumber,
                                      connectionOptions, out, err);
      int timeout = connectTimeout.getIntValue();
      connection.connectToHost(bindDNValue, bindPasswordValue, nextMessageID,
          timeout);

      ldapDelete = new LDAPDelete(nextMessageID, out, err);
      if(fileNameValue == null && dnStrings.isEmpty())
      {
        // Read from stdin.
        rdr = new InputStreamReader(System.in);
      } else if(fileNameValue != null)
      {
        rdr = new FileReader(fileNameValue);
      }

      if(rdr != null)
      {
        ldapDelete.readAndExecute(connection, rdr, deleteOptions);
      } else
      {
        ldapDelete.readAndExecute(connection, dnStrings, deleteOptions);
      }
    } catch(LDAPException le)
    {
      logger.traceException(le);
      LDAPToolUtils.printErrorMessage(err, le.getMessageObject(),
                                      le.getResultCode(),
                                      le.getErrorMessage(),
                                      le.getMatchedDN());
      return le.getResultCode();
    } catch(LDAPConnectionException lce)
    {
      logger.traceException(lce);
      LDAPToolUtils.printErrorMessage(err, lce.getMessageObject(),
                                      lce.getResultCode(),
                                      lce.getErrorMessage(),
                                      lce.getMatchedDN());
      return lce.getResultCode();
    }
    catch(ArgumentException e)
    {
      argParser.displayMessageAndUsageReference(err, e.getMessageObject());
      return 1;
    }
    catch (Exception e)
    {
      logger.traceException(e);
      printWrappedText(err, e.getMessage());
      return 1;
    } finally
    {
      if(connection != null)
      {
        if (ldapDelete == null)
        {
          connection.close(null);
        }
        else
        {
          connection.close(ldapDelete.nextMessageID);
        }
      }
    }
    return 0;
  }

}

