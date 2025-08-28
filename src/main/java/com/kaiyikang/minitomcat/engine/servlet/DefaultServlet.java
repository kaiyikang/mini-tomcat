package com.kaiyikang.minitomcat.engine.servlet;

import org.slf4j.LoggerFactory;

import com.kaiyikang.minitomcat.utils.ClassPathUtils;
import com.kaiyikang.minitomcat.utils.DateUtils;
import com.kaiyikang.minitomcat.utils.HtmlUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        logger.info("list file or directory: {}", uri);

        if (!uri.startsWith("/")) {
            logger.debug("skip process insecure uri: {}", uri);
            resp.sendError(404, "Not Found");
            return;
        }

        if (uri.equals("/WEB-INF") || uri.startsWith("/WEB-INF/")) {
            logger.debug("prevent access uri: {}", uri);
            resp.sendError(403, "Forbidden");
            return;
        }

        if (uri.indexOf("/../") > 0) {
            logger.debug("Prevent access insecure uri: {}", uri);
            resp.sendError(404, "Not Found");
            return;
        }

        String realPath = req.getServletContext().getRealPath(uri);
        Path path = Paths.get(realPath);
        logger.debug("try access path", path);

        if (Files.isDirectory(path)) {
            // Is a Dir but not end with /
            if (!uri.endsWith("/")) {
                resp.sendRedirect(uri + "/");
                return;
            }

            List<Path> files = Files.list(path).collect(Collectors.toList());
            Collections.sort(files, (f1, f2) -> {
                var s1 = f1.toString();
                var s2 = f2.toString();
                return s1.compareToIgnoreCase(s2);
            });
            StringBuilder sb = new StringBuilder(4096);

            // Add a go back link if it is not root
            if (!uri.equals("/")) {
                sb.append(tr(path.getParent(), -1, ".."));
            }
            // Load all files under the path
            for (Path file : files) {
                String name = file.getFileName().toString();
                long size = -1;
                if (Files.isDirectory(file)) {
                    name = name + "/";
                } else if (Files.isRegularFile(file)) {
                    size = Files.size(file);
                }
                sb.append(tr(file, size, name));
            }

            // Render it
            String trs = sb.toString();
            String html = this.indexTemplate.replace("${URL}", HtmlUtils.encodeHtml(uri))
                    .replace("${SERVER}", getServletContext().getServerInfo())
                    .replace("${TRS}", trs);
            PrintWriter pw = resp.getWriter();
            pw.write(html);
            pw.flush();
            return;
        } else if (Files.isRegularFile(path)) {
            // Render the file
            if (Files.isReadable(path)) {
                logger.debug("read file: {}", path);
                resp.setContentType(getServletContext().getMimeType(uri));
                ServletOutputStream output = resp.getOutputStream();
                try (InputStream input = new BufferedInputStream(new FileInputStream(path.toFile()))) {
                    input.transferTo(output);
                }
                output.flush();
                return;
            } else {
                resp.sendError(403, "Forbidden");
                return;
            }
        }
        resp.sendError(404, "Not Found");
        return;
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
