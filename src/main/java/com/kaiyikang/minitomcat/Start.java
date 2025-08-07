package com.kaiyikang.minitomcat;

import org.slf4j.Logger;

import com.kaiyikang.minitomcat.connector.HttpConnector;

public class Start {

    static Logger logger = org.slf4j.LoggerFactory.getLogger(Start.class);

    public static void main(String[] args) throws Exception {
        try (HttpConnector httpConnector = new HttpConnector()) {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("mini tomcat http server was shutdown.");
    }
}
