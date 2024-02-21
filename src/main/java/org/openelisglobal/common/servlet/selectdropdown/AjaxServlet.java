package org.openelisglobal.common.servlet.selectdropdown;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author diane benz bugzilla 2443
 */
public abstract class AjaxServlet extends HttpServlet {
    /**
       * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
       *      javax.servlet.http.HttpServletResponse)
       */
      public void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    
        String xml = null;
    
        try {
          xml = getXmlContent(request, response);
        } catch (Exception ex) {
          // Send back a 500 error code.
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can not create response");
          return;
        }
    
        // Set content to xml
        response.setContentType("text/xml; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        PrintWriter pw = response.getWriter();
        pw.write(xml);
        pw.close();
      }
    
      /**
       * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
       *      javax.servlet.http.HttpServletResponse)
       */
      public void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
        doGet(request, response);
      }
    
      /**
       * Each child class should override this method to generate the specific XML content necessary for
       * each AJAX action.
       *
       * @param request the {@javax.servlet.http.HttpServletRequest} object
       * @param response the {@javax.servlet.http.HttpServletResponse} object
       * @return a {@java.lang.String} representation of the XML response/content
       */
      public abstract String getXmlContent(HttpServletRequest request, HttpServletResponse response)
          throws Exception;
    }
    