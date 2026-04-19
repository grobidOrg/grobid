package org.grobid.core.data;

import org.grobid.core.utilities.TextUtilities;

/**
 * Class for representing a date.
 * We use our own representation of dates for having a comparable which prioritize the most fully specified
 * dates first, then the earliest date, i.e.:
 * 10.2010 < 2010
 * 20.10.2010 < 10.2010
 * 19.10.2010 < 20.10.2010
 * 1999 < 10.2000
 * 10.1999 < 2000
 * which is not the same as a comparison based only on time flow.
 * For comparing dates by strict time flow, please use java.util.Date + java.util.Calendar
 */
public class Date implements Comparable<Date> {
    private int day = -1;
    private int month = -1;
    private int year = -1;
    private String rawDate = null;
    private String dayString = null;
    private String monthString = null;
    private String yearString = null;

    public Date() {
    }

    public Date(Date fromDate) {
        this.day = fromDate.day;
        this.month = fromDate.month;
        this.year = fromDate.year;
        this.rawDate = fromDate.rawDate;
        this.dayString = fromDate.dayString;
        this.monthString = fromDate.monthString;
        this.yearString = fromDate.yearString;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int d) {
        day = d;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int d) {
        month = d;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int d) {
        year = d;
    }

    public String getRawDate() {
        return rawDate;
    }

    public void setRawDate(String s) {
        rawDate = s;
    }

    public String getDayString() {
        return dayString;
    }

    public void setDayString(String d) {
        dayString = d;
    }

    public String getMonthString() {
        return monthString;
    }

    public void setMonthString(String d) {
        monthString = d;
    }

    public String getYearString() {
        return yearString;
    }

    public void setYearString(String d) {
        yearString = d;
    }

    /*public java.util.Date getJavaDate() {
         java.util.Calendar cal = new java.util.Calendar();
         cal.set(year, month, day);
     }*/

    /**
     * The lowest date always win.
     */
    public int compareTo(Date another) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (another.getYear() == -1) {
            return BEFORE;
        } else if (year == -1) {
            return AFTER;
        } else if (year < another.getYear()) {
            return BEFORE;
        } else if (year > another.getYear()) {
            return AFTER;
        } else {
            // years are identical
            if (another.getMonth() == -1) {
                return BEFORE;
            } else if (month == -1) {
                return AFTER;
            } else if (month < another.getMonth()) {
                return BEFORE;
            } else if (month > another.getMonth()) {
                return AFTER;
            } else {
                // months are identical
                if (another.getDay() == -1) {
                    return BEFORE;
                } else if (day == -1) {
                    return AFTER;
                } else if (day < another.getDay()) {
                    return BEFORE;
                } else if (day > another.getDay()) {
                    return AFTER;
                }
            }
        }

        return EQUAL;
    }

    public boolean isNotNull() {
        return (rawDate != null) ||
            (dayString != null) ||
            (monthString != null) ||
            (yearString != null) ||
            (day != -1) ||
            (month != -1) ||
            (year != -1);
    }

    public boolean isAmbiguous() {
        return false;
    }

    public static String toISOString(Date date) {
        int year = date.getYear();
        int month = date.getMonth();
        int day = date.getDay();

        String when = "";
        if (year != -1) {
            if (year <= 9)
                when += "000" + year;
            else if (year <= 99)
                when += "00" + year;
            else if (year <= 999)
                when += "0" + year;
            else
                when += year;
            if (month != -1) {
                if (month <= 9)
                    when += "-0" + month;
                else
                    when += "-" + month;
                if (day != -1) {
                    if (day <= 9)
                        when += "-0" + day;
                    else
                        when += "-" + day;
                }
            }
        }
        return when;
    }

    /**
     * Return a new date instance by merging the date information from a first date with
     * the date information from a second date.
     * The merging follows the year, month, day sequence. If the years
     * for instance clash, the merging is stopped.
     *
     * Examples of merging:
     * "2010" "2010-10" -> "2010-10"
     * "2010" "2010-10-27" -> "2010-10-27"
     * "2010-10" "2010-10-27" -> "2010-10-27"
     * "2010-10-27" "2010-10" -> "2010-10-27"
     * "2011-10" "2010-10-27" -> "2011-10"
     * "2010" "2016-10-27" -> "2010"
     * "2011" "2010" -> 2011
     */
    public static Date merge(Date date1, Date date2) {
        if (date1.getYear() == -1) {
            return new Date(date2);
        }

        if (date1.getYear() == date2.getYear()) {
            if (date1.getMonth() == -1 && date2.getMonth() != -1) {
                return new Date(date2);
            }
            if (date1.getMonth() == date2.getMonth()) {
                if (date1.getDay() == -1 && date2.getDay() != -1) {
                    return new Date(date2);
                }
            }
        }

        return new Date(date1);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (day != -1) {
            sb.append(day).append("-");
        }
        if (month != -1) {
            sb.append(month).append("-");
        }
        if (year != -1) {
            sb.append(year);
        }
        sb.append(" / ");
        if (dayString != null) {
            sb.append(dayString).append("-");
        }
        if (monthString != null) {
            sb.append(monthString).append("-");
        }
        if (yearString != null) {
            sb.append(yearString);
        }
        return sb.toString();
    }

    public String toTEI() {
		// TEI uses ISO 8601 for date encoding
        String theDate = "<date when=\"";
        if (year != -1) {
            theDate += year;
        }
        if (month != -1) {
            theDate += "-" + month;
        }
        if (day != -1) {
            theDate += "-" + day;
        }

		if (rawDate != null) {
        	theDate += "\">"+TextUtilities.HTMLEncode(rawDate)+"</date>";
		}
		else {
			theDate += "\" />";
		}

        return theDate;
    }

    public String toXML() {
        String theDate = "<date>";
        if (day != -1) {
            theDate += "<day>" + day + "</day>";
        }
        if (month != -1) {
            theDate += "<month>" + month + "</month>";
        }
        if (year != -1) {
            theDate += "<year>" + year + "</year>";
        }

        theDate += "</date>";

        return theDate;
    }

}
