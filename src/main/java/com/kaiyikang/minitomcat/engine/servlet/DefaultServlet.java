package com.kaiyikang.minitomcat.engine.servlet;

import org.slf4j.LoggerFactory;

import com.kaiyikang.minitomcat.utils.ClassPathUtils;
import com.kaiyikang.minitomcat.utils.DateUtils;
import com.kaiyikang.minitomcat.utils.HtmlUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DefaultServlet extends HttpServlet {
    final Logger logger = LoggerFactory.getLogger(getClass());
    String indexTemplate;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.indexTemplate = ClassPathUtils.readString("/index.html");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {

    }

    static String tr(Path file, long size, String name) throws IOException {

        return "<tr><td><a href=\"" + name + "\">" + HtmlUtils.encodeHtml(name) + "</a></td><td>" + size(size)
                + "</td><td>"
                + DateUtils.formatDateTimeGMT(Files.getLastModifiedTime(file).toMillis()) + "</td>";
    }

    static String size(long size) {
        if (size < 0) {
            return "";
        }
        if (size > 1024 * 1024 * 1024) {
            return String.format(".3f GB", size / (1024 * 1024 * 1024.0));
        }
        if (size > 1024 * 1024) {
            return String.format("%.3f MB", size / (1024 * 1024.0));
        }
        if (size > 1024) {
            return String.format("%.3f KB", size / 1024.0);
        }
        return size + " B";
    }

}
