package com.kaiyikang.minitomcat.engine.mapping;

import jakarta.servlet.Filter;

public class FilterMapping extends AbstractMapping {

    public final String filterName;
    public final Filter filter;

    public FilterMapping(String filterName, String urlPattern, Filter filter) {
        super(urlPattern);
        this.filter = filter;
        this.filterName = filterName;
    }

}
