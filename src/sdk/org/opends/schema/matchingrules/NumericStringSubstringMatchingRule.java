package org.opends.schema.matchingrules;

import static org.opends.schema.StringPrepProfile.*;

import org.opends.schema.Schema;
import org.opends.server.types.ByteSequence;
import org.opends.server.types.ByteString;

/**
 * This class implements the numericStringSubstringsMatch matching rule defined
 * in X.520 and referenced in RFC 2252.
 */
public class NumericStringSubstringMatchingRule
    extends AbstractSubstringMatchingRuleImplementation
{
  public ByteSequence normalizeAttributeValue(Schema schema, ByteSequence value)
  {
    StringBuilder buffer = new StringBuilder();
    prepareUnicode(buffer, value, TRIM, NO_CASE_FOLD);

    if(buffer.length() == 0)
    {
      return ByteString.empty();
    }
    return ByteString.valueOf(buffer.toString());
  }
}
