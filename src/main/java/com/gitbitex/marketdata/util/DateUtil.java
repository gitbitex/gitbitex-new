package com.gitbitex.marketdata.util;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalField;

public class DateUtil {
    /**
     * https://stackoverflow.com/questions/3553964/how-to-round-time-to-the-nearest-quarter-hour-in-java/37423588
     * #37423588
     *
     * @param input
     * @param roundTo
     * @param roundIncrement
     * @return
     */
    public static ZonedDateTime round(ZonedDateTime input, TemporalField roundTo, int roundIncrement) {
        /* Extract the field being rounded. */
        int field = input.get(roundTo);

        /* Distance from previous floor. */
        int r = field % roundIncrement;

        /* Find floor and ceiling. Truncate values to base unit of field. */
        ZonedDateTime ceiling = input.plus(roundIncrement - r, roundTo.getBaseUnit()).truncatedTo(
                roundTo.getBaseUnit());

        return input.plus(-r, roundTo.getBaseUnit()).truncatedTo(roundTo.getBaseUnit());

        /*
         * Do a half-up rounding.
         *
         * If (input - floor) < (ceiling - input)
         * (i.e. floor is closer to input than ceiling)
         *  then return floor, otherwise return ceiling.
         */

        //return Duration.between(floor, input).compareTo(Duration.between(input, ceiling)) > 0 ? floor : ceiling;
    }

}
