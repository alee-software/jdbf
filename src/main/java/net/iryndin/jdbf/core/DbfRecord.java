package net.iryndin.jdbf.core;

import net.iryndin.jdbf.reader.MemoReader;
import net.iryndin.jdbf.util.BitUtils;
import net.iryndin.jdbf.util.JdbfUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DbfRecord {

    public static final String NUMERIC_OVERFLOW = "*";

    private byte[] bytes;
    private DbfMetadata metadata;
    private MemoReader memoReader;
    private Charset stringCharset;
    private final int recordNumber;

    public DbfRecord(byte[] source, DbfMetadata metadata, MemoReader memoReader, int recordNumber) {
        this.recordNumber = recordNumber;
        this.bytes = new byte[source.length];
        System.arraycopy(source, 0, this.bytes, 0, source.length);
        this.metadata = metadata;
        this.memoReader = memoReader;
    }

    /*
    public DbfRecord(DbfMetadata metadata) {
        this.metadata = metadata;
        fillBytesFromMetadata();
    }

    private void fillBytesFromMetadata() {
        bytes = new byte[metadata.getOneRecordLength()];
        BitUtils.memset(bytes, JdbfUtils.EMPTY);
    }
    */

    /**
     * Check if record is deleted.
     * According to documentation at
     * http://www.dbase.com/Knowledgebase/INT/db7_file_fmt.htm :
     * Data records are preceded by one byte, that is, a space (0x20) if the record is not deleted, an asterisk (0x2A) if the record is deleted.
     * So, if record is preceded by 0x2A - it is considered to be deleted
     * All other cases: record is considered to be not deleted
     *
     * @return
     */
    public boolean isDeleted() {
        return this.bytes[0] == 0x2A;
    }

    public Charset getStringCharset() {
        return stringCharset;
    }

    public void setStringCharset(Charset stringCharset) {
        this.stringCharset = stringCharset;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public String getString(String fieldName) {
        Charset charset = this.stringCharset;
        if (charset == null) {
            charset = metadata.getCharset();
        }
        return getString(fieldName, charset);
    }

    public String getString(String fieldName, String charsetName) {
        return getString(fieldName, Charset.forName(charsetName));
    }

    public String getString(String fieldName, Charset charset) {
        DbfField f = getField(fieldName);
        int actualOffset = f.getOffset();
        int actualLength = f.getLength();

        byte[] fieldBytes = new byte[actualLength];
        System.arraycopy(bytes, actualOffset, fieldBytes, 0, actualLength);

        // check for empty strings
        while ((actualLength > 0) && (bytes[actualOffset] == JdbfUtils.EMPTY)) {
            actualOffset++;
            actualLength--;
        }

        while ((actualLength > 0) && (bytes[actualOffset + actualLength - 1] == JdbfUtils.EMPTY)) {
            actualLength--;
        }

        if (actualLength == 0) {
            return null;
        }

        return new String(bytes, actualOffset, actualLength, charset);

		/*
        byte[] b = new byte[actualLength];
		System.arraycopy(bytes, actualOffset, b, 0, actualLength);
		// check for empty strings
		{
			for (int i = b.length-1; i>=0; i--) {
				if (b[i] == JdbfUtils.EMPTY) {
					actualLength--;
				} else {
					break;
				}
			}
			if (actualLength == 0) {
				return null;
			}
		}
		return new String(b, 0, actualLength, charset);
		*/
    }

    public byte[] getMemoAsBytes(String fieldName) throws IOException {
        DbfField f = getField(fieldName);
        if (f.getType() != DbfFieldTypeEnum.Memo) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not MEMO field!");
        }
        int offsetInBlocks = 0;
        if (f.getLength() == 10) {
            offsetInBlocks = getBigDecimal(fieldName).intValueExact();
        } else {
            byte[] dbfFieldBytes = new byte[f.getLength()];
            System.arraycopy(bytes, f.getOffset(), dbfFieldBytes, 0, f.getLength());
            offsetInBlocks = BitUtils.makeInt(dbfFieldBytes[0], dbfFieldBytes[1], dbfFieldBytes[2], dbfFieldBytes[3]);
        }
        if (offsetInBlocks == 0) return new byte[0];
        return memoReader.read(offsetInBlocks).getValue();
    }

    public String getMemoAsString(String fieldName, Charset charset) throws IOException {
        DbfField f = getField(fieldName);
        if (f.getType() != DbfFieldTypeEnum.Memo) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not MEMO field!");
        }
        int offsetInBlocks = 0;
        if (f.getLength() == 10) {
            offsetInBlocks = getBigDecimal(fieldName).intValueExact();
        } else {
            byte[] dbfFieldBytes = new byte[f.getLength()];
            System.arraycopy(bytes, f.getOffset(), dbfFieldBytes, 0, f.getLength());
            offsetInBlocks = BitUtils.makeInt(dbfFieldBytes[0], dbfFieldBytes[1], dbfFieldBytes[2], dbfFieldBytes[3]);
        }
        if (offsetInBlocks == 0) return "";
        return memoReader.read(offsetInBlocks).getValueAsString(charset);
    }

    public String getMemoAsString(String fieldName) throws IOException {
        Charset charset = getStringCharset();
        if (charset == null) {
            charset = metadata.getCharset();
        }
        return getMemoAsString(fieldName, charset);
    }

    public LocalDate getDate(String fieldName) {
        String s = getString(fieldName);
        if (s == null) {
            return null;
        }
        return JdbfUtils.parseDate(s);
    }

    public ZonedDateTime getDateTime(String fieldName) {
        // Дата и время.
        // Существует в двух вариантах: текстовом и бинарном.
        // Текстовый вариант - строка из 14 цифр в формате ГГГГММДДЧЧММСС; пустое значение - 14 пробелов.
        // Бинарный вариант - два двойных слова little-endian, т.е. всего 8 байт;
        //     первое двойное слово содержит число дней от начала Юлианского календаря (01.01.4713 до нашей эры),
        //     второе двойное слово - число миллисекунд от начала суток;
        //     пустое значение - 8 нулевых байтов

        final DbfField field = getField(fieldName);
        if (field.getType() == DbfFieldTypeEnum.DateTime) {
            if (field.getLength() != 8) {
                String s = getString(fieldName);
                if (s == null) {
                    return null;
                }
                return JdbfUtils.parseDateTime(s, ZoneId.systemDefault());
            }
        }

        final byte[] bytes = getBytes(fieldName);
        final int dateInDays = BitUtils.makeInt(bytes[0], bytes[1], bytes[2], bytes[3]);
        final int time = BitUtils.makeInt(bytes[4], bytes[5], bytes[6], bytes[7]);
        return JdbfUtils.parseJulianDateTime(dateInDays, time, ZoneId.systemDefault());
    }

    public BigDecimal getBigDecimal(String fieldName) {
        DbfField f = getField(fieldName);
        String s = getString(fieldName);

        if (s == null || s.trim().length() == 0) {
            return null;
        } else {
            s = s.trim();
        }

        if (s.contains(NUMERIC_OVERFLOW)) {
            return null;
        }

        //MathContext mc = new MathContext(f.getNumberOfDecimalPlaces());
        //return new BigDecimal(s, mc);
        return new BigDecimal(s);
    }

    public Boolean getBoolean(String fieldName) {
        String s = getString(fieldName);
        if (s == null) {
            return null;
        }
        if (s.equalsIgnoreCase("t")) {
            return Boolean.TRUE;
        } else if (s.equalsIgnoreCase("f")) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    public void setBoolean(String fieldName, Boolean value) {
        DbfField f = getField(fieldName);
        // TODO: write boolean
    }

    public byte[] getBytes(String fieldName) {
        DbfField f = getField(fieldName);
        byte[] b = new byte[f.getLength()];
        System.arraycopy(bytes, f.getOffset(), b, 0, f.getLength());
        return b;
    }

    public void setBytes(String fieldName, byte[] fieldBytes) {
        DbfField f = getField(fieldName);
        // TODO:
        // assert fieldBytes.length = f.getLength()
        System.arraycopy(fieldBytes, 0, bytes, f.getOffset(), f.getLength());
    }

    public Integer getInteger(String fieldName) {
        byte[] bytes = getBytes(fieldName);

        return BitUtils.makeInt(bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    public DbfField getField(String fieldName) {
        return metadata.getField(fieldName);
    }

    public Collection<DbfField> getFields() {
        return metadata.getFields();
    }

    public String getStringRepresentation() throws Exception {
        StringBuilder sb = new StringBuilder(bytes.length * 10);
        for (DbfField f : getFields()) {
            sb.append(f.getName()).append("=");
            switch (f.getType()) {
                case Character: {
                    String s = getString(f.getName());
                    sb.append(s);
                    break;
                }
                case Date: {
                    LocalDate d = getDate(f.getName());
                    sb.append(d);
                    break;
                }
                case Numeric: {
                    BigDecimal bd = getBigDecimal(f.getName());
                    sb.append(bd);
                    break;
                }
                case Logical: {
                    Boolean b = getBoolean(f.getName());
                    sb.append(b);
                    break;
                }
                /* @deprecated */
                case DateTime:
                case Timestamp: {
                    final ZonedDateTime dt = getDateTime(f.getName());
                    sb.append(dt);
                    break;
                }
                case Memo: {
                    final String ms = getMemoAsString(f.getName());
                    sb.append(ms);
                    break;
                }
            }
            sb.append(", ");
        }
        return sb.toString();
    }

    public Map<String, Object> toMap() throws IOException {
        Map<String, Object> map = new LinkedHashMap<>(getFields().size() * 2);

        for (DbfField f : getFields()) {
            String name = f.getName();

            switch (f.getType()) {

                case Character:
                    map.put(name, getString(name));
                    break;

                case Date:
                    map.put(name, getDate(name));
                    break;

                case Numeric:
                    map.put(name, getBigDecimal(name));
                    break;

                case Logical:
                    map.put(name, getBoolean(name));
                    break;

                case Integer:
                    map.put(name, getInteger(name));
                    break;

                /* @deprecated */
                case DateTime:
                case Timestamp:
                    map.put(name, getDateTime(name));
                    break;

                case Memo:
                    map.put(name, getMemoAsString(name));
                    break;
            }
        }

        return map;
    }
}
