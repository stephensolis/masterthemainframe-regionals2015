/**
 * This program creates a summary report from tables CENSUS.WORLDIMR,
 *   CENSUS.POPULATION, and GROWTH.
 * Written for IBM's Master the Mainframe Contest 2015, Part 3, #16.
 *
 * @author Stephen
 * @date   December 27, 2015
 */

import java.sql.*;
import java.util.*;
import java.math.*;

public class finalRpt {
    /***********\
    * Constants *
    \***********/

    /** The URL of the database server */
    static final String DB_URL = null;

    //The credentials to use to connect to the database server
    static final String DB_USERNAME = null;
    static final String DB_PASSWORD = null;

    /** A textual description for the report */
    static final String REPORT_DESCRIPTION =
        "Countries where growth rate is negative";

    /** The constraint in SQL indicating which countries to add to the report
        (You can set this to "1=1" to include everything) */
    static final String REPORT_CONSTRAINT =
        "growth.rate < 0";

    /** The SQL table to use for queries */
    static final String REPORT_TABLE =
        "growth " +
        "INNER JOIN census.population AS pop " +
            "ON pop.country = growth.country " +
        "INNER JOIN census.worldimr AS worldimr " +
            "ON worldimr.country = growth.country";

    /** The fields to average in generating SummaryData#imrAverages */
    static final String[] IMR_FIELDS = {
        "IMR", "IMRM", "IMRF",
        "IMR1_4", "IMR1_4M", "IMR1_4F",
        "IMR_5", "IMR_5M", "IMR_5F"
    };

    /** The fields to average in generating SummaryData#lerAverages */
    static final String[] LER_FIELDS = {
        "LER", "LERM", "LERF"
    };

    /** The values of 'pop.age' to use in generating
        SummaryData#sexRatioAverages */
    static final String[] SEXRATIO_RANGES = {
        "Total", "0-4", "5-9", "10-14", "15-19", "20-24", "25-29", "30-34",
        "35-39", "40-44", "45-49", "50-54", "55-59", "60-64", "65-69",
        "70-74", "75-79", "80-84", "85-89", "90-94", "95-99", "100+"
    };

    /** The maximum line width of the report */
    static final int PAGE_WIDTH = 80;

    //The number of decimal places to use when printing each field
    //  of CountryRecord
    static final int GROWTHRATE_PRECISION = 2;
    static final int IMR_PRECISION = 1;
    static final int LER_PRECISION = 1;
    static final int SEXRATIO_PRECISION = 1;

    /** The number of top or bottom countries to find in rankCountries */
    static final int RANK_NUM_COUNTRIES = 5;

    /** The number of bars to produce in distribution plots from printStats */
    static final int DISTRIBUTION_BARS = 8;

    /** The length of the longest bar in bar graphs generated by
        makeBarGraph */
    static final int BARGRAPH_LONGEST_BAR = 40;

    /**************\
    * Enumerations *
    \**************/

    /**
     * The possible ways to align text, used in formatTextField.
     */
    enum Alignment {
        LEFT, CENTER, RIGHT
    }

    /**
     * Accessors for float-valued fields in CountryRecord.
     */
    enum CountryField {
        GROWTHRATE {
            @Override
            public float get(CountryRecord country) {
                return country.growthRate;
            }
        },
        IMR {
            @Override
            public float get(CountryRecord country) {
                return country.imr;
            }
        },
        LER {
            @Override
            public float get(CountryRecord country) {
                return country.ler;
            }
        },
        SEXRATIO {
            @Override
            public float get(CountryRecord country) {
                return country.sexRatio;
            }
        };

        /**
         * Get the corresponding field from the given CountryRecord.
         *
         * @param country the CountryRecord to read
         * @return the value of the corresponding field
         */
        public abstract float get(CountryRecord country);
    }

    /*******************\
    * Container classes *
    \*******************/

    /**
     * A container to hold the data of one country.
     */
    static class CountryRecord {
        public String name;
        public float growthRate, imr, ler, sexRatio;
    }

    /**
     * A container to hold the statistics from one database field.
     */
    static class StatsData {
        public float average, stdDev, min, max;
    }

    /*************\
    * Main method *
    \*************/

