package springVibe.dev.common.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps Java Boolean to legacy DB values stored as "True"/"False" strings.
 * This avoids Hibernate attempting to migrate the column to BIT/TINYINT when
 * existing rows contain "False" (string) values.
 */
@Converter
public class BooleanTrueFalseConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) return null;
        return attribute ? "True" : "False";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String v = dbData.trim();
        if (v.isEmpty()) return null;

        // Be tolerant to common variants in case of mixed historical data.
        if ("1".equals(v) || "true".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v)) {
            return Boolean.TRUE;
        }
        if ("0".equals(v) || "false".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v) || "no".equalsIgnoreCase(v)) {
            return Boolean.FALSE;
        }

        // Unknown data: return null so callers can spot it (instead of silently lying).
        return null;
    }
}

