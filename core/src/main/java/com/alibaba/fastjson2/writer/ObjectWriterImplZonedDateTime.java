package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.codec.DateTimeCodec;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

final class ObjectWriterImplZonedDateTime
        extends DateTimeCodec
        implements ObjectWriter {
    static final ObjectWriterImplZonedDateTime INSTANCE = new ObjectWriterImplZonedDateTime(null, null);

    private final Function function;

    public ObjectWriterImplZonedDateTime(String format, Locale locale) {
        this(format, locale, null);
    }

    public ObjectWriterImplZonedDateTime(String format, Locale locale, Function function) {
        super(format, locale);
        this.function = function;
    }

    @Override
    public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        ZonedDateTime zdt;
        if (function != null) {
            zdt = (ZonedDateTime) function.apply(object);
        } else {
            zdt = (ZonedDateTime) object;
        }

        jsonWriter.writeZonedDateTime(zdt);
    }

    @Override
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (object == null) {
            jsonWriter.writeNull();
            return;
        }

        ZonedDateTime zdt;
        if (function != null) {
            zdt = (ZonedDateTime) function.apply(object);
        } else {
            zdt = (ZonedDateTime) object;
        }

        JSONWriter.Context ctx = jsonWriter.context;

        if (formatUnixTime || (format == null && ctx.isDateFormatUnixTime())) {
            long millis = zdt.toInstant().toEpochMilli();
            jsonWriter.writeInt64(millis / 1000);
            return;
        }

        if (formatMillis
                || (format == null
                && ctx.isDateFormatMillis()
                && ctx.provider.namingStrategy != PropertyNamingStrategy.CamelCase1x)
        ) {
            jsonWriter.writeInt64(zdt
                    .toInstant()
                    .toEpochMilli());
            return;
        }

        final int year = zdt.getYear();
        if (year >= 0 && year <= 9999) {
            if (formatISO8601 || ctx.isDateFormatISO8601()) {
                jsonWriter.writeDateTimeISO8601(
                        year,
                        zdt.getMonthValue(),
                        zdt.getDayOfMonth(),
                        zdt.getHour(),
                        zdt.getMinute(),
                        zdt.getSecond(),
                        zdt.getNano() / 1000_000,
                        zdt.getOffset().getTotalSeconds(),
                        true
                );
                return;
            }

            if (yyyyMMddhhmmss19) {
                jsonWriter.writeDateTime19(
                        year,
                        zdt.getMonthValue(),
                        zdt.getDayOfMonth(),
                        zdt.getHour(),
                        zdt.getMinute(),
                        zdt.getSecond()
                );
                return;
            }

            if (yyyyMMddhhmmss14) {
                jsonWriter.writeDateTime14(
                        year,
                        zdt.getMonthValue(),
                        zdt.getDayOfMonth(),
                        zdt.getHour(),
                        zdt.getMinute(),
                        zdt.getSecond()
                );
                return;
            }
        }

        DateTimeFormatter formatter = this.getDateFormatter();
        if (formatter == null) {
            formatter = ctx.getDateFormatter();
        }

        if (formatter == null) {
            jsonWriter.writeZonedDateTime(zdt);
            return;
        }

        String str = formatter.format(zdt);
        jsonWriter.writeString(str);
    }
}
