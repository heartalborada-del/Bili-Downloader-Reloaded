package me.heartalborada.biliDownloader.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Util {
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

    public static class NumberUtils {

        private static final String THOUSAND_UNIT = "K";
        private static final String MILLION_UNIT = "M";
        private static final BigDecimal MILLION = new BigDecimal(1000000);
        private static final BigDecimal THOUSAND = new BigDecimal(1000);

        public static String amountConversion(BigDecimal amount){
            if (amount == null) {
                return null;
            }
            amount = amount.setScale(1,RoundingMode.HALF_DOWN);
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
