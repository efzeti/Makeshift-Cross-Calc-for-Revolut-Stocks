package fz.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {


    private static final Logger log = LoggerFactory.getLogger(Main.class);


    // at this moment the program takes all the historica data from yahoofinance and calculates everything in the meantime.
    // It takes a while and is totally inefficient. Potential improvement: store historical data in SQL DB and update it
    // daily after the market closes. Taking data from SQL will be certainly faster than from online service. Second
    // potential improvement would be to store the SMA values in the SQL as well.

    public static void main(String[] args) {

        CrossCounter crossCounter = new CrossCounter();

        crossCounter.findCrosses(true);


    }

}




