package com.idevhub.coino.tradingengine.utils;

import java.math.BigDecimal;

public class BigDecimalUtils {

    private BigDecimalUtils() {
    }

    /**
     * Entry points of {@link BigDecimalUtils} <br/>
     * <br/>
     * Example usage:
     *
     * <pre>
     * <code>
     *      is(three).eq(four); //Equal
     * 		is(two).gt(two);    //Greater than
     * 		is(two).gte(one);   //Greater than equal
     * 		is(three).lt(two);  //Less than
     * 		is(three).lte(two); //Less than equal
     *
     *      is(three).notEq(four); //Not Equal
     * 		is(two).notGt(two);    //Not Greater than
     * 		is(two).notGte(one);   //Not Greater than equal
     * 		is(three).notLt(two);  //Not Less than
     * 		is(three).notLte(two); //Not Less than equal
     *
     *      is(three).isZero();
     *      is(three).notZero();
     *      is(three).isPositive(); // greater than zero
     *      is(three).isNegative(); // less than zero
     *      is(three).isNonPositive(); //less than or equal zero
     *      is(three).isNonNegative(); //greater than or equal zero
     * </code>
     * </pre>
     *
     * @param decimal
     *            your {@link BigDecimal}
     *
     * @return {@link BigDecimalWrapper}
     *
     * @see #isNot(BigDecimal)
     */
    public static BigDecimalWrapper is(BigDecimal decimal) {

        return new BigDecimalWrapper(decimal);
    }

    /**
     * Entry points of {@link BigDecimalUtils} <br/>
     * <br/>
     * Example usage:
     *
     * <pre>
     * <code>
     *      is(three).eq(four); //Equal
     * 		is(two).gt(two);    //Greater than
     * 		is(two).gte(one);   //Greater than equal
     * 		is(three).lt(two);  //Less than
     * 		is(three).lte(two); //Less than equal
     *
     *      is(three).notEq(four); //Not Equal
     * 		is(two).notGt(two);    //Not Greater than
     * 		is(two).notGte(one);   //Not Greater than equal
     * 		is(three).notLt(two);  //Not Less than
     * 		is(three).notLte(two); //Not Less than equal
     *
     *      is(three).isZero();
     *      is(three).notZero();
     *      is(three).isPositive(); // greater than zero
     *      is(three).isNegative(); // less than zero
     *      is(three).isNonPositive(); //less than or equal zero
     *      is(three).isNonNegative(); //greater than or equal zero
     *
     *      is(three).isNullOrZero(); //is null or zero
     *      is(three).notNullOrZero(); //not null or zero
     * </code>
     * </pre>
     *
     * @param decimal
     *            your {@link BigDecimal}
     *
     * @return {@link BigDecimalWrapper}
     *
     * @see #isNot(BigDecimal)
     */
    public static BigDecimalWrapper is(double dbl) {
        return is(BigDecimal.valueOf(dbl));
    }

}
