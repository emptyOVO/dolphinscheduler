/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.task.api.parser;

import static org.apache.commons.lang3.time.DateUtils.addWeeks;
import static org.apache.dolphinscheduler.common.utils.DateUtils.addDays;
import static org.apache.dolphinscheduler.common.utils.DateUtils.addMinutes;
import static org.apache.dolphinscheduler.common.utils.DateUtils.addMonths;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.ADD_CHAR;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.ADD_MONTHS;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.ADD_STRING;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.COMMA;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.DIVISION_CHAR;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.DIVISION_STRING;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.HYPHEN;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.LAST_DAY;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.LEFT_BRACE_CHAR;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.LEFT_BRACE_STRING;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.MONTH_BEGIN;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.MONTH_END;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.MONTH_FIRST_DAY;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.MONTH_LAST_DAY;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.MULTIPLY_CHAR;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.MULTIPLY_STRING;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.N;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.P;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.PARAMETER_FORMAT_TIME;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.RIGHT_BRACE_CHAR;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.SUBTRACT_CHAR;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.SUBTRACT_STRING;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.THIS_DAY;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.TIMESTAMP;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.WEEK_BEGIN;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.WEEK_END;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.WEEK_FIRST_DAY;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.WEEK_LAST_DAY;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.YEAR_WEEK;

import org.apache.dolphinscheduler.common.utils.DateUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

/**
 * time place holder utils
 */
@Slf4j
public class TimePlaceholderUtils {

    /**
     * calculate expression's value
     *
     * @param expression expression
     * @return expression's value
     */
    public static Integer calculate(String expression) {
        expression = StringUtils.trim(expression);
        expression = convert(expression);

        List<String> result = string2List(expression);
        result = convert2SuffixList(result);

        return calculate(result);
    }

