package me.heartalborada.biliDownloader.Utils;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    public static String StrArrToSting(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i != arr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static ZonedDateTime timestampToDate(long timestamp, ZoneId id) {
        Timestamp con = new Timestamp(timestamp);
        return con.toLocalDateTime().atZone(id);
    }

    public static String zonedDateToFormatString(ZonedDateTime date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return formatter.format(date);
    }

    public static String byteToUnit(long byteSize) {
        long size = 1;
        String[] unit = new String[]{"B", "KB", "MB", "GB", "TB"};
        String str = null;
        for (int i = 0; i < unit.length; i++) {
            long nextSize = size << 10;
            if (byteSize < nextSize) {
                str = String.format("%.2f%s", byteSize / (size + 0.0f), unit[i]);
                break;
            }
            size = nextSize;
        }
        return str == null ? String.format("%.2f%s", byteSize / (1 << 10 * (unit.length - 1)) + 0.0f, unit[unit.length - 1]) : str;
    }

    public static String generateProgressBar(char barCompleteChar, char barIncompleteChar, int barLength, long total, long current) {
        long p = total / barLength;
        StringBuilder sb = new StringBuilder();
        for (int i = 0, g = 0; i < barLength; i++, g += p) {
            if (g < current)
                sb.append(barCompleteChar);
            else
                sb.append(barIncompleteChar);
        }
        return sb.toString();
    }

    public static Field getTargetClassModifiers(Class clazz) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            try {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);
                Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                Field modifiers = null;
                for (Field each : fields) {
                    if ("modifiers".equals(each.getName())) {
                        modifiers = each;
                    }
                }
                return modifiers;
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

    public static int calculateHalfWidth(String text) {
        int totalWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int t = UCharacter.getIntPropertyValue(c, UProperty.EAST_ASIAN_WIDTH);
            if (UCharacter.EastAsianWidth.WIDE == t || UCharacter.EastAsianWidth.FULLWIDTH == t) {
                totalWidth += 2;
            } else {
                totalWidth++;
            }
        }
        return totalWidth;
    }

    public static class NumberUtils {

        private static final String THOUSAND_UNIT = "K";
        private static final String MILLION_UNIT = "M";
        private static final BigDecimal MILLION = new BigDecimal(1000000);
        private static final BigDecimal THOUSAND = new BigDecimal(1000);

        public static String amountConversion(BigDecimal amount) {
            if (amount == null) {
                return null;
            }
            amount = amount.setScale(1, RoundingMode.HALF_DOWN);
            if (amount.abs().compareTo(THOUSAND.multiply(BigDecimal.valueOf(10))) < 0) {
                return amount.stripTrailingZeros().toPlainString();
            }
            if (amount.abs().compareTo(MILLION.multiply(BigDecimal.valueOf(10))) < 0) {
                return amount.divide(THOUSAND, 4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + THOUSAND_UNIT;
            }
            return amount.divide(MILLION, 4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + MILLION_UNIT;
        }
    }
}
