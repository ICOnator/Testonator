package io.iconator.testrpcj;

/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;


@SuppressWarnings("serial")
public class RPCServlet extends HttpServlet {

    @Nullable
    private JsonRpcServer jsonRpcServer;

    public RPCServlet(JsonRpcServer jsonRpcServer) {
        this.jsonRpcServer = jsonRpcServer;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Assert.state(this.jsonRpcServer != null, "No HttpRequestHandler available");

        LocaleContextHolder.setLocale(request.getLocale());
        try {
            handleRequest(request, response);
        }
        catch (HttpRequestMethodNotSupportedException ex) {
            String[] supportedMethods = ex.getSupportedMethods();
            if (supportedMethods != null) {
                response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
            }
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
        }
        finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws  ServletException, IOException {
        jsonRpcServer.handle(request, response);
        response.getOutputStream().flush();
    }

}

