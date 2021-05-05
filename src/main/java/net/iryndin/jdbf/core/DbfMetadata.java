package net.iryndin.jdbf.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DbfMetadata {
    private DbfFileTypeEnum type;
    private LocalDate updateDate;
    private int recordsQty;
    private int fullHeaderLength;
    private int oneRecordLength;
    private byte uncompletedTxFlag;
    private byte ecnryptionFlag;
    private Charset charset;
    private Map<String, DbfField> fieldMap;

    public DbfFileTypeEnum getType() {
        return type;
    }

    public void setType(DbfFileTypeEnum type) throws IOException {
        if (type == null)
            throw new IOException("The file is corrupted or is not a dbf file");
        this.type = type;
    }

    public LocalDate getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDate updateDate) {
        this.updateDate = updateDate;
    }

    public int getRecordsQty() {
        return recordsQty;
    }

    public void setRecordsQty(int recordsQty) {
        this.recordsQty = recordsQty;
    }

    public int getFullHeaderLength() {
        return fullHeaderLength;
    }

    public void setFullHeaderLength(int fullHeaderLength) {
        this.fullHeaderLength = fullHeaderLength;
    }

    public int getOneRecordLength() {
        return oneRecordLength;
    }

    public void setOneRecordLength(int oneRecordLength) {
        this.oneRecordLength = oneRecordLength;
    }

    public byte getUncompletedTxFlag() {
        return uncompletedTxFlag;
    }

    public void setUncompletedTxFlag(byte uncompletedTxFlag) {
        this.uncompletedTxFlag = uncompletedTxFlag;
    }

    public byte getEcnryptionFlag() {
        return ecnryptionFlag;
    }

    public void setEcnryptionFlag(byte ecnryptionFlag) {
        this.ecnryptionFlag = ecnryptionFlag;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
    }

    public DbfField getField(String name) {
        return fieldMap.get(name);
    }

    public Collection<DbfField> getFields() {
        return fieldMap.values();
    }

    public void setFields(List<DbfField> fields) {
        processFields(fields);
    }

    private void processFields(List<DbfField> fields) {
        fieldMap = new LinkedHashMap<>(fields.size() * 2);
        int offset = 1;
        for (DbfField f : fields) {
            // 1. count offset
            f.setOffset(offset);
            offset += f.getLength();
            // 2. put field into map
            fieldMap.put(f.getName(), f);
        }
    }

    public String getFieldsStringRepresentation() {
        if (fieldMap == null) {
            return null;
        }
        int i = fieldMap.size();
        // i*64 - just to allocate enough space
        StringBuilder sb = new StringBuilder(i * 64);
        for (DbfField f : fieldMap.values()) {
            sb.append(f.getStringRepresentation());
            i--;
            if (i > 0) {
                sb.append("|");
            }
        }
        return sb.toString();
    }

    private String formatUpdateDate() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(updateDate);
    }

    @Override
    public String toString() {
        return "DbfMetadata [\n  type=" + type + ", \n  updateDate="
                + formatUpdateDate() + ", \n  recordsQty=" + recordsQty
                + ", \n  fullHeaderLength=" + fullHeaderLength
                + ", \n  oneRecordLength=" + oneRecordLength
                + ", \n  uncompletedTxFlag=" + uncompletedTxFlag
                + ", \n  ecnryptionFlag=" + ecnryptionFlag + ", \n  fields="
                //+ fields + "\n]";
                + getFieldsStringRepresentation() + "\n]";
    }
}