    public static void main(String[] args) {
        //these will hold the data retrieved from the database
        List<CountryRecord> countryData;
        int totalNumCountries;
        StatsData growthRateStats, imrStats, lerStats, sexRatioStats;
        float[] imrAverages, lerAverages, sexRatioAverages;

        //first, connect and fetch data from the database

        System.out.print("Retrieving data from the database... ");
        try (
            //open a connection
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME,
                                                          DB_PASSWORD)
        ) {
            //fetch country data
            countryData = getCountryData(conn);
            System.out.print("25%... ");

            //total number of countries
            totalNumCountries = getTotalNumCountries(conn);
            System.out.print("50%... ");

            //field statistics
            growthRateStats = getStats(conn, "growth.rate");
            imrStats = getStats(conn, "worldimr.imr");
            lerStats = getStats(conn, "worldimr.ler");
            sexRatioStats = getStats(conn, "pop.sex_ratio");
            System.out.print("75%... ");

            //field averages
            imrAverages = getWorldimrAverages(conn, IMR_FIELDS);
            lerAverages = getWorldimrAverages(conn, LER_FIELDS);
            sexRatioAverages = getSexRatioAverages(conn);
        } catch (SQLException e) {
            System.err.println("error:");
            e.printStackTrace();
            return;
        }
        System.out.println("done.");

        //now, print the report

        //main title
        System.out.println();
        printTitle("Country Data Report", Alignment.CENTER);
        System.out.println();

        //report information section
        printTitle("Report Information", Alignment.LEFT);
        System.out.println("Description: " + REPORT_DESCRIPTION);
        System.out.println("Generated on: " + new java.util.Date());
        System.out.println();

        //statistics section
        printTitle("Statistics", Alignment.LEFT);
        System.out.format("There are %d countries in the report " +
                          "out of %d total (%d%%).%n",
                          countryData.size(), totalNumCountries,
                          (countryData.size()*100)/totalNumCountries);
        System.out.println();

        //growth rate stats
        printStats("Growth Rate", growthRateStats, GROWTHRATE_PRECISION,
                   countryData, CountryField.GROWTHRATE);
        System.out.println();

        //imr stats
        printStats("Infant Mortality Rate", imrStats, IMR_PRECISION,
                   countryData, CountryField.IMR);
        System.out.println();
        printImrAverages(imrAverages);
        System.out.println();

        //ler stats
        printStats("Life Expectancy Rate", lerStats, LER_PRECISION,
                   countryData, CountryField.LER);
        System.out.println();
        printLerAverages(lerAverages);
        System.out.println();

        //sex ratio stats
        printStats("Sex Ratio", sexRatioStats, SEXRATIO_PRECISION,
                   countryData, CountryField.SEXRATIO);
        System.out.println();
        printSexRatioAverages(sexRatioAverages);
        System.out.println();

        //top and bottom countries section
        printTitle("Top and Bottom Countries", Alignment.LEFT);
        printTopBottomCountries("Growth Rate", countryData,
                                CountryField.GROWTHRATE, "highest growth",
                                "lowest growth", GROWTHRATE_PRECISION);
        System.out.println();
        printTopBottomCountries("Infant Mortality Rate", countryData,
                                CountryField.IMR, "worst rate", "best rate",
                                IMR_PRECISION);
        System.out.println();
        printTopBottomCountries("Life Expectancy Rate", countryData,
                                CountryField.LER, "best rate", "worst rate",
                                LER_PRECISION);
        System.out.println();
        printTopBottomCountries("Sex Ratio", countryData,
                                CountryField.SEXRATIO, "most males",
                                "most females", SEXRATIO_PRECISION);
        System.out.println();

