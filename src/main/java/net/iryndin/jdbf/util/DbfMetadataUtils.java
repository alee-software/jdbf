package net.iryndin.jdbf.util;

import net.iryndin.jdbf.core.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static net.iryndin.jdbf.util.JdbfUtils.FILE_HEADER_SIZE;

public class DbfMetadataUtils {

    public static DbfMetadata fromFieldsString(String s) throws IOException {
        List<DbfField> fields = JdbfUtils.createFieldsFromString(s);

        DbfMetadata metadata = new DbfMetadata();

        metadata.setType(DbfFileTypeEnum.FoxBASEPlus1);
        metadata.setUpdateDate(LocalDate.now());
        //metadata.setRecordsQty(recordsQty);
        int fullHeaderLength = calculateFullHeaderLength(fields);
        metadata.setFullHeaderLength(fullHeaderLength);
        int oneRecordLength = calculateOneRecordLength(fields);
        metadata.setOneRecordLength(oneRecordLength);

        metadata.setFields(fields);

        return metadata;
    }

    public static DbfMetadata fromFields(List<DbfField> fields, DbfFileTypeEnum fileType) throws IOException {
        DbfMetadata metadata = new DbfMetadata();

        metadata.setType(fileType);
        metadata.setUpdateDate(LocalDate.now());
        int fullHeaderLength = calculateFullHeaderLength(fields);
        metadata.setFullHeaderLength(fullHeaderLength);
        int oneRecordLength = calculateOneRecordLength(fields);
        metadata.setOneRecordLength(oneRecordLength);

        metadata.setFields(fields);

        return metadata;
    }

    public static int calculateOneRecordLength(List<DbfField> fields) {
        int result = 0;
        for (DbfField field : fields) {
            result += field.getLength();
        }
        result++;
        return result;
    }

    private static int calculateFullHeaderLength(List<DbfField> fields) {
        int result = 32;
        result += 32 * fields.size();
        result++;
        return result;
    }

//	public static byte[] toByteArray(DbfMetadata metadata) {
//		
//	}

    public static void fillHeaderFields(DbfMetadata metadata, byte[] headerBytes) throws IOException {
        metadata.setType(DbfFileTypeEnum.fromInt(headerBytes[0]));
        metadata.setUpdateDate(parseHeaderUpdateDate(headerBytes[1], headerBytes[2], headerBytes[3], metadata.getType()));
        metadata.setRecordsQty(BitUtils.makeInt(headerBytes[4], headerBytes[5], headerBytes[6], headerBytes[7]));
        metadata.setFullHeaderLength(BitUtils.makeInt(headerBytes[8], headerBytes[9]));
        metadata.setOneRecordLength(BitUtils.makeInt(headerBytes[10], headerBytes[11]));
        metadata.setUncompletedTxFlag(headerBytes[14]);
        metadata.setEcnryptionFlag(headerBytes[15]);
        metadata.setCharset(DbfCharset.fromInt(headerBytes[29]));
    }

    public static LocalDate parseHeaderUpdateDate(byte yearByte, byte monthByte, byte dayByte, DbfFileTypeEnum fileType) {
        int year = yearByte + 2000 - 1900;
        switch (fileType) {
            case FoxBASEPlus1:
                year = yearByte;
        }
        return LocalDate.of(year, monthByte, dayByte);
    }

    public static void readFields(DbfMetadata metadata, InputStream inputStream) throws IOException {
        List<DbfField> fields = new ArrayList<>();
        byte[] fieldBytes = new byte[JdbfUtils.FIELD_RECORD_LENGTH];
        int headerLength = 0;
        int fieldLength = 0;
        while (true) {
            if (inputStream.read(fieldBytes) != JdbfUtils.FIELD_RECORD_LENGTH)
                throw new IOException("The file is corrupted or is not a dbf file");

            DbfField field = createDbfField(fieldBytes);
            fields.add(field);

            fieldLength += field.getLength();
            headerLength += fieldBytes.length;

            long oldAvailable = inputStream.available();
            int terminator = inputStream.read();
            if (terminator == -1) {
                throw new IOException("The file is corrupted or is not a dbf file");
            } else if (terminator == JdbfUtils.HEADER_TERMINATOR) {
                break;
            } else {
                inputStream.reset();
                inputStream.skip(inputStream.available() - oldAvailable);
            }
        }
        fieldLength += 1;
        headerLength += 32;
        headerLength += 1;

        if (headerLength != metadata.getFullHeaderLength()) {
            // TODO: handle this anyway!
        }
        if (fieldLength != metadata.getOneRecordLength()) {
            // TODO: handle this anyway!
        }

        metadata.setFields(fields);
    }

    public static DbfField createDbfField(byte[] fieldBytes) {
        DbfField field = new DbfField();
        // 1. Set name
        {
            int i = 0;
            for (i = 0; i < 11 && fieldBytes[i] > 0; i++) ;
            field.setName(new String(fieldBytes, 0, i));
        }
        // 2. Set type
        field.setType(DbfFieldTypeEnum.fromChar((char) fieldBytes[11]));
        // 3. Set length
        {
            int length = fieldBytes[16];
            if (length < 0) {
                length = 256 + length;
            }
            field.setLength(length);
        }
        // 4. Set number of decimal places
        field.setNumberOfDecimalPlaces(fieldBytes[17]);

        return field;
    }

    public static void writeDbfField(DbfField field, byte[] fieldBytes) {
        BitUtils.memset(fieldBytes, 0);
        byte[] nameBytes = field.getName().getBytes();
        int nameLength = nameBytes.length;
        if (nameLength > 11) {
            // throw error here!
        }
        System.arraycopy(nameBytes, 0, fieldBytes, 0, nameBytes.length);
        fieldBytes[11] = field.getType().toByte();
        byte[] b = BitUtils.makeByte4(field.getOffset());
        fieldBytes[12] = b[0];
        fieldBytes[13] = b[1];
        fieldBytes[14] = b[2];
        fieldBytes[15] = b[3];
        int length = field.getLength();
        fieldBytes[16] = (byte) (length & 0xff);
        fieldBytes[17] = (byte) (field.getNumberOfDecimalPlaces() & 0xff);
    }

    public static byte[] toByteArrayHeader(DbfMetadata metadata) {
        byte[] headerBytes = new byte[FILE_HEADER_SIZE];
        BitUtils.memset(headerBytes, 0);


        headerBytes[0] = metadata.getType().toByte();

        LocalDate updateDate = metadata.getUpdateDate();
        // date
        if (updateDate == null) {
            updateDate = LocalDate.now();
        }
        // write date bytes
        {
            byte[] dateBytes = JdbfUtils.writeDateForHeader(updateDate);
            headerBytes[1] = dateBytes[0];
            headerBytes[2] = dateBytes[1];
            headerBytes[3] = dateBytes[2];
        }

        byte[] b = BitUtils.makeByte4(metadata.getRecordsQty());
        headerBytes[4] = b[0];
        headerBytes[5] = b[1];
        headerBytes[6] = b[2];
        headerBytes[7] = b[3];

        b = BitUtils.makeByte2(metadata.getFullHeaderLength());
        headerBytes[8] = b[0];
        headerBytes[9] = b[1];

        b = BitUtils.makeByte2(metadata.getOneRecordLength());
        headerBytes[10] = b[0];
        headerBytes[11] = b[1];

        headerBytes[12] = 0;
        headerBytes[13] = 0;

        headerBytes[14] = metadata.getUncompletedTxFlag();
        headerBytes[15] = metadata.getEcnryptionFlag();

        return headerBytes;
    }
}
