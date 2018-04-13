/*
 * Copyright 2015, 2016 Ether.Camp Inc. (US)
 * This file is part of Ethereum Harmony.
 *
 * Ethereum Harmony is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ethereum Harmony is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ethereum Harmony.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.iconator.commons.test.jsonrpc;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Stan Reshetnyk on 21.07.16.
 */
@WebFilter(urlPatterns = "/rpc")
public class AddContentTypeFilter implements Filter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddContentTypeFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if ((request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            if ("/rpc".equals(httpRequest.getRequestURI())) {
                log.info("Found " + httpRequest.getRequestURI());
                AddParamsToHeader updatedRequest = new AddParamsToHeader((HttpServletRequest) request);
                httpResponse.addHeader("content-type", "application/json");
                httpResponse.addHeader("accept", "application/json");
                chain.doFilter(updatedRequest, response);
            } else {
                chain.doFilter(request, response);
            }
        } else {
            throw new RuntimeException("AddContentTypeFilter supports only HTTP requests.");
        }
    }

    @Override
    public void destroy() {

    }
}

class AddParamsToHeader extends HttpServletRequestWrapper {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddParamsToHeader.class);

    public AddParamsToHeader(HttpServletRequest request) {
        super(request);
    }

    public String getHeader(String name) {
        log.info("getHeader " + name + ". Result:" + super.getHeader(name));
        if (name != null && "content-type".equals(name.toLowerCase())) {
            return "application/json";
        }

        return super.getHeader(name);
    }

    public Enumeration getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
//        names.add("content-type");
//        names.addAll(Collections.list(super.getParameterNames()));
        return Collections.enumeration(names);
    }
}
