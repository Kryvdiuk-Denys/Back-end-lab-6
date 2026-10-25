package com.coworking.booking.scheduling;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public final class QuartzJobParameters {

    private QuartzJobParameters() {
    }

    public static int requirePositiveInt(JobExecutionContext context, String key) throws JobExecutionException {
        int value = readInt(context, key);
        if (value <= 0) {
            throw new JobExecutionException("Quartz parameter '" + key + "' must be greater than zero");
        }
        return value;
    }

    public static int requireNonNegativeInt(JobExecutionContext context, String key) throws JobExecutionException {
        int value = readInt(context, key);
        if (value < 0) {
            throw new JobExecutionException("Quartz parameter '" + key + "' must not be negative");
        }
        return value;
    }

    private static int readInt(JobExecutionContext context, String key) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        Object value = dataMap.get(key);
        if (!(value instanceof Number number)) {
            throw new JobExecutionException(
                    "Quartz parameter '" + key + "' is missing or is not an integer"
            );
        }
        return number.intValue();
    }
}