    /**
     * Change the sign in the expression to P (positive) N (negative)
     *
     * @param expression
     * @return eg. "-3+-6*(+8)-(-5) -> S3+S6*(P8)-(S5)"
     */
    private static String convert(String expression) {
        char[] arr = expression.toCharArray();

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == SUBTRACT_CHAR) {
                if (i == 0) {
                    arr[i] = N;
                } else {
                    char c = arr[i - 1];
                    if (c == ADD_CHAR || c == SUBTRACT_CHAR || c == MULTIPLY_CHAR || c == DIVISION_CHAR
                            || c == LEFT_BRACE_CHAR) {
                        arr[i] = N;
                    }
                }
            } else if (arr[i] == ADD_CHAR) {
                if (i == 0) {
                    arr[i] = P;
                } else {
                    char c = arr[i - 1];
                    if (c == ADD_CHAR || c == SUBTRACT_CHAR || c == MULTIPLY_CHAR || c == DIVISION_CHAR
                            || c == LEFT_BRACE_CHAR) {
                        arr[i] = P;
                    }
                }
            }
        }

        return new String(arr);
    }

    /**
     * to suffix expression
     *
     * @param srcList
     * @return
     */
    private static List<String> convert2SuffixList(List<String> srcList) {
        List<String> result = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (int i = 0; i < srcList.size(); i++) {
            if (Character.isDigit(srcList.get(i).charAt(0))) {
                result.add(srcList.get(i));
            } else {
                switch (srcList.get(i).charAt(0)) {
                    case LEFT_BRACE_CHAR:
                        stack.push(srcList.get(i));
                        break;
                    case RIGHT_BRACE_CHAR:
                        while (!LEFT_BRACE_STRING.equals(stack.peek())) {
                            result.add(stack.pop());
                        }
                        stack.pop();
                        break;
                    default:
                        while (!stack.isEmpty() && compare(stack.peek(), srcList.get(i))) {
                            result.add(stack.pop());
                        }
                        stack.push(srcList.get(i));
                        break;
                }
            }
        }

        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }

        return result;
    }

    /**
     * Calculate the suffix expression
     *
     * @param result
     * @return
     */
    private static Integer calculate(List<String> result) {
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < result.size(); i++) {
            if (Character.isDigit(result.get(i).charAt(0))) {
                stack.push(Integer.parseInt(result.get(i)));
            } else {
                Integer backInt = stack.pop();
                Integer frontInt = 0;
                char op = result.get(i).charAt(0);

                if (!(op == P || op == N)) {
                    frontInt = stack.pop();
                }

                Integer res = 0;
                switch (result.get(i).charAt(0)) {
                    case P:
                        res = frontInt + backInt;
                        break;
                    case N:
                        res = frontInt - backInt;
                        break;
                    case ADD_CHAR:
                        res = frontInt + backInt;
                        break;
                    case SUBTRACT_CHAR:
                        res = frontInt - backInt;
                        break;
                    case MULTIPLY_CHAR:
                        res = frontInt * backInt;
                        break;
                    case DIVISION_CHAR:
                        res = frontInt / backInt;
                        break;
                    default:
                        break;
                }
                stack.push(res);
            }
        }

        return stack.pop();
    }

    /**
     * string to list
     *
     * @param expression
     * @return list
     */
    private static List<String> string2List(String expression) {
        List<String> result = new ArrayList<>();
        String num = "";
        for (int i = 0; i < expression.length(); i++) {
            if (Character.isDigit(expression.charAt(i))) {
                num = num + expression.charAt(i);
            } else {
                if (!StringUtils.isEmpty(num)) {
                    result.add(num);
                }
                result.add(expression.charAt(i) + "");
                num = "";
            }
        }

        if (!num.isEmpty()) {
            result.add(num);
        }

        return result;
    }

    /**
     * compare loginUser level
     *
     * @param peek
     * @param cur
     * @return true or false
     */
    private static boolean compare(String peek, String cur) {
        if (MULTIPLY_STRING.equals(peek) && (DIVISION_STRING.equals(cur) || MULTIPLY_STRING.equals(cur)
                || ADD_STRING.equals(cur) || SUBTRACT_STRING.equals(cur))) {
            return true;
        } else if (DIVISION_STRING.equals(peek) && (DIVISION_STRING.equals(cur) || MULTIPLY_STRING.equals(cur)
                || ADD_STRING.equals(cur) || SUBTRACT_STRING.equals(cur))) {
            return true;
        } else if (ADD_STRING.equals(peek) && (ADD_STRING.equals(cur) || SUBTRACT_STRING.equals(cur))) {
            return true;
        } else {
            return SUBTRACT_STRING.equals(peek) && (ADD_STRING.equals(cur) || SUBTRACT_STRING.equals(cur));
        }

    }

    /**
     * Format time expression with given date, the date cannot be null.
     * <p> If the expression is not a time expression, return the original expression.
     *
     */
    public static String formatTimeExpression(final String timeExpression, final Date date,
                                              final boolean ignoreInvalidExpression) {
        // After N years: $[add_months(yyyyMMdd,12*N)], the first N months: $[add_months(yyyyMMdd,-N)], etc
        if (date == null) {
            throw new IllegalArgumentException("Cannot parse the expression: " + timeExpression + ", date is null");
        }
        if (StringUtils.isEmpty(timeExpression)) {
            if (ignoreInvalidExpression) {
                return timeExpression;
            }
            throw new IllegalArgumentException("Cannot format the date" + date + " with null timeExpression");
        }
        try {
            if (timeExpression.startsWith(TIMESTAMP)) {
                return calculateTimeStamp(timeExpression, date);
            }
            if (timeExpression.startsWith(YEAR_WEEK)) {
                return calculateYearWeek(timeExpression, date);
            }
            return calcTimeExpression(timeExpression, date);
        } catch (Exception e) {
            if (ignoreInvalidExpression) {
                return timeExpression;
            }
            throw new IllegalArgumentException("Unsupported placeholder expression: " + timeExpression, e);
        }
    }

    /**
     * get week of year
     * @param expression expression
     * @param date       date
     * @return week of year
     */
    private static String calculateYearWeek(final String expression, final Date date) {

        String dataFormat = expression.substring(YEAR_WEEK.length() + 1, expression.length() - 1);

        String targetDate = "";
        try {

            if (dataFormat.contains(COMMA)) {
                String param1 = dataFormat.split(COMMA)[0];
                String param2 = dataFormat.split(COMMA)[1];
                dataFormat = param1;

                targetDate = transformYearWeek(date, dataFormat, calculate(param2));

            } else {
                targetDate = transformYearWeek(date, dataFormat, 1);
            }
        } catch (Exception e) {
            throw new RuntimeException("expression not valid");
        }

        return targetDate;
    }

    /**
     * transform week of year
     * @param date date
     * @param format date_format,for example: yyyy-MM-dd / yyyyMMdd
     * @param weekDay day of week
     * @return date_string
     */
    private static String transformYearWeek(Date date, String format, int weekDay) {
        Calendar calendar = Calendar.getInstance();
        // Minimum number of days required for the first week of the year
        calendar.setMinimalDaysInFirstWeek(4);

        // By default ,Set Monday as the first day of the week
        switch (weekDay) {
            case 2:
                calendar.setFirstDayOfWeek(Calendar.TUESDAY);
                break;
            case 3:
                calendar.setFirstDayOfWeek(Calendar.WEDNESDAY);
                break;
            case 4:
                calendar.setFirstDayOfWeek(Calendar.THURSDAY);
                break;
            case 5:
                calendar.setFirstDayOfWeek(Calendar.FRIDAY);
                break;
            case 6:
                calendar.setFirstDayOfWeek(Calendar.SATURDAY);
                break;
            case 7:
                calendar.setFirstDayOfWeek(Calendar.SUNDAY);
                break;
            default:
                calendar.setFirstDayOfWeek(Calendar.MONDAY);
                break;
        }
        calendar.setTimeInMillis(date.getTime());

        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

        int year = calendar.get(Calendar.YEAR);

        String weekYearStr = "";
        if (weekOfYear < 10 && format.contains(HYPHEN)) {
            weekYearStr = String.format("%d%s0%d", year, HYPHEN, weekOfYear);
        } else if (weekOfYear >= 10 && format.contains(HYPHEN)) {
            weekYearStr = String.format("%d%s%d", year, HYPHEN, weekOfYear);
        } else if (weekOfYear < 10) {
            weekYearStr = String.format("%d0%d", year, weekOfYear);
        } else {
            weekYearStr = String.format("%d%d", year, weekOfYear);
        }

        return weekYearStr;
    }

    private static String calculateTimeStamp(final String expression, final Date date) {
        final String timeExpression = expression.substring(TIMESTAMP.length() + 1, expression.length() - 1);
        final String calculatedTimeExpression = calcTimeExpression(timeExpression, date);
        if (StringUtils.equals(expression, calculatedTimeExpression)) {
            return expression;
        }
        final Date timestamp = DateUtils.parse(calculatedTimeExpression, PARAMETER_FORMAT_TIME);
        if (timestamp == null) {
            throw new IllegalArgumentException("Cannot parse the expression: " + expression + " with date: " + date);
        }
        return String.valueOf(timestamp.getTime() / 1000);
    }

    /**
     * calculate time expresstion
     *
     * @param expression expresstion
     * @param date       date
     * @return map with date, date format
     */
    private static String calcTimeExpression(final String expression, final Date date) {
        final Map.Entry<Date, String> resultEntry;

        if (expression.startsWith(ADD_MONTHS)) {
            resultEntry = calcMonths(expression, date);
        } else if (expression.startsWith(MONTH_BEGIN)) {
            resultEntry = calcMonthBegin(expression, date);
        } else if (expression.startsWith(MONTH_END)) {
            resultEntry = calcMonthEnd(expression, date);
        } else if (expression.startsWith(WEEK_BEGIN)) {
            resultEntry = calcWeekStart(expression, date);
        } else if (expression.startsWith(WEEK_END)) {
            resultEntry = calcWeekEnd(expression, date);
        } else if (expression.startsWith(MONTH_FIRST_DAY)) {
            resultEntry = calcCustomDay(expression, MONTH_FIRST_DAY, date);
        } else if (expression.startsWith(MONTH_LAST_DAY)) {
            resultEntry = calcCustomDay(expression, MONTH_LAST_DAY, date);
        } else if (expression.startsWith(THIS_DAY)) {
            resultEntry = calcCustomDay(expression, THIS_DAY, date);
        } else if (expression.startsWith(LAST_DAY)) {
            resultEntry = calcCustomDay(expression, LAST_DAY, date);
        } else if (expression.startsWith(WEEK_FIRST_DAY)) {
            resultEntry = calcCustomDay(expression, WEEK_FIRST_DAY, date);
        } else if (expression.startsWith(WEEK_LAST_DAY)) {
            resultEntry = calcCustomDay(expression, WEEK_LAST_DAY, date);
        } else {
            resultEntry = calcMinutes(expression, date);
        }
        final Date finalDate = resultEntry.getKey();
        final String finalFormat = resultEntry.getValue();
        return DateUtils.format(finalDate, finalFormat);
    }

    /**
     * get first day of month
     *
     * @param expression expresstion
     * @param date       date
     * @return first day of month
     */
    public static Map.Entry<Date, String> calcMonthBegin(String expression, Date date) {
        String addMonthExpr = expression.substring(MONTH_BEGIN.length() + 1, expression.length() - 1);
        String[] params = addMonthExpr.split(COMMA);

        if (params.length == 2) {
            String dateFormat = params[0];
            String dayExpr = params[1];
            Integer day = calculate(dayExpr);
            Date targetDate = DateUtils.getFirstDayOfMonth(date);
            targetDate = addDays(targetDate, day);

            return new AbstractMap.SimpleImmutableEntry<>(targetDate, dateFormat);
        }

        throw new RuntimeException("expression not valid");
    }

    /**
     * get last day of month
     *
     * @param expression expresstion
     * @param date       date
     * @return last day of month
     */
    public static Map.Entry<Date, String> calcMonthEnd(String expression, Date date) {
        String addMonthExpr = expression.substring(MONTH_END.length() + 1, expression.length() - 1);
        String[] params = addMonthExpr.split(COMMA);

        if (params.length == 2) {
            String dateFormat = params[0];
            String dayExpr = params[1];
            Integer day = calculate(dayExpr);
            Date targetDate = DateUtils.getLastDayOfMonth(date);
            targetDate = addDays(targetDate, day);

            return new AbstractMap.SimpleImmutableEntry<>(targetDate, dateFormat);
        }

        throw new RuntimeException("expression not valid");
    }

    /**
     * calculate time expression
     * month first day month last day
     * @param expression expression
     * @param date       date
     * @return calculate time expression with date,format
     */
    public static Map.Entry<Date, String> calcCustomDay(String expression, String keyDate, Date date) {
        String dataFormat = "yyyy-MM-dd";
        Date targetDate = new Date();

        switch (keyDate) {
            case MONTH_FIRST_DAY:
                dataFormat = expression.substring(MONTH_FIRST_DAY.length() + 1, expression.length() - 1);

                if (dataFormat.contains(COMMA)) {
                    String param1 = dataFormat.split(COMMA)[0];
                    String param2 = dataFormat.split(COMMA)[1];
                    dataFormat = param1;

                    targetDate = addMonths(DateUtils.getFirstDayOfMonth(date), calculate(param2));
                } else {
                    targetDate = DateUtils.getFirstDayOfMonth(date);
                }

                break;
            case MONTH_LAST_DAY:
                dataFormat = expression.substring(MONTH_LAST_DAY.length() + 1, expression.length() - 1);

                if (dataFormat.contains(COMMA)) {
                    String param1 = dataFormat.split(COMMA)[0];
                    String param2 = dataFormat.split(COMMA)[1];
                    dataFormat = param1;

                    Date lastMonthDay = addMonths(date, calculate(param2));

                    targetDate = DateUtils.getLastDayOfMonth(lastMonthDay);

                } else {
                    targetDate = DateUtils.getLastDayOfMonth(date);
                }
                break;
            case THIS_DAY:
                dataFormat = expression.substring(THIS_DAY.length() + 1, expression.length() - 1);
                targetDate = addDays(date, 0);
                break;
            case LAST_DAY:
                dataFormat = expression.substring(LAST_DAY.length() + 1, expression.length() - 1);
                targetDate = addDays(date, -1);
                break;
            case WEEK_FIRST_DAY:
                dataFormat = expression.substring(WEEK_FIRST_DAY.length() + 1, expression.length() - 1);

                if (dataFormat.contains(COMMA)) {
                    String param1 = dataFormat.split(COMMA)[0];
                    String param2 = dataFormat.split(COMMA)[1];
                    dataFormat = param1;

                    targetDate = addWeeks(DateUtils.getMonday(date), calculate(param2));
                } else {
                    targetDate = addWeeks(DateUtils.getMonday(date), 0);
                }
                break;
            case WEEK_LAST_DAY:
                dataFormat = expression.substring(WEEK_LAST_DAY.length() + 1, expression.length() - 1);

                if (dataFormat.contains(COMMA)) {
                    String param1 = dataFormat.split(COMMA)[0];
                    String param2 = dataFormat.split(COMMA)[1];
                    dataFormat = param1;

                    targetDate = addWeeks(DateUtils.getSunday(date), calculate(param2));
                } else {
                    targetDate = addWeeks(DateUtils.getSunday(date), 0);
                }
                break;
            default:
                break;
        }

        return new AbstractMap.SimpleImmutableEntry<>(targetDate, dataFormat);

    }

    /**
     * get first day of week
     *
     * @param expression expresstion
     * @param date       date
     * @return monday
     */
    public static Map.Entry<Date, String> calcWeekStart(String expression, Date date) {
        String addMonthExpr = expression.substring(WEEK_BEGIN.length() + 1, expression.length() - 1);
        String[] params = addMonthExpr.split(COMMA);

        if (params.length == 2) {
            String dateFormat = params[0];
            String dayExpr = params[1];
            Integer day = calculate(dayExpr);
            Date targetDate = DateUtils.getMonday(date);
            targetDate = addDays(targetDate, day);
            return new AbstractMap.SimpleImmutableEntry<>(targetDate, dateFormat);
        }

        throw new RuntimeException("expression not valid");
    }

    /**
     * get last day of week
     *
     * @param expression expresstion
     * @param date       date
     * @return last day of week
     */
    public static Map.Entry<Date, String> calcWeekEnd(String expression, Date date) {
        String addMonthExpr = expression.substring(WEEK_END.length() + 1, expression.length() - 1);
        String[] params = addMonthExpr.split(COMMA);

        if (params.length == 2) {
            String dateFormat = params[0];
            String dayExpr = params[1];
            Integer day = calculate(dayExpr);
            Date targetDate = DateUtils.getSunday(date);
            targetDate = addDays(targetDate, day);

            return new AbstractMap.SimpleImmutableEntry<>(targetDate, dateFormat);
        }

        throw new RuntimeException("Expression not valid");
    }

    /**
     * calc months expression
     *
     * @param expression expresstion
     * @param date       date
     * @return calc months
     */
    public static Map.Entry<Date, String> calcMonths(String expression, Date date) {
        String addMonthExpr = expression.substring(ADD_MONTHS.length() + 1, expression.length() - 1);
        String[] params = addMonthExpr.split(COMMA);

        if (params.length == 2) {
            String dateFormat = params[0];
            String monthExpr = params[1];
            Integer addMonth = calculate(monthExpr);
            Date targetDate = addMonths(date, addMonth);

            return new AbstractMap.SimpleImmutableEntry<>(targetDate, dateFormat);
        }

        throw new RuntimeException("expression not valid");
    }

    /**
     * calculate time expression
     *
     * @param expression expresstion
     * @param date       date
     * @return calculate time expression with date,format
     */
    public static Map.Entry<Date, String> calcMinutes(String expression, Date date) {
        if (expression.contains("+")) {
            int index = expression.lastIndexOf('+');

            if (Character.isDigit(expression.charAt(index + 1))) {
                String addMinuteExpr = expression.substring(index + 1);
                Date targetDate = addMinutes(date, calcMinutes(addMinuteExpr));
                String dateFormat = expression.substring(0, index);

                return new AbstractMap.SimpleImmutableEntry<>(targetDate, dateFormat);
            }
        } else if (expression.contains("-")) {
            int index = expression.lastIndexOf('-');

            if (Character.isDigit(expression.charAt(index + 1))) {
                String addMinuteExpr = expression.substring(index + 1);
                Date targetDate = addMinutes(date, 0 - calcMinutes(addMinuteExpr));
                String dateFormat = expression.substring(0, index);

                return new AbstractMap.SimpleImmutableEntry<>(targetDate, dateFormat);
            }

            // yyyy-MM-dd/HH:mm:ss
            return new AbstractMap.SimpleImmutableEntry<>(date, expression);
        }

        // $[HHmmss]
        return new AbstractMap.SimpleImmutableEntry<>(date, expression);
    }

    /**
     * calculate need minutes
     *
     * @param minuteExpression minute expression
     * @return calculate need minutes
     */
    public static Integer calcMinutes(String minuteExpression) {
        int index = minuteExpression.indexOf('/');

        String calcExpression;

        if (index == -1) {
            calcExpression = String.format("60*24*(%s)", minuteExpression);
        } else {

            calcExpression = String.format("60*24*(%s)%s", minuteExpression.substring(0, index),
                    minuteExpression.substring(index));
        }

        return calculate(calcExpression);
    }

}