        //country data section
        printTitle("Country Data Table", Alignment.LEFT);
        printCountryData(countryData);
    }

    /***************************\
    * Database access functions *
    \***************************/

    /**
     * This method retrieves all the data specified in CountryRecord for all
     *   countries satisfying REPORT_CONSTRAINT in REPORT_TABLE.
     *
     * @param  conn a connection to the database
     * @return the list of country data
     * @throws SQLException if a database error occurs
     */
    static List<CountryRecord> getCountryData(Connection conn)
            throws SQLException {
        //the query to execute
        String query =
            "SELECT growth.country, growth.rate, worldimr.imr, " +
                   "worldimr.ler, pop.sex_ratio " +
            "FROM " + REPORT_TABLE + " " +
            "WHERE " + REPORT_CONSTRAINT + " " +
                "AND pop.age = 'Total' " +
            "ORDER BY growth.country";

        //this will hold the returned data
        ArrayList<CountryRecord> countryData = new ArrayList<>();

        //get the data
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                //create and populate a record
                CountryRecord currRecord = new CountryRecord();

                currRecord.name = rs.getString(1).trim();
                currRecord.growthRate = rs.getFloat(2);
                currRecord.imr = rs.getFloat(3);
                currRecord.ler = rs.getFloat(4);
                currRecord.sexRatio = rs.getFloat(5);

                //append to the list
                countryData.add(currRecord);
            }
        }

        return countryData;
    }

    /**
     * This method returns the total number of countries in REPORT_TABLE.
     *
     * @param  conn a connection to the database
     * @return the total number of countries
     * @throws SQLException if a database error occurs
     */
    static int getTotalNumCountries(Connection conn) throws SQLException {
        //the query to execute
        String query =
            "SELECT COUNT(growth.country) " +
            "FROM " + REPORT_TABLE + " " +
            "WHERE pop.age = 'Total'";

        //execute the query
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            rs.next(); //move to the first ResultSet row

            //return the value
            return rs.getInt(1);
        }
    }

    /**
     * This method returns some statistics for the given column for all
     *   countries satisfying REPORT_CONSTRAINT in REPORT_TABLE.
     *
     * @param  conn  a connection to the database
     * @param  field the field for which to compute statistics
     * @return the resulting statistics
     * @throws SQLException if a database error occurs
     */
    static StatsData getStats(Connection conn, String field)
            throws SQLException {
        //the query to execute
        String query =
            "SELECT AVG(" + field + "), " + "STDDEV(" + field + "), " +
                   "MIN(" + field + "), " + "MAX(" + field + ") " +
            "FROM " + REPORT_TABLE + " " +
            "WHERE " + REPORT_CONSTRAINT + " " +
                "AND pop.age = 'Total'";

        //execute the query
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            rs.next(); //move to the first ResultSet row

            //create, populate, and return a StatsData object
            StatsData stats = new StatsData();

            stats.average = rs.getFloat(1);
            stats.stdDev = rs.getFloat(2);
            stats.min = rs.getFloat(3);
            stats.max = rs.getFloat(4);

            return stats;
        }
    }

    /**
     * This method returns the averages for a list of fields in the worldimr
     *   table for all countries satisfying REPORT_CONSTRAINT in REPORT_TABLE.
     *
     * @param  conn   a connection to the database
     * @param  fields the list of fields to average
     * @return a list of averages for each field
     * @throws SQLException if a database error occurs
     */
    static float[] getWorldimrAverages(Connection conn, String[] fields)
            throws SQLException {
        float[] averages = new float[fields.length];

        for (int i = 0; i < fields.length; i++) {
            //the query to execute
            String query =
                "SELECT AVG(worldimr." + fields[i] + ") " +
                "FROM " + REPORT_TABLE + " " +
                "WHERE " + REPORT_CONSTRAINT + " " +
                    "AND pop.age = 'Total'";

            //execute the query
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(query);
                rs.next(); //move to the first ResultSet row

                //get the average value
                averages[i] = rs.getFloat(1);
            }
        }

        return averages;
    }

    /**
     * This method returns the averages for sex_ratio for the age ranges in
     *   SEXRATIO_RANGES for all countries satisfying REPORT_CONSTRAINT in
     *   REPORT_TABLE.
     *
     * @param  conn a connection to the database
     * @return a list of averages for each age range
     * @throws SQLException if a database error occurs
     */
    static float[] getSexRatioAverages(Connection conn) throws SQLException {
        float[] averages = new float[SEXRATIO_RANGES.length];

        for (int i = 0; i < SEXRATIO_RANGES.length; i++) {
            //the query to execute
            String query =
                "SELECT AVG(pop.sex_ratio) " +
                "FROM " + REPORT_TABLE + " " +
                "WHERE " + REPORT_CONSTRAINT + " " +
                    "AND pop.age = '" + SEXRATIO_RANGES[i] + "'";

            //execute the query
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(query);
                rs.next(); //move to the first ResultSet row

                //get the average value
                averages[i] = rs.getFloat(1);
            }
        }

        return averages;
    }

    /***********************\
    * Text output functions *
    \***********************/

    /**
     * This method prints the given title string with an underline, aligned
     *   as specified.
     *
     * @param title     the title to print
     * @param alignment the way to align the given title
     */
    static void printTitle(String title, Alignment alignment) {
        //print the title
        System.out.println(formatTextField(title, alignment, PAGE_WIDTH));

        //print an underline for the title
        String underline = repeatChar('-', title.length());
        System.out.println(formatTextField(underline, alignment, PAGE_WIDTH));
    }

    /**
     * This method prints the information in the given StatsData object, as
     *   well as a histogram showing the distribution of the values.
     *
     * @param title       the title of the stats section
     * @param stats       the StatsData containing statistics to print
     * @param precision   the number of decimal places to use for numbers
     * @param countryData all country data, used to make the histogram
     * @param field       the field from countryData to use for the histogram
     */
    static void printStats(String title, StatsData stats, int precision,
                           List<CountryRecord> countryData,
                           CountryField field) {
        //print the title
        System.out.println(title + ":");

        //print stats, indented 2 spaces
        printStringList(indentStrings(Arrays.asList(
            "Average was " + formatFloat(stats.average, precision + 1) +
                " and standard deviation was " +
                formatFloat(stats.stdDev, precision + 1) + ".",
            "Range was from " + formatFloat(stats.min, precision) +
                " to " + formatFloat(stats.max, precision) + ".",
            "Distribution:"
        ), 2));

        //figure out the histogram step size
        float stepSize = (stats.max - stats.min)/DISTRIBUTION_BARS;

        //find the histogram counts
        float[] histCounts = new float[DISTRIBUTION_BARS];
        for (CountryRecord country : countryData) {
            //find the index of the histogram to increment
            int histIndex = (int)((field.get(country) - stats.min)/stepSize);

            //the max value in the array should go in the last bar
            if (histIndex == DISTRIBUTION_BARS)
                histIndex--;

            //increment the value
            histCounts[histIndex]++;
        }

        //make the graph row titles
        ArrayList<String> graphTitles = new ArrayList<String>();
        for (int i = 0; i < DISTRIBUTION_BARS; i++) {
            //find the lower bound for the range
            String rangeLowerBound = formatFloat(stats.min + i*stepSize,
                                                 precision);

            //find the upper bound for the range
            String rangeUpperBound;
            if (i == DISTRIBUTION_BARS - 1) {
                //for the last bar, do not remove the max value from the range
                rangeUpperBound = formatFloat(stats.min + (i+1)*stepSize,
                                              precision);
            } else {
                rangeUpperBound = formatFloat(stats.min + (i+1)*stepSize -
                                              (float)Math.pow(10, -precision),
                                              precision);
            }

            //add the range title
            graphTitles.add(formatTextField(
                rangeLowerBound + " - " + rangeUpperBound
            , Alignment.RIGHT, 13) + " ");
        }
        for (int i = 0; i < 3; i++) {
            //add padding space for the bottom part of the graph
            graphTitles.add(repeatChar(' ', 14));
        }

        //make the bar graph
        List<String> graph = makeBarGraph(histCounts, 0,
                                          "Number of countries");

        //print distribution histogram, indented 4 spaces
        printStringList(indentStrings(
            joinHorizontal(graphTitles, graph, false)
        , 4));
    }

    /**
     * This method prints the infant mortality rate averages retrieved by
     *   getWorldimrAverages.
     *
     * @param imrAverages the infant mortality rate averages to print
     */
    static void printImrAverages(float[] imrAverages) {
        //print the title
        System.out.println("Average infant mortality rate by age and gender:");

        //the graph vertical axis titles
        List<String> graphTitles = Arrays.asList(
            "Overall   | All     ",
            "          | Males   ",
            "          | Females ",
            "1-4 years | All     ",
            "          | Males   ",
            "          | Females ",
            "5 years   | All     ",
            "          | Males   ",
            "          | Females ",
            //padding space for the bottom part of the graph
            "                    ",
            "                    ",
            "                    "
        );

        //make the bar graph
        List<String> graph = makeBarGraph(imrAverages, IMR_PRECISION + 1,
                                          "Infant Mortality Rate");

        //format and print the graph
        printStringList(indentStrings(
            joinHorizontal(graphTitles, graph, false)
        , 2));
    }

    /**
     * This method prints the life expectancy rate averages retrieved by
     *   getWorldimrAverages.
     *
     * @param lerAverages the life expectancy rate averages to print
     */
    static void printLerAverages(float[] lerAverages) {
        //print the title
        System.out.println("Average life expectancy rate by gender:");

        //the graph vertical axis titles
        List<String> graphTitles = Arrays.asList(
            "All     ",
            "Males   ",
            "Females ",
            //padding space for the bottom part of the graph
            "        ",
            "        ",
            "        "
        );

        //make the bar graph
        List<String> graph = makeBarGraph(lerAverages, LER_PRECISION + 1,
                                          "Life Expectancy Rate");

        //format and print the graph
        printStringList(indentStrings(
            joinHorizontal(graphTitles, graph, false)
        , 2));
    }

    /**
     * This method prints the sex ratio averages retrieved by
     *   getSexRatioAverages.
     *
     * @param sexRatioAverages the sex ratio averages to print
     */
    static void printSexRatioAverages(float[] sexRatioAverages) {
        //print the title and legend
        System.out.println("Average sex ratio across age groups:");
        System.out.println(" Legend: M - Male, F - Female");

        //print the values
        for (int i = 0; i < SEXRATIO_RANGES.length; i++) {
            System.out.println(
                //column title
                formatTextField(SEXRATIO_RANGES[i], Alignment.RIGHT, 7) +
                //column value
                "|" + sexRatioToString(sexRatioAverages[i], 40)
            );
        }

        //print the footer
        System.out.println(repeatChar(' ', 7) + repeatChar('-', 49));
    }

    /**
     * This method prints the top and bottom countries in the given country
     *   list, ranked by the given field.
     *
     * @param title             the title of this section
     * @param countryData       the list of countries to rank
     * @param field             the field to use to rank the countries
     * @param topDescription    a description for the top countries
     * @param bottomDescription a description for the bottom countries
     * @param precision         the number of decimal places to use for numbers
     */
    static void printTopBottomCountries(String title,
                                        List<CountryRecord> countryData,
                                        CountryField field,
                                        String topDescription,
                                        String bottomDescription,
                                        int precision) {
        //print the title
        System.out.println(title + ":");

        //find the top countries
        List<String> topCountries = rankCountries(countryData, field, true,
                                                  precision, topDescription);

        //find the bottom countries
        List<String> bottomCountries = rankCountries(countryData, field, false,
                                                     precision,
                                                     bottomDescription);

        //join and print the lists
        printStringList(joinHorizontal(
            topCountries, indentStrings(bottomCountries, 1)
        , true));
    }

    /**
     * This method prints the information for all countries in the given list
     *   as a table.
     *
     * @param countryData the country data to print
     */
    static void printCountryData(List<CountryRecord> countryData) {
        //print the legend
        System.out.println(" Legend: IMR - Infant Mortality Rate");
        System.out.println("         LER - Life Expectancy Rate");
        System.out.println("         M - Male, F - Female");

        //print the table header
        System.out.println(repeatChar('-', 79));
        System.out.println("|           Country            |Growth Rate" +
                           "|  IMR  |  LER  |    Sex Ratio     |");
        System.out.println(repeatChar('-', 79));

        //print the table data
        for (CountryRecord country : countryData) {
            System.out.println(
                //country name
                "| " + formatTextField(country.name, Alignment.LEFT, 28) +
                //growth rate
                " | " + formatTextField(formatFloat(country.growthRate,
                                                    GROWTHRATE_PRECISION),
                                        Alignment.RIGHT, 9) +
                //imr
                " | " + formatTextField(formatFloat(country.imr,
                                                    IMR_PRECISION),
                                        Alignment.RIGHT, 5) +
                //imr
                " | " + formatTextField(formatFloat(country.ler,
                                                    LER_PRECISION),
                                        Alignment.RIGHT, 5) +
                //sex ratio
                " |" + formatTextField(sexRatioToString(country.sexRatio, 10),
                                       Alignment.LEFT, 18) + "|"
            );
        }

        //print the table footer
        System.out.println(repeatChar('-', 79));
    }

    /**
     * This method makes a bar graph from the given list of values.
     * All the values in the list should be positive.
     *
     * @param values    the numbers to plot
     * @param precision the number of decimal places to use for numbers
     * @param axisTitle the title of the bottom horizontal axis
     * @return a bar graph as a list of strings, each entry is a line
     */
    static List<String> makeBarGraph(float[] values, int precision,
                                     String axisTitle) {
        ArrayList<String> output = new ArrayList<>();

        //find the value of one bar tick
        float barScale = max(values)/BARGRAPH_LONGEST_BAR;

        //add the bars
        for (float val : values) {
            output.add("|" + repeatChar('#', Math.round(val/barScale)) +
                       " (" + formatFloat(val, precision) + ")");
        }

        //add the table bottom
        output.add(repeatChar('-', BARGRAPH_LONGEST_BAR + 1));

        //add the axis tick labels
        StringBuilder axisLabels = new StringBuilder();
        while (axisLabels.length() < BARGRAPH_LONGEST_BAR + 1) {
            axisLabels.append(formatFloat(axisLabels.length()*barScale,
                                          precision) + "  ");
        }
        output.add(axisLabels.toString());

        //add the bottom axis title
        output.add(formatTextField(axisTitle, Alignment.CENTER,
                                   BARGRAPH_LONGEST_BAR + 1));

        return output;
    }

    /**
     * This method ranks the given list of countries by the given field,
     *   and produces a text summary of the results.
     *
     * @param countryData the list of countries to rank
     * @param field       the field to use to rank the countries
     * @param getTop      if true, finds the top countries, otherwise finds
     *                      the bottom ones
     * @param precision   the number of decimal places to use for numbers
     * @param description a description for the bottom countries
     * @return a text summary of the results, each entry is a line
     */
    static List<String> rankCountries(List<CountryRecord> countryData,
                                      final CountryField field, boolean getTop,
                                      int precision, String description) {
        ArrayList<String> output = new ArrayList<>();

        //check if there are fewer than RANK_NUM_COUNTRIES countries available
        int numCountries = RANK_NUM_COUNTRIES;
        if (numCountries > countryData.size()) {
            numCountries = countryData.size();
        }

        //add the title
        output.add(formatTextField(
            (getTop ? "Top " : "Bottom ") + numCountries +
            " countries (" + description + "):"
        , Alignment.LEFT, 39));

        //sort the list in ascending order
        ArrayList<CountryRecord> sortedData = new ArrayList<>(countryData);
        Collections.sort(sortedData, new Comparator<CountryRecord>() {
            @Override
            public int compare(CountryRecord c1, CountryRecord c2) {
                return Float.compare(field.get(c1), field.get(c2));
            }
        });

        //for the top countries, reverse the sorted list
        if (getTop) {
            Collections.reverse(sortedData);
        }

        //add the values
        for (int i = 0; i < numCountries; i++) {
            CountryRecord country = sortedData.get(i);

            output.add(formatTextField(
                (i+1) + ". " + country.name + " (" +
                formatFloat(field.get(country), precision) + ")"
            , Alignment.LEFT, 39));
        }

        return output;
    }

    /**
     * This method prints a list of strings, with a newline after each string.
     *
     * @param list the list of strings to print
     */
    static void printStringList(List<String> list) {
        for (String str : list) {
            //print each string
            System.out.println(str);
        }
    }

    /**
     * This method adds the given number of spaces before each string in the
     *   given list.
     *
     * @param  list   the list of strings to indent
     * @param  indent the number of spaces to add
     * @return the list of strings, with spaces added
     */
    static List<String> indentStrings(List<String> list, int indent) {
        ArrayList<String> output = new ArrayList<>();

        for (String str : list) {
            //add the given number of spaces to each string
            output.add(repeatChar(' ', indent) + str);
        }

        return output;
    }

    /**
     * This method appends the strings in the given lists pairwise.
     * list1 and list2 should have the same size.
     *
     * @param list1        the first list of strings to join
     * @param list2        the second list of strings to join
     * @param addSeparator if true, adds '|' between the strings
     * @return the list of joined strings
     */
    static List<String> joinHorizontal(List<String> list1, List<String> list2,
                                       boolean addSeparator) {
        //the input lists should have the same size
        assert (list1.size() == list2.size());

        ArrayList<String> output = new ArrayList<>();

        for (int i = 0; i < list1.size(); i++) {
            //append the strings together
            if (addSeparator)
                output.add(list1.get(i) + "|" + list2.get(i));
            else
                output.add(list1.get(i) + list2.get(i));
        }

        return output;
    }

    /**
     * This method formats the given string for a fixed-size field.
     * text.length() should not be larger than fieldWidth.
     *
     * @param  text       the text for the field
     * @param  alignment  the desired alignment
     * @param  fieldWidth the width of the field
     * @return the given text formatted for a fixed-size field
     */
    static String formatTextField(String text, Alignment alignment,
                                  int fieldWidth) {
        //check for string which is too large
        if (text.length() >= fieldWidth) {
            return text; //just don't do any formatting
        }

        //this will hold the output string
        StringBuilder output = new StringBuilder(fieldWidth);

        //add padding before text
        if (alignment == Alignment.RIGHT) {
            //for right-alignment, add spaces on the left
            output.append(repeatChar(' ', fieldWidth - text.length()));
        } else if (alignment == Alignment.CENTER) {
            //for center-alignment, we prefer fewer spaces on the left (floor)
            output.append(repeatChar(' ',
                (int)Math.floor((double)(fieldWidth - text.length())/2)));
        }

        //add the text
        output.append(text);

        //add padding after text
        if (alignment == Alignment.LEFT) {
            //for left-alignment, add spaces on the right
            output.append(repeatChar(' ', fieldWidth - text.length()));
        } else if (alignment == Alignment.CENTER) {
            //for center-alignment, we prefer more spaces on the right (ceil)
            output.append(repeatChar(' ',
                (int)Math.ceil((double)(fieldWidth - text.length())/2)));
        }

        return output.toString();
    }

    /**
     * This method produces a visual representation of a sex ratio value.
     * It depicts the number of males and females which would be in a
     *   population of the given size.
     *
     * @param  sexRatio the sex ratio
     * @param  popSize  the size of the population to depict
     * @return a graphical representation of the given sex ratio
     */
    static String sexRatioToString(float sexRatio, int popSize) {
        //find number of males and females
        int numFemales = Math.round((popSize*100)/(sexRatio+100));
        int numMales = popSize - numFemales;

        //return the representation
        return repeatChar('M', numMales) + repeatChar('F', numFemales) +
               " (" + formatFloat(sexRatio, SEXRATIO_PRECISION) + ")";
    }

    /**
     * This method rounds the given float to a specified number of decimal
     *   places, returning the result as a string.
     * The BigDecimal.ROUND_HALF_UP rounding mode is used.
     *
     * @param  num      the number to format
     * @param  decimals the number of decimal places to include
     * @return a string with the given number rounded to the given number of
     *           decimal places
     */
    static String formatFloat(float num, int decimals) {
        return BigDecimal.valueOf(num)
                         .setScale(decimals, BigDecimal.ROUND_HALF_UP)
                         .toPlainString();
    }

    /**
     * This method creates a string which consists of the given character c
     *   repeated n times.
     *
     * @param  c the character to repeat
     * @param  n the number of times to repeat c
     * @return a string which consists of c repeated n times
     */
    static String repeatChar(char c, int n) {
        //make an array repeating the char n times
        char[] chars = new char[n];
        Arrays.fill(chars, c);

        //convert to a String
        return new String(chars);
    }

    /**
     * This method finds the maximum value in an array.
     *
     * @param  values the array
     * @return the maximum value in the given array
     */
    static float max(float[] values) {
        float maxVal = values[0];

        //loop through the array and find the largest value
        for (int i = 1; i < values.length; i++) {
            if (values[i] > maxVal) {
                maxVal = values[i];
            }
        }

        return maxVal;
    }
}