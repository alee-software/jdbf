package net.iryndin.jdbf.util;

import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfFieldTypeEnum;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JdbfUtils {
    public static final int FILE_HEADER_SIZE = 32;
    public static final int RECORD_HEADER_LENGTH = 8;
    public static int EMPTY = 0x20;
    public static final int FIELD_RECORD_LENGTH = 32;
    public static final int HEADER_TERMINATOR = 0x0D;

    public static final int MEMO_HEADER_LENGTH = 0x200; // 512 bytes

    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static List<DbfField> createFieldsFromString(String fieldsString) {
        List<DbfField> list = new ArrayList<>();
        String[] a = fieldsString.split("\\|");
        for (String b : a) {
            if (b.trim().length() == 0) {
                continue;
            }
            DbfField f = createDbfFieldFromString(b);
            list.add(f);
        }
        return list;
    }

    public static DbfField createDbfFieldFromString(String s) {
        String[] a = s.split(",");

        DbfField f = new DbfField();
        f.setName(a[0]);
        f.setType(DbfFieldTypeEnum.fromChar(a[1].charAt(0)));
        f.setLength(Integer.parseInt(a[2]));
        f.setNumberOfDecimalPlaces(Integer.parseInt(a[3]));

        return f;
    }

    public static byte[] writeDateForHeader(LocalDate date) {
        byte[] headerBytes = {
                (byte) (date.getYear() - 100),
                (byte) (date.getMonth().getValue()),
                (byte) (date.getDayOfMonth()),
        };
        return headerBytes;
    }

    public static byte[] writeDate(LocalDate date) {
        String s = dateFormat.format(date);
        return s.getBytes();
    }

    public static LocalDate parseDate(String s) {
        return LocalDate.parse(s, dateFormat);
    }

    public static boolean compareMaps(Map<String, Object> m1, Map<String, Object> m2) {
        if (!compareSets(m1.keySet(), m2.keySet())) {
            return false;
        }
        for (String s : m1.keySet()) {
            if (!compareObjects(m1.get(s), m2.get(s))) {
                return false;
            }
        }
        return true;
    }

    public static boolean compareSets(Set<String> set1, Set<String> set2) {
        if (set1.size() != set2.size()) {
            return false;
        }
        for (String s : set1) {
            if (!set2.contains(s)) {
                return false;
            }
        }
        return true;
    }

    public static boolean compareObjects(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            if (o2 == null) {
                return false;
            } else {
                return o1.equals(o2);
            }
        }
    }

    // todo All in UTC

    public static ZonedDateTime parseDateTime(String s, ZoneId zoneId) {
        return ZonedDateTime.parse(s, dateTimeFormat.withZone(zoneId));
    }

    /**
     * The difference between the ISO and Julian epoch day count (Julian 0001-01-01 to ISO 1970-01-01).
     */
    private static final int JULIAN_0001_TO_ISO_1970 = 678577 + 40587;  // MJD values

    public static ZonedDateTime parseJulianDateTime(int date, int time, ZoneId zoneId) {
        // Начала юлианского периода = 1 января 4713год до н.э.
        long year = 1 - 4713;
        long dayOfYear = 1;
        long julianEpochDay = ((year - 1) * 365) + Math.floorDiv((year - 1), 4) + (dayOfYear - 1);
        return LocalDate.ofEpochDay(julianEpochDay - JULIAN_0001_TO_ISO_1970)
                .plusDays(date)
                .atStartOfDay(zoneId)
                .plus(time, ChronoUnit.MILLIS);
    }

    public static byte[] writeJulianDate(ZonedDateTime d) {
        ByteBuffer bb = ByteBuffer.allocate(8);

        bb.putInt(0, julianDay(d));
        bb.putInt(4, d.getHour() * 60 * 60 * 1000 + d.getMinute() * 60 * 1000 + d.getSecond() * 1000);

        return bb.array();
    }

    private static int julianDay(ZonedDateTime d) {
        int year = d.getYear();
        int month = d.getMonth().getValue();
        int day = d.getDayOfMonth();

        double extra = (100.0 * year) + month - 190002.5;
        long l = (long) ((367.0 * year) -
                (Math.floor(7.0 * (year + Math.floor((month + 9.0) / 12.0)) / 4.0)) +
                Math.floor((275.0 * month) / 9.0) +
                day);

        // Unsigned types are too complicated they said... Only having signed ones makes it easier they said
        if (l > Integer.MAX_VALUE)
            return ~((int) l & Integer.MAX_VALUE);
        else
            return (int) (l & Integer.MAX_VALUE);
    }
}
