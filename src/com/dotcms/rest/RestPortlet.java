package com.dotcms.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.portlet.Portlet;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cmis.proxy.DotInvocationHandler;
import com.dotmarketing.cmis.proxy.DotResponseProxy;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseImpl;

public abstract class RestPortlet extends WebResource implements Portlet, Cloneable {

	@Override
	public void destroy() {

	}


	@Override
	public void init(PortletConfig config) throws PortletException {


	}

	@Override
	public void processAction(ActionRequest arg0, ActionResponse arg1) throws PortletException, IOException {

	}

	/**
	 * The render request will always be handled by the "render.jsp" located
	 * under the /WEB-INF/{classname}/ directory: the folder is based on the
	 * class name, lowercased, so this portlets jsp would be:
	 * /WEB-INF/jsp/restportlet/render.jsp
	 */
	@Override
	public void render(RenderRequest req, RenderResponse res)

	throws PortletException, IOException {
		HttpServletRequest request = ((RenderRequestImpl) req).getHttpServletRequest();
		HttpServletResponse response = ((RenderResponseImpl) res).getHttpServletResponse();


		try {
			response.getWriter().write(getJspResponse(request, "render"));
		} catch (ServletException e) {

			e.printStackTrace();
		}

	}

	
	
	private String getJspResponse(HttpServletRequest request, String jspName) throws ServletException, IOException{
		
		
		InvocationHandler dotInvocationHandler = new DotInvocationHandler(new HashMap());

		DotResponseProxy responseProxy = (DotResponseProxy) Proxy.newProxyInstance(DotResponseProxy.class.getClassLoader(),
				new Class[] { DotResponseProxy.class }, dotInvocationHandler);
		
		jspName = (!UtilMethods.isSet(jspName)) ? "render" : jspName;
		
		String path = "/WEB-INF/jsp/" + getName() + "/" + jspName + ".jsp";
		
		
		
		HttpServletResponseWrapper response = new ResponseWrapper(responseProxy);
		Logger.debug(this.getClass(), "trying: " + path);



		try {
			request.getRequestDispatcher(path).include(request, response);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.toString());

		}
		return ((ResponseWrapper) response).getResponseString();


	}
	
	
	
	
	@GET
	@Path("/layout/{params:.*}")
	@Produces("text/html")
	public Response getLayout(@Context HttpServletRequest request, @PathParam("params") String params)
			throws DotDataException, DotSecurityException, ServletException, IOException, DotRuntimeException, PortalException, SystemException {

		
		User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
		
		try {
			if (user == null ||  !com.dotmarketing.business.APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(getName() , user)) {
				Logger.error(this.getClass(), "Invalid User  " + user + "  attempting to access this portlet");
				ResponseBuilder builder = Response.status(403);
				return builder.build();
			}
		} catch (Exception e2) {
			com.dotmarketing.util.Logger.error(this.getClass(), e2.getMessage(), e2);
			ResponseBuilder builder = Response.status(500);
			return builder.build();
		}
		
		
		
		ResponseBuilder builder = Response.ok(getJspResponse(request, params), "text/html");
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);

		return builder.cacheControl(cc).build();

	}

	private class ResponseWrapper extends HttpServletResponseWrapper {

		private StringWriter writer = new StringWriter();

		public ResponseWrapper(HttpServletResponse response) {
			super(response);

		}

		public String getResponseString() {
			return writer.toString();
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			PrintWriter pw = new PrintWriter(writer);
			return pw;
		}

	}

	/**
	 * Returns the proper name for the folder for the jsps
	 * 
	 * @return
	 */

	public String getName() {

		return this.getClass().getSimpleName().toLowerCase();

	}

}
