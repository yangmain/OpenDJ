package org.opends.schema.matchingrules;

import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.util.StaticUtils.*;

import org.opends.schema.Schema;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.ByteSequence;
import org.opends.server.types.ByteString;
import org.opends.server.types.DebugLogLevel;
import org.opends.server.util.ServerConstants;

/**
 * This class implements the integerFirstComponentMatch matching rule defined in
 * X.520 and referenced in RFC 2252.  This rule is intended for use with
 * attributes whose values contain a set of parentheses enclosing a
 * space-delimited set of names and/or name-value pairs (like attribute type or
 * objectclass descriptions) in which the "first component" is the first item
 * after the opening parenthesis.
 */
public class IntegerFirstComponentEqualityMatchingRule
    extends AbstractEqualityMatchingRuleImplementation
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();

  public ByteSequence normalizeAttributeValue(Schema schema, ByteSequence value) {
    StringBuilder buffer = new StringBuilder();
    toLowerCase(value, buffer, true);

    int bufferLength = buffer.length();
    if (bufferLength == 0)
    {
      if (value.length() > 0)
      {
        // This should only happen if the value is composed entirely of spaces.
        // In that case, the normalized value is a single space.
        return ServerConstants.SINGLE_SPACE_VALUE;
      }
      else
      {
        // The value is empty, so it is already normalized.
        return ByteString.empty();
      }
    }


    // Replace any consecutive spaces with a single space.
    for (int pos = bufferLength-1; pos > 0; pos--)
    {
      if (buffer.charAt(pos) == ' ')
      {
        if (buffer.charAt(pos-1) == ' ')
        {
          buffer.delete(pos, pos+1);
        }
      }
    }

    String valueString = buffer.toString();
    int valueLength = valueString.length();

    if ((valueLength == 0) || (valueString.charAt(0) != '('))
    {
      // They cannot be equal if the attribute value is empty or doesn't start
      // with an open parenthesis.
      return value;
    }

    int  pos = 1;
    while ((pos < valueLength) && ((valueString.charAt(pos)) == ' '))
    {
      pos++;
    }

    if (pos >= valueLength)
    {
      return value;
    }


    // The current position must be the start position for the value.  Keep
    // reading until we find the next space.
    int startPos = pos++;
    while ((pos < valueLength) && ((valueString.charAt(pos)) != ' '))
    {
      pos++;
    }

    if (pos >= valueLength)
    {
      return value;
    }


    // We should now have the position of the integer value.  Make sure it's an
    // integer and return it.
    try
    {
      return ByteString.valueOf(
          Integer.parseInt(valueString.substring(startPos, pos)));
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      return value;
    }
  }

  @Override
  public ByteSequence normalizeAssertionValue(Schema schema, ByteSequence value) {
    try
    {
      return ByteString.valueOf(
          Integer.parseInt(value.toString()));
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      return value;
    }
  }
}
